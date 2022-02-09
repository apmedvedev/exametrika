/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.values;

import java.util.List;

import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.aggregator.IComponentComputer;
import com.exametrika.spi.aggregator.IComputeContext;
import com.exametrika.spi.aggregator.IMetricComputer;


/**
 * The {@link ComponentComputer} is an computer of component.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ComponentComputer implements IComponentComputer {
    private final List<IMetricComputer> metricComputers;
    private final List<String> metricTypeNames;

    public ComponentComputer(List<IMetricComputer> metricComputers, List<String> metricTypeNames) {
        Assert.notNull(metricComputers);
        Assert.notNull(metricTypeNames);

        this.metricComputers = metricComputers;
        this.metricTypeNames = metricTypeNames;
    }

    @Override
    public Object compute(IComponentValue value, IComputeContext context, boolean includeTime, boolean includeMetadata) {
        JsonObject res = compute(value, value.getMetrics(), context);

        if (includeTime || includeMetadata) {
            JsonObjectBuilder builder = new JsonObjectBuilder();
            builder.put("metrics", res);

            if (includeTime) {
                builder.put("time", context.getTime());
                builder.put("period", context.getPeriod());
            }

            if (includeMetadata && value.getMetadata() != null)
                builder.put("metadata", value.getMetadata());

            return builder.toJson();
        } else
            return res;
    }

    @Override
    public void computeSecondary(IComponentValue value, IComputeContext context) {
        computeSecondary(value, value.getMetrics(), context);
    }

    private JsonObject compute(IComponentValue value, List<? extends IMetricValue> metrics, IComputeContext context) {
        JsonObjectBuilder builder = new JsonObjectBuilder();

        for (int i = 0; i < metricComputers.size(); i++) {
            IMetricComputer metricComputer = metricComputers.get(i);
            if (metricComputer == null)
                continue;

            Object result = metricComputer.compute(value, metrics.get(i), context);
            if (result == null)
                continue;

            builder.put(metricTypeNames.get(i), result);
        }

        return builder.toJson();
    }

    private void computeSecondary(IComponentValue value, List<? extends IMetricValue> metrics, IComputeContext context) {
        for (int i = 0; i < metricComputers.size(); i++) {
            IMetricComputer metricComputer = metricComputers.get(i);
            if (metricComputer == null)
                continue;

            metricComputer.computeSecondary(value, metrics.get(i), context);
        }
    }
}
