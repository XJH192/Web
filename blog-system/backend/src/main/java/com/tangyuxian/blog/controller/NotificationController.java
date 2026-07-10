package com.tangyuxian.blog.controller;

import com.tangyuxian.blog.common.ApiResponse;
import com.tangyuxian.blog.model.Notification;
import com.tangyuxian.blog.model.User;
import com.tangyuxian.blog.repository.InMemoryBlogRepository;
import com.tangyuxian.blog.service.AuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
        List<Notification> notifications = repository.listNotifications(user.getId(), unreadOnly);
        for (Notification notification : notifications) fillLegacyActor(notification);
        return ApiResponse.ok(notifications);
    }

    @PutMapping("/read")
    public ApiResponse<Void> markRead(@RequestHeader(value = "X-Token", required = false) String token) {
        User user = authService.requireUser(token);
        repository.markNotificationsRead(user.getId());
        return ApiResponse.ok("消息已读", null);
    }

    @PutMapping("/{id}/read")
    public ApiResponse<Void> markOneRead(@RequestHeader(value = "X-Token", required = false) String token,
                                         @PathVariable Long id) {
        User user = authService.requireUser(token);
        repository.markNotificationRead(user.getId(), id);
        return ApiResponse.ok("消息已读", null);
    }

    private void fillLegacyActor(Notification notification) {
        if (notification == null || notification.getActorUserId() != null || notification.getContent() == null) return;
        String type = notification.getType();
        if (!"ARTICLE_LIKED".equals(type) && !"ARTICLE_COMMENT_APPROVED".equals(type) &&
                !"COMMENT_REPLY_APPROVED".equals(type) && !"COMMENT_LIKED".equals(type) &&
                !"USER_FOLLOWED".equals(type)) return;
        String content = notification.getContent().trim();
        int separator = content.indexOf(' ');
        if (separator <= 0) return;
        User actor = repository.findUserByUsername(content.substring(0, separator));
        if (actor == null) return;
        notification.setActorUserId(actor.getId());
        notification.setActorUsername(actor.getUsername());
    }
}
