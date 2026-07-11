package com.tangyuxian.blog.controller;

import com.tangyuxian.blog.common.ApiResponse;
import com.tangyuxian.blog.dto.MomentRequest;
import com.tangyuxian.blog.model.Moment;
import com.tangyuxian.blog.model.User;
import com.tangyuxian.blog.service.AuthService;
import com.tangyuxian.blog.service.MomentService;
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
@RequestMapping("/api/moments")
public class MomentController {
    private final MomentService momentService;
    private final AuthService authService;

    public MomentController(MomentService momentService, AuthService authService) {
        this.momentService = momentService;
        this.authService = authService;
    }

    /** 动态列表对所有访客公开，无需登录即可浏览。 */
    @GetMapping
    public ApiResponse<List<Moment>> list() {
        return ApiResponse.ok(momentService.list());
    }

    /** 仅管理员可以发布动态。 */
    @PostMapping
    public ApiResponse<Moment> create(@RequestHeader(value = "X-Token", required = false) String token,
                                      @RequestBody MomentRequest request) {
        User admin = authService.requireAdmin(token);
        return ApiResponse.ok("动态已发布", momentService.create(admin, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Moment> update(@RequestHeader(value = "X-Token", required = false) String token,
                                      @PathVariable Long id,
                                      @RequestBody MomentRequest request) {
        authService.requireAdmin(token);
        return ApiResponse.ok("动态已更新", momentService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@RequestHeader(value = "X-Token", required = false) String token,
                                    @PathVariable Long id) {
        authService.requireAdmin(token);
        momentService.delete(id);
        return ApiResponse.ok("动态已删除", null);
    }
}
