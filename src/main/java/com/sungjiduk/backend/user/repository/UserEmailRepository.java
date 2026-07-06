package com.sungjiduk.backend.user.repository;

import com.sungjiduk.backend.user.entity.User;

import java.util.Optional;

public interface UserEmailRepository {

    User userSave(User user);
    boolean existsUserByEmail(String email);
    User findByEmailOrThrow(String email);
    Optional<User> findByEmail(String email);
}
