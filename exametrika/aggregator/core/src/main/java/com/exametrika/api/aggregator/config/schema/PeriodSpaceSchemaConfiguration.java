/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.schema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.exametrika.api.aggregator.nodes.ISecondaryEntryPointNode.CombineType;
import com.exametrika.api.aggregator.schema.ICycleSchema;
import com.exametrika.api.exadb.core.schema.ISpaceSchema;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.InvalidArgumentException;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.aggregator.schema.CycleSchema;
import com.exametrika.impl.aggregator.schema.PeriodSpaceSchema;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.spi.exadb.core.config.schema.SchemaConfiguration;
import com.exametrika.spi.exadb.core.config.schema.SpaceSchemaConfiguration;


/**
 * The {@link PeriodSpaceSchemaConfiguration} represents a configuration of schema of periodic node space.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class PeriodSpaceSchemaConfiguration extends SpaceSchemaConfiguration {
    private static final IMessages messages = Messages.get(IMessages.class);
    private final List<PeriodSchemaConfiguration> periods;
    private final Map<String, PeriodSchemaConfiguration> periodsMap;
    private final Map<String, PeriodSchemaConfiguration> periodsByAliasMap;
    private final int pathIndex;
    private final int fullTextPathIndex;
    private final boolean useBatching;
    private final CombineType combineType;
    protected boolean freezed;

    public PeriodSpaceSchemaConfiguration(String name, List<PeriodSchemaConfiguration> periods) {
        this(name, name, null, periods, 0, 0);
    }

    public PeriodSpaceSchemaConfiguration(String name, String alias, String description, List<PeriodSchemaConfiguration> periods, int pathIndex,
                                          int fullTextPathIndex) {
        this(name, alias, description, periods, pathIndex, fullTextPathIndex, false, CombineType.STACK, true);
    }

    public PeriodSpaceSchemaConfiguration(String name, String alias, String description, List<PeriodSchemaConfiguration> periods,
                                          int pathIndex, int fullTextPathIndex, boolean useBatching, CombineType combineType, boolean freezed) {
        super(name, alias, description);

        Assert.notNull(periods);
        Assert.notNull(combineType);

        Map<String, PeriodSchemaConfiguration> periodsMap = new HashMap<String, PeriodSchemaConfiguration>();
        Map<String, PeriodSchemaConfiguration> periodsByAliasMap = new HashMap<String, PeriodSchemaConfiguration>();
        for (PeriodSchemaConfiguration period : periods) {
            Assert.isNull(periodsMap.put(period.getName(), period));
            Assert.isNull(periodsByAliasMap.put(period.getAlias(), period));
        }

        boolean first = true;
        PeriodSchemaConfiguration prevPeriod = null;
        for (PeriodSchemaConfiguration period : periods) {
            if (first) {
                first = false;
                if (period.isNonAggregating())
                    continue;
            } else
                Assert.isTrue(!period.isNonAggregating());

            if (prevPeriod != null) {
                boolean valid = period.getPeriod().getKind() == prevPeriod.getPeriod().getKind();
                long amount = period.getPeriod().getAbsoluteAmount();
                long prevAmount = prevPeriod.getPeriod().getAbsoluteAmount();
                if (valid && (amount / prevAmount < 2 || (amount % prevAmount != 0)))
                    valid = false;

                if (!valid)
                    throw new InvalidArgumentException(messages.periodNotValid(name, period.toString()));
            }

            prevPeriod = period;
        }

        this.freezed = freezed;
        this.periods = Immutables.wrap(periods);
        this.periodsMap = periodsMap;
        this.periodsByAliasMap = periodsByAliasMap;
        this.pathIndex = pathIndex;
        this.fullTextPathIndex = fullTextPathIndex;
        this.useBatching = useBatching;
        this.combineType = combineType;
    }

    @Override
    public void orderNodes(SpaceSchemaConfiguration oldSchema) {
        PeriodSpaceSchemaConfiguration periodSpaceSchema = (PeriodSpaceSchemaConfiguration) oldSchema;
        for (int i = 0; i < periods.size(); i++)
            periods.get(i).orderNodes(periodSpaceSchema.getPeriods().get(i));
    }

    @Override
    public void freeze() {
        if (freezed)
            return;

        freezed = true;
        for (PeriodSchemaConfiguration period : periods)
            period.freeze();
    }

    public PeriodSchemaConfiguration getCurrentPeriod() {
        return periods.get(0);
    }

    public List<PeriodSchemaConfiguration> getPeriods() {
        return periods;
    }

    public PeriodSchemaConfiguration findPeriod(String name) {
        Assert.notNull(name);

        return periodsMap.get(name);
    }

    public PeriodSchemaConfiguration findPeriodByAlias(String alias) {
        Assert.notNull(alias);

        return periodsByAliasMap.get(alias);
    }

    public int getPathIndex() {
        return pathIndex;
    }

    public int getFullTextPathIndex() {
        return fullTextPathIndex;
    }

    public boolean isUseBatching() {
        return useBatching;
    }

    public CombineType getCombineType() {
        return combineType;
    }

    @Override
    public ISpaceSchema createSchema(IDatabaseContext context, int version) {
        List<ICycleSchema> cycles = new ArrayList<ICycleSchema>();
        for (PeriodSchemaConfiguration period : periods) {
            ICycleSchema cycle = new CycleSchema(period, cycles.size(), context, version);
            cycles.add(cycle);
        }

        return new PeriodSpaceSchema(context, this, version, cycles);
    }

    @Override
    public <T extends SchemaConfiguration> T combine(T schema) {
        PeriodSpaceSchemaConfiguration periodSpaceSchema = (PeriodSpaceSchemaConfiguration) schema;

        Assert.isTrue(this.periods.size() == periodSpaceSchema.getPeriods().size());
        List<PeriodSchemaConfiguration> periods = new ArrayList<PeriodSchemaConfiguration>();
        for (int i = 0; i < this.periods.size(); i++)
            periods.add(this.periods.get(i).combine(periodSpaceSchema.getPeriods().get(i)));

        return (T) new PeriodSpaceSchemaConfiguration(combine(getName(), schema.getName()), combine(getAlias(), schema.getAlias()),
                combine(getDescription(), schema.getDescription()), periods, combine(pathIndex, periodSpaceSchema.getPathIndex()),
                combine(fullTextPathIndex, periodSpaceSchema.getFullTextPathIndex()),
                combine(useBatching, periodSpaceSchema.isUseBatching()), combine(combineType, periodSpaceSchema.getCombineType()), true);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof PeriodSpaceSchemaConfiguration))
            return false;

        PeriodSpaceSchemaConfiguration configuration = (PeriodSpaceSchemaConfiguration) o;
        return super.equals(o) && periods.equals(configuration.periods) &&
                pathIndex == configuration.pathIndex && fullTextPathIndex == configuration.fullTextPathIndex &&
                useBatching == configuration.useBatching && combineType == configuration.combineType;
    }

    @Override
    public boolean equalsStructured(SpaceSchemaConfiguration newSchema) {
        if (!(newSchema instanceof PeriodSpaceSchemaConfiguration))
            return false;

        PeriodSpaceSchemaConfiguration configuration = (PeriodSpaceSchemaConfiguration) newSchema;
        if (periods.size() != configuration.periods.size())
            return false;
        for (int i = 0; i < periods.size(); i++) {
            if (!periods.get(i).equalsStructured(configuration.periods.get(i)))
                return false;
        }
        return super.equalsStructured(configuration) && pathIndex == configuration.pathIndex &&
                fullTextPathIndex == configuration.fullTextPathIndex && combineType == configuration.combineType;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(periods, pathIndex, fullTextPathIndex, useBatching, combineType);
    }

    private interface IMessages {
        @DefaultMessage("Period ''{1}'' is not valid in space ''{0}''. Next period duration must be integral multiple of previous period duration.")
        ILocalizedMessage periodNotValid(String name, String period);
    }
}
