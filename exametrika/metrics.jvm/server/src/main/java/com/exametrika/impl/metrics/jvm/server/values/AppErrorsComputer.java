/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.jvm.server.values;

import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.api.aggregator.common.values.IObjectValue;
import com.exametrika.api.aggregator.nodes.IAggregationNode;
import com.exametrika.api.aggregator.nodes.IEntryPointNode;
import com.exametrika.api.aggregator.nodes.IStackNode;
import com.exametrika.api.metrics.jvm.server.config.model.AppErrorsRepresentationSchemaConfiguration;
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
 * The {@link AppErrorsComputer} is an application errors computer.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class AppErrorsComputer extends ObjectComputer {
    private final AppErrorsRepresentationSchemaConfiguration configuration;
    private IComponentAccessor firstAccessor;
    private final int metricIndex;

    public AppErrorsComputer(AppErrorsRepresentationSchemaConfiguration configuration, IComponentAccessorFactory componentAccessorFactory,
                             String metricName, int metricIndex) {
        this.configuration = configuration;
        this.metricIndex = metricIndex;
        switch (configuration.getType()) {
            case APP_REQUEST_ERRORS:
                firstAccessor = componentAccessorFactory.createAccessor(null, null, "app.entryPoint.errors.count.rate");
                break;
            case APP_STALLS_ERRORS:
                firstAccessor = componentAccessorFactory.createAccessor(null, null, "app.entryPoint.stalls.count.rate");
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

        IAggregationNode node = (IAggregationNode) ((IField) context.getObject()).getNode().getObject();
        double errors = 0;
        if (!node.isDerived() || !(node instanceof IEntryPointNode)) {
            switch (configuration.getType()) {
                case APP_REQUEST_ERRORS: {
                    Number entryErrors = (Number) firstAccessor.get(componentValue, context);
                    if (entryErrors != null)
                        errors = entryErrors.doubleValue();
                    break;
                }
                case APP_STALLS_ERRORS: {
                    Number stalls = (Number) firstAccessor.get(componentValue, context);
                    if (stalls != null)
                        errors = stalls.doubleValue();
                    break;
                }
                default:
                    Assert.error();
            }
        } else {
            double sum = 0;
            int count = 0;
            for (IStackNode child : ((IEntryPointNode) node).getScopeChildren()) {
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
