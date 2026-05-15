const memberForm = document.getElementById("memberCreateForm");
const memberMessage = document.getElementById("memberMessage");
const memberTableBody = document.getElementById("memberTableBody");

function setMemberMessage(text, type) {
    memberMessage.textContent = text;
    memberMessage.className = `message ${type || ""}`.trim();
}

async function requestMemberApi(path, options) {
    const response = await fetch(`${window.ddukSession.getApiBaseUrl()}${path}`, {
        ...options,
        headers: window.ddukSession.getAuthHeaders({
            "Content-Type": "application/json",
            ...(options && options.headers ? options.headers : {})
        })
    });

    const text = await response.text();
    const data = text ? JSON.parse(text) : null;

    if (!response.ok) {
        throw new Error(data && data.message ? data.message : "요청 처리에 실패했습니다.");
    }

    return data;
}

function formatDateTime(value) {
    if (!value) {
        return "-";
    }

    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
        return value;
    }

    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")} ${String(date.getHours()).padStart(2, "0")}:${String(date.getMinutes()).padStart(2, "0")}`;
}

function createRoleOptions(currentRole) {
    return ["ADMIN", "HR", "INVENTORY"].map(function (role) {
        const selected = role === currentRole ? " selected" : "";
        return `<option value="${role}"${selected}>${role}</option>`;
    }).join("");
}

function renderMembers(members) {
    memberTableBody.innerHTML = members.map(function (member) {
        const activeLabel = member.active ? "활성" : "비활성";
        const toggleLabel = member.active ? "비활성화" : "활성화";

        return `
            <tr>
                <td>${member.id}</td>
                <td>${member.loginId}</td>
                <td>${member.name}</td>
                <td>
                    <select data-role-select="${member.id}">
                        ${createRoleOptions(member.role)}
                    </select>
                </td>
                <td>${activeLabel}</td>
                <td>${formatDateTime(member.createdAt)}</td>
                <td class="actions">
                    <button type="button" data-action="update-role" data-member-id="${member.id}">권한 변경</button>
                    <button type="button" data-action="toggle-status" data-member-id="${member.id}" data-active="${member.active}">${toggleLabel}</button>
                </td>
            </tr>
        `;
    }).join("");
}

async function loadMembers() {
    const members = await requestMemberApi("/api/v1/admin/members", {
        method: "GET"
    });
    renderMembers(members);
}

memberForm.addEventListener("submit", async function (event) {
    event.preventDefault();

    const formData = new FormData(memberForm);
    const payload = {
        loginId: formData.get("loginId"),
        password: formData.get("password"),
        name: formData.get("name"),
        role: formData.get("role")
    };

    try {
        await requestMemberApi("/api/v1/admin/members", {
            method: "POST",
            body: JSON.stringify(payload)
        });

        memberForm.reset();
        setMemberMessage("계정을 생성했습니다.", "success");
        await loadMembers();
    } catch (error) {
        setMemberMessage(error.message, "error");
    }
});

memberTableBody.addEventListener("click", async function (event) {
    const button = event.target.closest("button");
    if (!button) {
        return;
    }

    const memberId = button.dataset.memberId;

    try {
        if (button.dataset.action === "update-role") {
            const roleSelect = document.querySelector(`[data-role-select="${memberId}"]`);

            await requestMemberApi(`/api/v1/admin/members/${memberId}/role`, {
                method: "PATCH",
                body: JSON.stringify({ role: roleSelect.value })
            });

            setMemberMessage("권한을 변경했습니다.", "success");
        }

        if (button.dataset.action === "toggle-status") {
            const nextActive = button.dataset.active !== "true";

            await requestMemberApi(`/api/v1/admin/members/${memberId}/status`, {
                method: "PATCH",
                body: JSON.stringify({ active: nextActive })
            });

            setMemberMessage("계정 상태를 변경했습니다.", "success");
        }

        await loadMembers();
    } catch (error) {
        setMemberMessage(error.message, "error");
    }
});

loadMembers().catch(function (error) {
    setMemberMessage(error.message, "error");
});
