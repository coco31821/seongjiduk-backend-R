package com.sungjiduk.backend.common.config;

import com.sungjiduk.backend.common.filter.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;


    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(   // 로그인 없이 접근 가능한 페이지
                                "/actuator/health",
                                "/actuator/prometheus",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/api/health",
                                "/api/auth/signup",
                                "/api/auth/login",
                                "/api/contents/**",
                                "/api/spots/**",
                                "/api/trips/generate",
                                "/api/booking-links"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/trips/*").permitAll()
                        // 명세(05) 기준 재생성은 생성과 동일하게 비회원 허용("선택" 인증)
                        .requestMatchers(HttpMethod.POST, "/api/trips/*/regenerate").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/events").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")  // 관리자 전용 페이지
                        .anyRequest().authenticated()
                )
                .httpBasic(basic -> basic.disable())
                .formLogin(basic -> basic.disable())
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class) // Spring Security 전에 (요청이 controller에 가기 전에) JWT를 먼저 검사
                .build();
    }

    @Bean
    UserDetailsService userDetailsService() {
        return new InMemoryUserDetailsManager(
                User.withUsername("user")
                        .password("{noop}password1234")
                        .roles("USER")
                        .build(),
                User.withUsername("admin")
                        .password("{noop}admin1234")
                        .roles("ADMIN")
                        .build()
        );
    }
}
