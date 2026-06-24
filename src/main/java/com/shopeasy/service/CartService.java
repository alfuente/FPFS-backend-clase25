package com.shopeasy.service;

import com.shopeasy.dto.request.CartItemRequest;
import com.shopeasy.dto.response.CartItemResponse;

import java.util.List;

public interface CartService {
    List<CartItemResponse> getCart(String userEmail);
    CartItemResponse addItem(String userEmail, CartItemRequest request);
    CartItemResponse updateItem(String userEmail, Long itemId, int quantity);
    void removeItem(String userEmail, Long itemId);
    void clearCart(String userEmail);
}
