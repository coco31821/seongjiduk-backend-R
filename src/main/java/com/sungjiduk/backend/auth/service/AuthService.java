package com.sungjiduk.backend.auth.service;

import com.sungjiduk.backend.auth.dto.request.LoginRequest;
import com.sungjiduk.backend.auth.dto.request.SignupRequest;
import com.sungjiduk.backend.auth.dto.response.LoginResponse;
import com.sungjiduk.backend.auth.dto.response.UserSummaryResponse;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    public UserSummaryResponse signup(SignupRequest request) {
        return new UserSummaryResponse(1L, request.email(), request.nickname());
    }

    public LoginResponse login(LoginRequest request) {
        return new LoginResponse("mock-access-token", "Bearer", 3600);
    }

    public void logout() {
    }

    public UserSummaryResponse me() {
        return new UserSummaryResponse(1L, "fan@example.com", "muse_fan");
    }
}
