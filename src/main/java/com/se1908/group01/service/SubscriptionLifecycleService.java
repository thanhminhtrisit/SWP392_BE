package com.se1908.group01.service;

import com.se1908.group01.entity.Payment;
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
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SubscriptionLifecycleService {

    public static final String FREE_PLAN_NAME = "FREE";

    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionPlanRepository planRepository;

    @Transactional
    public Subscription getOrCreateActiveSubscription(User user) {
        List<Subscription> activeSubscriptions = subscriptionRepository
                .findByUserAndStatus(user, SubscriptionStatus.ACTIVE);

        activeSubscriptions.stream()
                .filter(this::isExpired)
                .forEach(this::expire);

        Subscription effectiveSubscription = activeSubscriptions.stream()
                .filter(subscription -> !isExpired(subscription))
                .max(this::compareEffectiveSubscription)
                .orElse(null);

        if (effectiveSubscription != null) {
            return effectiveSubscription;
        }

        return activateDuePendingSubscription(user);
    }

    @Transactional
    public Subscription getEffectiveSubscription(User user) {
        return getOrCreateActiveSubscription(user);
    }

    @Transactional(readOnly = true)
    public Optional<Subscription> getNextPendingSubscription(User user) {
        return subscriptionRepository
                .findByUserAndStatus(user, SubscriptionStatus.PENDING)
                .stream()
                .max(this::compareEffectiveSubscription);
    }

    @Transactional
    public Subscription activatePaidSubscription(
            User user,
            SubscriptionPlan plan,
            Payment payment) {

        Subscription effectiveSubscription = getEffectiveSubscription(user);
        if (isDowngrade(effectiveSubscription.getPlan(), plan)) {
            return createPendingSubscription(
                    user,
                    plan,
                    payment,
                    effectiveSubscription.getEndDate()
            );
        }

        Subscription subscription = Subscription.builder()
                .user(user)
                .plan(plan)
                .payment(payment)
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(plan.getDurationDays()))
                .status(SubscriptionStatus.ACTIVE)
                .build();

        return subscriptionRepository.save(subscription);
    }

    @Transactional
    public Subscription activatePaidSubscription(
            User user,
            SubscriptionPlan plan) {

        return activatePaidSubscription(user, plan, null);
    }

    private Subscription activateDuePendingSubscription(User user) {
        List<Subscription> pendingSubscriptions = subscriptionRepository
                .findByUserAndStatus(user, SubscriptionStatus.PENDING);

        Subscription pendingSubscription = pendingSubscriptions.stream()
                .filter(this::isPendingReady)
                .max(this::compareEffectiveSubscription)
                .orElse(null);

        if (pendingSubscription != null) {
            pendingSubscription.setStatus(SubscriptionStatus.ACTIVE);
            return subscriptionRepository.save(pendingSubscription);
        }

        return createFreeSubscription(user);
    }

    private Subscription createPendingSubscription(
            User user,
            SubscriptionPlan plan,
            Payment payment,
            LocalDate currentEndDate) {

        LocalDate startDate = currentEndDate == null
                ? LocalDate.now()
                : currentEndDate.plusDays(1);

        Subscription subscription = Subscription.builder()
                .user(user)
                .plan(plan)
                .payment(payment)
                .startDate(startDate)
                .endDate(startDate.plusDays(plan.getDurationDays()))
                .status(SubscriptionStatus.PENDING)
                .build();

        return subscriptionRepository.save(subscription);
    }

    private boolean isDowngrade(
            SubscriptionPlan currentPlan,
            SubscriptionPlan requestedPlan) {

        return effectivePlanComparator()
                .compare(requestedPlan, currentPlan) < 0;
    }

    private boolean isPendingReady(Subscription subscription) {
        return subscription.getStartDate() == null
                || !subscription.getStartDate().isAfter(LocalDate.now());
    }

    private int compareEffectiveSubscription(
            Subscription left,
            Subscription right) {

        int planComparison = effectivePlanComparator()
                .compare(left.getPlan(), right.getPlan());
        if (planComparison != 0) {
            return planComparison;
        }

        Comparator<Subscription> subscriptionTieBreaker = Comparator
                .comparing(
                        Subscription::getEndDate,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(
                        Subscription::getStartDate,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(
                        Subscription::getId,
                        Comparator.nullsLast(Comparator.naturalOrder()));

        return subscriptionTieBreaker.compare(left, right);
    }

    private Comparator<SubscriptionPlan> effectivePlanComparator() {
        return Comparator
                .comparingInt(this::knownPlanLevel)
                .thenComparing(this::numberOrZero)
                .thenComparingLong(this::tokenLimitOrZero)
                .thenComparingInt(this::uploadSizeOrZero)
                .thenComparingInt(plan -> Boolean.TRUE.equals(plan.getMultipleDocuments()) ? 1 : 0)
                .thenComparingInt(plan -> Boolean.TRUE.equals(plan.getVideoUpload()) ? 1 : 0)
                .thenComparingDouble(plan -> plan.getPrice() == null ? 0D : plan.getPrice());
    }

    private int knownPlanLevel(SubscriptionPlan plan) {
        if (plan == null || plan.getName() == null) {
            return 0;
        }

        return switch (plan.getName().trim().toUpperCase()) {
            case "FREE" -> 0;
            case "BASIC" -> 1;
            case "PREMIUM", "PRO", "PLUS" -> 2;
            default -> 1;
        };
    }

    private int numberOrZero(SubscriptionPlan plan) {
        return plan.getStorageLimitGb() == null ? 0 : plan.getStorageLimitGb();
    }

    private long tokenLimitOrZero(SubscriptionPlan plan) {
        return plan.getMonthlyTokenLimit() == null ? 0L : plan.getMonthlyTokenLimit();
    }

    private int uploadSizeOrZero(SubscriptionPlan plan) {
        return plan.getMaxUploadSizeMb() == null ? 0 : plan.getMaxUploadSizeMb();
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
