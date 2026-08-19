package com.se1908.group01.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.se1908.group01.entity.User;
import com.se1908.group01.enums.AccountStatus;
import com.se1908.group01.enums.AuthProvider;
import com.se1908.group01.enums.Role;
import com.se1908.group01.repository.UserRepository;
import com.se1908.group01.service.CurrentUserService;
import com.se1908.group01.service.RefreshTokenService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceImplTest {

	@Mock
	private UserRepository userRepository;

	@Mock
	private CurrentUserService currentUserService;

	@Mock
	private RefreshTokenService refreshTokenService;

	private AdminUserServiceImpl service;

	@BeforeEach
	void setUp() {
		service = new AdminUserServiceImpl(
				userRepository,
				currentUserService,
				refreshTokenService
		);
	}

	@Test
	void getUsersReturnsPagedResult() {
		var user = user(2L, AccountStatus.ACTIVE);
		when(userRepository.findAll(
				any(Specification.class),
				any(Pageable.class)
		)).thenReturn(new PageImpl<>(List.of(user)));

		var response = service.getUsers(null, null, null, 0, 20);

		assertEquals(1, response.totalElements());
		assertEquals(2L, response.users().getFirst().userId());
	}

	@Test
	void blockUserRevokesRefreshTokens() {
		var user = user(2L, AccountStatus.ACTIVE);
		when(userRepository.findById(2L)).thenReturn(Optional.of(user));
		when(currentUserService.getCurrentUserId()).thenReturn(1L);
		when(userRepository.save(user)).thenReturn(user);

		var response = service.updateStatus(2L, "BLOCKED");

		assertEquals(AccountStatus.BLOCKED, response.status());
		verify(refreshTokenService).revokeAll(2L);
	}

	@Test
	void activateUserDoesNotRevokeRefreshTokens() {
		var user = user(2L, AccountStatus.BLOCKED);
		when(userRepository.findById(2L)).thenReturn(Optional.of(user));
		when(currentUserService.getCurrentUserId()).thenReturn(1L);
		when(userRepository.save(user)).thenReturn(user);

		var response = service.updateStatus(2L, "ACTIVE");

		assertEquals(AccountStatus.ACTIVE, response.status());
		verify(refreshTokenService, never()).revokeAll(2L);
	}

	@Test
	void adminCannotBlockOwnAccount() {
		var user = user(1L, AccountStatus.ACTIVE);
		when(userRepository.findById(1L)).thenReturn(Optional.of(user));
		when(currentUserService.getCurrentUserId()).thenReturn(1L);

		assertThrows(
				IllegalArgumentException.class,
				() -> service.updateStatus(1L, "BLOCKED")
		);
		verify(userRepository, never()).save(any(User.class));
	}

	private User user(Long id, AccountStatus status) {
		return User.builder()
				.userId(id)
				.fullName("Test User")
				.email("user@example.com")
				.provider(AuthProvider.LOCAL)
				.role(Role.USER)
				.status(status)
				.verifiedStatus(status == AccountStatus.ACTIVE)
				.createAt(LocalDateTime.now())
				.build();
	}
}
