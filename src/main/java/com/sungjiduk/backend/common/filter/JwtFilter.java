package com.sungjiduk.backend.common.filter;


import com.sungjiduk.backend.common.exception.BusinessException;
import com.sungjiduk.backend.common.security.service.TokenProvider;
import com.sungjiduk.backend.user.dto.TokenBody;
import com.sungjiduk.backend.user.entity.CurrentUser;
import com.sungjiduk.backend.user.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {
    private final TokenProvider tokenProvider;
    private final UserService userService;
    private final HandlerExceptionResolver handlerExceptionResolver;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String extractedToken = extractToken(request);

        if (extractedToken == null){
            filterChain.doFilter(request,response);
            return;
        }

        try {
            if (tokenProvider.validate(extractedToken)) {
                TokenBody tokenBody = tokenProvider.parseJwt(extractedToken);
                CurrentUser currentUser = userService.loadCurrentUserByEmail(tokenBody.email());
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        currentUser,
                        null,
                        currentUser.getAuthorities()
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (BusinessException exception) { // 요청이 controller에 도착하기 전에 예외발생하므로, handlerExceptionResolver를 필터에 주입해서 처리. 에러처리를 위함.
            handlerExceptionResolver.resolveException(request, response, null, exception);
            return;
        }

        filterChain.doFilter(request,response);
    }
    public String extractToken(HttpServletRequest request){
        String bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION);
        if(bearerToken != null && bearerToken.startsWith("Bearer ")) return bearerToken.substring(7);
        return null;
    }
}
