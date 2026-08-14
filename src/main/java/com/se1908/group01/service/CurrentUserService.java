package com.se1908.group01.service;

/** Xác định database user tương ứng với request hiện đã được xác thực. */
public interface CurrentUserService {

	Long getCurrentUserId();
}
