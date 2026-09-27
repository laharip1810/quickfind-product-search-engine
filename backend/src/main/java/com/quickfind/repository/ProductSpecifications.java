package com.quickfind.repository;

import com.quickfind.entity.Brand;
import com.quickfind.entity.Category;
import com.quickfind.entity.Color;
import com.quickfind.entity.Product;
import com.quickfind.search.MatchMode;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

/**
 * Builds the WHERE clauses for search, browsing and recommendation candidates with the
 * JPA Criteria API. Every value is bound as a parameter, never concatenated into SQL.
 *
 * <p>Filters (category, brand, price, rating, stock) map onto indexed columns. Color and
 * size filters use EXISTS subqueries instead of joins, so a product with three matching
 * colors is still returned once and SQL pagination stays correct.
 */
public final class ProductSpecifications {

    private ProductSpecifications() {
    }

    /** Structured filters. Empty/null arguments mean "no filter". */
    public static Specification<Product> filteredBy(Collection<Long> categoryIds,
                                                    Collection<Long> brandIds,
                                                    Collection<Long> colorIds,
                                                    String productSize,
                                                    BigDecimal minPrice,
                                                    BigDecimal maxPrice,
                                                    BigDecimal minRating,
                                                    boolean inStockOnly) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (categoryIds != null && !categoryIds.isEmpty()) {
                predicates.add(root.get("category").get("id").in(categoryIds));
            }
            if (brandIds != null && !brandIds.isEmpty()) {
                predicates.add(root.get("brand").get("id").in(brandIds));
            }
            if (colorIds != null && !colorIds.isEmpty()) {
                predicates.add(hasAnyColor(root, query, cb, colorIds));
            }
            if (productSize != null && !productSize.isBlank()) {
                predicates.add(hasSize(root, query, cb, productSize));
            }
            if (minPrice != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("salePrice"), minPrice));
            }
            if (maxPrice != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("salePrice"), maxPrice));
            }
            if (minRating != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("rating"), minRating));
            }
            if (inStockOnly) {
                predicates.add(cb.greaterThan(root.get("stockQuantity"), 0));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * Candidate retrieval for a text query. A token matches if it occurs in the name,
     * description, brand, subcategory, parent category or a color name. ALL_TERMS
     * requires every token to match somewhere; ANY_TERM requires at least one.
     *
     * <p>This is a deliberately broad substring match (LIKE '%token%'): the database only
     * narrows the candidate set, and {@code RelevanceScorer} then decides precisely
     * (word-prefix matching) and ranks the results.
     */
    public static Specification<Product> matchesText(List<String> tokens, MatchMode mode) {
        return (root, query, cb) -> {
            if (tokens == null || tokens.isEmpty()) {
                return cb.conjunction();
            }
            Join<Product, Brand> brand = root.join("brand", JoinType.INNER);
            Join<Product, Category> category = root.join("category", JoinType.INNER);
            Join<Category, Category> parent = category.join("parent", JoinType.LEFT);

            List<Predicate> perToken = new ArrayList<>();
            for (String token : tokens) {
                String pattern = "%" + token.toLowerCase(Locale.ROOT) + "%";
                perToken.add(cb.or(
                        cb.like(cb.lower(root.get("name")), pattern),
                        cb.like(cb.lower(root.get("description")), pattern),
                        cb.like(cb.lower(brand.get("name")), pattern),
                        cb.like(cb.lower(category.get("name")), pattern),
                        cb.like(cb.lower(parent.get("name")), pattern),
                        hasColorNameLike(root, query, cb, pattern)));
            }
            Predicate[] array = perToken.toArray(new Predicate[0]);
            return mode == MatchMode.ANY_TERM ? cb.or(array) : cb.and(array);
        };
    }

    public static Specification<Product> inStock() {
        return (root, query, cb) -> cb.greaterThan(root.get("stockQuantity"), 0);
    }

    /**
     * Recommendation candidates for a source product: in stock, not the source itself, and
     * related through the parent category, a complementary subcategory, the brand, or
     * co-interaction.
     */
    public static Specification<Product> recommendationCandidates(Long sourceId,
                                                                  Long parentCategoryId,
                                                                  Collection<Long> relatedCategoryIds,
                                                                  Long brandId,
                                                                  Collection<Long> coInteractedIds) {
        return (root, query, cb) -> {
            Join<Product, Category> category = root.join("category", JoinType.INNER);
            List<Predicate> related = new ArrayList<>();
            if (parentCategoryId != null) {
                related.add(cb.equal(category.get("parent").get("id"), parentCategoryId));
            }
            if (relatedCategoryIds != null && !relatedCategoryIds.isEmpty()) {
                related.add(category.get("id").in(relatedCategoryIds));
            }
            if (brandId != null) {
                related.add(cb.equal(root.get("brand").get("id"), brandId));
            }
            if (coInteractedIds != null && !coInteractedIds.isEmpty()) {
                related.add(root.get("id").in(coInteractedIds));
            }
            Predicate relatedAny = related.isEmpty() ? cb.disjunction() : cb.or(related.toArray(new Predicate[0]));
            return cb.and(
                    cb.notEqual(root.get("id"), sourceId),
                    cb.greaterThan(root.get("stockQuantity"), 0),
                    relatedAny);
        };
    }

    private static Predicate hasAnyColor(Root<Product> root, CriteriaQuery<?> query, CriteriaBuilder cb,
                                         Collection<Long> colorIds) {
        Subquery<Long> sub = query.subquery(Long.class);
        Root<Product> p = sub.from(Product.class);
        Join<Product, Color> color = p.join("colors");
        sub.select(p.get("id")).where(cb.equal(p.get("id"), root.get("id")), color.get("id").in(colorIds));
        return cb.exists(sub);
    }

    private static Predicate hasColorNameLike(Root<Product> root, CriteriaQuery<?> query, CriteriaBuilder cb,
                                              String pattern) {
        Subquery<Long> sub = query.subquery(Long.class);
        Root<Product> p = sub.from(Product.class);
        Join<Product, Color> color = p.join("colors");
        sub.select(p.get("id")).where(cb.equal(p.get("id"), root.get("id")),
                cb.like(cb.lower(color.get("name")), pattern));
        return cb.exists(sub);
    }

    private static Predicate hasSize(Root<Product> root, CriteriaQuery<?> query, CriteriaBuilder cb, String size) {
        Subquery<Long> sub = query.subquery(Long.class);
        Root<Product> p = sub.from(Product.class);
        Join<Product, String> sizes = p.join("sizes");
        sub.select(p.get("id")).where(cb.equal(p.get("id"), root.get("id")),
                cb.equal(cb.lower(sizes), size.trim().toLowerCase(Locale.ROOT)));
        return cb.exists(sub);
    }
}
