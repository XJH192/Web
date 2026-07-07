package com.tangyuxian.blog.service;

import com.tangyuxian.blog.common.BusinessException;
import com.tangyuxian.blog.dto.CategoryRequest;
import com.tangyuxian.blog.dto.TagRequest;
import com.tangyuxian.blog.model.Article;
import com.tangyuxian.blog.model.Category;
import com.tangyuxian.blog.model.Tag;
import com.tangyuxian.blog.repository.InMemoryBlogRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TaxonomyService {
    private final InMemoryBlogRepository repository;

    public TaxonomyService(InMemoryBlogRepository repository) {
        this.repository = repository;
    }

    public List<Category> listCategories() { return repository.listCategories(); }
    public List<Tag> listTags() { return repository.listTags(); }

    public Category createCategory(CategoryRequest request) {
        requireText(request.getName(), "\u8bf7\u8f93\u5165\u5206\u7c7b\u540d\u79f0");
        return repository.saveCategory(new Category(null, request.getName().trim(), request.getDescription(), LocalDateTime.now()));
    }

    public Category updateCategory(Long id, CategoryRequest request) {
        Category category = repository.findCategoryById(id);
        if (category == null) throw new BusinessException("\u5206\u7c7b\u4e0d\u5b58\u5728");
        requireText(request.getName(), "\u8bf7\u8f93\u5165\u5206\u7c7b\u540d\u79f0");
        category.setName(request.getName().trim());
        category.setDescription(request.getDescription());
        return repository.saveCategory(category);
    }

    public void deleteCategory(Long id) {
        if (repository.findCategoryById(id) == null) throw new BusinessException("\u5206\u7c7b\u4e0d\u5b58\u5728");
        for (Article article : repository.listArticles()) {
            if (id.equals(article.getCategoryId())) throw new BusinessException("\u5206\u7c7b\u5df2\u88ab\u6587\u7ae0\u4f7f\u7528\uff0c\u4e0d\u80fd\u5220\u9664");
        }
        repository.deleteCategory(id);
    }

    public Tag createTag(TagRequest request) {
        requireText(request.getName(), "\u8bf7\u8f93\u5165\u6807\u7b7e\u540d\u79f0");
        return repository.saveTag(new Tag(null, request.getName().trim(), LocalDateTime.now()));
    }

    public Tag updateTag(Long id, TagRequest request) {
        Tag tag = repository.findTagById(id);
        if (tag == null) throw new BusinessException("\u6807\u7b7e\u4e0d\u5b58\u5728");
        requireText(request.getName(), "\u8bf7\u8f93\u5165\u6807\u7b7e\u540d\u79f0");
        tag.setName(request.getName().trim());
        return repository.saveTag(tag);
    }

    public void deleteTag(Long id) {
        if (repository.findTagById(id) == null) throw new BusinessException("\u6807\u7b7e\u4e0d\u5b58\u5728");
        for (Article article : repository.listArticles()) {
            if (article.getTagIds() != null && article.getTagIds().contains(id)) {
                throw new BusinessException("\u6807\u7b7e\u5df2\u88ab\u6587\u7ae0\u4f7f\u7528\uff0c\u4e0d\u80fd\u5220\u9664");
            }
        }
        repository.deleteTag(id);
    }

    private void requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) throw new BusinessException(message);
    }
}