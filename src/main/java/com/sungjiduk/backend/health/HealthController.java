package com.sungjiduk.backend.health;

import com.sungjiduk.backend.common.api.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public ApiResponse<HealthResponse> health() {
        return ApiResponse.ok(new HealthResponse("UP", OffsetDateTime.now()));
    }

    public record HealthResponse(String status, OffsetDateTime checkedAt) {
    }
}
