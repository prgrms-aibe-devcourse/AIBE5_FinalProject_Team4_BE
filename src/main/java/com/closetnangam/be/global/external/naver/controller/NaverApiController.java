package com.closetnangam.be.global.external.naver.controller;

import com.closetnangam.be.global.external.naver.service.NaverApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/naver")
@RequiredArgsConstructor
public class NaverApiController {

    private final NaverApiService naverApiService;

    @GetMapping("/search")
    public ResponseEntity<String> search(@RequestParam String keyword) {
        String result = naverApiService.searchShop(keyword);
        return ResponseEntity.ok(result);
    }
}