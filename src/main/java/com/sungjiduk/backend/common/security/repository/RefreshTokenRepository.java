package com.sungjiduk.backend.common.security.repository;


import com.sungjiduk.backend.common.constants.ErrorCode;
import com.sungjiduk.backend.common.exception.BusinessException;
import com.sungjiduk.backend.common.security.domain.RefreshToken;
import org.springframework.data.repository.CrudRepository;

public interface RefreshTokenRepository extends CrudRepository<RefreshToken, String> {
    default RefreshToken findByRefreshTokenOrThrow(String refreshToken){
        return findById(refreshToken).orElseThrow(
                ()-> new BusinessException(ErrorCode.REFRESH_TOKEN_EXPIRED)
        );
    }
}
