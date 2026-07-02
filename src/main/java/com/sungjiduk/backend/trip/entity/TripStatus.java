package com.sungjiduk.backend.trip.entity;

/**
 * 여행 일정 상태.
 * DRAFT: AI가 막 생성한 임시 일정(저장 전, 비회원 포함)
 * SAVED: 회원이 마이페이지에 저장한 일정
 */
public enum TripStatus {
    DRAFT,
    SAVED
}
