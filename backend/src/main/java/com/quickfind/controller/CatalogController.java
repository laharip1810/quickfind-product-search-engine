package com.quickfind.controller;

import com.quickfind.dto.response.BrandResponse;
import com.quickfind.dto.response.CategoryResponse;
import com.quickfind.dto.response.ColorResponse;
import com.quickfind.dto.response.UserResponse;
import com.quickfind.service.CatalogService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** Reference data used by the filter sidebar and the admin form. */
@RestController
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/api/categories")
    public List<CategoryResponse> categories() {
        return catalogService.categories();
    }

    @GetMapping("/api/brands")
    public List<BrandResponse> brands() {
        return catalogService.brands();
    }

    @GetMapping("/api/colors")
    public List<ColorResponse> colors() {
        return catalogService.colors();
    }

    @GetMapping("/api/sizes")
    public List<String> sizes() {
        return catalogService.sizes();
    }

    /** The demo shopper used when no userId is sent (the MVP has no login). */
    @GetMapping("/api/users/demo")
    public UserResponse demoUser() {
        return catalogService.demoUser();
    }
}
