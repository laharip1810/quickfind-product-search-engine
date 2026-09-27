package com.quickfind.mapper;

import com.quickfind.dto.response.ColorResponse;
import com.quickfind.dto.response.ProductDetailResponse;
import com.quickfind.dto.response.ProductSummaryResponse;
import com.quickfind.entity.Category;
import com.quickfind.entity.Color;
import com.quickfind.entity.Product;
import com.quickfind.recommendation.RecommendationCandidate;
import com.quickfind.search.SearchDocument;
import com.quickfind.util.SizeOrder;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Entity → DTO / algorithm-input conversions. Must be called inside a transaction,
 * because it touches lazy associations (brand, category, colors, sizes).
 */
@Component
public class ProductMapper {

    public ProductSummaryResponse toSummary(Product p) {
        Category sub = p.getCategory();
        return new ProductSummaryResponse(
                p.getId(),
                p.getName(),
                p.getBrand().getName(),
                topLevelName(sub),
                sub.getName(),
                p.getPrice(),
                p.getDiscountPercent(),
                p.getSalePrice(),
                p.getRating(),
                p.getReviewCount(),
                p.getStockQuantity(),
                p.isInStock(),
                p.getPopularityScore(),
                colors(p),
                p.getImageUrl());
    }

    public ProductDetailResponse toDetail(Product p) {
        Category sub = p.getCategory();
        Category parent = sub.getParent();
        return new ProductDetailResponse(
                p.getId(),
                p.getName(),
                p.getDescription(),
                p.getBrand().getId(),
                p.getBrand().getName(),
                parent == null ? sub.getId() : parent.getId(),
                topLevelName(sub),
                sub.getId(),
                sub.getName(),
                p.getPrice(),
                p.getDiscountPercent(),
                p.getSalePrice(),
                p.getRating(),
                p.getReviewCount(),
                p.getStockQuantity(),
                p.isInStock(),
                p.getPopularityScore(),
                colors(p),
                p.getSizes().stream().sorted(SizeOrder.COMPARATOR).toList(),
                p.getImageUrl(),
                p.getVersion(),
                p.getCreatedAt(),
                p.getUpdatedAt());
    }

    public SearchDocument toSearchDocument(Product p) {
        Category sub = p.getCategory();
        return new SearchDocument(
                p.getId(),
                p.getName(),
                p.getDescription(),
                p.getBrand().getName(),
                sub.getName(),
                sub.getParent() == null ? null : sub.getParent().getName(),
                p.getColors().stream().map(Color::getName).toList(),
                p.getSalePrice(),
                p.getRating().doubleValue(),
                p.getReviewCount(),
                p.getPopularityScore(),
                p.isInStock());
    }

    public RecommendationCandidate toCandidate(Product p) {
        Category sub = p.getCategory();
        return new RecommendationCandidate(
                p.getId(),
                sub.getId(),
                sub.getParent() == null ? null : sub.getParent().getId(),
                p.getBrand().getId(),
                p.getSalePrice().doubleValue(),
                p.getColors().stream().map(Color::getId).collect(Collectors.toSet()),
                p.getPopularityScore(),
                p.getRating().doubleValue());
    }

    public ColorResponse toColor(Color color) {
        return new ColorResponse(color.getId(), color.getName(), color.getHexCode());
    }

    private List<ColorResponse> colors(Product p) {
        return p.getColors().stream()
                .sorted(Comparator.comparing(Color::getName))
                .map(this::toColor)
                .toList();
    }

    private static String topLevelName(Category sub) {
        return sub.getParent() == null ? sub.getName() : sub.getParent().getName();
    }
}
