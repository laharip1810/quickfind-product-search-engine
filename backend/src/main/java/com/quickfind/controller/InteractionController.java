package com.quickfind.controller;

import com.quickfind.dto.request.InteractionRequest;
import com.quickfind.dto.response.InteractionResponse;
import com.quickfind.dto.response.ProductSummaryResponse;
import com.quickfind.service.InteractionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** userId is optional on every endpoint; without it the demo user is used. */
@RestController
@RequestMapping("/api/interactions")
public class InteractionController {

    private final InteractionService interactionService;

    public InteractionController(InteractionService interactionService) {
        this.interactionService = interactionService;
    }

    /** 201 when a new event was stored; 200 when a wishlist entry already existed. */
    @PostMapping
    public ResponseEntity<InteractionResponse> record(@Valid @RequestBody InteractionRequest request) {
        InteractionService.RecordResult result = interactionService.record(request);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK).body(result.interaction());
    }

    @GetMapping("/wishlist")
    public List<ProductSummaryResponse> wishlist(@RequestParam(required = false) Long userId) {
        return interactionService.wishlist(userId);
    }

    @DeleteMapping("/wishlist/{productId}")
    public ResponseEntity<Void> removeFromWishlist(@PathVariable Long productId,
                                                   @RequestParam(required = false) Long userId) {
        interactionService.removeFromWishlist(userId, productId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/recent")
    public List<InteractionResponse> recent(@RequestParam(required = false) Long userId,
                                            @RequestParam(required = false) Integer limit) {
        return interactionService.recent(userId, limit);
    }
}
