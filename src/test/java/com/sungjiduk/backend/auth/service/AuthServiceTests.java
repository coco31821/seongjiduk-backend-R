package com.sungjiduk.backend.auth.service;

import com.sungjiduk.backend.auth.dto.request.LoginRequest;
import com.sungjiduk.backend.auth.dto.request.SignupRequest;
import com.sungjiduk.backend.auth.dto.response.LoginResponse;
import com.sungjiduk.backend.auth.dto.response.UserSummaryResponse;
import com.sungjiduk.backend.common.constants.ErrorCode;
import com.sungjiduk.backend.common.exception.BusinessException;
import com.sungjiduk.backend.common.properties.JwtProperties;
import com.sungjiduk.backend.common.security.repository.RefreshTokenRepository;
import com.sungjiduk.backend.user.entity.User;
import com.sungjiduk.backend.user.repository.UserEmailRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;

@SpringBootTest
@Transactional
class AuthServiceTests {

    @Autowired
    AuthService authService;

    @Autowired
    UserEmailRepository userEmailRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    JwtProperties jwtProperties;

    // 로그인 성공 시 TokenProvider가 RefreshToken을 Redis에 저장하려고 하므로, 테스트에서는 Redis Repository만 가짜로 대체한다.
    @MockitoBean
    RefreshTokenRepository refreshTokenRepository;

    @Nested
    @DisplayName("signup() 메서드에서")
    class Signup {

        @Test
        @DisplayName("유저가 성공적으로 생성된다")
        void signupTest() {
            // given
            SignupRequest request = new SignupRequest(
                "auth-service-signup-success@gmail.com",
                "tjdwlejr1234",
                "성지덕테스트3"
            );

            // when
            UserSummaryResponse savedUserResponse = authService.signup(request);

            // then
            assertThat(savedUserResponse).isNotNull();
            assertThat(savedUserResponse.email()).isEqualTo(request.email());
            assertThat(savedUserResponse.nickname()).isEqualTo(request.nickname());

            // 실제 DB에 저장되었는지 확인하기 위해 가입한 이메일로 다시 조회한다.
            User savedUser = userEmailRepository.findByEmail(request.email())
                .orElseThrow(() -> new AssertionError("회원가입된 사용자를 DB에서 찾을 수 없습니다."));

            assertThat(savedUser.getId()).isNotNull();
            assertThat(savedUser.getEmail()).isEqualTo(request.email());
            assertThat(savedUser.getNickname()).isEqualTo(request.nickname());

            // 비밀번호가 평문 그대로 저장되지 않고, 원래 비밀번호로 검증 가능한 해시인지 확인한다.
            assertThat(savedUser.getPasswordHash()).isNotEqualTo(request.password());
            assertThat(passwordEncoder.matches(request.password(), savedUser.getPasswordHash())).isTrue();
        }

        @Test
        @DisplayName("중복된 이메일로 가입 시도 시 실패한다")
        void signup_duplicated_email() {
            // given
            String duplicatedEmail = "auth-service-duplicated@gmail.com";
            User existingUser = User.builder()
                .email(duplicatedEmail)
                .passwordHash(passwordEncoder.encode("tjdwlejr1234"))
                .nickname("이미가입한유저")
                .build();
            userEmailRepository.userSave(existingUser);

            SignupRequest request = new SignupRequest(
                duplicatedEmail,
                "tjdwlejr1234",
                "성지덕테스트4"
            );

            // when & then
            assertThatThrownBy(() -> authService.signup(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.USER_EMAIL_DUPLICATED.getDescription());
        }
    }

    @Nested
    @DisplayName("login() 메서드에서")
    class Login {

        @Test
        @DisplayName("이메일과 비밀번호가 맞으면 토큰 응답을 반환한다")
        void login_success() {
            // given
            String email = "auth-service-login-success@gmail.com";
            String password = "tjdwlejr1234";
            User savedUser = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .nickname("로그인유저")
                .build();
            userEmailRepository.userSave(savedUser);

            LoginRequest request = new LoginRequest(email, password);

            // when
            LoginResponse response = authService.login(request);

            // then
            assertThat(response).isNotNull();
            assertThat(response.accessToken()).isNotBlank();
            assertThat(response.refreshToken()).isNotBlank();
            assertThat(response.tokenType()).isEqualTo("Bearer");
            assertThat(response.expiresInSeconds())
                .isEqualTo(jwtProperties.getValidations().getAccess() / 1000L);
        }

        @Test
        @DisplayName("비밀번호가 틀리면 로그인에 실패한다")
        void login_wrong_password() {
            // given
            String email = "auth-service-login-wrong-password@gmail.com";
            User savedUser = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("rightPassword1234"))
                .nickname("로그인유저")
                .build();
            userEmailRepository.userSave(savedUser);

            LoginRequest request = new LoginRequest(email, "wrongPassword1234");

            // when & then
            assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.LOGIN_FAILED.getDescription());
        }

        @Test
        @DisplayName("가입되지 않은 이메일이면 로그인에 실패한다")
        void login_email_not_found() {
            // given
            LoginRequest request = new LoginRequest(
                "auth-service-login-not-found@gmail.com",
                "tjdwlejr1234"
            );

            // when & then
            assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(ErrorCode.LOGIN_FAILED.getDescription());
        }
    }

    @Nested
    @DisplayName("logout() 메서드에서")
    class Logout {

        @Test
        @DisplayName("RefreshToken을 삭제한다")
        void logout_success() {
            // given
            String refreshToken = "refresh-token-for-logout";

            // when
            authService.logout(refreshToken);

            // then
            verify(refreshTokenRepository).deleteById(refreshToken);
        }
    }
}
