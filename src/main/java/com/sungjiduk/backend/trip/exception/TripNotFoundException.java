package com.sungjiduk.backend.trip.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** 존재하지 않는 여행 일정 접근 시. 전역 핸들러 없이 404로 응답하도록 @ResponseStatus 사용. */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class TripNotFoundException extends RuntimeException {

    public TripNotFoundException(Long tripId) {
        super("여행 일정을 찾을 수 없습니다: " + tripId);
    }
}
