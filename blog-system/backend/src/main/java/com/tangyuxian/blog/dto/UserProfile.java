package com.tangyuxian.blog.dto;

import com.tangyuxian.blog.model.Article;
import com.tangyuxian.blog.model.GalleryPhoto;

import java.util.ArrayList;
import java.util.List;

public class UserProfile {
    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String maskedEmail;
    private boolean privateDataVisible;
    private int followerCount;
    private int followingCount;
    private boolean ownProfile;
    private boolean followedByCurrentUser;
    private boolean followsCurrentUser;
    private boolean mutualFollow;
    private List<Article> articles = new ArrayList<Article>();
    private List<GalleryPhoto> photos = new ArrayList<GalleryPhoto>();

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getMaskedEmail() { return maskedEmail; }
    public void setMaskedEmail(String maskedEmail) { this.maskedEmail = maskedEmail; }
    public boolean isPrivateDataVisible() { return privateDataVisible; }
    public void setPrivateDataVisible(boolean privateDataVisible) { this.privateDataVisible = privateDataVisible; }
    public int getFollowerCount() { return followerCount; }
    public void setFollowerCount(int followerCount) { this.followerCount = followerCount; }
    public int getFollowingCount() { return followingCount; }
    public void setFollowingCount(int followingCount) { this.followingCount = followingCount; }
    public boolean isOwnProfile() { return ownProfile; }
    public void setOwnProfile(boolean ownProfile) { this.ownProfile = ownProfile; }
    public boolean isFollowedByCurrentUser() { return followedByCurrentUser; }
    public void setFollowedByCurrentUser(boolean followedByCurrentUser) { this.followedByCurrentUser = followedByCurrentUser; }
    public boolean isFollowsCurrentUser() { return followsCurrentUser; }
    public void setFollowsCurrentUser(boolean followsCurrentUser) { this.followsCurrentUser = followsCurrentUser; }
    public boolean isMutualFollow() { return mutualFollow; }
    public void setMutualFollow(boolean mutualFollow) { this.mutualFollow = mutualFollow; }
    public List<Article> getArticles() { return articles; }
    public void setArticles(List<Article> articles) { this.articles = articles; }
    public List<GalleryPhoto> getPhotos() { return photos; }
    public void setPhotos(List<GalleryPhoto> photos) { this.photos = photos; }
}
