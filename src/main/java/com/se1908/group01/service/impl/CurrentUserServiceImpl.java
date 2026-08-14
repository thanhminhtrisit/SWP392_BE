package com.se1908.group01.service.impl;

import com.se1908.group01.entity.User;
import com.se1908.group01.repository.UserRepository;
import com.se1908.group01.service.CurrentUserService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

@Service
/**
 * Xác định user id của application đã xác thực từ Spring Security context.
 * Ownership upload và subscription check dùng id này thay vì tin owner id do client gửi lên.
 */
public class CurrentUserServiceImpl implements CurrentUserService {

	private final UserRepository userRepository;

	public CurrentUserServiceImpl(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	@Override
	public Long getCurrentUserId() {
		// Từ chối request upload không tạo được security context đã xác thực bằng JWT.
		var authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication == null
				|| !authentication.isAuthenticated()
				|| authentication instanceof AnonymousAuthenticationToken) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication is required");
		}

		var email = authentication.getName();
		if (!StringUtils.hasText(email)) {
			throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user email is missing");
		}

		return userRepository.findByEmail(email)
				.map(User::getUserId)
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authenticated user not found"));
	}
}
