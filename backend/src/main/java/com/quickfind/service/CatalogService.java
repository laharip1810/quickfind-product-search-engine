package com.quickfind.service;

import com.quickfind.dto.response.BrandResponse;
import com.quickfind.dto.response.CategoryResponse;
import com.quickfind.dto.response.ColorResponse;
import com.quickfind.dto.response.UserResponse;
import com.quickfind.entity.Brand;
import com.quickfind.entity.Category;
import com.quickfind.entity.Color;
import com.quickfind.entity.User;
import com.quickfind.mapper.ProductMapper;
import com.quickfind.repository.BrandRepository;
import com.quickfind.repository.CategoryRepository;
import com.quickfind.repository.ColorRepository;
import com.quickfind.repository.ProductRepository;
import com.quickfind.util.SizeOrder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Reference data for filters and the admin form. */
@Service
public class CatalogService {

    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ColorRepository colorRepository;
    private final ProductRepository productRepository;
    private final ProductMapper mapper;
    private final DemoUserProvider demoUserProvider;

    public CatalogService(CategoryRepository categoryRepository, BrandRepository brandRepository,
                          ColorRepository colorRepository, ProductRepository productRepository,
                          ProductMapper mapper, DemoUserProvider demoUserProvider) {
        this.categoryRepository = categoryRepository;
        this.brandRepository = brandRepository;
        this.colorRepository = colorRepository;
        this.productRepository = productRepository;
        this.mapper = mapper;
        this.demoUserProvider = demoUserProvider;
    }

    /** Top-level categories with their subcategories, alphabetically. */
    @Transactional(readOnly = true)
    public List<CategoryResponse> categories() {
        List<Category> all = categoryRepository.findAllWithParent();
        Map<Long, List<CategoryResponse.SubcategoryResponse>> children = new LinkedHashMap<>();
        for (Category category : all) {
            if (category.getParent() != null) {
                children.computeIfAbsent(category.getParent().getId(), k -> new ArrayList<>())
                        .add(new CategoryResponse.SubcategoryResponse(category.getId(), category.getName()));
            }
        }
        return all.stream()
                .filter(c -> c.getParent() == null)
                .sorted(Comparator.comparing(Category::getName, String.CASE_INSENSITIVE_ORDER))
                .map(c -> new CategoryResponse(c.getId(), c.getName(), children.getOrDefault(c.getId(), List.of()).stream()
                        .sorted(Comparator.comparing(CategoryResponse.SubcategoryResponse::name, String.CASE_INSENSITIVE_ORDER))
                        .toList()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<BrandResponse> brands() {
        // Sorted in Java so the order does not depend on the database collation.
        return brandRepository.findAll().stream()
                .sorted(Comparator.comparing(Brand::getName, String.CASE_INSENSITIVE_ORDER))
                .map(b -> new BrandResponse(b.getId(), b.getName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ColorResponse> colors() {
        return colorRepository.findAll().stream()
                .sorted(Comparator.comparing(Color::getName, String.CASE_INSENSITIVE_ORDER))
                .map(mapper::toColor)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> sizes() {
        return productRepository.findDistinctSizes().stream().sorted(SizeOrder.COMPARATOR).toList();
    }

    @Transactional(readOnly = true)
    public UserResponse demoUser() {
        User user = demoUserProvider.demoUser();
        return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getRole().name());
    }
}
