package com.quickfind.repository;

import com.quickfind.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    /*
     * Brand and category (plus the parent category) are fetched eagerly with an entity graph,
     * because every response needs them. Sizes and colors are collections: fetching them
     * in the same query would break SQL pagination, so they are loaded in batches of 100
     * (@BatchSize on the entity) instead of one query per product.
     */

    @Override
    @EntityGraph(attributePaths = {"brand", "category", "category.parent"})
    Page<Product> findAll(Specification<Product> spec, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = {"brand", "category", "category.parent"})
    Page<Product> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"brand", "category", "category.parent"})
    Optional<Product> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"brand", "category", "category.parent"})
    List<Product> findAllByIdIn(Collection<Long> ids);

    boolean existsByBrandIdAndNameIgnoreCase(Long brandId, String name);

    boolean existsByBrandIdAndNameIgnoreCaseAndIdNot(Long brandId, String name, Long id);

    @Query("""
            select p.id as id, p.name as name, p.popularityScore as popularityScore,
                   b.name as brandName, c.name as categoryName
            from Product p join p.brand b join p.category c
            order by p.popularityScore desc, p.id asc
            """)
    List<ProductNameView> findAllNameViews();

    @Query("""
            select c.id as categoryId, parent.id as parentCategoryId, p.salePrice as salePrice
            from Product p join p.category c left join c.parent parent
            where p.stockQuantity > 0
            """)
    List<ProductPriceView> findInStockPriceViews();

    @Query(value = "select distinct size_label from product_sizes", nativeQuery = true)
    List<String> findDistinctSizes();

    /**
     * Atomic in-database increment, so concurrent interactions never lose updates and
     * no optimistic-lock version bump is needed for a counter.
     */
    @Modifying
    @Query("update Product p set p.popularityScore = p.popularityScore + :delta where p.id = :id")
    int incrementPopularity(@Param("id") Long id, @Param("delta") double delta);
}
