package com.quickfind.seed;

import com.quickfind.entity.Brand;
import com.quickfind.entity.Category;
import com.quickfind.entity.Color;
import com.quickfind.entity.InteractionType;
import com.quickfind.entity.Product;
import com.quickfind.entity.Role;
import com.quickfind.entity.User;
import com.quickfind.entity.UserInteraction;
import com.quickfind.repository.BrandRepository;
import com.quickfind.repository.CategoryRepository;
import com.quickfind.repository.ColorRepository;
import com.quickfind.repository.ProductRepository;
import com.quickfind.repository.UserInteractionRepository;
import com.quickfind.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Inserts {@link CatalogSeedData} into an empty database, in one transaction. */
@Service
public class SeedService {

    private static final Logger log = LoggerFactory.getLogger(SeedService.class);

    private final ProductRepository productRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final ColorRepository colorRepository;
    private final UserRepository userRepository;
    private final UserInteractionRepository interactionRepository;
    private final Clock clock;

    public SeedService(ProductRepository productRepository, BrandRepository brandRepository,
                       CategoryRepository categoryRepository, ColorRepository colorRepository,
                       UserRepository userRepository, UserInteractionRepository interactionRepository, Clock clock) {
        this.productRepository = productRepository;
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
        this.colorRepository = colorRepository;
        this.userRepository = userRepository;
        this.interactionRepository = interactionRepository;
        this.clock = clock;
    }

    /** @return true if data was inserted, false if the catalog already had products */
    @Transactional
    public boolean seedIfEmpty() {
        if (productRepository.count() > 0) {
            log.info("Catalog already has products; skipping seed data");
            return false;
        }
        long start = System.nanoTime();

        Map<String, Category> subcategories = new HashMap<>();
        CatalogSeedData.CATEGORIES.forEach((parentName, children) -> {
            Category parent = categoryRepository.save(new Category(parentName, null));
            for (String child : children) {
                subcategories.put(child, categoryRepository.save(new Category(child, parent)));
            }
        });

        Map<String, Brand> brands = new HashMap<>();
        for (String name : CatalogSeedData.BRANDS) {
            brands.put(name, brandRepository.save(new Brand(name)));
        }

        Map<String, Color> colors = new HashMap<>();
        CatalogSeedData.COLORS.forEach((name, hex) -> colors.put(name, colorRepository.save(new Color(name, hex))));

        Map<String, User> users = new HashMap<>();
        for (CatalogSeedData.SeedUser seedUser : CatalogSeedData.USERS) {
            User user = userRepository.findByEmail(seedUser.email())
                    .orElseGet(() -> userRepository.save(
                            new User(seedUser.email(), seedUser.displayName(), Role.valueOf(seedUser.role()))));
            users.put(seedUser.email(), user);
        }

        Map<String, Product> productsByName = new HashMap<>();
        List<Product> products = new ArrayList<>();
        for (CatalogSeedData.SeedProduct seed : CatalogSeedData.products()) {
            Product product = new Product();
            product.setName(seed.name());
            product.setDescription(seed.description());
            product.setBrand(require(brands, seed.brand(), "brand"));
            product.setCategory(require(subcategories, seed.subcategory(), "subcategory"));
            product.setPrice(BigDecimal.valueOf(seed.price()));
            product.setDiscountPercent(BigDecimal.valueOf(seed.discountPercent()));
            product.setRating(BigDecimal.valueOf(seed.rating()));
            product.setReviewCount(seed.reviewCount());
            product.setStockQuantity(seed.stock());
            product.setPopularityScore(CatalogSeedData.popularity(seed.rating(), seed.reviewCount()));
            Set<Color> productColors = new LinkedHashSet<>();
            seed.colors().forEach(c -> productColors.add(require(colors, c, "color")));
            product.replaceColors(productColors);
            product.replaceSizes(new LinkedHashSet<>(seed.sizes()));
            products.add(product);
            productsByName.put(seed.name(), product);
        }
        productRepository.saveAll(products);

        LocalDateTime now = LocalDateTime.now(clock);
        List<UserInteraction> interactions = new ArrayList<>();
        for (CatalogSeedData.SeedInteraction seed : CatalogSeedData.interactions()) {
            Product product = seed.productName() == null ? null : require(productsByName, seed.productName(), "product");
            UserInteraction interaction = new UserInteraction(require(users, seed.userEmail(), "user"), product,
                    InteractionType.valueOf(seed.type()), seed.query());
            interaction.setCreatedAt(now.minusHours(seed.hoursAgo()));
            interactions.add(interaction);
        }
        interactionRepository.saveAll(interactions);

        log.info("Seeded {} products, {} brands, {} categories, {} users and {} interactions in {} ms",
                products.size(), brands.size(), subcategories.size(), users.size(), interactions.size(),
                (System.nanoTime() - start) / 1_000_000);
        return true;
    }

    private static <T> T require(Map<String, T> map, String key, String kind) {
        T value = map.get(key);
        if (value == null) {
            throw new IllegalStateException("Seed data references unknown " + kind + " '" + key + "'");
        }
        return value;
    }
}
