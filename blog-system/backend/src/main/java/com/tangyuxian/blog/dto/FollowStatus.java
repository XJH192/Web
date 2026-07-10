package com.tangyuxian.blog.dto;

public class FollowStatus {
    private Long targetUserId;
    private int followerCount;
    private int followingCount;
    private boolean followedByCurrentUser;
    private boolean followsCurrentUser;
    private boolean mutualFollow;

    public FollowStatus() {
    }

    public FollowStatus(Long targetUserId, int followerCount, int followingCount,
                        boolean followedByCurrentUser, boolean followsCurrentUser) {
        this.targetUserId = targetUserId;
        this.followerCount = followerCount;
        this.followingCount = followingCount;
        this.followedByCurrentUser = followedByCurrentUser;
        this.followsCurrentUser = followsCurrentUser;
        this.mutualFollow = followedByCurrentUser && followsCurrentUser;
    }

    public Long getTargetUserId() { return targetUserId; }
    public void setTargetUserId(Long targetUserId) { this.targetUserId = targetUserId; }
    public int getFollowerCount() { return followerCount; }
    public void setFollowerCount(int followerCount) { this.followerCount = followerCount; }
    public int getFollowingCount() { return followingCount; }
    public void setFollowingCount(int followingCount) { this.followingCount = followingCount; }
    public boolean isFollowedByCurrentUser() { return followedByCurrentUser; }
    public void setFollowedByCurrentUser(boolean followedByCurrentUser) { this.followedByCurrentUser = followedByCurrentUser; }
    public boolean isFollowsCurrentUser() { return followsCurrentUser; }
    public void setFollowsCurrentUser(boolean followsCurrentUser) { this.followsCurrentUser = followsCurrentUser; }
    public boolean isMutualFollow() { return mutualFollow; }
    public void setMutualFollow(boolean mutualFollow) { this.mutualFollow = mutualFollow; }
}
