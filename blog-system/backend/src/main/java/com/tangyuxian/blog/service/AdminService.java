package com.tangyuxian.blog.service;

import com.tangyuxian.blog.dto.DashboardStats;
import com.tangyuxian.blog.model.Article;
import com.tangyuxian.blog.model.ArticleStatus;
import com.tangyuxian.blog.repository.InMemoryBlogRepository;
import org.springframework.stereotype.Service;

@Service
public class AdminService {
    private final InMemoryBlogRepository repository;

    public AdminService(InMemoryBlogRepository repository) {
        this.repository = repository;
    }

    public DashboardStats stats() {
        DashboardStats stats = new DashboardStats();
        stats.setUserCount(repository.listUsers().size());
        int visibleArticleCount = 0;
        for (Article article : repository.listArticles()) {
            if (article.getStatus() != ArticleStatus.DRAFT) visibleArticleCount++;
        }
        stats.setArticleCount(visibleArticleCount);
        stats.setCommentCount(repository.listComments().size());
        stats.setCategoryCount(repository.listCategories().size());
        stats.setTagCount(repository.listTags().size());
        stats.setTotalViews(repository.totalViews());
        stats.setAiUsageCount(repository.listAiUsageLogs().size());
        return stats;
    }
}
