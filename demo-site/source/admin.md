---
title: 管理员后台
permalink: admin.html
cover: /images/post/git.jpg
coverWidth: 1200
coverHeight: 320
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

  <div class="blog-panel">
    <div class="panel-heading">
      <div>
        <p class="section-kicker">账号</p>
        <h2>用户管理</h2>
        <p class="blog-muted">查看用户、调整身份、封禁违规用户，或删除无文章和无评论的用户。</p>
      </div>
    </div>
    <div id="admin-users" class="admin-list"></div>
  </div>

  <div class="blog-panel">
    <div class="panel-heading">
      <div>
        <p class="section-kicker">内容</p>
        <h2>文章审核与管理</h2>
        <p class="blog-muted">普通用户提交的文章默认待审核。通过后才会上架到用户首页。</p>
      </div>
    </div>
    <div id="admin-articles" class="admin-list"></div>
  </div>

  <div class="blog-panel">
    <div class="panel-heading">
      <div>
        <p class="section-kicker">互动</p>
        <h2>评论审核</h2>
        <p class="blog-muted">不合规评论可下架或删除，也可以封禁发布用户。</p>
      </div>
    </div>
    <div id="admin-comments" class="admin-list"></div>
  </div>

  <div class="blog-panel">
    <div class="panel-heading compact-heading">
      <div>
        <p class="section-kicker">分类</p>
        <h2>分类管理</h2>
      </div>
    </div>
    <form id="category-form" class="blog-form inline-form">
      <input id="category-name" required placeholder="分类名称">
      <input id="category-description" placeholder="分类说明">
      <button type="submit">新增分类</button>
    </form>
    <div id="admin-categories" class="admin-list compact-list"></div>
  </div>

  <div class="blog-panel">
    <div class="panel-heading compact-heading">
      <div>
        <p class="section-kicker">标签</p>
        <h2>标签管理</h2>
      </div>
    </div>
    <form id="tag-form" class="blog-form inline-form">
      <input id="tag-name" required placeholder="标签名称">
      <button type="submit">新增标签</button>
    </form>
    <div id="admin-tags" class="tag-zone"></div>
  </div>

  <div class="blog-panel">
    <div class="panel-heading">
      <div>
        <p class="section-kicker">AI</p>
        <h2>AI 使用记录</h2>
        <p class="blog-muted">文章大纲、摘要、标签推荐、评论审核和博客问答都会写入 ai_usage_logs 表。</p>
      </div>
    </div>
    <div id="admin-ai-logs" class="admin-list"></div>
  </div>
</section>

<script src="/js/blog-api.js"></script>