package com.closetnangam.be.domain.clothes.service;

import com.closetnangam.be.domain.clothes.dto.response.ClothesRegistrationMethodResponse;
import com.closetnangam.be.domain.clothes.dto.response.ClothesRegistrationMethodsResponse;
import com.closetnangam.be.domain.clothes.enums.ClothesRegistrationMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

@Service
@Transactional(readOnly = true)
public class ClothesRegistrationService {

    public ClothesRegistrationMethodsResponse getRegistrationMethods() {
        return new ClothesRegistrationMethodsResponse(
                "등록 방식 선택",
                "원하는 등록 방식을 선택해 주세요.",
                Arrays.stream(ClothesRegistrationMethod.values())
                        .map(ClothesRegistrationMethodResponse::from)
                        .toList()
        );
    }
}
