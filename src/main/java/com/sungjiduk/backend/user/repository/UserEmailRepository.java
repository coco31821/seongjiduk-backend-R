package com.sungjiduk.backend.user.repository;

import com.sungjiduk.backend.user.entity.User;
import org.springframework.data.domain.PageRequest;

import java.util.Optional;

public interface UserEmailRepository {

    User userSave(User user);
    boolean existsUserByEmail(String email);
    User findByEmailOrThrow(String email);
    Optional<User> findByEmail(String email);
    User findByIdOrThrow(Long id);
}
