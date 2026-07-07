package com.tangyuxian.blog.service;

import com.tangyuxian.blog.common.BusinessException;
import com.tangyuxian.blog.dto.CommentRequest;
import com.tangyuxian.blog.model.Article;
import com.tangyuxian.blog.model.ArticleStatus;
import com.tangyuxian.blog.model.Comment;
import com.tangyuxian.blog.model.CommentStatus;
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
        return repository.saveComment(comment);
    }

    public Comment moderate(Long id, String status) {
        Comment comment = repository.findCommentById(id);
        if (comment == null) throw new BusinessException("评论不存在");
        if ("APPROVED".equalsIgnoreCase(status)) comment.setStatus(CommentStatus.APPROVED);
        else if ("REJECTED".equalsIgnoreCase(status)) comment.setStatus(CommentStatus.REJECTED);
        else comment.setStatus(CommentStatus.PENDING);
        return repository.saveComment(comment);
    }

    public void delete(Long id) {
        if (repository.findCommentById(id) == null) throw new BusinessException("评论不存在");
        repository.deleteComment(id);
    }
}