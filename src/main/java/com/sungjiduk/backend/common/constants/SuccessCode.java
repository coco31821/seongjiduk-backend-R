package com.sungjiduk.backend.common.constants;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum SuccessCode {

    // USER
    USER_CREATED("계정이 정상적으로 생성되었습니다."),
    USER_SIGNIN("계정에 성공적으로 로그인되었습니다"),
    TOKEN_REFRESH_COMPLETED("토큰이 성공적으로 재발급되었습니다."),
    USER_DETAIL_READED("성공적으로 계정이 조회되었습니다."),
    USER_READ("계정이 성공적으로 조회되었습니다"),


    USER_ROLE_UPDATED("성공적으로 계정의 역할이 변경되었습니다."),
    USER_UPDATED("성공적으로 계정의  xx 정보가 변경되었습니다.")
    ;

    private final String successMessage;
}
