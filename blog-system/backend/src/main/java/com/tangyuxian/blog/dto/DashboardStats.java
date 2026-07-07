package com.tangyuxian.blog.dto;

public class DashboardStats {
    private int userCount;
    private int articleCount;
    private int commentCount;
    private int categoryCount;
    private int tagCount;
    private int totalViews;
    private int aiUsageCount;

    public int getUserCount() { return userCount; }
    public void setUserCount(int userCount) { this.userCount = userCount; }
    public int getArticleCount() { return articleCount; }
    public void setArticleCount(int articleCount) { this.articleCount = articleCount; }
    public int getCommentCount() { return commentCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }
    public int getCategoryCount() { return categoryCount; }
    public void setCategoryCount(int categoryCount) { this.categoryCount = categoryCount; }
    public int getTagCount() { return tagCount; }
    public void setTagCount(int tagCount) { this.tagCount = tagCount; }
    public int getTotalViews() { return totalViews; }
    public void setTotalViews(int totalViews) { this.totalViews = totalViews; }
    public int getAiUsageCount() { return aiUsageCount; }
    public void setAiUsageCount(int aiUsageCount) { this.aiUsageCount = aiUsageCount; }
}
