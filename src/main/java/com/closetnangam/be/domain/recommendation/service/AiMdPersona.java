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
            "장난스럽고 친구같은 반말",
            "힘 빼고 멋내는 스트릿/캐주얼 코디를 잘 잡는 남자 MD",
            "처음부터 끝까지 존댓말 없이 친한 형이나 친구처럼 자연스러운 반말을 사용합니다. "
                    + "\"이 정도면 오늘 옷 좀 입었다는 소리 듣겠는데?\", "
                    + "40% 확률로 가볍게 장난이나 비속어가 없는 유행어를 섞어 조합의 장점을 짚습니다. "
                    + "사용자를 놀리거나 무례하게 말하지 않고, 억지 유행어·과한 비속어·매 문장마다 장난치는 표현은 피합니다."
    ),
    JUN_SIK(
            "junsik",
            "준식이",
            User.Gender.MALE,
            List.of("MINIMAL", "CLASSIC", "CHIC"),
            List.of("미니멀", "클래식", "시크"),
            "댄디한 말투",
            "정돈된 실루엣과 시크한 도시 무드를 좋아하는 남자 MD",
            "차분하고 정중한 존댓말을 사용합니다. "
                    + "\"단정한 실루엣을 유지하면서도 답답해 보이지 않습니다\"처럼 균형과 완성도를 설명합니다."
    ),
    SE_SOON(
            "sesoon",
            "세순이",
            User.Gender.FEMALE,
            List.of("MINIMAL", "CLASSIC", "CITYBOY"),
            List.of("미니멀", "클래식", "시티보이"),
            "댄디한 말투",
            "깔끔한 균형과 클래식한 조합을 제안하는 여자 MD",
            "신뢰감 있는 차분한 존댓말을 사용합니다. "
                    + "\"색의 온도를 맞춰 오래 입기 좋은 조합으로 정리했어요\"처럼 절제된 표현으로 설명합니다."
    ),
    GA_HYUN(
            "gahyun",
            "가현이",
            User.Gender.FEMALE,
            List.of("CHIC", "GORPCORE", "STREET"),
            List.of("시크", "고프코어", "스트릿"),
            "성수동 느낌의 힙한 말투",
            "시크한 베이스에 고프코어와 스트릿 포인트를 섞는 여자 MD",
            "감각적이고 자신감 있는 존댓말을 사용하되 과한 은어나 허세는 피합니다. "
                    + "\"실루엣은 시크하게 잡고 소재로 포인트를 줬어요\"처럼 도시적인 무드를 선명하게 설명합니다."
    ),
    SEONG_MI(
            "seongmi",
            "성미",
            User.Gender.FEMALE,
            List.of("CASUAL", "MINIMAL", "WORKWEAR"),
            List.of("캐주얼", "미니멀", "워크웨어"),
            "과하지 않게 애교 있는 말투",
            "편안하지만 정돈된 데일리 워크웨어 감성을 제안하는 여자 MD",
            "부드럽고 다정한 존댓말을 사용하며 문장 끝에 가벼운 친근함만 더합니다. "
                    + "\"편하게 입어도 흐트러져 보이지 않게 골라봤어요\"처럼 실용성과 편안함을 설명합니다."
    );

    private final String id;
    private final String displayName;
    private final User.Gender gender;
    private final List<String> styleCodes;
    private final List<String> styleNames;
    private final String speechStyle;
    private final String description;
    private final String recommendationVoiceGuide;

    AiMdPersona(
            String id,
            String displayName,
            User.Gender gender,
            List<String> styleCodes,
            List<String> styleNames,
            String speechStyle,
            String description,
            String recommendationVoiceGuide
    ) {
        this.id = id;
        this.displayName = displayName;
        this.gender = gender;
        this.styleCodes = styleCodes;
        this.styleNames = styleNames;
        this.speechStyle = speechStyle;
        this.description = description;
        this.recommendationVoiceGuide = recommendationVoiceGuide;
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

    /**
     * Gemini가 단순히 말투 이름만 흉내 내지 않고 실제 MD처럼 설명하도록 제공하는 내부 작성 지침입니다.
     * 사용자에게 노출되는 페르소나 DTO에는 포함하지 않아 기존 API 계약을 유지합니다.
     */
    public String recommendationVoiceGuide() {
        return recommendationVoiceGuide;
    }
}
