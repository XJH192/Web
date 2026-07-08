package com.tangyuxian.blog.dto;

import com.tangyuxian.blog.model.ArticleAttachment;

import java.util.ArrayList;
import java.util.List;

public class ArticleRequest {
    private String title;
    private String summary;
    private String content;
    private Long categoryId;
    private String categoryName;
    private List<Long> tagIds = new ArrayList<Long>();
    private List<String> tagNames = new ArrayList<String>();
    private List<ArticleAttachment> attachments = new ArrayList<ArticleAttachment>();
    private String status;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public List<Long> getTagIds() { return tagIds; }
    public void setTagIds(List<Long> tagIds) { this.tagIds = tagIds; }
    public List<String> getTagNames() { return tagNames; }
    public void setTagNames(List<String> tagNames) { this.tagNames = tagNames; }
    public List<ArticleAttachment> getAttachments() { return attachments; }
    public void setAttachments(List<ArticleAttachment> attachments) { this.attachments = attachments; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
