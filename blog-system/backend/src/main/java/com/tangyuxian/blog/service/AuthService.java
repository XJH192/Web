package com.tangyuxian.blog.service;

import com.tangyuxian.blog.common.BusinessException;
import com.tangyuxian.blog.dto.AuthRequest;
import com.tangyuxian.blog.dto.LoginResponse;
import com.tangyuxian.blog.dto.RegisterRequest;
import com.tangyuxian.blog.model.Role;
import com.tangyuxian.blog.model.User;
import com.tangyuxian.blog.repository.GalleryPhotoRepository;
import com.tangyuxian.blog.repository.InMemoryBlogRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class AuthService {
    private final InMemoryBlogRepository repository;
    private final GalleryPhotoRepository galleryPhotoRepository;
    private final ConcurrentMap<String, Long> tokenStore = new ConcurrentHashMap<String, Long>();

    public AuthService(InMemoryBlogRepository repository, GalleryPhotoRepository galleryPhotoRepository) {
        this.repository = repository;
        this.galleryPhotoRepository = galleryPhotoRepository;
    }

    public User register(RegisterRequest request) {
        requireText(request.getUsername(), "请输入用户名");
        requireText(request.getPassword(), "请输入密码");
        requireText(request.getEmail(), "请输入邮箱");
        String username = request.getUsername().trim();
        if (username.length() > 50) throw new BusinessException("用户名不能超过 50 个字符");
        if (repository.findUserByUsername(username) != null) {
            throw new BusinessException("该用户名已被使用，请换一个用户名");
        }
        String email = request.getEmail().trim();
        if (!email.contains("@")) throw new BusinessException("请输入正确的邮箱");
        String nickname = username;
        User user = new User(null, username, request.getPassword(), nickname, Role.USER, LocalDateTime.now());
        user.setEmail(email);
        try {
            User saved = repository.saveUser(user);
            galleryPhotoRepository.ensureInitialized(saved.getId());
            return saved;
        } catch (DuplicateKeyException ex) {
            throw new BusinessException("该用户名已被使用，请换一个用户名");
        }
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

    public User optionalUser(String token) {
        if (token == null || token.trim().isEmpty()) return null;
        return requireUser(token);
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
