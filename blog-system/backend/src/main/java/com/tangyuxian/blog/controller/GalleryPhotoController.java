package com.tangyuxian.blog.controller;

import com.tangyuxian.blog.common.ApiResponse;
import com.tangyuxian.blog.dto.GalleryPhotoRequest;
import com.tangyuxian.blog.model.GalleryPhoto;
import com.tangyuxian.blog.model.User;
import com.tangyuxian.blog.service.AuthService;
import com.tangyuxian.blog.service.GalleryPhotoService;
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
@RequestMapping("/api/gallery/photos")
public class GalleryPhotoController {
    private final GalleryPhotoService galleryPhotoService;
    private final AuthService authService;

    public GalleryPhotoController(GalleryPhotoService galleryPhotoService, AuthService authService) {
        this.galleryPhotoService = galleryPhotoService;
        this.authService = authService;
    }

    @GetMapping
    public ApiResponse<List<GalleryPhoto>> list(@RequestHeader(value = "X-Token", required = false) String token) {
        User user = authService.requireUser(token);
        return ApiResponse.ok(galleryPhotoService.list(user));
    }

    @PostMapping
    public ApiResponse<GalleryPhoto> create(@RequestHeader(value = "X-Token", required = false) String token,
                                            @RequestBody GalleryPhotoRequest request) {
        User user = authService.requireUser(token);
        return ApiResponse.ok("图片上传成功", galleryPhotoService.create(user, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<GalleryPhoto> update(@RequestHeader(value = "X-Token", required = false) String token,
                                            @PathVariable Long id,
                                            @RequestBody GalleryPhotoRequest request) {
        User user = authService.requireUser(token);
        return ApiResponse.ok("图片修改成功", galleryPhotoService.update(user, id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@RequestHeader(value = "X-Token", required = false) String token,
                                    @PathVariable Long id) {
        User user = authService.requireUser(token);
        galleryPhotoService.delete(user, id);
        return ApiResponse.ok("图片已删除", null);
    }
}
