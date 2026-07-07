# AI 辅助功能与大模型接入说明

## 当前功能

后端实现位置：`blog-system/backend/src/main/java/com/tangyuxian/blog/service/AiService.java`

已接入页面功能：

1. AI 生成大纲：根据文章标题生成写作结构。
2. AI 生成摘要：根据正文生成短摘要。
3. AI 推荐标签：根据当前文章标题和正文推荐 3-6 个标签，并自动填入编辑器标签框。
4. AI 评论审核：评论提交时做本地风险词初审，管理员仍可最终审核。
5. AI 博客问答：根据当前博客内容做简单问答演示。

## 标签推荐逻辑

系统现在是“双模式”：

- 未配置大模型 API：使用本地规则推荐标签，例如 Spring Boot、Hexo、AI、JavaScript、随笔。
- 配置大模型 API：优先调用 DeepSeek 或其他 OpenAI 兼容接口，失败时自动回退到本地规则。

## 接入 DeepSeek API

DeepSeek 使用 OpenAI 兼容的 Chat Completions 格式。启动后端前设置环境变量即可：

```powershell
$env:DEEPSEEK_API_KEY="你的 DeepSeek API Key"
$env:DEEPSEEK_MODEL="deepseek-chat"
npm run web
```

如果你想显式指定接口地址：

```powershell
$env:AI_CHAT_ENDPOINT="https://api.deepseek.com/chat/completions"
$env:AI_CHAT_API_KEY="你的 DeepSeek API Key"
$env:AI_CHAT_MODEL="deepseek-chat"
npm run web
```

推荐使用 `AI_CHAT_*`，它更通用；`DEEPSEEK_*` 是为了方便 DeepSeek 单独配置。

## 接入其他大模型

只要对方兼容 OpenAI 的 `/chat/completions` 格式，就可以这样配置：

```powershell
$env:AI_CHAT_ENDPOINT="https://你的服务商域名/v1/chat/completions"
$env:AI_CHAT_API_KEY="你的 API Key"
$env:AI_CHAT_MODEL="模型名称"
npm run web
```

常见可替换服务：DeepSeek、通义千问兼容接口、智谱兼容接口、硅基流动、OpenAI 兼容代理、Ollama 兼容代理等。不同平台的 endpoint 和 model 名称不同，以平台控制台为准。

## 数据库记录

AI 调用会写入 `ai_usage_logs` 表，管理员后台可以查看功能类型、输入内容和输出结果。标签推荐生成的新标签会自动写入 `tags` 表，并通过 `article_tags` 与文章关联。