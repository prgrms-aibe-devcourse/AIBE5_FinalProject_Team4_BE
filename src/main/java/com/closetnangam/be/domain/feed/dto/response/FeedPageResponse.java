package com.closetnangam.be.domain.feed.dto.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record FeedPageResponse(
        List<FeedResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages,
        boolean hasNext
) {

    public static FeedPageResponse from(Page<FeedResponse> page) {
        return new FeedPageResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.hasNext()
        );
    }
}
