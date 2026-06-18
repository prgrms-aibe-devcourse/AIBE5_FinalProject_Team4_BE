package com.closetnangam.be.global.external.weather.service;


import com.closetnangam.be.global.external.weather.Enum.RegionGrid;
import com.closetnangam.be.global.external.weather.dto.WeatherInfoResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class WeatherService {

    @Value("${weather.api.url}")
    private String apiUrl;

    @Value("${weather.api.key}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<WeatherInfoResponse> getWeatherByRegion(String regionName) {
        // 1. 텍스트(ex: "대전광역시")를 매핑된 기상청 격자 좌표로 스왑 (No-DB)
        RegionGrid grid = RegionGrid.fromString(regionName);
        int nx = grid.getNx();
        int ny = grid.getNy();

        // 2. 시간 계산 (매시각 45분 전이면 이전 시간 정각 데이터 호출)
        LocalDateTime now = LocalDateTime.now();
        if (now.getMinute() < 45) {
            now = now.minusHours(1);
        }
        String baseDate = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String baseTime = now.format(DateTimeFormatter.ofPattern("HH00"));

        URI uri = UriComponentsBuilder.fromHttpUrl(apiUrl)
                .queryParam("pageNo", "1")
                .queryParam("numOfRows", "60")
                .queryParam("dataType", "JSON")
                .queryParam("base_date", baseDate)
                .queryParam("base_time", baseTime)
                .queryParam("nx", nx)
                .queryParam("ny", ny)
                .queryParam("serviceKey", apiKey)
                .build(true)
                .toUri();

        String jsonResult = restTemplate.getForObject(uri, String.class);
        return parseWeatherData(jsonResult);
    }

    /**
     * 기상청 Raw JSON을 프론트엔드가 쓰기 좋은 DTO 포맷 리스트로 파싱하는 메서드
     */
    private List<WeatherInfoResponse> parseWeatherData(String jsonResult) {
        List<WeatherInfoResponse> processedList = new ArrayList<>();

        try {
            // 1. JSON의 response -> body -> items -> item 배열 접근
            JsonNode root = objectMapper.readTree(jsonResult);
            JsonNode itemArray = root.path("response").path("body").path("items").path("item");

            // 시간순 정렬을 위해 TreeMap 사용 (Key: fcstTime, Value: 카테고리별 데이터 맵)
            Map<String, Map<String, String>> timeDataMap = new TreeMap<>();

            // 2. item 배열을 순회하며 필요한 카테고리 추출
            for (JsonNode item : itemArray) {
                String fcstTime = item.path("fcstTime").asText();     // 예보시간 (ex: 1300)
                String category = item.path("category").asText();     // 요소 (ex: T1H, SKY)
                String fcstValue = item.path("fcstValue").asText();   // 예보값 (ex: 28, 3)

                // 우리에게 필요한 날씨 항목만 필터링해서 맵에 누적
                if (List.of("T1H", "SKY", "REH", "RN1").contains(category)) {
                    timeDataMap.computeIfAbsent(fcstTime, k -> new HashMap<>()).put(category, fcstValue);
                }
            }

            // 3. 시간별로 모인 데이터를 DTO 객체로 조립
            for (Map.Entry<String, Map<String, String>> entry : timeDataMap.entrySet()) {
                String rawTime = entry.getKey(); // "1300"
                Map<String, String> values = entry.getValue();

                // SKY 코드 변환 (1: 맑음, 3: 구름많음, 4: 흐림)
                String skyStatus = switch (values.getOrDefault("SKY", "1")) {
                    case "1" -> "☀️ 맑음";
                    case "3" -> "⛅ 구름많음";
                    case "4" -> "☁️ 흐림";
                    default -> "☀️ 맑음";
                };

                // DTO 빌더 조립
                WeatherInfoResponse dto = WeatherInfoResponse.builder()
                        .fcstTime(rawTime.substring(0, 2) + "시") // "1300" -> "13시"
                        .temp(values.getOrDefault("T1H", "0") + "°C")
                        .sky(skyStatus)
                        .humidity(values.getOrDefault("REH", "0") + "%")
                        .rain(values.getOrDefault("RN1", "강수없음"))
                        .build();

                processedList.add(dto);
            }

        } catch (Exception e) {
            throw new RuntimeException("기상청 날씨 JSON 데이터를 파싱하는 데 실패했습니다.", e);
        }

        return processedList;
    }
}