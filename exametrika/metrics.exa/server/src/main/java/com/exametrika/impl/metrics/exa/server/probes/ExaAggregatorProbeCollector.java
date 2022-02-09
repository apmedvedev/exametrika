/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.exa.server.probes;


import java.util.ArrayList;

import com.exametrika.api.metrics.exa.server.config.ExaAggregatorProbeConfiguration;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Collections;
import com.exametrika.common.utils.SlotAllocator;
import com.exametrika.common.utils.SlotAllocator.Slot;
import com.exametrika.impl.aggregator.common.model.MetricName;
import com.exametrika.impl.metrics.exa.server.probes.ExaAggregatorProbe.CollectorInfo;
import com.exametrika.impl.profiler.probes.BaseProbeCollector;
import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor.Container;
import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor.ThreadLocalSlot;
import com.exametrika.spi.aggregator.common.meters.MeterContainer;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.IScope;
import com.exametrika.spi.profiler.IThreadLocalSlot;


/**
 * The {@link ExaAggregatorProbeCollector} is an Exa aggregator probe collector.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class ExaAggregatorProbeCollector extends BaseProbeCollector {
    final ExaAggregatorProbeConfiguration configuration;
    private final SlotAllocator slotAllocator;
    private final ThreadLocalSlot slot;
    private ArrayList<MeterContainer> meterContainers = new ArrayList<MeterContainer>();
    private int refreshIndex = -1;

    public ExaAggregatorProbeCollector(ExaAggregatorProbeConfiguration configuration, IProbeContext context, IScope scope,
                                       IThreadLocalSlot slot, Container container, SlotAllocator slotAllocator) {
        super(configuration, context, scope, container, null, false, configuration.getComponentType());

        Assert.notNull(slotAllocator);
        Assert.notNull(slot);
        Assert.notNull(container);

        this.slotAllocator = slotAllocator;
        this.slot = (ThreadLocalSlot) slot;
        this.configuration = configuration;
    }

    @Override
    public void begin() {
        super.begin();

        CollectorInfo info = slot.get(false);
        info.collector = this;
    }

    @Override
    public void end() {
        CollectorInfo info = slot.get(false);
        info.collector = null;

        super.end();
    }

    public void onDatabase(int id, JsonObject resourceAllocatorInfo, int currentFileCount, int pagePoolSize,
                           int transactionQueueSize) {
        updateMetersContainers(id >= meterContainers.size());

        ExaRawDbMeterContainer meters = getMeters(id);
        if (meters != null)
            meters.onDatabase(resourceAllocatorInfo, currentFileCount, pagePoolSize, transactionQueueSize);

        extract();
    }

    public void onBeforeFileRead(int id) {
        updateMetersContainers(id >= meterContainers.size());

        ExaRawDbMeterContainer meters = getMeters(id);
        if (meters != null)
            meters.onBeforeFileRead();
    }

    public void onAfterFileRead(int id, int size) {
        updateMetersContainers(id >= meterContainers.size());

        ExaRawDbMeterContainer meters = getMeters(id);
        if (meters != null)
            meters.onAfterFileRead(size);

        extract();
    }

    public void onBeforeFileWritten(int id) {
        updateMetersContainers(id >= meterContainers.size());

        ExaRawDbMeterContainer meters = getMeters(id);
        if (meters != null)
            meters.onBeforeFileWritten();
    }

    public void onAfterFileWritten(int id, int size) {
        updateMetersContainers(id >= meterContainers.size());

        ExaRawDbMeterContainer meters = getMeters(id);
        if (meters != null)
            meters.onAfterFileWritten(size);

        extract();
    }

    public void onFileLoaded(int id) {
        updateMetersContainers(id >= meterContainers.size());

        ExaRawDbMeterContainer meters = getMeters(id);
        if (meters != null)
            meters.onFileLoaded();

        extract();
    }

    public void onFileUnloaded(int id) {
        updateMetersContainers(id >= meterContainers.size());

        ExaRawDbMeterContainer meters = getMeters(id);
        if (meters != null)
            meters.onFileUnloaded();

        extract();
    }

    public void onBeforeLogFlushed(int id) {
        updateMetersContainers(id >= meterContainers.size());

        ExaRawDbMeterContainer meters = getMeters(id);
        if (meters != null)
            meters.onBeforeLogFlushed();
    }

    public void onAfterLogFlushed(int id, long size) {
        updateMetersContainers(id >= meterContainers.size());

        ExaRawDbMeterContainer meters = getMeters(id);
        if (meters != null)
            meters.onAfterLogFlushed(size);

        extract();
    }

    public void onTransactionStarted(int id) {
        updateMetersContainers(id >= meterContainers.size());

        ExaRawDbMeterContainer meters = getMeters(id);
        if (meters != null)
            meters.onTransactionStarted();
    }

    public void onTransactionCommitted(int id) {
        updateMetersContainers(id >= meterContainers.size());

        ExaRawDbMeterContainer meters = getMeters(id);
        if (meters != null)
            meters.onTransactionCommitted();

        extract();
    }

    public void onTransactionRolledBack(int id, Throwable exception) {
        updateMetersContainers(id >= meterContainers.size());

        ExaRawDbMeterContainer meters = getMeters(id);
        if (meters != null)
            meters.onTransactionRolledBack(exception);

        extract();
    }

    public void onPageCache(int id, long pageCacheSize, long maxPageCacheSize, long quota) {
        updateMetersContainers(id >= meterContainers.size());

        ExaCacheMeterContainer meters = getMeters(id);
        if (meters != null)
            meters.onCache(pageCacheSize, maxPageCacheSize, quota);

        extract();
    }

    public void onPageLoaded(int id) {
        updateMetersContainers(id >= meterContainers.size());

        ExaCacheMeterContainer meters = getMeters(id);
        if (meters != null)
            meters.onLoaded();

        extract();
    }

    public void onPageUnloaded(int id, boolean byTimer) {
        updateMetersContainers(id >= meterContainers.size());

        ExaCacheMeterContainer meters = getMeters(id);
        if (meters != null)
            meters.onUnloaded(byTimer);

        extract();
    }

    public void onPageType(int id, long currentRegionsCount, long currentRegionsSize) {
        updateMetersContainers(id >= meterContainers.size());

        ExaPageTypeMeterContainer meters = getMeters(id);
        if (meters != null)
            meters.onPageType(currentRegionsCount, currentRegionsSize);

        extract();
    }

    public void onRegionAllocated(int id, int size) {
        updateMetersContainers(id >= meterContainers.size());

        ExaPageTypeMeterContainer meters = getMeters(id);
        if (meters != null)
            meters.onRegionAllocated(size);

        extract();
    }

    public void onRegionFreed(int id, int size) {
        updateMetersContainers(id >= meterContainers.size());

        ExaPageTypeMeterContainer meters = getMeters(id);
        if (meters != null)
            meters.onRegionFreed(size);

        extract();
    }

    public void onBeforeFullTextAdded(int id) {
        id += 1;
        updateMetersContainers(id >= meterContainers.size());

        ExaFullTextMeterContainer meters = getMeters(id);
        if (meters != null)
            meters.onBeforeFullTextAdded();
    }

    public void onAfterFullTextAdded(int id) {
        id += 1;
        updateMetersContainers(id >= meterContainers.size());

        ExaFullTextMeterContainer meters = getMeters(id);
        if (meters != null)
            meters.onAfterFullTextAdded();

        extract();
    }

    public void onBeforeFullTextUpdated(int id) {
        id += 1;
        updateMetersContainers(id >= meterContainers.size());

        ExaFullTextMeterContainer meters = getMeters(id);
        if (meters != null)
            meters.onBeforeFullTextUpdated();
    }

    public void onAfterFullTextUpdated(int id) {
        id += 1;
        updateMetersContainers(id >= meterContainers.size());

        ExaFullTextMeterContainer meters = getMeters(id);
        if (meters != null)
            meters.onAfterFullTextUpdated();

        extract();
    }

    public void onBeforeFullTextDeleted(int id) {
        id += 1;
        updateMetersContainers(id >= meterContainers.size());

        ExaFullTextMeterContainer meters = getMeters(id);
        if (meters != null)
            meters.onBeforeFullTextDeleted();
    }

    public void onAfterFullTextDeleted(int id) {
        id += 1;
        updateMetersContainers(id >= meterContainers.size());

        ExaFullTextMeterContainer meters = getMeters(id);
        if (meters != null)
            meters.onAfterFullTextDeleted();

        extract();
    }

    public void onBeforeFullTextSearched(int id) {
        id += 1;
        updateMetersContainers(id >= meterContainers.size());

        ExaFullTextMeterContainer meters = getMeters(id);
        if (meters != null)
            meters.onBeforeFullTextSearched();
    }

    public void onAfterFullTextSearched(int id) {
        id += 1;
        updateMetersContainers(id >= meterContainers.size());

        ExaFullTextMeterContainer meters = getMeters(id);
        if (meters != null)
            meters.onAfterFullTextSearched();

        extract();
    }

    public void onBeforeFullTextSearcherUpdated(int id) {
        id += 1;
        updateMetersContainers(id >= meterContainers.size());

        ExaFullTextMeterContainer meters = getMeters(id);
        if (meters != null)
            meters.onBeforeFullTextSearcherUpdated();
    }

    public void onAfterFullTextSearcherUpdated(int id) {
        id += 1;
        updateMetersContainers(id >= meterContainers.size());

        ExaFullTextMeterContainer meters = getMeters(id);
        if (meters != null)
            meters.onAfterFullTextSearcherUpdated();

        extract();
    }

    public void onBeforeFullTextCommitted(int id) {
        id += 1;
        updateMetersContainers(id >= meterContainers.size());

        ExaFullTextMeterContainer meters = getMeters(id);
        if (meters != null)
            meters.onBeforeFullTextCommitted();
    }

    public void onAfterFullTextCommitted(int id) {
        id += 1;
        updateMetersContainers(id >= meterContainers.size());

        ExaFullTextMeterContainer meters = getMeters(id);
        if (meters != null)
            meters.onAfterFullTextCommitted();

        extract();
    }

    public void onNodeCache(int id, long cacheSize, long maxCacheSize, long quota) {
        updateMetersContainers(id >= meterContainers.size());

        ExaCacheMeterContainer meters = getMeters(id);
        if (meters != null)
            meters.onCache(cacheSize, maxCacheSize, quota);

        extract();
    }

    public void onNodeLoaded(int id) {
        updateMetersContainers(id >= meterContainers.size());

        ExaCacheMeterContainer meters = getMeters(id);
        if (meters != null)
            meters.onLoaded();

        extract();
    }

    public void onNodeUnloaded(int id, boolean byTimer) {
        updateMetersContainers(id >= meterContainers.size());

        ExaCacheMeterContainer meters = getMeters(id);
        if (meters != null)
            meters.onUnloaded(byTimer);

        extract();
    }

    public void onNameCache(int id, long cacheSize, long maxCacheSize, long quota) {
        id += 2;
        updateMetersContainers(id >= meterContainers.size());

        ExaCacheMeterContainer meters = getMeters(id);
        if (meters != null)
            meters.onCache(cacheSize, maxCacheSize, quota);

        extract();
    }

    public void onNameLoaded(int id) {
        id += 2;
        updateMetersContainers(id >= meterContainers.size());

        ExaCacheMeterContainer meters = getMeters(id);
        if (meters != null)
            meters.onLoaded();

        extract();
    }

    public void onNameUnloaded(int id, boolean byTimer) {
        id += 2;
        updateMetersContainers(id >= meterContainers.size());

        ExaCacheMeterContainer meters = getMeters(id);
        if (meters != null)
            meters.onUnloaded(byTimer);

        extract();
    }

    public void onBeforeAggregated(int id) {
        id += 3;
        updateMetersContainers(id >= meterContainers.size());

        ExaAggregatorMeterContainer meters = getMeters(id);
        if (meters != null)
            meters.onBeforeAggregated();
    }

    public void onAfterAggregated(int id, int measurementsCount) {
        id += 3;
        updateMetersContainers(id >= meterContainers.size());

        ExaAggregatorMeterContainer meters = getMeters(id);
        if (meters != null)
            meters.onAfterAggregated(measurementsCount);

        extract();
    }

    public void onBeforePeriodClosed(int id) {
        id += 3;
        updateMetersContainers(id >= meterContainers.size());

        ExaAggregatorMeterContainer meters = getMeters(id);
        if (meters != null)
            meters.onBeforePeriodClosed();
    }

    public void onAfterPeriodClosed(int id) {
        id += 3;
        updateMetersContainers(id >= meterContainers.size());

        ExaAggregatorMeterContainer meters = getMeters(id);
        if (meters != null)
            meters.onAfterPeriodClosed();

        extract();
    }

    public void onSelected(int id, long selectedTime, int selectedSize) {
        id += 3;
        updateMetersContainers(id >= meterContainers.size());

        ExaAggregatorMeterContainer meters = getMeters(id);
        if (meters != null)
            meters.onSelected(selectedTime, selectedSize);

        extract();
    }

    @Override
    protected void createMeters() {
    }

    @Override
    protected void updateMetersContainers(boolean force) {
        if (!force && refreshIndex == slotAllocator.getRefreshIndex())
            return;

        refreshIndex = slotAllocator.getRefreshIndex();

        for (int i = 0; i < slotAllocator.getSlotCount(); i++) {
            Slot slot = slotAllocator.getSlot(i);
            if (slot != null) {
                if (i >= meterContainers.size())
                    Collections.set(meterContainers, i, null);

                if (meterContainers.get(i) == null) {
                    MeterContainer meterContainer = createMeterContainer(slot);
                    addMeters(meterContainer);
                    meterContainers.set(i, meterContainer);
                }
            } else if (i < meterContainers.size() && meterContainers.get(i) != null) {
                meterContainers.get(i).delete();
                meterContainers.set(i, null);
            }
        }
    }

    private MeterContainer createMeterContainer(Slot slot) {
        if (slot.componentType.equals("exa.rawdb"))
            return new ExaRawDbMeterContainer(configuration, getMeasurementId(null, MetricName.get(slot.name), slot.componentType),
                    context, container.contextProvider, slot.metadata);
        else if (slot.componentType.equals("exa.rawdb.pageCache") || slot.componentType.equals("exa.exadb.nodeCache") ||
                slot.componentType.equals("exa.aggregator.nameCache"))
            return new ExaCacheMeterContainer(configuration, getMeasurementId(null, MetricName.get(slot.name), slot.componentType),
                    context, container.contextProvider, slot.metadata);
        else if (slot.componentType.equals("exa.rawdb.pageType"))
            return new ExaPageTypeMeterContainer(configuration, getMeasurementId(null, MetricName.get(slot.name), slot.componentType),
                    context, container.contextProvider, slot.metadata);
        else if (slot.componentType.equals("exa.exadb.fullText"))
            return new ExaFullTextMeterContainer(configuration, getMeasurementId(null, MetricName.get(slot.name), slot.componentType),
                    context, container.contextProvider, slot.metadata);
        else if (slot.componentType.equals("exa.aggregator"))
            return new ExaAggregatorMeterContainer(configuration, getMeasurementId(null, MetricName.get(slot.name), slot.componentType),
                    context, container.contextProvider, slot.metadata);
        else
            return Assert.error();
    }

    private <T extends MeterContainer> T getMeters(int id) {
        if (id < meterContainers.size())
            return (T) meterContainers.get(id);
        else
            return null;
    }
}
