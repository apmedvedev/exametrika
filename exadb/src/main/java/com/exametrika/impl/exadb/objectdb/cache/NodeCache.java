/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.cache;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.exametrika.api.exadb.core.IOperation;
import com.exametrika.api.exadb.core.config.CacheCategoryTypeConfiguration;
import com.exametrika.api.exadb.objectdb.INodeCache;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.rawdb.impl.RawDatabase;
import com.exametrika.common.resource.IResourceAllocator;
import com.exametrika.common.resource.IResourceConsumer;
import com.exametrika.common.time.ITimeService;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.SimpleList;
import com.exametrika.common.utils.SimpleList.Element;
import com.exametrika.impl.exadb.core.DatabaseInterceptor;
import com.exametrika.impl.exadb.objectdb.Node;
import com.exametrika.impl.exadb.objectdb.NodeSpace;

/**
 * The {@link NodeCache} is a cache of nodes.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public abstract class NodeCache implements IResourceConsumer, INodeCache {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(NodeCache.class);
    protected final String name;
    private CacheCategoryTypeConfiguration configuration;
    protected final NodeManager nodeManager;
    protected final ITimeService timeService;
    private final IResourceAllocator resourceAllocator;
    private final NodeCacheManager cacheManager;
    protected Map<IdKey, Node> nodeByIdMap = new HashMap<IdKey, Node>();
    private final SimpleList<Node> nodes = new SimpleList<Node>();
    private volatile long cacheSize;
    private volatile long maxCacheSize;
    private volatile long preparedQuota;
    private volatile long applyQuotaTime;
    private volatile long quota;
    private volatile long batchMaxCacheSize = Long.MAX_VALUE;
    private long unloadCount;
    private long unloadByTimerCount;
    private long unloadByOverflowCount;
    private int refCount;
    private int refreshIndex;
    private long refreshCacheSize;
    private final int interceptId;
    private boolean unloading;

    public NodeCache(String name, NodeManager nodeManager, ITimeService timeService,
                     IResourceAllocator resourceAllocator, CacheCategoryTypeConfiguration configuration,
                     NodeCacheManager cacheManager) {
        Assert.notNull(name);
        Assert.notNull(nodeManager);
        Assert.notNull(timeService);
        Assert.notNull(resourceAllocator);
        Assert.notNull(configuration);
        Assert.notNull(cacheManager);

        this.name = name;
        this.nodeManager = nodeManager;
        this.timeService = timeService;
        this.resourceAllocator = resourceAllocator;
        this.configuration = configuration;
        this.cacheManager = cacheManager;
        this.maxCacheSize = configuration.getInitialCacheSize();
        this.quota = maxCacheSize;

        resourceAllocator.register(getResourceConsumerName(), this);
        interceptId = DatabaseInterceptor.INSTANCE.onNodeCacheCreated(
                ((RawDatabase) nodeManager.getContext().getRawDatabase()).getInterceptId(), name);
    }

    public int getRefreshIndex() {
        return refreshIndex;
    }

    @Override
    public String getCacheCategory() {
        return name;
    }

    @Override
    public CacheCategoryTypeConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public long getCacheSize() {
        return cacheSize;
    }

    @Override
    public long getMaxCacheSize() {
        return maxCacheSize;
    }

    public void updateCacheSize(Node node, int delta) {
        cacheSize += delta;
        Assert.checkState(cacheSize >= 0);

        if (node.getRefreshIndex() == refreshIndex)
            updateRefreshCacheSize(delta, false);
    }

    public void setConfiguration(CacheCategoryTypeConfiguration configuration) {
        Assert.notNull(configuration);

        this.configuration = configuration;
    }

    public synchronized void setBatchMaxCacheSize(long value) {
        batchMaxCacheSize = value;
        maxCacheSize = Math.min(quota, batchMaxCacheSize);
        preparedQuota = 0;
        applyQuotaTime = 0;

        updateRefreshCacheSize(0, false);
        unloadExcessive(true);
    }

    public void addRef() {
        if (name.isEmpty())
            return;

        refCount++;
    }

    public void release() {
        if (name.isEmpty())
            return;

        refCount--;
        if (refCount <= 0) {
            cacheManager.removeCache(name);
            Assert.checkState(cacheSize == 0 && nodes.isEmpty());

            resourceAllocator.unregister(getResourceConsumerName());
            setQuota(configuration.getInitialCacheSize());
            DatabaseInterceptor.INSTANCE.onNodeCacheClosed(interceptId);
        }
    }

    public void addNode(Node node, boolean created) {
        if (checkNonCached(node, created))
            return;

        if (created)
            nodeManager.onNodeNew(node);

        Assert.checkState(nodeByIdMap.put(new IdKey(node.getFileIndex(), node.getId()), node) == null);

        cacheSize += node.getCacheSize();
        renewNode(node, false);

        unloadExcessive(false);
    }

    public int renewNode(Node node, boolean renew) {
        if (unloading && renew)
            return -1;

        node.setLastAccessTime((int) (timeService.getCurrentTime() >>> 13));

        Element element = node.getElement();
        element.remove();
        element.reset();

        nodes.addLast(element);
        DatabaseInterceptor.INSTANCE.onNodeLoaded(interceptId);
        updateRefreshCacheSize(node.getCacheSize(), renew);
        return preparedQuota == 0 ? refreshIndex : -1;
    }

    public void removeNode(Node node) {
        node.unload();
        nodeByIdMap.remove(new IdKey(node.getFileIndex(), node.getId()));
        cacheSize -= node.getCacheSize();
        Assert.checkState(cacheSize >= 0);

        if (node.getRefreshIndex() == refreshIndex)
            updateRefreshCacheSize(-node.getCacheSize(), false);
    }

    public <T extends Node> T findById(int fileIndex, long nodeBlockIndex) {
        Node node = nodeByIdMap.get(new IdKey(fileIndex, nodeBlockIndex));
        if (node != null)
            node.refresh();

        return (T) node;
    }

    public void close() {
        unloadNodes(true);

        resourceAllocator.unregister(getResourceConsumerName());
        setQuota(configuration.getInitialCacheSize());
        DatabaseInterceptor.INSTANCE.onNodeCacheClosed(interceptId);
    }

    public void unloadNodes(boolean removeAll) {
        unloadUsedNodes(removeAll, false);

        if (removeAll) {
            Assert.checkState(nodes.isEmpty());
            if (cacheSize != 0 && logger.isLogEnabled(LogLevel.WARNING))
                logger.log(LogLevel.WARNING, messages.cacheSizeNotZero(cacheSize));
            nodeByIdMap = new HashMap<IdKey, Node>();
        }
    }

    public void unloadExcessive(boolean force) {
        applyQuota();

        if (cacheSize > maxCacheSize &&
                (force || (nodeManager.getContext().getTransactionProvider().getTransaction().getOptions() & IOperation.DISABLE_NODES_UNLOAD) == 0))
            unloadUsedNodes(false, true);
    }

    public void unloadNodesOfDeletedSpaces(Set<? extends NodeSpace> spaces) {
        Assert.checkState(!nodeManager.hasCommitted());

        for (Iterator<Element<Node>> it = nodes.iterator(); it.hasNext(); ) {
            Node node = it.next().getValue();

            NodeSpace space = node.getSpace();
            if (spaces.contains(space)) {
                it.remove();
                removeNode(node);
                node.setStale();
                DatabaseInterceptor.INSTANCE.onNodeUnloaded(interceptId, false);
            }
        }
    }

    public void onTimer(long currentTime) {
        if (!nodes.isEmpty()) {
            refreshIndex++;

            currentTime = currentTime >>> 13;
            int maxIdlePeriod = (int) (configuration.getMaxIdlePeriod() >>> 13);

            Node node = nodes.getFirst().getValue();
            if (currentTime - node.getLastAccessTime() > maxIdlePeriod)
                unloadNodes(false);

            updateRefreshCacheSize(0, false);
            unloadExcessive(true);
        }

        DatabaseInterceptor.INSTANCE.onNodeCache(interceptId, cacheSize, maxCacheSize, quota);
    }

    @Override
    public long getAmount() {
        return cacheSize;
    }

    @Override
    public long getQuota() {
        return quota;
    }

    @Override
    public synchronized void setQuota(long value) {
        quota = value;
        long newSize = Math.min(quota, batchMaxCacheSize);
        if (newSize >= maxCacheSize)
            maxCacheSize = newSize;
        else {
            if (preparedQuota == 0)
                applyQuotaTime = timeService.getCurrentTime() + cacheManager.getConfiguration().getTimerPeriod() + 1000;

            preparedQuota = newSize;
        }
    }

    public String printStatistics() {
        return messages.statistics(!name.isEmpty() ? name : "default", configuration.toString(), maxCacheSize,
                cacheSize, unloadCount, unloadByOverflowCount, unloadByTimerCount).toString();
    }

    @Override
    public String toString() {
        return getResourceConsumerName();
    }

    protected boolean checkNonCached(Node node, boolean created) {
        if (!nodeManager.isCachingEnabled()) {
            Assert.isTrue(!created);
            node.setNonCached();
            return true;
        } else
            return false;
    }

    private void unloadUsedNodes(boolean removeAll, boolean exceedsMaxSize) {
        if (nodes.isEmpty() || unloading)
            return;

        applyQuota();

        if (exceedsMaxSize) {
            unloadByOverflowCount++;
            nodeManager.flushCommitted();
        } else {
            unloadByTimerCount++;
            if (removeAll)
                nodeManager.clearCommitted();
        }

        int currentTime = (int) (timeService.getCurrentTime() >>> 13);
        int maxIdlePeriod = (int) (configuration.getMaxIdlePeriod() >>> 13);

        Node node = nodes.getFirst().getValue();
        if (!removeAll && currentTime - node.getLastAccessTime() <= maxIdlePeriod && cacheSize <= maxCacheSize)
            return;

        unloading = true;

        long minCacheSize = (long) (maxCacheSize * configuration.getMinCachePercentage() / 100);

        for (Iterator<Element<Node>> it = nodes.iterator(); it.hasNext(); ) {
            node = it.next().getValue();

            if (!removeAll && node.getRefreshIndex() == refreshIndex)
                break;

            if (removeAll || currentTime - node.getLastAccessTime() > maxIdlePeriod || cacheSize > minCacheSize) {
                if (exceedsMaxSize) {
                    if (node.isModified()) {
                        node.validate();
                        node.flush();
                        nodeManager.setBigTransaction();
                    }
                } else {
                    Assert.checkState(!node.isModified());
                    if (node.isUncommitted())
                        nodeManager.flushCommitted();
                }

                node.getElement().remove();
                removeNode(node);
                node.setStale();
                unloadCount++;
                DatabaseInterceptor.INSTANCE.onNodeUnloaded(interceptId, !exceedsMaxSize);
            } else
                break;
        }

        unloading = false;
    }

    private void updateRefreshCacheSize(int size, boolean renew) {
        applyQuota();

        refreshCacheSize += size;
        Assert.checkState(refreshCacheSize >= 0);

        if (refreshCacheSize > maxCacheSize / 2) {
            refreshIndex++;
            refreshCacheSize = 0;

            if (renew)
                refreshCacheSize += size;
        }
    }

    private void applyQuota() {
        if (preparedQuota == 0)
            return;

        synchronized (this) {
            if (preparedQuota > 0 && timeService.getCurrentTime() > applyQuotaTime) {
                maxCacheSize = preparedQuota;
                preparedQuota = 0;
                applyQuotaTime = 0;
            }
        }
    }

    protected abstract String getResourceConsumerName();

    public static class IdKey {
        private final int fileIndex;
        private final long nodeBlockIndex;

        public IdKey(int fileIndex, long nodeBlockIndex) {
            this.fileIndex = fileIndex;
            this.nodeBlockIndex = nodeBlockIndex;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof IdKey))
                return false;

            IdKey key = (IdKey) o;
            return fileIndex == key.fileIndex && nodeBlockIndex == key.nodeBlockIndex;
        }

        @Override
        public int hashCode() {
            return 31 * (int) (nodeBlockIndex ^ (nodeBlockIndex >>> 32)) + fileIndex;
        }
    }

    private interface IMessages {
        @DefaultMessage("node cache ''{0}:{1}'' - max cache size: {2}, cache size: {3}, unload count: {4}, " + "unloadByOverflowCount: {5}, unloadByTimerCount: {6}")
        ILocalizedMessage statistics(String category, String categoryType, long maxCacheSize, long cacheSize,
                                     long unloadCount, long unloadByOverflowCount, long unloadByTimerCount);

        @DefaultMessage("Cache size is not 0: {0}")
        ILocalizedMessage cacheSizeNotZero(long cacheSize);
    }
}
