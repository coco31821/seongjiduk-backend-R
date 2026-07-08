package com.sungjiduk.backend.admin.dto.request;

public record AdminMissionUpdateRequest(String title, String description, String missionType, Boolean active) {
}
