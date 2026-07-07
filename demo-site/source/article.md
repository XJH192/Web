---
title: 文章详情
permalink: article.html
cover: /images/post/AI.jpg
coverWidth: 1200
coverHeight: 320
---

<link rel="stylesheet" href="/blog-system.css">

<section class="blog-app" id="article-page">
  <header class="blog-app-head">
    <div>
      <p class="section-kicker">文章</p>
      <h2>阅读与互动</h2>
      <p class="blog-muted">评论提交后进入管理员审核，通过后会公开显示。</p>
    </div>
    <div class="head-actions">
      <a class="blog-link-btn user-only-link" href="/blog.html">返回工作台</a>
      <a class="blog-link-btn admin-only-link" href="/admin.html">管理员后台</a>
    </div>
  </header>

  <article id="article-detail" class="blog-panel article-detail-panel"></article>

  <section class="blog-panel">
    <div class="panel-heading">
      <div>
        <p class="section-kicker">评论</p>
        <h2>评论与回复</h2>
      </div>
    </div>
    <div id="comment-list" class="comment-list"></div>
    <form id="comment-form" class="blog-form comment-form">
      <textarea id="comment-content" required placeholder="写下评论或回复" rows="4"></textarea>
      <input id="comment-parent-id" placeholder="回复评论编号，可留空">
      <button type="submit">提交评论</button>
      <p class="blog-muted">提交后会进入管理员审核，审核通过后才会显示。</p>
    </form>
  </section>

  <section class="blog-panel blog-panel-accent">
    <div class="panel-heading compact-heading">
      <div>
        <p class="section-kicker">AI</p>
        <h2>博客问答</h2>
      </div>
    </div>
    <div class="blog-form-row">
      <input id="qa-question" placeholder="请输入关于当前博客内容的问题">
      <button id="qa-btn">提问</button>
    </div>
    <div id="qa-output" class="ai-output"></div>
  </section>
</section>

<script src="/js/blog-api.js"></script>