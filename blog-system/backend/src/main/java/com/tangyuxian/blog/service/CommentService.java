package com.tangyuxian.blog.service;

import com.tangyuxian.blog.common.BusinessException;
import com.tangyuxian.blog.dto.CommentRequest;
import com.tangyuxian.blog.model.Article;
import com.tangyuxian.blog.model.ArticleStatus;
import com.tangyuxian.blog.model.Comment;
import com.tangyuxian.blog.model.CommentStatus;
import com.tangyuxian.blog.model.Notification;
import com.tangyuxian.blog.model.Role;
import com.tangyuxian.blog.model.User;
import com.tangyuxian.blog.repository.InMemoryBlogRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class CommentService {
    private final InMemoryBlogRepository repository;
    private final AiService aiService;

    public CommentService(InMemoryBlogRepository repository, AiService aiService) {
        this.repository = repository;
        this.aiService = aiService;
    }

    public List<Comment> listByArticle(Long articleId, boolean includeAll) {
        return listByArticle(articleId, includeAll, null);
    }

    public List<Comment> listByArticle(Long articleId, boolean includeAll, Long viewerId) {
        List<Comment> result = new ArrayList<Comment>();
        for (Comment comment : repository.listComments()) {
            if (!articleId.equals(comment.getArticleId())) continue;
            if (!includeAll && comment.getStatus() != CommentStatus.APPROVED) continue;
            result.add(decorate(comment, viewerId));
        }
        return result;
    }

    public List<Comment> listAll() {
        List<Comment> result = new ArrayList<Comment>();
        for (Comment comment : repository.listComments()) result.add(decorate(comment, null));
        return result;
    }

    public Comment create(Long articleId, User user, CommentRequest request) {
        Article article = repository.findArticleById(articleId);
        if (article == null) throw new BusinessException("文章不存在");
        if (article.getStatus() != ArticleStatus.PUBLISHED) throw new BusinessException("文章尚未上架，不能评论");
        if (request.getContent() == null || request.getContent().trim().isEmpty()) throw new BusinessException("请输入评论内容");
        if (request.getParentId() != null) {
            Comment parent = repository.findCommentById(request.getParentId());
            if (parent == null || !articleId.equals(parent.getArticleId())) {
                throw new BusinessException("父评论必须属于同一篇文章");
            }
            if (parent.getStatus() != CommentStatus.APPROVED) {
                throw new BusinessException("只能回复已公开评论");
            }
        }
        String review = aiService.reviewComment(request.getContent());
        Comment comment = new Comment();
        comment.setArticleId(articleId);
        comment.setUserId(user.getId());
        comment.setNickname(user.getNickname());
        comment.setParentId(request.getParentId());
        comment.setContent(request.getContent().trim());
        comment.setAiReviewResult(review);
        comment.setStatus(review.startsWith("PASS") ? CommentStatus.APPROVED : CommentStatus.PENDING);
        Comment saved = repository.saveComment(comment);
        if (saved.getStatus() == CommentStatus.APPROVED) {
            notifyPublishedInteractions(saved, article, user);
        } else {
            notifyAdmins("COMMENT_PENDING", "有疑似问题评论等待审核", notificationUsername(user) + " 评论了《" + article.getTitle() + "》", "/admin.html#comments", article.getId());
        }
        return decorate(saved, user.getId());
    }

    public Comment moderate(Long id, String status) {
        Comment comment = repository.findCommentById(id);
        if (comment == null) throw new BusinessException("评论不存在");
        CommentStatus previousStatus = comment.getStatus();
        if ("APPROVED".equalsIgnoreCase(status)) comment.setStatus(CommentStatus.APPROVED);
        else if ("REJECTED".equalsIgnoreCase(status)) comment.setStatus(CommentStatus.REJECTED);
        else comment.setStatus(CommentStatus.PENDING);
        Comment saved = repository.saveComment(comment);
        Article article = repository.findArticleById(saved.getArticleId());
        if (article != null) {
            if (saved.getStatus() == CommentStatus.APPROVED && previousStatus != CommentStatus.APPROVED) {
                User commenter = repository.findUserById(saved.getUserId());
                notifyPublishedInteractions(saved, article, commenter);
            } else if (saved.getStatus() == CommentStatus.REJECTED) {
                notifyUser(saved.getUserId(), "COMMENT_REJECTED", "评论未通过", "你在《" + article.getTitle() + "》下的评论已被下架", "/article.html?id=" + article.getId());
            }
        }
        return saved;
    }

    public Comment like(Long userId, Long commentId) {
        Comment comment = repository.findCommentById(commentId);
        if (comment == null) throw new BusinessException("评论不存在");
        if (comment.getStatus() != CommentStatus.APPROVED) throw new BusinessException("评论尚未公开，不能点赞");
        int changed = repository.likeComment(userId, commentId);
        Comment liked = repository.findCommentById(commentId);
        if (changed > 0 && liked.getUserId() != null && !liked.getUserId().equals(userId)) {
            User liker = repository.findUserById(userId);
            Article article = repository.findArticleById(liked.getArticleId());
            String username = notificationUsername(liker);
            String articleTitle = article == null ? "文章" : article.getTitle();
            notifyUser(liked.getUserId(), "COMMENT_LIKED", "你的评论收到点赞", username + " 点赞了你在《" + articleTitle + "》下的评论", "/article.html?id=" + liked.getArticleId() + "#comments", liker);
        }
        return decorate(liked, userId);
    }

    public Comment unlike(Long userId, Long commentId) {
        Comment comment = repository.findCommentById(commentId);
        if (comment == null) throw new BusinessException("评论不存在");
        repository.unlikeComment(userId, commentId);
        return decorate(repository.findCommentById(commentId), userId);
    }

    public void delete(Long id) {
        if (repository.findCommentById(id) == null) throw new BusinessException("评论不存在");
        repository.deleteComment(id);
    }

    private Comment decorate(Comment comment, Long viewerId) {
        if (comment == null) return null;
        comment.setLikeCount(repository.countCommentLikes(comment.getId()));
        comment.setLikedByCurrentUser(repository.hasCommentLike(viewerId, comment.getId()));
        return comment;
    }

    private void notifyPublishedInteractions(Comment comment, Article article, User commenter) {
        String username = notificationUsername(commenter);
        if (article.getAuthorId() != null && !article.getAuthorId().equals(comment.getUserId())) {
            notifyUser(article.getAuthorId(), "ARTICLE_COMMENTED", "你的博客收到评论", username + " 评论了《" + article.getTitle() + "》", "/article.html?id=" + article.getId() + "#comments", commenter);
        }
        Comment parent = comment.getParentId() == null ? null : repository.findCommentById(comment.getParentId());
        if (parent != null && parent.getUserId() != null && !parent.getUserId().equals(comment.getUserId()) &&
                !parent.getUserId().equals(article.getAuthorId())) {
            notifyUser(parent.getUserId(), "COMMENT_REPLIED", "你的评论收到回复", username + " 回复了你在《" + article.getTitle() + "》下的评论", "/article.html?id=" + article.getId() + "#comments", commenter);
        }
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
}
