/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator;

import com.exametrika.impl.exadb.core.DatabaseInterceptor;


/**
 * The {@link AggregatorInterceptor} is a aggregator interceptor.
 *
 * @author AndreyM
 * @threadsafety Implementations of this class and its methods are thread safe.
 */
public class AggregatorInterceptor extends DatabaseInterceptor {
    public static AggregatorInterceptor INSTANCE = new AggregatorInterceptor();

    public void onNameCache(int id, long cacheSize, long maxCacheSize, long quota) {
    }

    public void onNameLoaded(int id) {
    }

    public void onNameUnloaded(int id, boolean byTimer) {
    }

    public boolean onBeforeAggregated(int id) {
        return false;
    }

    public void onAfterAggregated(int id, int measurementsCount) {
    }

    public boolean onBeforePeriodClosed(int id) {
        return false;
    }

    public void onAfterPeriodClosed(int id) {
    }
}
