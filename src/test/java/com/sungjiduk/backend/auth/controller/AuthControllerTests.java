package com.sungjiduk.backend.auth.controller;

import com.sungjiduk.backend.auth.dto.request.LoginRequest;
import com.sungjiduk.backend.auth.dto.request.SignupRequest;
import com.sungjiduk.backend.common.constants.ErrorCode;
import com.sungjiduk.backend.common.properties.JwtProperties;
import com.sungjiduk.backend.common.security.repository.RefreshTokenRepository;
import com.sungjiduk.backend.user.entity.User;
import com.sungjiduk.backend.user.repository.UserEmailRepository;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
@AutoConfigureMockMvc
class AuthControllerTests {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    UserEmailRepository userEmailRepository;

    @Autowired
    JwtProperties jwtProperties;

    // 로그인 성공 시 TokenProvider가 RefreshToken을 Redis에 저장하려고 하므로, 테스트에서는 Redis Repository만 가짜로 대체한다.
    @MockitoBean
    RefreshTokenRepository refreshTokenRepository;

    String BASE_URL = "/api/auth";

    @Nested
    @DisplayName("회원가입")
    class Signup {

        @Test
        @DisplayName("회원가입 성공해 201 Created와 성공 응답을 반환한다.")
        void 회원가입성공() throws Exception {
            // given
            SignupRequest request = new SignupRequest(
                "auth-controller-signup-success@gmail.com",
                "tjdwlejr1234",
                "테스트2"
            );

            String json = objectMapper.writeValueAsString(request); // HTTP 요청 body에 넣기 위해 JAVA 객체를 JSON 형태로 변환

            // when
            mockMvc.perform(    // json post 요청을 만듦.
                    MockMvcRequestBuilders  // 가짜 http요청을 만들어주는 클래스
                        .post(BASE_URL + "/signup")
                        .contentType(MediaType.APPLICATION_JSON)    // 요청 body 형식이 json
                        .content(json)  // 요청 body에 담을 내용
                )
                // then
                .andExpect(status().isCreated())    // 상태코드가 201인지 확인
                .andExpect(jsonPath("$.success").value(true))   // success 값이 true인지 확인
                .andExpect(jsonPath("$.data.email").value(request.email())) // data 객체 안의 email 값 검사
                .andExpect(jsonPath("$.data.nickname").value(request.nickname()));

            User savedUser = userEmailRepository.findByEmail(request.email())
                .orElseThrow(() -> new AssertionError("회원가입된 사용자를 DB에서 찾을 수 없습니다."));   // 가입한 사용자가 DB에 없으면 테스트 실패

            assertNotNull(savedUser.getId());   // DB에 저장되어 id가 발급되었는지 확인
            assertEquals(request.email(), savedUser.getEmail());    // 회원가입 요청으로 보낸 이메일과 DB에 저장된 이메일이 같은지 확인
            assertEquals(request.nickname(), savedUser.getNickname());  // 요청 닉네임이 그대로 저장되었는가?
            assertNotEquals(request.password(), savedUser.getPasswordHash());   // 비밀번호가 평문 그대로 저장되지 않았는지 확인
            assertTrue(passwordEncoder.matches(request.password(), savedUser.getPasswordHash()));   // 원래 비밀번호로 로그인 검증이 가능한가?
        }

        @Test
        @DisplayName("실패 - 이메일 형식 위반, 400 반환")
        void 이메일형식위반() throws Exception {
            // given
            SignupRequest request = new SignupRequest(
                "test2.com",
                "tjdwlejr1234",
                "테스트2"
            );

            String json = objectMapper.writeValueAsString(request);

            // when
            mockMvc.perform(
                    MockMvcRequestBuilders
                        .post(BASE_URL + "/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                )
                // then
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value(ErrorCode.VALIDATION_FAILED.name()))
                .andExpect(jsonPath("$.error.message").value("이메일 형식이 올바르지 않습니다."));
        }

        @Test
        @DisplayName("실패 - 중복된 이메일로 가입 시도 시 409 반환")
        void 중복이메일() throws Exception {
            // given
            String duplicatedEmail = "auth-controller-duplicated@gmail.com";
            User existingUser = User.builder()
                .email(duplicatedEmail)
                .passwordHash(passwordEncoder.encode("tjdwlejr1234"))
                .nickname("이미가입한유저")
                .build();
            userEmailRepository.userSave(existingUser);

            SignupRequest request = new SignupRequest(
                duplicatedEmail,
                "tjdwlejr1234",
                "테스트2"
            );

            String json = objectMapper.writeValueAsString(request);

            // when
            mockMvc.perform(
                    MockMvcRequestBuilders
                        .post(BASE_URL + "/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                )
                // then
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value(ErrorCode.USER_EMAIL_DUPLICATED.name()))
                .andExpect(jsonPath("$.error.message").value(ErrorCode.USER_EMAIL_DUPLICATED.getDescription()));
        }
    }

    @Nested
    @DisplayName("로그인")
    class Login {

        @Test
        @DisplayName("로그인 성공해 200 OK와 토큰 응답을 반환한다.")
        void 로그인성공() throws Exception {
            // given
            String email = "auth-controller-login-success@gmail.com";
            String password = "tjdwlejr1234";
            User existingUser = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(password))
                .nickname("로그인유저")
                .build();
            userEmailRepository.userSave(existingUser);

            LoginRequest request = new LoginRequest(email, password);
            String json = objectMapper.writeValueAsString(request);

            // when
            mockMvc.perform(
                    MockMvcRequestBuilders
                        .post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                )
                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.expiresInSeconds")
                    .value(jwtProperties.getValidations().getAccess() / 1000L));
        }

        @Test
        @DisplayName("실패 - 비밀번호가 틀리면 401 반환")
        void 비밀번호틀림() throws Exception {
            // given
            String email = "auth-controller-login-wrong-password@gmail.com";
            User existingUser = User.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("rightPassword1234"))
                .nickname("로그인유저")
                .build();
            userEmailRepository.userSave(existingUser);

            LoginRequest request = new LoginRequest(email, "wrongPassword1234");
            String json = objectMapper.writeValueAsString(request);

            // when
            mockMvc.perform(
                    MockMvcRequestBuilders
                        .post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                )
                // then
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value(ErrorCode.LOGIN_FAILED.name()))
                .andExpect(jsonPath("$.error.message").value(ErrorCode.LOGIN_FAILED.getDescription()));
        }

        @Test
        @DisplayName("실패 - 가입되지 않은 이메일이면 401 반환")
        void 가입되지않은이메일() throws Exception {
            // given
            LoginRequest request = new LoginRequest(
                "auth-controller-login-not-found@gmail.com",
                "tjdwlejr1234"
            );
            String json = objectMapper.writeValueAsString(request);

            // when
            mockMvc.perform(
                    MockMvcRequestBuilders
                        .post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json)
                )
                // then
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value(ErrorCode.LOGIN_FAILED.name()))
                .andExpect(jsonPath("$.error.message").value(ErrorCode.LOGIN_FAILED.getDescription()));
        }
    }

    @Nested
    @DisplayName("로그아웃")
    class Logout {

        @Test
        @DisplayName("로그아웃 성공해 RefreshToken을 삭제하고 쿠키를 만료한다.")
        void 로그아웃성공() throws Exception {
            // given
            String refreshToken = "refresh-token-for-controller-logout";

            // when
            mockMvc.perform(
                    MockMvcRequestBuilders
                        .post(BASE_URL + "/logout")
                        .cookie(new Cookie("refreshToken", refreshToken))
                        .with(user("logout-user"))
                )
                // then
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andExpect(jsonPath("$.error").doesNotExist())
                // Set-Cookie 헤더가 refreshToken 쿠키를 삭제하도록 내려왔는지 확인. SET_COOKIE값을 검사.
                .andExpect(header().string(HttpHeaders.SET_COOKIE, allOf(
                    containsString("refreshToken="),    // 로그아웃 시 refresh token 쿠키를 대상
                    containsString("Path=/"),
                    containsString("Max-Age=0"),
                    containsString("HttpOnly"),
                    containsString("SameSite=Lax")
                )));

            verify(refreshTokenRepository).deleteById(refreshToken);
        }


        @Test
        @DisplayName("인증 없이 로그아웃 요청하면 실패한다")
        void 로그아웃_인증없음() throws Exception {
            // when
            mockMvc.perform(
                    MockMvcRequestBuilders
                        .post(BASE_URL + "/logout")
                        .cookie(new Cookie("refreshToken", "refresh-token"))
                )
            // then
                .andExpect(status().isForbidden());
        }
    }
}
