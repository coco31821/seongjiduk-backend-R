package com.sungjiduk.backend.trip.infra;

/**
 * AI 생성 동시성 한도를 대기해도 자리를 못 얻었을 때. 호출부(TripService)가 로컬 폴백으로 처리하도록
 * RuntimeException으로 전파한다(ai-service 호출 실패와 동일 취급).
 */
public class AiBusyException extends RuntimeException {
    public AiBusyException(String message) {
        super(message);
    }
}
