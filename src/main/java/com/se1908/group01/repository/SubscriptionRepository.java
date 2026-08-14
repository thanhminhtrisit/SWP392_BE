package com.se1908.group01.repository;

import com.se1908.group01.entity.Subscription;
import com.se1908.group01.entity.User;
import com.se1908.group01.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository
        extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByUserAndStatus(
            User user,
            SubscriptionStatus status);

    boolean existsByUserAndStatus(
            User user,
            SubscriptionStatus status);

    List<Subscription> findByUser(User user);
}