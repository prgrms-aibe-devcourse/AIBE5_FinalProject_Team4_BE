package com.closetnangam.be.global.external.weather.Enum;

import java.util.Arrays;

public enum RegionGrid {
    SEOUL("서울특별시", 60, 127),
    BUSAN("부산광역시", 98, 76),
    DAEGU("대구광역시", 89, 90),
    INCHEON("인천광역시", 55, 124),
    GWANGJU("광주광역시", 58, 74),
    DAEJEON("대전광역시", 67, 100),
    ULSAN("울산광역시", 102, 84),
    SEJONG("세종특별자치시", 66, 103),
    GYEONGGI("경기도", 60, 120),
    GANGWON("강원특별자치도", 73, 134),
    CHUNGBUK("충청북도", 69, 107),
    CHUNGNAM("충청남도", 68, 100),
    JEONBUK("전라북도", 63, 89),
    JEONNAM("전라남도", 51, 67),
    GYEONGBUK("경상북도", 89, 91),
    GYEONGNAM("경상남도", 91, 77),
    JEJU("제주특별자치도", 52, 38);

    private final String regionName;
    private final int nx;
    private final int ny;

    RegionGrid(String regionName, int nx, int ny) {
        this.regionName = regionName;
        this.nx = nx;
        this.ny = ny;
    }

    public String getRegionName() { return regionName; }
    public int getNx() { return nx; }
    public int getNy() { return ny; }

    // 사용자가 입력한 문자열(ex: "대전광역시" 또는 "대전")에 맞는 Enum을 찾아주는 메서드
    public static RegionGrid fromString(String text) {
        return Arrays.stream(values())
                .filter(r -> text.contains(r.regionName) || r.regionName.contains(text))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("지원하지 않는 지역명입니다: " + text));
    }
}