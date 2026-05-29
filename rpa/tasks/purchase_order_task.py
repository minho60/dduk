import os
import sys
import json
import time
import requests
from dotenv import load_dotenv

sys.path.append(os.path.join(os.path.dirname(__file__), ".."))
from engine.playwright_engine import PlaywrightEngine

dotenv_path = os.path.join(os.path.dirname(__file__), "..", "..", ".env")
load_dotenv(dotenv_path)


def run_purchase_order_task(task_id: str, task_type: str = "PURCHASE_PRICE", action_name: str = "collect_purchase_orders"):
    print(f"[RPA Task] Starting purchase order collection. Task ID: {task_id}, Task Type: {task_type}, Action: {action_name}")

    base_url = os.getenv("RPA_BASE_URL", "http://localhost:5050")
    vendor_username = os.getenv("RPA_VENDOR_USERNAME")
    vendor_password = os.getenv("RPA_VENDOR_PASSWORD")
    callback_token = os.getenv("RPA_CALLBACK_TOKEN")

    backend_callback_url = os.getenv("BACKEND_CALLBACK_URL")
    if not backend_callback_url:
        backend_base_url = os.getenv("BACKEND_API_BASE_URL", "http://localhost:8080/api/v1")
        backend_callback_url = f"{backend_base_url}/callbacks/rpa"

    if not vendor_username or not vendor_password:
        handle_error(
            task_id,
            task_type,
            action_name,
            "RPA_CONFIGURATION_ERROR",
            "RPA Vendor credentials are missing in .env",
            backend_callback_url,
            callback_token
        )
        return

    output_dir = os.path.join(os.path.dirname(__file__), "..", "outputs")
    os.makedirs(output_dir, exist_ok=True)
    screenshot_dir = os.path.join(output_dir, "screenshots")
    os.makedirs(screenshot_dir, exist_ok=True)

    engine = PlaywrightEngine()
    context = None
    page = None

    max_retries = 3
    last_exception = None
    last_screenshot_path = None

    for attempt in range(1, max_retries + 1):
        context = None
        page = None
        print(f"[RPA Task] Executing attempt {attempt} of {max_retries} for task: {task_id}")

        try:
            context = engine.get_context()
            page = context.new_page()

            login_url = f"{base_url}/login"
            print(f"[RPA Task] Navigating to {login_url}")
            page.goto(login_url)

            page.fill("#username", vendor_username)
            page.fill("#password", vendor_password)
            page.click("#login-btn")

            page.wait_for_selector("#order-list-table", timeout=10000)
            print("[RPA Task] Login success. Scraping orders...")

            rows = page.query_selector_all("#order-list-table tbody tr")
            orders = []

            for row in rows:
                cols = row.query_selector_all("td")
                if len(cols) >= 6:
                    unit_price_text = cols[3].inner_text().strip().replace(",", "").replace("원", "")
                    orders.append({
                        "orderNo": cols[0].inner_text().strip(),
                        "productName": cols[1].inner_text().strip(),
                        "quantity": int(cols[2].inner_text().strip().replace(",", "")),
                        "unitPrice": int(unit_price_text),
                        "spec": cols[4].inner_text().strip(),
                        "vendorName": cols[5].inner_text().strip()
                    })

            result_path = os.path.join(output_dir, f"orders_{task_id}.json")
            with open(result_path, "w", encoding="utf-8") as file:
                json.dump(orders, file, ensure_ascii=False, indent=2)

            print(f"[RPA Task] Data collection complete. Saved to: {result_path}")

            send_callback(backend_callback_url, callback_token, {
                "taskId": task_id,
                "taskType": task_type,
                "actionName": action_name,
                "status": "success",
                "data": {
                    "ordersCount": len(orders),
                    "filePath": f"rpa/outputs/orders_{task_id}.json"
                }
            })

            if context:
                context.close()
            return

        except Exception as exc:
            last_exception = exc
            print(f"[RPA Task] Attempt {attempt} failed: {str(exc)}")

            screenshot_path = os.path.join(screenshot_dir, f"err_{task_id}_attempt_{attempt}.png")
            last_screenshot_path = screenshot_path
            if page:
                try:
                    page.screenshot(path=screenshot_path)
                    print(f"[RPA Task] Error screenshot saved: {screenshot_path}")
                except Exception as screenshot_exception:
                    print(f"[RPA Task] Failed to take screenshot: {str(screenshot_exception)}")
                    last_screenshot_path = None

            if context:
                try:
                    context.close()
                except Exception:
                    pass
            try:
                engine.shutdown()
            except Exception:
                pass

            if attempt < max_retries:
                time.sleep(2)

    error_msg = str(last_exception) if last_exception else "RPA 스크래핑 시도 횟수를 초과했어."
    handle_error(
        task_id,
        task_type,
        action_name,
        "RPA_EXECUTION_FAILED",
        error_msg,
        backend_callback_url,
        callback_token,
        last_screenshot_path
    )


def send_callback(callback_url: str, token: str, payload: dict):
    headers = {
        "Content-Type": "application/json",
        "X-RPA-Token": token if token else ""
    }
    try:
        print(f"[RPA Task] Sending callback to {callback_url}")
        response = requests.post(callback_url, json=payload, headers=headers, timeout=5)
        print(f"[RPA Task] Callback response: HTTP {response.status_code}")
    except Exception as exc:
        print(f"[RPA Task] Failed to send callback to backend: {str(exc)}")


def handle_error(task_id: str, task_type: str, action_name: str, code: str, message: str, callback_url: str, token: str, screenshot_path: str = None):
    payload = {
        "taskId": task_id,
        "taskType": task_type,
        "actionName": action_name,
        "status": "failed",
        "error": {
            "code": code,
            "message": message
        }
    }
    if screenshot_path:
        filename = os.path.basename(screenshot_path)
        payload["error"]["screenshotPath"] = f"rpa/outputs/screenshots/{filename}"

    send_callback(callback_url, token, payload)


if __name__ == "__main__":
    task_id = sys.argv[1] if len(sys.argv) > 1 else f"task_{int(time.time())}"
    task_type = sys.argv[2] if len(sys.argv) > 2 else "PURCHASE_PRICE"
    action_name = sys.argv[3] if len(sys.argv) > 3 else "collect_purchase_orders"
    run_purchase_order_task(task_id, task_type, action_name)
