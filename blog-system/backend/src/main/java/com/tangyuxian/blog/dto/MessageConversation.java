package com.tangyuxian.blog.dto;

import com.tangyuxian.blog.model.PrivateMessage;

import java.util.ArrayList;
import java.util.List;

public class MessageConversation {
    private Long otherUserId;
    private String otherUsername;
    private String otherNickname;
    private boolean mutualFollow;
    private int sentCount;
    private int remainingMessageCount;
    private List<PrivateMessage> messages = new ArrayList<PrivateMessage>();

    public Long getOtherUserId() { return otherUserId; }
    public void setOtherUserId(Long otherUserId) { this.otherUserId = otherUserId; }
    public String getOtherUsername() { return otherUsername; }
    public void setOtherUsername(String otherUsername) { this.otherUsername = otherUsername; }
    public String getOtherNickname() { return otherNickname; }
    public void setOtherNickname(String otherNickname) { this.otherNickname = otherNickname; }
    public boolean isMutualFollow() { return mutualFollow; }
    public void setMutualFollow(boolean mutualFollow) { this.mutualFollow = mutualFollow; }
    public int getSentCount() { return sentCount; }
    public void setSentCount(int sentCount) { this.sentCount = sentCount; }
    public int getRemainingMessageCount() { return remainingMessageCount; }
    public void setRemainingMessageCount(int remainingMessageCount) { this.remainingMessageCount = remainingMessageCount; }
    public List<PrivateMessage> getMessages() { return messages; }
    public void setMessages(List<PrivateMessage> messages) { this.messages = messages; }
}
