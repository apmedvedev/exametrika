/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.values;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.spi.aggregator.common.values.IAggregationSchema;
import com.exametrika.spi.aggregator.common.values.IComponentTypeAggregationSchema;


/**
 * The {@link AggregationSchema} is an implementation of {@link IAggregationSchema}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class AggregationSchema implements IAggregationSchema {
    private final List<IComponentTypeAggregationSchema> componentTypes;
    private final Map<String, IComponentTypeAggregationSchema> componentTypesMap;
    private final int version;

    public AggregationSchema(Set<ComponentValueSchemaConfiguration> configurations, int version) {
        Assert.notNull(configurations);

        List<IComponentTypeAggregationSchema> componentTypes = new ArrayList<IComponentTypeAggregationSchema>();
        Map<String, IComponentTypeAggregationSchema> componentTypesMap = new HashMap<String, IComponentTypeAggregationSchema>();
        for (ComponentValueSchemaConfiguration configuration : configurations) {
            IComponentTypeAggregationSchema componentType = new ComponentTypeAggregationSchema(configuration);
            componentTypes.add(componentType);
            IComponentTypeAggregationSchema oldComponentType = componentTypesMap.put(configuration.getName(), componentType);
            Assert.isTrue(oldComponentType == null || oldComponentType.getConfiguration().equals(componentType.getConfiguration()),
                    "Component type ''{0}'' is not unique. Component types: {1}", componentType, componentTypesMap.keySet());
        }

        this.version = version;
        this.componentTypes = Immutables.wrap(componentTypes);
        this.componentTypesMap = componentTypesMap;
    }

    @Override
    public int getVersion() {
        return version;
    }

    @Override
    public List<IComponentTypeAggregationSchema> getComponentTypes() {
        return componentTypes;
    }

    @Override
    public IComponentTypeAggregationSchema findComponentType(String componentType) {
        Assert.notNull(componentType);

        return componentTypesMap.get(componentType);
    }
}
