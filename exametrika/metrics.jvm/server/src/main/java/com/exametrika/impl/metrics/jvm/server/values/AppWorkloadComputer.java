/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.jvm.server.values;

import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.api.aggregator.common.values.IObjectValue;
import com.exametrika.api.metrics.jvm.server.config.model.AppWorkloadRepresentationSchemaConfiguration;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.aggregator.common.values.ObjectBuilder;
import com.exametrika.impl.aggregator.values.ObjectComputer;
import com.exametrika.spi.aggregator.IComponentAccessor;
import com.exametrika.spi.aggregator.IComponentAccessorFactory;
import com.exametrika.spi.aggregator.IComputeContext;


/**
 * The {@link AppWorkloadComputer} is an application workload computer.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class AppWorkloadComputer extends ObjectComputer {
    private final AppWorkloadRepresentationSchemaConfiguration configuration;
    private IComponentAccessor firstAccessor;

    public AppWorkloadComputer(AppWorkloadRepresentationSchemaConfiguration configuration, IComponentAccessorFactory componentAccessorFactory,
                               String metricName, int metricIndex) {

        this.configuration = configuration;
        switch (configuration.getType()) {
            case APP_LATENCY_WORKLOAD:
                firstAccessor = componentAccessorFactory.createAccessor(null, null, "app.request.time.histo.percentile(50).value");
                break;
            case APP_THROUGHPUT_WORKLOAD:
                firstAccessor = componentAccessorFactory.createAccessor(null, null, "app.request.time.rate");
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

        String state = "normal";
        double workload = 0;
        switch (configuration.getType()) {
            case APP_LATENCY_WORKLOAD: {
                Number latency = (Number) firstAccessor.get(componentValue, context);
                if (latency != null) {
                    workload = latency.doubleValue();
                    if (workload < configuration.getWarningThreshold())
                        state = "normal";
                    else if (workload < configuration.getErrorThreshold())
                        state = "warning";
                    else
                        state = "error";
                }
                break;
            }
            case APP_THROUGHPUT_WORKLOAD: {
                Number throughput = (Number) firstAccessor.get(componentValue, context);
                if (throughput != null) {
                    workload = throughput.doubleValue();
                    if (workload > configuration.getWarningThreshold())
                        state = "normal";
                    else if (workload > configuration.getErrorThreshold())
                        state = "warning";
                    else
                        state = "error";
                }
                break;
            }
            default:
                Assert.error();
        }

        JsonObjectBuilder fields = new JsonObjectBuilder();
        fields.put("value", workload);
        fields.put("state", state);

        ObjectBuilder builder = (ObjectBuilder) value;
        builder.setObject(fields.toJson());
    }
}
