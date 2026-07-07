package com.tangyuxian.blog.controller;

import com.tangyuxian.blog.common.ApiResponse;
import com.tangyuxian.blog.dto.AuthRequest;
import com.tangyuxian.blog.dto.LoginResponse;
import com.tangyuxian.blog.dto.RegisterRequest;
import com.tangyuxian.blog.model.User;
import com.tangyuxian.blog.service.AuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ApiResponse<User> register(@RequestBody RegisterRequest request) {
        return ApiResponse.ok("\u6ce8\u518c\u6210\u529f", authService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@RequestBody AuthRequest request) {
        return ApiResponse.ok("\u767b\u5f55\u6210\u529f", authService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<User> me(@RequestHeader(value = "X-Token", required = false) String token) {
        return ApiResponse.ok(authService.requireUser(token));
    }
}