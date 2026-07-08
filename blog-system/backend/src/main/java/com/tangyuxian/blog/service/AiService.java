package com.tangyuxian.blog.service;

import com.tangyuxian.blog.dto.AiRequest;
import com.tangyuxian.blog.model.AiUsageLog;
import com.tangyuxian.blog.model.Article;
import com.tangyuxian.blog.model.Category;
import com.tangyuxian.blog.repository.InMemoryBlogRepository;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.net.Proxy;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiService {
    private static final String DEFAULT_INSTRUCTION = "请结合标题、正文、现有分类和已有标签，输出适合博客系统直接使用的中文结果；如果信息不足，请基于已有内容保守推荐。";

    private final InMemoryBlogRepository repository;
    private final Environment environment;
    private final RestTemplate restTemplate = createRestTemplate();

    public AiService(InMemoryBlogRepository repository, Environment environment) {
        this.repository = repository;
        this.environment = environment;
    }

    public Map<String, Object> outline(AiRequest request) {
        String title = safe(request.getTitle(), "未命名文章");
        String prompt = articlePrompt(request, "请为这篇文章生成 4 到 6 条中文大纲。每条单独一行，保留清晰层级，不要输出解释。");
        ChatResult remote = chat("你是博客写作大纲助手。输出要简洁、可直接复制到文章编辑器。", prompt, 700);
        List<String> remoteOutline = parseOutline(remote.getContent(), 6);
        if (!remoteOutline.isEmpty()) {
            Map<String, Object> remoteMap = new LinkedHashMap<String, Object>();
            remoteMap.put("title", title);
            remoteMap.put("outline", remoteOutline);
            remoteMap.put("mode", remote.getMode());
            remoteMap.put("thinking", thinkingSummary(remote, "根据用户提示词、标题和正文提取主题，再拆成可写作的文章层级。"));
            log("AI_OUTLINE", prompt, remoteMap.toString(), thinkingSummary(remote, "根据用户提示词、标题和正文提取主题，再拆成可写作的文章层级。"));
            return remoteMap;
        }
        if (!allowLocalFallback()) return aiError("AI_OUTLINE", prompt, remote, "DeepSeek 大纲生成失败");
        List<String> outline = Arrays.asList("引言：说明《" + title + "》的写作背景", "核心观点：拆解主题中的关键问题", "实践过程：结合项目或案例展开说明", "总结反思：给出经验、问题与后续计划");
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("title", title);
        map.put("outline", outline);
        map.put("mode", "local-rule-fallback");
        log("AI_OUTLINE", prompt, map.toString(), "DeepSeek 暂不可用，按配置启用本地兜底大纲。");
        return map;
    }

    public Map<String, Object> summary(AiRequest request) {
        String prompt = articlePrompt(request, "请生成 60 到 120 字中文摘要，突出主题、做法和价值。只输出摘要正文，不要输出标题或解释。");
        ChatResult remote = chat("你是博客摘要助手。摘要要自然、具体、像真实博客首页摘要。", prompt, 360);
        String remoteSummary = compactText(remote.getContent(), 180);
        if (!remoteSummary.isEmpty()) {
            Map<String, Object> remoteMap = new LinkedHashMap<String, Object>();
            remoteMap.put("summary", remoteSummary);
            remoteMap.put("mode", remote.getMode());
            remoteMap.put("thinking", thinkingSummary(remote, "先识别文章主题、关键做法和读者收益，再压缩为首页摘要。"));
            log("AI_SUMMARY", prompt, remoteMap.toString(), thinkingSummary(remote, "先识别文章主题、关键做法和读者收益，再压缩为首页摘要。"));
            return remoteMap;
        }
        if (!allowLocalFallback()) return aiError("AI_SUMMARY", prompt, remote, "DeepSeek 摘要生成失败");
        String content = safe(request.getContent(), "");
        String summary = content.length() <= 80 ? content : content.substring(0, 80) + "...";
        if (summary.isEmpty()) summary = "请先输入文章内容，再生成摘要。";
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("summary", summary);
        map.put("mode", "local-rule-fallback");
        log("AI_SUMMARY", prompt, map.toString(), "DeepSeek 暂不可用，按配置启用本地摘要兜底。");
        return map;
    }

    public Map<String, Object> recommendTags(AiRequest request) {
        String prompt = articlePrompt(request, "请推荐 3 到 6 个文章标签。标签可以是中文或技术英文，用中文逗号分隔，只输出标签，不要解释。");
        ChatResult remote = chat("你是博客系统的标签推荐助手。标签应短、清晰、利于归档和搜索。", prompt, 220);
        List<String> remoteTags = parseLabels(remote.getContent(), 6);
        if (!remoteTags.isEmpty()) {
            Map<String, Object> remoteMap = new LinkedHashMap<String, Object>();
            remoteMap.put("tags", remoteTags);
            remoteMap.put("mode", remote.getMode());
            remoteMap.put("thinking", thinkingSummary(remote, "从标题和正文中提取技术栈、主题和场景词，合并为不重复标签。"));
            log("AI_TAGS", prompt, remoteMap.toString(), thinkingSummary(remote, "从标题和正文中提取技术栈、主题和场景词，合并为不重复标签。"));
            return remoteMap;
        }
        if (!allowLocalFallback()) return aiError("AI_TAGS", prompt, remote, "DeepSeek 标签推荐失败");
        String text = safe(request.getTitle(), "") + " " + safe(request.getContent(), "");
        List<String> tags = new ArrayList<String>();
        String lower = text.toLowerCase();
        if (lower.contains("spring") || lower.contains("java")) tags.add("Spring Boot");
        if (lower.contains("hexo") || lower.contains("博客")) tags.add("Hexo");
        if (lower.contains("ai") || lower.contains("智能") || lower.contains("摘要")) tags.add("AI");
        if (lower.contains("前端") || lower.contains("javascript")) tags.add("前端");
        if (tags.isEmpty()) tags.add("随笔");
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("tags", tags);
        map.put("mode", "local-rule-fallback");
        log("AI_TAGS", prompt, map.toString(), "DeepSeek 暂不可用，按配置启用关键词标签兜底。");
        return map;
    }

    public Map<String, Object> recommendCategory(AiRequest request) {
        String prompt = articlePrompt(request, "现有分类：" + categoryNames() + "\n请推荐 1 个文章分类。优先使用现有分类；如果都不合适，可以给出一个 2 到 8 个字的新分类。只输出分类名，不要解释。");
        ChatResult remote = chat("你是博客系统的信息架构助手。分类要稳定、好维护，不要过细。", prompt, 140);
        String remoteCategory = firstLabel(remote.getContent());
        if (!remoteCategory.isEmpty()) {
            Map<String, Object> map = new LinkedHashMap<String, Object>();
            map.put("category", remoteCategory);
            map.put("mode", remote.getMode());
            map.put("thinking", thinkingSummary(remote, "先判断文章所属主题，再尽量映射到已有分类，必要时给出一个新分类。"));
            log("AI_CATEGORY", prompt, map.toString(), thinkingSummary(remote, "先判断文章所属主题，再尽量映射到已有分类，必要时给出一个新分类。"));
            return map;
        }
        if (!allowLocalFallback()) return aiError("AI_CATEGORY", prompt, remote, "DeepSeek 分类推荐失败");
        String fallback = localCategory(request);
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("category", fallback);
        map.put("mode", "local-rule-fallback");
        log("AI_CATEGORY", prompt, map.toString(), "DeepSeek 暂不可用，按配置启用关键词分类兜底。");
        return map;
    }

    public String reviewComment(String content) {
        String text = safe(content, "").toLowerCase();
        String result;
        String thinking;
        if (text.contains("广告") || text.contains("加微信") || text.contains("spam") || text.contains("http://") || text.contains("https://")) {
            result = "REJECT: 疑似广告或垃圾信息";
            thinking = "本地审核规则命中广告、外链或垃圾信息关键词。";
        } else if (text.contains("骂") || text.contains("垃圾") || text.contains("hate")) {
            result = "REJECT: 疑似不文明内容";
            thinking = "本地审核规则命中不文明或攻击性表达关键词。";
        } else {
            result = "PASS: 普通评论";
            thinking = "本地审核规则未发现明显广告、外链或不文明关键词。";
        }
        log("AI_COMMENT_REVIEW", content, result, thinking);
        return result;
    }

    public Map<String, Object> answer(AiRequest request) {
        String question = safe(request.getQuestion(), "");
        StringBuilder evidence = new StringBuilder();
        for (Article article : repository.listArticles()) {
            if (evidence.length() > 0) evidence.append("；");
            evidence.append(article.getTitle());
            if (evidence.length() > 160) break;
        }
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("question", question);
        map.put("answer", "根据当前博客内容，可以从这些文章入手：" + evidence.toString() + "。当前为本地规则回答，后续可替换为知识库问答或大模型 API。");
        map.put("mode", "local-rule-fallback");
        log("AI_QA", question, map.toString(), "读取当前文章标题作为证据，生成本地问答回复。若接入知识库，可改为向量检索后再调用 DeepSeek。");
        return map;
    }

    private static RestTemplate createRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15000);
        factory.setReadTimeout(60000);
        factory.setProxy(Proxy.NO_PROXY);
        return new RestTemplate(factory);
    }

    private ChatResult chat(String systemPrompt, String userPrompt, int maxTokens) {
        String apiKey = env("AI_CHAT_API_KEY", env("DEEPSEEK_API_KEY", ""));
        if (apiKey.trim().isEmpty()) return ChatResult.error("未配置 DEEPSEEK_API_KEY 或 AI_CHAT_API_KEY");
        String endpoint = env("AI_CHAT_ENDPOINT", "").trim();
        if (endpoint.isEmpty()) endpoint = trimSlash(env("AI_CHAT_BASE_URL", env("DEEPSEEK_BASE_URL", "https://api.deepseek.com"))) + "/chat/completions";
        String model = env("AI_CHAT_MODEL", env("DEEPSEEK_MODEL", "deepseek-v4-pro"));
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);
            Map<String, Object> system = new LinkedHashMap<String, Object>();
            system.put("role", "system");
            system.put("content", systemPrompt);
            Map<String, Object> user = new LinkedHashMap<String, Object>();
            user.put("role", "user");
            user.put("content", userPrompt);
            Map<String, Object> body = new LinkedHashMap<String, Object>();
            body.put("model", model);
            body.put("messages", Arrays.asList(system, user));
            body.put("max_tokens", maxTokens);
            String lowerModel = model.toLowerCase();
            String thinkingMode = env("AI_CHAT_THINKING", env("DEEPSEEK_THINKING", lowerModel.contains("reasoner") ? "" : "enabled"));
            if (!thinkingMode.isEmpty() && !lowerModel.contains("reasoner")) {
                Map<String, Object> thinking = new LinkedHashMap<String, Object>();
                thinking.put("type", thinkingMode);
                body.put("thinking", thinking);
            }
            if (!lowerModel.contains("reasoner") && thinkingMode.isEmpty()) body.put("temperature", 0.2);
            ResponseEntity<Map> response = restTemplate.postForEntity(endpoint, new HttpEntity<Map<String, Object>>(body, headers), Map.class);
            if (response.getBody() == null) return ChatResult.error("DeepSeek 返回为空响应");
            Object responseModel = response.getBody().get("model");
            Object choicesObj = response.getBody().get("choices");
            if (!(choicesObj instanceof List) || ((List) choicesObj).isEmpty()) return ChatResult.error("DeepSeek 返回 choices 为空");
            Object firstObj = ((List) choicesObj).get(0);
            if (!(firstObj instanceof Map)) return ChatResult.error("DeepSeek 返回 choice 格式异常");
            Map first = (Map) firstObj;
            Object messageObj = first.get("message");
            String content = "";
            String thinking = "";
            if (messageObj instanceof Map) {
                Map message = (Map) messageObj;
                content = extractText(message.get("content"));
                thinking = firstNonEmpty(extractText(message.get("reasoning_content")), extractText(message.get("thinking")), extractText(message.get("reasoning")));
            }
            if (content.isEmpty()) content = extractText(first.get("text"));
            if (thinking.isEmpty()) thinking = firstNonEmpty(extractText(first.get("reasoning_content")), extractText(first.get("thinking")), extractText(first.get("reasoning")));
            if (content.isEmpty()) return ChatResult.error("DeepSeek 未返回正文内容");
            return new ChatResult(content, thinking, String.valueOf(responseModel == null ? model : responseModel), endpoint, "");
        } catch (HttpStatusCodeException ex) {
            String detail = "HTTP " + ex.getRawStatusCode() + " " + compactText(ex.getResponseBodyAsString(), 500);
            log("AI_REMOTE_ERROR", userPrompt, detail, "DeepSeek 接口返回 HTTP 错误。请检查模型名、API Key、余额或请求参数。");
            return ChatResult.error(detail);
        } catch (Exception ex) {
            String detail = ex.getClass().getSimpleName() + ": " + safe(ex.getMessage(), "未知错误");
            if (detail.contains("127.0.0.1") || detail.toLowerCase().contains("proxy")) {
                detail += "。检测到本机代理可能不可用，请关闭 HTTP_PROXY/HTTPS_PROXY/ALL_PROXY 或启动对应代理。";
            }
            log("AI_REMOTE_ERROR", userPrompt, detail, "调用 DeepSeek 接口失败，已停止本地伪生成。错误信息：" + detail);
            return ChatResult.error(detail);
        }
    }

    private Map<String, Object> aiError(String feature, String prompt, ChatResult remote, String title) {
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        String detail = remote == null ? "未知错误" : safe(remote.getError(), "DeepSeek 未返回有效内容");
        map.put("error", title + "：" + detail);
        map.put("mode", "remote-deepseek-error");
        map.put("thinking", "已向 DeepSeek 发起请求，但未获得可用结果；系统没有使用固定模板替代。错误：" + detail);
        log(feature, prompt, map.toString(), String.valueOf(map.get("thinking")));
        return map;
    }

    private String articlePrompt(AiRequest request, String task) {
        StringBuilder builder = new StringBuilder();
        builder.append("用户给 AI 的提示词：").append(safe(request.getInstruction(), DEFAULT_INSTRUCTION)).append("\n");
        builder.append("任务：").append(task).append("\n");
        builder.append("标题：").append(safe(request.getTitle(), "未命名文章")).append("\n");
        builder.append("当前分类：").append(safe(request.getCategoryName(), "未选择")).append("\n");
        builder.append("当前标签：").append(joinTags(request.getTagNames())).append("\n");
        builder.append("正文：").append(safe(request.getContent(), ""));
        return builder.toString();
    }

    private String joinTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) return "暂无";
        List<String> values = new ArrayList<String>();
        for (String tag : tags) {
            if (tag != null && !tag.trim().isEmpty()) values.add(tag.trim());
        }
        return values.isEmpty() ? "暂无" : values.toString();
    }

    private String thinkingSummary(ChatResult result, String fallback) {
        if (result != null && !safe(result.getThinking(), "").isEmpty()) return result.getThinking();
        String model = result == null ? "DeepSeek" : result.getModel();
        return "模型：" + model + "。处理摘要：" + fallback;
    }

    private List<String> parseOutline(String content, int max) {
        List<String> result = new ArrayList<String>();
        String clean = safe(content, "").replace("\r", "\n");
        String[] lines = clean.split("\n+");
        for (String line : lines) {
            String item = line.replaceAll("^[\\s\\-*>#]*(\\d+[\\.、)]|[一二三四五六][、.])?\\s*", "").trim();
            item = item.replaceAll("^[-•]+\\s*", "").trim();
            if (item.isEmpty() || item.length() < 3 || result.contains(item)) continue;
            result.add(item.length() > 80 ? item.substring(0, 80) : item);
            if (result.size() >= max) break;
        }
        return result;
    }

    private String compactText(String content, int max) {
        String text = safe(content, "").replace("\r", " ").replace("\n", " ").replace("\"", "").trim();
        text = text.replaceAll("\\s+", " ");
        if (text.startsWith("摘要：")) text = text.substring(3).trim();
        return text.length() > max ? text.substring(0, max) : text;
    }

    private List<String> parseLabels(String content, int max) {
        List<String> result = new ArrayList<String>();
        String clean = safe(content, "").replace("[", "").replace("]", "").replace("#", "");
        String[] parts = clean.split("[，,、;；\\n\\r\\t ]+");
        for (String part : parts) {
            String label = part.replace("\"", "").replace("'", "").trim();
            if (label.isEmpty() || result.contains(label)) continue;
            result.add(label.length() > 30 ? label.substring(0, 30) : label);
            if (result.size() >= max) break;
        }
        return result;
    }

    private String firstLabel(String content) {
        List<String> labels = parseLabels(content, 1);
        return labels.isEmpty() ? "" : labels.get(0);
    }

    private String categoryNames() {
        List<String> names = new ArrayList<String>();
        for (Category category : repository.listCategories()) names.add(category.getName());
        return names.isEmpty() ? "技术随笔，生活记录，AI 实践" : names.toString();
    }

    private String localCategory(AiRequest request) {
        String text = (safe(request.getTitle(), "") + " " + safe(request.getContent(), "")).toLowerCase();
        if (text.contains("ai") || text.contains("智能") || text.contains("deepseek") || text.contains("模型")) return "AI 实践";
        if (text.contains("spring") || text.contains("java") || text.contains("hexo") || text.contains("前端") || text.contains("数据库")) return "技术随笔";
        return "生活记录";
    }

    private boolean allowLocalFallback() {
        return "true".equalsIgnoreCase(env("AI_ALLOW_LOCAL_FALLBACK", "false"));
    }

    private String env(String key, String fallback) {
        String value = environment.getProperty(key);
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private String trimSlash(String value) {
        String result = safe(value, "");
        while (result.endsWith("/")) result = result.substring(0, result.length() - 1);
        return result;
    }

    private String extractText(Object value) {
        if (value == null) return "";
        if (value instanceof String) return ((String) value).trim();
        if (value instanceof Map) {
            Map map = (Map) value;
            return firstNonEmpty(extractText(map.get("content")), extractText(map.get("text")), String.valueOf(value));
        }
        return String.valueOf(value).trim();
    }

    private String firstNonEmpty(String... values) {
        if (values == null) return "";
        for (String value : values) {
            if (value != null && !value.trim().isEmpty()) return value.trim();
        }
        return "";
    }

    private String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private void log(String feature, String prompt, String result) {
        log(feature, prompt, result, "");
    }

    private void log(String feature, String prompt, String result, String thinking) {
        String shortPrompt = prompt == null ? "" : prompt;
        String shortThinking = thinking == null ? "" : thinking;
        String shortResult = result == null ? "" : result;
        if (shortPrompt.length() > 1000) shortPrompt = shortPrompt.substring(0, 1000);
        if (shortThinking.length() > 2000) shortThinking = shortThinking.substring(0, 2000);
        if (shortResult.length() > 1000) shortResult = shortResult.substring(0, 1000);
        repository.saveAiUsageLog(new AiUsageLog(null, null, feature, shortPrompt, shortThinking, shortResult, LocalDateTime.now()));
    }

    private static class ChatResult {
        private final String content;
        private final String thinking;
        private final String model;
        private final String endpoint;
        private final String error;

        private ChatResult(String content, String thinking, String model, String endpoint, String error) {
            this.content = content == null ? "" : content.trim();
            this.thinking = thinking == null ? "" : thinking.trim();
            this.model = model == null ? "DeepSeek" : model;
            this.endpoint = endpoint == null ? "" : endpoint;
            this.error = error == null ? "" : error.trim();
        }

        static ChatResult error(String error) {
            return new ChatResult("", "", "DeepSeek", "", error);
        }

        String getContent() { return content; }
        String getThinking() { return thinking; }
        String getModel() { return model; }
        String getError() { return error; }
        String getMode() { return error.isEmpty() && !endpoint.isEmpty() ? "remote-deepseek" : "remote-deepseek-error"; }
    }
}