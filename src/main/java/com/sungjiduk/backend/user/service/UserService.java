package com.sungjiduk.backend.user.service;

import com.sungjiduk.backend.user.entity.CurrentUser;
import com.sungjiduk.backend.user.repository.UserEmailRepository;
import lombok.RequiredArgsConstructor;

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
