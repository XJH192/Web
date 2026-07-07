package com.tangyuxian.blog.controller;

import com.tangyuxian.blog.common.ApiResponse;
import com.tangyuxian.blog.dto.CommentRequest;
import com.tangyuxian.blog.model.Comment;
import com.tangyuxian.blog.model.User;
import com.tangyuxian.blog.service.AuthService;
import com.tangyuxian.blog.service.CommentService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class CommentController {
    private final CommentService commentService;
    private final AuthService authService;

    public CommentController(CommentService commentService, AuthService authService) {
        this.commentService = commentService;
        this.authService = authService;
    }

    @GetMapping("/articles/{articleId}/comments")
    public ApiResponse<List<Comment>> listByArticle(@PathVariable Long articleId) {
        return ApiResponse.ok(commentService.listByArticle(articleId, false));
    }

    @PostMapping("/articles/{articleId}/comments")
    public ApiResponse<Comment> create(@RequestHeader(value = "X-Token", required = false) String token,
                                       @PathVariable Long articleId,
                                       @RequestBody CommentRequest request) {
        User user = authService.requireUser(token);
        return ApiResponse.ok("\u8bc4\u8bba\u5df2\u63d0\u4ea4", commentService.create(articleId, user, request));
    }

    @GetMapping("/admin/comments")
    public ApiResponse<List<Comment>> listAll(@RequestHeader(value = "X-Token", required = false) String token) {
        authService.requireAdmin(token);
        return ApiResponse.ok(commentService.listAll());
    }

    @PutMapping("/admin/comments/{id}/moderate")
    public ApiResponse<Comment> moderate(@RequestHeader(value = "X-Token", required = false) String token,
                                         @PathVariable Long id,
                                         @RequestParam("status") String status) {
        authService.requireAdmin(token);
        return ApiResponse.ok("\u8bc4\u8bba\u72b6\u6001\u5df2\u66f4\u65b0", commentService.moderate(id, status));
    }

    @DeleteMapping("/admin/comments/{id}")
    public ApiResponse<Void> delete(@RequestHeader(value = "X-Token", required = false) String token,
                                    @PathVariable Long id) {
        authService.requireAdmin(token);
        commentService.delete(id);
        return ApiResponse.ok("\u8bc4\u8bba\u5df2\u5220\u9664", null);
    }
}