package com.closetnangam.be.domain.wardrobe.controller;

import com.closetnangam.be.domain.wardrobe.dto.response.WardrobeResponse;
import com.closetnangam.be.domain.wardrobe.service.WardrobeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Wardrobe", description = "옷장 API")
@RestController
@RequestMapping("/api/wardrobes")
@RequiredArgsConstructor
public class WardrobeController {

    private final WardrobeService wardrobeService;

    @Operation(summary = "회원 옷장 조회", description = "회원당 1개의 옷장을 조회합니다.")
    @GetMapping("/users/{userId}")
    public WardrobeResponse getWardrobeByUserId(@PathVariable Long userId) {
        return wardrobeService.getWardrobeByUserId(userId);
    }

    @Operation(summary = "회원 옷장 생성", description = "회원 가입 후 옷장을 1회 생성합니다.")
    @PostMapping("/users/{userId}")
    @ResponseStatus(HttpStatus.CREATED)
    public WardrobeResponse createWardrobe(@PathVariable Long userId) {
        return wardrobeService.createWardrobe(userId);
    }
}
