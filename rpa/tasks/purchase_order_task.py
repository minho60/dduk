import json
import os
import re
import sys
import time
from collections import deque
from urllib.parse import urlparse, urlunparse

import requests
from dotenv import load_dotenv

sys.path.append(os.path.join(os.path.dirname(__file__), ".."))
from engine.playwright_engine import PlaywrightEngine

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
if hasattr(sys.stderr, "reconfigure"):
    sys.stderr.reconfigure(encoding="utf-8", errors="replace")

dotenv_path = os.path.join(os.path.dirname(__file__), "..", "..", ".env")
load_dotenv(dotenv_path)

AMANTEA_VENDOR_NAME = "아망티"
DEFAULT_CATALOG_URL = "https://www.amantea.co.kr/"
DEFAULT_DISCOVERY_URLS = [
    "https://www.amantea.co.kr/",
    "https://www.amantea.co.kr/product/list.html?cate_no=42",
]
DEFAULT_PRODUCT_URLS = [
    "https://www.amantea.co.kr/product/detail.html?product_no=426",
    "https://www.amantea.co.kr/product/detail.html?product_no=614",
    "https://www.amantea.co.kr/product/detail.html?product_no=1122",
]
DEFAULT_MAX_PRODUCTS = 6
DEFAULT_TIMEOUT_MS = 10000
DEFAULT_DELAY_SECONDS = 0.1
DEFAULT_DISCOVERY_LIMIT = 16
DEFAULT_MAX_RETRIES = 1


def safe_int(value: str | None, default: int) -> int:
    try:
        return int(str(value).strip())
    except (TypeError, ValueError):
        return default


def safe_float(value: str | None, default: float) -> float:
    try:
        return float(str(value).strip())
    except (TypeError, ValueError):
        return default


def canonicalize_product_url(raw_url: str) -> str | None:
    if not raw_url:
        return None

    parsed = urlparse(raw_url.strip())
    hostname = (parsed.hostname or "").lower()
    path = parsed.path or ""
    if not hostname.endswith("amantea.co.kr"):
        return None

    if not (
        path.endswith("/product/detail.html")
        or path.endswith("/shop/product/product_view")
        or "/product/list.html" in path
        or bool(re.search(r"/product/.+/\d+/?$", path))
    ):
        return None

    return urlunparse(parsed._replace(fragment=""))


def parse_env_list(name: str, fallback: list[str]) -> list[str]:
    raw_value = os.getenv(name, "").strip()
    if not raw_value:
        return fallback
    return [part.strip() for part in raw_value.split(",") if part.strip()]


def wait_for_page_ready(page, timeout_ms: int):
    try:
        page.wait_for_load_state("domcontentloaded", timeout=timeout_ms)
        page.wait_for_timeout(400)
    except Exception:
        page.wait_for_timeout(600)


def configure_lightweight_page(page):
    page.route(
        "**/*",
        lambda route: route.abort()
        if route.request.resource_type in {"image", "media", "font", "stylesheet"}
        else route.continue_(),
    )


def collect_product_links(page, limit: int) -> list[str]:
    discovered = page.evaluate(
        """
        () => Array.from(document.querySelectorAll("a[href]"))
            .map((anchor) => anchor.href)
            .filter(Boolean)
        """
    )

    links = []
    seen = set()
    for raw_url in discovered:
        normalized = canonicalize_product_url(raw_url)
        if not normalized or normalized in seen:
            continue
        seen.add(normalized)
        links.append(normalized)
        if len(links) >= limit:
            break
    return links


def extract_product_code(product_url: str, index: int) -> str:
    parsed = urlparse(product_url)
    query = parsed.query or ""
    for key in ("product_no", "product_cd", "item_code"):
        match = re.search(rf"(?:^|[?&]){key}=([^&]+)", query)
        if match:
            return match.group(1)

    path_match = re.search(r"/(\d+)/?$", parsed.path)
    if path_match:
        return path_match.group(1)

    return f"public-{index:03d}"


def extract_shipping_fee_number(raw_text: str) -> int | None:
    match = re.search(r"([0-9,]+)", raw_text or "")
    if not match:
        return None
    return int(match.group(1).replace(",", ""))


def parse_product_detail(page, product_url: str, index: int, timeout_ms: int) -> tuple[dict, list[str]]:
    page.goto(product_url, wait_until="domcontentloaded", timeout=timeout_ms)
    wait_for_page_ready(page, timeout_ms)

    payload = page.evaluate(
        """
        () => {
            const normalize = (value) => (value || "").replace(/\\s+/g, " ").trim();
            const bodyTextRaw = document.body ? document.body.innerText : "";
            const bodyText = normalize(bodyTextRaw);
            const bodyLines = bodyTextRaw
                .split("\\n")
                .map((line) => normalize(line))
                .filter(Boolean);

            const title = [
                document.querySelector(".headingArea h2")?.textContent,
                document.querySelector(".prdName")?.textContent,
                document.querySelector('meta[property="og:title"]')?.content,
                document.title
            ].map(normalize).find(Boolean) || "";

            const priceMatch = bodyText.match(/([0-9,]{3,})\\s*원/);
            const optionTexts = Array.from(document.querySelectorAll("select option"))
                .map((option) => normalize(option.textContent))
                .filter((text) => text && !text.includes("옵션"));
            const shippingText = bodyLines.find((line) => line.includes("배송")) || "";

            return {
                title,
                priceText: priceMatch ? priceMatch[1] : "",
                optionSummary: optionTexts.slice(0, 4).join(", "),
                shippingFeeText: shippingText,
                categoryTrail: bodyLines.slice(0, 8).join(" > "),
                soldOut: /SOLD OUT|품절/.test(bodyText),
            };
        }
        """
    )

    product_name = str(payload.get("title") or "").strip()
    if not product_name:
        raise RuntimeError(f"상품명을 찾지 못했어: {product_url}")

    price_digits = re.sub(r"[^0-9]", "", str(payload.get("priceText") or ""))
    if not price_digits:
        raise RuntimeError(f"판매가를 찾지 못했어: {product_url}")

    order = {
        "orderNo": f"AMANTEA-{extract_product_code(product_url, index)}",
        "productCode": extract_product_code(product_url, index),
        "productName": product_name,
        "quantity": 1,
        "unitPrice": int(price_digits),
        "spec": str(payload.get("optionSummary") or "")[:140] or "공개 옵션 정보 없음",
        "vendorName": AMANTEA_VENDOR_NAME,
        "productUrl": product_url,
        "soldOut": bool(payload.get("soldOut")),
        "stockStatus": "SOLD_OUT" if payload.get("soldOut") else "AVAILABLE_OR_UNKNOWN",
        "shippingFee": extract_shipping_fee_number(str(payload.get("shippingFeeText") or "")),
        "category": str(payload.get("categoryTrail") or "").strip() or None,
        "reviewCount": 0,
        "manufacturer": None,
        "countryOfOrigin": None,
        "priceCandidates": [str(payload.get("priceText") or "")],
    }
    return order, []


def discover_initial_product_urls(context, catalog_url: str, max_products: int, timeout_ms: int) -> list[str]:
    seeded = []
    seen = set()

    for raw_url in parse_env_list("RPA_AMANTEA_PRODUCT_URLS", DEFAULT_PRODUCT_URLS):
        normalized = canonicalize_product_url(raw_url)
        if normalized and normalized not in seen:
            seen.add(normalized)
            seeded.append(normalized)

    for discovery_url in parse_env_list("RPA_AMANTEA_DISCOVERY_URLS", DEFAULT_DISCOVERY_URLS):
        if len(seeded) >= max_products:
            break
        page = context.new_page()
        configure_lightweight_page(page)
        try:
            page.goto(discovery_url or catalog_url, wait_until="domcontentloaded", timeout=timeout_ms)
            wait_for_page_ready(page, timeout_ms)
            for product_url in collect_product_links(page, DEFAULT_DISCOVERY_LIMIT):
                if product_url in seen:
                    continue
                seen.add(product_url)
                seeded.append(product_url)
                if len(seeded) >= max_products:
                    break
        except Exception as discovery_exception:
            print(f"[RPA Task] Discovery page skipped {discovery_url}: {str(discovery_exception)}")
        finally:
            page.close()

    return seeded[:max_products]


def send_callback(callback_url: str, token: str, payload: dict):
    headers = {
        "Content-Type": "application/json",
        "X-RPA-Token": token if token else "",
    }
    try:
        response = requests.post(callback_url, json=payload, headers=headers, timeout=5)
        print(f"[RPA Task] Callback response: HTTP {response.status_code}")
    except Exception as exc:
        print(f"[RPA Task] Failed to send callback to backend: {str(exc)}")


def handle_error(task_id: str, task_type: str, action_name: str, code: str, message: str, callback_url: str, token: str, screenshot_path: str | None = None):
    payload = {
        "taskId": task_id,
        "taskType": task_type,
        "actionName": action_name,
        "status": "failed",
        "error": {
            "code": code,
            "message": message,
        },
    }
    if screenshot_path:
        filename = os.path.basename(screenshot_path)
        payload["error"]["screenshotPath"] = f"rpa/outputs/screenshots/{filename}"
    send_callback(callback_url, token, payload)


def run_purchase_order_task(task_id: str, task_type: str = "PURCHASE_PRICE", action_name: str = "collect_purchase_orders"):
    print(f"[RPA Task] Starting low-footprint Amantea collection for task: {task_id}")

    catalog_url = os.getenv("RPA_AMANTEA_CATALOG_URL", DEFAULT_CATALOG_URL).strip() or DEFAULT_CATALOG_URL
    max_products = max(1, safe_int(os.getenv("RPA_AMANTEA_MAX_PRODUCTS"), DEFAULT_MAX_PRODUCTS))
    timeout_ms = max(4000, safe_int(os.getenv("RPA_PUBLIC_PAGE_TIMEOUT_MS"), DEFAULT_TIMEOUT_MS))
    per_item_delay = max(0.0, safe_float(os.getenv("RPA_PUBLIC_PAGE_DELAY_SECONDS"), DEFAULT_DELAY_SECONDS))
    max_retries = max(1, safe_int(os.getenv("RPA_AMANTEA_MAX_RETRIES"), DEFAULT_MAX_RETRIES))
    callback_token = os.getenv("RPA_CALLBACK_TOKEN")

    backend_callback_url = os.getenv("BACKEND_CALLBACK_URL")
    if not backend_callback_url:
        backend_base_url = os.getenv("BACKEND_API_BASE_URL", "http://localhost:8080/api/v1")
        backend_callback_url = f"{backend_base_url}/callbacks/rpa"

    output_dir = os.path.join(os.path.dirname(__file__), "..", "outputs")
    os.makedirs(output_dir, exist_ok=True)
    screenshot_dir = os.path.join(output_dir, "screenshots")
    os.makedirs(screenshot_dir, exist_ok=True)

    engine = PlaywrightEngine()
    last_exception = None
    last_screenshot_path = None

    for attempt in range(1, max_retries + 1):
        context = None
        last_page = None
        try:
            context = engine.get_context()
            initial_urls = discover_initial_product_urls(context, catalog_url, max_products, timeout_ms)
            if not initial_urls:
                raise RuntimeError("아망티 공개 페이지에서 상품 링크를 찾지 못했어.")

            queue = deque(initial_urls)
            processed = set()
            orders = []
            detail_index = 1

            while queue and len(orders) < max_products:
                product_url = queue.popleft()
                if product_url in processed:
                    continue
                processed.add(product_url)

                detail_page = context.new_page()
                configure_lightweight_page(detail_page)
                last_page = detail_page
                try:
                    order, _ = parse_product_detail(detail_page, product_url, detail_index, timeout_ms)
                    orders.append(order)
                    detail_index += 1
                    print(f"[RPA Task] Collected {order['productCode']} / {order['productName']}")
                except Exception as detail_exception:
                    print(f"[RPA Task] Failed to parse {product_url}: {str(detail_exception)}")
                finally:
                    detail_page.close()

                if per_item_delay > 0:
                    time.sleep(per_item_delay)

            if not orders:
                raise RuntimeError("공개 상품 페이지는 열렸지만 비교에 필요한 단가 데이터를 추출하지 못했어.")

            result_path = os.path.join(output_dir, f"orders_{task_id}.json")
            with open(result_path, "w", encoding="utf-8") as file:
                json.dump(orders, file, ensure_ascii=False, indent=2)

            send_callback(backend_callback_url, callback_token, {
                "taskId": task_id,
                "taskType": task_type,
                "actionName": action_name,
                "status": "success",
                "data": {
                    "ordersCount": len(orders),
                    "filePath": f"rpa/outputs/orders_{task_id}.json",
                },
            })
            if context:
                context.close()
            engine.shutdown()
            return
        except Exception as exc:
            last_exception = exc
            print(f"[RPA Task] Attempt {attempt} failed: {str(exc)}")
            if last_page:
                screenshot_path = os.path.join(screenshot_dir, f"err_{task_id}_attempt_{attempt}.png")
                try:
                    last_page.screenshot(path=screenshot_path)
                    last_screenshot_path = screenshot_path
                except Exception:
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

    handle_error(
        task_id,
        task_type,
        action_name,
        "RPA_EXECUTION_FAILED",
        str(last_exception) if last_exception else "아망티 공개 상품 수집 재시도 횟수를 초과했어.",
        backend_callback_url,
        callback_token,
        last_screenshot_path,
    )


if __name__ == "__main__":
    task_id = sys.argv[1] if len(sys.argv) > 1 else f"task_{int(time.time())}"
    task_type = sys.argv[2] if len(sys.argv) > 2 else "PURCHASE_PRICE"
    action_name = sys.argv[3] if len(sys.argv) > 3 else "collect_purchase_orders"
    run_purchase_order_task(task_id, task_type, action_name)
