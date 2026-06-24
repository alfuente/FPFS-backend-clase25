package com.shopeasy.service.impl;

import com.shopeasy.dto.request.OrderStatusRequest;
import com.shopeasy.dto.response.OrderItemResponse;
import com.shopeasy.dto.response.OrderResponse;
import com.shopeasy.exception.ResourceNotFoundException;
import com.shopeasy.model.*;
import com.shopeasy.repository.CartItemRepository;
import com.shopeasy.repository.OrderRepository;
import com.shopeasy.repository.ProductRepository;
import com.shopeasy.repository.UserRepository;
import com.shopeasy.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CartItemRepository cartItemRepository;

    @Autowired
    private ProductRepository productRepository;

    @Override
    @Transactional
    public OrderResponse createOrder(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        List<CartItem> cartItems = cartItemRepository.findByUserEmail(userEmail);

        if (cartItems.isEmpty()) {
            throw new IllegalArgumentException("El carrito está vacío");
        }

        // Validar stock y calcular total
        BigDecimal total = BigDecimal.ZERO;
        for (CartItem item : cartItems) {
            if (item.getProduct().getStock() < item.getQuantity()) {
                throw new IllegalArgumentException(
                    "Stock insuficiente para: " + item.getProduct().getName()
                );
            }
            total = total.add(item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }

        // Crear la orden
        Order order = new Order();
        order.setUser(user);
        order.setTotal(total);
        order.setStatus(OrderStatus.PENDIENTE);

        // Crear los items de la orden y descontar stock
        for (CartItem cartItem : cartItems) {
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(cartItem.getProduct());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setUnitPrice(cartItem.getProduct().getPrice());
            order.getItems().add(orderItem);

            // Descontar del stock
            Product product = cartItem.getProduct();
            product.setStock(product.getStock() - cartItem.getQuantity());
            productRepository.save(product);
        }

        Order savedOrder = orderRepository.save(order);

        // Vaciar el carrito
        cartItemRepository.deleteAllByUserEmail(userEmail);

        return toResponse(savedOrder);
    }

    @Override
    public List<OrderResponse> getMyOrders(String userEmail) {
        return orderRepository.findByUserEmailOrderByCreatedAtDesc(userEmail).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderResponse> getAll() {
        return orderRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public OrderResponse getById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada con id: " + id));
        return toResponse(order);
    }

    @Override
    public OrderResponse updateStatus(Long id, OrderStatusRequest request) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Orden no encontrada con id: " + id));
        order.setStatus(request.getStatus());
        return toResponse(orderRepository.save(order));
    }

    private OrderResponse toResponse(Order order) {
        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setUserEmail(order.getUser().getEmail());
        response.setUserName(order.getUser().getName());
        response.setStatus(order.getStatus());
        response.setTotal(order.getTotal());
        response.setCreatedAt(order.getCreatedAt());

        List<OrderItemResponse> items = order.getItems().stream().map(item -> {
            OrderItemResponse itemResponse = new OrderItemResponse();
            itemResponse.setId(item.getId());
            itemResponse.setProductId(item.getProduct().getId());
            itemResponse.setProductName(item.getProduct().getName());
            itemResponse.setQuantity(item.getQuantity());
            itemResponse.setUnitPrice(item.getUnitPrice());
            itemResponse.setSubtotal(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
            return itemResponse;
        }).collect(Collectors.toList());

        response.setItems(items);
        return response;
    }
}
