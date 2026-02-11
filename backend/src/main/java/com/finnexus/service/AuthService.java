package com.finnexus.service;

import com.finnexus.domain.dto.AuthResponse;
import com.finnexus.domain.dto.LoginRequest;
import com.finnexus.domain.dto.RegisterRequest;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
}
