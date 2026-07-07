package com.tangyuxian.blog.service;

import com.tangyuxian.blog.common.BusinessException;
import com.tangyuxian.blog.dto.AuthRequest;
import com.tangyuxian.blog.dto.LoginResponse;
import com.tangyuxian.blog.dto.RegisterRequest;
import com.tangyuxian.blog.model.Role;
import com.tangyuxian.blog.model.User;
import com.tangyuxian.blog.repository.InMemoryBlogRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class AuthService {
    private final InMemoryBlogRepository repository;
    private final ConcurrentMap<String, Long> tokenStore = new ConcurrentHashMap<String, Long>();

    public AuthService(InMemoryBlogRepository repository) {
        this.repository = repository;
    }

    public User register(RegisterRequest request) {
        requireText(request.getUsername(), "请输入用户名");
        requireText(request.getPassword(), "请输入密码");
        if (repository.findUserByUsername(request.getUsername()) != null) {
            throw new BusinessException("用户名已存在");
        }
        String nickname = request.getNickname() == null || request.getNickname().trim().isEmpty()
                ? request.getUsername().trim()
                : request.getNickname().trim();
        return repository.saveUser(new User(null, request.getUsername().trim(), request.getPassword(), nickname, Role.USER, LocalDateTime.now()));
    }

    public LoginResponse login(AuthRequest request) {
        requireText(request.getUsername(), "请输入用户名");
        requireText(request.getPassword(), "请输入密码");
        User user = repository.findUserByUsername(request.getUsername().trim());
        if (user == null || !user.getPassword().equals(request.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        if (user.isBanned()) {
            throw new BusinessException("该账号已被封禁，无法登录");
        }
        String token = UUID.randomUUID().toString();
        tokenStore.put(token, user.getId());
        return new LoginResponse(token, user);
    }

    public User requireUser(String token) {
        if (token == null || token.trim().isEmpty()) throw new BusinessException("请先登录");
        Long userId = tokenStore.get(token.trim());
        if (userId == null) throw new BusinessException("登录已失效，请重新登录");
        User user = repository.findUserById(userId);
        if (user == null) throw new BusinessException("用户不存在");
        if (user.isBanned()) throw new BusinessException("该账号已被封禁，无法继续操作");
        return user;
    }

    public User requireAdmin(String token) {
        User user = requireUser(token);
        if (user.getRole() != Role.ADMIN) throw new BusinessException("需要管理员权限");
        return user;
    }

    private void requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) throw new BusinessException(message);
    }
}