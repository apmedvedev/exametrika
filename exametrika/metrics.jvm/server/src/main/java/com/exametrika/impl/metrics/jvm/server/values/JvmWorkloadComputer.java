/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.jvm.server.values;

import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.api.aggregator.common.values.IObjectValue;
import com.exametrika.api.metrics.jvm.server.config.model.JvmWorkloadRepresentationSchemaConfiguration;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Numbers;
import com.exametrika.impl.aggregator.common.values.ObjectBuilder;
import com.exametrika.impl.aggregator.values.ObjectComputer;
import com.exametrika.spi.aggregator.IComponentAccessor;
import com.exametrika.spi.aggregator.IComponentAccessorFactory;
import com.exametrika.spi.aggregator.IComputeContext;


/**
 * The {@link JvmWorkloadComputer} is an jvm workload computer.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class JvmWorkloadComputer extends ObjectComputer {
    private final JvmWorkloadRepresentationSchemaConfiguration configuration;
    private IComponentAccessor firstAccessor;
    private IComponentAccessor secondAccessor;

    public JvmWorkloadComputer(JvmWorkloadRepresentationSchemaConfiguration configuration, IComponentAccessorFactory componentAccessorFactory,
                               String metricName, int metricIndex) {

        this.configuration = configuration;
        switch (configuration.getType()) {
            case JVM_CPU_WORKLOAD:
                firstAccessor = componentAccessorFactory.createAccessor(null, null, "host.process.cpu.total.std.sum");
                secondAccessor = componentAccessorFactory.createAccessor(null, null, "host.process.cpu.max.std.sum");
                break;
            case JVM_MEMORY_WORKLOAD:
                firstAccessor = componentAccessorFactory.createAccessor(null, null, "jvm.memory.heap.used.std.sum");
                secondAccessor = componentAccessorFactory.createAccessor(null, null, "jvm.memory.heap.max.std.sum");
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

        double workload = 0;
        switch (configuration.getType()) {
            case JVM_MEMORY_WORKLOAD:
            case JVM_CPU_WORKLOAD: {
                Number used = (Number) firstAccessor.get(componentValue, context);
                Number total = (Number) secondAccessor.get(componentValue, context);
                if (used != null && total != null && total.longValue() != 0)
                    workload = Numbers.percents(used.doubleValue(), total.doubleValue());
                break;
            }
            default:
                Assert.error();
        }

        String state;
        if (workload < configuration.getWarningThreshold())
            state = "normal";
        else if (workload < configuration.getErrorThreshold())
            state = "warning";
        else
            state = "error";

        JsonObjectBuilder fields = new JsonObjectBuilder();
        fields.put("value", workload);
        fields.put("state", state);

        ObjectBuilder builder = (ObjectBuilder) value;
        builder.setObject(fields.toJson());
    }
}
