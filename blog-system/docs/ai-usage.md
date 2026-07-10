# AI 辅助功能与大模型接入说明

## 当前功能

后端实现位置：`blog-system/backend/src/main/java/com/tangyuxian/blog/service/AiService.java`

已接入页面功能：

1. AI 生成大纲：根据文章标题、正文、分类、标签和用户提示词生成写作结构。
2. AI 生成摘要：根据文章上下文生成短摘要。
3. AI 推荐标签：根据当前文章上下文推荐 3-6 个标签，并自动填入编辑器标签框。
4. AI 评论审核：评论提交时做本地风险词初审，管理员仍可最终审核。
5. AI 博客问答：读取当前已发布文章的标题、摘要、正文、分类和标签，调用 DeepSeek 基于博客内容回答用户问题，并返回依据文章。

## 标签推荐逻辑

系统现在是“真实调用优先”：

- 默认必须调用 DeepSeek 或其他 OpenAI 兼容接口，失败时页面会显示真实错误，不再用固定模板伪生成。
- 如需课堂演示时临时启用本地兜底，可设置 `AI_ALLOW_LOCAL_FALLBACK=true`。

## 评论审核词库

评论提交时会先进入本地初审，命中风险词的评论会直接标记为 `REJECTED`，普通评论进入 `PENDING` 等待管理员最终审核。词库按场景分为广告引流、辱骂攻击、低俗色情、诈骗黑产四类，并会先对评论做简单归一化处理，兼容大小写、全角字符、空格和标点穿插等变体写法。

## 接入 DeepSeek API

DeepSeek 使用 OpenAI 兼容的 Chat Completions 格式。启动后端前设置环境变量即可：

```powershell
$env:DEEPSEEK_API_KEY="你的 DeepSeek API Key"
$env:DEEPSEEK_MODEL="deepseek-v4-pro"
$env:DEEPSEEK_THINKING="enabled"
$env:AI_ALLOW_LOCAL_FALLBACK="false"
npm run web
```

如果你想显式指定接口地址：

```powershell
$env:AI_CHAT_ENDPOINT="https://api.deepseek.com/chat/completions"
$env:AI_CHAT_API_KEY="你的 DeepSeek API Key"
$env:AI_CHAT_MODEL="deepseek-v4-pro"
$env:AI_CHAT_THINKING="enabled"
npm run web
```

推荐使用 `AI_CHAT_*`，它更通用；`DEEPSEEK_*` 是为了方便 DeepSeek 单独配置。
pm run web` 启动时会自动忽略不可用的 127.0.0.1 本地代理，避免 DeepSeek 请求被坏代理截断。

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

AI 调用会写入 `ai_usage_logs` 表，管理员后台可以查看功能类型、完整提示词、DeepSeek 返回的思考过程/处理摘要和输出结果。标签推荐生成的新标签会自动写入 `tags` 表，并通过 `article_tags` 与文章关联。
