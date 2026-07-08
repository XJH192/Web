package com.tangyuxian.blog.controller;

import com.tangyuxian.blog.common.ApiResponse;
import com.tangyuxian.blog.common.BusinessException;
import com.tangyuxian.blog.dto.DashboardStats;
import com.tangyuxian.blog.model.AiUsageLog;
import com.tangyuxian.blog.model.Article;
import com.tangyuxian.blog.model.ArticleStatus;
import com.tangyuxian.blog.model.Comment;
import com.tangyuxian.blog.model.Role;
import com.tangyuxian.blog.model.User;
import com.tangyuxian.blog.repository.InMemoryBlogRepository;
import com.tangyuxian.blog.service.AdminService;
import com.tangyuxian.blog.service.ArticleService;
import com.tangyuxian.blog.service.AuthService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AdminService adminService;
    private final ArticleService articleService;
    private final AuthService authService;
    private final InMemoryBlogRepository repository;

    public AdminController(AdminService adminService, ArticleService articleService, AuthService authService, InMemoryBlogRepository repository) {
        this.adminService = adminService;
        this.articleService = articleService;
        this.authService = authService;
        this.repository = repository;
    }

    @GetMapping("/stats")
    public ApiResponse<DashboardStats> stats(@RequestHeader(value = "X-Token", required = false) String token) {
        authService.requireAdmin(token);
        return ApiResponse.ok(adminService.stats());
    }

    @GetMapping("/users")
    public ApiResponse<List<User>> users(@RequestHeader(value = "X-Token", required = false) String token) {
        authService.requireAdmin(token);
        return ApiResponse.ok(repository.listUsers());
    }

    @PutMapping("/users/{id}/role")
    public ApiResponse<User> updateUserRole(@RequestHeader(value = "X-Token", required = false) String token,
                                            @PathVariable Long id,
                                            @RequestParam("role") String roleName) {
        authService.requireAdmin(token);
        User user = repository.findUserById(id);
        if (user == null) throw new BusinessException("用户不存在");
        Role role;
        try {
            role = Role.valueOf(roleName.toUpperCase());
        } catch (Exception ex) {
            throw new BusinessException("角色只能是 USER 或 ADMIN");
        }
        user.setRole(role);
        return ApiResponse.ok("已更新用户角色", repository.saveUser(user));
    }

    @PutMapping("/users/{id}/ban")
    public ApiResponse<User> banUser(@RequestHeader(value = "X-Token", required = false) String token,
                                     @PathVariable Long id,
                                     @RequestParam("banned") boolean banned) {
        User current = authService.requireAdmin(token);
        User user = repository.findUserById(id);
        if (user == null) throw new BusinessException("用户不存在");
        if (current.getId().equals(id)) throw new BusinessException("不能封禁当前登录的管理员");
        user.setBanned(banned);
        return ApiResponse.ok(banned ? "已封禁用户" : "已解封用户", repository.saveUser(user));
    }

    @DeleteMapping("/users/{id}")
    public ApiResponse<Void> deleteUser(@RequestHeader(value = "X-Token", required = false) String token,
                                        @PathVariable Long id) {
        User current = authService.requireAdmin(token);
        if (current.getId().equals(id)) throw new BusinessException("不能删除当前登录的管理员");
        if (repository.findUserById(id) == null) throw new BusinessException("用户不存在");
        for (Article article : repository.listArticles()) {
            if (id.equals(article.getAuthorId())) throw new BusinessException("该用户已发布文章，请先处理文章");
        }
        for (Comment comment : repository.listComments()) {
            if (id.equals(comment.getUserId())) throw new BusinessException("该用户已发表评论，请先处理评论");
        }
        repository.deleteUser(id);
        return ApiResponse.ok("已删除用户", null);
    }

    @GetMapping("/articles")
    public ApiResponse<List<Article>> articles(@RequestHeader(value = "X-Token", required = false) String token) {
        authService.requireAdmin(token);
        List<Article> visible = new ArrayList<Article>();
        for (Article article : repository.listArticles()) {
            if (article.getStatus() != ArticleStatus.DRAFT) visible.add(article);
        }
        return ApiResponse.ok(visible);
    }

    @PutMapping("/articles/{id}/status")
    public ApiResponse<Article> updateArticleStatus(@RequestHeader(value = "X-Token", required = false) String token,
                                                    @PathVariable Long id,
                                                    @RequestParam("status") String status) {
        authService.requireAdmin(token);
        return ApiResponse.ok("文章审核状态已更新", articleService.updateStatus(id, status));
    }

    @GetMapping("/ai-logs")
    public ApiResponse<List<AiUsageLog>> aiLogs(@RequestHeader(value = "X-Token", required = false) String token) {
        authService.requireAdmin(token);
        return ApiResponse.ok(repository.listAiUsageLogs());
    }
}