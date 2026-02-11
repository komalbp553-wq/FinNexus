package com.finnexus.service;

import com.finnexus.domain.dto.CreateOrderRequest;
import com.finnexus.domain.dto.OrderResponse;

import java.util.List;

public interface OrderService {
    OrderResponse placeOrder(CreateOrderRequest request);
    OrderResponse cancelOrder(Long orderId);
    List<OrderResponse> getOrders();
    void processPendingOrders();
}
