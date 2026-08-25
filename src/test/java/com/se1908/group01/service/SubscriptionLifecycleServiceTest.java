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
import java.util.List;
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
                .thenReturn(List.of(active));

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
                .thenReturn(List.of());
        when(subscriptionRepository.findPendingByUserOrderByActivationPriority(user))
                .thenReturn(List.of());
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
                .thenReturn(List.of(expiredPaid));
        when(subscriptionRepository.findPendingByUserOrderByActivationPriority(user))
                .thenReturn(List.of());
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
                .thenReturn(List.of());
        when(subscriptionRepository.findPendingByUserOrderByActivationPriority(user))
                .thenReturn(List.of());
        when(planRepository.findByNameIgnoreCaseAndActiveTrue("FREE"))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalStateException.class,
                () -> service.getOrCreateActiveSubscription(user)
        );
    }

    @Test
    void activatingPaidPlanCreatesIndependentActiveSubscription() {
        Subscription activeFree = subscription(freePlan(), null);
        SubscriptionPlan plusPlan = paidPlan();

        when(subscriptionRepository.findByUserAndStatus(
                user,
                SubscriptionStatus.ACTIVE))
                .thenReturn(List.of(activeFree));
        when(subscriptionRepository.save(any(Subscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Subscription result = service.activatePaidSubscription(
                user,
                plusPlan
        );

        assertEquals(SubscriptionStatus.ACTIVE, activeFree.getStatus());
        assertSame(plusPlan, result.getPlan());
        assertEquals(
                LocalDate.now().plusDays(30),
                result.getEndDate()
        );
    }

    @Test
    void downgradeFromPremiumCreatesPendingBasicSubscription() {
        Subscription activePremium = subscription(
                paidPlan(),
                LocalDate.now().plusDays(10)
        );

        when(subscriptionRepository.findByUserAndStatus(
                user,
                SubscriptionStatus.ACTIVE))
                .thenReturn(List.of(activePremium));
        when(subscriptionRepository.save(any(Subscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Subscription result = service.activatePaidSubscription(
                user,
                basicPlan()
        );

        assertEquals(SubscriptionStatus.ACTIVE, activePremium.getStatus());
        assertEquals(SubscriptionStatus.PENDING, result.getStatus());
        assertEquals("BASIC", result.getPlan().getName());
        assertEquals(activePremium.getEndDate().plusDays(1), result.getStartDate());
    }

    @Test
    void activatesPendingSubscriptionWhenActivePremiumExpires() {
        Subscription expiredPremium = subscription(
                paidPlan(),
                LocalDate.now().minusDays(1)
        );
        Subscription pendingBasic = Subscription.builder()
                .id(2L)
                .user(user)
                .plan(basicPlan())
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(30))
                .status(SubscriptionStatus.PENDING)
                .build();

        when(subscriptionRepository.findByUserAndStatus(
                user,
                SubscriptionStatus.ACTIVE))
                .thenReturn(List.of(expiredPremium));
        when(subscriptionRepository.findPendingByUserOrderByActivationPriority(user))
                .thenReturn(List.of(pendingBasic));
        when(subscriptionRepository.save(any(Subscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Subscription result = service.getEffectiveSubscription(user);

        assertEquals(SubscriptionStatus.EXPIRED, expiredPremium.getStatus());
        assertEquals(SubscriptionStatus.ACTIVE, pendingBasic.getStatus());
        assertSame(pendingBasic, result);
    }

    @Test
    void activatesPendingSubscriptionInsteadOfReturningExistingFreePlan() {
        Subscription expiredPremium = subscription(
                paidPlan(),
                LocalDate.now().minusDays(1)
        );
        Subscription activeFree = subscription(freePlan(), null);
        Subscription pendingBasic = Subscription.builder()
                .id(2L)
                .user(user)
                .plan(basicPlan())
                .startDate(LocalDate.now())
                .endDate(LocalDate.now().plusDays(30))
                .status(SubscriptionStatus.PENDING)
                .build();

        when(subscriptionRepository.findByUserAndStatus(
                user,
                SubscriptionStatus.ACTIVE))
                .thenReturn(List.of(expiredPremium, activeFree));
        when(subscriptionRepository.findPendingByUserOrderByActivationPriority(user))
                .thenReturn(List.of(pendingBasic));
        when(subscriptionRepository.save(any(Subscription.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Subscription result = service.getEffectiveSubscription(user);

        assertEquals(SubscriptionStatus.EXPIRED, expiredPremium.getStatus());
        assertEquals(SubscriptionStatus.ACTIVE, activeFree.getStatus());
        assertEquals(SubscriptionStatus.ACTIVE, pendingBasic.getStatus());
        assertSame(pendingBasic, result);
    }

    @Test
    void returnsHighestPlanFromMultipleActiveSubscriptions() {
        Subscription basic = subscription(basicPlan(), LocalDate.now().plusDays(10));
        Subscription premium = subscription(paidPlan(), LocalDate.now().plusDays(5));

        when(subscriptionRepository.findByUserAndStatus(
                user,
                SubscriptionStatus.ACTIVE))
                .thenReturn(List.of(basic, premium));

        Subscription result = service.getEffectiveSubscription(user);

        assertSame(premium, result);
        verify(planRepository, never())
                .findByNameIgnoreCaseAndActiveTrue(any());
    }

    @Test
    void ignoresExpiredPremiumAndKeepsActiveBasicEffective() {
        Subscription expiredPremium = subscription(paidPlan(), LocalDate.now().minusDays(1));
        Subscription activeBasic = subscription(basicPlan(), LocalDate.now().plusDays(10));

        when(subscriptionRepository.findByUserAndStatus(
                user,
                SubscriptionStatus.ACTIVE))
                .thenReturn(List.of(expiredPremium, activeBasic));

        Subscription result = service.getEffectiveSubscription(user);

        assertEquals(SubscriptionStatus.EXPIRED, expiredPremium.getStatus());
        assertSame(activeBasic, result);
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
                .name("PREMIUM")
                .price(99000D)
                .durationDays(30)
                .storageLimitGb(50)
                .monthlyTokenLimit(100000L)
                .maxUploadSizeMb(100)
                .multipleDocuments(true)
                .videoUpload(true)
                .active(true)
                .build();
    }

    private SubscriptionPlan basicPlan() {
        return SubscriptionPlan.builder()
                .id(3L)
                .name("BASIC")
                .price(49000D)
                .durationDays(30)
                .storageLimitGb(10)
                .monthlyTokenLimit(20000L)
                .maxUploadSizeMb(30)
                .multipleDocuments(false)
                .videoUpload(false)
                .active(true)
                .build();
    }
}
