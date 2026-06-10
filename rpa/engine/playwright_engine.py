import os
import threading

from playwright.sync_api import sync_playwright


class PlaywrightEngine:
    _instance = None
    _lock = threading.Lock()

    def __new__(cls, *args, **kwargs):
        with cls._lock:
            if cls._instance is None:
                cls._instance = super().__new__(cls)
                cls._instance._initialized = False
            return cls._instance

    def __init__(self):
        if self._initialized:
            return

        self.browser_type = os.getenv("RPA_BROWSER", "chromium")
        self.headless = os.getenv("RPA_HEADLESS", "true").lower() == "true"
        self.timeout = int(os.getenv("PLAYWRIGHT_TIMEOUT_MS", "30000"))
        self.playwright = None
        self.browser = None
        self._initialized = True

    def start(self):
        if self.playwright:
            return

        self.playwright = sync_playwright().start()
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
        if self.browser:
            self.browser.close()
            self.browser = None
        if self.playwright:
            self.playwright.stop()
            self.playwright = None
        print("[RPA Engine] Browser shut down successfully.")
