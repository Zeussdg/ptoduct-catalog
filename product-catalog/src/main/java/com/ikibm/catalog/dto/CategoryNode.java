package com.ikibm.catalog.dto;

import java.util.ArrayList;
import java.util.List;

/** Kategori kenar çubuğu için ağaç düğümü (canlı ürün sayılarıyla). */
public class CategoryNode {
    private final String name;
    private final String slug;
    private long count;
    private final List<CategoryNode> children = new ArrayList<>();

    public CategoryNode(String name, String slug) {
        this.name = name;
        this.slug = slug;
    }

    public String getName() { return name; }
    public String getSlug() { return slug; }
    public long getCount() { return count; }
    public void setCount(long count) { this.count = count; }
    public List<CategoryNode> getChildren() { return children; }
    public void addChild(CategoryNode child) { children.add(child); }
}
