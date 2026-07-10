(function () {
  window.cialloArchivesV2 = true;
  const API_BASE = window.BLOG_API_BASE || 'http://127.0.0.1:8080/api';
  const params = new URLSearchParams(location.search);
  const initialYear = params.get('year') || '';
  const initialMonth = params.get('month') || '';
  const initialTagId = params.get('tagId') || '';
  const initialCategoryId = params.get('categoryId') || '';
  let initialKeyword = params.get('keyword') || '';

  const nodes = {
    stats: document.getElementById('archive-stat-grid'),
    staticSearch: document.getElementById('archive-search'),
    staticYear: document.getElementById('archive-year-filter'),
    staticMonth: document.getElementById('archive-month-filter'),
    staticStatus: document.getElementById('archive-filter-status'),
    staticList: document.getElementById('archive-list'),
    categories: document.getElementById('archive-category-grid'),
    tags: document.getElementById('archive-tag-cloud'),
    dbList: document.getElementById('db-archive-list'),
    dbStatus: document.getElementById('db-archive-status'),
    dbSearch: document.getElementById('db-archive-search'),
    dbYear: document.getElementById('db-archive-year'),
    dbMonth: document.getElementById('db-archive-month')
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
    const match = String(value || '').match(/^(\d{4})/);
    return match ? match[1] : '未归档';
  }

  function monthOf(value) {
    const match = String(value || '').match(/^\d{4}-(\d{2})/);
    return match ? match[1] : '00';
  }

  function archiveKey(value) {
    return yearOf(value) + '-' + monthOf(value);
  }

  function dateOf(value) {
    return String(value || '').replace('T', ' ').slice(8, 10) || '--';
  }

  function fullDate(value) {
    return String(value || '').replace('T', ' ').slice(0, 10) || '未记录日期';
  }

  function listNames(items) {
    return (items || []).map(function (item) {
      return item && item.name ? item.name : item;
    }).filter(Boolean);
  }

  function increment(map, value) {
    const key = String(value || '').trim();
    if (key) map.set(key, (map.get(key) || 0) + 1);
  }

  function sortedCounts(map) {
    return Array.from(map.entries()).sort(function (a, b) {
      return b[1] - a[1] || String(a[0]).localeCompare(String(b[0]), 'zh-Hans-CN');
    });
  }

  function activeKeyword() {
    return (nodes.staticSearch && nodes.staticSearch.value || nodes.dbSearch && nodes.dbSearch.value || '').trim();
  }

  function syncKeyword(value, updateUrl) {
    const keyword = String(value || '');
    if (nodes.staticSearch && nodes.staticSearch.value !== keyword) nodes.staticSearch.value = keyword;
    if (nodes.dbSearch && nodes.dbSearch.value !== keyword) nodes.dbSearch.value = keyword;
    if (updateUrl && history.replaceState) {
      const next = new URL(location.href);
      if (keyword.trim()) next.searchParams.set('keyword', keyword.trim());
      else next.searchParams.delete('keyword');
      next.searchParams.delete('tagId');
      next.searchParams.delete('categoryId');
      history.replaceState(null, '', next.pathname + next.search + next.hash);
    }
    renderCloudsAndStats();
    renderStaticArchives();
    renderDbArchives();
  }

  function renderCloudsAndStats() {
    const categoryMap = new Map();
    const tagMap = new Map();
    staticPosts.forEach(function (post) {
      listNames(post.categories).forEach(function (name) { increment(categoryMap, name); });
      listNames(post.tags).forEach(function (name) { increment(tagMap, name); });
    });
    dbArticles.forEach(function (article) {
      increment(categoryMap, article.categoryName);
      (article.tagNames || []).forEach(function (name) { increment(tagMap, name); });
    });
    const categoryCounts = sortedCounts(categoryMap);
    const tagCounts = sortedCounts(tagMap);
    if (nodes.stats) {
      nodes.stats.innerHTML = [
        '<span><strong>' + (staticPosts.length + dbArticles.length) + '</strong>全部文章</span>',
        '<span><strong>' + categoryCounts.length + '</strong>分类</span>',
        '<span><strong>' + tagCounts.length + '</strong>标签</span>'
      ].join('');
    }
    const selected = activeKeyword().toLowerCase();
    if (nodes.categories) {
      nodes.categories.innerHTML = categoryCounts.length ? categoryCounts.map(function (item) {
        const active = selected && selected === String(item[0]).toLowerCase() ? ' is-active' : '';
        return '<button type="button" class="archive-filter-chip' + active + '" data-archive-keyword="' + escapeHtml(item[0]) + '"><strong>' + escapeHtml(item[0]) + '</strong><span>' + item[1] + ' 篇</span></button>';
      }).join('') : '<p class="archive-empty">还没有分类，小埋先把空盒子摆好。</p>';
    }
    if (nodes.tags) {
      nodes.tags.innerHTML = tagCounts.length ? '<ul>' + tagCounts.map(function (item) {
        const active = selected && selected === String(item[0]).toLowerCase() ? ' is-active' : '';
        return '<li><button type="button" class="archive-filter-chip' + active + '" data-archive-keyword="' + escapeHtml(item[0]) + '"># ' + escapeHtml(item[0]) + '<span>' + item[1] + '</span></button></li>';
      }).join('') + '</ul>' : '<p class="archive-empty">还没有标签，等文章多起来就热闹啦。</p>';
    }
  }

  function renderYearOptions(yearSelect, monthSelect, values, getDate) {
    if (!yearSelect) return;
    const years = Array.from(new Set(values.map(function (item) { return yearOf(getDate(item)); }))).sort().reverse();
    const preferred = yearSelect.value || initialYear;
    yearSelect.innerHTML = '<option value="">全部年份</option>' + years.map(function (year) {
      return '<option value="' + escapeHtml(year) + '">' + escapeHtml(year) + ' 年</option>';
    }).join('');
    if (preferred && years.indexOf(preferred) >= 0) yearSelect.value = preferred;
    renderMonthOptions(yearSelect, monthSelect, values, getDate, initialMonth);
  }

  function renderMonthOptions(yearSelect, monthSelect, values, getDate, preferredMonth) {
    if (!monthSelect) return;
    const year = yearSelect && yearSelect.value || '';
    if (!year) {
      monthSelect.innerHTML = '<option value="">请先选择年份</option>';
      monthSelect.value = '';
      monthSelect.disabled = true;
      return;
    }
    const months = Array.from(new Set(values.filter(function (item) {
      return yearOf(getDate(item)) === year;
    }).map(function (item) {
      return monthOf(getDate(item));
    }))).sort().reverse();
    const current = preferredMonth == null ? monthSelect.value : preferredMonth;
    monthSelect.disabled = false;
    monthSelect.innerHTML = '<option value="">全年月份</option>' + months.map(function (month) {
      return '<option value="' + escapeHtml(month) + '">' + escapeHtml(month) + ' 月</option>';
    }).join('');
    if (current && months.indexOf(current) >= 0) monthSelect.value = current;
  }

  function groupByMonth(items, getDate) {
    return items.reduce(function (groups, item) {
      const key = archiveKey(getDate(item));
      if (!groups[key]) groups[key] = [];
      groups[key].push(item);
      return groups;
    }, {});
  }

  function monthHeading(key) {
    const parts = String(key).split('-');
    return parts[1] && parts[1] !== '00' ? parts[0] + ' 年 ' + parts[1] + ' 月' : parts[0];
  }

  function timeMatches(value, yearSelect, monthSelect) {
    const year = yearSelect && yearSelect.value || '';
    const month = monthSelect && monthSelect.value || '';
    return (!year || yearOf(value) === year) && (!month || monthOf(value) === month);
  }

  function renderStaticArchives() {
    if (!nodes.staticList) return;
    const keyword = (nodes.staticSearch && nodes.staticSearch.value || '').trim().toLowerCase();
    const filtered = staticPosts.filter(function (post) {
      const names = listNames(post.categories).concat(listNames(post.tags));
      const haystack = [post.title, post.text, post.author, names.join(' ')].join(' ').toLowerCase();
      return timeMatches(post.date, nodes.staticYear, nodes.staticMonth) && (!keyword || haystack.indexOf(keyword) >= 0);
    });
    const groups = groupByMonth(filtered, function (post) { return post.date; });
    nodes.staticList.innerHTML = Object.keys(groups).sort().reverse().map(function (key) {
      return '<section class="archive-year-block"><h3>' + escapeHtml(monthHeading(key)) + '<span>' + groups[key].length + '</span></h3><ul>' + groups[key].map(function (post) {
        const category = listNames(post.categories)[0] || '未分类';
        const tags = listNames(post.tags).slice(0, 3).map(function (tag) { return '<b>#' + escapeHtml(tag) + '</b>'; }).join('');
        return '<li class="archive-item"><time>' + escapeHtml(dateOf(post.date)) + ' 日</time><a href="/' + escapeHtml(post.path || '') + '">' + escapeHtml(post.title || '未命名文章') + '</a><small>' + escapeHtml(category) + ' · ' + escapeHtml(fullDate(post.date)) + ' ' + tags + '</small></li>';
      }).join('') + '</ul></section>';
    }).join('') || '<p class="archive-empty">没有匹配文章，换个关键词试试吧。</p>';
    if (nodes.staticStatus) nodes.staticStatus.textContent = filtered.length ? '已筛到 ' + filtered.length + ' 篇' : '没有匹配文章';
  }

  function renderDbArchives() {
    if (!nodes.dbList) return;
    const keyword = (nodes.dbSearch && nodes.dbSearch.value || '').trim().toLowerCase();
    const filtered = dbArticles.filter(function (article) {
      const haystack = [article.title, article.summary, article.content, article.authorUsername, article.categoryName, (article.tagNames || []).join(' ')].join(' ').toLowerCase();
      return timeMatches(article.createdAt, nodes.dbYear, nodes.dbMonth) && (!keyword || haystack.indexOf(keyword) >= 0);
    });
    const groups = groupByMonth(filtered, function (article) { return article.createdAt; });
    nodes.dbList.innerHTML = Object.keys(groups).sort().reverse().map(function (key) {
      return '<section class="archive-year-block"><h3>' + escapeHtml(monthHeading(key)) + '<span>' + groups[key].length + '</span></h3><ul>' + groups[key].map(function (article) {
        const tags = (article.tagNames || []).slice(0, 3).map(function (tag) { return '<b>#' + escapeHtml(tag) + '</b>'; }).join('');
        const username = article.authorUsername || article.authorName || '未知用户';
        return '<li class="archive-item"><time>' + escapeHtml(dateOf(article.createdAt)) + ' 日</time><a href="/article.html?id=' + encodeURIComponent(article.id) + '">' + escapeHtml(article.title || '未命名文章') + '</a><small>作者 @' + escapeHtml(username) + ' · ' + escapeHtml(article.categoryName || '未分类') + ' · ' + (article.viewCount || 0) + ' 阅读 · ' + (article.likeCount || 0) + ' 赞 ' + tags + '</small></li>';
      }).join('') + '</ul></section>';
    }).join('') || '<p class="archive-empty">没有匹配的系统文章。</p>';
    if (nodes.dbStatus) nodes.dbStatus.textContent = filtered.length ? '已同步 ' + filtered.length + ' 篇' : '暂无匹配文章';
  }

  function resolveInitialTaxonomyKeyword() {
    if (initialKeyword || (!initialTagId && !initialCategoryId)) return;
    if (initialCategoryId) {
      const article = dbArticles.find(function (item) {
        return String(item.categoryId || '') === String(initialCategoryId);
      });
      if (article && article.categoryName) initialKeyword = article.categoryName;
    }
    if (!initialKeyword && initialTagId) {
      dbArticles.some(function (article) {
        const ids = (article.tagIds || []).map(String);
        const index = ids.indexOf(String(initialTagId));
        if (index < 0) return false;
        initialKeyword = (article.tagNames || [])[index] || '';
        return Boolean(initialKeyword);
      });
    }
    if (initialKeyword) syncKeyword(initialKeyword, false);
  }

  if (nodes.staticSearch) nodes.staticSearch.value = initialKeyword;
  if (nodes.dbSearch) nodes.dbSearch.value = initialKeyword;

  fetch('/content.json')
    .then(function (response) { return response.json(); })
    .then(function (json) {
      staticPosts = (json.posts || []).slice().sort(function (a, b) {
        return String(b.date).localeCompare(String(a.date));
      });
      renderYearOptions(nodes.staticYear, nodes.staticMonth, staticPosts, function (post) { return post.date; });
      renderCloudsAndStats();
      renderStaticArchives();
    })
    .catch(function () {
      if (nodes.staticStatus) nodes.staticStatus.textContent = '静态归档加载失败';
      if (nodes.staticList) nodes.staticList.innerHTML = '<p class="archive-empty">content.json 没有加载成功，重新生成站点后再试。</p>';
    });

  fetch(API_BASE + '/articles')
    .then(function (response) { return response.json(); })
    .then(function (json) {
      dbArticles = json && json.success && Array.isArray(json.data) ? json.data : [];
      renderYearOptions(nodes.dbYear, nodes.dbMonth, dbArticles, function (article) { return article.createdAt; });
      resolveInitialTaxonomyKeyword();
      renderCloudsAndStats();
      renderDbArchives();
    })
    .catch(function () {
      if (nodes.dbStatus) nodes.dbStatus.textContent = '后端未连接';
      if (nodes.dbList) nodes.dbList.innerHTML = '<p class="archive-empty">没有连上后端，请先启动系统后端。</p>';
    });

  function onSearchInput(event) {
    syncKeyword(event.target.value, true);
  }

  if (nodes.staticSearch) nodes.staticSearch.addEventListener('input', onSearchInput);
  if (nodes.dbSearch) nodes.dbSearch.addEventListener('input', onSearchInput);
  if (nodes.staticYear) nodes.staticYear.addEventListener('change', function () {
    renderMonthOptions(nodes.staticYear, nodes.staticMonth, staticPosts, function (post) { return post.date; }, '');
    renderStaticArchives();
  });
  if (nodes.staticMonth) nodes.staticMonth.addEventListener('change', renderStaticArchives);
  if (nodes.dbYear) nodes.dbYear.addEventListener('change', function () {
    renderMonthOptions(nodes.dbYear, nodes.dbMonth, dbArticles, function (article) { return article.createdAt; }, '');
    renderDbArchives();
  });
  if (nodes.dbMonth) nodes.dbMonth.addEventListener('change', renderDbArchives);

  const cloudPanel = document.querySelector('.archive-cloud-panel');
  if (cloudPanel) cloudPanel.addEventListener('click', function (event) {
    const button = event.target.closest('[data-archive-keyword]');
    if (!button) return;
    const value = button.getAttribute('data-archive-keyword') || '';
    syncKeyword(activeKeyword().toLowerCase() === value.toLowerCase() ? '' : value, true);
  });
}());
