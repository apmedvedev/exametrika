/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.core.config;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exametrika.common.config.Configuration;
import com.exametrika.common.resource.config.ResourceAllocatorConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.exadb.core.config.CacheCategorizationStrategyConfiguration;
import com.exametrika.spi.exadb.core.config.DatabaseExtensionConfiguration;
import com.exametrika.spi.exadb.core.config.DomainServiceConfiguration;


/**
 * The {@link DatabaseConfiguration} is a database configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class DatabaseConfiguration extends Configuration {
    public static final String SCHEMA = "com.exametrika.exadb-1.0";
    private final String name;
    private final List<String> paths;
    private final String initialSchemaPath;
    private final Map<String, DatabaseExtensionConfiguration> extensions;
    private final Map<String, DomainServiceConfiguration> domainServices;
    private final ResourceAllocatorConfiguration resourceAllocator;
    private final Map<String, CacheCategoryTypeConfiguration> cacheCategoryTypes;
    private final CacheCategoryTypeConfiguration defaultCacheCategoryType;
    private final CacheCategorizationStrategyConfiguration cacheCategorizationStrategy;
    private final long flushPeriod;
    private final long maxFlushSize;
    private final long timerPeriod;
    private final long batchRunPeriod;
    private final long batchIdlePeriod;

    public DatabaseConfiguration(String name, List<String> paths, String initialSchemaPath,
                                 Set<? extends DatabaseExtensionConfiguration> extensions,
                                 Set<? extends DomainServiceConfiguration> domainServices,
                                 ResourceAllocatorConfiguration resourceAllocator, Map<String, CacheCategoryTypeConfiguration> cacheCategoryTypes,
                                 CacheCategoryTypeConfiguration defaultCacheCategoryType, CacheCategorizationStrategyConfiguration cacheCategorizationStrategy,
                                 long flushPeriod, long maxFlushSize, long timerPeriod, long batchRunPeriod, long batchIdlePeriod) {
        Assert.notNull(name);
        Assert.notNull(paths);
        Assert.isTrue(!paths.isEmpty());
        Assert.notNull(extensions);
        Assert.notNull(domainServices);
        Assert.notNull(resourceAllocator);
        Assert.notNull(cacheCategoryTypes);
        Assert.notNull(defaultCacheCategoryType);
        Assert.notNull(cacheCategorizationStrategy);

        this.name = name;
        this.paths = Immutables.wrap(paths);
        this.initialSchemaPath = initialSchemaPath;

        Map<String, DatabaseExtensionConfiguration> extensionsMap = new LinkedHashMap<String, DatabaseExtensionConfiguration>();
        for (DatabaseExtensionConfiguration extension : extensions)
            Assert.isTrue(extensionsMap.put(extension.getName(), extension) == null);
        this.extensions = Immutables.wrap(extensionsMap);

        Map<String, DomainServiceConfiguration> domainServicesMap = new LinkedHashMap<String, DomainServiceConfiguration>();
        for (DomainServiceConfiguration domainService : domainServices)
            Assert.isTrue(domainServicesMap.put(domainService.getName(), domainService) == null);

        this.domainServices = Immutables.wrap(domainServicesMap);

        this.resourceAllocator = resourceAllocator;
        this.cacheCategoryTypes = Immutables.wrap(cacheCategoryTypes);
        this.defaultCacheCategoryType = defaultCacheCategoryType;
        this.cacheCategorizationStrategy = cacheCategorizationStrategy;
        this.flushPeriod = flushPeriod;
        this.maxFlushSize = maxFlushSize;
        this.timerPeriod = timerPeriod;
        this.batchRunPeriod = batchRunPeriod;
        this.batchIdlePeriod = batchIdlePeriod;
    }

    public String getName() {
        return name;
    }

    public List<String> getPaths() {
        return paths;
    }

    public String getInitialSchemaPath() {
        return initialSchemaPath;
    }

    public Map<String, DatabaseExtensionConfiguration> getExtensions() {
        return extensions;
    }

    public Map<String, DomainServiceConfiguration> getDomainServices() {
        return domainServices;
    }

    public ResourceAllocatorConfiguration getResourceAllocator() {
        return resourceAllocator;
    }

    public Map<String, CacheCategoryTypeConfiguration> getCacheCategoryTypes() {
        return cacheCategoryTypes;
    }

    public CacheCategoryTypeConfiguration getDefaultCacheCategoryType() {
        return defaultCacheCategoryType;
    }

    public CacheCategorizationStrategyConfiguration getCacheCategorizationStrategy() {
        return cacheCategorizationStrategy;
    }

    public long getFlushPeriod() {
        return flushPeriod;
    }

    public long getMaxFlushSize() {
        return maxFlushSize;
    }

    public long getTimerPeriod() {
        return timerPeriod;
    }

    public long getBatchRunPeriod() {
        return batchRunPeriod;
    }

    public long getBatchIdlePeriod() {
        return batchIdlePeriod;
    }

    public boolean isCompatible(DatabaseConfiguration configuration) {
        Assert.notNull(configuration);

        for (DatabaseExtensionConfiguration extension : extensions.values()) {
            DatabaseExtensionConfiguration newExtension = configuration.getExtensions().get(extension.getName());
            if (!extension.isCompatible(newExtension))
                return false;
        }

        for (DomainServiceConfiguration service : domainServices.values()) {
            DomainServiceConfiguration newService = configuration.getDomainServices().get(service.getName());
            if (!service.isCompatible(newService))
                return false;
        }
        return name.equals(configuration.name) && paths.equals(configuration.paths) &&
                defaultCacheCategoryType.getName().equals(configuration.defaultCacheCategoryType.getName()) &&
                cacheCategoryTypes.keySet().equals(configuration.cacheCategoryTypes.keySet());
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof DatabaseConfiguration))
            return false;

        DatabaseConfiguration configuration = (DatabaseConfiguration) o;
        return name.equals(configuration.name) && paths.equals(configuration.paths) &&
                Objects.equals(initialSchemaPath, configuration.initialSchemaPath) &&
                extensions.equals(configuration.extensions) &&
                domainServices.equals(configuration.domainServices) &&
                resourceAllocator.equals(configuration.resourceAllocator) &&
                cacheCategoryTypes.equals(configuration.cacheCategoryTypes) &&
                defaultCacheCategoryType.equals(configuration.defaultCacheCategoryType) &&
                cacheCategorizationStrategy.equals(configuration.cacheCategorizationStrategy) &&
                flushPeriod == configuration.flushPeriod && maxFlushSize == configuration.maxFlushSize &&
                timerPeriod == configuration.timerPeriod &&
                batchRunPeriod == configuration.batchRunPeriod && batchIdlePeriod == configuration.batchIdlePeriod;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(name, paths, initialSchemaPath, extensions, domainServices, resourceAllocator,
                cacheCategoryTypes, defaultCacheCategoryType, cacheCategorizationStrategy, flushPeriod, maxFlushSize,
                timerPeriod, batchRunPeriod, batchIdlePeriod);
    }

    @Override
    public String toString() {
        return paths.toString();
    }
}
