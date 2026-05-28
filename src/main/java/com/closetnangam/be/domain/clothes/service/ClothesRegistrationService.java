package com.closetnangam.be.domain.clothes.service;

import com.closetnangam.be.domain.clothes.constant.ClothesRegistrationMessages;
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
                ClothesRegistrationMessages.MODAL_TITLE,
                ClothesRegistrationMessages.MODAL_DESCRIPTION,
                Arrays.stream(ClothesRegistrationMethod.values())
                        .map(ClothesRegistrationMethodResponse::from)
                        .toList()
        );
    }
}
