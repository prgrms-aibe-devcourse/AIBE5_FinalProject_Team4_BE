package com.closetnangam.be.domain.clothes.scoring;

import com.closetnangam.be.domain.catalog.enums.ClothesColor;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * 이미지("옷 색깔 조합 3초 코디") 기반 색상 어울림 점수표.
 *
 * <p>score(anchor, candidate) 방향으로 조회합니다.
 * <ul>
 *   <li>GOOD : 1.0 — 잘 어울림</li>
 *   <li>SOSO : 0.5 — 무난함</li>
 *   <li>SAME : 0.7 — 동일 색상(모노톤 코디)</li>
 *   <li>DEFAULT : 0.1 — 차트에 없는 조합</li>
 * </ul>
 *
 * <p>WHITE·BLACK은 모든 색상과 GOOD.
 * 나머지 11개 색상은 차트 방향 그대로 비대칭 테이블로 관리합니다.
 * (예: ORANGE→BROWN=GOOD, BROWN→ORANGE=SOSO)
 */
public final class ColorCompatibilityTable {

    public static final double GOOD    = 1.0;
    public static final double SOSO    = 0.5;
    public static final double SAME    = 0.7;
    public static final double DEFAULT = 0.1;

    private static final Map<String, Map<String, Double>> TABLE;

    static {
        TABLE = new HashMap<>();
        for (ClothesColor c : ClothesColor.values()) {
            TABLE.put(c.name(), new HashMap<>());
        }

        // ── PINK anchor ────────────────────────────────────────────
        set("PINK", "WHITE",      GOOD);
        set("PINK", "GRAY",       GOOD);
        set("PINK", "BEIGE",      GOOD);
        set("PINK", "RED",        GOOD);
        set("PINK", "ORANGE",     GOOD);
        set("PINK", "BLACK",      GOOD);
        set("PINK", "LIGHT_BLUE", SOSO);
        set("PINK", "NAVY",       SOSO);

        // ── RED anchor ─────────────────────────────────────────────
        set("RED", "WHITE",      GOOD);
        set("RED", "PINK",       GOOD);
        set("RED", "BEIGE",      GOOD);
        set("RED", "BROWN",      GOOD);
        set("RED", "GRAY",       GOOD);
        set("RED", "BLACK",      GOOD);
        set("RED", "LIGHT_BLUE", SOSO);

        // ── ORANGE anchor ──────────────────────────────────────────
        set("ORANGE", "GREEN",      GOOD);
        set("ORANGE", "WHITE",      GOOD);
        set("ORANGE", "BROWN",      GOOD);
        set("ORANGE", "BEIGE",      GOOD);
        set("ORANGE", "BLACK",      GOOD);
        set("ORANGE", "YELLOW",     SOSO);
        set("ORANGE", "LIGHT_BLUE", SOSO);
        set("ORANGE", "GRAY",       SOSO);

        // ── BEIGE anchor ───────────────────────────────────────────
        set("BEIGE", "LIGHT_BLUE", GOOD);
        set("BEIGE", "WHITE",      GOOD);
        set("BEIGE", "BROWN",      GOOD);
        set("BEIGE", "NAVY",       GOOD);
        set("BEIGE", "BLACK",      GOOD);
        set("BEIGE", "ORANGE",     SOSO);
        set("BEIGE", "YELLOW",     SOSO);
        set("BEIGE", "GREEN",      SOSO);

        // ── YELLOW anchor ──────────────────────────────────────────
        set("YELLOW", "WHITE", GOOD);
        set("YELLOW", "GRAY",  GOOD);
        set("YELLOW", "BLACK", GOOD);
        set("YELLOW", "GREEN", SOSO);
        set("YELLOW", "BROWN", SOSO);

        // ── GREEN anchor ───────────────────────────────────────────
        set("GREEN", "WHITE",      GOOD);
        set("GREEN", "BROWN",      GOOD);
        set("GREEN", "BEIGE",      GOOD);
        set("GREEN", "GRAY",       GOOD);
        set("GREEN", "RED",        GOOD);
        set("GREEN", "BLACK",      GOOD);
        set("GREEN", "LIGHT_BLUE", SOSO);
        set("GREEN", "YELLOW",     SOSO);

        // ── LIGHT_BLUE anchor ──────────────────────────────────────
        set("LIGHT_BLUE", "BEIGE",  GOOD);
        set("LIGHT_BLUE", "WHITE",  GOOD);
        set("LIGHT_BLUE", "BROWN",  GOOD);
        set("LIGHT_BLUE", "GRAY",   GOOD);
        set("LIGHT_BLUE", "NAVY",   GOOD);
        set("LIGHT_BLUE", "BLACK",  GOOD);
        set("LIGHT_BLUE", "ORANGE", SOSO);
        set("LIGHT_BLUE", "PINK",   SOSO);

        // ── NAVY anchor ────────────────────────────────────────────
        set("NAVY", "WHITE",      GOOD);
        set("NAVY", "BROWN",      GOOD);
        set("NAVY", "LIGHT_BLUE", GOOD);
        set("NAVY", "GRAY",       GOOD);
        set("NAVY", "BLACK",      GOOD);
        set("NAVY", "YELLOW",     SOSO);
        set("NAVY", "PINK",       SOSO);

        // ── PURPLE anchor ──────────────────────────────────────────
        set("PURPLE", "GRAY",       GOOD);
        set("PURPLE", "WHITE",      GOOD);
        set("PURPLE", "BLACK",      GOOD);
        set("PURPLE", "LIGHT_BLUE", SOSO);

        // ── BROWN anchor ───────────────────────────────────────────
        set("BROWN", "BEIGE",      GOOD);
        set("BROWN", "WHITE",      GOOD);
        set("BROWN", "LIGHT_BLUE", GOOD);
        set("BROWN", "BLACK",      GOOD);
        set("BROWN", "NAVY",       GOOD);
        set("BROWN", "ORANGE",     SOSO);
        set("BROWN", "PINK",       SOSO);

        // ── GRAY anchor ────────────────────────────────────────────
        set("GRAY", "BLACK",      GOOD);
        set("GRAY", "WHITE",      GOOD);
        set("GRAY", "LIGHT_BLUE", GOOD);
        set("GRAY", "PINK",       GOOD);
        set("GRAY", "RED",        GOOD);
        set("GRAY", "NAVY",       GOOD);
        set("GRAY", "PURPLE",     SOSO);

        // ── WHITE·BLACK anchor — 모든 색상과 GOOD ──────────────────
        for (ClothesColor c : ClothesColor.values()) {
            TABLE.get("WHITE").putIfAbsent(c.name(), GOOD);
            TABLE.get("BLACK").putIfAbsent(c.name(), GOOD);
        }
    }

    private ColorCompatibilityTable() {}

    /**
     * 비대칭 테이블이므로 {@code colorA→colorB}, {@code colorB→colorA} 양방향 중 더 높은 점수를 반환합니다.
     */
    public static double bestHarmony(String colorA, String colorB) {
        return Math.max(score(colorA, colorB), score(colorB, colorA));
    }

    /**
     * anchorColor 기준으로 candidateColor의 어울림 점수를 반환합니다.
     *
     * @param anchorColor    기준 옷의 색상 코드 (예: "NAVY")
     * @param candidateColor 후보 옷의 색상 코드 (예: "WHITE")
     * @return 0.0 ~ 1.0 어울림 점수
     */
    public static double score(String anchorColor, String candidateColor) {
        if (anchorColor == null || candidateColor == null) {
            return 0.5;
        }
        if (anchorColor.equals(candidateColor)) {
            return SAME;
        }
        return TABLE
                .getOrDefault(anchorColor, Collections.emptyMap())
                .getOrDefault(candidateColor, DEFAULT);
    }

    private static void set(String from, String to, double score) {
        TABLE.get(from).put(to, score);
    }
}
