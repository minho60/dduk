import os
import threading

from playwright.sync_api import sync_playwright


class PlaywrightEngine:
    def __init__(self):
        self.browser_type = os.getenv("RPA_BROWSER", "chromium")
        self.headless = os.getenv("RPA_HEADLESS", "true").lower() == "true"
        self.timeout = int(os.getenv("PLAYWRIGHT_TIMEOUT_MS", "30000"))
        self._lock = threading.RLock()
        self._playwright_manager = None
        self.playwright = None
        self.browser = None

    def start(self):
        with self._lock:
            if self.browser is not None and self.playwright is not None:
                return

            self.shutdown()
            self._playwright_manager = sync_playwright()
            self.playwright = self._playwright_manager.start()

            if self.browser_type == "firefox":
                launch_fn = self.playwright.firefox.launch
            elif self.browser_type == "webkit":
                launch_fn = self.playwright.webkit.launch
            else:
                launch_fn = self.playwright.chromium.launch

            launch_args = [
                "--disable-dev-shm-usage",
                "--disable-gpu",
                "--disable-extensions",
                "--disable-background-networking",
                "--disable-sync",
                "--no-first-run",
                "--no-default-browser-check",
                "--mute-audio",
            ]
            if os.getenv("RPA_BROWSER_NO_SANDBOX", "true").lower() == "true":
                launch_args.append("--no-sandbox")

            self.browser = launch_fn(headless=self.headless, args=launch_args)
            print(f"[RPA Engine] Browser ({self.browser_type}) started. Headless: {self.headless}")

    def get_context(self):
        self.start()
        context = self.browser.new_context(
            viewport={"width": 1280, "height": 720},
            java_script_enabled=True,
            ignore_https_errors=True,
        )
        context.set_default_timeout(self.timeout)
        return context

    def shutdown(self):
        with self._lock:
            if self.browser is not None:
                try:
                    self.browser.close()
                except Exception:
                    pass
                self.browser = None

            if self._playwright_manager is not None:
                try:
                    self._playwright_manager.stop()
                except Exception:
                    pass
                self._playwright_manager = None

            self.playwright = None
