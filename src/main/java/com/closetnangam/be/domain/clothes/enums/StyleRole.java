package com.closetnangam.be.domain.clothes.enums;

/**
 * 옷에 부여된 스타일 태그의 역할.
 *
 * <ul>
 *   <li>{@link #PRIMARY} — 해당 옷을 대표하는 메인 스타일. 추천 점수 계산 시 anchor 의 PRIMARY 스타일과
 *       후보 옷의 스타일 태그(PRIMARY·SECONDARY 모두 포함)를 비교합니다.</li>
 *   <li>{@link #SECONDARY} — 서브 스타일. PRIMARY 스타일 일치 여부 판단에 포함되지만 점수 계산의
 *       기준(anchor)으로는 사용되지 않습니다.</li>
 * </ul>
 */
public enum StyleRole {

    PRIMARY,
    SECONDARY
}
