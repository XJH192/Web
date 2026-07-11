(function () {
  const API_BASE = window.BLOG_API_BASE || 'http://127.0.0.1:8080/api';
  const state = { categories: [], tags: [], currentUser: null, editorAttachments: [], homeArticles: [], homePage: 1, homePageSize: 4, homeYear: '', adminAiLogs: [], adminAiLogPage: 1, adminAiLogPageSize: 5, adminAiLogKeyword: '', adminAiLogFeature: '' };

  function $(id) { return document.getElementById(id); }
  function token() { return localStorage.getItem('blogToken') || ''; }
  function storedUser() {
    if (!token()) return null;
    try { return JSON.parse(localStorage.getItem('blogUser') || 'null'); }
    catch (error) { return null; }
  }
  function setSession(loginResponse) {
    sessionStorage.removeItem('cialloGuestMode');
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
  function isGuestBrowsing(user) {
    return !user && !token();
  }
  function guestLoginHref() {
    return '/login.html?redirect=' + encodeURIComponent(location.pathname + location.search);
  }
  function guestActionHint(action) {
    return `游客只能浏览公开内容，${action || '操作'}需要先登录。`;
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
      AI_CATEGORY: 'AI 分类推荐',
      AI_ARTICLE_REVIEW: 'AI 文章审核',
      AI_REMOTE_ERROR: 'AI 远程调用错误',
      AI_COMMENT_REVIEW: 'AI 评论审核',
      AI_QA: 'AI 博客问答'
    };
    return map[feature] || feature || '';
  }
  function reviewText(value) {
    if (!value) return '未审核';
    if (value.indexOf('PASS') === 0) return value.replace('PASS:', 'AI 初审通过：');
    if (value.indexOf('REVIEW') === 0) return value.replace('REVIEW:', 'AI 标记风险，等待人工判断：');
    if (value.indexOf('REJECT') === 0) return value.replace('REJECT:', '历史 AI 拒绝记录：');
    if (value.indexOf('SKIP') === 0) return value.replace('SKIP:', '未执行 AI 初审：');
    if (value.indexOf('MANUAL') === 0) return value.replace('MANUAL:', '人工处理：');
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
  function userProfileLink(userId, label, className) {
    if (!userId) return escapeHtml(label || '用户');
    return `<a class="${escapeHtml(className || 'user-profile-link')}" href="/user.html?id=${encodeURIComponent(userId)}">${escapeHtml(label || '用户')}</a>`;
  }
  function safeGalleryImageUrl(value) {
    const url = String(value || '');
    if (/^data:image\/(jpeg|png|webp|gif);base64,/i.test(url)) return url;
    if (/^\/images\/[a-z0-9_./-]+$/i.test(url)) return url;
    return '#';
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
    if (!user) {
      setLinksByHref('/blog.html', true);
      setLinksByHref('/admin.html', false);
      setLinksByHref('/memos.html', false);
      setLinksByHref('/messages.html', false);
      document.querySelectorAll('.admin-only-link').forEach(el => { el.style.display = 'none'; });
      document.querySelectorAll('.user-only-link').forEach(el => { el.style.display = 'inline-block'; });
      return;
    }
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
  function articleYear(value) {
    const match = String(value || '').match(/^\d{4}/);
    return match ? match[0] : '未归档';
  }
  function countBy(items, getter) {
    const counts = new Map();
    (items || []).forEach(function (item) {
      const key = getter(item);
      if (!key) return;
      counts.set(key, (counts.get(key) || 0) + 1);
    });
    return counts;
  }
  function renderSiteMap() {
    const stats = $('site-map-stats');
    if (!stats) return;
    const articles = state.homeArticles || [];
    const categoryCounts = countBy(articles, item => item.categoryName || '未分类');
    const tagCounts = new Map();
    articles.forEach(function (article) {
      (article.tagNames || []).forEach(function (name) {
        if (name) tagCounts.set(name, (tagCounts.get(name) || 0) + 1);
      });
    });
    const yearCounts = countBy(articles, item => articleYear(item.createdAt));
    stats.innerHTML = [
      ['已上架文章', articles.length],
      ['文章分类', categoryCounts.size || state.categories.length],
      ['文章标签', tagCounts.size || state.tags.length],
      ['归档年份', yearCounts.size]
    ].map(item => `<div class="stat-card"><strong>${item[0]}</strong><br>${item[1]}</div>`).join('');

    const categoryBox = $('site-category-cloud');
    if (categoryBox) {
      const categories = state.categories.map(c => ({ id: c.id, name: c.name, count: categoryCounts.get(c.name) || 0 }))
        .filter(item => item.count > 0 || state.categories.length <= 8);
      categoryBox.innerHTML = categories.length ? categories.map(item => `<button type="button" class="site-chip" data-site-category="${item.id}">${escapeHtml(item.name)} <span>${item.count}</span></button>`).join('') : '<span class="blog-muted">暂无分类。</span>';
    }

    const tagBox = $('site-tag-cloud');
    if (tagBox) {
      const tags = state.tags.map(t => ({ id: t.id, name: t.name, count: tagCounts.get(t.name) || 0 }))
        .filter(item => item.count > 0 || state.tags.length <= 10)
        .sort((a, b) => b.count - a.count || a.name.localeCompare(b.name, 'zh-Hans-CN'));
      tagBox.innerHTML = tags.length ? tags.map(item => `<button type="button" class="site-chip" data-site-tag="${item.id}">#${escapeHtml(item.name)} <span>${item.count}</span></button>`).join('') : '<span class="blog-muted">暂无标签。</span>';
    }

    const archiveBox = $('site-archive-list');
    if (archiveBox) {
      const years = Array.from(yearCounts.entries()).sort((a, b) => String(b[0]).localeCompare(String(a[0])));
      archiveBox.innerHTML = years.length ? years.map(item => `<button type="button" class="archive-link ${state.homeYear === item[0] ? 'is-active' : ''}" data-site-year="${escapeHtml(item[0])}">${escapeHtml(item[0])}<span>${item[1]}</span></button>`).join('') : '<span class="blog-muted">暂无归档。</span>';
    }
  }
  function renderTaxonomyControls() {
    const controls = [
      ['home-category-filter', categoryOptions(null, '全部分类')],
      ['home-tag-filter', tagOptions(null, '全部标签')],
      ['blog-category-filter', categoryOptions(null, '全部分类')],
      ['blog-tag-filter', tagOptions(null, '全部标签')],
      ['article-category', categoryOptions(null, '选择已有分类')]
    ];
    controls.forEach(function (item) {
      const el = $(item[0]);
      if (el) el.innerHTML = item[1];
    });
  }
  function setCategoryInput(name) {
    const value = String(name || '').trim();
    const select = $('article-category');
    const custom = $('article-category-custom');
    const match = state.categories.find(function (item) { return item.name && item.name.toLowerCase() === value.toLowerCase(); });
    if (select) select.value = match ? match.id : '';
    if (custom) custom.value = match ? '' : value;
  }
  function selectedCategoryName() {
    const input = $('article-category-custom');
    const custom = input ? input.value.trim() : '';
    if (custom) return custom;
    const select = $('article-category');
    if (select && select.value && select.selectedIndex >= 0) {
      const option = select.options[select.selectedIndex];
      return option ? option.textContent.trim() : '';
    }
    return '';
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
  function aiInstruction() {
    const input = $('article-ai-instruction');
    return input ? input.value.trim() : '';
  }
  function aiPayload() {
    return {
      title: $('article-title').value,
      content: $('article-content').value,
      instruction: aiInstruction(),
      categoryName: selectedCategoryName(),
      tagNames: selectedTagNames()
    };
  }
  function articleEditorPayload(statusOverride) {
    const selectedCategory = $('article-category') ? $('article-category').value : '';
    return {
      title: $('article-title').value,
      summary: $('article-summary').value,
      content: $('article-content').value,
      categoryId: selectedCategory ? Number(selectedCategory) : null,
      categoryName: selectedCategoryName(),
      tagIds: selectedTagIds(),
      tagNames: selectedTagNames(),
      attachments: normalizeAttachments(state.editorAttachments),
      status: statusOverride || ($('article-status') ? $('article-status').value : 'PUBLISHED')
    };
  }
  function resetArticleEditorForm() {
    const form = $('article-form');
    if (form) form.reset();
    if ($('article-id')) $('article-id').value = '';
    if ($('article-status')) $('article-status').value = 'PUBLISHED';
    setCategoryInput('');
    setTagInput([]);
    state.editorAttachments = [];
    renderAttachments();
    if ($('ai-output')) $('ai-output').innerHTML = '';
  }
  function selectedTagIds() { return []; }
  function renderTagPreview() {
    const box = $('article-tags');
    if (!box) return;
    const names = selectedTagNames();
    box.innerHTML = names.length
      ? names.map(name => `<span class="tag-pill">${escapeHtml(name)}</span>`).join('')
      : '<span class="blog-muted">暂未填写标签，可手动输入或点击 AI 推荐。</span>';
    try { window.dispatchEvent(new CustomEvent('ciallo:editor-tags', { detail: names })); } catch (error) { }
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
  function followButton(article) {
    const currentUser = state.currentUser || storedUser();
    if (!currentUser || String(currentUser.id) === String(article.authorId)) return '';
    const following = !!article.authorFollowedByCurrentUser;
    const mutual = !!article.mutualFollowWithAuthor;
    const label = mutual ? '已互关' : (following ? '已关注' : '关注');
    return `<button type="button" class="secondary follow-button ${following ? 'is-following' : ''} ${mutual ? 'is-mutual' : ''}" data-follow-user="${article.authorId}" data-following="${following ? 'true' : 'false'}" aria-pressed="${following ? 'true' : 'false'}">${label}</button>`;
  }
  function authorSocialRow(article) {
    return `<div class="article-author-row">
      <span class="article-meta">作者：${userProfileLink(article.authorId, article.authorName, 'article-author-link')} · <span class="follower-count">粉丝 ${article.authorFollowerCount || 0}</span></span>
      ${followButton(article)}
    </div>`;
  }
  function mergeAuthorFollowStatus(article, status) {
    return Object.assign({}, article, {
      authorFollowerCount: status.followerCount || 0,
      authorFollowedByCurrentUser: !!status.followedByCurrentUser,
      mutualFollowWithAuthor: !!status.mutualFollow
    });
  }
  function commentLikeButton(comment, articleId) {
    if (!state.currentUser && !storedUser()) return '';
    const liked = !!comment.likedByCurrentUser;
    return `<button type="button" class="secondary comment-like-btn ${liked ? 'is-liked' : ''}" data-like-comment="${comment.id}" data-comment-liked="${liked ? 'true' : 'false'}" data-comment-article="${articleId || comment.articleId}" aria-pressed="${liked ? 'true' : 'false'}">${liked ? '已赞' : '点赞'} ${comment.likeCount || 0}</button>`;
  }
  function buildCommentTree(comments) {
    const items = (comments || []).slice().sort((a, b) => Number(a.id || 0) - Number(b.id || 0));
    const byId = new Map();
    const roots = [];
    items.forEach(function (comment) {
      byId.set(String(comment.id), Object.assign({}, comment, { children: [] }));
    });
    byId.forEach(function (comment) {
      const parentId = comment.parentId == null ? '' : String(comment.parentId);
      const parent = parentId ? byId.get(parentId) : null;
      if (parent && parent.id !== comment.id) parent.children.push(comment);
      else roots.push(comment);
    });
    return roots;
  }
  function renderCommentItem(comment, depth, articleId, allowReply) {
    const children = comment.children || [];
    const replyText = comment.parentId ? ` 回复 #${comment.parentId}` : '';
    const replyButton = allowReply ? `<button type="button" class="secondary" data-reply="${comment.id}" data-reply-name="${escapeHtml(comment.nickname || '')}">回复</button>` : '';
    return `<div class="comment-item ${depth ? 'nested-comment' : ''}" data-comment-id="${comment.id}">
      <p>${escapeHtml(comment.content)}</p>
      <p class="comment-meta">#${comment.id}${replyText} ${userProfileLink(comment.userId, comment.nickname, 'comment-author-link')} | ${formatDate(comment.createdAt)} | ${escapeHtml(reviewText(comment.aiReviewResult))}</p>
      <div class="comment-actions">${commentLikeButton(comment, articleId)}${replyButton}</div>
      ${children.length ? `<div class="comment-children">${children.map(child => renderCommentItem(child, depth + 1, articleId, allowReply)).join('')}</div>` : ''}
    </div>`;
  }
  function renderCommentTree(comments, articleId, allowReply) {
    const roots = buildCommentTree(comments);
    return roots.length ? roots.map(item => renderCommentItem(item, 0, articleId, allowReply)).join('') : '<p class="blog-muted">暂无已通过评论。</p>';
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
        const link = item.link ? `<a href="${escapeHtml(item.link)}" data-notice-link>查看</a>` : '';
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
      const noticeLink = event.target.closest('[data-notice-link]');
      if (noticeLink) {
        event.preventDefault();
        const href = noticeLink.getAttribute('href');
        modal.removeEventListener('click', handleClick);
        closeModal();
        const url = new URL(href, window.location.origin);
        if (url.pathname === window.location.pathname && url.hash) {
          history.pushState(null, '', url.hash);
          const target = document.querySelector(url.hash);
          if (target) target.scrollIntoView({ behavior: 'smooth', block: 'start' });
        } else {
          window.location.href = url.pathname + url.search + url.hash;
        }
        return;
      }
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
    await updateNotificationIndicators();
  }
  function ensureNotificationBadgeStyle() {
    if (document.getElementById('ciallo-notification-badge-style')) return;
    const style = document.createElement('style');
    style.id = 'ciallo-notification-badge-style';
    style.textContent = `
      .sidebar-notification-label {
        position: relative;
        display: inline-block;
      }
      .sidebar-notification-dot {
        position: absolute;
        display: block;
        left: 100%;
        top: 50%;
        width: 8px;
        height: 8px;
        margin-left: 8px;
        transform: translateY(-50%);
        border-radius: 999px;
        background: #d92d20;
        box-shadow: 0 0 0 3px rgba(217,45,32,.12);
      }
      .sidebar-notification-dot.is-hidden { display: none; }
    `;
    document.head.appendChild(style);
  }
  function markNotificationMenu(count) {
    ensureNotificationBadgeStyle();
    document.querySelectorAll('[data-notification-entry]').forEach(function (link) {
      const dot = link.querySelector('.sidebar-notification-dot');
      if (!dot) return;
      if (count > 0) {
        dot.classList.remove('is-hidden');
        dot.setAttribute('aria-hidden', 'false');
        link.setAttribute('title', '动态与通知：' + count + ' 条未读消息');
      } else {
        dot.classList.add('is-hidden');
        dot.setAttribute('aria-hidden', 'true');
        link.setAttribute('title', '动态');
      }
    });
  }
  const APPROVED_NOTIFICATION_TYPES = [
    'ARTICLE_LIKED',
    'ARTICLE_REJECTED',
    'COMMENT_REJECTED',
    'ARTICLE_COMMENTED',
    'COMMENT_REPLIED',
    'COMMENT_LIKED',
    'USER_FOLLOWED',
    'USER_ARTICLE_PUBLISHED',
    'PRIVATE_MESSAGE',
    'ARTICLE_DELETED'
  ];
  function approvedNotifications(items) {
    return (Array.isArray(items) ? items : []).filter(function (item) {
      return item && APPROVED_NOTIFICATION_TYPES.indexOf(item.type) >= 0;
    });
  }
  async function unreadNotifications() {
    if (!token()) return [];
    try {
      return approvedNotifications(await api('/notifications?unread=true'));
    } catch (error) {
      return [];
    }
  }
  async function updateNotificationIndicators() {
    const unread = await unreadNotifications();
    const count = unread.length || 0;
    markNotificationMenu(count);
    const countEl = $('notification-unread-count');
    if (countEl) countEl.textContent = count > 0 ? count + ' 条未读' : '暂无未读';
    return count;
  }
  function notificationLink(item) {
    if (item.type === 'USER_FOLLOWED') return '';
    return item.link
      ? `<a href="${escapeHtml(item.link)}" data-notification-id="${escapeHtml(item.id)}" data-notification-read="${item.readFlag ? 'true' : 'false'}">查看</a>`
      : '';
  }
  function notificationContent(item) {
    if (item.actorUserId && item.actorUsername) {
      const content = String(item.content || '');
      const suffix = content.indexOf(item.actorUsername) === 0 ? content.slice(String(item.actorUsername).length) : (' ' + content);
      return `<a class="notification-user-link" href="/user.html?id=${encodeURIComponent(item.actorUserId)}" data-notification-id="${escapeHtml(item.id)}" data-notification-read="${item.readFlag ? 'true' : 'false'}">${escapeHtml(item.actorUsername)}</a>${escapeHtml(suffix)}`;
    }
    return escapeHtml(item.content || '');
  }
  function renderNotificationList(items) {
    const list = $('notification-list');
    if (!list) return;
    const rows = approvedNotifications(items);
    list.innerHTML = rows.length ? rows.map(function (item) {
      return `<article class="notification-card ${item.readFlag ? '' : 'is-unread'}">
        <div>
          <strong>${escapeHtml(item.title || '新消息')}</strong>
        </div>
        <p>${notificationContent(item)}</p>
        <small>${formatDate(item.createdAt)} ${notificationLink(item)}</small>
      </article>`;
    }).join('') : '<p class="notification-empty">暂无通知。</p>';
  }
  async function loadNotificationPanel() {
    const status = $('notification-status');
    try {
      if (!token()) {
        if (status) status.textContent = '登录后可查看评论、回复、关注动态、点赞和私信通知。';
        renderNotificationList([]);
        markNotificationMenu(0);
        return;
      }
      if (status) status.textContent = '正在加载通知...';
      const items = await api('/notifications?unread=false');
      renderNotificationList(items || []);
      const count = await updateNotificationIndicators();
      if (status) status.textContent = count > 0 ? '有新的未读互动通知。' : '暂无新的互动通知。';
    } catch (error) {
      if (status) status.textContent = error.message;
    }
  }
  function initNotificationPage() {
    loadNotificationPanel();
    const refresh = $('notification-refresh');
    const markRead = $('notification-mark-read');
    const list = $('notification-list');
    if (refresh) refresh.addEventListener('click', loadNotificationPanel);
    if (list) list.addEventListener('click', async function (event) {
      const link = event.target.closest('a[data-notification-id]');
      if (!link || !list.contains(link)) return;
      event.preventDefault();
      const destination = link.href;
      const notificationId = link.getAttribute('data-notification-id');
      try {
        if (notificationId && link.getAttribute('data-notification-read') !== 'true') {
          await api('/notifications/' + encodeURIComponent(notificationId) + '/read', { method: 'PUT' });
        }
      } catch (error) {
        console.warn('标记通知已读失败：', error);
      } finally {
        window.location.href = destination;
      }
    });
    if (markRead) markRead.addEventListener('click', async function () {
      const status = $('notification-status');
      try {
        if (status) status.textContent = '正在标记已读...';
        await api('/notifications/read', { method: 'PUT' });
        await loadNotificationPanel();
      } catch (error) {
        if (status) status.textContent = error.message;
      }
    });
  }
  function showAdminReviewNotice(articles, comments) {
    if (sessionStorage.getItem('cialloAdminReviewNoticeShown')) return;
    const pendingArticles = (articles || []).filter(item => item.status === 'PENDING');
    const pendingComments = (comments || []).filter(item => item.status === 'PENDING');
    const items = pendingArticles.slice(0, 4).map(item => ({ title: '待审核博客', content: `《${item.title}》等待审核上架`, link: '/admin.html#articles', createdAt: item.createdAt }))
      .concat(pendingComments.slice(0, 4).map(item => ({ title: '待审核评论', content: `${item.nickname || '用户'} 评论了《${item.articleTitle || '文章'}》`, link: '/admin.html#comments', createdAt: item.createdAt })));
    if (!items.length) return;
    sessionStorage.setItem('cialloAdminReviewNoticeShown', '1');
    showNoticeModal('后台有待审核内容', items);
  }
  function cleanArticleHeading(value) {
    return String(value || '')
      .replace(/<[^>]+>/g, '')
      .replace(/!\[([^\]]*)\]\([^)]+\)/g, '$1')
      .replace(/\[([^\]]+)\]\([^)]+\)/g, '$1')
      .replace(/^[*_`~\s]+|[*_`~#\s]+$/g, '')
      .trim();
  }

  function detectArticleHeading(lines, index) {
    const text = String(lines[index] || '').trim();
    if (!text) return null;
    const next = String(lines[index + 1] || '').trim();
    const markdown = text.match(/^(#{1,6})\s*(.+?)\s*#*$/);
    if (markdown) return { title: cleanArticleHeading(markdown[2]), level: Math.min(4, markdown[1].length + 1), consume: 1 };
    const html = text.match(/^<h([1-6])(?:\s[^>]*)?>([\s\S]*?)<\/h\1>$/i);
    if (html) return { title: cleanArticleHeading(html[2]), level: Math.min(4, Number(html[1]) + 1), consume: 1 };
    if (/^(?:=+|-+)$/.test(next) && next.length >= 3) {
      return { title: cleanArticleHeading(text), level: next.charAt(0) === '=' ? 2 : 3, consume: 2 };
    }
    const arabic = text.match(/^(\d+(?:\.\d+)*)(?:[、.)．]\s*|\s+)(.+)$/);
    if (arabic) {
      return { title: cleanArticleHeading(arabic[2]), level: Math.min(4, 1 + arabic[1].split('.').length), consume: 1 };
    }
    const chinese = text.match(/^(第[一二三四五六七八九十百千万0-9]+[章节部分篇]|[一二三四五六七八九十百]+[、.．]|[（(][一二三四五六七八九十百0-9]+[）)])\s*(.+)$/);
    if (chinese) {
      const nested = /^[（(]/.test(chinese[1]);
      return { title: cleanArticleHeading(chinese[2]), level: nested ? 3 : 2, consume: 1 };
    }
    const bold = text.match(/^(?:\*\*|__)(.+?)(?:\*\*|__)$/);
    if (bold) return { title: cleanArticleHeading(bold[1]), level: 2, consume: 1 };
    if (/^.{2,32}[：:]$/.test(text)) return { title: cleanArticleHeading(text.replace(/[：:]$/, '')), level: 2, consume: 1 };
    const previousBlank = index === 0 || !String(lines[index - 1] || '').trim();
    const nextBlank = index === lines.length - 1 || !next;
    if (previousBlank && nextBlank && text.length <= 32 && !/[。！？.!?；;，,]$/.test(text)) {
      return { title: cleanArticleHeading(text), level: 2, consume: 1 };
    }
    return null;
  }

  function renderArticleContent(content, attachments) {
    const lines = String(content || '').split(/\r?\n/);
    const toc = [];
    const parts = [];
    const paragraphs = [];
    for (let index = 0; index < lines.length;) {
      const line = lines[index];
      const text = line.trim();
      if (!text) {
        index += 1;
        continue;
      }
      const heading = detectArticleHeading(lines, index);
      if (heading && heading.title) {
        const id = 'article-section-' + toc.length;
        toc.push({ id: id, title: heading.title, level: heading.level });
        parts.push(`<h${heading.level} id="${id}">${escapeHtml(heading.title)}</h${heading.level}>`);
        index += heading.consume;
        continue;
      }
      const partIndex = parts.length;
      parts.push(`<p>${escapeHtml(line)}</p>`);
      paragraphs.push({ partIndex: partIndex, text: cleanArticleHeading(text) });
      index += 1;
    }
    if (!parts.length) parts.push('<p class="blog-muted">暂无正文内容。</p>');
    if (!toc.length) {
      const candidates = paragraphs.filter(item => item.text.length >= 6).slice(0, 8);
      if (candidates.length >= 2) {
        candidates.forEach(function (item, candidateIndex) {
          const id = 'article-paragraph-' + candidateIndex;
          const title = textPreview(item.text, 28);
          toc.push({ id: id, title: title, level: 2 });
          parts[item.partIndex] = parts[item.partIndex].replace('<p>', `<p id="${id}">`);
        });
      } else {
        toc.push({ id: 'article-body-start', title: '正文内容', level: 2 });
        parts.unshift('<h2 id="article-body-start">正文内容</h2>');
      }
    }
    const attachmentHtml = renderArticleAttachments(attachments);
    if (attachmentHtml) toc.push({ id: 'article-attachments-section', title: '文章附件', level: 2 });
    const tocHtml = `<nav class="article-inline-toc" aria-label="文章目录"><strong>文章目录</strong>${toc.map(item => `<a class="toc-level-${item.level || 2}" href="#${item.id}" data-article-toc>${escapeHtml(item.title)}</a>`).join('')}</nav>`;
    return `<div class="article-detail-layout"><div class="article-content-body">${parts.join('')}${attachmentHtml}</div>${tocHtml}</div>`;
  }

  function renderAiResult(targetId, data) {
    const target = $(targetId);
    if (!target) return;
    if (data.error) {
      target.innerHTML = `<div class="ai-error-card"><strong>DeepSeek 调用失败</strong><p>${escapeHtml(data.error)}</p>${data.thinking ? `<small>${escapeHtml(textPreview(data.thinking, 320))}</small>` : ''}</div>`;
      return;
    }
    if (data.outline) {
      target.innerHTML = `<strong>建议大纲</strong><ol>${data.outline.map(item => `<li>${escapeHtml(item.replace(/^\d+\.\s*/, ''))}</li>`).join('')}</ol>`;
      return;
    }
    if (data.summary) {
      target.innerHTML = `<strong>生成摘要</strong><p>${escapeHtml(data.summary)}</p>`;
      return;
    }
    if (data.category) {
      const category = String(data.category || '').trim();
      setCategoryInput(category);
      target.innerHTML = `<strong>推荐分类</strong><p><span class="tag-pill">${escapeHtml(category)}</span></p><p class="blog-muted">已同步到文章分类。若它不是已有分类，保存文章时会自动创建。</p>`;
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
      const sourceItems = Array.isArray(data.sources)
        ? uniqueNames(data.sources.map(item => String(item || '').trim()).filter(Boolean))
        : [];
      const sources = sourceItems.length
        ? `<div class="blog-muted"><strong>依据文章</strong><ul>${sourceItems.map(item => `<li>《${escapeHtml(item)}》</li>`).join('')}</ul></div>`
        : '';
      target.innerHTML = `<strong>回答</strong><p>${escapeHtml(data.answer)}</p>${sources}`;
      return;
    }
    target.textContent = JSON.stringify(data, null, 2);
  }
  async function runArticleAi(path, statusTargetId, outputTargetId) {
    const outputId = outputTargetId || 'ai-output';
    try {
      show(statusTargetId, 'AI 正在生成...');
      const data = await api(path, { method: 'POST', body: JSON.stringify(aiPayload()) });
      renderAiResult(outputId, data);
      show(statusTargetId, data.error ? 'DeepSeek 调用失败，错误已写入 AI 日志' : 'AI 生成完成，记录已写入数据库日志');
    } catch (error) {
      show(statusTargetId, error.message);
      const target = $(outputId);
      if (target) target.textContent = error.message;
    }
  }
  function buildQuery(fields) {
    const params = new URLSearchParams();
    fields.forEach(function (field) {
      const el = $(field.id);
      if (el && el.value && el.value.trim()) params.set(field.name, el.value.trim());
    });
    return params.toString();
  }
  function applyHomeFiltersFromUrl() {
    const params = new URLSearchParams(location.search || '');
    const keyword = params.get('keyword') || '';
    const categoryId = params.get('categoryId') || '';
    const tagId = params.get('tagId') || '';
    const year = params.get('year') || '';
    if (keyword && $('home-keyword')) $('home-keyword').value = keyword;
    if (categoryId && $('home-category-filter')) $('home-category-filter').value = categoryId;
    if (tagId && $('home-tag-filter')) $('home-tag-filter').value = tagId;
    state.homeYear = year;
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
      show('register-status', '正在把小伙伴写入数据库...');
      try {
        const user = await api('/auth/register', {
          method: 'POST',
          body: JSON.stringify({
            username: $('register-username').value.trim(),
            email: $('register-email').value.trim(),
            password: $('register-password').value
          })
        });
        show('register-status', `注册成功：${user.email || user.username}，请在左侧登录`);
        event.target.reset();
      } catch (error) {
        show('register-status', error.message);
      }
    });
  }

  async function initBlogPage() {
    const user = await verifySession();
    const guest = isGuestBrowsing(user);
    if (user && user.role === 'ADMIN') {
      location.href = '/admin.html';
      return;
    }
    applyRoleNavigation(user);
    await loadTaxonomy();
    if (guest) {
      sessionStorage.setItem('cialloGuestMode', '1');
      const kicker = document.querySelector('#blog-dashboard-section .section-kicker');
      const title = document.querySelector('#blog-dashboard-section h2');
      if (kicker) kicker.textContent = '游客浏览';
      if (title) title.textContent = '公开博客浏览';
      if ($('blog-social-counts')) $('blog-social-counts').hidden = true;
      if ($('blog-social-panel')) $('blog-social-panel').hidden = true;
      if ($('blog-manage-section')) $('blog-manage-section').hidden = true;
      if ($('blog-editor-section')) $('blog-editor-section').hidden = true;
      if ($('logout-btn')) $('logout-btn').textContent = '登录 / 注册';
      $('blog-user-summary').textContent = '当前为游客浏览模式：可以查看公开文章、评论、分类、标签、归档、资料卡和相册；点赞、评论、关注、私信、发布文章、管理相册和使用 AI 需要登录。';
      const homeIntro = document.querySelector('#blog-home-section .blog-muted');
      if (homeIntro) homeIntro.textContent = '游客可浏览 AI 初审通过或管理员确认公开的文章；互动操作请先登录。';
    }

    let myFollowStatus = null;
    async function refreshOwnSocialCounts() {
      try {
        myFollowStatus = await api('/users/' + user.id + '/follow-status');
        if ($('blog-following-count')) $('blog-following-count').textContent = myFollowStatus.followingCount || 0;
        if ($('blog-follower-count')) $('blog-follower-count').textContent = myFollowStatus.followerCount || 0;
      } catch (error) {
        myFollowStatus = null;
      }
    }
    if (!guest) {
      await refreshOwnSocialCounts();
      const socialSummary = myFollowStatus ? `粉丝 ${myFollowStatus.followerCount || 0}，关注 ${myFollowStatus.followingCount || 0}。` : '';
      $('blog-user-summary').textContent = `当前用户：${userLine(user)}。${socialSummary}文章和评论会先经过 AI 初审，正常内容直接公开，疑似问题再交管理员判断。`;
    }
    renderTaxonomyControls();
    applyHomeFiltersFromUrl();
    if (!guest) {
      setTagInput([]);
      state.editorAttachments = [];
      renderAttachments();
    }
    const localArticleDraftKey = guest ? '' : 'cialloLocalArticleDraft:' + user.id;

    function localDraftStatus(text) {
      const target = $('local-article-draft-status');
      if (target) target.textContent = text || '';
    }

    function saveLocalArticleDraft(payload, sourceArticleId) {
      const draft = Object.assign({}, payload, {
        sourceArticleId: sourceArticleId || '',
        aiInstruction: $('article-ai-instruction') ? $('article-ai-instruction').value : '',
        savedAt: new Date().toISOString()
      });
      try {
        localStorage.setItem(localArticleDraftKey, JSON.stringify(draft));
        return { attachmentsOmitted: 0 };
      } catch (error) {
        const attachmentCount = (draft.attachments || []).length;
        draft.attachments = [];
        draft.attachmentsOmitted = attachmentCount;
        try {
          localStorage.setItem(localArticleDraftKey, JSON.stringify(draft));
          return { attachmentsOmitted: attachmentCount };
        } catch (fallbackError) {
          throw new Error('浏览器本地存储空间不足，草稿保存失败，请缩短正文或清理浏览器存储。');
        }
      }
    }

    function clearLocalArticleDraft() {
      localStorage.removeItem(localArticleDraftKey);
      localDraftStatus('当前没有本地草稿');
    }

    function restoreLocalArticleDraft() {
      let draft = null;
      try {
        draft = JSON.parse(localStorage.getItem(localArticleDraftKey) || 'null');
      } catch (error) {
        localStorage.removeItem(localArticleDraftKey);
      }
      if (!draft) {
        localDraftStatus('当前没有本地草稿');
        return false;
      }
      $('article-id').value = draft.sourceArticleId || '';
      $('article-title').value = draft.title || '';
      $('article-summary').value = draft.summary || '';
      $('article-content').value = draft.content || '';
      $('article-status').value = 'DRAFT';
      setCategoryInput(draft.categoryName || '');
      setTagInput(draft.tagNames || []);
      if ($('article-ai-instruction') && draft.aiInstruction) {
        $('article-ai-instruction').value = draft.aiInstruction;
      }
      state.editorAttachments = normalizeAttachments(draft.attachments);
      renderAttachments();
      const savedAt = draft.savedAt ? formatDate(draft.savedAt) : '未知时间';
      const attachmentTip = draft.attachmentsOmitted
        ? `；另有 ${draft.attachmentsOmitted} 个过大附件未写入本地，请重新选择`
        : '';
      localDraftStatus(`已恢复本地草稿，保存于 ${savedAt}${attachmentTip}`);
      show('blog-status', '已从当前账号的浏览器本地存储恢复草稿');
      return true;
    }

    if (!guest) restoreLocalArticleDraft();

    function renderSocialUsers(items) {
      return (items || []).map(function (item) {
        const relation = item.ownProfile
          ? '<span class="status-pill success">你自己</span>'
          : (guest ? '<span class="status-pill muted">登录后可关注</span>' : followButton({
              authorId: item.id,
              authorFollowedByCurrentUser: item.followedByCurrentUser,
              mutualFollowWithAuthor: item.mutualFollow
            }));
        const messageLink = (guest || item.ownProfile) ? '' : `<a class="secondary-link social-message-link" href="/messages.html?userId=${item.id}">私信</a>`;
        return `<article class="social-user-card">
          <div>
            <strong>${userProfileLink(item.id, '@' + item.username, 'search-user-link')}</strong>
            <p>${escapeHtml(item.nickname || item.username)} · 粉丝 ${item.followerCount || 0}${item.mutualFollow ? ' · 已互关' : ''}</p>
          </div>
          <div class="social-user-actions">${relation}${messageLink}</div>
        </article>`;
      }).join('') || '<p class="blog-muted">这里暂时还没有用户。</p>';
    }

    function renumberBlogToc() {
      const root = document.querySelector('.nexmoe-toc > .toc');
      if (!root) return;
      let sectionNumber = 0;
      Array.from(root.children).forEach(function (item) {
        if (!item.matches('.toc-item') || item.hidden) return;
        sectionNumber += 1;
        const link = Array.from(item.children).find(function (child) {
          return child.matches('a.toc-link');
        });
        const number = link && link.querySelector('.toc-number');
        if (number) number.textContent = sectionNumber + '.';
        const childList = Array.from(item.children).find(function (child) {
          return child.matches('.toc-child');
        });
        if (!childList) return;
        Array.from(childList.children).forEach(function (childItem, childIndex) {
          const childNumber = childItem.querySelector('.toc-number');
          if (childNumber) childNumber.textContent = sectionNumber + '.' + (childIndex + 1) + '.';
        });
      });
    }

    function syncSocialListToc() {
      const panel = $('blog-social-panel');
      const link = document.querySelector('.nexmoe-toc a.toc-link[href="#blog-social-title"]');
      if (!panel || !link) return;
      const visible = !panel.hidden;
      const item = link.closest('.toc-item');
      link.classList.toggle('social-list-toc-visible', visible);
      link.setAttribute('aria-hidden', visible ? 'false' : 'true');
      link.tabIndex = visible ? 0 : -1;
      if (item) item.hidden = !visible;
      const text = link.querySelector('.toc-text');
      if (visible && text) text.textContent = $('blog-social-title').textContent;
      renumberBlogToc();
    }

    syncSocialListToc();

    async function loadSocialList(kind) {
      const isFollowers = kind === 'followers';
      $('blog-social-title').textContent = isFollowers ? '我的粉丝' : '我的关注';
      $('blog-social-panel').hidden = false;
      syncSocialListToc();
      $('blog-social-list').innerHTML = '<p class="blog-muted">正在加载...</p>';
      const items = await api('/users/' + user.id + '/' + (isFollowers ? 'followers' : 'following'));
      $('blog-social-list').innerHTML = renderSocialUsers(items);
      $('blog-social-panel').scrollIntoView({ behavior: 'smooth', block: 'start' });
    }

    $('blog-social-counts').addEventListener('click', async function (event) {
      const button = event.target.closest('[data-social-list]');
      if (!button) return;
      try {
        await loadSocialList(button.getAttribute('data-social-list'));
      } catch (error) {
        $('blog-social-list').innerHTML = `<p class="blog-muted">${escapeHtml(error.message)}</p>`;
      }
    });
    $('blog-social-close').addEventListener('click', function () {
      $('blog-social-panel').hidden = true;
      syncSocialListToc();
    });
    $('blog-social-list').addEventListener('click', async function (event) {
      const button = event.target.closest('[data-follow-user]');
      if (!button) return;
      try {
        const following = button.getAttribute('data-following') === 'true';
        button.disabled = true;
        await api('/users/' + button.getAttribute('data-follow-user') + '/follow', { method: following ? 'DELETE' : 'POST' });
        await refreshOwnSocialCounts();
        await loadSocialList($('blog-social-title').textContent === '我的粉丝' ? 'followers' : 'following');
      } catch (error) {
        show('blog-status', error.message);
        button.disabled = false;
      }
    });

    function renderGlobalSearchResults(result) {
      const users = result.users || [];
      const articles = result.articles || [];
      const userHtml = users.map(function (item) {
        const relation = item.ownProfile
          ? '<span class="status-pill success">你自己</span>'
          : (guest ? '<span class="status-pill muted">登录后可关注</span>' : followButton({
              authorId: item.id,
              authorFollowedByCurrentUser: item.followedByCurrentUser,
              mutualFollowWithAuthor: item.mutualFollow
            }));
        return `<article class="search-user-card">
          <div>
            <strong>${userProfileLink(item.id, '@' + item.username, 'search-user-link')}</strong>
            <p>${escapeHtml(item.nickname || item.username)} · 粉丝 ${item.followerCount || 0}</p>
          </div>
          <div class="social-user-actions">
            ${relation}
            ${(guest || item.ownProfile) ? '' : `<a class="secondary-link social-message-link" href="/messages.html?userId=${item.id}">私信</a>`}
          </div>
        </article>`;
      }).join('') || '<p class="blog-muted">没有匹配的用户。</p>';
      const articleHtml = articles.map(function (article) {
        return `<article class="search-article-card">
          <div>
            <strong><a href="/article.html?id=${article.id}">${escapeHtml(article.title)}</a></strong>
            <p>作者：${userProfileLink(article.authorId, article.authorName, 'search-user-link')} · ${formatDate(article.createdAt)}</p>
          </div>
          <span class="article-meta">${articleStats(article)}</span>
        </article>`;
      }).join('') || '<p class="blog-muted">没有匹配的公开文章。</p>';
      $('global-search-results').innerHTML = `
        <section><h3>用户 <span>${users.length}</span></h3><div class="global-search-list">${userHtml}</div></section>
        <section><h3>文章 <span>${articles.length}</span></h3><div class="global-search-list">${articleHtml}</div></section>
      `;
      show('global-search-status', `找到 ${users.length} 位用户、${articles.length} 篇文章`);
    }

    async function runGlobalSearch() {
      const keyword = $('global-search-input').value.trim();
      if (!keyword) {
        $('global-search-results').innerHTML = '';
        show('global-search-status', '请输入要搜索的用户名、昵称或文章标题');
        return;
      }
      show('global-search-status', '正在搜索...');
      const result = await api('/search?keyword=' + encodeURIComponent(keyword));
      renderGlobalSearchResults(result);
    }

    $('global-search-form').addEventListener('submit', async function (event) {
      event.preventDefault();
      try {
        await runGlobalSearch();
      } catch (error) {
        show('global-search-status', error.message);
      }
    });
    $('global-search-results').addEventListener('click', async function (event) {
      const button = event.target.closest('[data-follow-user]');
      if (!button) return;
      try {
        const following = button.getAttribute('data-following') === 'true';
        button.disabled = true;
        await api('/users/' + button.getAttribute('data-follow-user') + '/follow', { method: following ? 'DELETE' : 'POST' });
        await refreshOwnSocialCounts();
        await runGlobalSearch();
      } catch (error) {
        show('global-search-status', error.message);
        button.disabled = false;
      }
    });

    async function loadHomeComments(articleId) {
      const list = document.querySelector(`[data-comment-list="${articleId}"]`);
      if (!list) return;
      list.innerHTML = '<p class="blog-muted">正在加载评论...</p>';
      try {
        const comments = await api('/articles/' + articleId + '/comments');
        list.innerHTML = renderCommentTree(comments, articleId, false);
      } catch (error) {
        list.innerHTML = `<p class="blog-muted">${escapeHtml(error.message)}</p>`;
      }
    }

    function renderHomeArticles() {
      const allArticles = state.homeArticles || [];
      const articles = state.homeYear ? allArticles.filter(article => articleYear(article.createdAt) === state.homeYear) : allArticles;
      const total = articles.length;
      const totalPages = Math.max(1, Math.ceil(total / state.homePageSize));
      state.homePage = Math.max(1, Math.min(state.homePage, totalPages));
      const start = (state.homePage - 1) * state.homePageSize;
      const pageArticles = articles.slice(start, start + state.homePageSize);
      $('home-article-list').innerHTML = pageArticles.map(article => `
          <article class="article-card feed-card">
            <div class="article-card-head">
              <span class="status-pill success">已上架</span>
              <span class="article-meta">${articleStats(article)}</span>
            </div>
            <h3>${escapeHtml(article.title)}</h3>
            ${authorSocialRow(article)}
            <p class="article-meta">分类：${escapeHtml(article.categoryName)} | ${formatDate(article.createdAt)}</p>
            <p>${escapeHtml(article.summary || textPreview(article.content, 140))}</p>
            <p>${(article.tagNames || []).map(t => `<span class="tag-pill">${escapeHtml(t)}</span>`).join('')}</p>
            ${renderCompactAttachments(article.attachments)}
            <div class="article-actions">
              <a class="blog-link-btn" href="/article.html?id=${article.id}">查看全文</a>
              ${guest ? '' : likeButton(article)}
              <button type="button" class="secondary" data-home-comments="${article.id}">查看评论 ${article.commentCount || 0}</button>
            </div>
            <div class="home-comment-box" data-comment-box="${article.id}">
              ${guest ? `<p class="blog-muted">游客只能浏览公开评论，<a href="${guestLoginHref()}">登录后</a>可点赞、评论、关注和私信。</p>` : `<form data-home-comment-form="${article.id}" class="blog-form inline-comment-form">
                <textarea data-comment-input="${article.id}" required rows="3" placeholder="写下评论，AI 初审正常会直接公开"></textarea>
                <button type="submit">提交评论</button>
              </form>`}
              <div class="blog-status" data-comment-status="${article.id}"></div>
              <div data-comment-list="${article.id}" class="feed-comments"></div>
            </div>
          </article>
        `).join('') || '<p class="blog-muted">暂无已上架博客。管理员审核通过后，文章会显示在这里。</p>';
      const pager = $('home-pagination');
      if (pager) {
        pager.innerHTML = totalPages > 1 ? `<button type="button" class="secondary" data-home-page="${state.homePage - 1}" ${state.homePage <= 1 ? 'disabled' : ''}>上一页</button><span>第 ${state.homePage} / ${totalPages} 页</span><button type="button" class="secondary" data-home-page="${state.homePage + 1}" ${state.homePage >= totalPages ? 'disabled' : ''}>下一页</button>` : '';
      }
      renderSiteMap();
      show('home-status', total ? `共 ${total} 篇已上架文章，当前显示 ${pageArticles.length} 篇` : '暂无已上架文章');
    }

    async function loadHomeArticles(resetPage) {
      try {
        show('home-status', '正在加载首页文章...');
        const query = buildQuery([
          { id: 'home-keyword', name: 'keyword' },
          { id: 'home-category-filter', name: 'categoryId' },
          { id: 'home-tag-filter', name: 'tagId' }
        ]);
        const articles = await api('/articles/feed' + (query ? '?' + query : ''));
        state.homeArticles = articles || [];
        if (resetPage !== false) state.homePage = 1;
        renderHomeArticles();
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

    $('home-search-btn').addEventListener('click', function () { state.homeYear = ''; loadHomeArticles(true); });
    if ($('home-pagination')) $('home-pagination').addEventListener('click', function (event) {
      const page = Number(event.target.getAttribute('data-home-page'));
      if (!page) return;
      state.homePage = page;
      renderHomeArticles();
      const home = $('blog-home-section');
      if (home) home.scrollIntoView({ behavior: 'smooth', block: 'start' });
    });
    if ($('blog-site-map')) $('blog-site-map').addEventListener('click', function (event) {
      const categoryBtn = event.target.closest('[data-site-category]');
      const tagBtn = event.target.closest('[data-site-tag]');
      const yearBtn = event.target.closest('[data-site-year]');
      if (!categoryBtn && !tagBtn && !yearBtn) return;
      if (categoryBtn && $('home-category-filter')) {
        state.homeYear = '';
        $('home-category-filter').value = categoryBtn.getAttribute('data-site-category');
        if ($('home-tag-filter')) $('home-tag-filter').value = '';
        loadHomeArticles(true);
      }
      if (tagBtn && $('home-tag-filter')) {
        state.homeYear = '';
        $('home-tag-filter').value = tagBtn.getAttribute('data-site-tag');
        if ($('home-category-filter')) $('home-category-filter').value = '';
        loadHomeArticles(true);
      }
      if (yearBtn) {
        const year = yearBtn.getAttribute('data-site-year');
        state.homeYear = state.homeYear === year ? '' : year;
        state.homePage = 1;
        renderHomeArticles();
      }
      const home = $('blog-home-section');
      if (home) home.scrollIntoView({ behavior: 'smooth', block: 'start' });
    });
    $('home-article-list').addEventListener('click', async function (event) {
      const likeButtonEl = event.target.closest('[data-like-article]');
      const followButtonEl = event.target.closest('[data-follow-user]');
      const commentLikeButtonEl = event.target.closest('[data-like-comment]');
      const commentsButtonEl = event.target.closest('[data-home-comments]');
      const likeId = likeButtonEl ? likeButtonEl.getAttribute('data-like-article') : null;
      const followUserId = followButtonEl ? followButtonEl.getAttribute('data-follow-user') : null;
      const commentLikeId = commentLikeButtonEl ? commentLikeButtonEl.getAttribute('data-like-comment') : null;
      const commentArticleId = commentLikeButtonEl ? commentLikeButtonEl.getAttribute('data-comment-article') : null;
      const commentsId = commentsButtonEl ? commentsButtonEl.getAttribute('data-home-comments') : null;
      try {
        if (likeId) {
          const liked = likeButtonEl.getAttribute('data-liked') === 'true' || likeButtonEl.classList.contains('is-liked');
          likeButtonEl.disabled = true;
          const updated = await api('/articles/' + likeId + '/like', { method: liked ? 'DELETE' : 'POST' });
          state.homeArticles = state.homeArticles.map(function (article) {
            return String(article.id) === String(likeId) ? Object.assign({}, article, updated) : article;
          });
          show('home-status', liked ? '已取消点赞' : '点赞成功');
          renderHomeArticles();
        }
        if (followUserId) {
          const following = followButtonEl.getAttribute('data-following') === 'true';
          followButtonEl.disabled = true;
          const status = await api('/users/' + followUserId + '/follow', { method: following ? 'DELETE' : 'POST' });
          state.homeArticles = state.homeArticles.map(function (article) {
            return String(article.authorId) === String(followUserId) ? mergeAuthorFollowStatus(article, status) : article;
          });
          show('home-status', status.mutualFollow ? '你们已互相关注' : (status.followedByCurrentUser ? '关注成功' : '已取消关注'));
          await refreshOwnSocialCounts();
          renderHomeArticles();
        }
        if (commentLikeId) {
          const liked = commentLikeButtonEl.getAttribute('data-comment-liked') === 'true' || commentLikeButtonEl.classList.contains('is-liked');
          commentLikeButtonEl.disabled = true;
          await api('/comments/' + commentLikeId + '/like', { method: liked ? 'DELETE' : 'POST' });
          show('home-status', liked ? '已取消评论点赞' : '评论点赞成功');
          if (commentArticleId) await loadHomeComments(commentArticleId);
        }
        if (commentsId) await loadHomeComments(commentsId);
      } catch (error) {
        show('home-status', error.message);
        if (likeButtonEl) likeButtonEl.disabled = false;
        if (followButtonEl) followButtonEl.disabled = false;
        if (commentLikeButtonEl) commentLikeButtonEl.disabled = false;
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
        const comment = await api('/articles/' + articleId + '/comments', {
          method: 'POST',
          body: JSON.stringify({ content: input.value, parentId: null })
        });
        input.value = '';
        if (comment.status === 'APPROVED') {
          if (status) status.textContent = 'AI 初审通过，评论已直接公开。';
          await loadHomeComments(articleId);
        } else {
          if (status) status.textContent = 'AI 发现疑似问题，已转交管理员审核。';
        }
      } catch (error) {
        if (status) status.textContent = error.message;
      }
    });

    $('blog-search-btn').addEventListener('click', loadMyArticles);
    if ($('article-tag-input')) $('article-tag-input').addEventListener('input', renderTagPreview);
    if ($('article-category-custom')) $('article-category-custom').addEventListener('input', function () { if ($('article-category-custom').value.trim()) $('article-category').value = ''; });
    if ($('article-category')) $('article-category').addEventListener('change', function () { if ($('article-category').value && $('article-category-custom')) $('article-category-custom').value = ''; });
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
      setCategoryInput('');
      setTagInput([]);
      state.editorAttachments = [];
      renderAttachments();
      clearLocalArticleDraft();
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
          $('article-category').value = article.categoryId || '';
          if ($('article-category-custom')) $('article-category-custom').value = '';
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
      const selectedCategory = $('article-category').value;
      const payload = {
        title: $('article-title').value,
        summary: $('article-summary').value,
        content: $('article-content').value,
        categoryId: selectedCategory ? Number(selectedCategory) : null,
        categoryName: selectedCategoryName(),
        tagIds: selectedTagIds(),
        tagNames: selectedTagNames(),
        attachments: normalizeAttachments(state.editorAttachments),
        status: $('article-status').value
      };
      try {
        if (payload.status === 'DRAFT') {
          const result = saveLocalArticleDraft(payload, id);
          const attachmentTip = result.attachmentsOmitted
            ? `，正文等内容已保存；${result.attachmentsOmitted} 个附件因体积过大未写入本地`
            : '，刷新或重新进入工作台会自动恢复';
          localDraftStatus(`本地草稿保存于 ${new Date().toLocaleString('zh-CN')}`);
          show('blog-status', '草稿已保存在当前浏览器本地' + attachmentTip);
          return;
        }
        show('blog-status', '正在保存到数据库...');
        const savedArticle = await api('/articles' + (id ? '/' + id : ''), { method: id ? 'PUT' : 'POST', body: JSON.stringify(payload) });
        clearLocalArticleDraft();
        event.target.reset();
        $('article-id').value = '';
        await loadTaxonomy();
        renderTaxonomyControls();
        setCategoryInput('');
        setTagInput([]);
        state.editorAttachments = [];
        renderAttachments();
        await loadMyArticles();
        if (savedArticle.status === 'PUBLISHED') await loadHomeArticles(true);
        if (window.refreshCialloSidebar) window.refreshCialloSidebar();
        show('blog-status', savedArticle.status === 'PUBLISHED'
          ? 'AI 初审通过，文章已直接发布。'
          : 'AI 发现疑似问题，已转交管理员审核。');
      } catch (error) {
        show('blog-status', error.message);
      }
    });

    async function runAi(path, payload) {
      try {
        show('blog-status', 'AI 正在生成...');
        const data = await api(path, { method: 'POST', body: JSON.stringify(payload) });
        renderAiResult('ai-output', data);
        show('blog-status', data.error ? 'DeepSeek 调用失败，错误已写入 AI 日志' : 'AI 生成完成，记录已写入数据库日志');
      } catch (error) {
        show('blog-status', error.message);
        $('ai-output').textContent = error.message;
      }
    }
    $('ai-outline-btn').addEventListener('click', () => runAi('/ai/outline', aiPayload()));
    $('ai-summary-btn').addEventListener('click', () => runAi('/ai/summary', aiPayload()));
    $('ai-tags-btn').addEventListener('click', () => runAi('/ai/tags', aiPayload()));
    if ($('ai-category-btn')) $('ai-category-btn').addEventListener('click', () => runAi('/ai/category', aiPayload()));

    await loadHomeArticles(true);
    if (!guest) {
      await loadMyArticles();
      await showUnreadNotifications('你有新的博客消息');
    }
  }

  async function initArticlePage() {
    const user = await verifySession();
    const guest = isGuestBrowsing(user);
    if (guest) sessionStorage.setItem('cialloGuestMode', '1');
    applyRoleNavigation(user);
    const id = new URLSearchParams(location.search).get('id') || '1';
    if (guest) {
      const commentForm = $('comment-form');
      if (commentForm) {
        commentForm.innerHTML = `<p class="blog-muted">游客只能浏览公开评论，<a href="${guestLoginHref()}">登录后</a>可发表评论、回复和点赞。</p>`;
      }
      const qaPanel = $('qa-btn') ? $('qa-btn').closest('.blog-panel') : null;
      if (qaPanel) {
        qaPanel.innerHTML = '<p class="blog-muted">博客问答属于 AI 功能，游客模式不可使用；登录后可向 AI 提问。</p>';
      }
    }
    async function loadDetail() {
      const article = await api('/articles/' + id);
      $('article-detail').innerHTML = `
        <h1>${escapeHtml(article.title)}</h1>
        ${authorSocialRow(article)}
        <p class="article-meta">分类：${escapeHtml(article.categoryName)} | ${formatDate(article.createdAt)} | ${articleStats(article)}</p>
        <p><span class="status-pill ${statusClass(article.status)}">${statusText(article.status)}</span> ${(article.tagNames || []).map(t => `<span class="tag-pill">${escapeHtml(t)}</span>`).join('')}</p>
        <div class="article-detail-actions">${guest ? `<p class="blog-muted">游客只能浏览公开内容，<a href="${guestLoginHref()}">登录后</a>可点赞、评论、关注和私信。</p>` : likeButton(article)}</div>
        ${renderArticleContent(article.content, article.attachments || [])}
      `;
    }
    async function loadComments() {
      const comments = await api('/articles/' + id + '/comments');
      $('comment-list').innerHTML = renderCommentTree(comments, id, !guest);
    }
    $('article-detail').addEventListener('click', async function (event) {
      const likeButtonEl = event.target.closest('[data-like-article]');
      const followButtonEl = event.target.closest('[data-follow-user]');
      if (followButtonEl) {
        try {
          const following = followButtonEl.getAttribute('data-following') === 'true';
          followButtonEl.disabled = true;
          await api('/users/' + followButtonEl.getAttribute('data-follow-user') + '/follow', { method: following ? 'DELETE' : 'POST' });
          await loadDetail();
        } catch (error) {
          alert(error.message);
          followButtonEl.disabled = false;
        }
        return;
      }
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
    $('comment-list').addEventListener('click', async function (event) {
      const commentLikeButtonEl = event.target.closest('[data-like-comment]');
      if (commentLikeButtonEl) {
        try {
          const liked = commentLikeButtonEl.getAttribute('data-comment-liked') === 'true' || commentLikeButtonEl.classList.contains('is-liked');
          commentLikeButtonEl.disabled = true;
          await api('/comments/' + commentLikeButtonEl.getAttribute('data-like-comment') + '/like', { method: liked ? 'DELETE' : 'POST' });
          show('comment-status', liked ? '已取消评论点赞' : '评论点赞成功');
          await loadComments();
        } catch (error) {
          show('comment-status', error.message);
          commentLikeButtonEl.disabled = false;
        }
        return;
      }
      const replyId = event.target.getAttribute('data-reply');
      if (!replyId) return;
      $('comment-parent-id').value = replyId;
      const name = event.target.getAttribute('data-reply-name') || '';
      show('comment-status', '正在回复 #' + replyId + (name ? ' ' + name : ''));
      $('comment-content').focus();
    });
    $('comment-form').addEventListener('submit', async function (event) {
      event.preventDefault();
      if (guest) {
        show('comment-status', guestActionHint('评论'));
        return;
      }
      try {
        const comment = await api('/articles/' + id + '/comments', {
          method: 'POST',
          body: JSON.stringify({ content: $('comment-content').value, parentId: $('comment-parent-id').value ? Number($('comment-parent-id').value) : null })
        });
        $('comment-content').value = '';
        $('comment-parent-id').value = '';
        const message = comment.status === 'APPROVED'
          ? 'AI 初审通过，评论已直接公开。'
          : 'AI 发现疑似问题，已转交管理员审核。';
        show('comment-status', message);
        if (comment.status === 'APPROVED') await loadComments();
        alert(message);
      } catch (error) {
        show('comment-status', error.message);
        alert(error.message);
      }
    });
    if ($('qa-btn')) $('qa-btn').addEventListener('click', async function () {
      if (guest) {
        $('qa-output').textContent = guestActionHint('AI 问答');
        return;
      }
      try {
        const data = await api('/ai/qa', { method: 'POST', body: JSON.stringify({ question: $('qa-question').value }) });
        renderAiResult('qa-output', data);
      } catch (error) {
        $('qa-output').textContent = error.message;
      }
    });
    try { await loadDetail(); await loadComments(); } catch (error) { $('article-detail').textContent = error.message; }
  }

  async function initUserProfilePage() {
    const currentUser = await verifySession();
    const guest = isGuestBrowsing(currentUser);
    if (guest) sessionStorage.setItem('cialloGuestMode', '1');
    applyRoleNavigation(currentUser);
    const userId = new URLSearchParams(location.search).get('id');
    if (!userId) {
      $('user-profile-card').innerHTML = '<p class="blog-muted">缺少用户编号。</p>';
      return;
    }

    function renderProfile(profile) {
      const privateDataVisible = Boolean(profile.privateDataVisible);
      const email = privateDataVisible ? (profile.email || '未填写') : (profile.maskedEmail || '未公开');
      const relation = profile.ownProfile
        ? '<span class="status-pill success">这是你自己</span>'
        : (guest ? '<span class="status-pill muted">登录后可关注</span>' : followButton({
            authorId: profile.id,
            authorFollowedByCurrentUser: profile.followedByCurrentUser,
            mutualFollowWithAuthor: profile.mutualFollow
          }));
      const messageLink = (guest || profile.ownProfile) ? '' : `<a class="blog-link-btn secondary-link" href="/messages.html?userId=${profile.id}">发私信</a>`;
      $('user-profile-card').innerHTML = `
        <div class="user-profile-main">
          <div>
            <p class="section-kicker">用户 @${escapeHtml(profile.username)}</p>
            <h1>${escapeHtml(profile.nickname || profile.username)}</h1>
            <p class="user-profile-email">邮箱：${escapeHtml(email)}</p>
          </div>
          <div class="user-profile-relation">${relation}${messageLink}</div>
        </div>
        <div class="user-profile-stats">
          <span><strong>${profile.followerCount || 0}</strong> 粉丝</span>
          <span><strong>${profile.followingCount || 0}</strong> 关注</span>
          ${profile.mutualFollow ? '<span class="mutual-follow-label">已互关</span>' : ''}
        </div>
        <p class="privacy-note">${privateDataVisible
          ? (profile.ownProfile
              ? '当前为本人资料卡，邮箱以完整形式显示。'
              : '当前为管理员查看，用户邮箱以完整形式显示。')
          : '隐私保护：邮箱已脱敏；仅展示已上架文章和该用户主动保留在相册中的图片。'}</p>
      `;
      const intro = $('user-profile-intro');
      if (intro) {
        intro.textContent = guest
          ? '游客模式仅展示公开资料、脱敏邮箱、公开文章与个人相册；关注、私信等互动需要登录。'
          : privateDataVisible
          ? '当前权限可查看完整邮箱，同时展示公开文章与个人相册。'
          : '邮箱经过脱敏处理，展示该用户的公开文章与个人相册。';
      }
      const articles = profile.articles || [];
      $('user-profile-article-summary').textContent = `共 ${articles.length} 篇公开文章`;
      $('user-profile-articles').innerHTML = articles.map(function (article) {
        return `<article class="article-card">
          <div class="article-card-head">
            <span class="status-pill success">已上架</span>
            <span class="article-meta">${articleStats(article)}</span>
          </div>
          <h3>${escapeHtml(article.title)}</h3>
          <p class="article-meta">${escapeHtml(article.categoryName)} | ${formatDate(article.createdAt)}</p>
          <p>${escapeHtml(article.summary || textPreview(article.content, 140))}</p>
          <p>${(article.tagNames || []).map(t => `<span class="tag-pill">${escapeHtml(t)}</span>`).join('')}</p>
          <div class="article-actions">
            <a class="blog-link-btn" href="/article.html?id=${article.id}">查看全文</a>
          </div>
        </article>`;
      }).join('') || '<p class="blog-muted">该用户暂时没有已上架文章。</p>';
      const photos = profile.photos || [];
      $('user-profile-photo-summary').textContent = `共 ${photos.length} / 8 张图片`;
      $('user-profile-photos').innerHTML = photos.map(function (photo) {
        const imageUrl = escapeHtml(safeGalleryImageUrl(photo.imageDataUrl));
        const title = escapeHtml(photo.title || '未命名图片');
        const description = escapeHtml(photo.description || '');
        return `<article class="managed-gallery-card">
          <a class="managed-gallery-image" href="${imageUrl}" data-fancybox="profile-gallery" data-caption="${title}${description ? ' · ' + description : ''}">
            <img src="${imageUrl}" alt="${title}" loading="lazy">
          </a>
          <div class="managed-gallery-content">
            <h3>${title}</h3>
            ${description ? `<p>${description}</p>` : '<p class="blog-muted">没有填写图片说明。</p>'}
          </div>
        </article>`;
      }).join('') || '<p class="gallery-empty">该用户的相册暂时为空。</p>';
    }

    async function loadProfile() {
      const profile = await api('/users/' + encodeURIComponent(userId) + '/profile');
      renderProfile(profile);
    }

    $('user-profile-card').addEventListener('click', async function (event) {
      const button = event.target.closest('[data-follow-user]');
      if (!button) return;
      try {
        const following = button.getAttribute('data-following') === 'true';
        button.disabled = true;
        await api('/users/' + encodeURIComponent(userId) + '/follow', { method: following ? 'DELETE' : 'POST' });
        await loadProfile();
      } catch (error) {
        alert(error.message);
        button.disabled = false;
      }
    });

    try {
      await loadProfile();
    } catch (error) {
      $('user-profile-card').innerHTML = `<p class="blog-muted">${escapeHtml(error.message)}</p>`;
      $('user-profile-articles').innerHTML = '';
      $('user-profile-photos').innerHTML = '';
    }
  }

  async function initGalleryPage() {
    let currentUser = null;
    let photos = [];
    let editingId = null;
    let selectedImageData = '';
    const maxImageSize = 6 * 1024 * 1024;

    if (token()) {
      try {
        currentUser = await api('/auth/me');
        state.currentUser = currentUser;
        applyRoleNavigation(currentUser);
      } catch (error) {
        clearSession();
      }
    }
    $('gallery-manager').hidden = !currentUser;
    $('gallery-login-hint').hidden = !!currentUser;

    function canManage(photo) {
      return !!currentUser && String(currentUser.id) === String(photo.ownerId);
    }

    function syncGalleryLimit() {
      const full = photos.length >= 8;
      $('gallery-submit').disabled = !editingId && full;
      $('gallery-file').disabled = !editingId && full;
      if (!editingId && full) {
        show('gallery-form-status', '相册已满 8 张，请先删除一张再上传新图片。');
      }
    }

    function resetGalleryForm() {
      editingId = null;
      selectedImageData = '';
      $('gallery-form').reset();
      $('gallery-form-heading').textContent = '上传新图片';
      $('gallery-submit').textContent = '上传图片';
      $('gallery-cancel-edit').hidden = true;
      $('gallery-upload-preview').hidden = true;
      $('gallery-preview-image').removeAttribute('src');
      show('gallery-form-status', '');
      syncGalleryLimit();
    }

    function showGalleryPreview(url) {
      $('gallery-preview-image').src = safeGalleryImageUrl(url);
      $('gallery-upload-preview').hidden = false;
    }

    function renderGallery() {
      $('gallery-count').textContent = photos.length + ' / 8 张';
      $('gallery-photo-grid').innerHTML = photos.map(function (photo) {
        const imageUrl = escapeHtml(safeGalleryImageUrl(photo.imageDataUrl));
        const title = escapeHtml(photo.title || '未命名图片');
        const description = escapeHtml(photo.description || '');
        const owner = photo.ownerUsername || photo.ownerNickname || '用户';
        const actions = canManage(photo)
          ? `<div class="gallery-card-actions">
              <button type="button" class="secondary" data-edit-gallery-photo="${photo.id}">修改</button>
              <button type="button" class="danger" data-delete-gallery-photo="${photo.id}">删除</button>
            </div>`
          : '';
        return `<article class="managed-gallery-card">
          <a class="managed-gallery-image" href="${imageUrl}" data-fancybox="managed-gallery" data-caption="${title}${description ? ' · ' + description : ''}">
            <img src="${imageUrl}" alt="${title}" loading="lazy">
          </a>
          <div class="managed-gallery-content">
            <h3>${title}</h3>
            ${description ? `<p>${description}</p>` : '<p class="blog-muted">没有填写图片说明。</p>'}
            <small>上传者：${userProfileLink(photo.ownerId, '@' + owner, 'gallery-owner-link')} · ${formatDate(photo.updatedAt || photo.createdAt)}</small>
            ${actions}
          </div>
        </article>`;
      }).join('') || '<p class="gallery-empty">相册还是空的，登录后上传第一张图片吧。</p>';
      syncGalleryLimit();
    }

    async function loadGallery() {
      show('gallery-list-status', '正在加载相册...');
      photos = await api('/gallery/photos');
      renderGallery();
      show('gallery-list-status', photos.length ? '' : '暂时还没有图片。');
    }

    $('gallery-file').addEventListener('change', function () {
      const file = this.files && this.files[0];
      if (!file) {
        selectedImageData = '';
        if (!editingId) $('gallery-upload-preview').hidden = true;
        return;
      }
      if (!/^image\/(jpeg|png|webp|gif)$/i.test(file.type)) {
        this.value = '';
        show('gallery-form-status', '仅支持 JPG、PNG、WebP 或 GIF 图片。');
        return;
      }
      if (file.size > maxImageSize) {
        this.value = '';
        show('gallery-form-status', '图片超过 6MB，请压缩后再上传。');
        return;
      }
      show('gallery-form-status', '正在读取图片...');
      const reader = new FileReader();
      reader.onload = function () {
        selectedImageData = String(reader.result || '');
        showGalleryPreview(selectedImageData);
        show('gallery-form-status', '图片已就绪，可以上传。');
      };
      reader.onerror = function () {
        selectedImageData = '';
        show('gallery-form-status', '图片读取失败，请重新选择。');
      };
      reader.readAsDataURL(file);
    });

    $('gallery-form').addEventListener('submit', async function (event) {
      event.preventDefault();
      if (!currentUser) return;
      if (!editingId && !selectedImageData) {
        show('gallery-form-status', '请选择要上传的图片。');
        return;
      }
      const payload = {
        title: $('gallery-title').value.trim(),
        description: $('gallery-description').value.trim(),
        imageDataUrl: selectedImageData || null
      };
      try {
        const wasEditing = !!editingId;
        $('gallery-submit').disabled = true;
        show('gallery-form-status', editingId ? '正在保存修改...' : '正在上传图片...');
        await api('/gallery/photos' + (editingId ? '/' + editingId : ''), {
          method: editingId ? 'PUT' : 'POST',
          body: JSON.stringify(payload)
        });
        resetGalleryForm();
        await loadGallery();
        show('gallery-list-status', wasEditing ? '图片修改成功。' : '图片上传成功。');
      } catch (error) {
        show('gallery-form-status', error.message);
      } finally {
        syncGalleryLimit();
      }
    });

    $('gallery-cancel-edit').addEventListener('click', resetGalleryForm);

    $('gallery-photo-grid').addEventListener('click', async function (event) {
      const editButton = event.target.closest('[data-edit-gallery-photo]');
      const deleteButton = event.target.closest('[data-delete-gallery-photo]');
      if (editButton) {
        const photo = photos.find(function (item) {
          return String(item.id) === editButton.getAttribute('data-edit-gallery-photo');
        });
        if (!photo || !canManage(photo)) return;
        editingId = photo.id;
        selectedImageData = '';
        $('gallery-title').value = photo.title || '';
        $('gallery-description').value = photo.description || '';
        $('gallery-file').value = '';
        $('gallery-form-heading').textContent = '修改图片';
        $('gallery-submit').textContent = '保存修改';
        $('gallery-cancel-edit').hidden = false;
        syncGalleryLimit();
        showGalleryPreview(photo.imageDataUrl);
        show('gallery-form-status', '可以修改标题和说明；如需替换图片，再选择一个新文件。');
        $('gallery-manager').scrollIntoView({ behavior: 'smooth', block: 'start' });
        return;
      }
      if (deleteButton) {
        const id = deleteButton.getAttribute('data-delete-gallery-photo');
        const photo = photos.find(function (item) { return String(item.id) === id; });
        if (!photo || !canManage(photo) || !confirm('确认删除《' + photo.title + '》吗？此操作无法撤销。')) return;
        try {
          deleteButton.disabled = true;
          await api('/gallery/photos/' + encodeURIComponent(id), { method: 'DELETE' });
          if (String(editingId) === String(id)) resetGalleryForm();
          await loadGallery();
          show('gallery-list-status', '图片已删除。');
        } catch (error) {
          show('gallery-list-status', error.message);
          deleteButton.disabled = false;
        }
      }
    });

    if (currentUser) {
      try {
        await loadGallery();
      } catch (error) {
        show('gallery-list-status', error.message);
        $('gallery-photo-grid').innerHTML = '<p class="gallery-empty">相册加载失败，请稍后重试。</p>';
      }
    } else {
      $('gallery-count').textContent = '登录后查看';
      show('gallery-list-status', '请先登录，再查看和管理自己的八张相册图片。');
      $('gallery-photo-grid').innerHTML = '<p class="gallery-empty">每个账号都有独立相册，登录后即可查看。</p>';
    }
  }

  async function initMessagesPage() {
    const currentUser = await requireLogin();
    applyRoleNavigation(currentUser);
    const otherUserId = new URLSearchParams(location.search).get('userId');
    if (!otherUserId) {
      $('message-conversation-head').innerHTML = '<p class="blog-muted">请先从用户资料卡选择私信对象。</p>';
      $('message-form').hidden = true;
      return;
    }

    function renderConversation(conversation) {
      $('message-conversation-head').innerHTML = `
        <div>
          <p class="section-kicker">与 ${userProfileLink(conversation.otherUserId, '@' + conversation.otherUsername, 'message-user-link')} 的会话</p>
          <h2>${escapeHtml(conversation.otherNickname || conversation.otherUsername)}</h2>
        </div>
        <span class="status-pill ${conversation.mutualFollow ? 'success' : 'warning'}">${conversation.mutualFollow ? '已互关 · 私信不限量' : '非互关用户'}</span>
      `;
      const messages = conversation.messages || [];
      $('message-list').innerHTML = messages.map(function (message) {
        const sent = String(message.senderId) === String(currentUser.id);
        return `<article class="message-bubble ${sent ? 'is-sent' : 'is-received'}">
          <strong>${sent ? '我' : userProfileLink(message.senderId, message.senderUsername, 'message-user-link')}</strong>
          <p>${escapeHtml(message.content)}</p>
          <small>${formatDate(message.createdAt)}${!sent && message.readFlag ? ' · 已读' : ''}</small>
        </article>`;
      }).join('') || '<p class="blog-muted">还没有私信，打个招呼吧。</p>';
      const unlimited = conversation.mutualFollow;
      const remaining = conversation.remainingMessageCount;
      $('message-limit-status').textContent = unlimited
        ? '已互关，发送条数不限'
        : `非互关用户还可发送 ${remaining} 条（最多 3 条）`;
      $('message-send-btn').disabled = !unlimited && remaining <= 0;
      $('message-content').disabled = !unlimited && remaining <= 0;
      if (!unlimited && remaining <= 0) show('message-status', '已达到非互关用户的 3 条私信上限，互相关注后可继续发送。');
      requestAnimationFrame(function () {
        $('message-list').scrollTop = $('message-list').scrollHeight;
      });
    }

    async function loadConversation() {
      const conversation = await api('/messages/' + encodeURIComponent(otherUserId));
      renderConversation(conversation);
    }

    $('message-form').addEventListener('submit', async function (event) {
      event.preventDefault();
      const content = $('message-content').value.trim();
      if (!content) return;
      try {
        $('message-send-btn').disabled = true;
        show('message-status', '正在发送...');
        const conversation = await api('/messages/' + encodeURIComponent(otherUserId), {
          method: 'POST',
          body: JSON.stringify({ content: content })
        });
        $('message-content').value = '';
        renderConversation(conversation);
        show('message-status', '私信已发送，对方会收到通知。');
      } catch (error) {
        show('message-status', error.message);
        $('message-send-btn').disabled = false;
      }
    });

    try {
      await loadConversation();
    } catch (error) {
      $('message-conversation-head').innerHTML = `<p class="blog-muted">${escapeHtml(error.message)}</p>`;
      $('message-form').hidden = true;
    }
  }

  async function initAdminPage() {
    const user = await requireAdmin();
    applyRoleNavigation(user);
    $('admin-user-summary').textContent = `当前管理员：${userLine(user)}。你拥有文章审核、评论审核、用户封禁、分类标签和 AI 日志管理权限。`;

    function renderAdminAiLogs() {
      const keyword = String(state.adminAiLogKeyword || '').trim().toLowerCase();
      const feature = state.adminAiLogFeature || '';
      const allLogs = state.adminAiLogs || [];
      const features = uniqueNames(allLogs.map(log => log.feature)).filter(Boolean);
      const logs = allLogs.filter(function (log) {
        const haystack = [log.feature, log.prompt, log.thinking, log.result].map(item => String(item || '')).join(' ').toLowerCase();
        return (!feature || log.feature === feature) && (!keyword || haystack.indexOf(keyword) >= 0);
      });
      const totalPages = Math.max(1, Math.ceil(logs.length / state.adminAiLogPageSize));
      if (state.adminAiLogPage > totalPages) state.adminAiLogPage = totalPages;
      if (state.adminAiLogPage < 1) state.adminAiLogPage = 1;
      const start = (state.adminAiLogPage - 1) * state.adminAiLogPageSize;
      const pageLogs = logs.slice(start, start + state.adminAiLogPageSize);
      const listHtml = pageLogs.map(log => `<div class="admin-row ai-log-row">
          <div class="ai-log-head"><strong>#${log.id} ${escapeHtml(featureText(log.feature))}</strong><span>${formatDate(log.createdAt)}</span></div>
          <small class="ai-log-prompt">提示词：${escapeHtml(textPreview(log.prompt, 280))}</small>
          ${log.thinking ? `<small class="ai-log-thinking">思考过程：${escapeHtml(textPreview(log.thinking, 520))}</small>` : ''}
          <small>输出：${escapeHtml(textPreview(log.result, 300))}</small>
        </div>`).join('') || '<p class="blog-muted">当前筛选下暂无 AI 使用记录。</p>';
      $('admin-ai-logs').innerHTML = `
        <div class="admin-ai-tools">
          <input id="admin-ai-keyword" value="${escapeHtml(state.adminAiLogKeyword)}" placeholder="搜索提示词、思考过程或输出">
          <select id="admin-ai-feature">
            <option value="">全部 AI 功能</option>
            ${features.map(item => `<option value="${escapeHtml(item)}" ${item === feature ? 'selected' : ''}>${escapeHtml(featureText(item))}</option>`).join('')}
          </select>
          <button type="button" class="secondary" data-ai-log-clear>清空筛选</button>
        </div>
        <div class="admin-ai-summary">共 ${logs.length} 条记录，当前第 ${state.adminAiLogPage} / ${totalPages} 页</div>
        <div class="admin-ai-log-list">${listHtml}</div>
        <nav class="blog-pagination admin-ai-pagination" aria-label="AI 日志分页">
          <button type="button" class="secondary" data-ai-log-page="prev" ${state.adminAiLogPage <= 1 ? 'disabled' : ''}>上一页</button>
          <span>${state.adminAiLogPage} / ${totalPages}</span>
          <button type="button" class="secondary" data-ai-log-page="next" ${state.adminAiLogPage >= totalPages ? 'disabled' : ''}>下一页</button>
        </nav>`;
    }

    let adminAiFilterTimer = null;

    async function refresh() {
      try {
        show('admin-status', '正在加载后台数据...');
        const stats = await api('/admin/stats');
        const users = await api('/admin/users');
        const articles = await api('/admin/articles');
        const comments = await api('/admin/comments');
        const aiLogs = await api('/admin/ai-logs');
        await loadTaxonomy();
        renderTaxonomyControls();
        const statLabels = {
          userCount: '用户数', articleCount: '文章数', commentCount: '评论数', categoryCount: '分类数',
          tagCount: '标签数', totalViews: '总阅读量', aiUsageCount: 'AI 调用次数'
        };
        $('admin-stats').innerHTML = Object.keys(statLabels).map(key => `<div class="stat-card"><strong>${statLabels[key]}</strong><br>${stats[key] || 0}</div>`).join('');
        $('admin-users').innerHTML = users.map(u => `<div class="admin-row">
          <strong>#${u.id} ${userProfileLink(u.id, u.username, 'search-user-link')}</strong>
          <span>${escapeHtml(u.nickname || u.username)} | ${escapeHtml(u.email || '未填写邮箱')} | ${roleText(u.role)} | <span class="status-pill ${u.banned ? 'danger' : 'success'}">${u.banned ? '已封禁' : '正常'}</span> | 注册时间 ${formatDate(u.createdAt)}</span>
          <div class="admin-row-actions">
            <button type="button" class="secondary" data-user-role="${u.id}" data-role="${u.role === 'ADMIN' ? 'USER' : 'ADMIN'}">设为${u.role === 'ADMIN' ? '普通用户' : '管理员'}</button>
            <button type="button" class="${u.banned ? 'secondary' : 'danger'}" data-user-ban="${u.id}" data-banned="${u.banned ? 'false' : 'true'}">${u.banned ? '解封用户' : '封禁用户'}</button>
            <button type="button" class="danger" data-delete-user="${u.id}">删除用户</button>
          </div>
        </div>`).join('') || '<p class="blog-muted">暂无用户。</p>';
        $('admin-articles').innerHTML = articles.map(a => `<div class="admin-row">
          <strong>#${a.id} ${escapeHtml(a.title)}</strong>
          <span><span class="status-pill ${statusClass(a.status)}">${statusText(a.status)}</span> | 作者 ${userProfileLink(a.authorId, a.authorName, 'article-author-link')} | ${articleStats(a)} | 附件 ${(a.attachments || []).length}</span>
          <small>${escapeHtml(textPreview(a.summary || a.content, 150))}</small>
          <small>${escapeHtml(reviewText(a.aiReviewResult))}</small>
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
          <span><span class="status-pill ${statusClass(c.status)}">${statusText(c.status)}</span> | 用户 #${c.userId} ${userProfileLink(c.userId, c.nickname, 'comment-author-link')} | ${formatDate(c.createdAt)}</span>
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
        state.adminAiLogs = aiLogs;
        renderAdminAiLogs();
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
    if ($('article-tag-input')) $('article-tag-input').addEventListener('input', renderTagPreview);
    if ($('article-category-custom')) $('article-category-custom').addEventListener('input', function () { if ($('article-category-custom').value.trim()) $('article-category').value = ''; });
    if ($('article-category')) $('article-category').addEventListener('change', function () { if ($('article-category').value && $('article-category-custom')) $('article-category-custom').value = ''; });
    if ($('article-files')) $('article-files').addEventListener('change', async function (event) {
      try {
        show('admin-publish-status', '正在读取附件...');
        const files = await readFilesAsAttachments(event.target.files);
        state.editorAttachments = normalizeAttachments(state.editorAttachments).concat(files).slice(0, 8);
        renderAttachments();
        event.target.value = '';
        show('admin-publish-status', '附件已加入文章，发布后会同步保存');
      } catch (error) {
        show('admin-publish-status', error.message);
      }
    });
    if ($('article-attachments')) $('article-attachments').addEventListener('click', function (event) {
      const index = event.target.getAttribute('data-attachment-remove');
      if (index == null) return;
      state.editorAttachments.splice(Number(index), 1);
      renderAttachments();
    });
    if ($('article-reset-btn')) $('article-reset-btn').addEventListener('click', function () {
      resetArticleEditorForm();
      show('admin-publish-status', '表单已清空');
    });
    if ($('article-form')) $('article-form').addEventListener('submit', async function (event) {
      event.preventDefault();
      const payload = articleEditorPayload('PUBLISHED');
      try {
        show('admin-publish-status', '正在发布文章...');
        const savedArticle = await api('/articles', { method: 'POST', body: JSON.stringify(payload) });
        resetArticleEditorForm();
        await refresh();
        if (window.refreshCialloSidebar) window.refreshCialloSidebar();
        show('admin-publish-status', `管理员已直接发布《${savedArticle.title || payload.title}》`);
      } catch (error) {
        show('admin-publish-status', error.message);
      }
    });
    if ($('ai-outline-btn')) $('ai-outline-btn').addEventListener('click', () => runArticleAi('/ai/outline', 'admin-publish-status'));
    if ($('ai-summary-btn')) $('ai-summary-btn').addEventListener('click', () => runArticleAi('/ai/summary', 'admin-publish-status'));
    if ($('ai-tags-btn')) $('ai-tags-btn').addEventListener('click', () => runArticleAi('/ai/tags', 'admin-publish-status'));
    if ($('ai-category-btn')) $('ai-category-btn').addEventListener('click', () => runArticleAi('/ai/category', 'admin-publish-status'));
    $('admin-ai-logs').addEventListener('input', function (event) {
      if (event.target.id !== 'admin-ai-keyword') return;
      state.adminAiLogKeyword = event.target.value;
      clearTimeout(adminAiFilterTimer);
      adminAiFilterTimer = setTimeout(function () {
        state.adminAiLogPage = 1;
        renderAdminAiLogs();
      }, 220);
    });
    $('admin-ai-logs').addEventListener('change', function (event) {
      if (event.target.id !== 'admin-ai-feature') return;
      state.adminAiLogFeature = event.target.value;
      state.adminAiLogPage = 1;
      renderAdminAiLogs();
    });
    $('admin-ai-logs').addEventListener('click', function (event) {
      const pageAction = event.target.getAttribute('data-ai-log-page');
      if (pageAction === 'prev') { state.adminAiLogPage -= 1; renderAdminAiLogs(); return; }
      if (pageAction === 'next') { state.adminAiLogPage += 1; renderAdminAiLogs(); return; }
      if (event.target.getAttribute('data-ai-log-clear') != null) {
        state.adminAiLogKeyword = '';
        state.adminAiLogFeature = '';
        state.adminAiLogPage = 1;
        renderAdminAiLogs();
      }
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
    resetArticleEditorForm();
    refresh();
  }

  document.addEventListener('DOMContentLoaded', function () {
    applyRoleNavigation(storedUser());
    updateNotificationIndicators();
    if ($('login-page')) initLoginPage();
    if ($('blog-page')) initBlogPage();
    if ($('article-page')) initArticlePage();
    if ($('user-profile-page')) initUserProfilePage();
    if ($('gallery-page')) initGalleryPage();
    if ($('messages-page')) initMessagesPage();
    if ($('notification-page')) initNotificationPage();
    if ($('admin-page')) initAdminPage();
  });
}());
