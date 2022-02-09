/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.exametrika.api.aggregator.IPeriodName;
import com.exametrika.api.aggregator.IPeriodNameManager;
import com.exametrika.api.aggregator.common.model.IMetricName;
import com.exametrika.api.aggregator.common.model.Measurement;
import com.exametrika.api.aggregator.common.model.MeasurementId;
import com.exametrika.api.aggregator.common.model.Names;
import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.common.values.IObjectValue;
import com.exametrika.api.aggregator.config.model.ErrorLogTransformerSchemaConfiguration;
import com.exametrika.api.aggregator.fields.IAggregationRecord;
import com.exametrika.api.aggregator.fields.IPeriodAggregationField;
import com.exametrika.api.aggregator.nodes.IAggregationNode;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.NameFilter;
import com.exametrika.impl.aggregator.common.values.ComponentValue;
import com.exametrika.impl.aggregator.common.values.NameValue;
import com.exametrika.impl.aggregator.common.values.StandardValue;
import com.exametrika.impl.aggregator.nodes.StackLogNode;
import com.exametrika.spi.aggregator.IAggregationLogTransformer;
import com.exametrika.spi.aggregator.IErrorAggregationStrategy;
import com.exametrika.spi.aggregator.config.model.ErrorAggregationStrategySchemaConfiguration;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link ErrorLogTransformer} is an error log transformer.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class ErrorLogTransformer implements IAggregationLogTransformer {
    private final ErrorLogTransformerSchemaConfiguration configuration;
    private final List<IErrorAggregationStrategy> errorAggregationStrategies;
    private final IPeriodNameManager nameManager;

    public ErrorLogTransformer(ErrorLogTransformerSchemaConfiguration configuration, IDatabaseContext context) {
        Assert.notNull(configuration);
        Assert.notNull(context);

        this.configuration = configuration;
        this.nameManager = context.getTransactionProvider().getTransaction().findExtension(IPeriodNameManager.NAME);

        List<IErrorAggregationStrategy> errorAggregationStrategies = new ArrayList<IErrorAggregationStrategy>();
        for (ErrorAggregationStrategySchemaConfiguration strategy : configuration.getErrorAggregationStrategies())
            errorAggregationStrategies.add(strategy.createStrategy());

        this.errorAggregationStrategies = errorAggregationStrategies;
    }

    @Override
    public List<Measurement> transform(IAggregationNode node) {
        String componentType = node.getSchema().getConfiguration().getComponentType().getName();
        NameFilter filter = configuration.getStackTraceFilter();

        List<Measurement> measurements = new ArrayList<Measurement>();
        IPeriodAggregationField field = node.getField(node.getSchema().getAggregationField());

        JsonObject metadata = null;
        if (node instanceof StackLogNode) {
            StackLogNode stackLog = (StackLogNode) node;
            if (stackLog.getMainNode() != null)
                metadata = Json.object().put("main", stackLog.getMainNode().getComponentType()).toObject();
        }

        IComponentValue value = new ComponentValue(Collections.singletonList(
                new NameValue(Collections.singletonList(new StandardValue(1, 1, 1, 1)))), metadata);

        for (IAggregationRecord record : field.getPeriodRecords()) {
            IObjectValue logValue = (IObjectValue) record.getValue().getMetrics().get(0);
            JsonObject object = (JsonObject) logValue.getObject();

            String errorType = getErrorType(object, componentType);

            String metricName = getErrorMetricName(object, filter);
            if (metricName == null)
                metricName = "unknown";

            for (IErrorAggregationStrategy strategy : errorAggregationStrategies) {
                String derivedErrorType = strategy.getDerivedType(errorType);
                if (derivedErrorType == null)
                    continue;

                IMetricName metric = Names.getMetric(derivedErrorType + "&." + metricName);
                IPeriodName name = nameManager.addName(metric);

                MeasurementId id = new MeasurementId(node.getLocation().getScopeId(), name.getId(), configuration.getErrorComponentType());
                Measurement measurement = new Measurement(id, value, 0, null);

                measurements.add(measurement);
            }
        }

        return measurements;
    }

    public static String getErrorType(JsonObject object, String componentType) {
        String errorType = componentType.replace(".", "_");
        JsonObject exception = object.get("exception", null);
        if (exception != null)
            errorType += "." + exception.get("class");

        return errorType;
    }

    public static String getErrorMetricName(JsonObject object, NameFilter filter) {
        if (filter == null) {
            String errorLocation = object.get("errorLocation", null);
            if (errorLocation != null)
                return errorLocation;
        }

        JsonObject exception = object.get("exception", null);

        JsonArray stackTrace;
        if (exception != null)
            stackTrace = exception.get("stackTrace", null);
        else
            stackTrace = object.get("stackTrace", null);

        if (stackTrace == null || stackTrace.isEmpty())
            return null;

        String metricName = "";
        if (filter != null) {
            boolean found = false;
            for (Object element : stackTrace) {
                JsonObject stackTraceElement = (JsonObject) element;
                String className = stackTraceElement.get("class", null);
                if (className == null)
                    break;

                metricName = className + "." + stackTraceElement.get("method");
                if (filter.match(metricName)) {
                    found = true;
                    break;
                }
            }

            if (!found)
                return null;
        } else {
            JsonObject stackTraceElement = (JsonObject) stackTrace.get(0);
            String className = stackTraceElement.get("class", null);
            if (className == null)
                return null;

            metricName = stackTraceElement.get("class") + "." + stackTraceElement.get("method");
        }

        return metricName;
    }
}
