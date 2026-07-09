package com.sungjiduk.backend.auth.controller;

import com.sungjiduk.backend.auth.dto.request.LoginRequest;
import com.sungjiduk.backend.auth.dto.request.SignupRequest;
import com.sungjiduk.backend.auth.dto.response.TokenResponse;
import com.sungjiduk.backend.auth.dto.response.LoginResponse;
import com.sungjiduk.backend.auth.dto.response.MeResponse;
import com.sungjiduk.backend.auth.dto.response.UserSummaryResponse;
import com.sungjiduk.backend.auth.service.AuthService;
import com.sungjiduk.backend.common.api.ApiResponse;
import com.sungjiduk.backend.common.constants.SuccessCode;
import com.sungjiduk.backend.common.properties.JwtProperties;
import com.sungjiduk.backend.user.entity.CurrentUser;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController

@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtProperties jwtProperties;


    public AuthController(AuthService authService, JwtProperties jwtProperties) {
        this.authService = authService;
        this.jwtProperties = jwtProperties;
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<UserSummaryResponse>> signUp(
        @Valid @RequestBody SignupRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(
                ApiResponse.created(
                    authService.signup(request),
                    SuccessCode.USER_CREATED.getSuccessMessage()
                )
            );
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        ResponseCookie refreshTokenCookie = createRefreshTokenCookie(response.refreshToken());

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
            .body(ApiResponse.ok(response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
        @CookieValue("refreshToken") String refreshToken // 요청 쿠키에서 refreshToken 읽기
    ) {
        authService.logout(refreshToken);   // Redis 저장소에서 refreshToken 삭제

        // 브라우저에 다시 내려줄 쿠키를 만듦.(빈문자열)
        ResponseCookie expiredRefreshTokenCookie = ResponseCookie.from("refreshToken", "")
            .httpOnly(true) // JS에서 못읽게함.
            .secure(false)  // 운영때 true로 변경
            .path("/")  // 사이트 전체 경로에서 쿠키 적용 - 후에 변경
            .sameSite("Lax")    // 다른 사이트에서 온 요청에 쿠키를 자동으로 붙일지 정하는 옵션.
            .maxAge(0)  // 쿠키 바로 삭제 (수명 0초)
            .build();

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, expiredRefreshTokenCookie.toString())   // 브라우저의 refreshToken 쿠키도 삭제하도록 Set-Cookie 내려주기
            .body(ApiResponse.ok());
    }

    @GetMapping("/me")
    public ApiResponse<MeResponse> me(@AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.ok(authService.me(currentUser.getId()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<TokenResponse>> refreshToken(
        @CookieValue(name = "refreshToken", required = false) String refreshToken
    ) {
        TokenResponse response = authService.refresh(refreshToken);
        ResponseCookie refreshTokenCookie = createRefreshTokenCookie(response.refreshToken());

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
            .body(ApiResponse.ok(response));
    }

    private ResponseCookie createRefreshTokenCookie(String refreshToken) {
        return ResponseCookie.from("refreshToken", refreshToken)
            .httpOnly(true)
            .secure(false)
            .path("/")
            .sameSite("Lax")
            .maxAge(jwtProperties.getValidations().getRefresh() / 1000L)
            .build();
    }
}
