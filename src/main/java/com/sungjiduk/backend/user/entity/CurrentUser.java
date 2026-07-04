package com.sungjiduk.backend.user.entity;


import com.sungjiduk.backend.common.constants.ErrorCode;
import com.sungjiduk.backend.common.exception.BusinessException;
import com.sungjiduk.backend.user.constants.Role;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Getter
public class CurrentUser  {
    private Long id;
    private String email;
    private String nickname;
    private Role role;
    private Map<String,Object> attributes;

    @Builder
    private CurrentUser(
            Long id,
            String email,
            String nickname,
            Role role
            ){
        this.id = id;
        this.email = email;
        this.nickname = nickname;
        this.role = role;
    }

    public static CurrentUser from(User user){
        if (user == null) throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        return CurrentUser.builder()
                .id(user.getId())
                .email(user.getEmail())
                .nickname(user.getNickname())
                .role(user.getRole())
                .build();
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

}
