/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.config.model;

import java.util.Collections;
import java.util.List;

import com.exametrika.common.json.JsonArray;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.aggregator.values.HistogramAccessor;
import com.exametrika.impl.aggregator.values.HistogramAccessor.Type;
import com.exametrika.impl.aggregator.values.HistogramComputer;
import com.exametrika.spi.aggregator.IFieldAccessor;
import com.exametrika.spi.aggregator.IFieldComputer;
import com.exametrika.spi.aggregator.IMetricAccessorFactory;
import com.exametrika.spi.aggregator.common.values.config.FieldValueSchemaConfiguration;
import com.exametrika.spi.aggregator.config.model.FieldRepresentationSchemaConfiguration;


/**
 * The {@link HistogramRepresentationSchemaConfiguration} is a histogram aggregation field schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class HistogramRepresentationSchemaConfiguration extends FieldRepresentationSchemaConfiguration {
    private final int binCount;
    private final boolean computeValues;
    private final boolean computePercentages;
    private final boolean computeCumulativePercentages;
    private final boolean computeScale;
    private final List<Integer> percentiles;

    public HistogramRepresentationSchemaConfiguration(int binCount, boolean computeValues, boolean computePercentages,
                                                      boolean computeCumulativePercentages, boolean computeScale, List<Integer> percentiles, boolean enabled) {
        super("histo", enabled);

        Assert.notNull(percentiles);
        Assert.isTrue(computeValues || computePercentages || computeCumulativePercentages || !percentiles.isEmpty());
        Assert.isTrue(binCount > 0);

        for (int i : percentiles)
            Assert.isTrue(i >= 0 && i <= 100);

        this.binCount = binCount;
        this.computeValues = computeValues;
        this.computePercentages = computePercentages;
        this.computeCumulativePercentages = computeCumulativePercentages;
        this.computeScale = computeScale;

        Collections.sort(percentiles);
        this.percentiles = Immutables.wrap(percentiles);
    }

    public int getBinCount() {
        return binCount;
    }

    public boolean isComputeValues() {
        return computeValues;
    }

    public boolean isComputePercentages() {
        return computePercentages;
    }

    public boolean isComputeCumulativePercentages() {
        return computeCumulativePercentages;
    }

    public boolean isComputeScale() {
        return computeScale;
    }

    public List<Integer> getPercentiles() {
        return percentiles;
    }

    public abstract JsonArray getScale();

    @Override
    public boolean isValueSupported() {
        return true;
    }

    @Override
    public boolean isSecondaryComputationSupported() {
        return false;
    }

    @Override
    public IFieldAccessor createAccessor(String fieldName, FieldValueSchemaConfiguration schema, IMetricAccessorFactory accessorFactory) {
        String percentileFieldHeader = "percentile(";
        if (fieldName.startsWith(percentileFieldHeader)) {
            int pos = fieldName.indexOf(')');
            Assert.isTrue(pos != -1);
            int percentilePercentage = Integer.parseInt(fieldName.substring(percentileFieldHeader.length(), pos));
            boolean value = fieldName.substring(pos + 1).equals(".value");
            return new HistogramAccessor(percentilePercentage, value, (HistogramComputer) createComputer(schema, accessorFactory));
        }
        return new HistogramAccessor(getType(fieldName), (HistogramComputer) createComputer(schema, accessorFactory));
    }

    @Override
    public IFieldComputer createComputer(FieldValueSchemaConfiguration schema, IMetricAccessorFactory accessorFactory) {
        return new HistogramComputer(this, accessorFactory.createAccessor(null, null, "std.count"),
                accessorFactory.createAccessor(null, null, "std.min"), accessorFactory.createAccessor(null, null, "std.max"));
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof HistogramRepresentationSchemaConfiguration))
            return false;

        HistogramRepresentationSchemaConfiguration configuration = (HistogramRepresentationSchemaConfiguration) o;
        return super.equals(o) && binCount == configuration.binCount &&
                computeValues == configuration.computeValues &&
                computePercentages == configuration.computePercentages &&
                computeCumulativePercentages == configuration.computeCumulativePercentages &&
                computeScale == configuration.computeScale && percentiles.equals(configuration.percentiles);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(binCount, computeValues, computePercentages, computeCumulativePercentages,
                computeScale, percentiles);
    }

    private Type getType(String fieldName) {
        if (fieldName.equals("bins"))
            return Type.BINS;
        else if (fieldName.equals("min-oob"))
            return Type.MIN_OOB;
        else if (fieldName.equals("max-oob"))
            return Type.MAX_OOB;
        else if (fieldName.equals("percentiles"))
            return Type.PERCENTILES;
        else if (fieldName.equals("bins%"))
            return Type.BINS_PERCENTAGES;
        else if (fieldName.equals("min-oob%"))
            return Type.MIN_OOB_PERCENTAGES;
        else if (fieldName.equals("max-oob%"))
            return Type.MAX_OOB_PERCENTAGES;
        else if (fieldName.equals("bins+%"))
            return Type.BINS_CUMULATIVE_PERCENTAGES;
        else if (fieldName.equals("min-oob+%"))
            return Type.MIN_OOB_CUMULATIVE_PERCENTAGE;
        else if (fieldName.equals("max-oob+%"))
            return Type.MAX_OOB_CUMULATIVE_PERCENTAGE;
        else if (fieldName.equals("scale"))
            return Type.SCALE;
        else {
            Assert.isTrue(false);
            return null;
        }
    }
}
