/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.cache;

import com.exametrika.api.exadb.core.config.CacheCategoryTypeConfiguration;
import com.exametrika.common.resource.IResourceAllocator;
import com.exametrika.common.time.ITimeService;
import com.exametrika.impl.exadb.objectdb.cache.NodeCache;
import com.exametrika.impl.exadb.objectdb.cache.NodeCacheManager;
import com.exametrika.impl.exadb.objectdb.cache.NodeManager;


/**
 * The {@link PeriodNodeCache} is a cache of period nodes.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class PeriodNodeCache extends NodeCache {
    public PeriodNodeCache(String name, NodeManager nodeManager, ITimeService timeService,
                           IResourceAllocator resourceAllocator, CacheCategoryTypeConfiguration configuration, NodeCacheManager cacheManager) {
        super(name, nodeManager, timeService, resourceAllocator, configuration, cacheManager);
    }

    @Override
    protected String getResourceConsumerName() {
        return "heap.perfdb.nodes." + (!name.isEmpty() ? name : "<default>");
    }
}
