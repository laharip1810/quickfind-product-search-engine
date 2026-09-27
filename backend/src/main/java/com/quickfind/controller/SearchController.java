package com.quickfind.controller;

import com.quickfind.dto.request.SearchRequest;
import com.quickfind.dto.response.SearchResponse;
import com.quickfind.dto.response.SuggestionResponse;
import com.quickfind.service.AutocompleteService;
import com.quickfind.service.ProductSearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SearchController {

    private final ProductSearchService searchService;
    private final AutocompleteService autocompleteService;

    public SearchController(ProductSearchService searchService, AutocompleteService autocompleteService) {
        this.searchService = searchService;
        this.autocompleteService = autocompleteService;
    }

    /**
     * Example: /api/products/search?query=black running shoes&amp;category=Footwear&amp;minPrice=2000
     * &amp;maxPrice=12000&amp;minRating=4&amp;sort=relevance&amp;page=0&amp;size=20
     */
    @GetMapping("/api/products/search")
    public SearchResponse search(@ModelAttribute SearchRequest request) {
        return searchService.search(request);
    }

    @GetMapping("/api/search/suggestions")
    public SuggestionResponse suggestions(@RequestParam(required = false) String prefix,
                                          @RequestParam(required = false) Integer limit) {
        return autocompleteService.suggest(prefix, limit);
    }
}
