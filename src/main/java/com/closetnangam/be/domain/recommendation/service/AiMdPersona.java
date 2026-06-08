package com.closetnangam.be.domain.recommendation.service;

import com.closetnangam.be.domain.recommendation.dto.response.AiMdPersonaResponse;
import com.closetnangam.be.domain.user.entity.User;

import java.util.Arrays;
import java.util.List;

public enum AiMdPersona {

    TAE_SIK(
            "taesik",
            "태식이",
            User.Gender.MALE,
            List.of("STREET", "CASUAL", "GORPCORE", "CHIC"),
            List.of("스트릿", "캐주얼", "고프코어", "시크"),
            "가볍고 친구같은 말투",
            "힘 빼고 멋내는 스트릿/캐주얼 코디를 잘 잡는 남자 MD"
    ),
    JUN_SIK(
            "junsik",
            "준식이",
            User.Gender.MALE,
            List.of("MINIMAL", "CLASSIC", "CITYBOY"),
            List.of("미니멀", "클래식", "시티보이"),
            "댄디한 말투",
            "정돈된 실루엣과 도시적인 무드를 좋아하는 남자 MD"
    ),
    SE_SOON(
            "sesoon",
            "세순이",
            User.Gender.FEMALE,
            List.of("MINIMAL", "CLASSIC", "CITYBOY"),
            List.of("미니멀", "클래식", "시티보이"),
            "댄디한 말투",
            "깔끔한 균형과 클래식한 조합을 제안하는 여자 MD"
    ),
    GA_HYUN(
            "gahyun",
            "가현이",
            User.Gender.FEMALE,
            List.of("CHIC", "GORPCORE", "STREET"),
            List.of("시크", "고프코어", "스트릿"),
            "성수동 느낌의 힙한 말투",
            "시크한 베이스에 고프코어와 스트릿 포인트를 섞는 여자 MD"
    ),
    SEONG_MI(
            "seongmi",
            "성미",
            User.Gender.FEMALE,
            List.of("CASUAL", "MINIMAL", "WORKWEAR"),
            List.of("캐주얼", "미니멀", "워크웨어"),
            "과하지 않게 애교 있는 말투",
            "편안하지만 정돈된 데일리 워크웨어 감성을 제안하는 여자 MD"
    );

    private final String id;
    private final String displayName;
    private final User.Gender gender;
    private final List<String> styleCodes;
    private final List<String> styleNames;
    private final String speechStyle;
    private final String description;

    AiMdPersona(
            String id,
            String displayName,
            User.Gender gender,
            List<String> styleCodes,
            List<String> styleNames,
            String speechStyle,
            String description
    ) {
        this.id = id;
        this.displayName = displayName;
        this.gender = gender;
        this.styleCodes = styleCodes;
        this.styleNames = styleNames;
        this.speechStyle = speechStyle;
        this.description = description;
    }

    public static AiMdPersona fromId(String id) {
        return Arrays.stream(values())
                .filter(persona -> persona.id.equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 AI MD입니다."));
    }

    public static List<AiMdPersonaResponse> responsesFor(User.Gender gender) {
        return Arrays.stream(values())
                .filter(persona -> persona.gender == gender)
                .map(AiMdPersona::toResponse)
                .toList();
    }

    public boolean supports(User.Gender userGender) {
        return gender == userGender;
    }

    public AiMdPersonaResponse toResponse() {
        return new AiMdPersonaResponse(id, displayName, gender.name(), styleCodes, styleNames, speechStyle, description);
    }

    public String id() {
        return id;
    }

    public String displayName() {
        return displayName;
    }

    public User.Gender gender() {
        return gender;
    }

    public List<String> styleCodes() {
        return styleCodes;
    }

    public List<String> styleNames() {
        return styleNames;
    }

    public String speechStyle() {
        return speechStyle;
    }

    public String description() {
        return description;
    }
}
