package com.sungjiduk.backend.ailog.entity;

/** AI 호출 결과 — FALLBACK은 ai-service 실패로 로컬 배치를 쓴 경우. */
public enum AiRequestStatus {
    SUCCESS,
    FALLBACK
}
