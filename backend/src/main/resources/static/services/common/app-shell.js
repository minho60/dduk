(function () {
    function hasCurrentChild(item) {
        return Boolean(item.children && item.children.some(function (child) {
            return child.current || hasCurrentChild(child);
        }));
    }

    function renderNavLink(item, depth) {
        const isCurrent = item.current ? ' aria-current="page"' : "";
        const sizeClass = depth > 0 ? "rounded-xl py-2.5 text-xs" : "rounded-2xl py-3 text-sm";
        const indentClass = depth > 0 ? "ml-3" : "";
        return `
            <a href="${item.href || "#"}"${isCurrent} class="group flex items-center justify-between px-4 font-bold text-slate-500 transition hover:bg-slate-50 hover:text-brand-600 aria-[current=page]:bg-brand-50 aria-[current=page]:text-brand-600 ${sizeClass} ${indentClass}">
                <span>${item.label}</span>
                <span class="h-2 w-2 rounded-full bg-transparent group-aria-[current=page]:bg-brand-500"></span>
            </a>
        `;
    }

    function renderAccordion(item, path, depth) {
        const isOpen = item.open || hasCurrentChild(item);
        const panelId = `nav-accordion-${path}`;
        const sizeClass = depth > 0 ? "rounded-xl py-2.5 text-xs" : "rounded-2xl py-3 text-sm";
        const indentClass = depth > 0 ? "ml-3" : "";
        const children = (item.children || []).map(function (child, childIndex) {
            return renderNavItem(child, `${path}-${childIndex}`, depth + 1);
        }).join("");

        return `
            <div class="rounded-2xl">
                <button type="button" class="flex w-full items-center justify-between px-4 font-bold text-slate-700 transition hover:bg-slate-50 hover:text-brand-600 ${sizeClass} ${indentClass}" data-accordion-trigger="${panelId}" aria-expanded="${isOpen}" aria-controls="${panelId}">
                    <span>${item.label}</span>
                    <span class="text-xs transition ${isOpen ? "rotate-180" : ""}" data-accordion-icon>⌄</span>
                </button>
                <div id="${panelId}" class="mt-1 space-y-1 ${isOpen ? "" : "hidden"}" data-accordion-panel>
                    ${children}
                </div>
            </div>
        `;
    }

    function renderNavItem(item, path, depth) {
        if (item.children && item.children.length) {
            return renderAccordion(item, path, depth);
        }
        return renderNavLink(item, depth);
    }

    function renderNav(items) {
        return items.map(function (item, index) {
            return renderNavItem(item, String(index), 0);
        }).join("");
    }

    function bindAccordions(navElement) {
        navElement.querySelectorAll("[data-accordion-trigger]").forEach(function (trigger) {
            trigger.addEventListener("click", function () {
                const panel = document.getElementById(trigger.dataset.accordionTrigger);
                const icon = trigger.querySelector("[data-accordion-icon]");
                const isOpen = trigger.getAttribute("aria-expanded") === "true";
                trigger.setAttribute("aria-expanded", String(!isOpen));
                panel.classList.toggle("hidden", isOpen);
                if (icon) {
                    icon.classList.toggle("rotate-180", !isOpen);
                }
            });
        });
    }

    function renderCards(items) {
        return items.map(function (item) {
            return `
                <article class="rounded-2xl border border-slate-200 bg-white p-5 shadow-sm">
                    <p class="text-sm font-bold text-slate-500">${item.title}</p>
                    <strong class="mt-2 block text-2xl font-black tracking-tight text-slate-950">${item.value || item.description || "-"}</strong>
                    ${item.description && item.value ? `<span class="mt-2 block text-xs font-semibold text-slate-400">${item.description}</span>` : ""}
                </article>
            `;
        }).join("");
    }

    function hydratePage(config) {
        const navElement = document.querySelector("[data-nav]");
        const titleElement = document.querySelector("[data-page-title]");
        const descriptionElement = document.querySelector("[data-page-description]");
        const cardsElement = document.querySelector("[data-summary-cards]");

        if (navElement) {
            navElement.innerHTML = renderNav(config.navItems || []);
            bindAccordions(navElement);
        }

        if (titleElement) {
            titleElement.textContent = config.title || "";
        }

        if (descriptionElement) {
            descriptionElement.textContent = config.description || "";
        }

        if (cardsElement) {
            cardsElement.innerHTML = renderCards(config.cards || []);
        }
    }

    window.ddukAppShell = {
        hydratePage
    };
})();
