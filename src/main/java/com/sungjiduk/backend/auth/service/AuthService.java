package com.sungjiduk.backend.auth.service;

import com.sungjiduk.backend.auth.dto.response.MeResponse;
import com.sungjiduk.backend.user.entity.User;
import com.sungjiduk.backend.auth.dto.request.LoginRequest;
import com.sungjiduk.backend.auth.dto.request.SignupRequest;
import com.sungjiduk.backend.auth.dto.response.LoginResponse;
import com.sungjiduk.backend.auth.dto.response.UserSummaryResponse;
import com.sungjiduk.backend.common.constants.ErrorCode;
import com.sungjiduk.backend.common.dto.KeyPair;
import com.sungjiduk.backend.common.exception.BusinessException;
import com.sungjiduk.backend.common.properties.JwtProperties;
import com.sungjiduk.backend.common.security.repository.RefreshTokenRepository;
import com.sungjiduk.backend.common.security.service.TokenProvider;
import com.sungjiduk.backend.common.util.PreConditions;
import com.sungjiduk.backend.user.repository.UserEmailRepository;
import com.sungjiduk.backend.user.repository.UserPreferenceRepository;
import com.sungjiduk.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class AuthService {

    private final UserEmailRepository userEmailRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenProvider tokenProvider;
    private final JwtProperties jwtProperties;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final UserPreferenceRepository userPreferenceRepository;

    @Transactional
    public UserSummaryResponse signup(SignupRequest request) {

        PreConditions.validate(
            !userEmailRepository.existsUserByEmail(request.email()),
            ErrorCode.USER_EMAIL_DUPLICATED
        );

        return UserSummaryResponse.from(
            userEmailRepository.userSave(
                User.builder()
                    .email(request.email())
                    .passwordHash(passwordEncoder.encode(request.password()))
                    .nickname(request.nickname())
                    .build()
            )
        );
    }

    public LoginResponse login(LoginRequest request) {
        User user = userEmailRepository.findByEmail(request.email())
            .filter(foundUser -> passwordEncoder.matches(request.password(), foundUser.getPasswordHash()))
            .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));

        KeyPair keyPair = tokenProvider.issueKeyPair(user.getEmail(), user.getRole());

        return new LoginResponse(
            keyPair.accessToken(),
            keyPair.refreshToken(),
            "Bearer",
            jwtProperties.getValidations().getAccess() / 1000L
        );
    }

    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.deleteById(refreshToken);
    }

    public MeResponse me(Long userId) {
        User user = userRepository.findByIdOrThrow(userId);

        return MeResponse.from(
            user,
            userPreferenceRepository.findFirstByUserIdOrderByIdDesc(userId).orElse(null)
        );
    }

}
