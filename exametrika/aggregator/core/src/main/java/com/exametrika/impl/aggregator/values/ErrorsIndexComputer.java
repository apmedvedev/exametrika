/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.values;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.api.aggregator.config.model.ComponentRepresentationSchemaConfiguration;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.spi.aggregator.IComponentAccessor;
import com.exametrika.spi.aggregator.IComponentAccessorFactory;
import com.exametrika.spi.aggregator.IComputeContext;
import com.exametrika.spi.aggregator.IMetricComputer;


/**
 * The {@link ErrorsIndexComputer} is an errors index computer.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class ErrorsIndexComputer implements IMetricComputer {
    private final List<IComponentAccessor> errorsAccessors;
    private final List<String> metricNames;

    public ErrorsIndexComputer(ComponentRepresentationSchemaConfiguration componentConfiguration, IComponentAccessorFactory componentAccessorFactory) {
        List<IComponentAccessor> errorsAccessors = new ArrayList<IComponentAccessor>();
        List<String> metricNames = new ArrayList<String>();
        for (String metric : componentConfiguration.getMetrics().keySet()) {
            if (metric.endsWith(".errors") && metric.split("[.]").length == 3) {
                errorsAccessors.add(componentAccessorFactory.createAccessor(null, null, metric));
                metricNames.add(metric);
            }
        }

        this.errorsAccessors = errorsAccessors;
        this.metricNames = metricNames;
    }

    public Object getValue(IComponentValue componentValue, IComputeContext context, boolean causes) {
        int value = 0;
        List<String> metrics = null;
        for (int i = 0; i < errorsAccessors.size(); i++) {
            IComponentAccessor accessor = errorsAccessors.get(i);

            Object errors = accessor.get(componentValue, context);
            if (!(errors instanceof JsonObject))
                continue;

            Object state = ((JsonObject) errors).get("state", null);
            if (!(state instanceof String))
                continue;

            if (state.equals("error")) {
                if (causes) {
                    if (metrics == null)
                        metrics = new ArrayList<String>();

                    if (value < 2)
                        metrics.clear();

                    metrics.add(metricNames.get(i));
                }

                value = 2;
            } else if (state.equals("warning") && value < 2) {
                if (causes) {
                    if (metrics == null)
                        metrics = new ArrayList<String>();

                    metrics.add(metricNames.get(i));
                }

                value = 1;
            }
        }

        String state;
        switch (value) {
            case 2:
                state = "error";
                break;
            case 1:
                state = "warning";
                break;
            default:
                state = "normal";
        }

        if (!causes)
            return state;
        else
            return Json.object().put("state", state).putIf("causes", JsonUtils.toJson(metrics), metrics != null).toObject();
    }

    @Override
    public Object compute(IComponentValue componentValue, IMetricValue value, IComputeContext context) {
        return getValue(componentValue, context, false);
    }

    @Override
    public void computeSecondary(IComponentValue componentValue, IMetricValue value, IComputeContext context) {
    }
}
