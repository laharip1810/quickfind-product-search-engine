package com.quickfind.dto.response;

import java.util.List;

public record SuggestionResponse(String prefix, List<Item> suggestions) {

    public record Item(String text, String type, double score, Long productId) {
    }
}
