package com.tangyuxian.blog.service;

import com.tangyuxian.blog.common.BusinessException;
import com.tangyuxian.blog.dto.MomentRequest;
import com.tangyuxian.blog.model.Moment;
import com.tangyuxian.blog.model.User;
import com.tangyuxian.blog.repository.MomentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class MomentService {
    public static final int MAX_CONTENT_LENGTH = 2000;
    public static final int MAX_IMAGES_PER_MOMENT = 9;
    private static final int MAX_IMAGE_DATA_LENGTH = 9 * 1024 * 1024;

    private final MomentRepository repository;

    public MomentService(MomentRepository repository) {
        this.repository = repository;
    }

    public List<Moment> list() {
        return repository.listAll();
    }

    @Transactional
    public Moment create(User author, MomentRequest request) {
        Moment moment = new Moment();
        moment.setAuthorId(author.getId());
        apply(moment, request);
        return repository.insert(moment);
    }

    @Transactional
    public Moment update(Long id, MomentRequest request) {
        Moment moment = requireMoment(id);
        apply(moment, request);
        return repository.update(moment);
    }

    public void delete(Long id) {
        requireMoment(id);
        repository.delete(id);
    }

    private Moment requireMoment(Long id) {
        Moment moment = repository.findById(id);
        if (moment == null) throw new BusinessException("动态不存在或已被删除");
        return moment;
    }

    private void apply(Moment moment, MomentRequest request) {
        if (request == null) throw new BusinessException("请填写动态内容");
        String content = request.getContent() == null ? "" : request.getContent().trim();
        List<String> images = sanitizeImages(request.getImages());
        if (content.isEmpty() && images.isEmpty()) {
            throw new BusinessException("请填写文字内容，或至少上传一张图片");
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new BusinessException("动态内容不能超过 " + MAX_CONTENT_LENGTH + " 个字");
        }
        moment.setContent(content);
        moment.setImages(images);
    }

    private List<String> sanitizeImages(List<String> images) {
        List<String> result = new ArrayList<String>();
        if (images == null) return result;
        for (String raw : images) {
            String image = raw == null ? "" : raw.trim();
            if (image.isEmpty()) continue;
            if (!isSupportedImage(image)) throw new BusinessException("仅支持 JPG、PNG、WebP 或 GIF 图片");
            if (image.length() > MAX_IMAGE_DATA_LENGTH) throw new BusinessException("单张图片过大，请压缩到 6MB 以内");
            result.add(image);
            if (result.size() > MAX_IMAGES_PER_MOMENT) {
                throw new BusinessException("每条动态最多上传 " + MAX_IMAGES_PER_MOMENT + " 张图片");
            }
        }
        return result;
    }

    private boolean isSupportedImage(String image) {
        String value = image.toLowerCase();
        return value.startsWith("data:image/jpeg;base64,") ||
                value.startsWith("data:image/png;base64,") ||
                value.startsWith("data:image/webp;base64,") ||
                value.startsWith("data:image/gif;base64,");
    }
}
