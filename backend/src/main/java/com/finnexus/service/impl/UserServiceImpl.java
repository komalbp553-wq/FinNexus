package com.finnexus.service.impl;

import com.finnexus.domain.dto.UserProfileResponse;
import com.finnexus.domain.entity.User;
import com.finnexus.exception.NotFoundException;
import com.finnexus.repository.UserRepository;
import com.finnexus.service.UserService;
import com.finnexus.util.SecurityUtil;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserProfileResponse getProfile() {
        String username = SecurityUtil.getCurrentUsername();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("User not found"));
        return new UserProfileResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole().name(), user.isEnabled());
    }

    @Override
    public void setUserEnabled(Long userId, boolean enabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        user.setEnabled(enabled);
        userRepository.save(user);
    }
}
