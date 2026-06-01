(function () {
    const shared = window.OcrDocumentsShared;
    if (!shared) {
        throw new Error('OcrDocumentsShared is required before ocr-upload-page.js');
    }

    const { requestApi, setMessage } = shared;

function initUploadPage() {
        const form = document.getElementById("ocrUploadForm");
        if (!form) {
            return;
        }

        const message = document.getElementById("uploadMessage");
        const submitButton = document.getElementById("uploadSubmitBtn");
        const resultBox = document.getElementById("uploadResult");

        if (!window.ddukSession.requireRole(["ADMIN", "HR", "INVENTORY"])) {
            return;
        }

        form.addEventListener("submit", async function (event) {
            event.preventDefault();

            const fileInput = document.getElementById("ocrFile");
            const documentType = document.getElementById("documentType").value;

            if (!fileInput.files || fileInput.files.length === 0) {
                setMessage(message, "?낅줈?쒗븷 ?뚯씪???좏깮??", "error");
                return;
            }

            const formData = new FormData();
            formData.append("file", fileInput.files[0]);
            formData.append("documentType", documentType);

            submitButton.disabled = true;
            submitButton.textContent = "?낅줈??以?..";
            setMessage(message, "", "");
            resultBox.classList.add("hidden");

            try {
                const data = await requestApi("/api/v1/ai/ocr/documents", "OCR ?낅줈?쒖뿉 ?ㅽ뙣?덉뼱.", {
                    method: "POST",
                    body: formData
                });
                document.getElementById("resultDocumentId").textContent = data.id;
                document.getElementById("resultFilename").textContent = data.originalFilename;
                document.getElementById("resultStatus").textContent = data.processingStatus;
                resultBox.classList.remove("hidden");
                setMessage(message, data.message || "OCR 臾몄꽌瑜??깅줉?덉뼱.", "success");
                form.reset();
            } catch (error) {
                setMessage(message, error.message, "error");
            } finally {
                submitButton.disabled = false;
                submitButton.textContent = "업로드";
            }
        });
    }

    initUploadPage();
})();