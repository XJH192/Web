package com.tangyuxian.blog.model;

import java.time.LocalDateTime;

public class AiUsageLog {
    private Long id;
    private Long userId;
    private String feature;
    private String prompt;
    private String thinking;
    private String result;
    private LocalDateTime createdAt;

    public AiUsageLog() {}

    public AiUsageLog(Long id, Long userId, String feature, String prompt, String thinking, String result, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.feature = feature;
        this.prompt = prompt;
        this.thinking = thinking;
        this.result = result;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getFeature() { return feature; }
    public void setFeature(String feature) { this.feature = feature; }
    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    public String getThinking() { return thinking; }
    public void setThinking(String thinking) { this.thinking = thinking; }
    public String getResult() { return result; }
    public void setResult(String result) { this.result = result; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
