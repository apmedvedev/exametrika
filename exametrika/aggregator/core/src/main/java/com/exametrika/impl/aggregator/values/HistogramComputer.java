/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.values;

import java.util.List;

import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.api.aggregator.common.values.IHistogramValue;
import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.api.aggregator.config.model.HistogramRepresentationSchemaConfiguration;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonArrayBuilder;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Numbers;
import com.exametrika.spi.aggregator.IComputeContext;
import com.exametrika.spi.aggregator.IFieldComputer;
import com.exametrika.spi.aggregator.IMetricAccessor;
import com.exametrika.spi.aggregator.common.values.IFieldValueBuilder;


/**
 * The {@link HistogramComputer} is an implementation of {@link IFieldComputer} for histogram fields.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class HistogramComputer implements IFieldComputer {
    private final HistogramRepresentationSchemaConfiguration configuration;
    private final IMetricAccessor countAccessor;
    private final IMetricAccessor minAccessor;
    private final IMetricAccessor maxAccessor;

    public HistogramComputer(HistogramRepresentationSchemaConfiguration configuration, IMetricAccessor countAccessor,
                             IMetricAccessor minAccessor, IMetricAccessor maxAccessor) {
        Assert.notNull(configuration);
        Assert.notNull(countAccessor);
        Assert.notNull(minAccessor);
        Assert.notNull(maxAccessor);

        this.configuration = configuration;
        this.countAccessor = countAccessor;
        this.minAccessor = minAccessor;
        this.maxAccessor = maxAccessor;
    }

    public IMetricAccessor getCountAccessor() {
        return countAccessor;
    }

    public IMetricAccessor getMinAccessor() {
        return minAccessor;
    }

    public IMetricAccessor getMaxAccessor() {
        return maxAccessor;
    }

    public JsonArray getBins(IHistogramValue value) {
        JsonArrayBuilder bins = new JsonArrayBuilder();
        for (int i = 0; i < value.getBinCount(); i++)
            bins.add(value.getBin(i));

        return bins;
    }

    public JsonObject getPercentiles(IHistogramValue value, long count, long min, long max) {
        List<Integer> percentiles = configuration.getPercentiles();
        JsonArray scale = configuration.getScale();
        JsonArrayBuilder builderPercentages = new JsonArrayBuilder();
        JsonArrayBuilder builderValues = new JsonArrayBuilder();
        JsonArrayBuilder builderErrors = new JsonArrayBuilder();
        int k = 0;
        long sum = 0;
        long prevScaleValue = min;
        for (int i = 0; i < value.getBinCount() + 2; i++) {
            long binValue;
            if (i == 0)
                binValue = value.getMinOutOfBounds();
            else if (i < value.getBinCount() + 1)
                binValue = value.getBin(i - 1);
            else
                binValue = value.getMaxOutOfBounds();

            sum += binValue;

            long scaleValue;
            if (i < value.getBinCount() + 1)
                scaleValue = (Long) scale.get(i);
            else
                scaleValue = max;

            if (scaleValue < min)
                scaleValue = min;
            if (scaleValue > max)
                scaleValue = max;

            double percentage = Numbers.percents(sum, count);
            while (percentage >= percentiles.get(k)) {
                builderPercentages.add(percentiles.get(k).toString());
                builderValues.add(scaleValue);
                builderErrors.add(scaleValue - prevScaleValue);

                k++;
                if (k >= percentiles.size())
                    break;
            }

            if (k >= percentiles.size())
                break;

            prevScaleValue = scaleValue;
        }

        JsonObjectBuilder object = new JsonObjectBuilder();
        object.put("%", builderPercentages.toJson());
        object.put("values", builderValues.toJson());
        object.put("errors", builderErrors.toJson());
        return object.toJson();
    }

    public Object getPercentile(IHistogramValue value, long count, long min, long max, int percentilePercentage,
                                boolean percentileValue) {
        JsonArray scale = configuration.getScale();
        long sum = 0;
        for (int i = 0; i < value.getBinCount() + 2; i++) {
            long binValue;
            if (i == 0)
                binValue = value.getMinOutOfBounds();
            else if (i < value.getBinCount() + 1)
                binValue = value.getBin(i - 1);
            else
                binValue = value.getMaxOutOfBounds();

            sum += binValue;

            long scaleValue;
            if (i < value.getBinCount() + 1)
                scaleValue = (Long) scale.get(i);
            else
                scaleValue = max;

            if (scaleValue < min)
                scaleValue = min;
            if (scaleValue > max)
                scaleValue = max;

            double percentage = Numbers.percents(sum, count);
            if (percentage >= percentilePercentage) {
                if (!percentileValue)
                    return Json.object().put("value", scaleValue).put("bin", i).toObject();
                else
                    return scaleValue;
            }
        }

        return null;
    }

    public JsonArray getBinsPercentages(IHistogramValue value, long count) {
        JsonArrayBuilder percentages = new JsonArrayBuilder();
        for (int i = 0; i < value.getBinCount(); i++)
            percentages.add(Numbers.percents(value.getBin(i), count));

        return percentages;
    }

    public double getMinOobPercentage(IHistogramValue value, long count) {
        return Numbers.percents(value.getMinOutOfBounds(), count);
    }

    public double getMaxOobPercentage(IHistogramValue value, long count) {
        return Numbers.percents(value.getMaxOutOfBounds(), count);
    }

    public JsonArray getBinsCumulativePercentages(IHistogramValue value, long count) {
        JsonArrayBuilder builder = new JsonArrayBuilder();
        long sum = value.getMinOutOfBounds();
        for (int i = 0; i < value.getBinCount(); i++) {
            sum += value.getBin(i);
            builder.add(Numbers.percents(sum, count));
        }

        return builder;
    }

    public double getMinOobCumulativePercentage(IHistogramValue value, long count) {
        return Numbers.percents(value.getMinOutOfBounds(), count);
    }

    public double getMaxOobCumulativePercentage(IHistogramValue value, long count) {
        return 100;
    }

    public JsonArray getScale() {
        return configuration.getScale();
    }

    @Override
    public Object compute(IComponentValue componentValue, IMetricValue metricValue, IFieldValue v, IComputeContext context) {
        IHistogramValue value = (IHistogramValue) v;
        Long count = (Long) countAccessor.get(componentValue, metricValue, context);
        if (count == null)
            return null;

        Long min = (Long) minAccessor.get(componentValue, metricValue, context);
        Long max = (Long) maxAccessor.get(componentValue, metricValue, context);
        if (min == null || max == null)
            return null;

        JsonObjectBuilder fields = new JsonObjectBuilder();

        if (configuration.isComputeValues()) {
            fields.put("bins", getBins(value));
            fields.put("min-oob", value.getMinOutOfBounds());
            fields.put("max-oob", value.getMaxOutOfBounds());
        }

        if (configuration.isComputePercentages()) {
            fields.put("bins%", getBinsPercentages(value, count));
            fields.put("min-oob%", getMinOobPercentage(value, count));
            fields.put("max-oob%", getMaxOobPercentage(value, count));
        }

        if (configuration.isComputeCumulativePercentages()) {
            fields.put("bins+%", getBinsCumulativePercentages(value, count));
            fields.put("min-oob+%", getMinOobCumulativePercentage(value, count));
            fields.put("max-oob+%", getMaxOobCumulativePercentage(value, count));
        }

        if (!configuration.getPercentiles().isEmpty())
            fields.put("percentiles", getPercentiles(value, count, min, max));

        if (configuration.isComputeScale())
            fields.put("scale", getScale());

        return fields.toJson();
    }

    @Override
    public void computeSecondary(IComponentValue componentValue, IMetricValue metricValue, IFieldValueBuilder value, IComputeContext context) {
    }
}
