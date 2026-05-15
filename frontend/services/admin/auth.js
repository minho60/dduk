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

function loadSavedValues() {
    const saveCompanyCode = document.querySelector('[name="saveCompanyCode"]');
    const savedCompanyCode = localStorage.getItem('savedCompanyCode');
    if (savedCompanyCode) {
        document.getElementById('companyCode').value = savedCompanyCode;
        if (saveCompanyCode) saveCompanyCode.checked = true;
    }

    const rememberId = document.querySelector('[name="rememberId"]');
    const savedLoginId = localStorage.getItem('savedLoginId');
    if (savedLoginId) {
        document.getElementById('loginId').value = savedLoginId;
        if (rememberId) rememberId.checked = true;
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

loadSavedValues();
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

        // 회사코드 저장
        const saveCompanyCodeCheckbox = document.querySelector('[name="saveCompanyCode"]');
        const companyCodeVal = document.getElementById('companyCode').value.trim();
        if (saveCompanyCodeCheckbox && saveCompanyCodeCheckbox.checked) {
            localStorage.setItem('savedCompanyCode', companyCodeVal);
        } else {
            localStorage.removeItem('savedCompanyCode');
        }

        // 아이디 저장
        const rememberIdCheckbox = document.querySelector('[name="rememberId"]');
        if (rememberIdCheckbox && rememberIdCheckbox.checked) {
            localStorage.setItem('savedLoginId', loginId);
        } else {
            localStorage.removeItem('savedLoginId');
        }

        loginSucceeded = true;
        setMessage("로그인에 성공했습니다. 페이지로 이동합니다.", "success");
        setLoginButtonText("Access Granted");

        const redirectPath = "dashboard.html";
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
