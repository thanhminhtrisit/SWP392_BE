package com.se1908.group01.repository;

import com.se1908.group01.entity.UserSettings;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserSettingsRepository extends JpaRepository<UserSettings, Long> {

	Optional<UserSettings> findByUserId(Long userId);
}
