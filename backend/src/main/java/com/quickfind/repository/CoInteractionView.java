package com.quickfind.repository;

/**
 * Result row of the co-interaction native query. Declared as {@link Number} because
 * MySQL returns SUM(...) as DECIMAL while H2 returns BIGINT.
 */
public interface CoInteractionView {

    Number getProductId();

    Number getStrength();
}
