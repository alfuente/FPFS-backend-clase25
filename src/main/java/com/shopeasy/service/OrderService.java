package com.shopeasy.service;

import com.shopeasy.dto.request.OrderStatusRequest;
import com.shopeasy.dto.response.OrderResponse;

import java.util.List;

public interface OrderService {
    OrderResponse createOrder(String userEmail);
    List<OrderResponse> getMyOrders(String userEmail);
    List<OrderResponse> getAll();
    OrderResponse getById(Long id);
    OrderResponse updateStatus(Long id, OrderStatusRequest request);
}
