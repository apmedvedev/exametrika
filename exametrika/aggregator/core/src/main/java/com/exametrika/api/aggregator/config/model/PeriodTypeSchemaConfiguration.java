/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.model;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.exametrika.api.exadb.jobs.config.model.StandardSchedulePeriodSchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.exadb.core.config.schema.SchemaConfiguration;


/**
 * The {@link PeriodTypeSchemaConfiguration} represents a configuration of schema of period type.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class PeriodTypeSchemaConfiguration extends SchemaConfiguration {
    private final Set<AggregationComponentTypeSchemaConfiguration> componentTypes;
    private final Map<String, AggregationComponentTypeSchemaConfiguration> componentTypesMap;
    private final StandardSchedulePeriodSchemaConfiguration period;
    private final int cyclePeriodCount;
    private final boolean nonAggregating;
    private final String parentDomain;

    public PeriodTypeSchemaConfiguration(String name, Set<AggregationComponentTypeSchemaConfiguration> componentTypes,
                                         StandardSchedulePeriodSchemaConfiguration period, int cyclePeriodCount, boolean nonAggregating, String parentDomain) {
        super(name, name, null);

        Assert.notNull(componentTypes);
        Assert.notNull(period);
        Assert.isTrue(!nonAggregating || cyclePeriodCount == 1);

        Map<String, AggregationComponentTypeSchemaConfiguration> componentTypesMap = new LinkedHashMap<String, AggregationComponentTypeSchemaConfiguration>();
        for (AggregationComponentTypeSchemaConfiguration componentType : componentTypes) {
            Assert.isNull(componentTypesMap.put(componentType.getName(), componentType));

            if (nonAggregating && componentType instanceof NameSchemaConfiguration) {
                Assert.isTrue(((NameSchemaConfiguration) componentType).getComponentDiscoveryStrategies().isEmpty());
                Assert.isNull(((NameSchemaConfiguration) componentType).getComponentDeletionStrategy());
            }

            if (nonAggregating && componentType instanceof PrimaryEntryPointSchemaConfiguration) {
                Assert.isTrue(((PrimaryEntryPointSchemaConfiguration) componentType).getComponentDiscoveryStrategies().isEmpty());
                Assert.isNull(((PrimaryEntryPointSchemaConfiguration) componentType).getComponentDeletionStrategy());
            }
        }

        this.componentTypes = Immutables.wrap(componentTypes);
        this.componentTypesMap = componentTypesMap;
        this.period = period;
        this.cyclePeriodCount = cyclePeriodCount;
        this.nonAggregating = nonAggregating;
        this.parentDomain = parentDomain;
    }

    public Set<AggregationComponentTypeSchemaConfiguration> getComponentTypes() {
        return componentTypes;
    }

    public AggregationComponentTypeSchemaConfiguration findComponentType(String componentType) {
        Assert.notNull(componentType);

        return componentTypesMap.get(componentType);
    }

    public StandardSchedulePeriodSchemaConfiguration getPeriod() {
        return period;
    }

    public int getCyclePeriodCount() {
        return cyclePeriodCount;
    }

    public boolean isNonAggregating() {
        return nonAggregating;
    }

    public String getParentDomain() {
        return parentDomain;
    }

    @Override
    public <T extends SchemaConfiguration> T combine(T schema) {
        PeriodTypeSchemaConfiguration periodTypeSchema = (PeriodTypeSchemaConfiguration) schema;

        Set<AggregationComponentTypeSchemaConfiguration> componentTypes = new LinkedHashSet<AggregationComponentTypeSchemaConfiguration>();
        Map<String, AggregationComponentTypeSchemaConfiguration> componentTypesMap =
                new LinkedHashMap<String, AggregationComponentTypeSchemaConfiguration>(this.componentTypesMap);
        for (AggregationComponentTypeSchemaConfiguration componentType : periodTypeSchema.getComponentTypes())
            componentTypes.add(combine(componentType, componentTypesMap));
        componentTypes.addAll(componentTypesMap.values());

        return (T) new PeriodTypeSchemaConfiguration(combine(getName(), schema.getName()), componentTypes, combine(period, periodTypeSchema.getPeriod()),
                combine(cyclePeriodCount, periodTypeSchema.getCyclePeriodCount()),
                combine(nonAggregating, periodTypeSchema.isNonAggregating()).booleanValue(),
                combine(parentDomain, periodTypeSchema.getParentDomain()));
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof PeriodTypeSchemaConfiguration))
            return false;

        PeriodTypeSchemaConfiguration configuration = (PeriodTypeSchemaConfiguration) o;
        return super.equals(configuration) && componentTypes.equals(configuration.componentTypes) &&
                period.equals(configuration.period) && cyclePeriodCount == configuration.cyclePeriodCount &&
                nonAggregating == configuration.nonAggregating && Objects.equals(parentDomain, configuration.parentDomain);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(componentTypes, period, cyclePeriodCount, nonAggregating, parentDomain);
    }
}
