package com.se1908.group01.repository;

import com.se1908.group01.entity.Subscription;
import com.se1908.group01.entity.User;
import com.se1908.group01.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SubscriptionRepository
        extends JpaRepository<Subscription, Long> {

    List<Subscription> findByUserAndStatus(
            User user,
            SubscriptionStatus status);

    boolean existsByUserAndStatus(
            User user,
            SubscriptionStatus status);

    @Query("""
            SELECT s
            FROM Subscription s
            LEFT JOIN s.payment p
            WHERE s.user = :user
              AND s.status = com.se1908.group01.enums.SubscriptionStatus.PENDING
            ORDER BY
              CASE WHEN s.startDate IS NULL THEN 1 ELSE 0 END,
              s.startDate ASC,
              CASE WHEN p.createdAt IS NULL THEN 1 ELSE 0 END,
              p.createdAt ASC,
              s.id ASC
            """)
    List<Subscription> findPendingByUserOrderByActivationPriority(User user);

    List<Subscription> findByUser(User user);
}
