/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.core;

import java.util.Map;

import com.exametrika.common.utils.Pair;


/**
 * The {@link ICacheCategorizationStrategy} represents a cache categorization strategy.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface ICacheCategorizationStrategy {
    /**
     * Categorizes cache element by its properties
     *
     * @param cacheElementProperties cache element properties
     * @return pair - category:categoryType of specified cache element
     */
    Pair<String, String> categorize(Map<String, String> cacheElementProperties);
}
