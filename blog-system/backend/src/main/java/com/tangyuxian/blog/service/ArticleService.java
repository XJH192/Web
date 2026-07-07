package com.tangyuxian.blog.service;

import com.tangyuxian.blog.common.BusinessException;
import com.tangyuxian.blog.dto.ArticleRequest;
import com.tangyuxian.blog.model.Article;
import com.tangyuxian.blog.model.ArticleAttachment;
import com.tangyuxian.blog.model.ArticleStatus;
import com.tangyuxian.blog.model.Notification;
import com.tangyuxian.blog.model.Role;
import com.tangyuxian.blog.model.Tag;
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

    public Article detail(Long id, boolean increaseViews, Long viewerId) {
        Article article = repository.findArticleById(id);
        if (article == null) throw new BusinessException("文章不存在");
        if (increaseViews) {
            article.setViewCount(article.getViewCount() + 1);
            article = repository.saveArticle(article);
        }
        return decorate(article, viewerId);
    }

    public Article create(User user, ArticleRequest request) {
        Article article = new Article();
        article.setAuthorId(user.getId());
        apply(article, request, user);
        Article saved = repository.saveArticle(article);
        if (saved.getStatus() == ArticleStatus.PENDING) {
            notifyAdmins("ARTICLE_PENDING", "有新博客等待审核", user.getNickname() + " 提交了《" + saved.getTitle() + "》", "/admin.html#articles");
        }
        return decorate(saved, user.getId());
    }

    public Article update(User user, Long id, ArticleRequest request) {
        Article article = repository.findArticleById(id);
        if (article == null) throw new BusinessException("文章不存在");
        if (user.getRole() != Role.ADMIN && !user.getId().equals(article.getAuthorId())) {
            throw new BusinessException("只能编辑自己的文章");
        }
        apply(article, request, user);
        Article saved = repository.saveArticle(article);
        if (saved.getStatus() == ArticleStatus.PENDING) {
            notifyAdmins("ARTICLE_PENDING", "有博客更新等待审核", user.getNickname() + " 更新了《" + saved.getTitle() + "》", "/admin.html#articles");
        }
        return decorate(saved, user.getId());
    }

    public Article updateStatus(Long id, String status) {
        Article article = repository.findArticleById(id);
        if (article == null) throw new BusinessException("文章不存在");
        article.setStatus(parseStatus(status, ArticleStatus.PENDING));
        Article saved = repository.saveArticle(article);
        if (saved.getAuthorId() != null) {
            if (saved.getStatus() == ArticleStatus.PUBLISHED) {
                notifyUser(saved.getAuthorId(), "ARTICLE_PUBLISHED", "博客已通过审核", "《" + saved.getTitle() + "》已上架到首页", "/article.html?id=" + saved.getId());
            } else if (saved.getStatus() == ArticleStatus.REJECTED) {
                notifyUser(saved.getAuthorId(), "ARTICLE_REJECTED", "博客未通过审核", "《" + saved.getTitle() + "》已被管理员驳回", "/blog.html#editor");
            }
        }
        return decorate(saved, null);
    }

    public void delete(User user, Long id) {
        Article article = repository.findArticleById(id);
        if (article == null) throw new BusinessException("文章不存在");
        if (user.getRole() != Role.ADMIN && !user.getId().equals(article.getAuthorId())) {
            throw new BusinessException("只能删除自己的文章");
        }
        repository.deleteArticle(id);
    }

    public Article like(Long userId, Long articleId) {
        Article article = repository.findArticleById(articleId);
        if (article == null) throw new BusinessException("文章不存在");
        if (article.getStatus() != ArticleStatus.PUBLISHED) throw new BusinessException("文章未上架，不能点赞");
        int changed = repository.likeArticle(userId, articleId);
        Article liked = repository.findArticleById(articleId);
        if (changed > 0 && liked.getAuthorId() != null && !liked.getAuthorId().equals(userId)) {
            User liker = repository.findUserById(userId);
            String nickname = liker == null ? "有用户" : liker.getNickname();
            notifyUser(liked.getAuthorId(), "ARTICLE_LIKED", "你的博客收到点赞", nickname + " 点赞了《" + liked.getTitle() + "》", "/article.html?id=" + liked.getId());
        }
        return decorate(liked, userId);
    }

    public Article unlike(Long userId, Long articleId) {
        Article article = repository.findArticleById(articleId);
        if (article == null) throw new BusinessException("文章不存在");
        repository.unlikeArticle(userId, articleId);
        return decorate(repository.findArticleById(articleId), userId);
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
            result.add(decorate(article, userId));
        }
        return result;
    }

    private Article decorate(Article article, Long viewerId) {
        if (article == null) return null;
        article.setCommentCount(repository.countApprovedComments(article.getId()));
        article.setLikedByCurrentUser(repository.hasArticleLike(viewerId, article.getId()));
        return article;
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
        if (request.getTagNames() != null) {
            for (String rawName : request.getTagNames()) {
                String name = rawName == null ? "" : rawName.trim();
                if (name.isEmpty()) continue;
                if (name.length() > 30) throw new BusinessException("标签过长：" + name);
                Tag tag = repository.findTagByName(name);
                if (tag == null) tag = repository.saveTag(new Tag(null, name, null));
                if (!tagIds.contains(tag.getId())) tagIds.add(tag.getId());
            }
        }
        article.setTitle(request.getTitle().trim());
        article.setSummary(request.getSummary() == null ? "" : request.getSummary().trim());
        article.setContent(request.getContent().trim());
        article.setAttachments(sanitizeAttachments(request.getAttachments()));
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

    private List<ArticleAttachment> sanitizeAttachments(List<ArticleAttachment> attachments) {
        List<ArticleAttachment> result = new ArrayList<ArticleAttachment>();
        if (attachments == null) return result;
        int count = 0;
        for (ArticleAttachment item : attachments) {
            if (item == null || item.getName() == null || item.getDataUrl() == null) continue;
            String name = item.getName().trim();
            String dataUrl = item.getDataUrl().trim();
            if (name.isEmpty() || !dataUrl.startsWith("data:")) continue;
            if (item.getSize() > 12L * 1024L * 1024L) throw new BusinessException(name + " 超过 12MB，请压缩后再上传");
            result.add(new ArticleAttachment(name, item.getType() == null ? "application/octet-stream" : item.getType(), item.getSize(), dataUrl));
            count++;
            if (count >= 8) break;
        }
        return result;
    }

    private void notifyAdmins(String type, String title, String content, String link) {
        for (User admin : repository.listUsers()) {
            if (admin.getRole() == Role.ADMIN && !admin.isBanned()) {
                notifyUser(admin.getId(), type, title, content, link);
            }
        }
    }

    private void notifyUser(Long userId, String type, String title, String content, String link) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setLink(link);
        notification.setReadFlag(false);
        repository.saveNotification(notification);
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