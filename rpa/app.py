import os
import sys
import time
import subprocess
import logging
import threading
import requests
from flask import Flask, request, jsonify
from dotenv import load_dotenv

# 루트 .env 로드
dotenv_path = os.path.join(os.path.dirname(__file__), "..", ".env")
load_dotenv(dotenv_path)

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

# 동시 실행 최대 수 3개 통제 세마포어
MAX_CONCURRENT_TASKS = 3
task_semaphore = threading.Semaphore(MAX_CONCURRENT_TASKS)

# 백엔드 연동 설정
def get_backend_callback_url():
    url = os.getenv("BACKEND_CALLBACK_URL")
    if url:
        return url
    base_url = os.getenv("BACKEND_API_BASE_URL", "http://localhost:8080/api/v1")
    return f"{base_url}/callbacks/rpa"

BACKEND_CALLBACK_URL = get_backend_callback_url()
RPA_CALLBACK_TOKEN = os.getenv("RPA_CALLBACK_TOKEN")

def send_failure_callback(task_id: str, error_message: str):
    """
    RPA 실행 중 타임아웃 또는 예외가 발생했을 때 백엔드 콜백 엔드포인트로 실패 웹훅을 전송합니다.
    """
    if not RPA_CALLBACK_TOKEN:
        logger.error("[RPA Callback] RPA_CALLBACK_TOKEN is missing in environment variables. Webhook skipped.")
        return

    headers = {
        "Content-Type": "application/json",
        "X-RPA-Token": RPA_CALLBACK_TOKEN
    }
    payload = {
        "taskId": task_id,
        "status": "failed",
        "error": {
            "code": "RPA_EXECUTION_TIMEOUT",
            "message": error_message
        }
    }
    try:
        response = requests.post(BACKEND_CALLBACK_URL, json=payload, headers=headers, timeout=5)
        logger.info(f"[RPA Callback] Sent failure callback for task {task_id}. Response Code: {response.status_code}")
    except Exception as e:
        logger.error(f"[RPA Callback] Failed to send failure callback for task {task_id}: {str(e)}")


def run_task_with_timeout(task_id: str, python_executable: str, task_script: str, creation_flags: int):
    """
    서브프로세스로 Playwright 태스크를 띄우고 최대 120초 동안 생명주기를 감시합니다.
    """
    try:
        logger.info(f"[RPA Monitor Thread] Spawning subprocess for task: {task_id}")
        proc = subprocess.Popen(
            [python_executable, task_script, task_id],
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
            creationflags=creation_flags
        )
        
        try:
            # 최대 120초 동안 서브프로세스 종료 대기
            proc.wait(timeout=120)
            logger.info(f"[RPA Monitor Thread] Task {task_id} process terminated naturally.")
        except subprocess.TimeoutExpired:
            logger.error(f"[RPA Monitor Thread] Task {task_id} exceeded 120 seconds limit! Force killing process.")
            proc.kill()
            proc.wait() # 잔여 자원 회수 및 좀비 소멸 보장
            logger.info(f"[RPA Monitor Thread] Task {task_id} process force killed successfully.")
            # 백엔드에 타임아웃 에러 통보
            send_failure_callback(task_id, "RPA 태스크가 제한시간(120초)을 초과하여 강제 종료되었습니다.")
            
    except Exception as e:
        logger.error(f"[RPA Monitor Thread] Exception during process lifecycle management for task {task_id}: {str(e)}")
        send_failure_callback(task_id, f"RPA 모니터링 내부 에러: {str(e)}")
    finally:
        # 작업 종료 후 자원 반환
        task_semaphore.release()
        logger.info(f"[RPA Monitor Thread] Semaphore released. Current available resources.")


def create_app():
    app = Flask(__name__)

    @app.route("/api/v1/rpa/trigger", methods=["POST"])
    def trigger_rpa():
        """
        비동기 RPA 실행 요청을 수신하여 세마포어가 가용하면 즉시 감시 스레드를 기동하고
        202 Accepted를 반환합니다. 가용한 리소스가 없으면 429 Too Many Requests를 반환합니다.
        """
        # 세마포어 논블로킹 획득 시도
        acquired = task_semaphore.acquire(blocking=False)
        if not acquired:
            logger.warning("[RPA Trigger Server] Rejecting request: Max concurrent limit reached.")
            return jsonify({
                "status": "error",
                "message": "현재 동시 실행 가능한 RPA 작업 최대치(3개)를 초과했습니다. 잠시 후 다시 시도해 주세요.",
                "code": "RPA_LIMIT_EXCEEDED"
            }), 429

        try:
            data = request.get_json() or {}
            task_id = data.get("taskId")
            action = data.get("action")

            if not task_id:
                task_semaphore.release()
                return jsonify({
                    "status": "error",
                    "message": "요청에 taskId가 누락되었습니다.",
                    "code": "INVALID_TRIGGER_PARAMETER"
                }), 400

            logger.info(f"[RPA Trigger Server] Accepted trigger request. Task ID: {task_id}, Action: {action}")

            # 파이썬 실행 경로 구성
            python_executable = sys.executable
            task_script = os.path.join(os.path.dirname(__file__), "tasks", "purchase_order_task.py")

            creation_flags = 0
            if sys.platform == "win32":
                creation_flags = subprocess.CREATE_NO_WINDOW

            # 모니터 및 라이프사이클 관리 스레드 실행 (자원 격리 & 백엔드 즉시 응답 반환 구조)
            monitor_thread = threading.Thread(
                target=run_task_with_timeout,
                args=(task_id, python_executable, task_script, creation_flags),
                daemon=True
            )
            monitor_thread.start()

            return jsonify({
                "status": "accepted",
                "taskId": task_id,
                "message": "RPA 태스크가 대기열에서 해제되어 백그라운드에서 실행되었습니다."
            }), 202

        except Exception as e:
            task_semaphore.release()
            logger.error(f"[RPA Trigger Server] Unexpected error while staging task: {str(e)}")
            return jsonify({
                "status": "error",
                "message": "RPA 태스크 기동 도중 서버 내부 에러가 발생했습니다.",
                "code": "INTERNAL_SERVER_ERROR"
            }), 500

    @app.route("/login", methods=["GET", "POST"])
    def mock_login():
        if request.method == "POST":
            # 로그인 성공 후 테이블 반환
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
                            <th>Vendor Name</th>
                        </tr>
                    </thead>
                    <tbody>
                        <tr>
                            <td>PO-2026-0001</td>
                            <td>유기농 찹쌀 경단</td>
                            <td>150</td>
                            <td>밀양 떡집</td>
                        </tr>
                        <tr>
                            <td>PO-2026-0002</td>
                            <td>단팥 찰떡 세트</td>
                            <td>300</td>
                            <td>종로 떡방</td>
                        </tr>
                    </tbody>
                </table>
            </body>
            </html>
            """
        # GET 요청 시 로그인 폼 반환
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

    @app.route("/health", methods=["GET"])
    def health():
        return {"status": "UP"}, 200

    return app

app = create_app()

if __name__ == "__main__":
    port = int(os.getenv("RPA_SERVER_PORT", 5050))
    host = os.getenv("RPA_SERVER_HOST", "127.0.0.1")
    logger.info(f"[RPA Trigger Server] Starting on {host}:{port} ...")
    app.run(host=host, port=port, debug=False)
