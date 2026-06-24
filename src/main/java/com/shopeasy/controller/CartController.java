package com.shopeasy.controller;

import com.shopeasy.dto.request.CartItemRequest;
import com.shopeasy.dto.response.CartItemResponse;
import com.shopeasy.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @GetMapping
    public ResponseEntity<List<CartItemResponse>> getCart(Principal principal) {
        return ResponseEntity.ok(cartService.getCart(principal.getName()));
    }

    @PostMapping
    public ResponseEntity<CartItemResponse> addItem(Principal principal,
                                                     @RequestBody CartItemRequest request) {
        return ResponseEntity.ok(cartService.addItem(principal.getName(), request));
    }

    @PutMapping("/{itemId}")
    public ResponseEntity<CartItemResponse> updateItem(Principal principal,
                                                        @PathVariable Long itemId,
                                                        @RequestParam int quantity) {
        return ResponseEntity.ok(cartService.updateItem(principal.getName(), itemId, quantity));
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> removeItem(Principal principal, @PathVariable Long itemId) {
        cartService.removeItem(principal.getName(), itemId);
        return ResponseEntity.noContent().build();
    }
}
