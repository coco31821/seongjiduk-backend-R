package com.sungjiduk.backend.user.repository;

import com.sungjiduk.backend.common.constants.ErrorCode;
import com.sungjiduk.backend.common.exception.BusinessException;
import com.sungjiduk.backend.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User,Long> {
    Optional<User> findByEmail(String email);
    default User findByEmailOrThrow(String email){
        return findByEmail(email).orElseThrow(()-> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
    boolean existsUserByEmail(String email);
    default User findByIdOrThrow(Long id){
        return findById(id).orElseThrow(()-> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
}
