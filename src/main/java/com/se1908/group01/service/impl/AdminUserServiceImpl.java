package com.se1908.group01.service.impl;

import com.se1908.group01.dto.AdminUserListResponse;
import com.se1908.group01.dto.AdminUserResponse;
import com.se1908.group01.entity.User;
import com.se1908.group01.enums.AccountStatus;
import com.se1908.group01.enums.Role;
import com.se1908.group01.exception.ResourceNotFoundException;
import com.se1908.group01.repository.UserRepository;
import com.se1908.group01.service.AdminUserService;
import com.se1908.group01.service.CurrentUserService;
import com.se1908.group01.service.RefreshTokenService;
import java.util.Locale;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AdminUserServiceImpl implements AdminUserService {

	private static final int MAX_PAGE_SIZE = 100;

	private final UserRepository userRepository;
	private final CurrentUserService currentUserService;
	private final RefreshTokenService refreshTokenService;

	public AdminUserServiceImpl(
			UserRepository userRepository,
			CurrentUserService currentUserService,
			RefreshTokenService refreshTokenService
	) {
		this.userRepository = userRepository;
		this.currentUserService = currentUserService;
		this.refreshTokenService = refreshTokenService;
	}

	@Override
	@Transactional(readOnly = true)
	public AdminUserListResponse getUsers(
			String keyword,
			String status,
			String role,
			int page,
			int size
	) {
		validatePagination(page, size);

		Specification<User> specification = (root, query, criteriaBuilder) ->
				criteriaBuilder.conjunction();

		if (StringUtils.hasText(keyword)) {
			var normalizedKeyword = "%" + keyword.trim().toLowerCase(Locale.ROOT) + "%";
			specification = specification.and((root, query, criteriaBuilder) ->
					criteriaBuilder.or(
							criteriaBuilder.like(
									criteriaBuilder.lower(root.get("fullName")),
									normalizedKeyword
							),
							criteriaBuilder.like(
									criteriaBuilder.lower(root.get("email")),
									normalizedKeyword
							)
					));
		}

		if (StringUtils.hasText(status)) {
			var accountStatus = parseStatus(status, true);
			specification = specification.and((root, query, criteriaBuilder) ->
					criteriaBuilder.equal(root.get("status"), accountStatus));
		}

		if (StringUtils.hasText(role)) {
			var userRole = parseRole(role);
			specification = specification.and((root, query, criteriaBuilder) ->
					criteriaBuilder.equal(root.get("role"), userRole));
		}

		var pageable = PageRequest.of(
				page,
				size,
				Sort.by(Sort.Direction.DESC, "createAt")
		);
		var result = userRepository.findAll(specification, pageable);

		return new AdminUserListResponse(
				result.getContent().stream().map(this::toResponse).toList(),
				result.getNumber(),
				result.getSize(),
				result.getTotalElements(),
				result.getTotalPages()
		);
	}

	@Override
	@Transactional(readOnly = true)
	public AdminUserResponse getUser(Long userId) {
		return toResponse(findUser(userId));
	}

	@Override
	@Transactional
	public AdminUserResponse updateStatus(Long userId, String status) {
		var newStatus = parseStatus(status, false);
		var user = findUser(userId);
		var currentAdminId = currentUserService.getCurrentUserId();

		if (currentAdminId.equals(userId) && newStatus == AccountStatus.BLOCKED) {
			throw new IllegalArgumentException("You cannot block your own account");
		}

		user.setStatus(newStatus);
		if (newStatus == AccountStatus.ACTIVE) {
			user.setVerifiedStatus(true);
		} else {
			refreshTokenService.revokeAll(userId);
		}

		return toResponse(userRepository.save(user));
	}

	private User findUser(Long userId) {
		if (userId == null) {
			throw new IllegalArgumentException("User ID is required");
		}
		return userRepository.findById(userId)
				.orElseThrow(() -> new ResourceNotFoundException("User not found"));
	}

	private AccountStatus parseStatus(String status, boolean allowPending) {
		if (!StringUtils.hasText(status)) {
			throw new IllegalArgumentException("Status is required");
		}
		try {
			var parsed = AccountStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
			if (!allowPending && parsed == AccountStatus.PENDING) {
				throw new IllegalArgumentException("Status must be ACTIVE or BLOCKED");
			}
			return parsed;
		} catch (IllegalArgumentException exception) {
			throw new IllegalArgumentException(
					allowPending
							? "Status must be PENDING, ACTIVE, or BLOCKED"
							: "Status must be ACTIVE or BLOCKED"
			);
		}
	}

	private Role parseRole(String role) {
		try {
			return Role.valueOf(role.trim().toUpperCase(Locale.ROOT));
		} catch (RuntimeException exception) {
			throw new IllegalArgumentException("Role must be USER or ADMIN");
		}
	}

	private void validatePagination(int page, int size) {
		if (page < 0) {
			throw new IllegalArgumentException("Page must be greater than or equal to 0");
		}
		if (size < 1 || size > MAX_PAGE_SIZE) {
			throw new IllegalArgumentException("Size must be between 1 and " + MAX_PAGE_SIZE);
		}
	}

	private AdminUserResponse toResponse(User user) {
		return new AdminUserResponse(
				user.getUserId(),
				user.getFullName(),
				user.getEmail(),
				user.getProvider(),
				user.getRole(),
				user.getStatus(),
				user.isVerifiedStatus(),
				user.getBio(),
				user.getCreateAt(),
				user.getUpdatedAt()
		);
	}
}
