package com.closetnangam.be.domain.recommendation.dto.response;

import java.util.List;

/**
 * 사용자가 추천을 요청할 AI MD 선택지.
 *
 * MD는 사용자 성별과 관계없이 전체 목록으로 내려가며, 프론트는 id를 다음 추천 API 호출에 사용한다.
 * gender는 MD 페르소나 성별이며 추천 옷 성별 필터가 아니다.
 */
public record AiMdPersonaResponse(
        String id,
        String name,
        String gender,
        List<String> styleCodes,
        List<String> styleNames,
        String speechStyle,
        String description
) {
}
