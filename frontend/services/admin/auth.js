const API_BASE_URL = window.location.hostname === "localhost"
    ? "http://localhost:8080"
    : "";

const loginForm = document.getElementById("loginForm");
const loginButton = document.getElementById("loginBtn");
const loginButtonText = loginButton.querySelector("span");
const messageElement = document.getElementById("message");
const passwordInput = document.getElementById("password");
const passwordToggle = document.querySelector(".password-toggle");

const roleRedirectMap = {
    ADMIN: "dashboard.html",
    HR: "dashboard.html",
    INVENTORY: "dashboard.html"
};

function setMessage(text, type) {
    messageElement.textContent = text;
    messageElement.className = `message ${type}`;
}

function setLoginButtonText(text) {
    if (loginButtonText) {
        loginButtonText.textContent = text;
        return;
    }

    loginButton.textContent = text;
}

function storeSession(data) {
    localStorage.setItem("token", data.token);
    localStorage.setItem("loginId", data.loginId);
    localStorage.setItem("role", data.role);
    localStorage.setItem("userName", data.name);
}

function redirectIfSessionExists() {
    const token = localStorage.getItem("token");
    const role = localStorage.getItem("role");

    if (!token || !role) {
        return;
    }

    const redirectPath = roleRedirectMap[role];
    if (redirectPath) {
        window.location.href = redirectPath;
    }
}

function initPasswordToggle() {
    if (!passwordToggle || !passwordInput) {
        return;
    }

    passwordToggle.addEventListener("click", () => {
        const isVisible = passwordInput.type === "text";

        passwordInput.type = isVisible ? "password" : "text";
        passwordToggle.setAttribute("aria-pressed", String(!isVisible));
        passwordToggle.setAttribute("aria-label", isVisible ? "비밀번호 보기" : "비밀번호 숨기기");
    });
}

redirectIfSessionExists();
initPasswordToggle();

loginForm.addEventListener("submit", async (event) => {
    event.preventDefault();

    const loginId = document.getElementById("loginId").value.trim();
    const password = passwordInput.value;
    let loginSucceeded = false;

    setMessage("", "");
    loginButton.disabled = true;
    loginButton.classList.add("is-loading");
    setLoginButtonText("Authenticating...");

    try {
        const response = await fetch(`${API_BASE_URL}/api/v1/auth/login`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json"
            },
            body: JSON.stringify({ loginId, password })
        });

        const data = await response.json();

        if (!response.ok) {
            setMessage(data.message || "로그인에 실패했습니다.", "error");
            return;
        }

        storeSession(data);
        loginSucceeded = true;
        setMessage("로그인에 성공했습니다. 페이지로 이동합니다.", "success");
        setLoginButtonText("Access Granted");

        const redirectPath = roleRedirectMap[data.role] || roleRedirectMap.INVENTORY;
        window.setTimeout(() => {
            window.location.href = redirectPath;
        }, 300);
    } catch (error) {
        console.error("로그인 중 오류 발생:", error);
        setMessage("서버와 통신할 수 없습니다.", "error");
    } finally {
        if (loginSucceeded) {
            loginButton.classList.remove("is-loading");
            return;
        }

        loginButton.disabled = false;
        loginButton.classList.remove("is-loading");
        setLoginButtonText("로그인");
    }
});
