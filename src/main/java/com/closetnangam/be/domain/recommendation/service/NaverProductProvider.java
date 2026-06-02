//package com.closetnangam.be.domain.recommendation.service;
//
//import com.closetnangam.be.domain.recommendation.dto.response.RecommendResponse;
//import com.closetnangam.be.global.external.naver.service.NaverApiService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Component;
//
//import java.util.List;
//import java.util.stream.Collectors;
//
//// 1. 네가 만든 클래스 (나중에 실제 로직을 여기에 채워!)
//@Component
//@RequiredArgsConstructor
//public class NaverProductProvider {
//
//    // 외부 API 서비스 (이미 네 팀에 존재하겠지?)
//    private final NaverApiService naverApiService;
//
//    public List<RecommendResponse> fetchProducts(String query) {
//        // 여기에 네이버 API 호출 로직을 구현하면 돼!
//        return naverApiService.searchItems(query).stream()
//                .map(item -> new RecommendResponse(
//                        item.getTitle(),
//                        item.getLink(),
//                        item.getImage(),
//                        item.getLprice()))
//                .collect(Collectors.toList());
//    }
//}