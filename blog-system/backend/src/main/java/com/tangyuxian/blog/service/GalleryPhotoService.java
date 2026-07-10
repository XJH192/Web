package com.tangyuxian.blog.service;

import com.tangyuxian.blog.common.BusinessException;
import com.tangyuxian.blog.dto.GalleryPhotoRequest;
import com.tangyuxian.blog.model.GalleryPhoto;
import com.tangyuxian.blog.model.User;
import com.tangyuxian.blog.repository.GalleryPhotoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GalleryPhotoService {
    public static final int MAX_PHOTOS_PER_USER = 8;
    private static final int MAX_IMAGE_DATA_LENGTH = 9 * 1024 * 1024;
    private final GalleryPhotoRepository repository;

    public GalleryPhotoService(GalleryPhotoRepository repository) {
        this.repository = repository;
    }

    public List<GalleryPhoto> list(User user) {
        return repository.listByOwner(user.getId());
    }

    public List<GalleryPhoto> listByOwner(Long ownerId) {
        return repository.listByOwner(ownerId);
    }

    @Transactional
    public synchronized GalleryPhoto create(User user, GalleryPhotoRequest request) {
        if (repository.countByOwner(user.getId()) >= MAX_PHOTOS_PER_USER) {
            throw new BusinessException("每个账号最多保存8张图片，请先删除一张再上传");
        }
        GalleryPhoto photo = new GalleryPhoto();
        photo.setOwnerId(user.getId());
        apply(photo, request, true);
        return repository.insert(photo);
    }

    public GalleryPhoto update(User user, Long id, GalleryPhotoRequest request) {
        GalleryPhoto photo = requirePhoto(id);
        requireManagePermission(user, photo);
        apply(photo, request, false);
        return repository.update(photo);
    }

    public void delete(User user, Long id) {
        GalleryPhoto photo = requirePhoto(id);
        requireManagePermission(user, photo);
        repository.delete(id);
    }

    private GalleryPhoto requirePhoto(Long id) {
        GalleryPhoto photo = repository.findById(id);
        if (photo == null) throw new BusinessException("相册图片不存在");
        return photo;
    }

    private void requireManagePermission(User user, GalleryPhoto photo) {
        if (!user.getId().equals(photo.getOwnerId())) {
            throw new BusinessException("只能修改或删除自己上传的图片");
        }
    }

    private void apply(GalleryPhoto photo, GalleryPhotoRequest request, boolean imageRequired) {
        if (request == null) throw new BusinessException("请填写图片信息");
        String title = trim(request.getTitle());
        String description = trim(request.getDescription());
        if (title.isEmpty()) throw new BusinessException("请输入图片标题");
        if (title.length() > 80) throw new BusinessException("图片标题不能超过80个字");
        if (description.length() > 500) throw new BusinessException("图片说明不能超过500个字");
        photo.setTitle(title);
        photo.setDescription(description);

        String image = trim(request.getImageDataUrl());
        if (image.isEmpty()) {
            if (imageRequired || photo.getImageDataUrl() == null) throw new BusinessException("请选择要上传的图片");
            return;
        }
        if (!isSupportedImage(image)) throw new BusinessException("仅支持 JPG、PNG、WebP 或 GIF 图片");
        if (image.length() > MAX_IMAGE_DATA_LENGTH) throw new BusinessException("图片过大，请压缩到6MB以内");
        photo.setImageDataUrl(image);
    }

    private boolean isSupportedImage(String image) {
        String value = image.toLowerCase();
        return value.startsWith("data:image/jpeg;base64,") ||
                value.startsWith("data:image/png;base64,") ||
                value.startsWith("data:image/webp;base64,") ||
                value.startsWith("data:image/gif;base64,");
    }

    private String trim(String value) {
        return value == null ? "" : value.trim();
    }
}
