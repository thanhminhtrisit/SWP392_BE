package com.se1908.group01.service;

import com.se1908.group01.dto.ChangePasswordRequest;
import com.se1908.group01.dto.UpdateUserProfileRequest;
import com.se1908.group01.dto.UserProfileResponse;
import java.io.IOException;
import org.springframework.web.multipart.MultipartFile;

public interface UserProfileService {

	UserProfileResponse getMyProfile();

	UserProfileResponse updateMyProfile(UpdateUserProfileRequest request);

	UserProfileResponse updateAvatar(MultipartFile file) throws IOException;

	void changePassword(ChangePasswordRequest request);
}
