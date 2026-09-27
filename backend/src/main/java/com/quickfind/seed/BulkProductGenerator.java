package com.quickfind.seed;

import com.quickfind.config.QuickFindProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;

/**
 * Generates N synthetic products for benchmarking (profile "generate").
 *
 * <p>Uses JDBC batch inserts instead of JPA: 100,000 products with sizes and colors is
 * about 600,000 rows, and going through the persistence context would be much slower
 * and memory-hungry. Output is deterministic for a given starting id (seeded Random),
 * so benchmark runs are reproducible. Runs after {@link DataSeeder} (which creates the
 * brands, categories and colors) and then exits the JVM.
 *
 * <p>Usage: scripts/generate-test-data.(sh|ps1) 10000
 */
@Component
@Profile("generate")
@Order(2)
public class BulkProductGenerator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(BulkProductGenerator.class);

    private static final String[] ADJECTIVES = {"Swift", "Aero", "Urban", "Classic", "Ultra", "Flex", "Prime",
            "Core", "Edge", "Trail", "Street", "Nova", "Pulse", "Zen", "Storm", "Vista"};
    private static final String[] SERIES = {"Pro", "Lite", "Max", "Plus", "Air", "Fit", "Sport", "Elite"};
    private static final String[] FEATURES = {"a breathable build", "reinforced stitching", "a lightweight feel",
            "recycled materials", "a water-resistant finish", "extra cushioning", "a secure fit",
            "quick-dry fabric"};
    private static final int[] DISCOUNTS = {0, 0, 5, 10, 15, 20, 30, 40, 50};
    private static final Map<String, String> NOUNS = Map.of(
            CatalogSeedData.RUNNING_SHOES, "Running Shoes", CatalogSeedData.SNEAKERS, "Sneakers",
            CatalogSeedData.T_SHIRTS, "T-Shirt", CatalogSeedData.JEANS, "Jeans",
            CatalogSeedData.JACKETS, "Jacket", CatalogSeedData.SHORTS, "Shorts",
            CatalogSeedData.TRACK_PANTS, "Track Pants", CatalogSeedData.WATCHES, "Watch",
            CatalogSeedData.BAGS, "Backpack", CatalogSeedData.SPORTS_ACCESSORIES, "Training Gear");

    private final JdbcTemplate jdbc;
    private final ConfigurableApplicationContext context;
    private final QuickFindProperties properties;

    public BulkProductGenerator(JdbcTemplate jdbc, ConfigurableApplicationContext context,
                                QuickFindProperties properties) {
        this.jdbc = jdbc;
        this.context = context;
        this.properties = properties;
    }

    private record Subcategory(long id, String name) {
    }

    @Override
    public void run(ApplicationArguments args) {
        int count = properties.generate() == null ? 10_000 : properties.generate().count();
        int batchSize = properties.generate() == null ? 1_000 : properties.generate().batchSize();
        long start = System.nanoTime();

        List<Map<String, Object>> brandRows = jdbc.queryForList("SELECT id, name FROM brands");
        List<Subcategory> subcategories = jdbc.query("SELECT id, name FROM categories WHERE parent_id IS NOT NULL",
                (rs, n) -> new Subcategory(rs.getLong("id"), rs.getString("name")));
        List<Long> colorIds = jdbc.queryForList("SELECT id FROM colors", Long.class);
        if (brandRows.isEmpty() || subcategories.isEmpty() || colorIds.isEmpty()) {
            throw new IllegalStateException("Brands, categories and colors must exist first (keep SEED_ENABLED=true)");
        }
        long nextId = jdbc.queryForObject("SELECT COALESCE(MAX(id), 0) FROM products", Long.class) + 1;
        Random random = new Random(42L + nextId);
        Timestamp now = Timestamp.valueOf(LocalDateTime.now(ZoneOffset.UTC));

        log.info("Generating {} products starting at id {} (batch size {})", count, nextId, batchSize);
        int generated = 0;
        while (generated < count) {
            int thisBatch = Math.min(batchSize, count - generated);
            List<Object[]> productRows = new ArrayList<>(thisBatch);
            List<Object[]> sizeRows = new ArrayList<>();
            List<Object[]> colorRows = new ArrayList<>();
            for (int i = 0; i < thisBatch; i++) {
                long id = nextId + generated + i;
                Map<String, Object> brand = brandRows.get(random.nextInt(brandRows.size()));
                Subcategory sub = subcategories.get(random.nextInt(subcategories.size()));
                String brandName = (String) brand.get("name");
                String adjective = ADJECTIVES[random.nextInt(ADJECTIVES.length)];
                String noun = NOUNS.getOrDefault(sub.name(), sub.name());
                String name = brandName + " " + adjective + " " + SERIES[random.nextInt(SERIES.length)] + " " + id + " " + noun;
                String description = adjective + " " + noun.toLowerCase(Locale.ROOT) + " by " + brandName + " with "
                        + FEATURES[random.nextInt(FEATURES.length)] + ". " + CatalogSeedData.subcategoryDescription(sub.name());

                BigDecimal price = BigDecimal.valueOf(priceFor(sub.name(), random));
                BigDecimal discount = BigDecimal.valueOf(DISCOUNTS[random.nextInt(DISCOUNTS.length)]);
                BigDecimal salePrice = price.multiply(BigDecimal.valueOf(100).subtract(discount))
                        .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                double rating = Math.round((3.0 + random.nextDouble() * 2.0) * 10) / 10.0;
                int reviews = random.nextInt(5000);
                int stock = random.nextInt(10) == 0 ? 0 : 1 + random.nextInt(300);

                productRows.add(new Object[]{id, name, description, ((Number) brand.get("id")).longValue(), sub.id(),
                        price, discount, salePrice, BigDecimal.valueOf(rating), reviews, stock,
                        CatalogSeedData.popularity(rating, reviews), now, now});
                for (String size : CatalogSeedData.defaultSizes(sub.name())) {
                    sizeRows.add(new Object[]{id, size});
                }
                Set<Long> chosenColors = new LinkedHashSet<>();
                int colorCount = 1 + random.nextInt(3);
                while (chosenColors.size() < colorCount) {
                    chosenColors.add(colorIds.get(random.nextInt(colorIds.size())));
                }
                chosenColors.forEach(colorId -> colorRows.add(new Object[]{id, colorId}));
            }
            jdbc.batchUpdate("""
                    INSERT INTO products (id, name, description, brand_id, category_id, price, discount_percent,
                        sale_price, rating, review_count, stock_quantity, popularity_score, version, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?, ?)
                    """, productRows);
            jdbc.batchUpdate("INSERT INTO product_sizes (product_id, size_label) VALUES (?, ?)", sizeRows);
            jdbc.batchUpdate("INSERT INTO product_colors (product_id, color_id) VALUES (?, ?)", colorRows);
            generated += thisBatch;
            log.info("Inserted {}/{} products", generated, count);
        }

        Long total = jdbc.queryForObject("SELECT COUNT(*) FROM products", Long.class);
        log.info("Done: generated {} products in {} ms; catalog now has {} products",
                count, (System.nanoTime() - start) / 1_000_000, total);
        System.exit(SpringApplication.exit(context, () -> 0));
    }

    private static int priceFor(String subcategory, Random random) {
        int[] range = switch (subcategory) {
            case CatalogSeedData.RUNNING_SHOES, CatalogSeedData.SNEAKERS -> new int[]{1500, 18000};
            case CatalogSeedData.WATCHES -> new int[]{1500, 45000};
            case CatalogSeedData.JACKETS -> new int[]{2000, 25000};
            case CatalogSeedData.BAGS -> new int[]{500, 12000};
            case CatalogSeedData.JEANS -> new int[]{1200, 7000};
            default -> new int[]{300, 4500};
        };
        return range[0] + random.nextInt(range[1] - range[0]);
    }
}
