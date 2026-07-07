let searchData;

function clearSearchResults() {
    const body = document.getElementsByClassName('search-body')[0];
    if (body) body.innerHTML = '';
}

function loadData(keywords) {
    if (!keywords || keywords.length === 0 || !keywords[0]) {
        clearSearchResults();
        return;
    }

    if (!searchData) {
        const xhr = new XMLHttpRequest();
        xhr.open('GET', '/search.json', true);
        xhr.onload = function () {
            if (this.status >= 200 && this.status < 300) {
                const res = JSON.parse(this.response || this.responseText);
                searchData = Array.isArray(res) ? res : res.posts || [];
                searchkey(keywords);
            } else {
                console.error('Failed to load search data:', this.statusText);
            }
        };
        xhr.onerror = function () {
            console.error('Failed to load search data.');
        };
        xhr.send();
    } else {
        searchkey(keywords);
    }
}

function searchkey(keywords) {
    clearSearchResults();
    const results = [];

    keywords.forEach(word => {
        const keyword = word.trim();
        if (!keyword) return;
        const reg = new RegExp(keyword.replace(/[.*+?^${}()|[\]\\]/g, '\\$&'), 'gi');

        searchData.forEach(post => {
            const title = post.title || '';
            const content = (post.content || '').replace(/<[^>]+>/g, ' ');
            const textpos = content.search(reg);
            const titleMatch = title.search(reg) !== -1;
            const contentMatch = textpos !== -1;

            if (!titleMatch && !contentMatch) return;

            const start = Math.max(0, textpos - 20);
            const excerpt = contentMatch ? content.substring(start, textpos + 80) : content.substring(0, 100);
            results.push({
                title: title.replace(reg, match => `<span class="keyword">${match}</span>`),
                text: excerpt.replace(reg, match => `<span class="keyword">${match}</span>`),
                href: post.url || post.path || '#'
            });
        });
    });

    results.slice(0, 12).forEach(render);
}

function render(data) {
    const body = document.getElementsByClassName('search-body')[0];
    if (!body) return;
    const ele = document.createElement('div');
    ele.className = 'search-result';
    ele.innerHTML = `<a href="${data.href}"><div class="search-result-title">${data.title}</div><div class="search-result-text">${data.text}</div></a>`;
    body.appendChild(ele);
}

const key = decodeURI(location.search.split('?q=')[1]);
if (key !== undefined && key !== 'undefined') {
    const input = document.getElementsByClassName('search-input')[0];
    if (input) input.value = key;
    loadData(format(key));
    const searchSpace = document.getElementById('nexmoe-search-space');
    if (searchSpace) searchSpace.style.display = 'flex';
}

function sclose() {
    const searchSpace = document.getElementById('nexmoe-search-space');
    if (searchSpace) searchSpace.style.display = 'none';
}

function sinput() {
    const input = document.getElementsByClassName('search-input')[0];
    clearSearchResults();
    if (input) loadData(format(input.value));
}

function format(word) {
    return word.trim().split(/\s+/).filter(Boolean);
}
