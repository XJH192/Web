package com.tangyuxian.blog.controller;

import com.tangyuxian.blog.common.ApiResponse;
import com.tangyuxian.blog.dto.AiRequest;
import com.tangyuxian.blog.service.AuthService;
import com.tangyuxian.blog.service.AiService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {
    private final AiService aiService;
    private final AuthService authService;

    public AiController(AiService aiService, AuthService authService) {
        this.aiService = aiService;
        this.authService = authService;
    }

    @PostMapping("/outline")
    public ApiResponse<Map<String, Object>> outline(@RequestHeader(value = "X-Token", required = false) String token,
                                                    @RequestBody AiRequest request) {
        authService.requireUser(token);
        return ApiResponse.ok(aiService.outline(request));
    }

    @PostMapping("/summary")
    public ApiResponse<Map<String, Object>> summary(@RequestHeader(value = "X-Token", required = false) String token,
                                                    @RequestBody AiRequest request) {
        authService.requireUser(token);
        return ApiResponse.ok(aiService.summary(request));
    }

    @PostMapping("/tags")
    public ApiResponse<Map<String, Object>> tags(@RequestHeader(value = "X-Token", required = false) String token,
                                                 @RequestBody AiRequest request) {
        authService.requireUser(token);
        return ApiResponse.ok(aiService.recommendTags(request));
    }

    @PostMapping("/category")
    public ApiResponse<Map<String, Object>> category(@RequestHeader(value = "X-Token", required = false) String token,
                                                     @RequestBody AiRequest request) {
        authService.requireUser(token);
        return ApiResponse.ok(aiService.recommendCategory(request));
    }

    @PostMapping("/qa")
    public ApiResponse<Map<String, Object>> answer(@RequestHeader(value = "X-Token", required = false) String token,
                                                   @RequestBody AiRequest request) {
        authService.requireUser(token);
        return ApiResponse.ok(aiService.answer(request));
    }
}
