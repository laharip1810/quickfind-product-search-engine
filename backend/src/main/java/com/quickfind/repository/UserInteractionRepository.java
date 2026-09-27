package com.quickfind.repository;

import com.quickfind.entity.InteractionType;
import com.quickfind.entity.UserInteraction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserInteractionRepository extends JpaRepository<UserInteraction, Long> {

    @EntityGraph(attributePaths = {"product"})
    List<UserInteraction> findByUser_IdOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);

    /** Product ids the user wishlisted, newest first (may contain duplicates). */
    @Query("""
            select i.product.id from UserInteraction i
            where i.user.id = :userId and i.eventType = :type and i.product is not null
            order by i.createdAt desc, i.id desc
            """)
    List<Long> findProductIdsByUserAndType(@Param("userId") Long userId,
                                           @Param("type") InteractionType type);

    Optional<UserInteraction> findFirstByUser_IdAndProduct_IdAndEventTypeOrderByCreatedAtDesc(
            Long userId, Long productId, InteractionType type);

    @Modifying
    @Query("delete from UserInteraction i where i.user.id = :userId and i.product.id = :productId and i.eventType = :type")
    int deleteByUserAndProductAndType(@Param("userId") Long userId,
                                      @Param("productId") Long productId,
                                      @Param("type") InteractionType type);

    /** Subcategory ids of the products this user engaged with most recently. */
    @Query("""
            select p.category.id from UserInteraction i join i.product p
            where i.user.id = :userId and i.eventType <> com.quickfind.entity.InteractionType.SEARCH
            order by i.createdAt desc
            """)
    List<Long> findRecentCategoryIdsByUser(@Param("userId") Long userId, Pageable pageable);

    @Query("""
            select i.product.id as productId, i.eventType as eventType from UserInteraction i
            where i.createdAt >= :since and i.product is not null
            order by i.createdAt desc
            """)
    List<RecentInteractionView> findRecentProductEvents(@Param("since") LocalDateTime since, Pageable pageable);

    @Query("""
            select lower(i.queryText) as term, count(i) as hits from UserInteraction i
            where i.eventType = com.quickfind.entity.InteractionType.SEARCH and i.queryText is not null
            group by lower(i.queryText)
            order by count(i) desc
            """)
    List<SearchTermCountView> findTopSearchTerms(Pageable pageable);

    /**
     * "Users who engaged with product X also engaged with ..." — a self-join on
     * user_interactions, weighted by how strong each event is on the candidate side.
     */
    @Query(value = """
            SELECT other.product_id AS productId,
                   SUM(CASE other.event_type
                           WHEN 'ADD_TO_CART' THEN 5
                           WHEN 'WISHLIST' THEN 3
                           ELSE 1 END) AS strength
            FROM user_interactions src
            JOIN user_interactions other ON other.user_id = src.user_id
            WHERE src.product_id = :productId
              AND src.event_type <> 'SEARCH'
              AND other.product_id IS NOT NULL
              AND other.product_id <> :productId
              AND other.event_type <> 'SEARCH'
            GROUP BY other.product_id
            ORDER BY strength DESC
            LIMIT 200
            """, nativeQuery = true)
    List<CoInteractionView> findCoInteractions(@Param("productId") Long productId);
}
