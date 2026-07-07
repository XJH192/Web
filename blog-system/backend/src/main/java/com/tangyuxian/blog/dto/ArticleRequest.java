package com.tangyuxian.blog.dto;

import java.util.ArrayList;
import java.util.List;

public class ArticleRequest {
    private String title;
    private String summary;
    private String content;
    private Long categoryId;
    private List<Long> tagIds = new ArrayList<Long>();
    private String status;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public List<Long> getTagIds() { return tagIds; }
    public void setTagIds(List<Long> tagIds) { this.tagIds = tagIds; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
