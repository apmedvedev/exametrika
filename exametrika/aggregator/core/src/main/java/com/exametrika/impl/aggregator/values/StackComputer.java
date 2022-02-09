/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.values;

import java.util.List;
import java.util.Map;

import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.api.aggregator.common.values.IStackValue;
import com.exametrika.api.aggregator.config.model.StackRepresentationSchemaConfiguration;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.aggregator.IComputeContext;
import com.exametrika.spi.aggregator.IFieldComputer;
import com.exametrika.spi.aggregator.IMetricComputer;
import com.exametrika.spi.aggregator.common.values.IFieldValueBuilder;
import com.exametrika.spi.aggregator.config.model.FieldRepresentationSchemaConfiguration;


/**
 * The {@link StackComputer} is a computer of fields in stack metric type.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class StackComputer implements IMetricComputer {
    private final StackRepresentationSchemaConfiguration configuration;
    private final List<IFieldComputer> inherentFieldComputers;
    private final List<IFieldComputer> totalFieldComputers;

    public StackComputer(StackRepresentationSchemaConfiguration configuration, List<IFieldComputer> inherentFieldComputers,
                         List<IFieldComputer> totalFieldComputers) {
        Assert.notNull(configuration);
        Assert.notNull(inherentFieldComputers);
        Assert.notNull(totalFieldComputers);

        this.configuration = configuration;
        this.inherentFieldComputers = inherentFieldComputers;
        this.totalFieldComputers = totalFieldComputers;
    }

    @Override
    public Object compute(IComponentValue componentValue, IMetricValue value, IComputeContext c) {
        IStackValue metricValue = (IStackValue) value;
        if (metricValue == null)
            return null;

        JsonObjectBuilder builder = new JsonObjectBuilder();

        ComputeContext context = (ComputeContext) c;
        context.setInherent(true);
        builder.put("inherent", compute(componentValue, metricValue, metricValue.getInherentFields(), inherentFieldComputers, context));

        context.setInherent(false);
        context.setTotal(true);
        builder.put("total", compute(componentValue, metricValue, metricValue.getTotalFields(), totalFieldComputers, context));

        context.setTotal(false);

        return builder.toJson();
    }

    @Override
    public void computeSecondary(IComponentValue componentValue, IMetricValue value, IComputeContext c) {
        if (value == null)
            return;

        IStackValue metricValue = (IStackValue) value;

        ComputeContext context = (ComputeContext) c;
        context.setInherent(true);
        computeSecondary(componentValue, metricValue, metricValue.getInherentFields(), inherentFieldComputers, context);

        context.setInherent(false);
        context.setTotal(true);
        computeSecondary(componentValue, metricValue, metricValue.getTotalFields(), totalFieldComputers, context);

        context.setTotal(false);
    }

    private JsonObject compute(IComponentValue componentValue, IMetricValue metricValue, List<? extends IFieldValue> fields, List<IFieldComputer> fieldComputers,
                               IComputeContext context) {
        JsonObjectBuilder builder = new JsonObjectBuilder();

        int k = 0;
        for (int i = 0; i < fieldComputers.size(); i++) {
            IFieldComputer fieldComputer = fieldComputers.get(i);
            if (fieldComputer == null)
                continue;

            FieldRepresentationSchemaConfiguration fieldConfiguration = configuration.getFields().get(i);

            IFieldValue childValue = null;
            if (fieldConfiguration.isValueSupported()) {
                childValue = fields.get(k);
                k++;
            }

            Object result = fieldComputer.compute(componentValue, metricValue, childValue, context);
            if (result == null)
                continue;

            if (result instanceof JsonObject) {
                for (Map.Entry<String, Object> entry : (JsonObject) result)
                    builder.put(fieldConfiguration.getName() + "." + entry.getKey(), entry.getValue());
            } else
                builder.put(fieldConfiguration.getName(), result);
        }

        return builder.toJson();
    }

    private void computeSecondary(IComponentValue componentValue, IMetricValue metricValue, List<? extends IFieldValue> fields, List<IFieldComputer> fieldComputers,
                                  IComputeContext context) {
        int k = 0;
        for (int i = 0; i < fieldComputers.size(); i++) {
            IFieldComputer fieldComputer = fieldComputers.get(i);
            if (fieldComputer == null)
                continue;

            FieldRepresentationSchemaConfiguration fieldConfiguration = configuration.getFields().get(i);

            IFieldValueBuilder childValue = null;
            if (fieldConfiguration.isValueSupported()) {
                childValue = (IFieldValueBuilder) fields.get(k);
                k++;
            }

            if (fieldConfiguration.isSecondaryComputationSupported())
                fieldComputer.computeSecondary(componentValue, metricValue, childValue, context);
        }
    }
}
