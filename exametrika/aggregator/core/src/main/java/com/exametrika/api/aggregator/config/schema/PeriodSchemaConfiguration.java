/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.schema;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.exametrika.api.exadb.core.schema.ISpaceSchema;
import com.exametrika.api.exadb.jobs.config.model.StandardSchedulePeriodSchemaConfiguration;
import com.exametrika.api.exadb.jobs.config.model.StandardSchedulePeriodSchemaConfiguration.Kind;
import com.exametrika.api.exadb.jobs.config.model.StandardSchedulePeriodSchemaConfiguration.UnitType;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.exadb.core.Constants;
import com.exametrika.spi.aggregator.config.schema.PeriodNodeSchemaConfiguration;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.spi.exadb.core.config.schema.SchemaConfiguration;
import com.exametrika.spi.exadb.core.config.schema.SpaceSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.NodeSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.NodeSpaceSchemaConfiguration;


/**
 * The {@link PeriodSchemaConfiguration} represents a configuration of schema of period.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class PeriodSchemaConfiguration extends NodeSpaceSchemaConfiguration {
    private final String cyclePeriodRootNodeType;
    private final StandardSchedulePeriodSchemaConfiguration period;
    private final int cyclePeriodCount;
    private final boolean nonAggregating;
    private final String parentDomain;
    private final int totalLocationIndexCount;

    public PeriodSchemaConfiguration(String name, Set<? extends PeriodNodeSchemaConfiguration> nodes,
                                     String rootNodeType, String cyclePeriodRootNodeType, int duration, int cyclePeriodCount, boolean nonAggregating,
                                     String parentDomain) {
        this(name, name, null, nodes, rootNodeType, cyclePeriodRootNodeType, new StandardSchedulePeriodSchemaConfiguration(
                UnitType.MILLISECOND, Kind.RELATIVE, duration), cyclePeriodCount, nonAggregating, parentDomain);
    }

    public PeriodSchemaConfiguration(String name, String alias, String description,
                                     Set<? extends PeriodNodeSchemaConfiguration> nodes, String rootNodeType, String cyclePeriodRootNodeType,
                                     StandardSchedulePeriodSchemaConfiguration period, int cyclePeriodCount, boolean nonAggregating, String parentDomain) {
        this(name, alias, description, nodes, rootNodeType, cyclePeriodRootNodeType, period, cyclePeriodCount, nonAggregating,
                parentDomain, true);
    }

    public PeriodSchemaConfiguration(String name, String alias, String description,
                                     Set<? extends PeriodNodeSchemaConfiguration> nodes, String rootNodeType, String cyclePeriodRootNodeType,
                                     StandardSchedulePeriodSchemaConfiguration period, int cyclePeriodCount, boolean nonAggregating, String parentDomain,
                                     boolean freezed) {
        super(name, alias, description, nodes, rootNodeType, freezed);

        Assert.isTrue(nodes.size() <= Constants.MAX_SPACE_NODE_SCHEMA_COUNT);
        Assert.notNull(period);
        Assert.isTrue(!nonAggregating || cyclePeriodCount == 1);

        checkPrimaryFields(nodes);

        if (cyclePeriodRootNodeType != null) {
            NodeSchemaConfiguration rootSchema = findNode(cyclePeriodRootNodeType);
            Assert.notNull(rootSchema);
        }

        int totalLocationIndexCount = 0;
        for (NodeSchemaConfiguration node : nodes) {
            for (FieldSchemaConfiguration field : node.getFields()) {
                if (field instanceof IndexedLocationFieldSchemaConfiguration)
                    totalLocationIndexCount++;
            }
        }

        this.period = period;
        this.cyclePeriodCount = cyclePeriodCount;
        this.nonAggregating = nonAggregating;
        this.cyclePeriodRootNodeType = cyclePeriodRootNodeType;
        this.parentDomain = parentDomain;
        this.totalLocationIndexCount = totalLocationIndexCount;
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

    public String getCyclePeriodRootNodeType() {
        return cyclePeriodRootNodeType;
    }

    public int getTotalLocationIndexCount() {
        return totalLocationIndexCount;
    }

    @Override
    public ISpaceSchema createSchema(IDatabaseContext context, int version) {
        Assert.supports(false);
        return null;
    }

    @Override
    public <T extends SchemaConfiguration> T combine(T schema) {
        PeriodSchemaConfiguration periodSchema = (PeriodSchemaConfiguration) schema;
        Set<PeriodNodeSchemaConfiguration> nodes = new HashSet<PeriodNodeSchemaConfiguration>();
        Map<String, PeriodNodeSchemaConfiguration> nodesMap = new HashMap<String, PeriodNodeSchemaConfiguration>((Map) this.nodesMap);
        for (NodeSchemaConfiguration node : periodSchema.getNodes())
            nodes.add(combine((PeriodNodeSchemaConfiguration) node, nodesMap));
        nodes.addAll(nodesMap.values());

        return (T) new PeriodSchemaConfiguration(combine(getName(), schema.getName()), combine(getAlias(), schema.getAlias()),
                combine(getDescription(), schema.getDescription()), nodes, combine(getRootNodeType(),
                periodSchema.getRootNodeType()), combine(getCyclePeriodRootNodeType(),
                periodSchema.getCyclePeriodRootNodeType()), combine(period, periodSchema.getPeriod()),
                combine(cyclePeriodCount, periodSchema.getCyclePeriodCount()),
                combine(nonAggregating, periodSchema.isNonAggregating()).booleanValue(),
                combine(parentDomain, periodSchema.getParentDomain()));
    }

    @Override
    public void addNode(NodeSchemaConfiguration node) {
        Assert.isTrue(node instanceof PeriodNodeSchemaConfiguration);
        super.addNode(node);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof PeriodSchemaConfiguration))
            return false;

        PeriodSchemaConfiguration configuration = (PeriodSchemaConfiguration) o;
        return super.equals(configuration) && period.equals(configuration.period) &&
                cyclePeriodCount == configuration.cyclePeriodCount &&
                nonAggregating == configuration.nonAggregating &&
                Objects.equals(parentDomain, configuration.parentDomain) &&
                Objects.equals(cyclePeriodRootNodeType, configuration.cyclePeriodRootNodeType);
    }

    @Override
    public boolean equalsStructured(SpaceSchemaConfiguration newSchema) {
        if (!(newSchema instanceof PeriodSchemaConfiguration))
            return false;

        PeriodSchemaConfiguration configuration = (PeriodSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration) && period.equals(configuration.period) &&
                cyclePeriodCount == configuration.cyclePeriodCount &&
                nonAggregating == configuration.nonAggregating &&
                Objects.equals(parentDomain, configuration.parentDomain) &&
                Objects.equals(cyclePeriodRootNodeType, configuration.cyclePeriodRootNodeType);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(period, cyclePeriodCount, nonAggregating, parentDomain,
                cyclePeriodRootNodeType);
    }

    private void checkPrimaryFields(Set<? extends PeriodNodeSchemaConfiguration> nodes) {
        for (PeriodNodeSchemaConfiguration node : nodes) {
            for (FieldSchemaConfiguration field : node.getFields())
                Assert.isTrue(!field.isPrimary() || field instanceof IndexedLocationFieldSchemaConfiguration);
        }
    }
}
