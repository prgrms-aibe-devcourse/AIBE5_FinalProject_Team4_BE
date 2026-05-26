package com.closetnangam.be.global.external.weather.dto;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WeatherInfoResponse {
    private String fcstTime;   // 예보 시각 (예: "13시")
    private String temp;       // 기온
    private String sky;        // 하늘 상태 (맑음 / 흐림 등)
    private String humidity;   // 습도
    private String rain;       // 강수량 정보
}