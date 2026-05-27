package com.closetnangam.be.global.external.weather.service;

import java.util.HashMap;
import java.util.Map;

public class WeatherCoordinateConverter {
    public static Map<String, Integer> convertGrid(double lat, double lon) {
        double RE = 6371.00877; // 지구 반지름(km)
        double GRID = 5.0; // 격자 간격(km)
        double SLAT1 = 30.0; // 투영 위도1(degree)
        double SLAT2 = 60.0; // 투영 위도2(degree)
        double OLON = 126.0; // 기준점 경도(degree)
        double OLAT = 38.0; // 기준점 위도(degree)
        double XO = 43; // 기준점 X좌표(GRID)
        double YO = 136; // 기준점 Y좌표(GRID)

        double DEGRAD = Math.PI / 180.0;

        double re = RE / GRID;
        double sn = Math.tan(Math.PI * 0.25 + SLAT2 * DEGRAD * 0.5) / Math.tan(Math.PI * 0.25 + SLAT1 * DEGRAD * 0.5);
        sn = Math.log(Math.cos(SLAT1 * DEGRAD) / Math.cos(SLAT2 * DEGRAD)) / Math.log(sn);
        double sf = Math.tan(Math.PI * 0.25 + SLAT1 * DEGRAD * 0.5);
        sf = Math.pow(sf, sn) * Math.cos(SLAT1 * DEGRAD) / sn;
        double ro = Math.tan(Math.PI * 0.25 + OLAT * DEGRAD * 0.5);
        ro = re * sf / Math.pow(ro, sn);

        Map<String, Integer> map = new HashMap<>();
        double ra = Math.tan(Math.PI * 0.25 + (lat) * DEGRAD * 0.5);
        ra = re * sf / Math.pow(ra, sn);
        double theta = lon * DEGRAD - OLON * DEGRAD;
        if (theta > Math.PI) theta -= 2.0 * Math.PI;
        if (theta < -Math.PI) theta += 2.0 * Math.PI;
        theta *= sn;

        map.put("nx", (int) Math.floor(ra * Math.sin(theta) + XO + 0.5));
        map.put("ny", (int) Math.floor(ro - ra * Math.cos(theta) + YO + 0.5));

        return map;
    }
}