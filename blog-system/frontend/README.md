# Frontend Notes

本项目复用仓库现有 Hexo 主题作为前端，因此动态页面没有放在 `blog-system/frontend` 内，而是直接集成到：

```text
D:\hexo-theme-tangyuxian\demo-site\source
```

新增页面：

- blog.md -> /blog.html
- article.md -> /article.html?id=1
- login.md -> /login.html
- admin.md -> /admin.html

新增资源：

- demo-site/source/js/blog-api.js
- demo-site/source/blog-system.css

这些页面通过 `fetch('http://127.0.0.1:8080/api/...')` 调用 Spring Boot 后端。
