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
        List<Comment> result = new ArrayList<Comment>();
        for (Comment comment : repository.listComments()) {
            if (!articleId.equals(comment.getArticleId())) continue;
            if (!includeAll && comment.getStatus() != CommentStatus.APPROVED) continue;
            result.add(comment);
        }
        return result;
    }

    public List<Comment> listAll() { return repository.listComments(); }

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
        }
        String review = aiService.reviewComment(request.getContent());
        Comment comment = new Comment();
        comment.setArticleId(articleId);
        comment.setUserId(user.getId());
        comment.setNickname(user.getNickname());
        comment.setParentId(request.getParentId());
        comment.setContent(request.getContent().trim());
        comment.setAiReviewResult(review);
        comment.setStatus(review.startsWith("PASS") ? CommentStatus.PENDING : CommentStatus.REJECTED);
        Comment saved = repository.saveComment(comment);
        notifyAdmins("COMMENT_PENDING", "有新评论等待审核", user.getNickname() + " 评论了《" + article.getTitle() + "》", "/admin.html#comments");
        if (article.getAuthorId() != null && !article.getAuthorId().equals(user.getId())) {
            notifyUser(article.getAuthorId(), "ARTICLE_COMMENTED", "你的博客收到评论", user.getNickname() + " 评论了《" + article.getTitle() + "》，管理员审核后会公开", "/article.html?id=" + article.getId());
        }
        return saved;
    }

    public Comment moderate(Long id, String status) {
        Comment comment = repository.findCommentById(id);
        if (comment == null) throw new BusinessException("评论不存在");
        if ("APPROVED".equalsIgnoreCase(status)) comment.setStatus(CommentStatus.APPROVED);
        else if ("REJECTED".equalsIgnoreCase(status)) comment.setStatus(CommentStatus.REJECTED);
        else comment.setStatus(CommentStatus.PENDING);
        Comment saved = repository.saveComment(comment);
        Article article = repository.findArticleById(saved.getArticleId());
        if (article != null) {
            if (saved.getStatus() == CommentStatus.APPROVED) {
                notifyUser(saved.getUserId(), "COMMENT_APPROVED", "评论已通过", "你在《" + article.getTitle() + "》下的评论已公开", "/article.html?id=" + article.getId());
                if (article.getAuthorId() != null && !article.getAuthorId().equals(saved.getUserId())) {
                    notifyUser(article.getAuthorId(), "ARTICLE_COMMENT_APPROVED", "你的博客有新公开评论", saved.getNickname() + " 的评论已通过审核", "/article.html?id=" + article.getId());
                }
            } else if (saved.getStatus() == CommentStatus.REJECTED) {
                notifyUser(saved.getUserId(), "COMMENT_REJECTED", "评论未通过", "你在《" + article.getTitle() + "》下的评论已被下架", "/article.html?id=" + article.getId());
            }
        }
        return saved;
    }

    public void delete(Long id) {
        if (repository.findCommentById(id) == null) throw new BusinessException("评论不存在");
        repository.deleteComment(id);
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
}