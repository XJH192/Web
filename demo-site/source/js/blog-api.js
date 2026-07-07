(function () {
  const API_BASE = window.BLOG_API_BASE || 'http://127.0.0.1:8080/api';
  const state = { categories: [], tags: [], currentUser: null, editorAttachments: [] };

  function $(id) { return document.getElementById(id); }
  function token() { return localStorage.getItem('blogToken') || ''; }
  function storedUser() {
    try { return JSON.parse(localStorage.getItem('blogUser') || 'null'); }
    catch (error) { return null; }
  }
  function setSession(loginResponse) {
    localStorage.setItem('blogToken', loginResponse.token);
    localStorage.setItem('blogUser', JSON.stringify(loginResponse.user));
    state.currentUser = loginResponse.user;
    applyRoleNavigation(loginResponse.user);
  }
  function clearSession() {
    localStorage.removeItem('blogToken');
    localStorage.removeItem('blogUser');
    state.currentUser = null;
  }
  function escapeHtml(value) {
    return String(value == null ? '' : value)
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }
  function textPreview(value, max) {
    const text = String(value || '').replace(/<[^>]+>/g, '').trim();
    return text.length > max ? text.slice(0, max) + '...' : text;
  }
  function show(id, message) {
    const el = $(id);
    if (el) el.textContent = message || '';
  }
  function roleText(role) {
    return role === 'ADMIN' ? '管理员' : '普通用户';
  }
  function statusText(status) {
    const map = {
      PUBLISHED: '已上架',
      DRAFT: '草稿',
      PENDING: '待审核',
      REJECTED: '已驳回',
      APPROVED: '已通过'
    };
    return map[status] || status || '';
  }
  function statusClass(status) {
    const map = { PUBLISHED: 'success', APPROVED: 'success', PENDING: 'warning', DRAFT: 'muted', REJECTED: 'danger' };
    return map[status] || 'muted';
  }
  function featureText(feature) {
    const map = {
      AI_OUTLINE: 'AI 大纲生成',
      AI_SUMMARY: 'AI 摘要生成',
      AI_TAGS: 'AI 标签推荐',
      AI_COMMENT_REVIEW: 'AI 评论审核',
      AI_QA: 'AI 博客问答'
    };
    return map[feature] || feature || '';
  }
  function reviewText(value) {
    if (!value) return '未审核';
    if (value.indexOf('PASS') === 0) return 'AI 初审通过：普通评论';
    if (value.indexOf('REJECT') === 0) return value.replace('REJECT:', 'AI 初审拒绝：');
    return value;
  }
  function formatDate(value) {
    if (!value) return '';
    return String(value).replace('T', ' ').slice(0, 16);
  }
  function userLine(user) {
    if (!user) return '未登录';
    return `${user.nickname || user.username}（${roleText(user.role)}${user.banned ? '，已封禁' : ''}）`;
  }
  function loginUrl() {
    return '/login.html?redirect=' + encodeURIComponent(location.pathname + location.search);
  }
  function redirectByRole(user) {
    location.href = user.role === 'ADMIN' ? '/admin.html' : '/blog.html';
  }
  function setLinksByHref(href, visible) {
    document.querySelectorAll('a[href]').forEach(function (link) {
      const raw = link.getAttribute('href') || '';
      if (raw === href || raw.endsWith(href)) {
        const item = link.closest('.nexmoe-list-item') || link;
        item.style.display = visible ? (item.classList.contains('nexmoe-list-item') ? 'flex' : 'inline-block') : 'none';
      }
    });
  }
  function applyRoleNavigation(user) {
    if (!user) return;
    if (user.role === 'ADMIN') {
      setLinksByHref('/admin.html', true);
      setLinksByHref('/blog.html', false);
      document.querySelectorAll('.admin-only-link').forEach(el => { el.style.display = 'inline-block'; });
      document.querySelectorAll('.user-only-link').forEach(el => { el.style.display = 'none'; });
    } else {
      setLinksByHref('/blog.html', true);
      setLinksByHref('/admin.html', false);
      document.querySelectorAll('.user-only-link').forEach(el => { el.style.display = 'inline-block'; });
      document.querySelectorAll('.admin-only-link').forEach(el => { el.style.display = 'none'; });
    }
  }

  async function api(path, options) {
    const opts = options || {};
    const headers = Object.assign({ 'Content-Type': 'application/json' }, opts.headers || {});
    if (token()) headers['X-Token'] = token();
    let response;
    try {
      response = await fetch(API_BASE + path, Object.assign({}, opts, { headers }));
    } catch (error) {
      throw new Error('无法连接后端，请确认 MySQL 和后端服务已启动');
    }
    let json;
    try {
      json = await response.json();
    } catch (error) {
      throw new Error('后端返回格式异常');
    }
    if (!response.ok || !json.success) throw new Error(json.message || '请求失败');
    return json.data;
  }
  async function verifySession() {
    if (!token()) return null;
    try {
      const user = await api('/auth/me');
      localStorage.setItem('blogUser', JSON.stringify(user));
      state.currentUser = user;
      applyRoleNavigation(user);
      return user;
    } catch (error) {
      clearSession();
      return null;
    }
  }
  async function requireLogin() {
    const user = await verifySession();
    if (!user) {
      location.href = loginUrl();
      throw new Error('请先登录');
    }
    return user;
  }
  async function requireAdmin() {
    const user = await requireLogin();
    if (user.role !== 'ADMIN') {
      alert('当前账号不是管理员，已返回用户工作台');
      location.href = '/blog.html';
      throw new Error('需要管理员权限');
    }
    return user;
  }
  async function loadTaxonomy() {
    const categories = await api('/categories');
    const tags = await api('/tags');
    state.categories = categories || [];
    state.tags = tags || [];
  }
  function categoryOptions(selected, placeholder) {
    const first = placeholder == null ? '' : `<option value="">${escapeHtml(placeholder)}</option>`;
    if (!state.categories.length) return first || '<option value="">暂无分类</option>';
    return first + state.categories.map(c => `<option value="${c.id}" ${Number(selected) === c.id ? 'selected' : ''}>${escapeHtml(c.name)}</option>`).join('');
  }
  function tagOptions(selected, placeholder) {
    const first = placeholder == null ? '<option value="">全部标签</option>' : `<option value="">${escapeHtml(placeholder)}</option>`;
    return first + state.tags.map(t => `<option value="${t.id}" ${Number(selected) === t.id ? 'selected' : ''}>${escapeHtml(t.name)}</option>`).join('');
  }
  function uniqueNames(names) {
    const seen = new Set();
    return (names || []).map(name => String(name || '').trim()).filter(function (name) {
      if (!name || seen.has(name.toLowerCase())) return false;
      seen.add(name.toLowerCase());
      return true;
    });
  }
  function splitTagNames(value) {
    return uniqueNames(String(value || '').split(/[，,、;；\s]+/));
  }
  function setTagInput(names) {
    const input = $('article-tag-input');
    if (input) input.value = uniqueNames(names).join('，');
    renderTagPreview();
  }
  function selectedTagNames() {
    const input = $('article-tag-input');
    return splitTagNames(input ? input.value : '');
  }
  function selectedTagIds() { return []; }
  function renderTagPreview() {
    const box = $('article-tags');
    if (!box) return;
    const names = selectedTagNames();
    box.innerHTML = names.length
      ? names.map(name => `<span class="tag-pill">${escapeHtml(name)}</span>`).join('')
      : '<span class="blog-muted">暂未填写标签，可手动输入或点击 AI 推荐。</span>';
  }
  function normalizeAttachments(value) {
    return Array.isArray(value) ? value.filter(item => item && item.name && item.dataUrl) : [];
  }
  function formatBytes(value) {
    const size = Number(value || 0);
    if (size >= 1024 * 1024) return (size / 1024 / 1024).toFixed(1) + 'MB';
    if (size >= 1024) return Math.round(size / 1024) + 'KB';
    return size + 'B';
  }
  function safeDataUrl(value) {
    const text = String(value || '');
    return text.indexOf('data:') === 0 ? text : '#';
  }
  function fileKind(attachment) {
    const type = String(attachment.type || '').toLowerCase();
    const name = String(attachment.name || '').toLowerCase();
    if (type.indexOf('image/') === 0) return '图片';
    if (name.endsWith('.ppt') || name.endsWith('.pptx')) return 'PPT';
    if (name.endsWith('.pdf')) return 'PDF';
    if (name.endsWith('.doc') || name.endsWith('.docx')) return '文档';
    return '附件';
  }
  function renderAttachments() {
    const box = $('article-attachments');
    if (!box) return;
    const attachments = normalizeAttachments(state.editorAttachments);
    box.innerHTML = attachments.length ? attachments.map((item, index) => `
      <div class="attachment-item">
        <span><strong>${escapeHtml(item.name)}</strong><small>${escapeHtml(fileKind(item))} · ${formatBytes(item.size)}</small></span>
        <button type="button" class="danger attachment-remove" data-attachment-remove="${index}">移除</button>
      </div>
    `).join('') : '<p class="blog-muted">还没有上传附件，支持图片、PPT、PDF、Word 等文件。</p>';
  }
  function readFilesAsAttachments(fileList) {
    const files = Array.from(fileList || []);
    const maxSize = 12 * 1024 * 1024;
    return Promise.all(files.map(function (file) {
      if (file.size > maxSize) throw new Error(file.name + ' 超过 12MB，请压缩后再上传。');
      return new Promise(function (resolve, reject) {
        const reader = new FileReader();
        reader.onload = function () {
          resolve({ name: file.name, type: file.type || 'application/octet-stream', size: file.size, dataUrl: reader.result });
        };
        reader.onerror = function () { reject(new Error(file.name + ' 读取失败')); };
        reader.readAsDataURL(file);
      });
    }));
  }
  function renderArticleAttachments(attachments) {
    const list = normalizeAttachments(attachments);
    if (!list.length) return '';
    return `<section class="article-attachments" id="article-attachments-section"><h2>文章附件</h2><div class="article-attachment-grid">${list.map(function (item) {
      const url = escapeHtml(safeDataUrl(item.dataUrl));
      const title = escapeHtml(item.name);
      if (String(item.type || '').indexOf('image/') === 0) {
        return `<a class="attachment-link article-attachment-preview" href="${url}" download="${title}"><span><strong>${title}</strong><small>图片 · ${formatBytes(item.size)}</small></span><img src="${url}" alt="${title}"></a>`;
      }
      return `<a class="attachment-link" href="${url}" download="${title}"><span><strong>${title}</strong><small>${escapeHtml(fileKind(item))} · ${formatBytes(item.size)}</small></span><span>下载</span></a>`;
    }).join('')}</div></section>`;
  }
  function renderCompactAttachments(attachments) {
    const list = normalizeAttachments(attachments);
    if (!list.length) return '';
    return `<div class="attachment-strip">${list.slice(0, 3).map(function (item) {
      const url = escapeHtml(safeDataUrl(item.dataUrl));
      const title = escapeHtml(item.name);
      return `<a class="attachment-chip" href="${url}" download="${title}" title="${title}"><span>${escapeHtml(fileKind(item))}</span>${title}</a>`;
    }).join('')}${list.length > 3 ? `<span class="attachment-chip more">+${list.length - 3}</span>` : ''}</div>`;
  }
  function articleStats(article) {
    return `点赞 ${article.likeCount || 0} | 评论 ${article.commentCount || 0} | 阅读 ${article.viewCount || 0}`;
  }
  function likeButton(article) {
    const liked = !!article.likedByCurrentUser;
    return `<button type="button" class="umaru-like ${liked ? 'is-liked' : ''}" data-like-article="${article.id}" data-liked="${liked ? 'true' : 'false'}" aria-pressed="${liked ? 'true' : 'false'}">
      <svg class="umaru-like-icon" viewBox="0 0 32 32" aria-hidden="true">
        <path class="umaru-hood" d="M16 4c6.1 0 11 4.7 11 10.8 0 7.2-5.2 12.2-11 12.2S5 22 5 14.8C5 8.7 9.9 4 16 4Z" />
        <path class="umaru-face" d="M10.5 15.5c1.2-1.4 2.8-2.1 5.5-2.1s4.3.7 5.5 2.1" />
        <path class="umaru-mouth" d="M12.4 20.2c2.3 1.6 4.9 1.6 7.2 0" />
        <circle class="umaru-eye" cx="12" cy="17" r="1.2" />
        <circle class="umaru-eye" cx="20" cy="17" r="1.2" />
      </svg>
      <span>${liked ? '已点赞' : '点赞'} ${article.likeCount || 0}</span>
    </button>`;
  }
  function showNoticeModal(title, items, onClose) {
    const list = Array.isArray(items) ? items : [];
    if (!list.length) return;
    let modal = $('blog-notice-modal');
    if (!modal) {
      modal = document.createElement('div');
      modal.id = 'blog-notice-modal';
      modal.className = 'blog-notice-modal';
      modal.setAttribute('aria-hidden', 'true');
      document.body.appendChild(modal);
    }
    modal.innerHTML = `<section class="blog-notice-dialog" role="dialog" aria-modal="true" aria-labelledby="blog-notice-title">
      <button type="button" class="blog-notice-close" aria-label="关闭">×</button>
      <p class="section-kicker">消息提醒</p>
      <h2 id="blog-notice-title">${escapeHtml(title || '你有新消息')}</h2>
      <div class="blog-notice-list">${list.map(function (item) {
        const link = item.link ? `<a href="${escapeHtml(item.link)}">查看</a>` : '';
        return `<article class="blog-notice-item"><strong>${escapeHtml(item.title || '新消息')}</strong><p>${escapeHtml(item.content || '')}</p><small>${formatDate(item.createdAt)} ${link}</small></article>`;
      }).join('')}</div>
    </section>`;
    function closeModal() {
      modal.classList.remove('is-visible');
      modal.setAttribute('aria-hidden', 'true');
      document.body.classList.remove('blog-modal-open');
      document.body.style.overflow = '';
      if (onClose) onClose();
      document.removeEventListener('keydown', onKeydown);
    }
    function onKeydown(event) { if (event.key === 'Escape') closeModal(); }
    modal.addEventListener('click', function handleClick(event) {
      if (event.target === modal || event.target.closest('.blog-notice-close')) {
        modal.removeEventListener('click', handleClick);
        closeModal();
      }
    });
    document.addEventListener('keydown', onKeydown);
    document.body.classList.add('blog-modal-open');
    document.body.style.overflow = 'hidden';
    modal.classList.add('is-visible');
    modal.setAttribute('aria-hidden', 'false');
    const close = modal.querySelector('.blog-notice-close');
    if (close) close.focus();
  }
  async function showUnreadNotifications(title) {
    try {
      const items = await api('/notifications?unread=true');
      if (items && items.length) {
        showNoticeModal(title || '你有新消息', items, function () {
          api('/notifications/read', { method: 'PUT' }).catch(function () {});
        });
      }
    } catch (error) {}
  }
  function showAdminReviewNotice(articles, comments) {
    if (sessionStorage.getItem('xjhAdminReviewNoticeShown')) return;
    const pendingArticles = (articles || []).filter(item => item.status === 'PENDING');
    const pendingComments = (comments || []).filter(item => item.status === 'PENDING');
    const items = pendingArticles.slice(0, 4).map(item => ({ title: '待审核博客', content: `《${item.title}》等待审核上架`, link: '/admin.html#articles', createdAt: item.createdAt }))
      .concat(pendingComments.slice(0, 4).map(item => ({ title: '待审核评论', content: `${item.nickname || '用户'} 评论了《${item.articleTitle || '文章'}》`, link: '/admin.html#comments', createdAt: item.createdAt })));
    if (!items.length) return;
    sessionStorage.setItem('xjhAdminReviewNoticeShown', '1');
    showNoticeModal('后台有待审核内容', items);
  }
  function renderArticleContent(content, attachments) {
    const lines = String(content || '').split(/\r?\n/);
    const toc = [];
    const parts = [];
    lines.forEach(function (line) {
      const text = line.trim();
      if (!text) return;
      const heading = text.match(/^(#{1,4})\s+(.+)$/);
      const numbered = text.match(/^([0-9]+[.、]\s*)(.+)$/);
      if (heading || numbered) {
        const title = heading ? heading[2].trim() : numbered[2].trim();
        const level = heading ? Math.min(4, heading[1].length + 1) : 2;
        const id = 'article-section-' + toc.length;
        toc.push({ id: id, title: title });
        parts.push(`<h${level} id="${id}">${escapeHtml(title)}</h${level}>`);
      } else {
        parts.push(`<p>${escapeHtml(line)}</p>`);
      }
    });
    if (!parts.length) parts.push('<p class="blog-muted">暂无正文内容。</p>');
    if (!toc.length) {
      toc.push({ id: 'article-body-start', title: '正文内容' });
      parts.unshift('<h2 id="article-body-start">正文内容</h2>');
    }
    const attachmentHtml = renderArticleAttachments(attachments);
    if (attachmentHtml) toc.push({ id: 'article-attachments-section', title: '文章附件' });
    const tocHtml = `<nav class="article-inline-toc" aria-label="文章目录"><strong>文章目录</strong>${toc.map(item => `<a href="#${item.id}" data-article-toc>${escapeHtml(item.title)}</a>`).join('')}</nav>`;
    return `<div class="article-detail-layout"><div class="article-content-body">${parts.join('')}${attachmentHtml}</div>${tocHtml}</div>`;
  }

  function renderAiResult(targetId, data) {
    const target = $(targetId);
    if (!target) return;
    if (data.outline) {
      target.innerHTML = `<strong>建议大纲</strong><ol>${data.outline.map(item => `<li>${escapeHtml(item.replace(/^\d+\.\s*/, ''))}</li>`).join('')}</ol>`;
      return;
    }
    if (data.summary) {
      target.innerHTML = `<strong>生成摘要</strong><p>${escapeHtml(data.summary)}</p>`;
      return;
    }
    if (data.tags) {
      const tags = uniqueNames(data.tags);
      if ($('article-tag-input')) {
        setTagInput(uniqueNames(selectedTagNames().concat(tags)));
      }
      target.innerHTML = `<strong>推荐标签</strong><p>${tags.map(tag => `<span class="tag-pill">${escapeHtml(tag)}</span>`).join('')}</p><p class="blog-muted">已同步到文章标签输入框，可继续手动修改。</p>`;
      return;
    }
    if (data.answer) {
      target.innerHTML = `<strong>回答</strong><p>${escapeHtml(data.answer)}</p>`;
      return;
    }
    target.textContent = JSON.stringify(data, null, 2);
  }
  function buildQuery(fields) {
    const params = new URLSearchParams();
    fields.forEach(function (field) {
      const el = $(field.id);
      if (el && el.value && el.value.trim()) params.set(field.name, el.value.trim());
    });
    return params.toString();
  }

  async function initLoginPage() {
    const user = await verifySession();
    const cached = user || storedUser();
    if (cached) {
      show('login-status', `当前已登录：${userLine(cached)}`);
      const enterBtn = $('enter-system-btn');
      if (enterBtn) enterBtn.hidden = false;
    }

    if ($('login-form')) $('login-form').addEventListener('submit', async function (event) {
      event.preventDefault();
      show('login-status', '正在登录并校验数据库账号...');
      try {
        const data = await api('/auth/login', {
          method: 'POST',
          body: JSON.stringify({ username: $('login-username').value.trim(), password: $('login-password').value })
        });
        setSession(data);
        show('login-status', `登录成功：${userLine(data.user)}，正在进入对应页面...`);
        setTimeout(function () { redirectByRole(data.user); }, 350);
      } catch (error) {
        show('login-status', error.message);
      }
    });

    if ($('register-form')) $('register-form').addEventListener('submit', async function (event) {
      event.preventDefault();
      show('register-status', '正在写入数据库...');
      try {
        const user = await api('/auth/register', {
          method: 'POST',
          body: JSON.stringify({
            username: $('register-username').value.trim(),
            nickname: $('register-nickname').value.trim(),
            password: $('register-password').value
          })
        });
        show('register-status', `注册成功：${user.nickname || user.username}，请在左侧登录`);
        event.target.reset();
      } catch (error) {
        show('register-status', error.message);
      }
    });
  }

  async function initBlogPage() {
    const user = await requireLogin();
    if (user.role === 'ADMIN') {
      location.href = '/admin.html';
      return;
    }
    applyRoleNavigation(user);
    await loadTaxonomy();

    $('blog-user-summary').textContent = `当前用户：${userLine(user)}。提交后的文章需要管理员审核，通过后才会显示到首页。`;
    $('home-category-filter').innerHTML = categoryOptions(null, '全部分类');
    $('home-tag-filter').innerHTML = tagOptions(null, '全部标签');
    $('blog-category-filter').innerHTML = categoryOptions(null, '全部分类');
    $('blog-tag-filter').innerHTML = tagOptions(null, '全部标签');
    $('article-category').innerHTML = categoryOptions(null, '请选择分类');
    setTagInput([]);
    state.editorAttachments = [];
    renderAttachments();

    async function loadHomeComments(articleId) {
      const list = document.querySelector(`[data-comment-list="${articleId}"]`);
      if (!list) return;
      list.innerHTML = '<p class="blog-muted">正在加载评论...</p>';
      try {
        const comments = await api('/articles/' + articleId + '/comments');
        list.innerHTML = comments.map(c => `
          <div class="comment-item feed-comment">
            <p>${escapeHtml(c.content)}</p>
            <p class="comment-meta">${escapeHtml(c.nickname)} | ${formatDate(c.createdAt)} | ${escapeHtml(reviewText(c.aiReviewResult))}</p>
          </div>
        `).join('') || '<p class="blog-muted">暂无已通过评论。</p>';
      } catch (error) {
        list.innerHTML = `<p class="blog-muted">${escapeHtml(error.message)}</p>`;
      }
    }

    async function loadHomeArticles() {
      try {
        show('home-status', '正在加载首页文章...');
        const query = buildQuery([
          { id: 'home-keyword', name: 'keyword' },
          { id: 'home-category-filter', name: 'categoryId' },
          { id: 'home-tag-filter', name: 'tagId' }
        ]);
        const articles = await api('/articles/feed' + (query ? '?' + query : ''));
        $('home-article-list').innerHTML = articles.map(article => `
          <article class="article-card feed-card">
            <div class="article-card-head">
              <span class="status-pill success">已上架</span>
              <span class="article-meta">${articleStats(article)}</span>
            </div>
            <h3>${escapeHtml(article.title)}</h3>
            <p class="article-meta">作者：${escapeHtml(article.authorName)} | 分类：${escapeHtml(article.categoryName)} | ${formatDate(article.createdAt)}</p>
            <p>${escapeHtml(article.summary || textPreview(article.content, 140))}</p>
            <p>${(article.tagNames || []).map(t => `<span class="tag-pill">${escapeHtml(t)}</span>`).join('')}</p>
            ${renderCompactAttachments(article.attachments)}
            <div class="article-actions">
              <a class="blog-link-btn" href="/article.html?id=${article.id}">查看全文</a>
              ${likeButton(article)}
              <button type="button" class="secondary" data-home-comments="${article.id}">查看评论 ${article.commentCount || 0}</button>
            </div>
            <div class="home-comment-box" data-comment-box="${article.id}">
              <form data-home-comment-form="${article.id}" class="blog-form inline-comment-form">
                <textarea data-comment-input="${article.id}" required rows="3" placeholder="写下评论，提交后等待管理员审核"></textarea>
                <button type="submit">提交评论</button>
              </form>
              <div class="blog-status" data-comment-status="${article.id}"></div>
              <div data-comment-list="${article.id}" class="feed-comments"></div>
            </div>
          </article>
        `).join('') || '<p class="blog-muted">暂无其他用户已上架博客。管理员审核通过后，文章会显示在这里。</p>';
        show('home-status', `首页已加载 ${articles.length} 篇已上架文章`);
      } catch (error) {
        show('home-status', error.message);
      }
    }

    async function loadMyArticles() {
      try {
        show('blog-status', '正在加载你的文章...');
        const query = buildQuery([
          { id: 'blog-keyword', name: 'keyword' },
          { id: 'blog-category-filter', name: 'categoryId' },
          { id: 'blog-tag-filter', name: 'tagId' }
        ]);
        const articles = await api('/articles/mine' + (query ? '?' + query : ''));
        $('article-list').innerHTML = articles.map(article => `
          <article class="article-card">
            <div class="article-card-head">
              <span class="status-pill ${statusClass(article.status)}">${statusText(article.status)}</span>
              <span class="article-meta">${articleStats(article)}</span>
            </div>
            <h3>${escapeHtml(article.title)}</h3>
            <p class="article-meta">${escapeHtml(article.categoryName)} | ${formatDate(article.createdAt)}</p>
            <p>${escapeHtml(article.summary || textPreview(article.content, 120))}</p>
            <p>${(article.tagNames || []).map(t => `<span class="tag-pill">${escapeHtml(t)}</span>`).join('')}</p>
            ${renderCompactAttachments(article.attachments)}
            <div class="article-actions">
              <a class="blog-link-btn" href="/article.html?id=${article.id}">查看</a>
              <button type="button" class="secondary" data-edit="${article.id}">编辑</button>
              <button type="button" class="danger" data-delete="${article.id}">删除</button>
            </div>
          </article>
        `).join('') || '<p class="blog-muted">你还没有文章，可以在下方发布第一篇。</p>';
        show('blog-status', `已加载 ${articles.length} 篇你的文章`);
      } catch (error) {
        show('blog-status', error.message);
      }
    }

    $('home-search-btn').addEventListener('click', loadHomeArticles);
    $('home-article-list').addEventListener('click', async function (event) {
      const likeButtonEl = event.target.closest('[data-like-article]');
      const commentsButtonEl = event.target.closest('[data-home-comments]');
      const likeId = likeButtonEl ? likeButtonEl.getAttribute('data-like-article') : null;
      const commentsId = commentsButtonEl ? commentsButtonEl.getAttribute('data-home-comments') : null;
      try {
        if (likeId) {
          const liked = likeButtonEl.getAttribute('data-liked') === 'true';
          likeButtonEl.disabled = true;
          await api('/articles/' + likeId + '/like', { method: liked ? 'DELETE' : 'POST' });
          show('home-status', liked ? '已取消点赞' : '点赞成功');
          await loadHomeArticles();
        }
        if (commentsId) await loadHomeComments(commentsId);
      } catch (error) {
        show('home-status', error.message);
        if (likeButtonEl) likeButtonEl.disabled = false;
      }
    });
    $('home-article-list').addEventListener('submit', async function (event) {
      const articleId = event.target.getAttribute('data-home-comment-form');
      if (!articleId) return;
      event.preventDefault();
      const input = document.querySelector(`[data-comment-input="${articleId}"]`);
      const status = document.querySelector(`[data-comment-status="${articleId}"]`);
      try {
        if (status) status.textContent = '评论提交中...';
        await api('/articles/' + articleId + '/comments', {
          method: 'POST',
          body: JSON.stringify({ content: input.value, parentId: null })
        });
        input.value = '';
        if (status) status.textContent = '评论已提交，等待管理员审核后公开显示。';
      } catch (error) {
        if (status) status.textContent = error.message;
      }
    });

    $('blog-search-btn').addEventListener('click', loadMyArticles);
    if ($('article-tag-input')) $('article-tag-input').addEventListener('input', renderTagPreview);
    if ($('article-files')) $('article-files').addEventListener('change', async function (event) {
      try {
        show('blog-status', '正在读取附件...');
        const files = await readFilesAsAttachments(event.target.files);
        state.editorAttachments = normalizeAttachments(state.editorAttachments).concat(files).slice(0, 8);
        renderAttachments();
        event.target.value = '';
        show('blog-status', '附件已加入文章，保存后会同步到数据库');
      } catch (error) {
        show('blog-status', error.message);
      }
    });
    if ($('article-attachments')) $('article-attachments').addEventListener('click', function (event) {
      const index = event.target.getAttribute('data-attachment-remove');
      if (index == null) return;
      state.editorAttachments.splice(Number(index), 1);
      renderAttachments();
    });
    $('logout-btn').addEventListener('click', function () {
      clearSession();
      location.href = '/login.html';
    });
    $('article-reset-btn').addEventListener('click', function () {
      $('article-form').reset();
      $('article-id').value = '';
      setTagInput([]);
      state.editorAttachments = [];
      renderAttachments();
      show('blog-status', '已清空编辑表单');
    });
    $('article-list').addEventListener('click', async function (event) {
      const editId = event.target.getAttribute('data-edit');
      const deleteId = event.target.getAttribute('data-delete');
      try {
        if (editId) {
          const article = await api('/articles/' + editId);
          $('article-id').value = article.id;
          $('article-title').value = article.title;
          $('article-summary').value = article.summary || '';
          $('article-content').value = article.content;
          $('article-category').value = article.categoryId;
          $('article-status').value = article.status === 'DRAFT' ? 'DRAFT' : 'PENDING';
          setTagInput(article.tagNames || []);
          state.editorAttachments = normalizeAttachments(article.attachments);
          renderAttachments();
          show('blog-status', `正在编辑文章：${article.title}`);
          $('article-title').focus();
        }
        if (deleteId && confirm('确认删除这篇文章吗？')) {
          await api('/articles/' + deleteId, { method: 'DELETE' });
          await loadMyArticles();
        }
      } catch (error) {
        show('blog-status', error.message);
      }
    });

    $('article-form').addEventListener('submit', async function (event) {
      event.preventDefault();
      const id = $('article-id').value;
      const payload = {
        title: $('article-title').value,
        summary: $('article-summary').value,
        content: $('article-content').value,
        categoryId: Number($('article-category').value),
        tagIds: selectedTagIds(),
        tagNames: selectedTagNames(),
        attachments: normalizeAttachments(state.editorAttachments),
        status: $('article-status').value
      };
      try {
        show('blog-status', '正在保存到数据库...');
        await api('/articles' + (id ? '/' + id : ''), { method: id ? 'PUT' : 'POST', body: JSON.stringify(payload) });
        event.target.reset();
        $('article-id').value = '';
        setTagInput([]);
        state.editorAttachments = [];
        renderAttachments();
        await loadMyArticles();
        show('blog-status', payload.status === 'DRAFT' ? '草稿已保存' : '文章已提交审核，管理员通过后会上架首页');
      } catch (error) {
        show('blog-status', error.message);
      }
    });

    async function runAi(path, payload) {
      try {
        show('blog-status', 'AI 正在生成...');
        const data = await api(path, { method: 'POST', body: JSON.stringify(payload) });
        renderAiResult('ai-output', data);
        show('blog-status', 'AI 生成完成，记录已写入数据库日志');
      } catch (error) {
        show('blog-status', error.message);
        $('ai-output').textContent = error.message;
      }
    }
    $('ai-outline-btn').addEventListener('click', () => runAi('/ai/outline', { title: $('article-title').value }));
    $('ai-summary-btn').addEventListener('click', () => runAi('/ai/summary', { content: $('article-content').value }));
    $('ai-tags-btn').addEventListener('click', () => runAi('/ai/tags', { title: $('article-title').value, content: $('article-content').value }));

    await loadHomeArticles();
    await loadMyArticles();
    await showUnreadNotifications('你有新的博客消息');
  }

  async function initArticlePage() {
    const user = await requireLogin();
    applyRoleNavigation(user);
    const id = new URLSearchParams(location.search).get('id') || '1';
    async function loadDetail() {
      const article = await api('/articles/' + id);
      $('article-detail').innerHTML = `
        <h1>${escapeHtml(article.title)}</h1>
        <p class="article-meta">作者：${escapeHtml(article.authorName)} | 分类：${escapeHtml(article.categoryName)} | ${formatDate(article.createdAt)} | ${articleStats(article)}</p>
        <p><span class="status-pill ${statusClass(article.status)}">${statusText(article.status)}</span> ${(article.tagNames || []).map(t => `<span class="tag-pill">${escapeHtml(t)}</span>`).join('')}</p>
        <div class="article-detail-actions">${likeButton(article)}</div>
        ${renderArticleContent(article.content, article.attachments || [])}
      `;
    }
    async function loadComments() {
      const comments = await api('/articles/' + id + '/comments');
      $('comment-list').innerHTML = comments.map(c => `
        <div class="comment-item ${c.parentId ? 'nested-comment' : ''}">
          <p>${escapeHtml(c.content)}</p>
          <p class="comment-meta">#${c.id}${c.parentId ? ' 回复 #' + c.parentId : ''} ${escapeHtml(c.nickname)} | ${formatDate(c.createdAt)} | ${escapeHtml(reviewText(c.aiReviewResult))}</p>
          <button type="button" class="secondary" data-reply="${c.id}">回复</button>
        </div>
      `).join('') || '<p class="blog-muted">暂无已通过评论。</p>';
    }
    $('article-detail').addEventListener('click', async function (event) {
      const likeButtonEl = event.target.closest('[data-like-article]');
      if (likeButtonEl) {
        try {
          const liked = likeButtonEl.getAttribute('data-liked') === 'true';
          likeButtonEl.disabled = true;
          await api('/articles/' + likeButtonEl.getAttribute('data-like-article') + '/like', { method: liked ? 'DELETE' : 'POST' });
          await loadDetail();
        } catch (error) {
          alert(error.message);
          likeButtonEl.disabled = false;
        }
        return;
      }
      const link = event.target.closest('[data-article-toc]');
      if (!link) return;
      const target = document.querySelector(link.getAttribute('href'));
      if (target) {
        event.preventDefault();
        target.scrollIntoView({ behavior: 'smooth', block: 'start' });
      }
    });
    $('comment-list').addEventListener('click', function (event) {
      const replyId = event.target.getAttribute('data-reply');
      if (!replyId) return;
      $('comment-parent-id').value = replyId;
      $('comment-content').focus();
    });
    $('comment-form').addEventListener('submit', async function (event) {
      event.preventDefault();
      try {
        await api('/articles/' + id + '/comments', {
          method: 'POST',
          body: JSON.stringify({ content: $('comment-content').value, parentId: $('comment-parent-id').value ? Number($('comment-parent-id').value) : null })
        });
        $('comment-content').value = '';
        $('comment-parent-id').value = '';
        alert('评论已提交，等待管理员审核后显示。');
      } catch (error) {
        alert(error.message);
      }
    });
    $('qa-btn').addEventListener('click', async function () {
      try {
        const data = await api('/ai/qa', { method: 'POST', body: JSON.stringify({ question: $('qa-question').value }) });
        renderAiResult('qa-output', data);
      } catch (error) {
        $('qa-output').textContent = error.message;
      }
    });
    try { await loadDetail(); await loadComments(); } catch (error) { $('article-detail').textContent = error.message; }
  }

  async function initAdminPage() {
    const user = await requireAdmin();
    applyRoleNavigation(user);
    $('admin-user-summary').textContent = `当前管理员：${userLine(user)}。你拥有文章审核、评论审核、用户封禁、分类标签和 AI 日志管理权限。`;

    async function refresh() {
      try {
        show('admin-status', '正在加载后台数据...');
        const stats = await api('/admin/stats');
        const users = await api('/admin/users');
        const articles = await api('/admin/articles');
        const comments = await api('/admin/comments');
        const aiLogs = await api('/admin/ai-logs');
        await loadTaxonomy();
        const statLabels = {
          userCount: '用户数', articleCount: '文章数', commentCount: '评论数', categoryCount: '分类数',
          tagCount: '标签数', totalViews: '总阅读量', aiUsageCount: 'AI 调用次数'
        };
        $('admin-stats').innerHTML = Object.keys(statLabels).map(key => `<div class="stat-card"><strong>${statLabels[key]}</strong><br>${stats[key] || 0}</div>`).join('');
        $('admin-users').innerHTML = users.map(u => `<div class="admin-row">
          <strong>#${u.id} ${escapeHtml(u.username)}</strong>
          <span>${escapeHtml(u.nickname)} | ${roleText(u.role)} | <span class="status-pill ${u.banned ? 'danger' : 'success'}">${u.banned ? '已封禁' : '正常'}</span> | 注册时间 ${formatDate(u.createdAt)}</span>
          <div class="admin-row-actions">
            <button type="button" class="secondary" data-user-role="${u.id}" data-role="${u.role === 'ADMIN' ? 'USER' : 'ADMIN'}">设为${u.role === 'ADMIN' ? '普通用户' : '管理员'}</button>
            <button type="button" class="${u.banned ? 'secondary' : 'danger'}" data-user-ban="${u.id}" data-banned="${u.banned ? 'false' : 'true'}">${u.banned ? '解封用户' : '封禁用户'}</button>
            <button type="button" class="danger" data-delete-user="${u.id}">删除用户</button>
          </div>
        </div>`).join('') || '<p class="blog-muted">暂无用户。</p>';
        $('admin-articles').innerHTML = articles.map(a => `<div class="admin-row">
          <strong>#${a.id} ${escapeHtml(a.title)}</strong>
          <span><span class="status-pill ${statusClass(a.status)}">${statusText(a.status)}</span> | 作者 ${escapeHtml(a.authorName)} | ${articleStats(a)} | 附件 ${(a.attachments || []).length}</span>
          <small>${escapeHtml(textPreview(a.summary || a.content, 150))}</small>
          ${renderCompactAttachments(a.attachments)}
          <div class="admin-row-actions">
            <a class="blog-link-btn" href="/article.html?id=${a.id}">查看</a>
            <button type="button" data-article-id="${a.id}" data-article-status="PUBLISHED">通过上架</button>
            <button type="button" class="secondary" data-article-id="${a.id}" data-article-status="PENDING">退回待审</button>
            <button type="button" class="danger" data-article-id="${a.id}" data-article-status="REJECTED">驳回</button>
            <button type="button" class="danger" data-admin-delete-article="${a.id}">删除文章</button>
          </div>
        </div>`).join('') || '<p class="blog-muted">暂无文章。</p>';
        $('admin-comments').innerHTML = comments.map(c => `<div class="admin-row">
          <strong>#${c.id} ${escapeHtml(c.articleTitle || '未知文章')}</strong>
          <span><span class="status-pill ${statusClass(c.status)}">${statusText(c.status)}</span> | 用户 #${c.userId} ${escapeHtml(c.nickname)} | ${formatDate(c.createdAt)}</span>
          <p>${escapeHtml(c.content)}</p>
          <small>${escapeHtml(reviewText(c.aiReviewResult))}</small>
          <div class="admin-row-actions">
            <button type="button" data-comment-id="${c.id}" data-comment-status="APPROVED">通过评论</button>
            <button type="button" class="secondary" data-comment-id="${c.id}" data-comment-status="PENDING">设为待审</button>
            <button type="button" class="danger" data-comment-id="${c.id}" data-comment-status="REJECTED">下架评论</button>
            <button type="button" class="danger" data-comment-delete="${c.id}">删除评论</button>
            <button type="button" class="danger" data-comment-ban-user="${c.userId}">封禁该用户</button>
          </div>
        </div>`).join('') || '<p class="blog-muted">暂无评论。</p>';
        $('admin-categories').innerHTML = state.categories.map(c => `<div class="admin-row"><span>#${c.id} ${escapeHtml(c.name)} - ${escapeHtml(c.description || '')}</span><button type="button" class="danger" data-delete-category="${c.id}">删除分类</button></div>`).join('') || '<p class="blog-muted">暂无分类。</p>';
        $('admin-tags').innerHTML = state.tags.map(t => `<span class="tag-pill">#${t.id} ${escapeHtml(t.name)} <button type="button" class="danger tiny-btn" data-delete-tag="${t.id}">删除</button></span>`).join('') || '<p class="blog-muted">暂无标签。</p>';
        $('admin-ai-logs').innerHTML = aiLogs.map(log => `<div class="admin-row">
          <strong>#${log.id} ${escapeHtml(featureText(log.feature))}</strong>
          <span>${formatDate(log.createdAt)}</span>
          <small>输入：${escapeHtml(textPreview(log.prompt, 120))}</small>
          <small>输出：${escapeHtml(textPreview(log.result, 160))}</small>
        </div>`).join('') || '<p class="blog-muted">暂无 AI 使用记录。</p>';
        show('admin-status', '后台数据已刷新');
        showAdminReviewNotice(articles, comments);
      } catch (error) {
        show('admin-status', error.message);
      }
    }

    $('admin-refresh-btn').addEventListener('click', refresh);
    $('logout-btn').addEventListener('click', function () {
      clearSession();
      location.href = '/login.html';
    });
    $('admin-users').addEventListener('click', async function (event) {
      const roleUserId = event.target.getAttribute('data-user-role');
      const nextRole = event.target.getAttribute('data-role');
      const banUserId = event.target.getAttribute('data-user-ban');
      const banned = event.target.getAttribute('data-banned');
      const deleteUserId = event.target.getAttribute('data-delete-user');
      try {
        if (roleUserId && confirm('确认修改该用户角色吗？')) {
          await api('/admin/users/' + roleUserId + '/role?role=' + encodeURIComponent(nextRole), { method: 'PUT' });
          await refresh();
        }
        if (banUserId && confirm(banned === 'true' ? '确认封禁该用户吗？封禁后不能登录和操作。' : '确认解封该用户吗？')) {
          await api('/admin/users/' + banUserId + '/ban?banned=' + banned, { method: 'PUT' });
          await refresh();
        }
        if (deleteUserId && confirm('确认删除该用户吗？已有文章或评论的用户不能直接删除。')) {
          await api('/admin/users/' + deleteUserId, { method: 'DELETE' });
          await refresh();
        }
      } catch (error) { show('admin-status', error.message); }
    });
    $('admin-articles').addEventListener('click', async function (event) {
      const articleId = event.target.getAttribute('data-article-id');
      const nextStatus = event.target.getAttribute('data-article-status');
      const deleteId = event.target.getAttribute('data-admin-delete-article');
      try {
        if (articleId && nextStatus) {
          await api('/admin/articles/' + articleId + '/status?status=' + encodeURIComponent(nextStatus), { method: 'PUT' });
          await refresh();
        }
        if (deleteId && confirm('确认删除这篇文章吗？')) {
          await api('/articles/' + deleteId, { method: 'DELETE' });
          await refresh();
        }
      } catch (error) { show('admin-status', error.message); }
    });
    $('admin-comments').addEventListener('click', async function (event) {
      const commentId = event.target.getAttribute('data-comment-id');
      const status = event.target.getAttribute('data-comment-status');
      const del = event.target.getAttribute('data-comment-delete');
      const banUserId = event.target.getAttribute('data-comment-ban-user');
      try {
        if (commentId && status) await api('/admin/comments/' + commentId + '/moderate?status=' + encodeURIComponent(status), { method: 'PUT' });
        if (del && confirm('确认删除这条评论吗？')) await api('/admin/comments/' + del, { method: 'DELETE' });
        if (banUserId && confirm('确认封禁这条评论的发布用户吗？')) await api('/admin/users/' + banUserId + '/ban?banned=true', { method: 'PUT' });
        await refresh();
      } catch (error) { show('admin-status', error.message); }
    });
    $('admin-categories').addEventListener('click', async function (event) {
      const id = event.target.getAttribute('data-delete-category');
      if (!id || !confirm('确认删除该分类吗？')) return;
      try { await api('/categories/' + id, { method: 'DELETE' }); await refresh(); }
      catch (error) { show('admin-status', error.message); }
    });
    $('admin-tags').addEventListener('click', async function (event) {
      const id = event.target.getAttribute('data-delete-tag');
      if (!id || !confirm('确认删除该标签吗？')) return;
      try { await api('/tags/' + id, { method: 'DELETE' }); await refresh(); }
      catch (error) { show('admin-status', error.message); }
    });
    $('category-form').addEventListener('submit', async function (event) {
      event.preventDefault();
      try {
        await api('/categories', { method: 'POST', body: JSON.stringify({ name: $('category-name').value, description: $('category-description').value }) });
        event.target.reset();
        await refresh();
      } catch (error) { show('admin-status', error.message); }
    });
    $('tag-form').addEventListener('submit', async function (event) {
      event.preventDefault();
      try {
        await api('/tags', { method: 'POST', body: JSON.stringify({ name: $('tag-name').value }) });
        event.target.reset();
        await refresh();
      } catch (error) { show('admin-status', error.message); }
    });
    refresh();
  }

  document.addEventListener('DOMContentLoaded', function () {
    applyRoleNavigation(storedUser());
    if ($('login-page')) initLoginPage();
    if ($('blog-page')) initBlogPage();
    if ($('article-page')) initArticlePage();
    if ($('admin-page')) initAdminPage();
  });
}());