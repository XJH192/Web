---
title: 相册
date: 2026-07-06 14:40:00
permalink: gallery.html
cover: /images/background/相册.jpg
coverWidth: 1920
coverHeight: 760
tags:
  - 主题展示
  - 相册
categories:
  - 主题组件
---

<link rel="stylesheet" href="/blog-system.css">

<section class="blog-app gallery-app" id="gallery-page">
  <header class="blog-app-head" id="gallery-heading">
    <div>
      <p class="section-kicker">光影记录</p>
      <h2>我的相册</h2>
      <p class="blog-muted">每个账号拥有独立相册，默认包含现有 8 张图片；删除后可以上传新图片，也可以修改标题、说明或图片。</p>
    </div>
  </header>

  <section class="blog-panel gallery-manager-panel" id="gallery-manager" hidden>
    <div class="panel-heading compact-heading">
      <div>
        <p class="section-kicker">相册管理</p>
        <h2 id="gallery-form-heading">上传新图片</h2>
        <p class="blog-muted">在这里选择图片并上传；每张照片下方都有“修改”和“删除”按钮。最多保存 8 张。</p>
      </div>
      <span class="status-pill success">上传 / 修改 / 删除</span>
    </div>
    <form id="gallery-form" class="gallery-form">
      <label>图片标题
        <input id="gallery-title" maxlength="80" required placeholder="给这张图片起个名字">
      </label>
      <label>图片说明
        <textarea id="gallery-description" maxlength="500" rows="3" placeholder="记录拍摄地点、故事或灵感（可选）"></textarea>
      </label>
      <label>选择图片
        <input id="gallery-file" type="file" accept="image/jpeg,image/png,image/webp,image/gif">
      </label>
      <div class="gallery-upload-preview" id="gallery-upload-preview" hidden>
        <img id="gallery-preview-image" alt="待上传图片预览">
      </div>
      <div class="gallery-form-actions">
        <button id="gallery-submit" type="submit">上传图片</button>
        <button id="gallery-cancel-edit" class="secondary" type="button" hidden>取消修改</button>
      </div>
      <p id="gallery-form-status" class="blog-status" aria-live="polite"></p>
    </form>
  </section>

  <p id="gallery-login-hint" class="gallery-login-hint" hidden>
    想添加自己的照片？<a href="/login.html?redirect=%2Fgallery.html">登录后上传</a>
  </p>

  <section class="blog-panel gallery-list-panel">
    <div class="panel-heading compact-heading">
      <div>
        <p class="section-kicker">当前账号</p>
        <h2>我的相册墙</h2>
      </div>
      <span id="gallery-count" class="status-pill muted">0 张</span>
    </div>
    <p id="gallery-list-status" class="blog-status" aria-live="polite">正在加载相册...</p>
    <div id="gallery-photo-grid" class="managed-gallery-grid"></div>
  </section>
</section>

<script src="/js/blog-api.js"></script>
