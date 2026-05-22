import os
import sys
import json
import time
import requests
from dotenv import load_dotenv

# 상위 디렉토리에서 라이브러리 임포트 가능하도록 경로 추가
sys.path.append(os.path.join(os.path.dirname(__file__), ".."))
from engine.playwright_engine import PlaywrightEngine

# 루트 .env 로드
dotenv_path = os.path.join(os.path.dirname(__file__), "..", "..", ".env")
load_dotenv(dotenv_path)

def run_purchase_order_task(task_id: str):
    print(f"[RPA Task] Starting purchase order collection. Task ID: {task_id}")
    
    # 1. 환경변수 검증
    base_url = os.getenv("RPA_BASE_URL", "http://localhost:5500")
    vendor_username = os.getenv("RPA_VENDOR_USERNAME")
    vendor_password = os.getenv("RPA_VENDOR_PASSWORD")
    callback_token = os.getenv("RPA_CALLBACK_TOKEN")
    
    # 공통 백엔드 콜백 웹훅 URL 획득 (rpa/app.py 와 동일한 해석 규칙 적용)
    backend_callback_url = os.getenv("BACKEND_CALLBACK_URL")
    if not backend_callback_url:
        backend_base_url = os.getenv("BACKEND_API_BASE_URL", "http://localhost:8080/api/v1")
        backend_callback_url = f"{backend_base_url}/callbacks/rpa"
    
    if not vendor_username or not vendor_password:
        handle_error(task_id, "RPA_CONFIGURATION_ERROR", "RPA Vendor credentials are missing in .env", backend_callback_url, callback_token)
        return

    # 출력 디렉토리 확보
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
            # 2. 브라우저 컨텍스트 기동
            context = engine.get_context()
            page = context.new_page()
            
            # 3. 모의 거래처 로그인 페이지 접속
            login_url = f"{base_url}/login"
            print(f"[RPA Task] Navigating to {login_url}")
            page.goto(login_url)
            
            # 4. 로그인 수행
            page.fill("#username", vendor_username)
            page.fill("#password", vendor_password)
            page.click("#login-btn")
            
            # 로그인 후 특정 헤더나 요소가 보일 때까지 대기
            page.wait_for_selector("#order-list-table", timeout=10000)
            print("[RPA Task] Login success. Scraping orders...")

            # 5. 주문 테이블 데이터 스크래핑
            rows = page.query_selector_all("#order-list-table tbody tr")
            orders = []
            
            for row in rows:
                cols = row.query_selector_all("td")
                if len(cols) >= 4:
                    order_data = {
                        "orderNo": cols[0].inner_text().strip(),
                        "productName": cols[1].inner_text().strip(),
                        "quantity": int(cols[2].inner_text().strip().replace(",", "")),
                        "vendorName": cols[3].inner_text().strip()
                    }
                    orders.append(order_data)
                    
            # 6. JSON 파일로 결과 저장
            result_path = os.path.join(output_dir, f"orders_{task_id}.json")
            with open(result_path, "w", encoding="utf-8") as f:
                json.dump(orders, f, ensure_ascii=False, indent=2)
                
            print(f"[RPA Task] Data collection complete. Saved to: {result_path}")
            
            # 7. 성공 콜백 전송
            send_callback(backend_callback_url, callback_token, {
                "taskId": task_id,
                "status": "success",
                "data": {
                    "ordersCount": len(orders),
                    "filePath": f"rpa/outputs/orders_{task_id}.json"
                }
            })
            
            # 자원 정리 후 정상 종료
            if context:
                context.close()
            return
            
        except Exception as e:
            last_exception = e
            print(f"[RPA Task] Attempt {attempt} failed: {str(e)}")
            
            # 에러 발생 시 시도별 스크린샷 저장
            screenshot_path = os.path.join(screenshot_dir, f"err_{task_id}_attempt_{attempt}.png")
            last_screenshot_path = screenshot_path
            if page:
                try:
                    page.screenshot(path=screenshot_path)
                    print(f"[RPA Task] Error screenshot saved: {screenshot_path}")
                except Exception as se:
                    print(f"[RPA Task] Failed to take screenshot: {str(se)}")
                    last_screenshot_path = None
            
            # 리소스 해제 후 브라우저 인스턴스 전면 초기화
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
                
    # 3회 시도 모두 실패 시 최종 에러 콜백 전송
    error_msg = str(last_exception) if last_exception else "RPA 스크래핑 시도 횟수가 초과되었습니다."
    handle_error(task_id, "RPA_EXECUTION_FAILED", error_msg, backend_callback_url, callback_token, last_screenshot_path)
        # 싱글톤 브라우저는 기동 상태를 유지하고 컨텍스트만 클로즈함

def send_callback(callback_url: str, token: str, payload: dict):
    headers = {
        "Content-Type": "application/json",
        "X-RPA-Token": token if token else ""
    }
    try:
        print(f"[RPA Task] Sending callback to {callback_url}")
        res = requests.post(callback_url, json=payload, headers=headers, timeout=5)
        print(f"[RPA Task] Callback response: HTTP {res.status_code}")
    except Exception as e:
        print(f"[RPA Task] Failed to send callback to backend: {str(e)}")

def handle_error(task_id: str, code: str, msg: str, callback_url: str, token: str, screenshot_path: str = None):
    payload = {
        "taskId": task_id,
        "status": "failed",
        "error": {
            "code": code,
            "message": msg
        }
    }
    if screenshot_path:
        # 실제 마지막에 성공적으로 저장된 스크린샷 물리 파일명 획득 (상대 경로로 변환)
        filename = os.path.basename(screenshot_path)
        payload["error"]["screenshotPath"] = f"rpa/outputs/screenshots/{filename}"
        
    send_callback(callback_url, token, payload)

if __name__ == "__main__":
    # 실행 시 taskId를 명령줄 파라미터로 입력 받음. 없으면 기본값 부여
    tid = sys.argv[1] if len(sys.argv) > 1 else f"task_{int(time.time())}"
    run_purchase_order_task(tid)
