package com.sungjiduk.backend.auth.controller;

import com.sungjiduk.backend.auth.dto.request.LoginRequest;
import com.sungjiduk.backend.auth.dto.request.SignupRequest;
import com.sungjiduk.backend.auth.dto.response.LoginResponse;
import com.sungjiduk.backend.auth.dto.response.UserSummaryResponse;
import com.sungjiduk.backend.auth.service.AuthService;
import com.sungjiduk.backend.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ApiResponse<UserSummaryResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ApiResponse.ok(authService.signup(request));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(authService.login(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        authService.logout();
        return ApiResponse.ok();
    }

    @GetMapping("/me")
    public ApiResponse<UserSummaryResponse> me() {
        return ApiResponse.ok(authService.me());
    }
}
