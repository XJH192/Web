package com.tangyuxian.blog.controller;

import com.tangyuxian.blog.common.ApiResponse;
import com.tangyuxian.blog.model.Notification;
import com.tangyuxian.blog.model.User;
import com.tangyuxian.blog.repository.InMemoryBlogRepository;
import com.tangyuxian.blog.service.AuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final InMemoryBlogRepository repository;
    private final AuthService authService;

    public NotificationController(InMemoryBlogRepository repository, AuthService authService) {
        this.repository = repository;
        this.authService = authService;
    }

    @GetMapping
    public ApiResponse<List<Notification>> list(@RequestHeader(value = "X-Token", required = false) String token,
                                                @RequestParam(value = "unread", defaultValue = "true") boolean unreadOnly) {
        User user = authService.requireUser(token);
        return ApiResponse.ok(repository.listNotifications(user.getId(), unreadOnly));
    }

    @PutMapping("/read")
    public ApiResponse<Void> markRead(@RequestHeader(value = "X-Token", required = false) String token) {
        User user = authService.requireUser(token);
        repository.markNotificationsRead(user.getId());
        return ApiResponse.ok("消息已读", null);
    }
}