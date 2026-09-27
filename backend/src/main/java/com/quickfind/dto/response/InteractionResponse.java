package com.quickfind.dto.response;

import java.time.LocalDateTime;

public record InteractionResponse(
        Long id,
        Long userId,
        Long productId,
        String productName,
        String eventType,
        String query,
        LocalDateTime createdAt) {
}
