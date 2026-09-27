package com.quickfind.service;

import com.quickfind.dto.request.InteractionRequest;
import com.quickfind.dto.response.InteractionResponse;
import com.quickfind.dto.response.ProductSummaryResponse;
import com.quickfind.entity.InteractionType;
import com.quickfind.entity.Product;
import com.quickfind.entity.User;
import com.quickfind.entity.UserInteraction;
import com.quickfind.exception.InvalidInteractionException;
import com.quickfind.exception.ProductNotFoundException;
import com.quickfind.exception.ResourceNotFoundException;
import com.quickfind.mapper.ProductMapper;
import com.quickfind.repository.ProductRepository;
import com.quickfind.repository.UserInteractionRepository;
import com.quickfind.repository.UserRepository;
import com.quickfind.util.TextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Records user events and serves the wishlist / recent-activity views.
 *
 * <p>Each product event also nudges the product's popularity score by 0.1 × event weight,
 * with an atomic SQL increment. The cached product detail is intentionally not evicted
 * for this: a slightly stale popularity number for up to the cache TTL is an acceptable
 * trade for not invalidating the cache on every page view.
 */
@Service
public class InteractionService {

    private static final Logger log = LoggerFactory.getLogger(InteractionService.class);
    private static final double POPULARITY_FACTOR = 0.1;

    /** The outcome of recording an event; created=false means an idempotent repeat (wishlist). */
    public record RecordResult(InteractionResponse interaction, boolean created) {
    }

    private final UserInteractionRepository interactionRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final DemoUserProvider demoUserProvider;
    private final ProductMapper mapper;

    public InteractionService(UserInteractionRepository interactionRepository, ProductRepository productRepository,
                              UserRepository userRepository, DemoUserProvider demoUserProvider, ProductMapper mapper) {
        this.interactionRepository = interactionRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.demoUserProvider = demoUserProvider;
        this.mapper = mapper;
    }

    @Transactional
    public RecordResult record(InteractionRequest request) {
        User user = requireUser(request.userId());
        InteractionType type = request.eventType();
        String query = TextUtils.clean(request.query());

        Product product = null;
        if (type.requiresProduct()) {
            if (request.productId() == null) {
                throw new InvalidInteractionException("productId is required for " + type + " events");
            }
            product = productRepository.findById(request.productId())
                    .orElseThrow(() -> new ProductNotFoundException(request.productId()));
        } else if (query == null) {
            throw new InvalidInteractionException("query is required for SEARCH events");
        }

        if (type == InteractionType.WISHLIST) {
            Optional<UserInteraction> existing = interactionRepository
                    .findFirstByUser_IdAndProduct_IdAndEventTypeOrderByCreatedAtDesc(user.getId(), product.getId(), type);
            if (existing.isPresent()) {
                return new RecordResult(toResponse(existing.get()), false);
            }
        }

        UserInteraction saved = interactionRepository.save(
                new UserInteraction(user, product, type, type == InteractionType.SEARCH ? query : null));
        if (product != null && type.weight() > 0) {
            productRepository.incrementPopularity(product.getId(), type.weight() * POPULARITY_FACTOR);
        }
        log.info("Recorded {} user={} product={} query='{}'", type, user.getId(),
                product == null ? null : product.getId(), saved.getQueryText());
        return new RecordResult(toResponse(saved), true);
    }

    @Transactional(readOnly = true)
    public List<ProductSummaryResponse> wishlist(Long userIdParam) {
        User user = requireUser(userIdParam);
        List<Long> ids = new ArrayList<>(new LinkedHashSet<>(
                interactionRepository.findProductIdsByUserAndType(user.getId(), InteractionType.WISHLIST)));
        Map<Long, Product> byId = productRepository.findAllByIdIn(ids).stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        return ids.stream().map(byId::get).filter(p -> p != null).map(mapper::toSummary).toList();
    }

    @Transactional
    public void removeFromWishlist(Long userIdParam, Long productId) {
        User user = requireUser(userIdParam);
        int removed = interactionRepository.deleteByUserAndProductAndType(user.getId(), productId, InteractionType.WISHLIST);
        log.info("Removed product {} from wishlist of user {} ({} rows)", productId, user.getId(), removed);
    }

    @Transactional(readOnly = true)
    public List<InteractionResponse> recent(Long userIdParam, Integer limitParam) {
        int limit = limitParam == null ? 20 : limitParam;
        if (limit < 1 || limit > 100) {
            throw new InvalidInteractionException("limit must be between 1 and 100");
        }
        User user = requireUser(userIdParam);
        return interactionRepository.findByUser_IdOrderByCreatedAtDescIdDesc(user.getId(), PageRequest.of(0, limit))
                .stream().map(this::toResponse).toList();
    }

    private User requireUser(Long userId) {
        Long id = demoUserProvider.resolve(userId);
        return userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User", id));
    }

    private InteractionResponse toResponse(UserInteraction interaction) {
        Product product = interaction.getProduct();
        return new InteractionResponse(
                interaction.getId(),
                interaction.getUser().getId(),
                product == null ? null : product.getId(),
                product == null ? null : product.getName(),
                interaction.getEventType().name(),
                interaction.getQueryText(),
                interaction.getCreatedAt());
    }
}
