import logging
import os
import platform
import subprocess
import sys
import threading

try:
    import resource
except ImportError:  # pragma: no cover - Windows local check
    resource = None

import requests
from dotenv import load_dotenv
from flask import Flask, jsonify, request

dotenv_path = os.path.join(os.path.dirname(__file__), "..", ".env")
load_dotenv(dotenv_path)

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

MAX_CONCURRENT_TASKS = max(1, int(os.getenv("RPA_MAX_CONCURRENT_TASKS", "1")))
TASK_TIMEOUT_SECONDS = max(30, int(os.getenv("RPA_TASK_TIMEOUT_SECONDS", "90")))
TASK_MEMORY_MB = max(256, int(os.getenv("RPA_TASK_MEMORY_MB", "640")))
PURCHASE_ACTION = "collect_purchase_orders"

task_semaphore = threading.Semaphore(MAX_CONCURRENT_TASKS)
purchase_task_lock = threading.Lock()
active_purchase_tasks = set()

ACTION_SCRIPT_MAP = {
    "collect_purchase_orders": os.path.join(os.path.dirname(__file__), "tasks", "purchase_order_task.py"),
    "check_inventory_shortage": os.path.join(os.path.dirname(__file__), "tasks", "inventory_shortage_task.py"),
    "collect_hr_reference": os.path.join(os.path.dirname(__file__), "tasks", "hr_reference_task.py"),
}


def get_backend_callback_url():
    url = os.getenv("BACKEND_CALLBACK_URL")
    if url:
        return url
    base_url = os.getenv("BACKEND_API_BASE_URL", "http://localhost:8080/api/v1")
    return f"{base_url}/callbacks/rpa"


BACKEND_CALLBACK_URL = get_backend_callback_url()
RPA_CALLBACK_TOKEN = os.getenv("RPA_CALLBACK_TOKEN")


def send_failure_callback(task_id: str, task_type: str, action_name: str, error_message: str):
    if not RPA_CALLBACK_TOKEN:
        logger.error("[RPA Callback] RPA_CALLBACK_TOKEN is missing. Webhook skipped.")
        return

    headers = {
        "Content-Type": "application/json",
        "X-RPA-Token": RPA_CALLBACK_TOKEN,
    }
    payload = {
        "taskId": task_id,
        "taskType": task_type,
        "actionName": action_name,
        "status": "failed",
        "error": {
            "code": "RPA_EXECUTION_TIMEOUT",
            "message": error_message,
        },
    }
    try:
        response = requests.post(BACKEND_CALLBACK_URL, json=payload, headers=headers, timeout=5)
        logger.info("[RPA Callback] Sent failure callback for task %s. Response Code: %s", task_id, response.status_code)
    except Exception as exc:
        logger.error("[RPA Callback] Failed to send failure callback for task %s: %s", task_id, str(exc))


def mark_purchase_task_started(task_id: str):
    with purchase_task_lock:
        active_purchase_tasks.add(task_id)


def mark_purchase_task_finished(task_id: str):
    with purchase_task_lock:
        active_purchase_tasks.discard(task_id)


def has_active_purchase_task() -> bool:
    with purchase_task_lock:
        return bool(active_purchase_tasks)


def current_active_purchase_task() -> str | None:
    with purchase_task_lock:
        return next(iter(active_purchase_tasks), None)


def build_linux_resource_limiter():
    if platform.system().lower() != "linux" or resource is None:
        return None

    memory_bytes = TASK_MEMORY_MB * 1024 * 1024

    def limit_resources():
        resource.setrlimit(resource.RLIMIT_AS, (memory_bytes, memory_bytes))

    return limit_resources


def run_task_with_timeout(
    task_id: str,
    task_type: str,
    action_name: str,
    python_executable: str,
    task_script: str,
    creation_flags: int,
):
    try:
        logger.info("[RPA Monitor Thread] Spawning subprocess for task: %s (%s / %s)", task_id, task_type, action_name)
        proc = subprocess.Popen(
            [python_executable, task_script, task_id, task_type, action_name],
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
            creationflags=creation_flags,
            preexec_fn=build_linux_resource_limiter(),
        )

        try:
            proc.wait(timeout=TASK_TIMEOUT_SECONDS)
            logger.info("[RPA Monitor Thread] Task %s process terminated naturally.", task_id)
        except subprocess.TimeoutExpired:
            logger.error(
                "[RPA Monitor Thread] Task %s exceeded %s seconds limit. Force killing process.",
                task_id,
                TASK_TIMEOUT_SECONDS,
            )
            proc.kill()
            proc.wait()
            send_failure_callback(
                task_id,
                task_type,
                action_name,
                f"RPA 작업이 제한시간({TASK_TIMEOUT_SECONDS}초)을 초과해서 강제 종료됐어.",
            )
    except Exception as exc:
        logger.error("[RPA Monitor Thread] Exception during process lifecycle management for task %s: %s", task_id, str(exc))
        send_failure_callback(task_id, task_type, action_name, f"RPA 모니터링 내부 에러: {str(exc)}")
    finally:
        if action_name == PURCHASE_ACTION:
            mark_purchase_task_finished(task_id)
        task_semaphore.release()
        logger.info("[RPA Monitor Thread] Semaphore released.")


def create_app():
    app = Flask(__name__)

    @app.route("/api/v1/rpa/trigger", methods=["POST"])
    def trigger_rpa():
        data = request.get_json() or {}
        task_id = data.get("taskId")
        task_type = data.get("taskType") or "PURCHASE_PRICE"
        action = data.get("action")

        if not task_id or not action:
            return jsonify({
                "status": "error",
                "message": "taskId 또는 action 이 비어 있어.",
                "code": "INVALID_TRIGGER_PARAMETER",
            }), 400

        task_script = ACTION_SCRIPT_MAP.get(action)
        if not task_script:
            logger.warning("[RPA Trigger Server] Unsupported action received. Task ID: %s, Task Type: %s, Action: %s", task_id, task_type, action)
            return jsonify({
                "status": "error",
                "message": "지원하지 않는 RPA action 이야.",
                "code": "UNSUPPORTED_RPA_ACTION",
            }), 400

        if action == PURCHASE_ACTION and has_active_purchase_task():
            running_task_id = current_active_purchase_task() or "unknown"
            logger.warning("[RPA Trigger Server] Rejecting overlapping purchase crawl. Existing task: %s", running_task_id)
            return jsonify({
                "status": "error",
                "message": f"이미 구매 단가 수집 작업({running_task_id})이 진행 중이야. 완료 후 다시 요청해줘.",
                "code": "PURCHASE_PRICE_ALREADY_RUNNING",
            }), 409

        if not task_semaphore.acquire(blocking=False):
            logger.warning("[RPA Trigger Server] Rejecting request: Max concurrent limit reached.")
            return jsonify({
                "status": "error",
                "message": "현재 동시에 실행 가능한 RPA 작업 수를 넘었어. 잠시 뒤 다시 시도해줘.",
                "code": "RPA_LIMIT_EXCEEDED",
            }), 429

        try:
            python_executable = sys.executable
            creation_flags = subprocess.CREATE_NO_WINDOW if sys.platform == "win32" else 0

            if action == PURCHASE_ACTION:
                mark_purchase_task_started(task_id)

            monitor_thread = threading.Thread(
                target=run_task_with_timeout,
                args=(task_id, task_type, action, python_executable, task_script, creation_flags),
                daemon=True,
            )
            monitor_thread.start()

            return jsonify({
                "status": "accepted",
                "taskId": task_id,
                "taskType": task_type,
                "actionName": action,
                "message": "RPA 작업을 저사양 안전모드로 시작했어.",
            }), 202
        except Exception as exc:
            if action == PURCHASE_ACTION:
                mark_purchase_task_finished(task_id)
            task_semaphore.release()
            logger.error("[RPA Trigger Server] Unexpected error while staging task: %s", str(exc))
            return jsonify({
                "status": "error",
                "message": "RPA 작업 시작 직전에 서버 오류가 발생했어.",
                "code": "INTERNAL_SERVER_ERROR",
            }), 500

    @app.route("/login", methods=["GET", "POST"])
    def mock_login():
        if request.method == "POST":
            return """
            <html>
            <head><title>Mock Vendor Portal - Active Orders</title></head>
            <body>
                <h1>Active Orders</h1>
                <table id="order-list-table" border="1">
                    <thead>
                        <tr>
                            <th>Order No</th>
                            <th>Product Name</th>
                            <th>Quantity</th>
                            <th>Unit Price</th>
                            <th>Spec</th>
                            <th>Vendor Name</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td>PO-2026-0001</td>
                            <td>Sample Almond Powder</td>
                            <td>150</td>
                            <td>18,500</td>
                            <td>1kg / bag</td>
                            <td>Amantea</td>
                        </tr>
                        <tr>
                            <td>PO-2026-0002</td>
                            <td>Sample Stick Butter</td>
                            <td>300</td>
                            <td>9,900</td>
                            <td>10ea / box</td>
                            <td>Amantea</td>
                        </tr>
                    </tbody>
                </table>
            </body>
            </html>
            """
        return """
        <html>
        <head><title>Mock Vendor Portal Login</title></head>
        <body>
            <h2>Mock Vendor Portal Login</h2>
            <form action="/login" method="POST">
                <label>Username: <input type="text" id="username" name="username"></label><br>
                <label>Password: <input type="password" id="password" name="password"></label><br>
                <button type="submit" id="login-btn">Login</button>
            </form>
        </body>
        </html>
        """

    @app.route("/inventory-alerts", methods=["GET"])
    def inventory_alerts():
        return """
        <html>
        <head><title>Mock Vendor Portal - Inventory Alerts</title></head>
        <body>
            <h1>Inventory Alerts</h1>
            <table id="inventory-alert-table" border="1">
                <thead>
                    <tr>
                        <th>Product Name</th>
                        <th>Vendor Name</th>
                        <th>Stock Status</th>
                        <th>Expected Restock Date</th>
                        <th>Recommended Action</th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td>Sample Almond Powder</td>
                        <td>Amantea</td>
                        <td>Low Stock</td>
                        <td>2026-06-03</td>
                        <td>Check urgent purchase order</td>
                    </tr>
                </tbody>
            </table>
        </body>
        </html>
        """

    @app.route("/hr-reference", methods=["GET"])
    def hr_reference():
        return """
        <html>
        <head><title>Mock HR Reference</title></head>
        <body>
            <h1>HR Minimum Wage Reference</h1>
            <table id="hr-reference-table" border="1">
                <thead>
                    <tr>
                        <th>Source</th>
                        <th>Effective Date</th>
                        <th>Minimum Hourly Wage</th>
                        <th>Minimum Monthly Salary</th>
                        <th>Guideline</th>
                    </tr>
                </thead>
                <tbody>
                    <tr>
                        <td>MOEL Notice</td>
                        <td>2026-01-01</td>
                        <td>10,300</td>
                        <td>2,152,700</td>
                        <td>40 hours per week base</td>
                    </tr>
                </tbody>
            </table>
        </body>
        </html>
        """

    @app.route("/health", methods=["GET"])
    def health():
        return {"status": "UP"}, 200

    return app


app = create_app()

if __name__ == "__main__":
    port = int(os.getenv("RPA_SERVER_PORT", 5050))
    host = os.getenv("RPA_SERVER_HOST", "127.0.0.1")
    logger.info("[RPA Trigger Server] Starting on %s:%s ...", host, port)
    app.run(host=host, port=port, debug=False)
