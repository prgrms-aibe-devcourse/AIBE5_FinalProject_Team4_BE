package com.closetnangam.be.global.external.weather.controller;



import com.closetnangam.be.global.external.weather.dto.WeatherInfoResponse;import com.closetnangam.be.global.external.weather.service.WeatherService;import org.springframework.web.bind.annotation.GetMapping;import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
public class WeatherController {

    private final WeatherService weatherService;

    // 생성자 주입
    public WeatherController(WeatherService weatherService) {
        this.weatherService = weatherService;
    }

    /**
     * 프론트엔드 HTML에서 선택한 지역명을 받아 시간별 날씨 예보 리스트를 JSON으로 반환합니다.
     * 예: GET http://localhost:8000/api/weather?region=대전광역시
     */
    @GetMapping("/api/weather")
    public List<WeatherInfoResponse> getWeather(@RequestParam String region) {
        // WeatherService 내부에서 RegionGrid를 통해 DB 없이 격자 좌표를 획득하고,
        // 기상청 API를 호출하여 최종 가공된 DTO 리스트를 반환합니다.
        return weatherService.getWeatherByRegion(region);
    }
    @GetMapping("/login")
    public String mockLoginPage() {
        // 이 한 줄이 실행되면 스프링이 뺑뺑이를 멈추고 글자를 화면에 띄웁니다.
        return "임시 로그인 페이지입니다. 무한 루프 탈출 성공!";
    }
}