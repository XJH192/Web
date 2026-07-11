package com.tangyuxian.blog.service;

import com.tangyuxian.blog.common.BusinessException;
import com.tangyuxian.blog.dto.ArticleRequest;
import com.tangyuxian.blog.model.Article;
import com.tangyuxian.blog.model.ArticleAttachment;
import com.tangyuxian.blog.model.ArticleStatus;
import com.tangyuxian.blog.model.Category;
import com.tangyuxian.blog.model.Notification;
import com.tangyuxian.blog.model.Role;
import com.tangyuxian.blog.model.Tag;
import com.tangyuxian.blog.model.User;
import com.tangyuxian.blog.repository.InMemoryBlogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class ArticleService {
    private final InMemoryBlogRepository repository;
    private final AiService aiService;

    public ArticleService(InMemoryBlogRepository repository, AiService aiService) {
        this.repository = repository;
        this.aiService = aiService;
    }

    public List<Article> list(String keyword, Long categoryId, Long tagId, boolean includeDrafts) {
        return filter(repository.listArticles(), keyword, categoryId, tagId, includeDrafts, null, false);
    }

    public List<Article> feed(User user, String keyword, Long categoryId, Long tagId) {
        return filter(repository.listArticles(), keyword, categoryId, tagId, false, user == null ? null : user.getId(), null);
    }

    public List<Article> listMine(User user, String keyword, Long categoryId, Long tagId) {
        return filter(repository.listArticles(), keyword, categoryId, tagId, true, user.getId(), false);
    }

    public List<Article> listPublishedByAuthor(Long authorId, Long viewerId) {
        List<Article> result = new ArrayList<Article>();
        for (Article article : repository.listArticles()) {
            if (!authorId.equals(article.getAuthorId()) || article.getStatus() != ArticleStatus.PUBLISHED) continue;
            result.add(decorate(article, viewerId));
        }
        return result;
    }

    public List<Article> searchPublishedByTitle(String keyword, Long viewerId, int limit) {
        List<Article> result = new ArrayList<Article>();
        String query = keyword == null ? "" : keyword.trim().toLowerCase();
        if (query.isEmpty()) return result;
        for (Article article : repository.listArticles()) {
            if (article.getStatus() != ArticleStatus.PUBLISHED) continue;
            String title = article.getTitle() == null ? "" : article.getTitle().toLowerCase();
            if (!title.contains(query)) continue;
            result.add(decorate(article, viewerId));
            if (result.size() >= limit) break;
        }
        return result;
    }

    public Article detail(Long id, boolean increaseViews, Long viewerId) {
        Article article = repository.findArticleById(id);
        if (article == null) throw new BusinessException("文章不存在");
        if (viewerId == null && article.getStatus() != ArticleStatus.PUBLISHED) {
            throw new BusinessException("文章未公开，登录后才可查看");
        }
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
            notifyAdmins("ARTICLE_PENDING", "有新博客等待审核", user.getNickname() + " 提交了《" + saved.getTitle() + "》", "/admin.html#articles", saved.getId());
        } else if (saved.getStatus() == ArticleStatus.PUBLISHED) {
            notifyFollowersOfPublishedArticle(saved);
        }
        return decorate(saved, user.getId());
    }

    public Article update(User user, Long id, ArticleRequest request) {
        Article article = repository.findArticleById(id);
        if (article == null) throw new BusinessException("文章不存在");
        if (user.getRole() != Role.ADMIN && !user.getId().equals(article.getAuthorId())) {
            throw new BusinessException("只能编辑自己的文章");
        }
        ArticleStatus previousStatus = article.getStatus();
        apply(article, request, user);
        Article saved = repository.saveArticle(article);
        if (saved.getStatus() == ArticleStatus.PENDING) {
            notifyAdmins("ARTICLE_PENDING", "有博客更新等待审核", user.getNickname() + " 更新了《" + saved.getTitle() + "》", "/admin.html#articles", saved.getId());
        } else if (saved.getStatus() == ArticleStatus.PUBLISHED && previousStatus != ArticleStatus.PUBLISHED) {
            notifyFollowersOfPublishedArticle(saved);
        }
        return decorate(saved, user.getId());
    }

    public Article updateStatus(Long id, String status) {
        Article article = repository.findArticleById(id);
        if (article == null) throw new BusinessException("文章不存在");
        ArticleStatus previousStatus = article.getStatus();
        article.setStatus(parseStatus(status, ArticleStatus.PENDING));
        Article saved = repository.saveArticle(article);
        if (saved.getAuthorId() != null) {
            if (saved.getStatus() == ArticleStatus.PUBLISHED) {
                if (previousStatus != ArticleStatus.PUBLISHED) notifyFollowersOfPublishedArticle(saved);
            } else if (saved.getStatus() == ArticleStatus.REJECTED) {
                notifyUser(saved.getAuthorId(), "ARTICLE_REJECTED", "博客未通过审核", "《" + saved.getTitle() + "》已被管理员驳回", "/blog.html#editor", saved.getId());
            }
        }
        return decorate(saved, null);
    }

    @Transactional
    public void delete(User user, Long id) {
        Article article = repository.findArticleById(id);
        if (article == null) throw new BusinessException("文章不存在");
        if (user.getRole() != Role.ADMIN && !user.getId().equals(article.getAuthorId())) {
            throw new BusinessException("只能删除自己的文章");
        }
        Set<Long> recipients = new LinkedHashSet<Long>(repository.listArticleAffectedUserIds(id));
        recipients.add(article.getAuthorId());
        for (User admin : repository.listUsers()) {
            if (admin.getRole() == Role.ADMIN && !admin.isBanned()) recipients.add(admin.getId());
        }

        repository.deleteArticleWithRelations(id);
        for (Long recipientId : recipients) {
            if (recipientId == null || repository.findUserById(recipientId) == null) continue;
            notifyUser(
                    recipientId,
                    "ARTICLE_DELETED",
                    "文章已删除",
                    user.getUsername() + " 删除了《" + article.getTitle() + "》",
                    null,
                    user
            );
        }
    }

    public Article like(Long userId, Long articleId) {
        Article article = repository.findArticleById(articleId);
        if (article == null) throw new BusinessException("文章不存在");
        if (article.getStatus() != ArticleStatus.PUBLISHED) throw new BusinessException("文章未上架，不能点赞");
        int changed = repository.likeArticle(userId, articleId);
        Article liked = repository.findArticleById(articleId);
        if (changed > 0 && liked.getAuthorId() != null && !liked.getAuthorId().equals(userId)) {
            User liker = repository.findUserById(userId);
            String username = notificationUsername(liker);
            notifyUser(liked.getAuthorId(), "ARTICLE_LIKED", "你的博客收到点赞", username + " 点赞了《" + liked.getTitle() + "》", "/article.html?id=" + liked.getId(), liker);
        }
        return decorate(liked, userId);
    }

    public Article unlike(Long userId, Long articleId) {
        Article article = repository.findArticleById(articleId);
        if (article == null) throw new BusinessException("文章不存在");
        repository.unlikeArticle(userId, articleId);
        return decorate(repository.findArticleById(articleId), userId);
    }

    private List<Article> filter(List<Article> source, String keyword, Long categoryId, Long tagId, boolean includeDrafts, Long userId, Boolean excludeUser) {
        List<Article> result = new ArrayList<Article>();
        for (Article article : source) {
            if (userId != null && excludeUser != null && excludeUser && userId.equals(article.getAuthorId())) continue;
            if (userId != null && excludeUser != null && !excludeUser && !userId.equals(article.getAuthorId())) continue;
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
        Long authorId = article.getAuthorId();
        article.setAuthorFollowerCount(repository.countUserFollowers(authorId));
        boolean followingAuthor = viewerId != null && !viewerId.equals(authorId) && repository.hasUserFollow(viewerId, authorId);
        article.setAuthorFollowedByCurrentUser(followingAuthor);
        article.setMutualFollowWithAuthor(followingAuthor && repository.hasUserFollow(authorId, viewerId));
        return article;
    }

    private void apply(Article article, ArticleRequest request, User user) {
        requireText(request.getTitle(), "请输入文章标题");
        requireText(request.getContent(), "请输入文章正文");
        Long categoryId = resolveCategoryId(request);
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
        article.setCategoryId(categoryId);
        article.setTagIds(tagIds);
        if (user.getRole() == Role.ADMIN) {
            article.setStatus(parseStatus(request.getStatus(), ArticleStatus.PUBLISHED));
            article.setAiReviewResult(article.getStatus() == ArticleStatus.DRAFT ? "SKIP: 草稿未审核" : "MANUAL: 管理员直接处理");
        } else if ("DRAFT".equalsIgnoreCase(request.getStatus())) {
            article.setStatus(ArticleStatus.DRAFT);
            article.setAiReviewResult("SKIP: 草稿未审核");
        } else {
            String review = aiService.reviewArticle(article.getTitle(), article.getSummary(), article.getContent());
            article.setAiReviewResult(review);
            article.setStatus(review.startsWith("PASS") ? ArticleStatus.PUBLISHED : ArticleStatus.PENDING);
        }
    }

    private Long resolveCategoryId(ArticleRequest request) {
        String customName = request.getCategoryName() == null ? "" : request.getCategoryName().trim();
        if (!customName.isEmpty()) {
            if (customName.length() > 30) throw new BusinessException("分类名称过长：" + customName);
            Category category = repository.findCategoryByName(customName);
            if (category == null) category = repository.saveCategory(new Category(null, customName, "用户自定义分类", null));
            return category.getId();
        }
        if (request.getCategoryId() == null || repository.findCategoryById(request.getCategoryId()) == null) {
            throw new BusinessException("请选择有效分类，或填写自定义分类");
        }
        return request.getCategoryId();
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

    private void notifyAdmins(String type, String title, String content, String link, Long articleId) {
        for (User admin : repository.listUsers()) {
            if (admin.getRole() == Role.ADMIN && !admin.isBanned()) {
                notifyUser(admin.getId(), type, title, content, link, articleId);
            }
        }
    }

    private void notifyUser(Long userId, String type, String title, String content, String link) {
        notifyUser(userId, type, title, content, link, (User) null);
    }

    private void notifyUser(Long userId, String type, String title, String content, String link, Long articleId) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setArticleId(articleId);
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setLink(link);
        notification.setReadFlag(false);
        repository.saveNotification(notification);
    }

    private void notifyFollowersOfPublishedArticle(Article article) {
        User author = repository.findUserById(article.getAuthorId());
        if (author == null) return;
        for (User follower : repository.listUserFollowers(author.getId())) {
            notifyUser(
                    follower.getId(),
                    "USER_ARTICLE_PUBLISHED",
                    "你关注的用户发布了新文章",
                    author.getUsername() + " 发布了《" + article.getTitle() + "》",
                    "/article.html?id=" + article.getId(),
                    author
            );
        }
    }

    private void notifyUser(Long userId, String type, String title, String content, String link, User actor) {
        Notification notification = new Notification();
        notification.setUserId(userId);
        if (actor != null) {
            notification.setActorUserId(actor.getId());
            notification.setActorUsername(actor.getUsername());
        }
        notification.setType(type);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setLink(link);
        notification.setReadFlag(false);
        repository.saveNotification(notification);
    }

    private String notificationUsername(User user) {
        return user == null || user.getUsername() == null || user.getUsername().trim().isEmpty()
                ? "有用户"
                : user.getUsername();
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
