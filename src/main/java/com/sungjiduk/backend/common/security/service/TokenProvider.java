package com.sungjiduk.backend.common.security.service;

import com.sungjiduk.backend.common.constants.ErrorCode;
import com.sungjiduk.backend.common.exception.BusinessException;
import com.sungjiduk.backend.common.properties.JwtProperties;
import com.sungjiduk.backend.common.security.domain.RefreshToken;
import com.sungjiduk.backend.common.security.repository.RefreshTokenRepository;
import com.sungjiduk.backend.user.constants.Role;
import com.sungjiduk.backend.common.dto.KeyPair;
import com.sungjiduk.backend.user.dto.TokenBody;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class TokenProvider {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    private SecretKey getSecretKey(){
        return Keys.hmacShaKeyFor(
                jwtProperties.getSecrets().getAppKey().getBytes());
    }

    private String issueRefreshToken(
            String email
    ){
        String issuedRefreshToken = Jwts.builder()
                .subject(jwtProperties.getPayload().getSubjectRefreshToken())
                .claim("email", email)
                .issuedAt(new Date())
                .expiration(new Date(new Date().getTime() + jwtProperties.getValidations().getRefresh()))
                .signWith(getSecretKey())
                .compact();

        long refreshTokenTtlSeconds =
            jwtProperties.getValidations().getRefresh() / 1000L;

        refreshTokenRepository.save(
                RefreshToken.builder()
                        .refreshToken(issuedRefreshToken)
                        .email(email)
                        .ttl(refreshTokenTtlSeconds)
                        .build()
        );
        return issuedRefreshToken;
    }

    private String issueAccessToken(
            String email,
            Role role
    ){
        return Jwts.builder()
                .subject(jwtProperties.getPayload().getSubjectAccessToken())
                .claim("role", role.name())
                .issuer(jwtProperties.getPayload().getIssuer())
                .claim("email", email)
                .issuedAt(new Date())
                .expiration(new Date(new Date().getTime() + jwtProperties.getValidations().getAccess()))
                .signWith(getSecretKey())
                .compact();
    }

    public KeyPair issueKeyPair(
            String email,
            Role role
    ){
        return new KeyPair(
                issueAccessToken(email,role),
                issueRefreshToken(email)
        );
    }

    public boolean validate(String token){
        try {
            Jwts.parser()
                    .verifyWith(getSecretKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch(ExpiredJwtException e){
            throw new BusinessException(ErrorCode.EXPIRED_TOKEN);
        } catch(MalformedJwtException e){
            throw new BusinessException(ErrorCode.ABNORMAL_TOKEN);
        } catch(JwtException e){
            throw new BusinessException(ErrorCode.ERROR_FROM_TOKEN);
        }
    }

    public Jws<Claims> parseClaims(String token){
        return Jwts.parser()
                .verifyWith(getSecretKey())
                .build()
                .parseSignedClaims(token);
    }
    public TokenBody parseJwt(String token){
        Jws<Claims> claimsJws = parseClaims(token);
        return TokenBody.builder()
                .email(String.valueOf(claimsJws.getPayload().get("email")))
                .build();
    };
}
