package com.tangyuxian.blog.service;

import com.tangyuxian.blog.dto.AiRequest;
import com.tangyuxian.blog.model.AiUsageLog;
import com.tangyuxian.blog.model.Article;
import com.tangyuxian.blog.model.ArticleStatus;
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
    private static final String[] AD_KEYWORDS = {
            "广告", "加微信", "加我微信", "微信联系", "vx", "v信", "薇信", "微商", "私聊", "私信",
            "联系方式", "二维码", "扫码", "推广", "引流", "互粉", "涨粉", "刷粉", "刷赞", "刷单",
            "兼职", "日结", "返利", "优惠券", "代理", "招商", "加盟", "代写", "论文代写", "代发",
            "qq群", "q群", "扣群", "telegram", "whatsapp", "t.me", "http://", "https://", "www."
    };
    private static final String[] ABUSE_KEYWORDS = {
            "傻逼", "傻屄", "煞笔", "傻比", "沙币", "sb", "nmsl", "cnm", "cnmd",
            "你妈", "你妹", "滚", "滚蛋", "爬", "去死", "死全家", "废物", "真垃圾", "太垃圾",
            "垃圾东西", "垃圾人", "垃圾博主", "蠢货",
            "脑残", "脑瘫", "弱智", "智障", "低能", "有病", "神经病", "畜生", "司马",
            "小丑", "键盘侠", "杠精", "喷子", "破防了", "急了", "骂"
    };
    private static final String[] VULGAR_KEYWORDS = {
            "色情", "黄色", "低俗", "约炮", "裸聊", "成人视频", "福利视频", "你懂的", "看片",
            "开车", "ghs", "卖片", "资源群", "同城约", "上门服务"
    };
    private static final String[] FRAUD_KEYWORDS = {
            "博彩", "赌博", "网赌", "娱乐城", "时时彩", "六合彩", "赌球", "贷款", "套现",
            "代办证", "办证", "假证", "发票", "洗钱", "灰产", "黑产", "盗号", "外挂",
            "破解", "免杀", "接码", "卖号", "代充", "资金盘", "杀猪盘", "虚拟币稳赚"
    };

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
        String raw = safe(content, "");
        String text = normalizeForReview(raw);
        String result;
        String thinking;
        if (containsAny(text, AD_KEYWORDS) || text.contains("spam")) {
            result = "REVIEW: 疑似广告或垃圾信息";
            thinking = "本地审核规则命中广告引流、外链、社群导流或垃圾信息关键词。";
        } else if (containsAny(text, ABUSE_KEYWORDS) || text.contains("hate")) {
            result = "REVIEW: 疑似不文明内容";
            thinking = "本地审核规则命中辱骂、攻击性表达或当代网络攻击用语。";
        } else if (containsAny(text, VULGAR_KEYWORDS)) {
            result = "REVIEW: 疑似低俗色情内容";
            thinking = "本地审核规则命中低俗、色情或擦边引流关键词。";
        } else if (containsAny(text, FRAUD_KEYWORDS)) {
            result = "REVIEW: 疑似诈骗或违规推广";
            thinking = "本地审核规则命中博彩、诈骗、黑产或违规交易关键词。";
        } else {
            result = "PASS: 普通评论";
            thinking = "本地审核规则未发现明显广告、外链、不文明、低俗或违规推广关键词。";
        }
        log("AI_COMMENT_REVIEW", content, result, thinking);
        return result;
    }

    public String reviewArticle(String title, String summary, String content) {
        String raw = safe(title, "") + "\n" + safe(summary, "") + "\n" + safe(content, "");
        String text = normalizeForReview(raw);
        String result;
        String thinking;
        if (containsAny(text, AD_KEYWORDS) || text.contains("spam")) {
            result = "REVIEW: 疑似广告、外链或引流内容";
            thinking = "文章初审命中广告、外链、社群导流或垃圾信息关键词，转交管理员复核。";
        } else if (containsAny(text, ABUSE_KEYWORDS) || text.contains("hate")) {
            result = "REVIEW: 疑似攻击或不文明内容";
            thinking = "文章初审命中辱骂、攻击性表达或不文明关键词，转交管理员复核。";
        } else if (containsAny(text, VULGAR_KEYWORDS)) {
            result = "REVIEW: 疑似低俗色情内容";
            thinking = "文章初审命中低俗、色情或擦边内容关键词，转交管理员复核。";
        } else if (containsAny(text, FRAUD_KEYWORDS)) {
            result = "REVIEW: 疑似诈骗或违规内容";
            thinking = "文章初审命中博彩、诈骗、黑产或违规交易关键词，转交管理员复核。";
        } else {
            result = "PASS: 文章内容未发现明显风险";
            thinking = "文章初审未发现明显广告、攻击、低俗、诈骗或违规推广关键词，可直接公开。";
        }
        log("AI_ARTICLE_REVIEW", raw, result, thinking);
        return result;
    }

    public Map<String, Object> answer(AiRequest request) {
        String question = safe(request.getQuestion(), "");
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        if (question.isEmpty()) {
            map.put("question", question);
            map.put("answer", "请先输入你想了解的问题。");
            map.put("mode", "local-validation");
            log("AI_QA", question, map.toString(), "用户未输入问题，未发起远程调用。");
            return map;
        }
        List<Article> publicArticles = publishedArticles();
        if (publicArticles.isEmpty()) {
            map.put("question", question);
            map.put("answer", "当前还没有已发布文章，暂时无法基于博客内容回答。");
            map.put("mode", "local-no-content");
            log("AI_QA", question, map.toString(), "当前没有已发布文章，未发起远程调用。");
            return map;
        }

        List<Article> evidenceArticles = relevantArticles(publicArticles, question, 5);
        if (evidenceArticles.isEmpty()) {
            map.put("question", question);
            map.put("answer", "没有在已发布文章中找到与“" + question + "”明确相关的内容，因此不生成无依据回答。你可以换一个更具体的关键词再问。");
            map.put("mode", "local-no-match");
            map.put("sources", new ArrayList<String>());
            log("AI_QA", question, map.toString(), "本地检索未命中明确相关文章，未发起远程调用，避免引用无关文章。");
            return map;
        }
        String context = blogKnowledgeContext(evidenceArticles);
        String prompt = "用户问题：" + question + "\n\n当前博客已发布文章内容：\n" + context +
                "\n\n请基于上面的博客内容回答用户问题。要求：" +
                "\n1. 只能依据给出的博客内容回答，不要编造博客中没有的信息。" +
                "\n2. 如果博客内容不足以回答，请明确说明没有找到相关内容，并给出最接近的文章线索。" +
                "\n3. 回答使用中文，只输出回答正文；不要列出文章标题，不要出现“依据文章”“参考文章”或“来源”等段落，系统会单独展示来源。" +
                "\n4. 不要输出 JSON、Markdown 表格、项目符号或额外解释。";
        ChatResult remote = chat("你是博客内容问答助手。你会阅读当前博客已发布文章，并基于证据回答用户问题。", prompt, 900);
        String answer = cleanQaAnswer(remote.getContent());
        if (!answer.isEmpty()) {
            map.put("question", question);
            map.put("answer", answer);
            map.put("mode", remote.getMode());
            map.put("thinking", thinkingSummary(remote, "检索已发布文章标题、摘要、分类、标签和正文片段，选择相关证据后回答用户问题。"));
            map.put("sources", sourceTitles(evidenceArticles, 8));
            log("AI_QA", prompt, map.toString(), thinkingSummary(remote, "检索已发布文章标题、摘要、分类、标签和正文片段，选择相关证据后回答用户问题。"));
            return map;
        }
        if (!allowLocalFallback()) {
            Map<String, Object> error = aiError("AI_QA", prompt, remote, "DeepSeek 博客问答失败");
            error.put("question", question);
            error.put("answer", String.valueOf(error.get("error")));
            error.put("sources", sourceTitles(evidenceArticles, 8));
            return error;
        }

        List<String> matchedTitles = sourceTitles(evidenceArticles, 5);
        map.put("question", question);
        map.put("answer", fallbackBlogAnswer(question, matchedTitles));
        map.put("mode", "local-rule-fallback");
        map.put("sources", matchedTitles);
        log("AI_QA", prompt, map.toString(), "DeepSeek 暂不可用，按配置启用本地文章标题匹配兜底。");
        return map;
    }

    private List<Article> publishedArticles() {
        List<Article> result = new ArrayList<Article>();
        for (Article article : repository.listArticles()) {
            if (article != null && article.getStatus() == ArticleStatus.PUBLISHED) result.add(article);
        }
        return result;
    }

    private String blogKnowledgeContext(List<Article> articles) {
        StringBuilder builder = new StringBuilder();
        int total = 0;
        int index = 1;
        for (Article article : articles) {
            if (index > 8 || total > 7000) break;
            String block = "文章 " + index + "\n" +
                    "标题：" + safe(article.getTitle(), "未命名文章") + "\n" +
                    "分类：" + safe(article.getCategoryName(), "未分类") + "\n" +
                    "标签：" + joinTags(article.getTagNames()) + "\n" +
                    "摘要：" + safe(article.getSummary(), "暂无摘要") + "\n" +
                    "正文片段：" + compactText(safe(article.getContent(), ""), 1000) + "\n";
            builder.append(block).append("\n");
            total += block.length();
            index++;
        }
        return builder.toString();
    }

    private List<String> sourceTitles(List<Article> articles, int max) {
        List<String> titles = new ArrayList<String>();
        if (articles == null) return titles;
        for (Article article : articles) {
            if (article == null || safe(article.getTitle(), "").isEmpty()) continue;
            titles.add(article.getTitle().trim());
            if (titles.size() >= max) break;
        }
        return titles;
    }

    private List<Article> relevantArticles(List<Article> articles, String question, int max) {
        List<Article> ranked = new ArrayList<Article>();
        List<Integer> scores = new ArrayList<Integer>();
        if (articles == null || articles.isEmpty()) return ranked;
        for (Article article : articles) {
            int score = articleRelevanceScore(article, question);
            if (score <= 0) continue;
            int index = 0;
            while (index < scores.size() && scores.get(index) >= score) index++;
            ranked.add(index, article);
            scores.add(index, score);
            if (ranked.size() > max) {
                ranked.remove(ranked.size() - 1);
                scores.remove(scores.size() - 1);
            }
        }
        if (!scores.isEmpty()) {
            int threshold = Math.max(45, (int) Math.ceil(scores.get(0) * 0.35d));
            while (!scores.isEmpty() && scores.get(scores.size() - 1) < threshold) {
                scores.remove(scores.size() - 1);
                ranked.remove(ranked.size() - 1);
            }
        }
        return ranked;
    }

    private int articleRelevanceScore(Article article, String question) {
        if (article == null) return 0;
        String normalizedQuestion = normalizedQuestionKeywords(question);
        if (normalizedQuestion.isEmpty()) return 0;
        String title = normalizeForReview(article.getTitle());
        String summary = normalizeForReview(article.getSummary());
        String tags = normalizeForReview(joinTags(article.getTagNames()));
        String category = normalizeForReview(article.getCategoryName());
        String content = normalizeForReview(article.getContent());
        String full = title + summary + tags + category + content;
        int score = 0;
        if (title.contains(normalizedQuestion)) score += 120;
        if (summary.contains(normalizedQuestion)) score += 80;
        if (content.contains(normalizedQuestion)) score += 60;
        if ((tags + category).contains(normalizedQuestion)) score += 40;
        List<String> terms = queryTerms(normalizedQuestion);
        for (String term : terms) {
            if (term.length() < 2) continue;
            if (title.contains(term)) score += term.length() >= 4 ? 45 : 28;
            if (summary.contains(term)) score += term.length() >= 4 ? 24 : 12;
            if (content.contains(term)) score += term.length() >= 4 ? 18 : 5;
            if ((tags + category).contains(term)) score += 18;
        }
        if (score < 30 && !full.contains(normalizedQuestion)) return 0;
        return score;
    }

    private String normalizedQuestionKeywords(String question) {
        String text = normalizeForReview(question);
        String[] stopWords = {"什么是", "什么叫", "请问", "介绍一下", "解释一下", "一下", "这个", "那个", "当前", "内容", "文章", "可以", "如何", "怎么", "哪些", "有什么", "是什么", "是啥", "系统", "实现", "功能", "了", "吗", "呢", "的"};
        for (String stopWord : stopWords) text = text.replace(normalizeForReview(stopWord), "");
        return text;
    }

    private List<String> queryTerms(String normalizedQuestion) {
        List<String> terms = new ArrayList<String>();
        addTerm(terms, normalizedQuestion);
        for (int length = Math.min(6, normalizedQuestion.length()); length >= 2; length--) {
            for (int i = 0; i + length <= normalizedQuestion.length(); i++) {
                addTerm(terms, normalizedQuestion.substring(i, i + length));
            }
        }
        return terms;
    }

    private void addTerm(List<String> terms, String term) {
        if (term == null || term.length() < 2 || terms.contains(term)) return;
        terms.add(term);
    }

    private List<String> matchedArticleTitles(List<Article> articles, String question) {
        List<String> titles = new ArrayList<String>();
        for (Article article : articles) {
            if (articleRelevanceScore(article, question) > 0) titles.add(article.getTitle());
            if (titles.size() >= 5) break;
        }
        return titles;
    }

    private String fallbackBlogAnswer(String question, List<String> matchedTitles) {
        if (matchedTitles == null || matchedTitles.isEmpty()) {
            return "DeepSeek 暂不可用，本地兜底没有在已发布文章中找到与“" + question + "”明显相关的内容。你可以换一个更具体的问题，或稍后重试大模型问答。";
        }
        return "DeepSeek 暂不可用。本地兜底已定位到与“" + question + "”相关的内容，请查看下方依据文章，或稍后重试大模型问答。";
    }

    private String cleanQaAnswer(String content) {
        String answer = compactText(content, 1200);
        answer = answer.replaceFirst("(?s)\\s*(?:依据文章|参考文章|来源文章|参考来源)\\s*[:：].*$", "").trim();
        return answer;
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

    private boolean containsAny(String text, String[] keywords) {
        if (text == null || text.isEmpty() || keywords == null) return false;
        for (String keyword : keywords) {
            if (keyword != null && !keyword.trim().isEmpty() && text.contains(normalizeForReview(keyword))) return true;
        }
        return false;
    }

    private String normalizeForReview(String value) {
        String text = safe(value, "").toLowerCase();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char ch = text.charAt(i);
            if (ch >= 'Ａ' && ch <= 'Ｚ') ch = (char) (ch - 'Ａ' + 'a');
            else if (ch >= 'ａ' && ch <= 'ｚ') ch = (char) (ch - 'ａ' + 'a');
            else if (ch >= '０' && ch <= '９') ch = (char) (ch - '０' + '0');
            if (Character.isLetterOrDigit(ch) || isChinese(ch)) builder.append(ch);
        }
        return builder.toString()
                .replace("vx号", "vx")
                .replace("v信", "vx")
                .replace("傻b", "傻逼")
                .replace("煞b", "傻逼")
                .replace("杀b", "傻逼")
                .replace("脑can", "脑残")
                .replace("司m", "司马");
    }

    private boolean isChinese(char ch) {
        return ch >= '\u4e00' && ch <= '\u9fff';
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
