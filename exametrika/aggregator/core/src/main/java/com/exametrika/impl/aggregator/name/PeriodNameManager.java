/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.name;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.exametrika.api.aggregator.IPeriodName;
import com.exametrika.api.aggregator.IPeriodNameManager;
import com.exametrika.api.aggregator.common.model.ICallPath;
import com.exametrika.api.aggregator.common.model.IMetricName;
import com.exametrika.api.aggregator.common.model.IName;
import com.exametrika.api.aggregator.common.model.Names;
import com.exametrika.api.aggregator.config.schema.NameSpaceSchemaConfiguration;
import com.exametrika.api.exadb.core.config.CacheCategoryTypeConfiguration;
import com.exametrika.api.exadb.core.config.DatabaseConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.MapBuilder;
import com.exametrika.common.utils.Pair;
import com.exametrika.impl.aggregator.forecast.IBehaviorTypeIdAllocator;
import com.exametrika.spi.aggregator.common.model.INameDictionary;
import com.exametrika.spi.exadb.core.ICacheControl;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.spi.exadb.core.IExtensionSpace;


/**
 * The {@link PeriodNameManager} is a manager of names.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class PeriodNameManager implements IPeriodNameManager, ICacheControl, IExtensionSpace, INameDictionary, IBehaviorTypeIdAllocator {
    private final IDatabaseContext context;
    private final PeriodNameCache nameCache;
    private final Set<PeriodName> newNames = new LinkedHashSet<PeriodName>();
    private DatabaseConfiguration configuration;
    private PeriodNameSpace nameSpace;
    private boolean bigTransaction;
    private NameSpaceSchemaConfiguration schema = new NameSpaceSchemaConfiguration();

    public PeriodNameManager(IDatabaseContext context) {
        Assert.notNull(context);

        this.context = context;
        this.configuration = context.getConfiguration();

        Pair<String, String> pair = context.getCacheCategorizationStrategy().categorize(new MapBuilder<String, String>()
                .put("type", "names.period")
                .toMap());

        String category = pair.getKey();
        String categoryType = pair.getValue();
        CacheCategoryTypeConfiguration categoryTypeConfiguration;
        if (categoryType == null || categoryType.isEmpty())
            categoryTypeConfiguration = configuration.getDefaultCacheCategoryType();
        else {
            categoryTypeConfiguration = configuration.getCacheCategoryTypes().get(categoryType);
            Assert.notNull(categoryTypeConfiguration);
        }

        nameCache = new PeriodNameCache(category, this, context.getTimeService(), context.getResourceAllocator(), categoryTypeConfiguration);
    }

    public DatabaseConfiguration getConfiguration() {
        return configuration;
    }

    public IDatabaseContext getContext() {
        return context;
    }

    public void setSchema(NameSpaceSchemaConfiguration schema) {
        this.schema = schema;
    }

    public PeriodNameCache getNameCache() {
        return nameCache;
    }

    public void setConfiguration(DatabaseConfiguration configuration, boolean clearCache) {
        this.configuration = configuration;

        if (nameCache.getConfiguration().getName().isEmpty())
            nameCache.setConfiguration(configuration.getDefaultCacheCategoryType());
        else
            nameCache.setConfiguration(configuration.getCacheCategoryTypes().get(nameCache.getConfiguration().getName()));
    }

    public void onNameNew(PeriodName name) {
        newNames.add(name);
    }

    @Override
    public void validate() {
    }

    @Override
    public void onTransactionStarted() {
    }

    @Override
    public void onTransactionCommitted() {
        for (PeriodName name : newNames)
            name.clearNew();

        newNames.clear();
        bigTransaction = false;
    }

    @Override
    public boolean onBeforeTransactionRolledBack() {
        return false;
    }

    @Override
    public void onTransactionRolledBack() {
        if (!newNames.isEmpty()) {
            for (PeriodName name : newNames) {
                name.getElement().remove();

                nameCache.removeName(name);
                name.setStale();
            }

            newNames.clear();
        }

        if (bigTransaction) {
            clear(true);
            bigTransaction = false;
        }
    }

    @Override
    public void flush(boolean full) {
    }

    @Override
    public void clear(boolean full) {
        nameCache.unloadNames(full);
        if (full)
            nameSpace = null;
    }

    @Override
    public void unloadExcessive() {
    }

    @Override
    public void setCachingEnabled(boolean value) {
    }

    @Override
    public void setMaxCacheSize(String category, long value) {
    }

    public void setBigTransaction() {
        bigTransaction = true;
    }

    public void close() {
        nameCache.close();
        nameSpace = null;
    }

    public void onTimer(long currentTime) {
        nameCache.onTimer(currentTime);
    }

    public String printStatistics() {
        return nameCache.printStatistics();
    }

    @Override
    public List<String> getFiles() {
        ensureSpace();
        return nameSpace.getFiles();
    }

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public void create() {
        nameSpace = PeriodNameSpace.create(context, nameCache, schema);
    }

    @Override
    public void open() {
        ensureSpace();
    }

    @Override
    public IPeriodName addName(IName name) {
        Assert.notNull(name);

        ensureSpace();
        return nameSpace.addName(name);
    }

    @Override
    public IPeriodName findById(long id) {
        ensureSpace();
        return nameSpace.findById(id);
    }

    @Override
    public IPeriodName findByName(IName name) {
        Assert.notNull(name);

        ensureSpace();
        return nameSpace.findByName(name);
    }

    @Override
    public IName getName(long persistentNameId) {
        IPeriodName name = findById(persistentNameId);
        if (name != null)
            return name.getName();
        else
            return null;
    }

    @Override
    public long getName(IName name) {
        Assert.notNull(name);
        if (name.isEmpty())
            return 0;

        IPeriodName periodName = addName(name);
        return periodName.getId();
    }

    @Override
    public long getCallPath(long parentCallPathId, long metricId) {
        if (parentCallPathId == 0 && metricId == 0)
            return 0;

        Assert.isTrue(metricId > 0);

        ICallPath parentCallPath;
        if (parentCallPathId != 0) {
            IPeriodName parentName = findById(parentCallPathId);
            Assert.notNull(parentName);
            parentCallPath = parentName.getName();
        } else
            parentCallPath = Names.rootCallPath();

        IPeriodName metricName = findById(metricId);
        Assert.notNull(metricName);

        ICallPath callPath = Names.getCallPath(parentCallPath, (IMetricName) metricName.getName());
        IPeriodName periodName = addName(callPath);
        return periodName.getId();
    }

    @Override
    public int allocateTypeId() {
        ensureSpace();
        return nameSpace.allocateTypeId();
    }

    private void ensureSpace() {
        if (nameSpace == null)
            nameSpace = PeriodNameSpace.open(context, nameCache, schema);
    }
}
