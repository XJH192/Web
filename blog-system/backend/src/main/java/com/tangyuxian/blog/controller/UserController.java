package com.tangyuxian.blog.controller;

import com.tangyuxian.blog.common.ApiResponse;
import com.tangyuxian.blog.common.BusinessException;
import com.tangyuxian.blog.dto.FollowStatus;
import com.tangyuxian.blog.dto.UserProfile;
import com.tangyuxian.blog.dto.UserSearchItem;
import com.tangyuxian.blog.model.Notification;
import com.tangyuxian.blog.model.Role;
import com.tangyuxian.blog.model.User;
import com.tangyuxian.blog.repository.InMemoryBlogRepository;
import com.tangyuxian.blog.service.ArticleService;
import com.tangyuxian.blog.service.AuthService;
import com.tangyuxian.blog.service.GalleryPhotoService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final InMemoryBlogRepository repository;
    private final AuthService authService;
    private final ArticleService articleService;
    private final GalleryPhotoService galleryPhotoService;

    public UserController(InMemoryBlogRepository repository, AuthService authService, ArticleService articleService,
                          GalleryPhotoService galleryPhotoService) {
        this.repository = repository;
        this.authService = authService;
        this.articleService = articleService;
        this.galleryPhotoService = galleryPhotoService;
    }

    @GetMapping("/{id}/follow-status")
    public ApiResponse<FollowStatus> followStatus(@RequestHeader(value = "X-Token", required = false) String token,
                                                  @PathVariable Long id) {
        User currentUser = authService.requireUser(token);
        requireTarget(id);
        return ApiResponse.ok(buildStatus(currentUser.getId(), id));
    }

    @PostMapping("/{id}/follow")
    public ApiResponse<FollowStatus> follow(@RequestHeader(value = "X-Token", required = false) String token,
                                            @PathVariable Long id) {
        User currentUser = authService.requireUser(token);
        User target = requireTarget(id);
        if (currentUser.getId().equals(id)) throw new BusinessException("不能关注自己");
        if (target.isBanned()) throw new BusinessException("该用户当前不可关注");
        int changed = repository.followUser(currentUser.getId(), id);
        if (changed > 0) notifyFollowedUser(target, currentUser);
        return ApiResponse.ok("关注成功", buildStatus(currentUser.getId(), id));
    }

    @DeleteMapping("/{id}/follow")
    public ApiResponse<FollowStatus> unfollow(@RequestHeader(value = "X-Token", required = false) String token,
                                              @PathVariable Long id) {
        User currentUser = authService.requireUser(token);
        requireTarget(id);
        if (currentUser.getId().equals(id)) throw new BusinessException("不能取消关注自己");
        repository.unfollowUser(currentUser.getId(), id);
        return ApiResponse.ok("已取消关注", buildStatus(currentUser.getId(), id));
    }

    @GetMapping("/{id}/profile")
    public ApiResponse<UserProfile> profile(@RequestHeader(value = "X-Token", required = false) String token,
                                            @PathVariable Long id) {
        User currentUser = authService.optionalUser(token);
        User target = requireTarget(id);
        Long currentUserId = currentUser == null ? null : currentUser.getId();
        FollowStatus status = buildStatus(currentUserId, id);
        boolean ownProfile = currentUserId != null && currentUserId.equals(id);
        boolean privateDataVisible = ownProfile || (currentUser != null && currentUser.getRole() == Role.ADMIN);
        UserProfile profile = new UserProfile();
        profile.setId(target.getId());
        profile.setUsername(target.getUsername());
        profile.setNickname(target.getNickname());
        profile.setEmail(privateDataVisible ? target.getEmail() : null);
        profile.setMaskedEmail(maskEmail(target.getEmail()));
        profile.setPrivateDataVisible(privateDataVisible);
        profile.setFollowerCount(status.getFollowerCount());
        profile.setFollowingCount(status.getFollowingCount());
        profile.setOwnProfile(ownProfile);
        profile.setFollowedByCurrentUser(status.isFollowedByCurrentUser());
        profile.setFollowsCurrentUser(status.isFollowsCurrentUser());
        profile.setMutualFollow(status.isMutualFollow());
        profile.setArticles(articleService.listPublishedByAuthor(id, currentUserId));
        profile.setPhotos(galleryPhotoService.listByOwner(id));
        return ApiResponse.ok(profile);
    }

    @GetMapping("/{id}/followers")
    public ApiResponse<List<UserSearchItem>> followers(@RequestHeader(value = "X-Token", required = false) String token,
                                                        @PathVariable Long id) {
        User currentUser = authService.requireUser(token);
        requireTarget(id);
        return ApiResponse.ok(socialItems(currentUser.getId(), repository.listUserFollowers(id)));
    }

    @GetMapping("/{id}/following")
    public ApiResponse<List<UserSearchItem>> following(@RequestHeader(value = "X-Token", required = false) String token,
                                                        @PathVariable Long id) {
        User currentUser = authService.requireUser(token);
        requireTarget(id);
        return ApiResponse.ok(socialItems(currentUser.getId(), repository.listUserFollowing(id)));
    }

    private User requireTarget(Long id) {
        User target = repository.findUserById(id);
        if (target == null) throw new BusinessException("用户不存在");
        return target;
    }

    private FollowStatus buildStatus(Long currentUserId, Long targetUserId) {
        boolean self = currentUserId != null && currentUserId.equals(targetUserId);
        boolean followedByCurrentUser = currentUserId != null && !self && repository.hasUserFollow(currentUserId, targetUserId);
        boolean followsCurrentUser = currentUserId != null && !self && repository.hasUserFollow(targetUserId, currentUserId);
        return new FollowStatus(
                targetUserId,
                repository.countUserFollowers(targetUserId),
                repository.countUserFollowing(targetUserId),
                followedByCurrentUser,
                followsCurrentUser
        );
    }

    private void notifyFollowedUser(User target, User actor) {
        Notification notification = new Notification();
        notification.setUserId(target.getId());
        notification.setActorUserId(actor.getId());
        notification.setActorUsername(actor.getUsername());
        notification.setType("USER_FOLLOWED");
        notification.setTitle("你有新的关注者");
        notification.setContent(actor.getUsername() + " 关注了你");
        notification.setLink("/user.html?id=" + actor.getId());
        notification.setReadFlag(false);
        repository.saveNotification(notification);
    }

    private List<UserSearchItem> socialItems(Long currentUserId, List<User> users) {
        List<UserSearchItem> result = new ArrayList<UserSearchItem>();
        for (User user : users) {
            boolean self = currentUserId.equals(user.getId());
            boolean following = !self && repository.hasUserFollow(currentUserId, user.getId());
            UserSearchItem item = new UserSearchItem();
            item.setId(user.getId());
            item.setUsername(user.getUsername());
            item.setNickname(user.getNickname());
            item.setFollowerCount(repository.countUserFollowers(user.getId()));
            item.setOwnProfile(self);
            item.setFollowedByCurrentUser(following);
            item.setMutualFollow(following && repository.hasUserFollow(user.getId(), currentUserId));
            result.add(item);
        }
        return result;
    }

    private String maskEmail(String email) {
        if (email == null || email.trim().isEmpty()) return "未公开";
        String value = email.trim();
        int at = value.indexOf('@');
        if (at <= 0 || at == value.length() - 1) return value.substring(0, 1) + "***";
        return value.substring(0, 1) + "***@" + value.substring(at + 1);
    }
}
