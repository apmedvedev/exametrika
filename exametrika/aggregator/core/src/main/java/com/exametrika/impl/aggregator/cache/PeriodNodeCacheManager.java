/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.cache;

import com.exametrika.api.exadb.core.config.CacheCategoryTypeConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.exadb.objectdb.cache.NodeCache;
import com.exametrika.impl.exadb.objectdb.cache.NodeCacheManager;
import com.exametrika.impl.exadb.objectdb.cache.NodeManager;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link PeriodNodeCacheManager} is a manager of period node caches.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class PeriodNodeCacheManager extends NodeCacheManager {
    private final NodeManager nodeManager;
    private final IDatabaseContext context;

    public PeriodNodeCacheManager(IDatabaseContext context, NodeManager nodeManager) {
        super(context.getConfiguration());

        Assert.notNull(nodeManager);
        Assert.notNull(context);

        this.nodeManager = nodeManager;
        this.context = context;
    }

    @Override
    protected NodeCache createNodeCache(String category, CacheCategoryTypeConfiguration categoryTypeConfiguration) {
        return new PeriodNodeCache(category, nodeManager, context.getTimeService(),
                context.getResourceAllocator(), categoryTypeConfiguration, this);
    }
}
