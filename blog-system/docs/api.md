# REST API 文档

基础地址：`http://127.0.0.1:8080/api`

统一返回：

```json
{
  "success": true,
  "message": "ok",
  "data": {}
}
```

需要登录的接口在请求头中携带：

```http
X-Token: 登录接口返回的 token
```

## 认证

- `POST /auth/register` 注册
- `POST /auth/login` 登录
- `GET /auth/me` 当前用户

登录请求：

```json
{"username":"admin","password":"123456"}
```

## 文章

- `GET /articles` 文章列表，支持 `keyword`、`categoryId`、`tagId`、`includeDrafts`
- `GET /articles/{id}` 文章详情，同时阅读量 +1
- `POST /articles` 新增文章，需登录
- `PUT /articles/{id}` 更新文章，需作者或管理员
- `DELETE /articles/{id}` 删除文章，需作者或管理员

文章请求：

```json
{
  "title": "文章标题",
  "summary": "摘要",
  "content": "正文",
  "categoryId": 1,
  "tagIds": [1, 2],
  "status": "PUBLISHED"
}
```

## 分类标签

- `GET /categories`
- `POST /categories` 管理员
- `PUT /categories/{id}` 管理员
- `DELETE /categories/{id}` 管理员，已被文章引用时不允许删除
- `GET /tags`
- `POST /tags` 管理员
- `PUT /tags/{id}` 管理员
- `DELETE /tags/{id}` 管理员，已被文章引用时不允许删除

## 评论

- `GET /articles/{articleId}/comments` 查看已通过评论
- `POST /articles/{articleId}/comments` 发表评论或回复，需登录
- `GET /admin/comments` 管理员查看全部评论
- `PUT /admin/comments/{id}/moderate?status=APPROVED` 管理员审核
- `DELETE /admin/comments/{id}` 管理员删除

评论请求：

```json
{"content":"写得很好", "parentId": null}
```

## 后台

- `GET /admin/stats` 后台统计
- `GET /admin/users` 用户列表
- `GET /admin/articles` 文章列表，含草稿
- `GET /admin/ai-logs` AI 使用日志

## AI

- `POST /ai/outline` 根据标题生成大纲
- `POST /ai/summary` 根据正文生成摘要
- `POST /ai/tags` 推荐标签
- `POST /ai/qa` 博客问答

请求：

```json
{"title":"Spring Boot 博客系统", "content":"文章正文", "question":"系统有哪些功能？"}
```