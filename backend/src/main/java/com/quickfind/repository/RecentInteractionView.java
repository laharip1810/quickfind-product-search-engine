package com.quickfind.repository;

import com.quickfind.entity.InteractionType;

/** Projection used for trending: which product, and what kind of event. */
public interface RecentInteractionView {

    Long getProductId();

    InteractionType getEventType();
}
