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
        User user = authService.requireUser(token);
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
    public ApiResponse<Article> detail(@PathVariable Long id) {
        return ApiResponse.ok(articleService.detail(id, true));
    }

    @PostMapping
    public ApiResponse<Article> create(@RequestHeader(value = "X-Token", required = false) String token,
                                       @RequestBody ArticleRequest request) {
        User user = authService.requireUser(token);
        return ApiResponse.ok("文章已提交，管理员审核通过后会上架到首页", articleService.create(user, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Article> update(@RequestHeader(value = "X-Token", required = false) String token,
                                       @PathVariable Long id,
                                       @RequestBody ArticleRequest request) {
        User user = authService.requireUser(token);
        return ApiResponse.ok("文章已更新，非草稿内容需要管理员重新审核", articleService.update(user, id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@RequestHeader(value = "X-Token", required = false) String token,
                                    @PathVariable Long id) {
        User user = authService.requireUser(token);
        articleService.delete(user, id);
        return ApiResponse.ok("已删除", null);
    }

    @PostMapping("/{id}/like")
    public ApiResponse<Void> like(@RequestHeader(value = "X-Token", required = false) String token,
                                  @PathVariable Long id) {
        User user = authService.requireUser(token);
        articleService.like(user.getId(), id);
        return ApiResponse.ok("点赞成功", null);
    }

    @DeleteMapping("/{id}/like")
    public ApiResponse<Void> unlike(@RequestHeader(value = "X-Token", required = false) String token,
                                    @PathVariable Long id) {
        User user = authService.requireUser(token);
        articleService.unlike(user.getId(), id);
        return ApiResponse.ok("取消点赞", null);
    }
}