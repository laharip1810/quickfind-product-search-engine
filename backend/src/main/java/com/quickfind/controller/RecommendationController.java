package com.quickfind.controller;

import com.quickfind.dto.response.RecommendationResponse;
import com.quickfind.service.RecommendationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    /** userId is optional; with it, the user's recent interests add a personal boost. */
    @GetMapping("/api/products/{id}/recommendations")
    public RecommendationResponse recommendations(@PathVariable Long id,
                                                  @RequestParam(required = false) Long userId,
                                                  @RequestParam(required = false) Integer limit) {
        return recommendationService.recommend(id, userId, limit);
    }
}
