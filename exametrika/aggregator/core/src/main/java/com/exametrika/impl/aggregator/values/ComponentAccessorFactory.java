/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.values;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.exametrika.common.services.Services;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.aggregator.IComponentAccessor;
import com.exametrika.spi.aggregator.IComponentAccessorFactory;
import com.exametrika.spi.aggregator.IMetricAccessor;
import com.exametrika.spi.aggregator.IMetricAccessorFactory;
import com.exametrika.spi.aggregator.INavigationAccessorFactory;


/**
 * The {@link ComponentAccessorFactory} is a component accessor factory.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ComponentAccessorFactory implements IComponentAccessorFactory {
    private final Map<String, IMetricAccessorFactory> metricFactoriesMap;
    private final Map<String, INavigationAccessorFactory> navigationFactoriesMap;
    private final Map<String, IComponentAccessor> accessors = new HashMap<String, IComponentAccessor>();

    public ComponentAccessorFactory(Map<String, IMetricAccessorFactory> metricFactoriesMap) {
        Assert.notNull(metricFactoriesMap);

        this.metricFactoriesMap = metricFactoriesMap;

        for (IMetricAccessorFactory factory : metricFactoriesMap.values())
            factory.setComponentAccessorFactory(this);

        List<INavigationAccessorFactory> factories = Services.loadProviders(INavigationAccessorFactory.class);
        Map<String, INavigationAccessorFactory> factoriesMap = new HashMap<String, INavigationAccessorFactory>();
        for (INavigationAccessorFactory factory : factories) {
            for (String navigationType : factory.getTypes())
                Assert.isNull(factoriesMap.put(navigationType, factory));
        }

        this.navigationFactoriesMap = factoriesMap;
    }

    @Override
    public boolean hasMetric(String fieldName) {
        for (String key : metricFactoriesMap.keySet()) {
            if (fieldName.equals(key) || fieldName.startsWith(key + "."))
                return true;
        }

        return false;
    }

    @Override
    public IComponentAccessor createAccessor(String navigationType, String navigationArgs, String fieldName) {
        if (navigationType != null) {
            INavigationAccessorFactory factory = navigationFactoriesMap.get(navigationType);
            Assert.notNull(factory);

            IComponentAccessor accessor = createAccessor(null, null, fieldName);
            if (accessor != null)
                return factory.createAccessor(navigationType, navigationArgs, accessor);
            else
                return null;
        }

        IComponentAccessor accessor = accessors.get(fieldName);
        if (accessor == null) {
            if (!accessors.containsKey(fieldName)) {
                accessor = createAccessor(fieldName);
                accessors.put(fieldName, accessor);
            }
        }

        return accessor;
    }

    private IComponentAccessor createAccessor(String fieldName) {
        String metricName = null;
        for (String key : metricFactoriesMap.keySet()) {
            if (fieldName.equals(key)) {
                metricName = fieldName;
                fieldName = "";
                break;
            } else if (fieldName.startsWith(key + ".")) {
                metricName = fieldName.substring(0, key.length());
                fieldName = fieldName.substring(key.length() + 1);
                break;
            }
        }

        if (metricName == null)
            return null;

        IMetricAccessorFactory factory = metricFactoriesMap.get(metricName);
        Assert.notNull(factory);

        IMetricAccessor metricAccessor = factory.createAccessor(null, null, fieldName);
        if (metricAccessor != null)
            return new ComponentAccessor(metricAccessor, factory.getMetricIndex());
        else
            return null;
    }
}
