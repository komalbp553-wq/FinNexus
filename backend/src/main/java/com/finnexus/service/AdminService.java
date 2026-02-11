package com.finnexus.service;

import com.finnexus.domain.dto.AdminStatsResponse;
import com.finnexus.domain.dto.AdminUserResponse;

import java.util.List;

public interface AdminService {
    List<AdminUserResponse> listUsers();
    AdminStatsResponse stats();
}
