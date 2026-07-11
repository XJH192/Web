package com.tangyuxian.blog.controller;

import com.tangyuxian.blog.common.ApiResponse;
import com.tangyuxian.blog.dto.ArticleRequest;
import com.tangyuxian.blog.model.Article;
import com.tangyuxian.blog.model.User;
import com.tangyuxian.blog.service.ArticleService;
import com.tangyuxian.blog.service.AuthService;
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
@RequestMapping("/api/articles")
public class ArticleController {
    private final ArticleService articleService;
    private final AuthService authService;

    public ArticleController(ArticleService articleService, AuthService authService) {
        this.articleService = articleService;
        this.authService = authService;
    }

    @GetMapping
    public ApiResponse<List<Article>> list(@RequestParam(value = "keyword", required = false) String keyword,
                                           @RequestParam(value = "categoryId", required = false) Long categoryId,
                                           @RequestParam(value = "tagId", required = false) Long tagId,
                                           @RequestParam(value = "includeDrafts", defaultValue = "false") boolean includeDrafts) {
        return ApiResponse.ok(articleService.list(keyword, categoryId, tagId, includeDrafts));
    }

    @GetMapping("/feed")
    public ApiResponse<List<Article>> feed(@RequestHeader(value = "X-Token", required = false) String token,
                                           @RequestParam(value = "keyword", required = false) String keyword,
                                           @RequestParam(value = "categoryId", required = false) Long categoryId,
                                           @RequestParam(value = "tagId", required = false) Long tagId) {
        User user = authService.optionalUser(token);
        return ApiResponse.ok(articleService.feed(user, keyword, categoryId, tagId));
    }

    @GetMapping("/mine")
    public ApiResponse<List<Article>> mine(@RequestHeader(value = "X-Token", required = false) String token,
                                           @RequestParam(value = "keyword", required = false) String keyword,
                                           @RequestParam(value = "categoryId", required = false) Long categoryId,
                                           @RequestParam(value = "tagId", required = false) Long tagId) {
        User user = authService.requireUser(token);
        return ApiResponse.ok(articleService.listMine(user, keyword, categoryId, tagId));
    }

    @GetMapping("/{id}")
    public ApiResponse<Article> detail(@RequestHeader(value = "X-Token", required = false) String token,
                                       @PathVariable Long id) {
        User user = authService.optionalUser(token);
        return ApiResponse.ok(articleService.detail(id, true, user == null ? null : user.getId()));
    }

    @PostMapping
    public ApiResponse<Article> create(@RequestHeader(value = "X-Token", required = false) String token,
                                       @RequestBody ArticleRequest request) {
        User user = authService.requireUser(token);
        Article article = articleService.create(user, request);
        return ApiResponse.ok(articleMessage(article, false), article);
    }

    @PutMapping("/{id}")
    public ApiResponse<Article> update(@RequestHeader(value = "X-Token", required = false) String token,
                                       @PathVariable Long id,
                                       @RequestBody ArticleRequest request) {
        User user = authService.requireUser(token);
        Article article = articleService.update(user, id, request);
        return ApiResponse.ok(articleMessage(article, true), article);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@RequestHeader(value = "X-Token", required = false) String token,
                                    @PathVariable Long id) {
        User user = authService.requireUser(token);
        articleService.delete(user, id);
        return ApiResponse.ok("已删除", null);
    }

    @PostMapping("/{id}/like")
    public ApiResponse<Article> like(@RequestHeader(value = "X-Token", required = false) String token,
                                     @PathVariable Long id) {
        User user = authService.requireUser(token);
        return ApiResponse.ok("点赞成功", articleService.like(user.getId(), id));
    }

    @DeleteMapping("/{id}/like")
    public ApiResponse<Article> unlike(@RequestHeader(value = "X-Token", required = false) String token,
                                       @PathVariable Long id) {
        User user = authService.requireUser(token);
        return ApiResponse.ok("已取消点赞", articleService.unlike(user.getId(), id));
    }

    private String articleMessage(Article article, boolean updated) {
        if (article.getStatus() == com.tangyuxian.blog.model.ArticleStatus.DRAFT) return updated ? "草稿已更新" : "草稿已保存";
        if (article.getStatus() == com.tangyuxian.blog.model.ArticleStatus.PUBLISHED) return "AI 初审通过，文章已直接发布";
        return "AI 初审发现疑似问题，已转交管理员审核";
    }
}
