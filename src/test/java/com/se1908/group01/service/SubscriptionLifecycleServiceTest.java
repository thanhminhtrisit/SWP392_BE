package com.se1908.group01.service;

import com.se1908.group01.entity.Subscription;
import com.se1908.group01.entity.SubscriptionPlan;
import com.se1908.group01.entity.User;
import com.se1908.group01.enums.SubscriptionStatus;
import com.se1908.group01.repository.SubscriptionPlanRepository;
import com.se1908.group01.repository.SubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubscriptionLifecycleServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private SubscriptionPlanRepository planRepository;

    private SubscriptionLifecycleService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new SubscriptionLifecycleService(
                subscriptionRepository,
                planRepository
        );
        user = User.builder()
                .userId(1L)
                .email("user@example.com")
                .build();
    }

    @Test
    void returnsCurrentSubscriptionWhenItHasNotExpired() {
        Subscription active = subscription(
                paidPlan(),
                LocalDate.now().plusDays(1)
        );
        when(subscriptionRepository.findByUserAndStatus(
                user,
                SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(active));

        Subscription result = service.getOrCreateActiveSubscription(user);

        assertSame(active, result);
        verify(planRepository, never())
                .findByNameIgnoreCaseAndActiveTrue(any());
    }

    @Test
    void createsFreeSubscriptionWhenUserHasNoActivePlan() {
        SubscriptionPlan freePlan = freePlan();
        when(subscriptionRepository.findByUserAndStatus(
                user,
                SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(planRepository.findByNameIgnoreCaseAndActiveTrue("FREE"))
                .thenReturn(Optional.of(freePlan));
        when(subscriptionRepository.save(any(Subscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Subscription result = service.getOrCreateActiveSubscription(user);

        assertSame(freePlan, result.getPlan());
        assertEquals(SubscriptionStatus.ACTIVE, result.getStatus());
        assertNull(result.getEndDate());
    }

    @Test
    void expiresPaidSubscriptionAndFallsBackToFree() {
        Subscription expiredPaid = subscription(
                paidPlan(),
                LocalDate.now().minusDays(1)
        );
        SubscriptionPlan freePlan = freePlan();

        when(subscriptionRepository.findByUserAndStatus(
                user,
                SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(expiredPaid));
        when(planRepository.findByNameIgnoreCaseAndActiveTrue("FREE"))
                .thenReturn(Optional.of(freePlan));
        when(subscriptionRepository.save(any(Subscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Subscription result = service.getOrCreateActiveSubscription(user);

        assertEquals(SubscriptionStatus.EXPIRED, expiredPaid.getStatus());
        assertSame(freePlan, result.getPlan());
        assertEquals(SubscriptionStatus.ACTIVE, result.getStatus());
    }

    @Test
    void failsClearlyWhenFreePlanIsNotConfigured() {
        when(subscriptionRepository.findByUserAndStatus(
                user,
                SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.empty());
        when(planRepository.findByNameIgnoreCaseAndActiveTrue("FREE"))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalStateException.class,
                () -> service.getOrCreateActiveSubscription(user)
        );
    }

    @Test
    void activatingPaidPlanExpiresCurrentFreeSubscription() {
        Subscription activeFree = subscription(freePlan(), null);
        SubscriptionPlan plusPlan = paidPlan();

        when(subscriptionRepository.findByUserAndStatus(
                user,
                SubscriptionStatus.ACTIVE))
                .thenReturn(Optional.of(activeFree));
        when(subscriptionRepository.save(any(Subscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Subscription result = service.activatePaidSubscription(
                user,
                plusPlan
        );

        assertEquals(SubscriptionStatus.EXPIRED, activeFree.getStatus());
        assertSame(plusPlan, result.getPlan());
        assertEquals(
                LocalDate.now().plusDays(30),
                result.getEndDate()
        );
    }

    private Subscription subscription(
            SubscriptionPlan plan,
            LocalDate endDate) {

        return Subscription.builder()
                .id(1L)
                .user(user)
                .plan(plan)
                .startDate(LocalDate.now().minusDays(1))
                .endDate(endDate)
                .status(SubscriptionStatus.ACTIVE)
                .build();
    }

    private SubscriptionPlan freePlan() {
        return SubscriptionPlan.builder()
                .id(1L)
                .name("FREE")
                .price(0D)
                .durationDays(30)
                .active(true)
                .build();
    }

    private SubscriptionPlan paidPlan() {
        return SubscriptionPlan.builder()
                .id(2L)
                .name("PLUS")
                .price(99000D)
                .durationDays(30)
                .active(true)
                .build();
    }
}
