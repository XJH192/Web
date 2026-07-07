---
title: 用户工作台
permalink: blog.html
cover: /images/post/JavaScript.jpg
coverWidth: 1200
coverHeight: 320
---

<link rel="stylesheet" href="/blog-system.css">

<section class="blog-app" id="blog-page">
  <header class="blog-app-head">
    <div>
      <p class="section-kicker">普通用户</p>
      <h2>博客工作台</h2>
      <p id="blog-user-summary" class="blog-muted"></p>
    </div>
    <button id="logout-btn" class="danger">退出登录</button>
  </header>

  <section class="blog-panel blog-panel-accent">
    <div class="panel-heading">
      <div>
        <p class="section-kicker">首页</p>
        <h2>已上架博客</h2>
        <p class="blog-muted">查看其他用户审核通过的文章，支持点赞和提交评论。评论通过管理员审核后公开显示。</p>
      </div>
      <div id="home-status" class="blog-status" aria-live="polite"></div>
    </div>
    <div class="blog-toolbar compact-toolbar" aria-label="首页文章筛选">
      <input id="home-keyword" placeholder="搜索首页文章">
      <select id="home-category-filter"><option value="">全部分类</option></select>
      <select id="home-tag-filter"><option value="">全部标签</option></select>
      <button id="home-search-btn">刷新首页</button>
    </div>
    <div id="home-article-list" class="article-grid feed-grid"></div>
  </section>

  <section class="blog-panel">
    <div class="panel-heading">
      <div>
        <p class="section-kicker">我的内容</p>
        <h2>文章管理</h2>
        <p class="blog-muted">草稿仅自己可见；提交审核后，管理员通过才会上架到首页。</p>
      </div>
      <div id="blog-status" class="blog-status" aria-live="polite"></div>
    </div>
    <div class="blog-toolbar compact-toolbar" aria-label="我的文章筛选">
      <input id="blog-keyword" placeholder="搜索我的文章标题、摘要或正文">
      <select id="blog-category-filter"><option value="">全部分类</option></select>
      <select id="blog-tag-filter"><option value="">全部标签</option></select>
      <button id="blog-search-btn">搜索我的文章</button>
    </div>
    <div id="article-list" class="article-grid"></div>
  </section>

  <section class="blog-panel">
    <div class="panel-heading">
      <div>
        <p class="section-kicker">编辑器</p>
        <h2>发布或编辑文章</h2>
        <p class="blog-muted">文章会同步保存到 MySQL 的 articles 与 article_tags 表。</p>
      </div>
    </div>
    <form id="article-form" class="blog-form editor-form">
      <input type="hidden" id="article-id">
      <div class="editor-main">
        <input id="article-title" required placeholder="文章标题">
        <textarea id="article-summary" placeholder="文章摘要"></textarea>
        <textarea id="article-content" required placeholder="文章正文" rows="9"></textarea>
      </div>
      <aside class="editor-side" aria-label="文章设置">
        <label class="field-label" for="article-category">文章分类</label>
        <select id="article-category" required></select>
        <label class="field-label" for="article-status">保存方式</label>
        <select id="article-status">
          <option value="PENDING">提交审核</option>
          <option value="DRAFT">保存草稿</option>
        </select>
        <label class="field-label" for="article-tag-input">文章标签</label>
        <div class="tag-compose">
          <input id="article-tag-input" placeholder="输入标签，用逗号分隔；也可点击 AI 推荐">
          <button type="button" id="ai-tags-btn" class="secondary">AI 推荐标签</button>
        </div>
        <div id="article-tags" class="tag-preview" aria-live="polite"></div>
        <p class="blog-muted">AI 会根据当前标题和正文推荐标签，你也可以自己填写。</p>
        <label class="field-label" for="article-files">文章附件</label>
        <input id="article-files" type="file" multiple accept="image/*,.ppt,.pptx,.pdf,.doc,.docx,.xls,.xlsx,.zip,.txt,.md">
        <div id="article-attachments" class="attachment-list" aria-live="polite"></div>
      </aside>
      <div class="blog-form-actions editor-actions">
        <button type="submit">保存文章</button>
        <button type="button" id="article-reset-btn" class="secondary">清空表单</button>
        <button type="button" id="ai-outline-btn" class="secondary">AI 生成大纲</button>
        <button type="button" id="ai-summary-btn" class="secondary">AI 生成摘要</button>
      </div>
    </form>
    <div id="ai-output" class="ai-output"></div>
  </section>
</section>

<script src="/js/blog-api.js"></script>