/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.values;

import java.util.List;
import java.util.Map;

import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.api.aggregator.common.values.INameValue;
import com.exametrika.api.aggregator.config.model.NameRepresentationSchemaConfiguration;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.aggregator.IComputeContext;
import com.exametrika.spi.aggregator.IFieldComputer;
import com.exametrika.spi.aggregator.IMetricComputer;
import com.exametrika.spi.aggregator.common.values.IFieldValueBuilder;
import com.exametrika.spi.aggregator.config.model.FieldRepresentationSchemaConfiguration;


/**
 * The {@link NameComputer} is an computer of fields in field metric type.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class NameComputer implements IMetricComputer {
    private final NameRepresentationSchemaConfiguration configuration;
    private final List<IFieldComputer> fieldComputers;

    public NameComputer(NameRepresentationSchemaConfiguration configuration, List<IFieldComputer> fieldComputers) {
        Assert.notNull(configuration);
        Assert.notNull(fieldComputers);

        this.configuration = configuration;
        this.fieldComputers = fieldComputers;
    }

    @Override
    public Object compute(IComponentValue componentValue, IMetricValue value, IComputeContext context) {
        INameValue metricValue = (INameValue) value;

        if (metricValue != null)
            return compute(componentValue, metricValue, metricValue.getFields(), context);
        else
            return null;
    }

    @Override
    public void computeSecondary(IComponentValue componentValue, IMetricValue value, IComputeContext context) {
        INameValue metricValue = (INameValue) value;

        if (metricValue != null)
            computeSecondary(componentValue, metricValue, metricValue.getFields(), context);
    }

    private JsonObject compute(IComponentValue componentValue, IMetricValue metricValue, List<? extends IFieldValue> fields, IComputeContext context) {
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

    private void computeSecondary(IComponentValue componentValue, IMetricValue metricValue, List<? extends IFieldValue> fields, IComputeContext context) {
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
