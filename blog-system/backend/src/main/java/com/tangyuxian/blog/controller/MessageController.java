package com.tangyuxian.blog.controller;

import com.tangyuxian.blog.common.ApiResponse;
import com.tangyuxian.blog.common.BusinessException;
import com.tangyuxian.blog.dto.MessageConversation;
import com.tangyuxian.blog.dto.PrivateMessageRequest;
import com.tangyuxian.blog.model.Notification;
import com.tangyuxian.blog.model.PrivateMessage;
import com.tangyuxian.blog.model.User;
import com.tangyuxian.blog.repository.InMemoryBlogRepository;
import com.tangyuxian.blog.service.AuthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/messages")
public class MessageController {
    private static final int NON_MUTUAL_LIMIT = 3;

    private final InMemoryBlogRepository repository;
    private final AuthService authService;

    public MessageController(InMemoryBlogRepository repository, AuthService authService) {
        this.repository = repository;
        this.authService = authService;
    }

    @GetMapping("/{otherUserId}")
    public ApiResponse<MessageConversation> conversation(@RequestHeader(value = "X-Token", required = false) String token,
                                                          @PathVariable Long otherUserId) {
        User currentUser = authService.requireUser(token);
        User otherUser = requireOtherUser(currentUser, otherUserId);
        repository.markPrivateMessagesRead(currentUser.getId(), otherUserId);
        return ApiResponse.ok(buildConversation(currentUser, otherUser));
    }

    @PostMapping("/{receiverId}")
    public ApiResponse<MessageConversation> send(@RequestHeader(value = "X-Token", required = false) String token,
                                                  @PathVariable Long receiverId,
                                                  @RequestBody PrivateMessageRequest request) {
        User sender = authService.requireUser(token);
        User receiver = requireOtherUser(sender, receiverId);
        String content = request == null || request.getContent() == null ? "" : request.getContent().trim();
        if (content.isEmpty()) throw new BusinessException("请输入私信内容");
        if (content.length() > 1000) throw new BusinessException("私信不能超过 1000 个字符");
        boolean mutual = isMutual(sender.getId(), receiverId);
        int sentCount = repository.countPrivateMessagesSent(sender.getId(), receiverId);
        if (!mutual && sentCount >= NON_MUTUAL_LIMIT) {
            throw new BusinessException("非互关用户最多只能发送 3 条私信");
        }

        PrivateMessage message = new PrivateMessage();
        message.setSenderId(sender.getId());
        message.setReceiverId(receiverId);
        message.setContent(content);
        message.setReadFlag(false);
        repository.savePrivateMessage(message);
        notifyReceiver(sender, receiver);
        return ApiResponse.ok("私信已发送", buildConversation(sender, receiver));
    }

    private User requireOtherUser(User currentUser, Long otherUserId) {
        if (currentUser.getId().equals(otherUserId)) throw new BusinessException("不能给自己发送私信");
        User otherUser = repository.findUserById(otherUserId);
        if (otherUser == null) throw new BusinessException("用户不存在");
        if (otherUser.isBanned()) throw new BusinessException("该用户当前无法接收私信");
        return otherUser;
    }

    private boolean isMutual(Long firstUserId, Long secondUserId) {
        return repository.hasUserFollow(firstUserId, secondUserId) &&
                repository.hasUserFollow(secondUserId, firstUserId);
    }

    private MessageConversation buildConversation(User currentUser, User otherUser) {
        boolean mutual = isMutual(currentUser.getId(), otherUser.getId());
        int sentCount = repository.countPrivateMessagesSent(currentUser.getId(), otherUser.getId());
        MessageConversation conversation = new MessageConversation();
        conversation.setOtherUserId(otherUser.getId());
        conversation.setOtherUsername(otherUser.getUsername());
        conversation.setOtherNickname(otherUser.getNickname());
        conversation.setMutualFollow(mutual);
        conversation.setSentCount(sentCount);
        conversation.setRemainingMessageCount(mutual ? -1 : Math.max(0, NON_MUTUAL_LIMIT - sentCount));
        conversation.setMessages(repository.listPrivateMessages(currentUser.getId(), otherUser.getId()));
        return conversation;
    }

    private void notifyReceiver(User sender, User receiver) {
        Notification notification = new Notification();
        notification.setUserId(receiver.getId());
        notification.setActorUserId(sender.getId());
        notification.setActorUsername(sender.getUsername());
        notification.setType("PRIVATE_MESSAGE");
        notification.setTitle("收到新私信");
        notification.setContent(sender.getUsername() + " 给你发送了私信");
        notification.setLink("/messages.html?userId=" + sender.getId());
        notification.setReadFlag(false);
        repository.saveNotification(notification);
    }
}
