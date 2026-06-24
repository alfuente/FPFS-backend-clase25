package com.shopeasy.service.impl;

import com.shopeasy.dto.request.CartItemRequest;
import com.shopeasy.dto.response.CartItemResponse;
import com.shopeasy.exception.ResourceNotFoundException;
import com.shopeasy.model.CartItem;
import com.shopeasy.model.Product;
import com.shopeasy.model.User;
import com.shopeasy.repository.CartItemRepository;
import com.shopeasy.repository.ProductRepository;
import com.shopeasy.repository.UserRepository;
import com.shopeasy.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Override
    public List<CartItemResponse> getCart(String userEmail) {
        return cartItemRepository.findByUserEmail(userEmail).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public CartItemResponse addItem(String userEmail, CartItemRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado"));

        // Si ya existe el item en el carrito, solo incrementar la cantidad
        Optional<CartItem> existing = cartItemRepository.findByUserEmailAndProductId(userEmail, product.getId());

        CartItem cartItem;
        if (existing.isPresent()) {
            cartItem = existing.get();
            cartItem.setQuantity(cartItem.getQuantity() + request.getQuantity());
        } else {
            cartItem = new CartItem();
            cartItem.setUser(user);
            cartItem.setProduct(product);
            cartItem.setQuantity(request.getQuantity());
        }

        return toResponse(cartItemRepository.save(cartItem));
    }

    @Override
    public CartItemResponse updateItem(String userEmail, Long itemId, int quantity) {
        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item del carrito no encontrado"));

        if (!cartItem.getUser().getEmail().equals(userEmail)) {
            throw new IllegalArgumentException("No tienes permiso para modificar este item");
        }

        cartItem.setQuantity(quantity);
        return toResponse(cartItemRepository.save(cartItem));
    }

    @Override
    public void removeItem(String userEmail, Long itemId) {
        CartItem cartItem = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new ResourceNotFoundException("Item del carrito no encontrado"));

        if (!cartItem.getUser().getEmail().equals(userEmail)) {
            throw new IllegalArgumentException("No tienes permiso para eliminar este item");
        }

        cartItemRepository.delete(cartItem);
    }

    @Override
    @Transactional
    public void clearCart(String userEmail) {
        cartItemRepository.deleteAllByUserEmail(userEmail);
    }

    private CartItemResponse toResponse(CartItem item) {
        CartItemResponse response = new CartItemResponse();
        response.setId(item.getId());
        response.setProductId(item.getProduct().getId());
        response.setProductName(item.getProduct().getName());
        response.setProductImageUrl(item.getProduct().getImageUrl());
        response.setProductPrice(item.getProduct().getPrice());
        response.setQuantity(item.getQuantity());
        response.setSubtotal(item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        return response;
    }
}
