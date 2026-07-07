package com.tangyuxian.blog.service;

import com.tangyuxian.blog.dto.AiRequest;
import com.tangyuxian.blog.model.AiUsageLog;
import com.tangyuxian.blog.model.Article;
import com.tangyuxian.blog.repository.InMemoryBlogRepository;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AiService {
    private final InMemoryBlogRepository repository;
    private final Environment environment;
    private final RestTemplate restTemplate = new RestTemplate();

    public AiService(InMemoryBlogRepository repository, Environment environment) {
        this.repository = repository;
        this.environment = environment;
    }

    public Map<String, Object> outline(AiRequest request) {
        String title = safe(request.getTitle(), "\u672a\u547d\u540d\u6587\u7ae0");
        List<String> outline = Arrays.asList(
                "1. \u5f15\u8a00\uff1a\u8bf4\u660e\u300a" + title + "\u300b\u7684\u5199\u4f5c\u80cc\u666f",
                "2. \u6838\u5fc3\u89c2\u70b9\uff1a\u62c6\u89e3\u4e3b\u9898\u4e2d\u7684\u5173\u952e\u95ee\u9898",
                "3. \u5b9e\u8df5\u8fc7\u7a0b\uff1a\u7ed3\u5408\u9879\u76ee\u6216\u6848\u4f8b\u5c55\u5f00\u8bf4\u660e",
                "4. \u603b\u7ed3\u53cd\u601d\uff1a\u7ed9\u51fa\u7ecf\u9a8c\u3001\u95ee\u9898\u4e0e\u540e\u7eed\u8ba1\u5212"
        );
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("title", title);
        map.put("outline", outline);
        map.put("mode", "local-rule-simulation");
        log("AI_OUTLINE", title, map.toString());
        return map;
    }

    public Map<String, Object> summary(AiRequest request) {
        String content = safe(request.getContent(), "");
        String summary = content.length() <= 80 ? content : content.substring(0, 80) + "...";
        if (summary.isEmpty()) summary = "\u8bf7\u8f93\u5165\u6587\u7ae0\u5185\u5bb9\u540e\u751f\u6210\u6458\u8981\u3002";
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("summary", summary);
        map.put("mode", "local-rule-simulation");
        log("AI_SUMMARY", content, map.toString());
        return map;
    }

    public Map<String, Object> recommendTags(AiRequest request) {
        String text = (safe(request.getTitle(), "") + " " + safe(request.getContent(), "")).toLowerCase();
        List<String> remoteTags = recommendTagsByModel(request, text);
        if (!remoteTags.isEmpty()) {
            Map<String, Object> remoteMap = new LinkedHashMap<String, Object>();
            remoteMap.put("tags", remoteTags);
            remoteMap.put("mode", "remote-openai-compatible");
            log("AI_TAGS", text, remoteMap.toString());
            return remoteMap;
        }
        List<String> tags = new ArrayList<String>();
        if (text.contains("spring") || text.contains("java")) tags.add("Spring Boot");
        if (text.contains("hexo") || text.contains("\u535a\u5ba2")) tags.add("Hexo");
        if (text.contains("ai") || text.contains("\u667a\u80fd") || text.contains("\u6458\u8981")) tags.add("AI");
        if (text.contains("\u524d\u7aef") || text.contains("javascript")) tags.add("JavaScript");
        if (tags.isEmpty()) tags.add("\u968f\u7b14");
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("tags", tags);
        map.put("mode", "local-rule-simulation");
        log("AI_TAGS", text, map.toString());
        return map;
    }

    public String reviewComment(String content) {
        String text = safe(content, "").toLowerCase();
        String result;
        if (text.contains("\u5e7f\u544a") || text.contains("\u52a0\u5fae\u4fe1") || text.contains("spam") || text.contains("http://") || text.contains("https://")) {
            result = "REJECT: \u7591\u4f3c\u5e7f\u544a\u6216\u5783\u573e\u4fe1\u606f";
        } else if (text.contains("\u9a82") || text.contains("\u5783\u573e") || text.contains("hate")) {
            result = "REJECT: \u7591\u4f3c\u4e0d\u6587\u660e\u5185\u5bb9";
        } else {
            result = "PASS: \u666e\u901a\u8bc4\u8bba";
        }
        log("AI_COMMENT_REVIEW", content, result);
        return result;
    }

    public Map<String, Object> answer(AiRequest request) {
        String question = safe(request.getQuestion(), "");
        StringBuilder evidence = new StringBuilder();
        for (Article article : repository.listArticles()) {
            if (evidence.length() > 0) evidence.append("; ");
            evidence.append(article.getTitle());
            if (evidence.length() > 160) break;
        }
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("question", question);
        map.put("answer", "\u6839\u636e\u5f53\u524d\u535a\u5ba2\u5185\u5bb9\uff0c\u53ef\u4ee5\u4ece\u8fd9\u4e9b\u6587\u7ae0\u5165\u624b\uff1a" + evidence.toString() + "\u3002\u5f53\u524d\u4e3a\u672c\u5730\u89c4\u5219\u6a21\u62df\uff0c\u540e\u7eed\u53ef\u66ff\u6362\u4e3a\u77e5\u8bc6\u5e93\u95ee\u7b54\u6216\u5927\u6a21\u578b API\u3002");
        map.put("mode", "local-rule-simulation");
        log("AI_QA", question, map.toString());
        return map;
    }

    private List<String> recommendTagsByModel(AiRequest request, String text) {
        String apiKey = env("AI_CHAT_API_KEY", env("DEEPSEEK_API_KEY", ""));
        if (apiKey.trim().isEmpty()) return Collections.emptyList();
        String endpoint = env("AI_CHAT_ENDPOINT", "").trim();
        if (endpoint.isEmpty()) {
            String baseUrl = trimSlash(env("AI_CHAT_BASE_URL", env("DEEPSEEK_BASE_URL", "https://api.deepseek.com")));
            endpoint = baseUrl + "/chat/completions";
        }
        String model = env("AI_CHAT_MODEL", env("DEEPSEEK_MODEL", "deepseek-chat"));
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> system = new LinkedHashMap<String, Object>();
            system.put("role", "system");
            system.put("content", "你是博客系统的标签推荐助手。只输出 3 到 6 个中文或技术英文标签，用中文逗号分隔，不要解释。");
            Map<String, Object> user = new LinkedHashMap<String, Object>();
            user.put("role", "user");
            user.put("content", "标题：" + safe(request.getTitle(), "") + "\n正文：" + safe(request.getContent(), ""));

            Map<String, Object> body = new LinkedHashMap<String, Object>();
            body.put("model", model);
            body.put("messages", Arrays.asList(system, user));
            body.put("temperature", 0.2);
            body.put("max_tokens", 120);

            ResponseEntity<Map> response = restTemplate.postForEntity(endpoint, new HttpEntity<Map<String, Object>>(body, headers), Map.class);
            if (response.getBody() == null) return Collections.emptyList();
            Object choicesObj = response.getBody().get("choices");
            if (!(choicesObj instanceof List) || ((List) choicesObj).isEmpty()) return Collections.emptyList();
            Object firstObj = ((List) choicesObj).get(0);
            if (!(firstObj instanceof Map)) return Collections.emptyList();
            Object messageObj = ((Map) firstObj).get("message");
            String content = "";
            if (messageObj instanceof Map && ((Map) messageObj).get("content") != null) {
                content = String.valueOf(((Map) messageObj).get("content"));
            } else if (((Map) firstObj).get("text") != null) {
                content = String.valueOf(((Map) firstObj).get("text"));
            }
            return parseTags(content);
        } catch (Exception ex) {
            log("AI_TAGS_REMOTE_ERROR", text, ex.getMessage());
            return Collections.emptyList();
        }
    }

    private List<String> parseTags(String content) {
        List<String> result = new ArrayList<String>();
        String clean = safe(content, "").replace("[", "").replace("]", "").replace("#", "");
        String[] parts = clean.split("[，,、;；\n\r\t ]+");
        for (String part : parts) {
            String tag = part.replace("\"", "").replace("'", "").trim();
            if (tag.isEmpty() || result.contains(tag)) continue;
            result.add(tag.length() > 30 ? tag.substring(0, 30) : tag);
            if (result.size() >= 6) break;
        }
        return result;
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

    private String safe(String value, String fallback) {
        return value == null || value.trim().isEmpty() ? fallback : value.trim();
    }

    private void log(String feature, String prompt, String result) {
        String shortPrompt = prompt == null ? "" : prompt;
        String shortResult = result == null ? "" : result;
        if (shortPrompt.length() > 500) shortPrompt = shortPrompt.substring(0, 500);
        if (shortResult.length() > 500) shortResult = shortResult.substring(0, 500);
        repository.saveAiUsageLog(new AiUsageLog(null, null, feature, shortPrompt, shortResult, LocalDateTime.now()));
    }
}