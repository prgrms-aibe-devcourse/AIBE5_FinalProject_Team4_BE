package com.closetnangam.be.domain.clothes.enums;

/**
 * 옷에 부여된 색상 태그의 역할.
 *
 * <ul>
 *   <li>{@link #PRIMARY} — 가장 넓은 면적을 차지하는 대표 색상 (가중치 1.0).</li>
 *   <li>{@link #SECONDARY} — 포인트·보조 색상 (가중치 0.6).
 *       색상 어울림 점수 계산 시 양방향 최대 harmony 값에 가중치를 곱해 반영합니다.</li>
 * </ul>
 */
public enum ColorRole {

    PRIMARY(1.0),
    SECONDARY(0.6);

    private final double weight;

    ColorRole(double weight) {
        this.weight = weight;
    }

    public double getWeight() {
        return weight;
    }
}
