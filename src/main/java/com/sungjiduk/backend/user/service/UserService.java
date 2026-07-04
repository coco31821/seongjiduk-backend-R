package com.sungjiduk.backend.user.service;

import com.sungjiduk.backend.auth.dto.request.SignupRequest;
import com.sungjiduk.backend.auth.dto.response.UserSummaryResponse;
import com.sungjiduk.backend.common.constants.ErrorCode;
import com.sungjiduk.backend.common.security.service.TokenProvider;
import com.sungjiduk.backend.common.util.PreConditions;
import com.sungjiduk.backend.user.dto.response.UserCreateResponse;
import com.sungjiduk.backend.user.entity.CurrentUser;
import com.sungjiduk.backend.user.repository.UserEmailRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

    private final UserEmailRepository userEmailRepository;



    public CurrentUser loadCurrentUserByEmail(String email){
        return CurrentUser.from(
            userEmailRepository.findByEmailOrThrow(email)
        );
    }


}
