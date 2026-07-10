// tabble gallery
document.querySelectorAll('table:has(img)').forEach(item => {
    item.classList.add('nexmoe-album');
});

// search
function search() {
    window.open(
        document.querySelector('#search_form').getAttribute('action_e')
			+ ' '
			+ document.querySelector('#search_value').value
    );
    return false;
}

// catalog
document.querySelectorAll('a.toc-link').forEach(item => {
    item.addEventListener('click', function(ev) {
        const href = this.getAttribute('href') || '';
        if (!href.startsWith('#')) return;
        const target = document.querySelector(decodeURI(href));
        if (!target) return;
        ev.preventDefault();
        window.scroll({
            top: Math.max(target.offsetTop - 24, 0),
            behavior: 'smooth'
        });
        if (history.pushState) history.pushState(null, '', href);
    });
});
document.addEventListener('copy', () => {
    if (!window.copyTip) {
        return;
    }
    const sel = document.getSelection();
    const ele = document.createElement('div');
    ele.innerHTML
		= '<div style="position: fixed;opacity: 0;white-space: pre;">'
		+ sel
		+ '\n\n'
		+ window.copyTip.replaceAll('%url', document.location.href)
		+ ' </div>';
    document.body.appendChild(ele);
    sel.selectAllChildren(ele);
    setTimeout(() => {
        document.body.removeChild(ele);
    });
});

function imgOnError(_this) {
    _this.src = 'data:image/png;base64,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';
    _this.onerror = null;
}

document
    .querySelectorAll('.nexmoe-post-cover.absolute')
    .forEach(item => {
        item.style.minHeight = item.childNodes[3].clientHeight + 'px';
    });

window.onload = function() {
    lax.init();

    // Add a driver that we use to control our animations
    lax.addDriver('scrollY', () => {
        return window.scrollY;
    });

    // Add animation bindings to elements
    lax.addElements('.backtop', {
        scrollY: {
            opacity: [
                ['screenHeight', 'screenHeight+300', 'screenHeight+600'],
                [0, 0, 1]
            ]
        }
    });

    lax.addElements('.nexmoe-post-cover', {
        scrollY: {
            opacity: [
                ['elInY', 'elInY+200'],
                [0, 1]
            ]
        }
    });
};

(function () {
    const API_BASE = window.BLOG_API_BASE || 'http://127.0.0.1:8080/api';
    let sidebarData = { tags: [], categories: [], articles: [] };
    let editorTags = [];

    function escapeHtml(value) {
        return String(value == null ? '' : value)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
            .replace(/'/g, '&#039;');
    }

    function ensureSidebar() {
        const drawer = document.getElementById('drawer');
        if (!drawer) return null;
        let box = document.getElementById('ciallo-dynamic-sidebar');
        if (box) return box;
        box = document.createElement('div');
        box.id = 'ciallo-dynamic-sidebar';
        const menu = drawer.querySelector('.nexmoe-list');
        if (menu && menu.parentNode) menu.insertAdjacentElement('afterend', box);
        else drawer.appendChild(box);
        return box;
    }

    function yearOf(value) {
        const match = String(value || '').match(/^\d{4}/);
        return match ? match[0] : '未归档';
    }

    function monthOf(value) {
        const match = String(value || '').match(/^\d{4}-(\d{2})/);
        return match ? match[1] : '00';
    }

    function readSidebarUser() {
        try { return JSON.parse(localStorage.getItem('blogUser') || 'null'); }
        catch (error) { return null; }
    }

    function countBy(items, getter) {
        const map = new Map();
        (items || []).forEach(item => {
            const key = getter(item);
            if (!key) return;
            map.set(key, (map.get(key) || 0) + 1);
        });
        return map;
    }

    async function fetchJson(path, useToken) {
        const headers = {};
        const token = localStorage.getItem('blogToken') || '';
        if (useToken && token) headers['X-Token'] = token;
        const response = await fetch(API_BASE + path, { headers: headers });
        const json = await response.json();
        if (!response.ok || !json.success) throw new Error(json.message || '请求失败');
        return json.data || [];
    }

    function renderSidebar() {
        const box = ensureSidebar();
        if (!box) return;
        const tags = sidebarData.tags || [];
        const articles = sidebarData.articles || [];
        const tagCount = new Map();
        const categoryCount = new Map();
        articles.forEach(article => {
            (article.tagNames || []).forEach(name => tagCount.set(name, (tagCount.get(name) || 0) + 1));
            if (article.categoryName) categoryCount.set(article.categoryName, (categoryCount.get(article.categoryName) || 0) + 1);
        });
        const usedTags = Array.from(tagCount.keys()).map(name => {
            const taxonomy = tags.find(tag => String(tag.name).toLowerCase() === String(name).toLowerCase());
            return { name: name, id: taxonomy ? taxonomy.id : '' };
        });
        const writingTags = editorTags
            .filter(name => name && !usedTags.some(tag => String(tag.name).toLowerCase() === String(name).toLowerCase()))
            .map(name => ({ name: name, id: '', writing: true }));
        const shownTags = writingTags.concat(usedTags).slice(0, 28);
        const archiveCount = Array.from(countBy(articles, article => yearOf(article.createdAt) + '-' + monthOf(article.createdAt)).entries())
            .sort((a, b) => String(b[0]).localeCompare(String(a[0])));
        const latest = articles.slice().sort((a, b) => String(b.createdAt || '').localeCompare(String(a.createdAt || ''))).slice(0, 6);
        const articleCountNode = document.getElementById('ciallo-sidebar-article-count');
        const tagCountNode = document.getElementById('ciallo-sidebar-tag-count');
        const categoryCountNode = document.getElementById('ciallo-sidebar-category-count');
        if (articleCountNode) articleCountNode.textContent = articles.length;
        if (tagCountNode) tagCountNode.textContent = tagCount.size;
        if (categoryCountNode) categoryCountNode.textContent = categoryCount.size;

        box.innerHTML = `
          <div class="nexmoe-widget-wrap ciallo-live-widget">
            <h3 class="nexmoe-widget-title">文章标签</h3>
            <div class="ciallo-live-tags">
              ${shownTags.length ? shownTags.map((tag, index) => {
                const label = escapeHtml(tag.name);
                const count = tagCount.get(tag.name) || 0;
                const href = tag.id ? `/archives.html?tagId=${encodeURIComponent(tag.id)}#ciallo-db-archives` : '/archives.html';
                return `<a class="ciallo-live-tag color-${index % 8}${tag.writing ? ' is-writing' : ''}" href="${href}" title="${label}"># ${label}${count ? `<span>${count}</span>` : ''}</a>`;
              }).join('') : '<span class="ciallo-widget-empty">暂无标签</span>'}
            </div>
          </div>
          <div class="nexmoe-widget-wrap ciallo-live-widget">
            <h3 class="nexmoe-widget-title">文章归档</h3>
            <ul class="ciallo-archive-list">
              ${archiveCount.length ? archiveCount.map(item => {
                const parts = String(item[0]).split('-');
                const label = parts[0] + ' 年 ' + parts[1] + ' 月';
                return `<li><a href="/archives.html?year=${encodeURIComponent(parts[0])}&month=${encodeURIComponent(parts[1])}#ciallo-db-archives">${escapeHtml(label)}<span>${item[1]}</span></a></li>`;
              }).join('') : '<li><span class="ciallo-widget-empty">暂无归档</span></li>'}
            </ul>
          </div>
          <div class="nexmoe-widget-wrap ciallo-live-widget">
            <h3 class="nexmoe-widget-title">最新文章</h3>
            <ul class="ciallo-latest-list">
              ${latest.length ? latest.map(article => `<li><a href="/article.html?id=${encodeURIComponent(article.id)}" title="${escapeHtml(article.title)}">${escapeHtml(article.title)}</a></li>`).join('') : '<li><span class="ciallo-widget-empty">暂无上架文章</span></li>'}
            </ul>
          </div>`;
    }

    async function refreshSidebar() {
        try {
            const user = readSidebarUser();
            const articlePath = user ? (user.role === 'ADMIN' ? '/admin/articles' : '/articles/mine') : '/articles';
            const result = await Promise.all([fetchJson('/tags'), fetchJson('/categories'), fetchJson(articlePath, Boolean(user))]);
            sidebarData = { tags: result[0] || [], categories: result[1] || [], articles: result[2] || [] };
            renderSidebar();
        } catch (error) {
            renderSidebar();
        }
    }

    window.refreshCialloSidebar = refreshSidebar;
    window.addEventListener('ciallo:editor-tags', event => {
        editorTags = Array.isArray(event.detail) ? event.detail : [];
        renderSidebar();
    });

    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', refreshSidebar);
    else refreshSidebar();
}());
