package com.sungjiduk.backend.common.security.domain;


import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Getter
@ToString
@Builder
@RedisHash("refresh_token")
public class RefreshToken {
    @Id
    private String refreshToken;
    private String email;

    // 몇 초 뒤 자동 삭제할 지
    @TimeToLive
    private Long ttl;

    public RefreshToken(String refreshToken, String email, Long ttl){
        this.refreshToken = refreshToken;
        this.email = email;
        this.ttl = ttl;
    }
}
