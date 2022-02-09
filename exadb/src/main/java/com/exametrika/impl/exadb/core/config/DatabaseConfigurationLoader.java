/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.core.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exametrika.api.exadb.core.config.CacheCategoryTypeConfiguration;
import com.exametrika.api.exadb.core.config.DatabaseConfiguration;
import com.exametrika.api.exadb.core.config.ExpressionCacheCategorizationStrategyConfiguration;
import com.exametrika.common.config.AbstractExtensionLoader;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.config.InvalidConfigurationException;
import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.resource.config.AllocationPolicyConfiguration;
import com.exametrika.common.resource.config.ChildResourceAllocatorConfiguration;
import com.exametrika.common.resource.config.DynamicFixedAllocationPolicyConfiguration;
import com.exametrika.common.resource.config.DynamicPercentageAllocationPolicyConfiguration;
import com.exametrika.common.resource.config.DynamicUniformAllocationPolicyConfiguration;
import com.exametrika.common.resource.config.FixedAllocationPolicyConfiguration;
import com.exametrika.common.resource.config.FixedResourceProviderConfiguration;
import com.exametrika.common.resource.config.FloatingAllocationPolicyConfiguration;
import com.exametrika.common.resource.config.LimitingAllocationPolicyConfiguration;
import com.exametrika.common.resource.config.MemoryResourceProviderConfiguration;
import com.exametrika.common.resource.config.PercentageAllocationPolicyConfiguration;
import com.exametrika.common.resource.config.PercentageResourceProviderConfiguration;
import com.exametrika.common.resource.config.ResourceAllocatorConfiguration;
import com.exametrika.common.resource.config.ResourceProviderConfiguration;
import com.exametrika.common.resource.config.RootResourceAllocatorConfiguration;
import com.exametrika.common.resource.config.SharedMemoryResourceProviderConfiguration;
import com.exametrika.common.resource.config.SharedResourceAllocatorConfiguration;
import com.exametrika.common.resource.config.ThresholdAllocationPolicyConfiguration;
import com.exametrika.common.resource.config.UniformAllocationPolicyConfiguration;
import com.exametrika.common.utils.Pair;
import com.exametrika.spi.exadb.core.config.CacheCategorizationStrategyConfiguration;
import com.exametrika.spi.exadb.core.config.DatabaseExtensionConfiguration;
import com.exametrika.spi.exadb.core.config.DomainServiceConfiguration;


/**
 * The {@link DatabaseConfigurationLoader} is a loader of {@link DatabaseConfiguration}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class DatabaseConfigurationLoader extends AbstractExtensionLoader {
    @Override
    public Object loadExtension(String name, String type, Object object, ILoadContext context) {
        JsonObject element = (JsonObject) object;
        if (type.equals("Database")) {
            String dbName = element.get("name");
            JsonArray paths = element.get("paths");
            String initialSchemaPath = element.get("initialSchemaPath", null);
            long flushPeriod = element.get("flushPeriod");
            long maxFlushSize = element.get("maxFlushSize");
            long timerPeriod = element.get("timerPeriod");
            long batchRunPeriod = element.get("batchRunPeriod");
            long batchIdlePeriod = element.get("batchIdlePeriod");

            Set<DatabaseExtensionConfiguration> extensions = loadExtensions((JsonArray) element.get("extensions"), context);
            Set<DomainServiceConfiguration> domainServices = loadDomainServices((JsonArray) element.get("domainServices"), context);
            ResourceAllocatorConfiguration resourceAllocator = loadResourceAllocator((JsonObject) element.get("resourceAllocator"), context);
            Map<String, CacheCategoryTypeConfiguration> cacheCategoryTypes = loadCacheCategoryTypes((JsonObject) element.get("cacheCategoryTypes"));
            CacheCategoryTypeConfiguration defaultCacheCategoryType = loadCacheCategoryType("", (JsonObject) element.get("defaultCacheCategoryType"));
            CacheCategorizationStrategyConfiguration cacheCategorizationStrategy = loadCacheCategorizationStrategy(
                    (JsonObject) element.get("cacheCategorizationStrategy"), context);

            return new DatabaseConfiguration(dbName, JsonUtils.<String>toList(paths), initialSchemaPath, extensions, domainServices, resourceAllocator,
                    cacheCategoryTypes, defaultCacheCategoryType, cacheCategorizationStrategy, flushPeriod,
                    maxFlushSize, timerPeriod, batchRunPeriod, batchIdlePeriod);
        } else
            throw new InvalidConfigurationException();
    }

    private Set<DatabaseExtensionConfiguration> loadExtensions(JsonArray array, ILoadContext context) {
        Set<DatabaseExtensionConfiguration> extensions = new LinkedHashSet<DatabaseExtensionConfiguration>();
        for (Object element : array) {
            DatabaseExtensionConfiguration extension = load(null, null, element, context);
            extensions.add(extension);
        }

        return extensions;
    }

    private Set<DomainServiceConfiguration> loadDomainServices(JsonArray array, ILoadContext context) {
        Set<DomainServiceConfiguration> domainServices = new LinkedHashSet<DomainServiceConfiguration>();
        for (Object element : array) {
            DomainServiceConfiguration service = load(null, null, element, context);
            domainServices.add(service);
        }

        return domainServices;
    }

    private ResourceAllocatorConfiguration loadResourceAllocator(JsonObject element, ILoadContext context) {
        String type = getType(element);
        if (type.equals("ChildResourceAllocator")) {
            String name = element.get("name");
            Map<String, AllocationPolicyConfiguration> allocationPolicies = loadAllocationPolicies((JsonObject) element.get("allocationPolicies"), context);
            AllocationPolicyConfiguration defaultAllocationPolicy = loadAllocationPolicy((JsonObject) element.get("defaultAllocationPolicy"), context);
            long quotaIncreaseDelay = element.get("quotaIncreaseDelay");
            long initializePeriod = element.get("initializePeriod");
            return new ChildResourceAllocatorConfiguration(name, allocationPolicies, defaultAllocationPolicy, quotaIncreaseDelay, initializePeriod);
        } else if (type.equals("RootResourceAllocator")) {
            String name = element.get("name");
            Map<String, AllocationPolicyConfiguration> allocationPolicies = loadAllocationPolicies((JsonObject) element.get("allocationPolicies"), context);
            AllocationPolicyConfiguration defaultAllocationPolicy = loadAllocationPolicy((JsonObject) element.get("defaultAllocationPolicy"), context);
            long quotaIncreaseDelay = element.get("quotaIncreaseDelay");
            long initializePeriod = element.get("initializePeriod");
            long timerPeriod = element.get("timerPeriod");
            long allocationPeriod = element.get("allocationPeriod");
            ResourceProviderConfiguration resourceProvider = loadResourceProvider((JsonObject) element.get("resourceProvider"), context);

            return new RootResourceAllocatorConfiguration(name, resourceProvider, timerPeriod, allocationPeriod, allocationPolicies,
                    defaultAllocationPolicy, quotaIncreaseDelay, initializePeriod);
        } else if (type.equals("SharedResourceAllocator")) {
            String name = element.get("name");
            Map<String, AllocationPolicyConfiguration> allocationPolicies = loadAllocationPolicies((JsonObject) element.get("allocationPolicies"), context);
            AllocationPolicyConfiguration defaultAllocationPolicy = loadAllocationPolicy((JsonObject) element.get("defaultAllocationPolicy"), context);
            long quotaIncreaseDelay = element.get("quotaIncreaseDelay");
            long initializePeriod = element.get("initializePeriod");
            long timerPeriod = element.get("timerPeriod");
            long allocationPeriod = element.get("allocationPeriod");
            ResourceProviderConfiguration resourceProvider = loadResourceProvider((JsonObject) element.get("resourceProvider"), context);
            String dataExchangeFileName = element.get("dataExchangeFileName");
            long dataExchangePeriod = element.get("dataExchangePeriod");
            long staleAllocatorPeriod = element.get("staleAllocatorPeriod");
            long initialQuota = element.get("initialQuota");

            return new SharedResourceAllocatorConfiguration(dataExchangeFileName, allocationPeriod, dataExchangePeriod,
                    staleAllocatorPeriod, initialQuota, name, resourceProvider, timerPeriod, allocationPolicies,
                    defaultAllocationPolicy, quotaIncreaseDelay, initializePeriod);
        } else
            return load(null, type, element, context);
    }

    private CacheCategorizationStrategyConfiguration loadCacheCategorizationStrategy(JsonObject element, ILoadContext context) {
        String type = getType(element);
        if (type.equals("ExpressionCacheCategorizationStrategy")) {
            String expression = element.get("expression");
            return new ExpressionCacheCategorizationStrategyConfiguration(expression);
        } else
            return load(null, type, element, context);
    }

    private Map<String, CacheCategoryTypeConfiguration> loadCacheCategoryTypes(JsonObject element) {
        Map<String, CacheCategoryTypeConfiguration> map = new LinkedHashMap<String, CacheCategoryTypeConfiguration>();
        for (Map.Entry<String, Object> entry : element)
            map.put(entry.getKey(), loadCacheCategoryType(entry.getKey(), (JsonObject) entry.getValue()));
        return map;
    }

    private CacheCategoryTypeConfiguration loadCacheCategoryType(String name, JsonObject element) {
        long initialCacheSize = element.get("initialCacheSize");
        double minCachePercentage = element.get("minCachePercentage");
        long maxIdlePeriod = element.get("maxIdlePeriod");
        return new CacheCategoryTypeConfiguration(name, initialCacheSize, minCachePercentage, maxIdlePeriod);
    }

    private Map<String, AllocationPolicyConfiguration> loadAllocationPolicies(JsonObject element,
                                                                              ILoadContext context) {
        Map<String, AllocationPolicyConfiguration> map = new LinkedHashMap<String, AllocationPolicyConfiguration>();
        for (Map.Entry<String, Object> entry : element)
            map.put(entry.getKey(), loadAllocationPolicy((JsonObject) entry.getValue(), context));
        return map;
    }

    private AllocationPolicyConfiguration loadAllocationPolicy(JsonObject element, ILoadContext context) {
        String type = getType(element);
        if (type.equals("FixedAllocationPolicy")) {
            List<Pair<String, Long>> quotas = new ArrayList<Pair<String, Long>>();
            for (Map.Entry<String, Object> entry : (JsonObject) element.get("quotas"))
                quotas.add(new Pair<String, Long>(entry.getKey(), (Long) entry.getValue()));
            AllocationPolicyConfiguration otherPolicy = loadAllocationPolicy((JsonObject) element.get("otherPolicy"), context);
            return new FixedAllocationPolicyConfiguration(quotas, otherPolicy);
        } else if (type.equals("PercentageAllocationPolicy")) {
            List<Pair<String, Double>> quotas = new ArrayList<Pair<String, Double>>();
            for (Map.Entry<String, Object> entry : (JsonObject) element.get("quotas"))
                quotas.add(new Pair<String, Double>(entry.getKey(), (Double) entry.getValue()));
            AllocationPolicyConfiguration otherPolicy = loadAllocationPolicy((JsonObject) element.get("otherPolicy"), context);
            return new PercentageAllocationPolicyConfiguration(quotas, otherPolicy);
        } else if (type.equals("FloatingAllocationPolicy")) {
            String floatingSegment = element.get("floatingSegment");
            double reservePercentage = element.get("reservePercentage");
            long minQuota = element.get("minQuota");
            return new FloatingAllocationPolicyConfiguration(floatingSegment, reservePercentage, minQuota);
        } else if (type.equals("ThresholdAllocationPolicy")) {
            List<Pair<Long, AllocationPolicyConfiguration>> thresholds = new ArrayList<Pair<Long, AllocationPolicyConfiguration>>();
            for (Map.Entry<String, Object> entry : (JsonObject) element.get("thresholds"))
                thresholds.add(new Pair<Long, AllocationPolicyConfiguration>(Long.valueOf(entry.getKey()),
                        loadAllocationPolicy((JsonObject) entry.getValue(), context)));
            return new ThresholdAllocationPolicyConfiguration(thresholds);
        } else if (type.equals("UniformAllocationPolicy"))
            return new UniformAllocationPolicyConfiguration();
        else if (type.equals("DynamicFixedAllocationPolicy")) {
            List<Pair<String, Long>> quotas = new ArrayList<Pair<String, Long>>();
            for (Map.Entry<String, Object> entry : (JsonObject) element.get("quotas"))
                quotas.add(new Pair<String, Long>(entry.getKey(), (Long) entry.getValue()));
            AllocationPolicyConfiguration otherPolicy = loadAllocationPolicy((JsonObject) element.get("otherPolicy"), context);
            double underloadedThresholdPercentage = element.get("underloadedThresholdPercentage");
            double overloadedThresholdPercentage = element.get("overloadedThresholdPercentage");
            double underloadedReservePercentage = element.get("underloadedReservePercentage");
            double overloadedReservePercentage = element.get("overloadedReservePercentage");
            long minQuota = element.get("minQuota");
            return new DynamicFixedAllocationPolicyConfiguration(quotas, otherPolicy, underloadedThresholdPercentage,
                    overloadedThresholdPercentage, underloadedReservePercentage, overloadedReservePercentage, minQuota);
        } else if (type.equals("DynamicPercentageAllocationPolicy")) {
            List<Pair<String, Double>> quotas = new ArrayList<Pair<String, Double>>();
            for (Map.Entry<String, Object> entry : (JsonObject) element.get("quotas"))
                quotas.add(new Pair<String, Double>(entry.getKey(), (Double) entry.getValue()));
            AllocationPolicyConfiguration otherPolicy = loadAllocationPolicy((JsonObject) element.get("otherPolicy"), context);
            double underloadedThresholdPercentage = element.get("underloadedThresholdPercentage");
            double overloadedThresholdPercentage = element.get("overloadedThresholdPercentage");
            double underloadedReservePercentage = element.get("underloadedReservePercentage");
            double overloadedReservePercentage = element.get("overloadedReservePercentage");
            long minQuota = element.get("minQuota");
            return new DynamicPercentageAllocationPolicyConfiguration(quotas, otherPolicy, underloadedThresholdPercentage,
                    overloadedThresholdPercentage, underloadedReservePercentage, overloadedReservePercentage, minQuota);
        } else if (type.equals("DynamicUniformAllocationPolicy")) {
            double underloadedThresholdPercentage = element.get("underloadedThresholdPercentage");
            double overloadedThresholdPercentage = element.get("overloadedThresholdPercentage");
            double underloadedReservePercentage = element.get("underloadedReservePercentage");
            double overloadedReservePercentage = element.get("overloadedReservePercentage");
            long minQuota = element.get("minQuota");
            return new DynamicUniformAllocationPolicyConfiguration(underloadedThresholdPercentage,
                    overloadedThresholdPercentage, underloadedReservePercentage, overloadedReservePercentage, minQuota);
        } else if (type.equals("LimitingAllocationPolicy")) {
            AllocationPolicyConfiguration basePolicy = loadAllocationPolicy((JsonObject) element.get("basePolicy"), context);
            double limitPercentage = element.get("limitPercentage");

            return new LimitingAllocationPolicyConfiguration(basePolicy, limitPercentage);
        } else
            return load(null, type, element, context);
    }

    private ResourceProviderConfiguration loadResourceProvider(JsonObject element, ILoadContext context) {
        String type = getType(element);
        if (type.equals("PercentageResourceProvider")) {
            double percentage = element.get("percentage");
            ResourceProviderConfiguration resourceProvider = loadResourceProvider((JsonObject) element.get("resourceProvider"), context);
            return new PercentageResourceProviderConfiguration(resourceProvider, percentage);
        } else if (type.equals("FixedResourceProvider")) {
            long amount = element.get("amount");
            return new FixedResourceProviderConfiguration(amount);
        } else if (type.equals("MemoryResourceProvider")) {
            boolean nativeMemory = element.get("nativeMemory");
            return new MemoryResourceProviderConfiguration(nativeMemory);
        } else if (type.equals("SharedMemoryResourceProvider")) {
            return new SharedMemoryResourceProviderConfiguration();
        } else
            return load(null, type, element, context);
    }
}