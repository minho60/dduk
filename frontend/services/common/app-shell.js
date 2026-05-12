(function () {
    function renderNav(items) {
        return items.map(function (item) {
            const isCurrent = item.current ? ' aria-current="page"' : "";
            return `<a href="${item.href}"${isCurrent}>${item.label}</a>`;
        }).join("");
    }

    function renderCards(items) {
        return items.map(function (item) {
            return `
                <article class="panel">
                    <h3>${item.title}</h3>
                    <p>${item.description}</p>
                </article>
            `;
        }).join("");
    }

    function renderList(items) {
        return items.map(function (item) {
            return `<li>${item}</li>`;
        }).join("");
    }

    function hydratePage(config) {
        const navElement = document.querySelector("[data-nav]");
        const titleElement = document.querySelector("[data-page-title]");
        const descriptionElement = document.querySelector("[data-page-description]");
        const cardsElement = document.querySelector("[data-summary-cards]");
        const todoElement = document.querySelector("[data-todo-list]");
        const tableElement = document.querySelector("[data-table-list]");
        const apiElement = document.querySelector("[data-api-list]");

        if (navElement) {
            navElement.innerHTML = renderNav(config.navItems || []);
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

        if (todoElement) {
            todoElement.innerHTML = renderList(config.todoItems || []);
        }

        if (tableElement) {
            tableElement.innerHTML = renderList(config.tables || []);
        }

        if (apiElement) {
            apiElement.innerHTML = renderList(config.apis || []);
        }
    }

    window.ddukAppShell = {
        hydratePage
    };
})();
