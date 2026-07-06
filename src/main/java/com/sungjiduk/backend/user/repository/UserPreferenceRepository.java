package com.sungjiduk.backend.user.repository;

import com.sungjiduk.backend.user.entity.UserPreference;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {

    Optional<UserPreference> findFirstByUserIdOrderByIdDesc(Long userId);
}
