package com.quickfind.service;

import com.quickfind.entity.Brand;
import com.quickfind.entity.Category;
import com.quickfind.entity.Color;
import com.quickfind.repository.BrandRepository;
import com.quickfind.repository.CategoryRepository;
import com.quickfind.repository.ColorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * In-memory HashMaps of the small, rarely changing reference data (brands, categories,
 * colors), keyed by lowercase name and by id.
 *
 * <p>Search filters arrive as names ("?category=Footwear&brand=Nike"). Resolving them with
 * an O(1) map lookup instead of a query per filter keeps search to one database round
 * trip. The maps are rebuilt as a whole and swapped atomically (a volatile snapshot), so
 * readers never see a half-built state.
 */
@Service
public class CatalogLookupService {

    /** Immutable snapshot of the lookup tables. */
    private record Snapshot(Map<String, Long> brandIdsByName,
                            Map<Long, String> brandNamesById,
                            Map<String, CategoryInfo> categoriesByName,
                            Map<Long, CategoryInfo> categoriesById,
                            Map<String, Long> colorIdsByName) {
    }

    /**
     * @param childIds for a top-level category, its subcategory ids; empty for a subcategory
     */
    public record CategoryInfo(Long id, String name, Long parentId, Set<Long> childIds) {

        public boolean isTopLevel() {
            return parentId == null;
        }

        /**
         * Ids a product's category_id must be in to belong to this category. A top-level
         * category without subcategories yields its own id, which matches no product
         * (never an empty set, which would mean "no filter").
         */
        public Set<Long> matchingProductCategoryIds() {
            return isTopLevel() && !childIds.isEmpty() ? childIds : Set.of(id);
        }
    }

    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final ColorRepository colorRepository;
    private volatile Snapshot snapshot;

    public CatalogLookupService(BrandRepository brandRepository, CategoryRepository categoryRepository,
                                ColorRepository colorRepository) {
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
        this.colorRepository = colorRepository;
    }

    public Optional<Long> brandIdByName(String name) {
        return Optional.ofNullable(current().brandIdsByName().get(key(name)));
    }

    public Optional<String> brandNameById(Long id) {
        return Optional.ofNullable(current().brandNamesById().get(id));
    }

    public Optional<CategoryInfo> categoryByName(String name) {
        return Optional.ofNullable(current().categoriesByName().get(key(name)));
    }

    public Optional<CategoryInfo> categoryById(Long id) {
        return Optional.ofNullable(current().categoriesById().get(id));
    }

    public Optional<Long> colorIdByName(String name) {
        return Optional.ofNullable(current().colorIdsByName().get(key(name)));
    }

    /** Ids of the named categories; unknown names are ignored. */
    public Set<Long> categoryIdsByNames(Collection<String> names) {
        Set<Long> ids = new HashSet<>();
        for (String name : names) {
            categoryByName(name).ifPresent(info -> ids.add(info.id()));
        }
        return ids;
    }

    /** Reloads the maps. Called after seeding; cheap because the tables are small. */
    @Transactional(readOnly = true)
    public synchronized void refresh() {
        Map<String, Long> brandIds = new HashMap<>();
        Map<Long, String> brandNames = new HashMap<>();
        for (Brand brand : brandRepository.findAll()) {
            brandIds.put(key(brand.getName()), brand.getId());
            brandNames.put(brand.getId(), brand.getName());
        }

        List<Category> categories = categoryRepository.findAllWithParent();
        Map<Long, Set<Long>> childrenByParent = new HashMap<>();
        for (Category category : categories) {
            if (category.getParent() != null) {
                childrenByParent.computeIfAbsent(category.getParent().getId(), k -> new HashSet<>()).add(category.getId());
            }
        }
        Map<String, CategoryInfo> byName = new HashMap<>();
        Map<Long, CategoryInfo> byId = new HashMap<>();
        for (Category category : categories) {
            Long parentId = category.getParent() == null ? null : category.getParent().getId();
            Set<Long> children = Set.copyOf(childrenByParent.getOrDefault(category.getId(), Collections.emptySet()));
            CategoryInfo info = new CategoryInfo(category.getId(), category.getName(), parentId, children);
            byName.put(key(category.getName()), info);
            byId.put(category.getId(), info);
        }

        Map<String, Long> colorIds = new HashMap<>();
        for (Color color : colorRepository.findAll()) {
            colorIds.put(key(color.getName()), color.getId());
        }
        snapshot = new Snapshot(Map.copyOf(brandIds), Map.copyOf(brandNames), Map.copyOf(byName),
                Map.copyOf(byId), Map.copyOf(colorIds));
    }

    private Snapshot current() {
        Snapshot current = snapshot;
        if (current == null) {
            refresh();
            current = snapshot;
        }
        return current;
    }

    private static String key(String name) {
        return name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
    }
}
