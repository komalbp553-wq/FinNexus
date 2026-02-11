package com.finnexus.service.impl;

import com.finnexus.domain.dto.AdminStatsResponse;
import com.finnexus.domain.dto.AdminUserResponse;
import com.finnexus.domain.entity.User;
import com.finnexus.domain.enums.TradeStatus;
import com.finnexus.repository.OrderRepository;
import com.finnexus.repository.TradeRepository;
import com.finnexus.repository.UserRepository;
import com.finnexus.service.AdminService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AdminServiceImpl implements AdminService {
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final TradeRepository tradeRepository;

    public AdminServiceImpl(UserRepository userRepository, OrderRepository orderRepository, TradeRepository tradeRepository) {
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.tradeRepository = tradeRepository;
    }

    @Override
    public List<AdminUserResponse> listUsers() {
        return userRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public AdminStatsResponse stats() {
        long totalUsers = userRepository.count();
        long totalOrders = orderRepository.count();
        long totalTrades = tradeRepository.count();
        long openTrades = tradeRepository.findAll().stream().filter(t -> t.getStatus() == TradeStatus.OPEN).count();
        return new AdminStatsResponse(totalUsers, totalTrades, totalOrders, openTrades);
    }

    private AdminUserResponse toResponse(User user) {
        return new AdminUserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole().name(), user.isEnabled());
    }
}
