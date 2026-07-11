package com.tangyuxian.blog.controller;

import com.tangyuxian.blog.common.ApiResponse;
import com.tangyuxian.blog.dto.SearchResult;
import com.tangyuxian.blog.dto.UserSearchItem;
import com.tangyuxian.blog.model.User;
import com.tangyuxian.blog.repository.InMemoryBlogRepository;
import com.tangyuxian.blog.service.ArticleService;
import com.tangyuxian.blog.service.AuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/search")
public class SearchController {
    private final InMemoryBlogRepository repository;
    private final AuthService authService;
    private final ArticleService articleService;

    public SearchController(InMemoryBlogRepository repository, AuthService authService, ArticleService articleService) {
        this.repository = repository;
        this.authService = authService;
        this.articleService = articleService;
    }

    @GetMapping
    public ApiResponse<SearchResult> search(@RequestHeader(value = "X-Token", required = false) String token,
                                            @RequestParam(value = "keyword", required = false) String keyword) {
        User currentUser = authService.optionalUser(token);
        Long currentUserId = currentUser == null ? null : currentUser.getId();
        String query = keyword == null ? "" : keyword.trim().toLowerCase();
        SearchResult result = new SearchResult();
        if (query.isEmpty()) return ApiResponse.ok(result);

        List<UserSearchItem> users = new ArrayList<UserSearchItem>();
        for (User user : repository.listUsers()) {
            if (user.isBanned()) continue;
            String username = user.getUsername() == null ? "" : user.getUsername().toLowerCase();
            String nickname = user.getNickname() == null ? "" : user.getNickname().toLowerCase();
            if (!username.contains(query) && !nickname.contains(query)) continue;
            boolean self = currentUserId != null && currentUserId.equals(user.getId());
            boolean following = currentUserId != null && !self && repository.hasUserFollow(currentUserId, user.getId());
            UserSearchItem item = new UserSearchItem();
            item.setId(user.getId());
            item.setUsername(user.getUsername());
            item.setNickname(user.getNickname());
            item.setFollowerCount(repository.countUserFollowers(user.getId()));
            item.setOwnProfile(self);
            item.setFollowedByCurrentUser(following);
            item.setMutualFollow(following && repository.hasUserFollow(user.getId(), currentUser.getId()));
            users.add(item);
            if (users.size() >= 10) break;
        }
        result.setUsers(users);
        result.setArticles(articleService.searchPublishedByTitle(query, currentUserId, 12));
        return ApiResponse.ok(result);
    }
}
