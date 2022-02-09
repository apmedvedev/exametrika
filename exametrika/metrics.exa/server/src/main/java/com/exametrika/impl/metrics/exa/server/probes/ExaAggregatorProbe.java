/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.exa.server.probes;

import com.exametrika.api.metrics.exa.server.config.ExaAggregatorProbeConfiguration;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.rawdb.impl.RawDatabaseInterceptor;
import com.exametrika.common.utils.SlotAllocator;
import com.exametrika.common.utils.SlotAllocator.Slot;
import com.exametrika.impl.aggregator.AggregatorInterceptor;
import com.exametrika.impl.exadb.core.DatabaseInterceptor;
import com.exametrika.impl.profiler.probes.BaseProbe;
import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor.Container;
import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor.ThreadLocalSlot;
import com.exametrika.spi.profiler.IProbeCollector;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.IScope;
import com.exametrika.spi.profiler.IThreadLocalProvider;
import com.exametrika.spi.profiler.IThreadLocalSlot;


/**
 * The {@link ExaAggregatorProbe} is a Exa aggregator probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ExaAggregatorProbe extends BaseProbe implements IThreadLocalProvider {
    private final ExaAggregatorProbeConfiguration configuration;
    private ThreadLocalSlot slot;
    private final SlotAllocator slotAllocator = new SlotAllocator();

    public ExaAggregatorProbe(ExaAggregatorProbeConfiguration configuration, IProbeContext context) {
        super(configuration, context);

        this.configuration = configuration;
    }

    @Override
    public boolean isSystem() {
        return true;
    }

    @Override
    public synchronized void start() {
        if (!(AggregatorInterceptor.INSTANCE instanceof ExaAggregatorInterceptor)) {
            ExaAggregatorInterceptor interceptor = new ExaAggregatorInterceptor();
            RawDatabaseInterceptor.INSTANCE = interceptor;
            DatabaseInterceptor.INSTANCE = interceptor;
            AggregatorInterceptor.INSTANCE = interceptor;
        } else
            ((ExaAggregatorInterceptor) AggregatorInterceptor.INSTANCE).setEnabled(true);
    }

    @Override
    public synchronized void stop() {
        if (AggregatorInterceptor.INSTANCE instanceof ExaAggregatorInterceptor)
            ((ExaAggregatorInterceptor) AggregatorInterceptor.INSTANCE).setEnabled(false);
    }

    @Override
    public boolean isStack() {
        return false;
    }

    @Override
    public IProbeCollector createCollector(IScope scope) {
        return new ExaAggregatorProbeCollector(configuration, context, scope, slot, threadLocalAccessor.get(false),
                slotAllocator);
    }

    @Override
    public void onTimer() {
    }

    @Override
    public void setSlot(IThreadLocalSlot slot) {
        this.slot = (ThreadLocalSlot) slot;
    }

    @Override
    public Object allocate() {
        return new CollectorInfo();
    }

    static class CollectorInfo {
        public ExaAggregatorProbeCollector collector;
    }

    private class ExaAggregatorInterceptor extends AggregatorInterceptor {
        private volatile boolean enabled = true;

        public void setEnabled(boolean value) {
            this.enabled = value;
        }

        @Override
        public int onStarted(String databaseName) {
            int index = slotAllocator.allocate("aggregators." + databaseName, "exa.rawdb",
                    Json.object().put("aggregator", databaseName).toObject()).id;
            slotAllocator.allocate("aggregators." + databaseName, "exa.exadb.fullText",
                    Json.object().put("aggregator", databaseName).toObject());
            slotAllocator.allocate("aggregators." + databaseName, "exa.aggregator.nameCache",
                    Json.object().put("aggregator", databaseName).toObject());
            slotAllocator.allocate("aggregators." + databaseName, "exa.aggregator",
                    Json.object().put("aggregator", databaseName).toObject());

            return index;
        }

        @Override
        public void onStopped(int id) {
            slotAllocator.free(id);
            slotAllocator.free(id + 1);
            slotAllocator.free(id + 2);
            slotAllocator.free(id + 3);
        }

        @Override
        public int onNodeCacheCreated(int id, String cacheName) {
            if (cacheName.isEmpty())
                cacheName = "default";

            Slot parent = slotAllocator.getSlot(id);
            return slotAllocator.allocate(parent.name + ".nodes." + cacheName, "exa.exadb.nodeCache",
                    Json.object(parent.metadata).put("nodeCache", cacheName).toObject()).id;
        }

        @Override
        public void onNodeCacheClosed(int id) {
            slotAllocator.free(id);
        }

        @Override
        public int onPageCacheCreated(int id, String pageCacheName, int pageSize) {
            if (pageCacheName.isEmpty())
                pageCacheName = "default";

            Slot parent = slotAllocator.getSlot(id);
            return slotAllocator.allocate(parent.name + ".pages." + pageCacheName, "exa.rawdb.pageCache",
                    Json.object(parent.metadata).put("pageCache", pageCacheName).put("pageSize", pageSize).toObject()).id;
        }

        @Override
        public void onPageCacheClosed(int id) {
            slotAllocator.free(id);
        }

        @Override
        public int onPageTypeCreated(int id, String pageTypeName, int pageSize) {
            Slot parent = slotAllocator.getSlot(id);
            return slotAllocator.allocate(parent.name + ".pages." + pageTypeName, "exa.rawdb.pageType",
                    Json.object(parent.metadata).put("pageType", pageTypeName).put("pageSize", pageSize).toObject()).id;
        }

        @Override
        public void onPageTypeClosed(int id) {
            slotAllocator.free(id);
        }

        @Override
        public void onDatabase(int id, JsonObject resourceAllocatorInfo, int currentFileCount, int pagePoolSize,
                               int transactionQueueSize) {
            Container container = threadLocalAccessor.get(false);
            if (!enabled || container == null)
                return;

            CollectorInfo info = slot.get(false);
            if (info.collector == null)
                return;

            info.collector.onDatabase(id, resourceAllocatorInfo, currentFileCount, pagePoolSize, transactionQueueSize);
        }

        @Override
        public boolean onBeforeFileRead(int id) {
            Container container = threadLocalAccessor.get(false);
            if (!enabled || container == null)
                return false;

            CollectorInfo info = slot.get(false);
            if (info.collector == null)
                return false;

            info.collector.onBeforeFileRead(id);
            return true;
        }

        @Override
        public void onAfterFileRead(int id, int size) {
            Container container = threadLocalAccessor.get(false);
            if (!enabled || container == null)
                return;

            CollectorInfo info = slot.get(false);
            if (info.collector == null)
                return;

            info.collector.onAfterFileRead(id, size);
        }

        @Override
        public boolean onBeforeFileWritten(int id) {
            Container container = threadLocalAccessor.get(false);
            if (!enabled || container == null)
                return false;

            CollectorInfo info = slot.get(false);
            if (info.collector == null)
                return false;

            info.collector.onBeforeFileWritten(id);
            return true;
        }

        @Override
        public void onAfterFileWritten(int id, int size) {
            Container container = threadLocalAccessor.get(false);
            if (!enabled || container == null)
                return;

            CollectorInfo info = slot.get(false);
            if (info.collector == null)
                return;

            info.collector.onAfterFileWritten(id, size);
        }

        @Override
        public void onFileLoaded(int id) {
            Container container = threadLocalAccessor.get(false);
            if (!enabled || container == null)
                return;

            CollectorInfo info = slot.get(false);
            if (info.collector == null)
                return;

            info.collector.onFileLoaded(id);
        }

        @Override
        public void onFileUnloaded(int id) {
            Container container = threadLocalAccessor.get(false);
            if (!enabled || container == null)
                return;

            CollectorInfo info = slot.get(false);
            if (info.collector == null)
                return;

            info.collector.onFileUnloaded(id);
        }

        @Override
        public boolean onBeforeLogFlushed(int id) {
            Container container = threadLocalAccessor.get(false);
            if (!enabled || container == null)
                return false;

            CollectorInfo info = slot.get(false);
            if (info.collector == null)
                return false;

            info.collector.onBeforeLogFlushed(id);
            return true;
        }

        @Override
        public void onAfterLogFlushed(int id, long size) {
            Container container = threadLocalAccessor.get(false);
            if (!enabled || container == null)
                return;

            CollectorInfo info = slot.get(false);
            if (info.collector == null)
                return;

            info.collector.onAfterLogFlushed(id, size);
        }

        @Override
        public boolean onTransactionStarted(int id) {
            Container container = threadLocalAccessor.get(false);
            if (!enabled || container == null)
                return false;

            CollectorInfo info = slot.get(false);
            if (info.collector == null)
                return false;

            info.collector.onTransactionStarted(id);
            return true;
        }

        @Override
        public void onTransactionCommitted(int id) {
            Container container = threadLocalAccessor.get(false);
            if (!enabled || container == null)
                return;

            CollectorInfo info = slot.get(false);
            if (info.collector == null)
                return;

            info.collector.onTransactionCommitted(id);
        }

        @Override
        public void onTransactionRolledBack(int id, Throwable exception) {
            Container container = threadLocalAccessor.get(false);
            if (!enabled || container == null)
                return;

            CollectorInfo info = slot.get(false);
            if (info.collector == null)
                return;

            info.collector.onTransactionRolledBack(id, exception);
        }

        @Override
        public void onPageCache(int id, long pageCacheSize, long maxPageCacheSize, long quota) {
            Container container = threadLocalAccessor.get(false);
            if (!enabled || container == null)
                return;

            CollectorInfo info = slot.get(false);
            if (info.collector == null)
                return;

            info.collector.onPageCache(id, pageCacheSize, maxPageCacheSize, quota);
        }

        @Override
        public void onPageLoaded(int id) {
            Container container = threadLocalAccessor.get(false);
            if (!enabled || container == null)
                return;

            CollectorInfo info = slot.get(false);
            if (info.collector == null)
                return;

            info.collector.onPageLoaded(id);
        }

        @Override
        public void onPageUnloaded(int id, boolean byTimer) {
            Container container = threadLocalAccessor.get(false);
            if (!enabled || container == null)
                return;

            CollectorInfo info = slot.get(false);
            if (info.collector == null)
                return;

            info.collector.onPageUnloaded(id, byTimer);
        }

        @Override
        public void onPageType(int id, long currentRegionsCount, long currentRegionsSize) {
            Container container = threadLocalAccessor.get(false);
            if (!enabled || container == null)
                return;

            CollectorInfo info = slot.get(false);
            if (info.collector == null)
                return;

            info.collector.onPageType(id, currentRegionsCount, currentRegionsSize);
        }

        @Override
        public void onRegionAllocated(int id, int size) {
            Container container = threadLocalAccessor.get(false);
            if (!enabled || container == null)
                return;

            CollectorInfo info = slot.get(false);
            if (info.collector == null)
                return;

            info.collector.onRegionAllocated(id, size);
        }

        @Override
        public void onRegionFreed(int id, int size) {
            Container container = threadLocalAccessor.get(false);
            if (!enabled || container == null)
                return;

            CollectorInfo info = slot.get(false);
            if (info.collector == null)
                return;

            info.collector.onRegionFreed(id, size);
        }

        @Override
        public boolean onBeforeFullTextAdded(int id) {
            Container container = threadLocalAccessor.get(false);
            if (!enabled || container == null)
                return false;

            CollectorInfo info = slot.get(false);
            if (info.collector == null)
                return false;

            info.collector.onBeforeFullTextAdded(id);
            return true;
        }

        @Override
        public void onAfterFullTextAdded(int id) {
            Container container = threadLocalAccessor.get(false);
            if (!enabled || container == null)
                return;

            CollectorInfo info = slot.get(false);
            if (info.collector == null)
                return;

            info.collector.onAfterFullTextAdded(id);
        }

        @Override
        public boolean onBeforeFullTextUpdated(int id) {
            Container container = threadLocalAccessor.get(false);
            if (!enabled || container == null)
                return false;

            CollectorInfo info = slot.get(false);
            if (info.collector == null)
                return false;

            info.collector.onBeforeFullTextUpdated(id);
            return true;
        }

        @Override
        public void onAfterFullTextUpdated(int id) {
            Container container = threadLocalAccessor.get(false);
            if (!enabled || container == null)
                return;

            CollectorInfo info = slot.get(false);
            if (info.collector == null)
                return;

            info.collector.onAfterFullTextUpdated(id);
        }

        @Override
        public boolean onBeforeFullTextDeleted(int id) {
            Container container = threadLocalAccessor.get(false);
            if (!enabled || container == null)
                return false;

            CollectorInfo info = slot.get(false);
            if (info.collector == null)
                return false;

            info.collector.onBeforeFullTextDeleted(id);
            return true;
        }

        @Override
        public void onAfterFullTextDeleted(int id) {
            Container container = threadLocalAccessor.get(false);
            if (!enabled || container == null)
                return;

            CollectorInfo info = slot.get(false);
            if (info.collector == null)
                return;

            info.collector.onAfterFullTextDeleted(id);
        }

        @Override
        public boolean onBeforeFullTextSearched(int id) {
            Container container = threadLocalAccessor.get(false);
            if (!enabled || container == null)
                return false;

            CollectorInfo info = slot.get(false);
            if (info.collector == null)
                return false;

            info.collector.onBeforeFullTextSearched(id);
            return true;
        }

        @Override
        public void onAfterFullTextSearched(int id) {
            Container container = threadLocalAccessor.get(false);
            if (!enabled || container == null)
                return;

            CollectorInfo info = slot.get(false);
            if (info.collector == null)
                return;

            info.collector.onAfterFullTextSearched(id);
        }

        @Override
        public boolean onBeforeFullTextSearcherUpdated(int id) {
            Container container = threadLocalAccessor.get(false);
            if (!enabled || container == null)
                return false;

            CollectorInfo info = slot.get(false);
            if (info.collector == null)
                return false;

            info.collector.onBeforeFullTextSearcherUpdated(id);
            return true;
        }

        @Override
        public void onAfterFullTextSearcherUpdated(int id) {
            Container container = threadLocalAccessor.get(false);
            if (!enabled || container == null)
                return;

            CollectorInfo info = slot.get(false);
            if (info.collector == null)
                return;

            info.collector.onAfterFullTextSearcherUpdated(id);
        }

        @Override
        public boolean onBeforeFullTextCommitted(int id) {
            Container container = threadLocalAccessor.get(false);
            if (!enabled || container == null)
                return false;

            CollectorInfo info = slot.get(false);
            if (info.collector == null)
                return false;

            info.collector.onBeforeFullTextCommitted(id);
            return true;
        }

        @Override
        public void onAfterFullTextCommitted(int id) {
            Container container = threadLocalAccessor.get(false);
            if (!enabled || container == null)
                return;

            CollectorInfo info = slot.get(false);
            if (info.collector == null)
                return;

            info.collector.onAfterFullTextCommitted(id);
        }

        @Override
        public void onNodeCache(int id, long cacheSize, long maxCacheSize, long quota) {
            Container container = threadLocalAccessor.get(false);
            if (!enabled || container == null)
                return;

            CollectorInfo info = slot.get(false);
            if (info.collector == null)
                return;

            info.collector.onNodeCache(id, cacheSize, maxCacheSize, quota);
        }

        @Override
        public void onNodeLoaded(int id) {
            Container container = threadLocalAccessor.get(false);
            if (!enabled || container == null)
                return;

            CollectorInfo info = slot.get(false);
            if (info.collector == null)
                return;

            info.collector.onNodeLoaded(id);
        }

        @Override
        public void onNodeUnloaded(int id, boolean byTimer) {
            Container container = threadLocalAccessor.get(false);
            if (!enabled || container == null)
                return;

            CollectorInfo info = slot.get(false);
            if (info.collector == null)
                return;

            info.collector.onNodeUnloaded(id, byTimer);
        }

        @Override
        public void onNameCache(int id, long cacheSize, long maxCacheSize, long quota) {
            Container container = threadLocalAccessor.get(false);
            if (!enabled || container == null)
                return;

            CollectorInfo info = slot.get(false);
            if (info.collector == null)
                return;

            info.collector.onNameCache(id, cacheSize, maxCacheSize, quota);
        }

        @Override
        public void onNameLoaded(int id) {
            Container container = threadLocalAccessor.get(false);
            if (!enabled || container == null)
                return;

            CollectorInfo info = slot.get(false);
            if (info.collector == null)
                return;

            info.collector.onNameLoaded(id);
        }

        @Override
        public void onNameUnloaded(int id, boolean byTimer) {
            Container container = threadLocalAccessor.get(false);
            if (!enabled || container == null)
                return;

            CollectorInfo info = slot.get(false);
            if (info.collector == null)
                return;

            info.collector.onNameUnloaded(id, byTimer);
        }

        @Override
        public boolean onBeforeAggregated(int id) {
            Container container = threadLocalAccessor.get(false);
            if (!enabled || container == null)
                return false;

            CollectorInfo info = slot.get(false);
            if (info.collector == null)
                return false;

            info.collector.onBeforeAggregated(id);
            return true;
        }

        @Override
        public void onAfterAggregated(int id, int measurementsCount) {
            Container container = threadLocalAccessor.get(false);
            if (!enabled || container == null)
                return;

            CollectorInfo info = slot.get(false);
            if (info.collector == null)
                return;

            info.collector.onAfterAggregated(id, measurementsCount);
        }

        @Override
        public boolean onBeforePeriodClosed(int id) {
            Container container = threadLocalAccessor.get(false);
            if (!enabled || container == null)
                return false;

            CollectorInfo info = slot.get(false);
            if (info.collector == null)
                return false;

            info.collector.onBeforePeriodClosed(id);
            return true;
        }

        @Override
        public void onAfterPeriodClosed(int id) {
            Container container = threadLocalAccessor.get(false);
            if (!enabled || container == null)
                return;

            CollectorInfo info = slot.get(false);
            if (info.collector == null)
                return;

            info.collector.onAfterPeriodClosed(id);
        }

        @Override
        public void onSelected(int id, long selectedTime, int selectionSize) {
            Container container = threadLocalAccessor.get(false);
            if (!enabled || container == null)
                return;

            CollectorInfo info = slot.get(false);
            if (info.collector == null)
                return;

            info.collector.onSelected(id, selectedTime, selectionSize);
        }
    }
}
