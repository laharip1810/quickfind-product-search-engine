package com.quickfind.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "products")
public class Product {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 2000)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "brand_id", nullable = false)
    private Brand brand;

    /** Always a subcategory; the top-level category is {@code category.getParent()}. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "discount_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal discountPercent = BigDecimal.ZERO;

    @Column(name = "sale_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal salePrice;

    @Column(nullable = false, precision = 2, scale = 1)
    private BigDecimal rating = BigDecimal.ZERO;

    @Column(name = "review_count", nullable = false)
    private int reviewCount;

    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity;

    @Column(name = "popularity_score", nullable = false)
    private double popularityScore;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "product_sizes", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "size_label", nullable = false, length = 20)
    @BatchSize(size = 100)
    private Set<String> sizes = new LinkedHashSet<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "product_colors",
            joinColumns = @JoinColumn(name = "product_id"),
            inverseJoinColumns = @JoinColumn(name = "color_id"))
    @BatchSize(size = 100)
    private Set<Color> colors = new LinkedHashSet<>();

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public Product() {
    }

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
        recalculateSalePrice();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now(ZoneOffset.UTC);
        recalculateSalePrice();
    }

    /** salePrice = price * (1 - discount / 100), rounded half-up to 2 decimals. */
    public void recalculateSalePrice() {
        if (price == null) {
            return;
        }
        BigDecimal discount = discountPercent == null ? BigDecimal.ZERO : discountPercent;
        BigDecimal multiplier = HUNDRED.subtract(discount).divide(HUNDRED, 6, RoundingMode.HALF_UP);
        salePrice = price.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }

    public boolean isInStock() {
        return stockQuantity > 0;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Brand getBrand() {
        return brand;
    }

    public void setBrand(Brand brand) {
        this.brand = brand;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
        recalculateSalePrice();
    }

    public BigDecimal getDiscountPercent() {
        return discountPercent;
    }

    public void setDiscountPercent(BigDecimal discountPercent) {
        this.discountPercent = discountPercent == null ? BigDecimal.ZERO : discountPercent;
        recalculateSalePrice();
    }

    public BigDecimal getSalePrice() {
        return salePrice;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public void setRating(BigDecimal rating) {
        this.rating = rating == null ? BigDecimal.ZERO : rating;
    }

    public int getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(int reviewCount) {
        this.reviewCount = reviewCount;
    }

    public int getStockQuantity() {
        return stockQuantity;
    }

    public void setStockQuantity(int stockQuantity) {
        this.stockQuantity = stockQuantity;
    }

    public double getPopularityScore() {
        return popularityScore;
    }

    public void setPopularityScore(double popularityScore) {
        this.popularityScore = popularityScore;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Set<String> getSizes() {
        return sizes;
    }

    public void replaceSizes(Set<String> newSizes) {
        sizes.clear();
        if (newSizes != null) {
            sizes.addAll(newSizes);
        }
    }

    public Set<Color> getColors() {
        return colors;
    }

    public void replaceColors(Set<Color> newColors) {
        colors.clear();
        if (newColors != null) {
            colors.addAll(newColors);
        }
    }

    public Long getVersion() {
        return version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
