package com.ganaderia4.backend.dto;

import org.springframework.data.domain.Page;

import java.util.List;

public class PagedResponseDTO<T> {

    private List<T> content;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
    private boolean empty;
    private int numberOfElements;

    public static <T> PagedResponseDTO<T> from(Page<T> source) {
        PagedResponseDTO<T> dto = new PagedResponseDTO<>();
        dto.setContent(source.getContent());
        dto.setPage(source.getNumber());
        dto.setSize(source.getSize());
        dto.setTotalElements(source.getTotalElements());
        dto.setTotalPages(source.getTotalPages());
        dto.setFirst(source.isFirst());
        dto.setLast(source.isLast());
        dto.setEmpty(source.isEmpty());
        dto.setNumberOfElements(source.getNumberOfElements());
        return dto;
    }

    public List<T> getContent() {
        return content;
    }

    public void setContent(List<T> content) {
        this.content = content;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public boolean isFirst() {
        return first;
    }

    public void setFirst(boolean first) {
        this.first = first;
    }

    public boolean isLast() {
        return last;
    }

    public void setLast(boolean last) {
        this.last = last;
    }

    public boolean isEmpty() {
        return empty;
    }

    public void setEmpty(boolean empty) {
        this.empty = empty;
    }

    public int getNumberOfElements() {
        return numberOfElements;
    }

    public void setNumberOfElements(int numberOfElements) {
        this.numberOfElements = numberOfElements;
    }
}
