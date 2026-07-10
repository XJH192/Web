---
title: 归档
permalink: archives.html
cover: /images/background/zhuti.jpg
coverWidth: 1920
coverHeight: 760
comments: false
reprint: true
---

<section class="ciallo-archive-page" id="ciallo-archive-page">
  <header class="archive-hero-panel">
    <div>
      <p class="archive-kicker">小埋的文章仓库</p>
      <h1 id="archive-timeline">归档时间线</h1>
      <p>这里收纳站点文章和数据库已上架博客。管理员审核通过后，系统归档会自动同步，小伙伴们就能按年份、分类和标签快速找到内容。</p>
    </div>
    <div class="archive-stat-grid" id="archive-stat-grid">
      <span><strong>0</strong>全部文章</span>
      <span><strong>0</strong>分类</span>
      <span><strong>0</strong>标签</span>
    </div>
  </header>

  <section class="archive-tools-panel" aria-label="归档筛选">
    <input id="archive-search" placeholder="搜索标题、作者用户名、分类或标签">
    <select id="archive-year-filter">
      <option value="">全部年份</option>
    </select>
    <select id="archive-month-filter" disabled>
      <option value="">请先选择年份</option>
    </select>
    <a class="archive-tool-link" href="#ciallo-db-archives">查看系统上架博客</a>
  </section>

  <section class="archive-cloud-panel">
    <div>
      <h2 id="archive-category-section">文章分类</h2>
      <div class="archive-category-grid" id="archive-category-grid"></div>
    </div>
    <div>
      <h2 id="archive-tag-section">标签云</h2>
      <div class="archive-tag-cloud" id="archive-tag-cloud"></div>
    </div>
  </section>

  <article class="archive-timeline-panel" id="hexo-archives">
    <div class="archive-section-head">
      <div>
        <h2 id="hexo-archive-section">Hexo 文章归档</h2>
        <p>主题教程、开发记录和本地站点说明会显示在这里。</p>
      </div>
      <span id="archive-filter-status">小埋正在整理文章...</span>
    </div>
    <div id="archive-list"></div>
  </article>

  <section class="archive-timeline-panel db-archive-panel" id="ciallo-db-archives">
    <div class="archive-section-head">
      <div>
        <h2 id="db-archive-section">系统上架博客</h2>
        <p>这里读取 MySQL 中已通过审核的文章，适合演示管理员审核后的动态归档。</p>
      </div>
      <span id="db-archive-status">正在加载...</span>
    </div>
    <div class="archive-tools-panel compact">
      <input id="db-archive-search" placeholder="搜索文章、作者用户名、分类或标签">
      <select id="db-archive-year"><option value="">全部年份</option></select>
      <select id="db-archive-month" disabled><option value="">请先选择年份</option></select>
    </div>
    <div id="db-archive-list" class="db-archive-list"></div>
  </section>
</section>

<script src="/js/archives.js"></script>
<script>
(function () {
  if (window.cialloArchivesV2) return;
  const API_BASE = window.BLOG_API_BASE || 'http://127.0.0.1:8080/api';
  const params = new URLSearchParams(location.search);
  const initialYear = params.get('year') || '';
  const initialDbTagId = params.get('tagId') || '';
  const initialDbCategoryId = params.get('categoryId') || '';

  const nodes = {
    stats: document.getElementById('archive-stat-grid'),
    staticSearch: document.getElementById('archive-search'),
    staticYear: document.getElementById('archive-year-filter'),
    staticStatus: document.getElementById('archive-filter-status'),
    staticList: document.getElementById('archive-list'),
    categories: document.getElementById('archive-category-grid'),
    tags: document.getElementById('archive-tag-cloud'),
    dbList: document.getElementById('db-archive-list'),
    dbStatus: document.getElementById('db-archive-status'),
    dbSearch: document.getElementById('db-archive-search'),
    dbYear: document.getElementById('db-archive-year')
  };

  let staticPosts = [];
  let dbArticles = [];

  function escapeHtml(value) {
    return String(value == null ? '' : value)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }

  function yearOf(value) {
    const match = String(value || '').match(/^\d{4}/);
    return match ? match[0] : '未归档';
  }

  function dateOf(value) {
    return String(value || '').replace('T', ' ').slice(5, 10) || '--';
  }

  function fullDate(value) {
    return String(value || '').replace('T', ' ').slice(0, 10) || '未记录日期';
  }

  function listNames(items) {
    return (items || []).map(function (item) { return item && item.name ? item.name : item; }).filter(Boolean);
  }

  function countBy(items, getValues) {
    const map = new Map();
    items.forEach(function (item) {
      const values = [].concat(getValues(item) || []).filter(Boolean);
      values.forEach(function (value) { map.set(value, (map.get(value) || 0) + 1); });
    });
    return Array.from(map.entries()).sort(function (a, b) { return b[1] - a[1] || String(a[0]).localeCompare(String(b[0]), 'zh-Hans-CN'); });
  }

  function groupByYear(items, getDate) {
    return items.reduce(function (groups, item) {
      const year = yearOf(getDate(item));
      if (!groups[year]) groups[year] = [];
      groups[year].push(item);
      return groups;
    }, {});
  }

  function renderYearOptions(select, years) {
    if (!select) return;
    const current = select.value || initialYear;
    select.innerHTML = '<option value="">全部年份</option>' + years.map(function (year) {
      return '<option value="' + escapeHtml(year) + '">' + escapeHtml(year) + ' 年</option>';
    }).join('');
    if (current && years.indexOf(current) >= 0) select.value = current;
  }

  function renderStaticClouds(posts) {
    const categoryCounts = countBy(posts, function (post) { return listNames(post.categories); });
    const tagCounts = countBy(posts, function (post) { return listNames(post.tags); });
    if (nodes.stats) {
      nodes.stats.innerHTML = [
        '<span><strong>' + posts.length + '</strong>Hexo 文章</span>',
        '<span><strong>' + categoryCounts.length + '</strong>分类</span>',
        '<span><strong>' + tagCounts.length + '</strong>标签</span>'
      ].join('');
    }
    if (nodes.categories) {
      nodes.categories.innerHTML = categoryCounts.length ? categoryCounts.map(function (item) {
        return '<a href="/archives.html?keyword=' + encodeURIComponent(item[0]) + '"><strong>' + escapeHtml(item[0]) + '</strong><span>' + item[1] + ' 篇</span></a>';
      }).join('') : '<p class="archive-empty">还没有分类，小埋先把空盒子摆好。</p>';
    }
    if (nodes.tags) {
      nodes.tags.innerHTML = tagCounts.length ? '<ul>' + tagCounts.map(function (item) {
        return '<li><a href="/archives.html?keyword=' + encodeURIComponent(item[0]) + '"># ' + escapeHtml(item[0]) + '<span>' + item[1] + '</span></a></li>';
      }).join('') + '</ul>' : '<p class="archive-empty">还没有标签，等文章多起来就热闹啦。</p>';
    }
  }

  function renderStaticArchives() {
    if (!nodes.staticList) return;
    const urlKeyword = params.get('keyword') || '';
    if (urlKeyword && nodes.staticSearch && !nodes.staticSearch.value) nodes.staticSearch.value = urlKeyword;
    const keyword = (nodes.staticSearch && nodes.staticSearch.value || '').trim().toLowerCase();
    const year = nodes.staticYear && nodes.staticYear.value || '';
    const filtered = staticPosts.filter(function (post) {
      const names = listNames(post.categories).concat(listNames(post.tags));
      const haystack = [post.title, post.text, names.join(' ')].join(' ').toLowerCase();
      return (!year || yearOf(post.date) === year) && (!keyword || haystack.indexOf(keyword) >= 0);
    });
    const groups = groupByYear(filtered, function (post) { return post.date; });
    nodes.staticList.innerHTML = Object.keys(groups).sort().reverse().map(function (yearKey) {
      return '<section class="archive-year-block"><h3>' + escapeHtml(yearKey) + '<span>' + groups[yearKey].length + '</span></h3><ul>' + groups[yearKey].map(function (post) {
        const category = listNames(post.categories)[0] || '未分类';
        const tagText = listNames(post.tags).slice(0, 3).map(function (tag) { return '<b>#' + escapeHtml(tag) + '</b>'; }).join('');
        return '<li class="archive-item"><time>' + escapeHtml(dateOf(post.date)) + '</time><a href="/' + escapeHtml(post.path || '') + '">' + escapeHtml(post.title || '未命名文章') + '</a><small>' + escapeHtml(category) + ' · ' + escapeHtml(fullDate(post.date)) + ' ' + tagText + '</small></li>';
      }).join('') + '</ul></section>';
    }).join('') || '<p class="archive-empty">没有匹配文章，换个关键词试试吧。</p>';
    if (nodes.staticStatus) nodes.staticStatus.textContent = filtered.length ? '已筛到 ' + filtered.length + ' 篇' : '没有匹配文章';
  }

  function renderDbArchives() {
    if (!nodes.dbList) return;
    const keyword = (nodes.dbSearch && nodes.dbSearch.value || '').trim().toLowerCase();
    const year = nodes.dbYear && nodes.dbYear.value || '';
    const filtered = dbArticles.filter(function (article) {
      const haystack = [article.title, article.summary, article.content, article.authorName, article.categoryName, (article.tagNames || []).join(' ')].join(' ').toLowerCase();
      const tagMatch = !initialDbTagId || (article.tagIds || []).map(String).indexOf(String(initialDbTagId)) >= 0;
      const categoryMatch = !initialDbCategoryId || String(article.categoryId || '') === String(initialDbCategoryId);
      return tagMatch && categoryMatch && (!year || yearOf(article.createdAt) === year) && (!keyword || haystack.indexOf(keyword) >= 0);
    });
    const groups = groupByYear(filtered, function (article) { return article.createdAt; });
    nodes.dbList.innerHTML = Object.keys(groups).sort().reverse().map(function (yearKey) {
      return '<section class="archive-year-block"><h3>' + escapeHtml(yearKey) + '<span>' + groups[yearKey].length + '</span></h3><ul>' + groups[yearKey].map(function (article) {
        const tags = (article.tagNames || []).slice(0, 3).map(function (tag) { return '<b>#' + escapeHtml(tag) + '</b>'; }).join('');
        return '<li class="archive-item"><time>' + escapeHtml(dateOf(article.createdAt)) + '</time><a href="/article.html?id=' + encodeURIComponent(article.id) + '">' + escapeHtml(article.title || '未命名文章') + '</a><small>' + escapeHtml(article.categoryName || '未分类') + ' · ' + (article.viewCount || 0) + ' 阅读 · ' + (article.likeCount || 0) + ' 赞 ' + tags + '</small></li>';
      }).join('') + '</ul></section>';
    }).join('') || '<p class="archive-empty">还没有上架博客，等管理员审核通过后就会出现在这里。</p>';
    if (nodes.dbStatus) nodes.dbStatus.textContent = filtered.length ? '已同步 ' + filtered.length + ' 篇' : '暂无匹配文章';
  }

  fetch('/content.json')
    .then(function (res) { return res.json(); })
    .then(function (json) {
      staticPosts = (json.posts || []).slice().sort(function (a, b) { return String(b.date).localeCompare(String(a.date)); });
      const years = Array.from(new Set(staticPosts.map(function (post) { return yearOf(post.date); }))).sort().reverse();
      renderYearOptions(nodes.staticYear, years);
      renderStaticClouds(staticPosts);
      renderStaticArchives();
    })
    .catch(function () {
      if (nodes.staticStatus) nodes.staticStatus.textContent = '静态归档加载失败';
      if (nodes.staticList) nodes.staticList.innerHTML = '<p class="archive-empty">content.json 没有加载成功，重新生成站点后再试。</p>';
    });

  fetch(API_BASE + '/articles')
    .then(function (res) { return res.json(); })
    .then(function (json) {
      dbArticles = json && json.success && Array.isArray(json.data) ? json.data : [];
      const years = Array.from(new Set(dbArticles.map(function (article) { return yearOf(article.createdAt); }))).sort().reverse();
      renderYearOptions(nodes.dbYear, years);
      renderDbArchives();
    })
    .catch(function () {
      if (nodes.dbStatus) nodes.dbStatus.textContent = '后端未连接';
      if (nodes.dbList) nodes.dbList.innerHTML = '<p class="archive-empty">没有连上后端，启动 npm run web 后可查看数据库上架归档。</p>';
    });

  if (nodes.staticSearch) nodes.staticSearch.addEventListener('input', renderStaticArchives);
  if (nodes.staticYear) nodes.staticYear.addEventListener('change', renderStaticArchives);
  if (nodes.dbSearch) nodes.dbSearch.addEventListener('input', renderDbArchives);
  if (nodes.dbYear) nodes.dbYear.addEventListener('change', renderDbArchives);
}());
</script>
