package com.se1908.group01.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.se1908.group01.entity.AiTokenUsage;
import com.se1908.group01.entity.Subscription;
import com.se1908.group01.entity.SubscriptionPlan;
import com.se1908.group01.entity.User;
import com.se1908.group01.repository.AiTokenUsageRepository;
import com.se1908.group01.repository.DocumentRepository;
import com.se1908.group01.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class SubscriptionEntitlementServiceTest {

	@Mock
	private UserRepository userRepository;
	@Mock
	private SubscriptionLifecycleService subscriptionLifecycleService;
	@Mock
	private DocumentRepository documentRepository;
	@Mock
	private AiTokenUsageRepository aiTokenUsageRepository;

	private SubscriptionEntitlementService service;

	@BeforeEach
	void setUp() {
		service = new SubscriptionEntitlementService(
				userRepository,
				subscriptionLifecycleService,
				documentRepository,
				aiTokenUsageRepository
		);
	}

	@Test
	void enforceUploadEntitlementsRejectsVideoWhenPlanDisallowsVideoUpload() {
		var plan = planBuilder()
				.videoUpload(false)
				.build();
		var file = file("lesson.mp4", "video/mp4", "video");

		assertThrows(
				IllegalArgumentException.class,
				() -> service.enforceUploadEntitlements(1L, file, plan, true)
		);
	}

	@Test
	void enforceUploadEntitlementsRejectsWhenStorageLimitWouldBeExceeded() {
		var plan = planBuilder()
				.storageLimitGb(1)
				.videoUpload(true)
				.build();
		var file = file("notes.pdf", "application/pdf", "hello");
		when(documentRepository.sumActiveStorageBytesByUserId(1L))
				.thenReturn((1024L * 1024L * 1024L) - 4L);

		assertThrows(
				IllegalArgumentException.class,
				() -> service.enforceUploadEntitlements(1L, file, plan, false)
		);
	}

	@Test
	void enforceDocumentChatEntitlementRejectsMultipleDocumentsWhenPlanDisallowsIt() {
		mockActivePlan(planBuilder()
				.multipleDocuments(false)
				.monthlyTokenLimit(100L)
				.build());

		assertThrows(
				IllegalArgumentException.class,
				() -> service.enforceDocumentChatEntitlement(1L, 2)
		);
	}

	@Test
	void enforceAiTokenBudgetRejectsWhenEstimatedMonthlyLimitWouldBeExceeded() {
		mockActivePlan(planBuilder()
				.multipleDocuments(true)
				.monthlyTokenLimit(10L)
				.build());
		when(aiTokenUsageRepository.sumEstimatedTokensSince(org.mockito.Mockito.eq(1L), org.mockito.Mockito.any(Instant.class)))
				.thenReturn(9L);

		assertThrows(
				IllegalArgumentException.class,
				() -> service.enforceAiTokenBudget(1L, "12345678")
		);
	}

	@Test
	void recordAiTokenUsageStoresEstimatedPromptAndAnswerTokens() {
		service.recordAiTokenUsage(1L, "1234", "12345");

		var captor = ArgumentCaptor.forClass(AiTokenUsage.class);
		verify(aiTokenUsageRepository).save(captor.capture());
		assertEquals(1L, captor.getValue().getUserId());
		assertEquals(3L, captor.getValue().getEstimatedTokens());
	}

	@Test
	void enforceAiRequestEntitlementsAllowsSingleDocumentWithinTokenLimit() {
		mockActivePlan(planBuilder()
				.multipleDocuments(false)
				.monthlyTokenLimit(100L)
				.build());
		when(aiTokenUsageRepository.sumEstimatedTokensSince(org.mockito.Mockito.eq(1L), org.mockito.Mockito.any(Instant.class)))
				.thenReturn(10L);

		assertDoesNotThrow(() -> service.enforceAiRequestEntitlements(1L, 1, "question"));
	}

	private void mockActivePlan(SubscriptionPlan plan) {
		var user = User.builder()
				.userId(1L)
				.email("user@example.com")
				.build();
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		when(subscriptionLifecycleService.getOrCreateActiveSubscription(user))
				.thenReturn(Subscription.builder()
						.user(user)
						.plan(plan)
						.build());
	}

	private SubscriptionPlan.SubscriptionPlanBuilder planBuilder() {
		return SubscriptionPlan.builder()
				.storageLimitGb(10)
				.maxUploadSizeMb(20)
				.multipleDocuments(true)
				.videoUpload(true)
				.monthlyTokenLimit(100L);
	}

	private MockMultipartFile file(String name, String contentType, String content) {
		return new MockMultipartFile(
				"file",
				name,
				contentType,
				content.getBytes(StandardCharsets.UTF_8)
		);
	}
}
