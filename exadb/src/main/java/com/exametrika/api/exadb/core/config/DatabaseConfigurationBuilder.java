/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.core.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exametrika.common.resource.config.ResourceAllocatorConfiguration;
import com.exametrika.common.resource.config.RootResourceAllocatorConfigurationBuilder;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.exadb.core.config.CacheCategorizationStrategyConfiguration;
import com.exametrika.spi.exadb.core.config.DatabaseExtensionConfiguration;
import com.exametrika.spi.exadb.core.config.DomainServiceConfiguration;


/**
 * The {@link DatabaseConfigurationBuilder} is a database configuration builder.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class DatabaseConfigurationBuilder {
    private String name = "exadb";
    private List<String> paths = new ArrayList<String>();
    private String initialSchemaPath;
    private final Set<DatabaseExtensionConfiguration> extensions = new LinkedHashSet<DatabaseExtensionConfiguration>();
    private final Set<DomainServiceConfiguration> domainServices = new LinkedHashSet<DomainServiceConfiguration>();
    private ResourceAllocatorConfiguration resourceAllocator = new RootResourceAllocatorConfigurationBuilder().toConfiguration();
    private Map<String, CacheCategoryTypeConfiguration> cacheCategoryTypes = new LinkedHashMap<String, CacheCategoryTypeConfiguration>();
    private CacheCategoryTypeConfiguration defaultCacheCategoryType = new CacheCategoryTypeConfiguration("",
            10000000, 90, 600000);
    private CacheCategorizationStrategyConfiguration cacheCategorizationStrategy =
            new ExpressionCacheCategorizationStrategyConfiguration("['','']");
    private long flushPeriod = 3000;
    private long maxFlushSize = Long.MAX_VALUE;
    private long timerPeriod = 1000;
    private long batchRunPeriod = 100;
    private long batchIdlePeriod = 900;

    public DatabaseConfigurationBuilder addPath(String path) {
        Assert.notNull(path);

        this.paths.add(path);
        return this;
    }

    public DatabaseConfigurationBuilder setName(String name) {
        this.name = name;
        return this;
    }

    public DatabaseConfigurationBuilder setInitialSchemaPath(String value) {
        this.initialSchemaPath = value;
        return this;
    }

    public DatabaseConfigurationBuilder addExtension(DatabaseExtensionConfiguration extension) {
        Assert.notNull(extension);

        extensions.add(extension);
        return this;
    }

    public DatabaseConfigurationBuilder addDomainService(DomainServiceConfiguration service) {
        Assert.notNull(service);

        domainServices.add(service);
        return this;
    }

    public DatabaseConfigurationBuilder setResourceAllocator(ResourceAllocatorConfiguration resourceAllocator) {
        Assert.notNull(resourceAllocator);

        this.resourceAllocator = resourceAllocator;
        return this;
    }

    public DatabaseConfigurationBuilder addCacheCategoryType(CacheCategoryTypeConfiguration categoryType) {
        Assert.notNull(categoryType);

        Assert.isTrue(this.cacheCategoryTypes.put(categoryType.getName(), categoryType) == null);
        return this;
    }

    public DatabaseConfigurationBuilder setDefaultCacheCategoryType(CacheCategoryTypeConfiguration categoryType) {
        Assert.notNull(categoryType);

        this.defaultCacheCategoryType = categoryType;
        return this;
    }

    public DatabaseConfigurationBuilder setCacheCategorizationStrategy(CacheCategorizationStrategyConfiguration cacheCategorizationStrategy) {
        Assert.notNull(cacheCategorizationStrategy);

        this.cacheCategorizationStrategy = cacheCategorizationStrategy;
        return this;
    }

    public DatabaseConfigurationBuilder setFlushPeriod(long flushPeriod) {
        this.flushPeriod = flushPeriod;
        return this;
    }

    public DatabaseConfigurationBuilder setMaxFlushSize(long maxFlushSize) {
        this.maxFlushSize = maxFlushSize;
        return this;
    }

    public DatabaseConfigurationBuilder setTimerPeriod(long timerPeriod) {
        this.timerPeriod = timerPeriod;
        return this;
    }

    public DatabaseConfigurationBuilder setBatchRunPeriod(long batchRunPeriod) {
        this.batchRunPeriod = batchRunPeriod;
        return this;
    }

    public DatabaseConfigurationBuilder setBatchIdlePeriod(long batchIdlePeriod) {
        this.batchIdlePeriod = batchIdlePeriod;
        return this;
    }

    public DatabaseConfiguration toConfiguration() {
        return new DatabaseConfiguration(name, new ArrayList<String>(paths), initialSchemaPath, new LinkedHashSet<DatabaseExtensionConfiguration>(extensions),
                new LinkedHashSet<DomainServiceConfiguration>(domainServices), resourceAllocator,
                new LinkedHashMap<String, CacheCategoryTypeConfiguration>(cacheCategoryTypes), defaultCacheCategoryType, cacheCategorizationStrategy,
                flushPeriod, maxFlushSize, timerPeriod, batchRunPeriod, batchIdlePeriod);
    }
}
