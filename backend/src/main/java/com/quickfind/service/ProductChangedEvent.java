package com.quickfind.service;

/** Published after a product is created, updated, restocked or deleted. */
public record ProductChangedEvent(Long productId, ChangeType changeType) {

    public enum ChangeType {
        CREATED, UPDATED, STOCK_CHANGED, DELETED
    }
}
