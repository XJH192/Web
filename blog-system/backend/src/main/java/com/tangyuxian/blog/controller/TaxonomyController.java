package com.tangyuxian.blog.controller;

import com.tangyuxian.blog.common.ApiResponse;
import com.tangyuxian.blog.dto.CategoryRequest;
import com.tangyuxian.blog.dto.TagRequest;
import com.tangyuxian.blog.model.Category;
import com.tangyuxian.blog.model.Tag;
import com.tangyuxian.blog.service.AuthService;
import com.tangyuxian.blog.service.TaxonomyService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class TaxonomyController {
    private final TaxonomyService taxonomyService;
    private final AuthService authService;

    public TaxonomyController(TaxonomyService taxonomyService, AuthService authService) {
        this.taxonomyService = taxonomyService;
        this.authService = authService;
    }

    @GetMapping("/categories")
    public ApiResponse<List<Category>> listCategories() { return ApiResponse.ok(taxonomyService.listCategories()); }

    @PostMapping("/categories")
    public ApiResponse<Category> createCategory(@RequestHeader(value = "X-Token", required = false) String token,
                                                @RequestBody CategoryRequest request) {
        authService.requireAdmin(token);
        return ApiResponse.ok("\u5df2\u65b0\u589e\u5206\u7c7b", taxonomyService.createCategory(request));
    }

    @PutMapping("/categories/{id}")
    public ApiResponse<Category> updateCategory(@RequestHeader(value = "X-Token", required = false) String token,
                                                @PathVariable Long id,
                                                @RequestBody CategoryRequest request) {
        authService.requireAdmin(token);
        return ApiResponse.ok("\u5df2\u66f4\u65b0\u5206\u7c7b", taxonomyService.updateCategory(id, request));
    }

    @DeleteMapping("/categories/{id}")
    public ApiResponse<Void> deleteCategory(@RequestHeader(value = "X-Token", required = false) String token,
                                            @PathVariable Long id) {
        authService.requireAdmin(token);
        taxonomyService.deleteCategory(id);
        return ApiResponse.ok("\u5df2\u5220\u9664\u5206\u7c7b", null);
    }

    @GetMapping("/tags")
    public ApiResponse<List<Tag>> listTags() { return ApiResponse.ok(taxonomyService.listTags()); }

    @PostMapping("/tags")
    public ApiResponse<Tag> createTag(@RequestHeader(value = "X-Token", required = false) String token,
                                      @RequestBody TagRequest request) {
        authService.requireAdmin(token);
        return ApiResponse.ok("\u5df2\u65b0\u589e\u6807\u7b7e", taxonomyService.createTag(request));
    }

    @PutMapping("/tags/{id}")
    public ApiResponse<Tag> updateTag(@RequestHeader(value = "X-Token", required = false) String token,
                                      @PathVariable Long id,
                                      @RequestBody TagRequest request) {
        authService.requireAdmin(token);
        return ApiResponse.ok("\u5df2\u66f4\u65b0\u6807\u7b7e", taxonomyService.updateTag(id, request));
    }

    @DeleteMapping("/tags/{id}")
    public ApiResponse<Void> deleteTag(@RequestHeader(value = "X-Token", required = false) String token,
                                       @PathVariable Long id) {
        authService.requireAdmin(token);
        taxonomyService.deleteTag(id);
        return ApiResponse.ok("\u5df2\u5220\u9664\u6807\u7b7e", null);
    }
}