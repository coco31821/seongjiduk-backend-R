package com.sungjiduk.backend.event.entity;

/**
 * 접속/이용 이벤트 종류. 관리자 통계(접속자/이용)의 원천.
 * 정의: 기획/07_관리자_통계_설계.md "수집 이벤트".
 */
public enum EventType {
    PAGE_VIEW,
    SIGNUP,
    LOGIN,
    CONTENT_SELECTED,
    SPOT_VIEWED,
    TRIP_GENERATED,
    TRIP_REGENERATED,
    TRIP_SAVED,
    TRIP_SHARED,
    VISIT_CREATED,
    SPOT_REPORTED
}
