package com.yiweilai.DreamArchive.DTO;

import java.util.List;

public class PagedDreamContentResponse {
    private List<DreamContent> items;
    private long total;
    private int page;
    private int pageSize;
    private int totalPages;

    public PagedDreamContentResponse() {}

    public PagedDreamContentResponse(List<DreamContent> items, long total, int page, int pageSize, int totalPages) {
        this.items = items;
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
        this.totalPages = totalPages;
    }

    public List<DreamContent> getItems() {
        return items;
    }

    public void setItems(List<DreamContent> items) {
        this.items = items;
    }

    public long getTotal() {
        return total;
    }

    public void setTotal(long total) {
        this.total = total;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
}
