package com.closetnangam.be.domain.recommendation.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FeedbackType {
    SAVED("저장하기"),
    DISLIKE("싫어요"),
    EXCLUDE("추천 제외");

    private final String description;
}
