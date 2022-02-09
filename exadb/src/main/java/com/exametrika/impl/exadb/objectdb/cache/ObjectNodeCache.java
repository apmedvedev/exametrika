/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.cache;

import java.util.Set;

import com.exametrika.api.exadb.core.config.CacheCategoryTypeConfiguration;
import com.exametrika.common.resource.IResourceAllocator;
import com.exametrika.common.time.ITimeService;
import com.exametrika.impl.exadb.objectdb.NodeSpace;


/**
 * The {@link ObjectNodeCache} is a cache of object nodes.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class ObjectNodeCache extends NodeCache {
    public ObjectNodeCache(String name, NodeManager nodeManager, ITimeService timeService,
                           IResourceAllocator resourceAllocator, CacheCategoryTypeConfiguration configuration, NodeCacheManager cacheManager) {
        super(name, nodeManager, timeService, resourceAllocator, configuration, cacheManager);
    }

    @Override
    public void unloadNodesOfDeletedSpaces(Set<? extends NodeSpace> spaces) {
        super.unloadNodesOfDeletedSpaces(spaces);

        ((ObjectNodeManager) nodeManager).unloadFreeNodesOfDeletedSpaces(spaces);
    }

    @Override
    protected String getResourceConsumerName() {
        return "heap.objectdb.nodes." + (!name.isEmpty() ? name : "<default>");
    }
}
