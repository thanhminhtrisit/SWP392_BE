package com.se1908.group01.service;

import com.se1908.group01.entity.AiTokenUsage;
import com.se1908.group01.entity.SubscriptionPlan;
import com.se1908.group01.exception.ResourceNotFoundException;
import com.se1908.group01.repository.AiTokenUsageRepository;
import com.se1908.group01.repository.DocumentRepository;
import com.se1908.group01.repository.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
/**
 * Tập trung các subscription check dùng chung cho upload và AI workflow.
 * Rule riêng của upload gồm quyền video và tổng storage đang active.
 */
public class SubscriptionEntitlementService {

	private static final long BYTES_PER_GB = 1024L * 1024L * 1024L;
	private static final int APPROX_CHARS_PER_TOKEN = 4;

	private final UserRepository userRepository;
	private final SubscriptionLifecycleService subscriptionLifecycleService;
	private final DocumentRepository documentRepository;
	private final AiTokenUsageRepository aiTokenUsageRepository;

	public SubscriptionEntitlementService(
			UserRepository userRepository,
			SubscriptionLifecycleService subscriptionLifecycleService,
			DocumentRepository documentRepository,
			AiTokenUsageRepository aiTokenUsageRepository
	) {
		this.userRepository = userRepository;
		this.subscriptionLifecycleService = subscriptionLifecycleService;
		this.documentRepository = documentRepository;
		this.aiTokenUsageRepository = aiTokenUsageRepository;
	}

	@Transactional
	public SubscriptionPlan getActivePlan(Long userId) {
		// Lấy hoặc tạo subscription active của user trước khi đọc entitlement upload.
		if (userId == null) {
			throw new IllegalArgumentException("userId is required");
		}
		var user = userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));
		var subscription = subscriptionLifecycleService.getOrCreateActiveSubscription(user);
		var plan = subscription.getPlan();
		if (plan == null) {
			throw new IllegalStateException("Active subscription plan is not configured");
		}
		return plan;
	}

	@Transactional(readOnly = true)
	public void enforceUploadEntitlements(
			Long userId,
			MultipartFile file,
			SubscriptionPlan plan,
			boolean video
	) {
		// File video hợp lệ vẫn bị từ chối nếu plan active không bật quyền upload video.
		if (video && !Boolean.TRUE.equals(plan.getVideoUpload())) {
			throw new IllegalArgumentException("Video upload is not allowed by your subscription plan");
		}
		// Storage được kiểm tra bằng các document active hiện có cộng với size file sắp upload.
		enforceStorageLimit(userId, plan, file.getSize());
	}

	@Transactional
	public void enforceAiRequestEntitlements(
			Long userId,
			int documentCount,
			String prompt
	) {
		// Stateless single-document vẫn chịu monthly token limit dù chỉ có một document.
		var plan = getActivePlan(userId);
		enforceMultipleDocumentLimit(plan, documentCount);
		enforceMonthlyTokenLimit(userId, plan, estimateTokens(prompt));
	}

	@Transactional
	public void enforceDocumentChatEntitlement(Long userId, int documentCount) {
		// Session chat kiểm tra số document ở cả lúc tạo session và lúc gửi message.
		var plan = getActivePlan(userId);
		enforceMultipleDocumentLimit(plan, documentCount);
	}

	@Transactional
	public void enforceAiTokenBudget(Long userId, String prompt) {
		// Kiểm tra prompt ước lượng trước khi gọi LLM để không vượt ngân sách tháng của plan.
		var plan = getActivePlan(userId);
		enforceMonthlyTokenLimit(userId, plan, estimateTokens(prompt));
	}

	@Transactional
	public void recordAiTokenUsage(Long userId, String prompt, String answer) {
		// Chỉ ghi usage sau khi answer đã sinh; source dùng ước lượng theo số ký tự thay vì usage provider.
		var estimatedTokens = estimateTokens(prompt) + estimateTokens(answer);
		if (estimatedTokens <= 0) {
			return;
		}
		var usage = new AiTokenUsage();
		usage.setUserId(userId);
		usage.setEstimatedTokens(estimatedTokens);
		aiTokenUsageRepository.save(usage);
	}

	private void enforceStorageLimit(Long userId, SubscriptionPlan plan, long incomingBytes) {
		var storageLimitGb = plan.getStorageLimitGb();
		if (storageLimitGb == null || storageLimitGb < 0) {
			throw new IllegalStateException("Active subscription plan storage limit is not configured");
		}
		var storageLimitBytes = storageLimitGb * BYTES_PER_GB;
		// Tổng từ repository loại document đã xóa, đúng với storage active user đang sử dụng.
		var usedBytes = documentRepository.sumActiveStorageBytesByUserId(userId);
		if (usedBytes + incomingBytes > storageLimitBytes) {
			throw new IllegalArgumentException("Storage limit exceeded for your subscription plan");
		}
	}

	private void enforceMultipleDocumentLimit(SubscriptionPlan plan, int documentCount) {
		if (documentCount > 1 && !Boolean.TRUE.equals(plan.getMultipleDocuments())) {
			throw new IllegalArgumentException("Multiple document chat is not allowed by your subscription plan");
		}
	}

	private void enforceMonthlyTokenLimit(Long userId, SubscriptionPlan plan, long requestedTokens) {
		var monthlyTokenLimit = plan.getMonthlyTokenLimit();
		if (monthlyTokenLimit == null || monthlyTokenLimit < 0) {
			throw new IllegalStateException("Active subscription plan token limit is not configured");
		}
		var usedTokens = aiTokenUsageRepository.sumEstimatedTokensSince(userId, currentMonthStart());
		if (usedTokens + requestedTokens > monthlyTokenLimit) {
			throw new IllegalArgumentException("Monthly AI token limit exceeded for your subscription plan");
		}
	}

	private Instant currentMonthStart() {
		return LocalDate.now()
				.withDayOfMonth(1)
				.atStartOfDay(ZoneId.systemDefault())
				.toInstant();
	}

	private long estimateTokens(String text) {
		if (!StringUtils.hasText(text)) {
			return 0L;
		}
		return Math.max(1L, (text.trim().length() + APPROX_CHARS_PER_TOKEN - 1L) / APPROX_CHARS_PER_TOKEN);
	}
}
