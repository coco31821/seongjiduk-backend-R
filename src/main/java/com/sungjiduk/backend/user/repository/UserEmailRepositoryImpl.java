package com.sungjiduk.backend.user.repository;

import com.sungjiduk.backend.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class UserEmailRepositoryImpl implements UserEmailRepository {

    private final UserRepository userRepository;


    @Override
    public User userSave(User user) {
        return userRepository.save(user);
    }

    @Override
    public boolean existsUserByEmail(String email) {
        return userRepository.existsUserByEmail(email);
    }

    @Override
    public User findByEmailOrThrow(String email) {

        return userRepository.findByEmailOrThrow(email);
    }


    @Override
    public Optional<User> findByEmail(String email) {

        return userRepository.findByEmail(email);
    }

    @Override
    public User findByIdOrThrow(Long id) {

        return userRepository.findByIdOrThrow(id);
    }
}
