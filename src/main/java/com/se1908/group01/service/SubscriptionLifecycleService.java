package com.se1908.group01.service;

import com.se1908.group01.entity.Subscription;
import com.se1908.group01.entity.SubscriptionPlan;
import com.se1908.group01.entity.User;
import com.se1908.group01.enums.SubscriptionStatus;
import com.se1908.group01.repository.SubscriptionPlanRepository;
import com.se1908.group01.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class SubscriptionLifecycleService {

    public static final String FREE_PLAN_NAME = "FREE";

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository planRepository;

    @Transactional
    public Subscription getOrCreateActiveSubscription(User user) {
        Subscription activeSubscription = subscriptionRepository
                .findByUserAndStatus(user, SubscriptionStatus.ACTIVE)
                .orElse(null);

        if (activeSubscription != null
                && !isExpired(activeSubscription)) {
            return activeSubscription;
        }

        if (activeSubscription != null) {
            expire(activeSubscription);
        }

        return createFreeSubscription(user);
    }

    @Transactional
    public Subscription activatePaidSubscription(
            User user,
            SubscriptionPlan plan) {

        subscriptionRepository
                .findByUserAndStatus(user, SubscriptionStatus.ACTIVE)
                .ifPresent(this::expire);

        Subscription subscription = Subscription.builder()
                .user(user)
                .plan(plan)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(plan.getDurationDays()))
                .status(SubscriptionStatus.ACTIVE)
                .build();

        return subscriptionRepository.save(subscription);
    }

    private boolean isExpired(Subscription subscription) {
        return subscription.getEndDate() != null
                && subscription.getEndDate().isBefore(LocalDate.now());
    }

    private void expire(Subscription subscription) {
        subscription.setStatus(SubscriptionStatus.EXPIRED);
        subscriptionRepository.save(subscription);
    }

    private Subscription createFreeSubscription(User user) {
        SubscriptionPlan freePlan = planRepository
                .findByNameIgnoreCaseAndActiveTrue(FREE_PLAN_NAME)
                .orElseThrow(() -> new IllegalStateException(
                        "Active FREE subscription plan is not configured"));

        Subscription subscription = Subscription.builder()
                .user(user)
                .plan(freePlan)
                .startDate(LocalDate.now())
                .endDate(null)
                .status(SubscriptionStatus.ACTIVE)
                .build();

        return subscriptionRepository.save(subscription);
    }
}
