/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.cache;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.exametrika.api.exadb.core.config.CacheCategoryTypeConfiguration;
import com.exametrika.api.exadb.core.config.DatabaseConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.exadb.objectdb.NodeSpace;


/**
 * The {@link NodeCacheManager} is a manager of node caches.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public abstract class NodeCacheManager {
    private DatabaseConfiguration configuration;
    private final Map<String, NodeCache> nodeCaches = new LinkedHashMap<String, NodeCache>();

    public NodeCacheManager(DatabaseConfiguration configuration) {
        Assert.notNull(configuration);

        this.configuration = configuration;
    }

    public DatabaseConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(DatabaseConfiguration configuration, boolean clearCache) {
        this.configuration = configuration;

        if (!clearCache) {
            for (NodeCache nodeCache : nodeCaches.values()) {
                if (nodeCache.getConfiguration().getName().isEmpty())
                    nodeCache.setConfiguration(configuration.getDefaultCacheCategoryType());
                else
                    nodeCache.setConfiguration(configuration.getCacheCategoryTypes().get(nodeCache.getConfiguration().getName()));
            }
        }
    }

    public NodeCache getExistingNodeCache(String category) {
        if (category == null)
            category = "";

        NodeCache nodeCache = nodeCaches.get(category);
        return nodeCache;
    }

    public <T extends NodeCache> T getNodeCache(String category, String categoryType) {
        if (category == null)
            category = "";

        NodeCache nodeCache = nodeCaches.get(category);
        if (nodeCache != null) {
            if (categoryType != null && !categoryType.isEmpty())
                Assert.isTrue(nodeCache.getConfiguration().getName().equals(categoryType));
            else
                Assert.isTrue(nodeCache.getConfiguration().getName().isEmpty());
            return (T) nodeCache;
        }

        CacheCategoryTypeConfiguration categoryTypeConfiguration;
        if (categoryType == null || categoryType.isEmpty())
            categoryTypeConfiguration = configuration.getDefaultCacheCategoryType();
        else {
            categoryTypeConfiguration = configuration.getCacheCategoryTypes().get(categoryType);
            Assert.notNull(categoryTypeConfiguration);
        }

        nodeCache = createNodeCache(category, categoryTypeConfiguration);
        nodeCaches.put(category, nodeCache);

        return (T) nodeCache;
    }

    public void removeCache(String category) {
        nodeCaches.remove(category);
    }

    public void unloadNodes(boolean removeAll) {
        for (NodeCache nodeCache : nodeCaches.values())
            nodeCache.unloadNodes(removeAll);
    }

    public void unloadExcessive() {
        for (NodeCache nodeCache : nodeCaches.values())
            nodeCache.unloadExcessive(true);
    }

    public void unloadNodesOfDeletedSpaces(Set<? extends NodeSpace> spaces) {
        for (NodeCache nodeCache : nodeCaches.values())
            nodeCache.unloadNodesOfDeletedSpaces(spaces);
    }

    public void close() {
        for (NodeCache nodeCache : nodeCaches.values())
            nodeCache.close();

        nodeCaches.clear();
    }

    public void onTimer(long currentTime) {
        for (NodeCache nodeCache : nodeCaches.values())
            nodeCache.onTimer(currentTime);
    }

    public String printStatistics() {
        StringBuilder builder = new StringBuilder();

        boolean first = true;
        for (NodeCache nodeCache : nodeCaches.values()) {
            if (first)
                first = false;
            else
                builder.append('\n');

            builder.append(nodeCache.printStatistics());
        }

        return builder.toString();
    }

    @Override
    public String toString() {
        return configuration.toString();
    }

    protected abstract NodeCache createNodeCache(String category, CacheCategoryTypeConfiguration categoryTypeConfiguration);
}
