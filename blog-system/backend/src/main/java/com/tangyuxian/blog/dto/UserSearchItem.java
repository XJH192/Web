package com.tangyuxian.blog.dto;

public class UserSearchItem {
    private Long id;
    private String username;
    private String nickname;
    private int followerCount;
    private boolean ownProfile;
    private boolean followedByCurrentUser;
    private boolean mutualFollow;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public int getFollowerCount() { return followerCount; }
    public void setFollowerCount(int followerCount) { this.followerCount = followerCount; }
    public boolean isOwnProfile() { return ownProfile; }
    public void setOwnProfile(boolean ownProfile) { this.ownProfile = ownProfile; }
    public boolean isFollowedByCurrentUser() { return followedByCurrentUser; }
    public void setFollowedByCurrentUser(boolean followedByCurrentUser) { this.followedByCurrentUser = followedByCurrentUser; }
    public boolean isMutualFollow() { return mutualFollow; }
    public void setMutualFollow(boolean mutualFollow) { this.mutualFollow = mutualFollow; }
}
