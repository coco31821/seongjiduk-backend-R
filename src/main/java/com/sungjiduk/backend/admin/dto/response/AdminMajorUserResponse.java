package com.sungjiduk.backend.admin.dto.response;

import java.util.List;

public record AdminMajorUserResponse(
    List<Long> values,
    List<String> dates
) {
}
