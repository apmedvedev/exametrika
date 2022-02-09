/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.cache;

import com.exametrika.api.exadb.core.config.CacheCategoryTypeConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link ObjectNodeCacheManager} is a manager of object node caches.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class ObjectNodeCacheManager extends NodeCacheManager {
    private final NodeManager nodeManager;
    private final IDatabaseContext context;

    public ObjectNodeCacheManager(IDatabaseContext context, NodeManager nodeManager) {
        super(context.getConfiguration());

        Assert.notNull(nodeManager);
        Assert.notNull(context);

        this.nodeManager = nodeManager;
        this.context = context;
    }

    @Override
    protected NodeCache createNodeCache(String category, CacheCategoryTypeConfiguration categoryTypeConfiguration) {
        return new ObjectNodeCache(category, nodeManager, context.getTimeService(),
                context.getResourceAllocator(), categoryTypeConfiguration, this);
    }
}
