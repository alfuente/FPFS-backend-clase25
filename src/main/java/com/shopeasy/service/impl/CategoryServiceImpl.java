package com.shopeasy.service.impl;

import com.shopeasy.dto.request.CategoryRequest;
import com.shopeasy.dto.response.CategoryResponse;
import com.shopeasy.exception.ResourceNotFoundException;
import com.shopeasy.model.Category;
import com.shopeasy.repository.CategoryRepository;
import com.shopeasy.service.CategoryService;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ModelMapper modelMapper;

    @Override
    public List<CategoryResponse> getAll() {
        return categoryRepository.findAll().stream()
                .map(c -> modelMapper.map(c, CategoryResponse.class))
                .collect(Collectors.toList());
    }

    @Override
    public CategoryResponse getById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con id: " + id));
        return modelMapper.map(category, CategoryResponse.class);
    }

    @Override
    public CategoryResponse create(CategoryRequest request) {
        Category category = new Category();
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        return modelMapper.map(categoryRepository.save(category), CategoryResponse.class);
    }

    @Override
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoría no encontrada con id: " + id));
        category.setName(request.getName());
        category.setDescription(request.getDescription());
        return modelMapper.map(categoryRepository.save(category), CategoryResponse.class);
    }

    @Override
    public void delete(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new ResourceNotFoundException("Categoría no encontrada con id: " + id);
        }
        categoryRepository.deleteById(id);
    }
}
