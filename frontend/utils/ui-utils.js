/**
 * UI Utilities for Notifications and Loading States
 */
export const UIUtils = {
    /**
     * Show a simple toast notification
     */
    showToast: (message, type = 'info') => {
        console.log(`[Toast][${type.toUpperCase()}] ${message}`);
        
        // In a real browser env, this would create a DOM element
        const toastId = 'global-toast-' + Date.now();
        const toast = document.createElement('div');
        toast.id = toastId;
        toast.className = `toast toast-${type}`;
        toast.innerHTML = `
            <div class="toast-content">
                <span class="toast-icon">${type === 'error' ? '❌' : '✅'}</span>
                <span class="toast-message">${message}</span>
            </div>
        `;
        
        document.body.appendChild(toast);
        
        // Auto-remove after 3 seconds
        setTimeout(() => {
            const el = document.getElementById(toastId);
            if (el) el.remove();
        }, 3000);
    },

    /**
     * Set loading state for a button
     */
    setLoading: (buttonId, isLoading) => {
        const btn = document.getElementById(buttonId);
        if (!btn) return;

        if (isLoading) {
            btn.disabled = true;
            btn.dataset.originalText = btn.innerText;
            btn.innerText = '처리 중...';
        } else {
            btn.disabled = false;
            btn.innerText = btn.dataset.originalText || btn.innerText;
        }
    }
};
