/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.name;

import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.exametrika.api.aggregator.common.model.IName;
import com.exametrika.api.exadb.core.config.CacheCategoryTypeConfiguration;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.rawdb.impl.RawDatabase;
import com.exametrika.common.resource.IResourceAllocator;
import com.exametrika.common.resource.IResourceConsumer;
import com.exametrika.common.time.ITimeService;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.SimpleList;
import com.exametrika.common.utils.SimpleList.Element;
import com.exametrika.impl.aggregator.AggregatorInterceptor;


/**
 * The {@link PeriodNameCache} is a cache of names.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class PeriodNameCache implements IResourceConsumer {
    private static final IMessages messages = Messages.get(IMessages.class);
    private final String name;
    private CacheCategoryTypeConfiguration configuration;
    private final PeriodNameManager nameManager;
    private final ITimeService timeService;
    private final IResourceAllocator resourceAllocator;
    private TLongObjectMap<PeriodName> nameByIdMap = new TLongObjectHashMap<PeriodName>();
    private Map<IName, PeriodName> nameByNameMap = new HashMap<IName, PeriodName>();
    private final SimpleList<PeriodName> names = new SimpleList<PeriodName>();
    private volatile long cacheSize;
    private volatile long maxCacheSize;
    private volatile long preparedQuota;
    private volatile long applyQuotaTime;
    private volatile long quota;
    private long unloadCount;
    private long unloadByTimerCount;
    private long unloadByOverflowCount;
    private int refreshIndex;
    private long refreshCacheSize;

    public PeriodNameCache(String name, PeriodNameManager nameManager, ITimeService timeService,
                           IResourceAllocator resourceAllocator, CacheCategoryTypeConfiguration configuration) {
        Assert.notNull(name);
        Assert.notNull(nameManager);
        Assert.notNull(timeService);
        Assert.notNull(resourceAllocator);
        Assert.notNull(configuration);

        this.name = name;
        this.nameManager = nameManager;
        this.timeService = timeService;
        this.resourceAllocator = resourceAllocator;
        this.configuration = configuration;
        this.maxCacheSize = configuration.getInitialCacheSize();
        this.quota = maxCacheSize;

        resourceAllocator.register(getResourceConsumerName(), this);
    }

    public int getRefreshIndex() {
        return refreshIndex;
    }

    public CacheCategoryTypeConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(CacheCategoryTypeConfiguration configuration) {
        Assert.notNull(configuration);

        this.configuration = configuration;
    }

    public void addName(PeriodName name, boolean created) {
        Assert.checkState(nameByIdMap.put(name.getId(), name) == null);
        Assert.checkState(nameByNameMap.put(name.getName(), name) == null);

        if (created)
            nameManager.onNameNew(name);

        cacheSize += name.getCacheSize();
        renewName(name, false);

        unloadExcessive();
    }

    public int renewName(PeriodName name, boolean renew) {
        name.setLastAccessTime((int) (timeService.getCurrentTime() >>> 13));

        Element element = name.getElement();
        element.remove();
        element.reset();

        names.addLast(element);
        AggregatorInterceptor.INSTANCE.onNameLoaded(((RawDatabase) nameManager.getContext().getRawDatabase()).getInterceptId());
        updateRefreshCacheSize(name.getCacheSize(), renew);
        return preparedQuota == 0 ? refreshIndex : -1;
    }

    public void removeName(PeriodName name) {
        nameByIdMap.remove(name.getId());
        nameByNameMap.remove(name.getName());
        cacheSize -= name.getCacheSize();
        Assert.checkState(cacheSize >= 0);

        if (name.getRefreshIndex() == refreshIndex)
            updateRefreshCacheSize(-name.getCacheSize(), false);
    }

    public PeriodName findById(long id) {
        PeriodName name = nameByIdMap.get(id);
        if (name != null)
            name.refresh();

        return name;
    }

    public PeriodName findByName(IName n) {
        Assert.notNull(n);

        PeriodName name = nameByNameMap.get(n);
        if (name != null)
            name.refresh();

        return name;
    }

    public void close() {
        unloadNames(true);

        resourceAllocator.unregister(getResourceConsumerName());
        setQuota(configuration.getInitialCacheSize());
    }

    public void unloadNames(boolean removeAll) {
        unloadUsedNames(removeAll, false);

        if (removeAll) {
            Assert.checkState(names.isEmpty() && cacheSize == 0);
            nameByIdMap = new TLongObjectHashMap<PeriodName>();
            nameByNameMap = new HashMap<IName, PeriodName>();
        }
    }

    public void unloadExcessive() {
        applyQuota();

        if (cacheSize > maxCacheSize)
            unloadUsedNames(false, true);
    }

    public void onTimer(long currentTime) {
        if (!names.isEmpty()) {
            refreshIndex++;

            currentTime = currentTime >>> 13;
            int maxIdlePeriod = (int) (configuration.getMaxIdlePeriod() >>> 13);

            PeriodName name = names.getFirst().getValue();
            if (currentTime - name.getLastAccessTime() > maxIdlePeriod)
                unloadNames(false);

            updateRefreshCacheSize(0, false);
            unloadExcessive();
        }

        AggregatorInterceptor.INSTANCE.onNameCache(((RawDatabase) nameManager.getContext().getRawDatabase()).getInterceptId(),
                cacheSize, maxCacheSize, quota);
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
        if (quota >= maxCacheSize)
            maxCacheSize = quota;
        else {
            if (preparedQuota == 0)
                applyQuotaTime = timeService.getCurrentTime() + nameManager.getConfiguration().getTimerPeriod() + 1000;

            preparedQuota = quota;
        }
    }

    public String printStatistics() {
        return messages.statistics(!name.isEmpty() ? name : "default", configuration.toString(), maxCacheSize, cacheSize, unloadCount,
                unloadByOverflowCount, unloadByTimerCount).toString();
    }

    @Override
    public String toString() {
        return getResourceConsumerName();
    }

    private void unloadUsedNames(boolean removeAll, boolean exceedsMaxSize) {
        if (names.isEmpty())
            return;

        applyQuota();

        if (exceedsMaxSize)
            unloadByOverflowCount++;
        else
            unloadByTimerCount++;

        int currentTime = (int) (timeService.getCurrentTime() >>> 13);
        int maxIdlePeriod = (int) (configuration.getMaxIdlePeriod() >>> 13);

        PeriodName name = names.getFirst().getValue();
        if (!removeAll && currentTime - name.getLastAccessTime() <= maxIdlePeriod &&
                cacheSize <= maxCacheSize)
            return;

        long minCacheSize = (long) (maxCacheSize * configuration.getMinCachePercentage() / 100);

        for (Iterator<Element<PeriodName>> it = names.iterator(); it.hasNext(); ) {
            name = it.next().getValue();

            if (!removeAll && name.getRefreshIndex() == refreshIndex)
                break;

            if (removeAll || currentTime - name.getLastAccessTime() > maxIdlePeriod || cacheSize > minCacheSize) {
                if (exceedsMaxSize) {
                    if (name.isNew())
                        nameManager.setBigTransaction();
                }

                it.remove();
                removeName(name);
                name.setStale();
                unloadCount++;
                AggregatorInterceptor.INSTANCE.onNameUnloaded(((RawDatabase) nameManager.getContext().getRawDatabase()).getInterceptId(),
                        !exceedsMaxSize);
            } else
                break;
        }
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

    private String getResourceConsumerName() {
        return "heap.perfdb.names." + (!name.isEmpty() ? name : "<default>");
    }

    private interface IMessages {
        @DefaultMessage("name cache ''{0}:{1}'' - max cache size: {2}, cache size: {3}, unload count: {4}, " +
                "unloadByOverflowCount: {5}, unloadByTimerCount: {6}")
        ILocalizedMessage statistics(String category, String categoryType, long maxCacheSize, long cacheSize,
                                     long unloadCount, long unloadByOverflowCount, long unloadByTimerCount);
    }
}
