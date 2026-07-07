package com.tangyuxian.blog.service;

import com.tangyuxian.blog.common.BusinessException;
import com.tangyuxian.blog.dto.ArticleRequest;
import com.tangyuxian.blog.model.Article;
import com.tangyuxian.blog.model.ArticleStatus;
import com.tangyuxian.blog.model.Role;
import com.tangyuxian.blog.model.User;
import com.tangyuxian.blog.repository.InMemoryBlogRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ArticleService {
    private final InMemoryBlogRepository repository;

    public ArticleService(InMemoryBlogRepository repository) {
        this.repository = repository;
    }

    public List<Article> list(String keyword, Long categoryId, Long tagId, boolean includeDrafts) {
        return filter(repository.listArticles(), keyword, categoryId, tagId, includeDrafts, null, false);
    }

    public List<Article> feed(User user, String keyword, Long categoryId, Long tagId) {
        return filter(repository.listArticles(), keyword, categoryId, tagId, false, user == null ? null : user.getId(), true);
    }

    public List<Article> listMine(User user, String keyword, Long categoryId, Long tagId) {
        return filter(repository.listArticles(), keyword, categoryId, tagId, true, user.getId(), false);
    }

    public Article detail(Long id, boolean increaseViews) {
        Article article = repository.findArticleById(id);
        if (article == null) throw new BusinessException("文章不存在");
        if (increaseViews) {
            article.setViewCount(article.getViewCount() + 1);
            repository.saveArticle(article);
        }
        return article;
    }

    public Article create(User user, ArticleRequest request) {
        Article article = new Article();
        article.setAuthorId(user.getId());
        apply(article, request, user);
        return repository.saveArticle(article);
    }

    public Article update(User user, Long id, ArticleRequest request) {
        Article article = repository.findArticleById(id);
        if (article == null) throw new BusinessException("文章不存在");
        if (user.getRole() != Role.ADMIN && !user.getId().equals(article.getAuthorId())) {
            throw new BusinessException("只能编辑自己的文章");
        }
        apply(article, request, user);
        return repository.saveArticle(article);
    }

    public Article updateStatus(Long id, String status) {
        Article article = repository.findArticleById(id);
        if (article == null) throw new BusinessException("文章不存在");
        article.setStatus(parseStatus(status, ArticleStatus.PENDING));
        return repository.saveArticle(article);
    }

    public void delete(User user, Long id) {
        Article article = repository.findArticleById(id);
        if (article == null) throw new BusinessException("文章不存在");
        if (user.getRole() != Role.ADMIN && !user.getId().equals(article.getAuthorId())) {
            throw new BusinessException("只能删除自己的文章");
        }
        repository.deleteArticle(id);
    }

    public void like(Long userId, Long articleId) {
        Article article = repository.findArticleById(articleId);
        if (article == null) throw new BusinessException("文章不存在");
        if (article.getStatus() != ArticleStatus.PUBLISHED) throw new BusinessException("文章未上架，不能点赞");
        repository.likeArticle(userId, articleId);
    }

    public void unlike(Long userId, Long articleId) {
        Article article = repository.findArticleById(articleId);
        if (article == null) throw new BusinessException("文章不存在");
        repository.unlikeArticle(userId, articleId);
    }

    private List<Article> filter(List<Article> source, String keyword, Long categoryId, Long tagId, boolean includeDrafts, Long userId, boolean excludeUser) {
        List<Article> result = new ArrayList<Article>();
        for (Article article : source) {
            if (userId != null && excludeUser && userId.equals(article.getAuthorId())) continue;
            if (userId != null && !excludeUser && !userId.equals(article.getAuthorId())) continue;
            if (!includeDrafts && article.getStatus() != ArticleStatus.PUBLISHED) continue;
            if (categoryId != null && !categoryId.equals(article.getCategoryId())) continue;
            if (tagId != null && (article.getTagIds() == null || !article.getTagIds().contains(tagId))) continue;
            if (keyword != null && !keyword.trim().isEmpty()) {
                String text = (article.getTitle() + " " + article.getSummary() + " " + article.getContent()).toLowerCase();
                if (!text.contains(keyword.trim().toLowerCase())) continue;
            }
            result.add(article);
        }
        return result;
    }

    private void apply(Article article, ArticleRequest request, User user) {
        requireText(request.getTitle(), "请输入文章标题");
        requireText(request.getContent(), "请输入文章正文");
        if (request.getCategoryId() == null || repository.findCategoryById(request.getCategoryId()) == null) {
            throw new BusinessException("请选择有效分类");
        }
        List<Long> tagIds = request.getTagIds() == null ? new ArrayList<Long>() : new ArrayList<Long>(request.getTagIds());
        for (Long tagId : tagIds) {
            if (tagId == null || repository.findTagById(tagId) == null) {
                throw new BusinessException("标签不存在：" + tagId);
            }
        }
        article.setTitle(request.getTitle().trim());
        article.setSummary(request.getSummary() == null ? "" : request.getSummary().trim());
        article.setContent(request.getContent().trim());
        article.setCategoryId(request.getCategoryId());
        article.setTagIds(tagIds);
        if (user.getRole() == Role.ADMIN) {
            article.setStatus(parseStatus(request.getStatus(), ArticleStatus.PUBLISHED));
        } else if ("DRAFT".equalsIgnoreCase(request.getStatus())) {
            article.setStatus(ArticleStatus.DRAFT);
        } else {
            article.setStatus(ArticleStatus.PENDING);
        }
    }

    private ArticleStatus parseStatus(String status, ArticleStatus fallback) {
        if (status == null || status.trim().isEmpty()) return fallback;
        try {
            return ArticleStatus.valueOf(status.trim().toUpperCase());
        } catch (Exception ex) {
            throw new BusinessException("文章状态无效");
        }
    }

    private void requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) throw new BusinessException(message);
    }
}