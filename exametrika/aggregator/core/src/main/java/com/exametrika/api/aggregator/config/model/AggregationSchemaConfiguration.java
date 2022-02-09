/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.AggregationComponentTypeSchemaConfiguration.Kind;
import com.exametrika.api.aggregator.nodes.ISecondaryEntryPointNode.CombineType;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.InvalidArgumentException;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.aggregator.common.values.AggregationSchema;
import com.exametrika.spi.aggregator.common.values.IAggregationSchema;
import com.exametrika.spi.exadb.core.config.schema.SchemaConfiguration;


/**
 * The {@link AggregationSchemaConfiguration} represents a configuration of aggregation schema.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class AggregationSchemaConfiguration extends SchemaConfiguration {
    public static final String SCHEMA = "com.exametrika.aggregation-1.0";
    private static final IMessages messages = Messages.get(IMessages.class);
    private final List<PeriodTypeSchemaConfiguration> periodTypes;
    private final Map<String, PeriodTypeSchemaConfiguration> periodTypesMap;
    private final CombineType combineType;
    private final int version;

    public AggregationSchemaConfiguration(List<PeriodTypeSchemaConfiguration> periodTypes, CombineType combineType, int version) {
        super("AggregationSchema", "AggregationSchema", null);

        Assert.notNull(periodTypes);
        Assert.isTrue(periodTypes.size() >= 1);
        Assert.isTrue(!periodTypes.get(0).isNonAggregating() || periodTypes.size() >= 2);
        Assert.notNull(combineType);

        Map<String, PeriodTypeSchemaConfiguration> periodTypesMap = new HashMap<String, PeriodTypeSchemaConfiguration>();
        for (PeriodTypeSchemaConfiguration periodType : periodTypes)
            Assert.isNull(periodTypesMap.put(periodType.getName(), periodType));

        boolean first = true;
        PeriodTypeSchemaConfiguration prevPeriodType = null;
        for (PeriodTypeSchemaConfiguration periodType : periodTypes) {
            if (first) {
                first = false;
                if (periodType.isNonAggregating())
                    continue;
            } else
                Assert.isTrue(!periodType.isNonAggregating());

            if (prevPeriodType != null) {
                boolean valid = periodType.getPeriod().getKind() == prevPeriodType.getPeriod().getKind();
                long amount = periodType.getPeriod().getAbsoluteAmount();
                long prevAmount = prevPeriodType.getPeriod().getAbsoluteAmount();
                if (valid && (amount / prevAmount < 2 || (amount % prevAmount != 0)))
                    valid = false;

                if (!valid)
                    throw new InvalidArgumentException(messages.periodNotValid(periodType.toString()));

                checkCompatibility(prevPeriodType, periodType);
            }

            checkCompatibility(periodType);

            prevPeriodType = periodType;
        }

        this.periodTypes = Immutables.wrap(periodTypes);
        this.periodTypesMap = periodTypesMap;
        this.combineType = combineType;
        this.version = version;
    }

    public List<PeriodTypeSchemaConfiguration> getPeriodTypes() {
        return periodTypes;
    }

    public PeriodTypeSchemaConfiguration findPeriodType(String name) {
        Assert.notNull(name);

        return periodTypesMap.get(name);
    }

    public CombineType getCombineType() {
        return combineType;
    }

    public int getVersion() {
        return version;
    }

    public IAggregationSchema createSchema() {
        boolean firstNonAggregating = periodTypes.get(0).isNonAggregating();
        Map<String, AggregationComponentTypeSchemaConfiguration> componentTypes = new LinkedHashMap<String, AggregationComponentTypeSchemaConfiguration>();
        for (int i = 0; i < periodTypes.size(); i++) {
            PeriodTypeSchemaConfiguration periodType = periodTypes.get(i);
            for (AggregationComponentTypeSchemaConfiguration componentType : periodType.getComponentTypes()) {
                if ((i == 1 && firstNonAggregating) || !componentTypes.containsKey(componentType.getName()))
                    componentTypes.put(componentType.getName(), componentType);
            }
        }

        Set<ComponentValueSchemaConfiguration> list = new LinkedHashSet<ComponentValueSchemaConfiguration>();
        for (AggregationComponentTypeSchemaConfiguration componentType : componentTypes.values())
            list.add(componentType.getMetrics());

        return new AggregationSchema(list, version);
    }

    @Override
    public <T extends SchemaConfiguration> T combine(T schema) {
        AggregationSchemaConfiguration aggregationSchema = (AggregationSchemaConfiguration) schema;
        Assert.isTrue(this.periodTypes.size() == aggregationSchema.getPeriodTypes().size());
        List<PeriodTypeSchemaConfiguration> periodTypes = new ArrayList<PeriodTypeSchemaConfiguration>();
        for (int i = 0; i < this.periodTypes.size(); i++)
            periodTypes.add(this.periodTypes.get(i).combine(aggregationSchema.getPeriodTypes().get(i)));

        return (T) new AggregationSchemaConfiguration(periodTypes, combine(combineType, aggregationSchema.combineType),
                combine(version, aggregationSchema.version));
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof AggregationSchemaConfiguration))
            return false;

        AggregationSchemaConfiguration configuration = (AggregationSchemaConfiguration) o;
        return super.equals(configuration) && periodTypes.equals(configuration.periodTypes) &&
                combineType == configuration.combineType && version == configuration.version;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(periodTypes, combineType, version);
    }

    private void checkCompatibility(PeriodTypeSchemaConfiguration periodType) {
        for (AggregationComponentTypeSchemaConfiguration componentType : periodType.getComponentTypes()) {
            if (componentType instanceof StackSchemaConfiguration) {
                StackSchemaConfiguration stackComponentType = (StackSchemaConfiguration) componentType;
                if (stackComponentType.getStackNameComponentType() != null) {
                    NameSchemaConfiguration stackNameComponentType = (NameSchemaConfiguration) periodType.findComponentType(
                            stackComponentType.getStackNameComponentType());
                    Assert.notNull(stackNameComponentType);
                    Assert.isTrue(stackNameComponentType.getKind() == Kind.STACK_NAME);
                    Assert.isTrue(stackComponentType.getMetrics().isCompatible(stackNameComponentType.getMetrics()),
                            "Stack name type ''{0}'' is not compatible with ''{1}''.", stackNameComponentType.getName(),
                            stackComponentType.getName());
                }
            }

            if (componentType instanceof PrimaryEntryPointSchemaConfiguration) {
                PrimaryEntryPointSchemaConfiguration entryComponentType = (PrimaryEntryPointSchemaConfiguration) componentType;
                if (entryComponentType.getTransactionFailureDependenciesComponentType() != null) {
                    StackLogSchemaConfiguration failureDependeciesComponentType = (StackLogSchemaConfiguration) periodType.findComponentType(
                            entryComponentType.getTransactionFailureDependenciesComponentType());
                    Assert.notNull(failureDependeciesComponentType);
                }
            }

            if (componentType instanceof StackLogSchemaConfiguration) {
                if (componentType instanceof StackErrorLogSchemaConfiguration) {
                    StackErrorLogSchemaConfiguration errorLogComponentType = (StackErrorLogSchemaConfiguration) componentType;

                    if (errorLogComponentType.getErrorComponentType() != null) {
                        NameSchemaConfiguration errorComponentType = (NameSchemaConfiguration) periodType.findComponentType(
                                errorLogComponentType.getErrorComponentType());
                        Assert.notNull(errorComponentType);
                    }

                    if (errorLogComponentType.getTransactionFailureComponentType() != null) {
                        NameSchemaConfiguration transactionFailureComponentType = (NameSchemaConfiguration) periodType.findComponentType(
                                errorLogComponentType.getTransactionFailureComponentType());
                        Assert.notNull(transactionFailureComponentType);
                    }
                }
            }
        }
    }

    private void checkCompatibility(PeriodTypeSchemaConfiguration periodType1, PeriodTypeSchemaConfiguration periodType2) {
        for (AggregationComponentTypeSchemaConfiguration componentType1 : periodType1.getComponentTypes()) {
            AggregationComponentTypeSchemaConfiguration componentType2 = periodType2.findComponentType(componentType1.getName());
            if (componentType2 == null)
                continue;

            Assert.isTrue(componentType1.getMetrics().isCompatible(componentType2.getMetrics()));
        }
    }

    private interface IMessages {
        @DefaultMessage("Period ''{0}'' is not valid. Next period duration must be integral multiple of previous period duration.")
        ILocalizedMessage periodNotValid(String period);
    }
}
