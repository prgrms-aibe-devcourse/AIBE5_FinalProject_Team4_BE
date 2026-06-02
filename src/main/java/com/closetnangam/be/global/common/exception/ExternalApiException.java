package com.closetnangam.be.global.common.exception;

/**
 * 네이버 쇼핑, 날씨, AI처럼 애플리케이션 밖의 API 호출이 실패했을 때 사용하는 예외.
 *
 * 외부 API 장애는 클라이언트 요청 형식 오류나 내부 비즈니스 검증 실패와 성격이 다르다.
 * GlobalExceptionHandler에서 이 타입을 502 Bad Gateway로 변환해 프론트가 "외부 연동 실패"로
 * 구분해서 안내하거나 fallback UI를 띄울 수 있게 한다.
 */
public class ExternalApiException extends RuntimeException {

    public ExternalApiException(String message) {
        super(message);
    }

    public ExternalApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
