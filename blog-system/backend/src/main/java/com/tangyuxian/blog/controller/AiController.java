package com.tangyuxian.blog.controller;

import com.tangyuxian.blog.common.ApiResponse;
import com.tangyuxian.blog.dto.AiRequest;
import com.tangyuxian.blog.service.AiService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiController {
    private final AiService aiService;

    public AiController(AiService aiService) {
        this.aiService = aiService;
    }

    @PostMapping("/outline")
    public ApiResponse<Map<String, Object>> outline(@RequestBody AiRequest request) {
        return ApiResponse.ok(aiService.outline(request));
    }

    @PostMapping("/summary")
    public ApiResponse<Map<String, Object>> summary(@RequestBody AiRequest request) {
        return ApiResponse.ok(aiService.summary(request));
    }

    @PostMapping("/tags")
    public ApiResponse<Map<String, Object>> tags(@RequestBody AiRequest request) {
        return ApiResponse.ok(aiService.recommendTags(request));
    }

    @PostMapping("/qa")
    public ApiResponse<Map<String, Object>> answer(@RequestBody AiRequest request) {
        return ApiResponse.ok(aiService.answer(request));
    }
}
