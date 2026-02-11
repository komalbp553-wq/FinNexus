package com.finnexus.service;

import com.finnexus.domain.dto.UserProfileResponse;

public interface UserService {
    UserProfileResponse getProfile();
    void setUserEnabled(Long userId, boolean enabled);
}
