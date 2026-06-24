package com.shopeasy.service;

import com.shopeasy.dto.request.ProductRequest;
import com.shopeasy.dto.response.ProductResponse;

import java.util.List;

public interface ProductService {
    List<ProductResponse> getAll();
    List<ProductResponse> getByCategory(Long categoryId);
    ProductResponse getById(Long id);
    ProductResponse create(ProductRequest request);
    ProductResponse update(Long id, ProductRequest request);
    void delete(Long id);
}
