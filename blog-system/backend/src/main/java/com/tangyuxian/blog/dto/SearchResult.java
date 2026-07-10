package com.tangyuxian.blog.dto;

import com.tangyuxian.blog.model.Article;

import java.util.ArrayList;
import java.util.List;

public class SearchResult {
    private List<UserSearchItem> users = new ArrayList<UserSearchItem>();
    private List<Article> articles = new ArrayList<Article>();

    public List<UserSearchItem> getUsers() { return users; }
    public void setUsers(List<UserSearchItem> users) { this.users = users; }
    public List<Article> getArticles() { return articles; }
    public void setArticles(List<Article> articles) { this.articles = articles; }
}
