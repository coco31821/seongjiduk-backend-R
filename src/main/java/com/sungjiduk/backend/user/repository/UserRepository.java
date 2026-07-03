package com.sungjiduk.backend.user.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.sungjiduk.backend.user.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

}
