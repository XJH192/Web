---
title: 管理员后台
permalink: admin.html
cover: /images/background/gongzuotai.jpg
coverWidth: 1920
coverHeight: 760
---

<link rel="stylesheet" href="/blog-system.css">

<section class="blog-app" id="admin-page">
  <header class="blog-app-head">
    <div>
      <p class="section-kicker">管理员</p>
      <h2>审核与系统管理</h2>
      <p id="admin-user-summary" class="blog-muted"></p>
    </div>
    <div class="head-actions">
      <button id="admin-refresh-btn">刷新数据</button>
      <button id="logout-btn" class="danger">退出登录</button>
    </div>
  </header>

  <div class="blog-panel blog-panel-accent">
    <div class="panel-heading">
      <div>
        <p class="section-kicker">总览</p>
        <h2>后台数据</h2>
      </div>
      <div id="admin-status" class="blog-status" aria-live="polite"></div>
    </div>
    <div id="admin-stats" class="stats-grid"></div>
  </div>

  <div class="blog-panel" id="admin-publish-section">
    <div class="panel-heading compact-heading">
      <div>
        <p class="section-kicker">内容发布</p>
        <h2>管理员发布文章</h2>
        <p class="blog-muted">管理员可以在后台直接发布正式文章，不进入普通用户的 AI 审核队列；分类、标签和附件会同步写入数据库。</p>
      </div>
      <div id="admin-publish-status" class="blog-status" aria-live="polite"></div>
    </div>
    <form id="article-form" class="blog-form editor-form">
      <input type="hidden" id="article-id">
      <input type="hidden" id="article-status" value="PUBLISHED">
      <div class="editor-main">
        <input id="article-title" required placeholder="文章标题">
        <textarea id="article-summary" placeholder="一句话摘要，留空也可以"></textarea>
        <textarea id="article-content" required placeholder="文章正文" rows="10"></textarea>
      </div>
      <aside class="editor-side" aria-label="文章设置">
        <label class="field-label" for="article-category">文章分类</label>
        <select id="article-category"></select>
        <div class="category-compose">
          <input id="article-category-custom" placeholder="自定义分类，例如 项目总结">
          <button type="button" id="ai-category-btn" class="secondary">AI 推荐分类</button>
        </div>
        <p class="blog-muted compact-help">选择已有分类，或填写新分类；保存时会自动补充分类库。</p>
        <label class="field-label" for="article-tag-input">文章标签</label>
        <div class="tag-compose">
          <input id="article-tag-input" placeholder="输入标签，用逗号、空格或顿号分隔">
          <button type="button" id="ai-tags-btn" class="secondary">AI 推荐标签</button>
        </div>
        <div id="article-tags" class="tag-preview" aria-live="polite"></div>
        <label class="field-label" for="article-ai-instruction">给 AI 的提示词</label>
        <textarea id="article-ai-instruction" class="ai-instruction-input" rows="4" placeholder="告诉 AI：文章面向谁、重点是什么、希望输出什么风格。">请结合文章标题、正文、分类和标签，生成适合博客系统直接使用的中文结果；如果信息不足，请基于已有内容给出保守建议。</textarea>
        <p class="blog-muted compact-help">AI 大纲、摘要、分类和标签推荐都会读取这段提示词。</p>
        <label class="field-label" for="article-files">文章附件</label>
        <input id="article-files" type="file" multiple accept="image/*,.ppt,.pptx,.pdf,.doc,.docx,.xls,.xlsx,.zip,.txt,.md">
        <div id="article-attachments" class="attachment-list" aria-live="polite"></div>
      </aside>
      <div class="blog-form-actions editor-actions">
        <button type="submit">直接发布文章</button>
        <button type="button" id="article-reset-btn" class="secondary">清空表单</button>
        <button type="button" id="ai-outline-btn" class="secondary">AI 生成大纲</button>
        <button type="button" id="ai-summary-btn" class="secondary">AI 生成摘要</button>
      </div>
    </form>
    <div id="ai-output" class="ai-output"></div>
  </div>

  <div class="blog-panel" id="users">
    <div class="panel-heading">
      <div>
        <p class="section-kicker">账号</p>
        <h2>用户管理</h2>
        <p class="blog-muted">查看用户、调整身份、封禁违规用户，或删除无文章和无评论的用户。</p>
      </div>
    </div>
    <div id="admin-users" class="admin-list"></div>
  </div>

  <div class="blog-panel" id="articles">
    <div class="panel-heading">
      <div>
        <p class="section-kicker">内容</p>
        <h2>文章审核与管理</h2>
        <p class="blog-muted">AI 初审正常的文章会直接上架；这里只需重点判断 AI 标记为疑似问题的待审核文章。</p>
      </div>
    </div>
    <div id="admin-articles" class="admin-list"></div>
  </div>

  <div class="blog-panel" id="comments">
    <div class="panel-heading">
      <div>
        <p class="section-kicker">互动</p>
        <h2>评论审核</h2>
        <p class="blog-muted">AI 初审正常的评论会直接公开；疑似问题评论进入待审核，可由管理员通过、下架或删除。</p>
      </div>
    </div>
    <div id="admin-comments" class="admin-list"></div>
  </div>

  <div class="blog-panel" id="categories">
    <div class="panel-heading compact-heading">
      <div>
        <p class="section-kicker">分类</p>
        <h2 id="category-manage">分类管理</h2>
      </div>
    </div>
    <form id="category-form" class="blog-form inline-form">
      <input id="category-name" required placeholder="分类名称">
      <input id="category-description" placeholder="分类说明">
      <button type="submit">新增分类</button>
    </form>
    <div id="admin-categories" class="admin-list compact-list"></div>
  </div>

  <div class="blog-panel" id="tags">
    <div class="panel-heading compact-heading">
      <div>
        <p class="section-kicker">标签</p>
        <h2 id="tag-manage">标签管理</h2>
      </div>
    </div>
    <form id="tag-form" class="blog-form inline-form">
      <input id="tag-name" required placeholder="标签名称">
      <button type="submit">新增标签</button>
    </form>
    <div id="admin-tags" class="tag-zone"></div>
  </div>

  <div class="blog-panel" id="ai-logs">
    <div class="panel-heading">
      <div>
        <p class="section-kicker">AI</p>
        <h2 id="ai-log-manage">AI 使用记录</h2>
        <p class="blog-muted">小埋会把大纲、摘要、标签推荐、评论审核和博客问答都记到 ai_usage_logs 表里，DeepSeek 的思考过程也会一起留档，方便管理员回看。</p>
      </div>
    </div>
    <div id="admin-ai-logs" class="admin-list"></div>
  </div>
</section>

<script src="/js/blog-api.js"></script>
