package com.quickfind.repository;

import java.math.BigDecimal;

/** Projection used to build the in-memory price index (binary search). */
public interface ProductPriceView {

    Long getCategoryId();

    Long getParentCategoryId();

    BigDecimal getSalePrice();
}
