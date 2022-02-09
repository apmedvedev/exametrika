/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.jvm.server.values;

import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.api.aggregator.common.values.IObjectValue;
import com.exametrika.api.aggregator.nodes.INameNode;
import com.exametrika.api.metrics.jvm.server.config.model.JvmErrorsRepresentationSchemaConfiguration;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.aggregator.common.values.ObjectBuilder;
import com.exametrika.impl.aggregator.values.ComputeContext;
import com.exametrika.impl.aggregator.values.ObjectComputer;
import com.exametrika.spi.aggregator.IComponentAccessor;
import com.exametrika.spi.aggregator.IComponentAccessorFactory;
import com.exametrika.spi.aggregator.IComputeContext;
import com.exametrika.spi.exadb.objectdb.fields.IField;


/**
 * The {@link JvmErrorsComputer} is an jvm errors computer.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class JvmErrorsComputer extends ObjectComputer {
    private final JvmErrorsRepresentationSchemaConfiguration configuration;
    private IComponentAccessor firstAccessor;
    private final int metricIndex;

    public JvmErrorsComputer(JvmErrorsRepresentationSchemaConfiguration configuration, IComponentAccessorFactory componentAccessorFactory,
                             String metricName, int metricIndex) {
        this.configuration = configuration;
        this.metricIndex = metricIndex;
        switch (configuration.getType()) {
            case JVM_GC_ERRORS:
                firstAccessor = componentAccessorFactory.createAccessor(null, null, "jvm.gc.stops.rate");
                break;
            case JVM_SWAP_ERRORS:
                firstAccessor = componentAccessorFactory.createAccessor(null, null, "host.process.memory.majorFaults.rate");
                break;
            default:
                Assert.error();
        }
    }

    public Object getValue(IMetricValue value, boolean thresholds) {
        if (!thresholds) {
            IObjectValue metricValue = (IObjectValue) value;
            if (metricValue != null)
                return metricValue.getObject();
            else
                return null;
        } else
            return Json.array().add(configuration.getWarningThreshold()).add(configuration.getErrorThreshold()).toArray();
    }

    @Override
    public void computeSecondary(IComponentValue componentValue, IMetricValue value, IComputeContext context) {
        if (value == null)
            return;
        if (((IObjectValue) value).getObject() instanceof JsonObject)
            return;

        INameNode node = (INameNode) ((IField) context.getObject()).getNode().getObject();
        double errors = 0;
        if (!node.isDerived()) {
            switch (configuration.getType()) {
                case JVM_GC_ERRORS: {
                    Number majorCollections = (Number) firstAccessor.get(componentValue, context);
                    if (majorCollections != null)
                        errors = majorCollections.doubleValue();
                    break;
                }
                case JVM_SWAP_ERRORS: {
                    Number majorFaults = (Number) firstAccessor.get(componentValue, context);
                    if (majorFaults != null)
                        errors = majorFaults.doubleValue();
                    break;
                }
                default:
                    Assert.error();
            }
        } else {
            double sum = 0;
            int count = 0;
            for (INameNode child : node.getScopeChildren()) {
                IComponentValue componentChildValue = child.getAggregationField().getValue(false);
                IObjectValue childValue = (IObjectValue) componentChildValue.getMetrics().get(metricIndex);
                Object oldObject = context.getObject();
                ((ComputeContext) context).setObject(child.getAggregationField());
                computeSecondary(componentChildValue, childValue, context);
                ((ComputeContext) context).setObject(oldObject);

                sum += (Double) ((JsonObject) childValue.getObject()).get("value");
                count++;
            }

            if (count > 0)
                errors = sum / count;
        }

        String state;
        if (errors < configuration.getWarningThreshold())
            state = "normal";
        else if (errors < configuration.getErrorThreshold())
            state = "warning";
        else
            state = "error";

        JsonObjectBuilder fields = new JsonObjectBuilder();
        fields.put("value", errors);
        fields.put("state", state);

        ObjectBuilder builder = (ObjectBuilder) value;
        builder.setObject(fields.toJson());
    }
}
