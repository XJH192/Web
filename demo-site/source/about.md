---
title: "关于 Ciallo～(∠・ω< )⌒☆ 博客系统"
layout: py
permalink: about.html
cover: /images/background/zhuti.jpg
coverWidth: 1920
coverHeight: 620
toc: true
---

<div class="about-page ciallo-about-page">
  <section class="about-hero-panel">
    <div>
      <p class="about-kicker">ABOUT Ciallo～(∠・ω&lt; )⌒☆ BLOG</p>
      <h2>把 Hexo 主题做成可登录、可审核、可互动的博客系统</h2>
      <p>这个网站保留小埋主题的视觉气质，同时加入 Spring Boot、MySQL、管理员审核、文章互动和 AI 辅助写作。它不是单纯的静态主题展示，而是一个可以完成课程验收演示的完整博客产品。</p>
      <div class="about-actions">
        <a href="/login.html">进入登录</a>
        <a href="/blog.html">用户工作台</a>
        <a href="https://github.com/XJH192/Web" target="_blank" rel="noopener">代码仓库</a>
      </div>
    </div>
    <div class="about-profile-card">
      <img src="/images/head/head.jpg" alt="Ciallo～(∠・ω&lt; )⌒☆ 博客头像">
      <strong>Ciallo～(∠・ω&lt; )⌒☆ 博客系统</strong>
      <span>Spring Boot · MySQL · Hexo · AI</span>
    </div>
  </section>

  <section>
    <h2>系统能力</h2>
    <div class="about-feature-grid">
      <article><strong>用户写作</strong><span>登录后发布文章、保存草稿、上传图片/PPT/PDF 等附件，并等待管理员审核。</span></article>
      <article><strong>首页互动</strong><span>已上架文章支持阅读、点赞、评论、附件下载，作者能收到点赞和评论消息。</span></article>
      <article><strong>管理员审核</strong><span>管理员可审核文章与评论，管理用户状态、分类标签，并查看 AI 调用日志。</span></article>
      <article><strong>AI 辅助</strong><span>摘要、大纲、标签和分类推荐优先调用 DeepSeek API，失败时自动使用本地规则兜底。</span></article>
    </div>
  </section>

  <section>
    <h2>权限流程</h2>
    <div class="about-timeline">
      <div><b>1</b><strong>账号登录</strong><span>系统根据数据库中的角色进入用户工作台或管理员后台。</span></div>
      <div><b>2</b><strong>提交内容</strong><span>普通用户提交文章后进入待审核，草稿仅自己可见。</span></div>
      <div><b>3</b><strong>后台审核</strong><span>管理员通过后文章上架首页，驳回后用户可继续修改。</span></div>
      <div><b>4</b><strong>互动治理</strong><span>评论需要审核，违规评论可下架，违规用户可封禁。</span></div>
    </div>
  </section>

  <section>
    <h2>技术栈与数据表</h2>
    <div class="about-chip-cloud">
      <span>Hexo 前端主题</span><span>Spring Boot API</span><span>MySQL / Navicat</span><span>DeepSeek API</span><span>文章附件</span><span>权限控制</span><span>通知消息</span><span>AI 日志</span>
    </div>
    <div class="about-table-grid">
      <article><strong>users</strong><span>账号、邮箱、角色、封禁状态</span></article>
      <article><strong>articles</strong><span>正文、分类、状态、阅读量、点赞数</span></article>
      <article><strong>comments</strong><span>评论、回复、AI 审核结果</span></article>
      <article><strong>notifications</strong><span>审核、点赞、评论消息提醒</span></article>
      <article><strong>categories / tags</strong><span>自定义分类和文章标签</span></article>
      <article><strong>ai_usage_logs</strong><span>记录每次 AI 调用和结果</span></article>
    </div>
  </section>

  <section>
    <h2>AI 接入说明</h2>
    <p>后端使用兼容 OpenAI 格式的 DeepSeek Chat Completions 接口。启动前设置 <code>DEEPSEEK_API_KEY</code>，摘要、大纲、标签和分类推荐就会优先走远程模型；如果网络或 Key 不可用，系统仍会用本地规则返回结果，保证演示不中断。</p>
    <pre><code>$env:DEEPSEEK_API_KEY="你的 DeepSeek API Key"
npm run web</code></pre>
  </section>

  <section>
    <h2>联系与维护</h2>
    <p>这个项目的目标是让博客系统的功能路径讲得清、跑得通、页面也有自己的风格。后续可以继续扩展全文搜索、图片对象存储、Markdown 编辑器和更完整的 AI 内容审核。</p>
    <div class="about-actions compact">
      <a href="mailto:706972112@qq.com">706972112@qq.com</a>
      <a href="https://www.bilibili.com/" target="_blank" rel="noopener">B站首页</a>
      <a href="https://github.com/XJH192/Web" target="_blank" rel="noopener">GitHub 仓库</a>
    </div>
  </section>
</div>
