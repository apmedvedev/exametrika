/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.jvm.server.selectors;

import java.util.Map;

import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.nodes.IAggregationNode;
import com.exametrika.api.component.ISelectionService.IRepresentationBuilder;
import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.api.component.schema.ISelectorSchema;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.spi.aggregator.IComponentAccessorFactory;
import com.exametrika.spi.aggregator.IComputeContext;
import com.exametrika.spi.component.Selectors;


/**
 * The {@link TransactionSelector} is an transaction selector.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class TransactionSelector extends ApplicationSelector {
    public TransactionSelector(IComponent component, ISelectorSchema schema) {
        super(component, schema, "primary.app.entryPoint");
    }

    @Override
    protected boolean isTransaction() {
        return true;
    }

    @Override
    protected Object doSelect(Map<String, ?> parameters) {
        String type = (String) parameters.get("type");
        Long scopeId = (Long) parameters.get("secondaryScopeId");
        if (scopeId == null)
            scopeId = component.getScopeId();

        if (type.equals("latency"))
            return selectLatency(parameters);
        else if (type.equals("receiveBytesHistogram"))
            return Selectors.selectHistogram(scopeId, selectionService, "primary.app.entryPoint", "app.receive.bytes", 0, parameters);
        else if (type.equals("sendBytesHistogram"))
            return Selectors.selectHistogram(scopeId, selectionService, "primary.app.entryPoint", "app.send.bytes", 0, parameters);
        else
            return super.doSelect(parameters);
    }

    @Override
    protected JsonObjectBuilder doBuildKpiMetrics(long time, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                                  IComputeContext computeContext) {
        Json json = Json.object();
        json.put("time", time)
                .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "app.request.time.rate"))
                .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "app.request.time.histo.percentile(50).value"))
                .put("y3", Selectors.getMetric(value, accessorFactory, computeContext, "app.entryPoint.errors.count.rate"))
                .put("y4", Selectors.getMetric(value, accessorFactory, computeContext, "app.entryPoint.stalls.count.std.sum"))
                .put("y5", Selectors.getMetric(value, accessorFactory, computeContext, "app.receive.bytes.rate(bytes)"))
                .put("y6", Selectors.getMetric(value, accessorFactory, computeContext, "app.send.bytes.rate(bytes)"))
                .put("y7", Selectors.getMetric(value, accessorFactory, computeContext, "app.request.time.std.count"))
                .put("y8", Selectors.getMetric(value, accessorFactory, computeContext, "app.entryPoint.errors.count.std.sum"))
                .put("y9", Selectors.getMetric(value, accessorFactory, computeContext, "app.receive.bytes.std.sum"))
                .put("y10", Selectors.getMetric(value, accessorFactory, computeContext, "app.send.bytes.std.sum"))
                .put("y11", Selectors.getMetric(value, accessorFactory, computeContext, "app.entryPoint.errors.count.%errors"))
                .put("y12", Selectors.getMetric(value, accessorFactory, computeContext, "app.entryPoint.stalls.count.%stalls"));

        JsonArray latencyThresholds = Selectors.getMetric(value, accessorFactory, computeContext, "app.latency.workload.thresholds");
        JsonArray throughputThresholds = Selectors.getMetric(value, accessorFactory, computeContext, "app.throughput.workload.thresholds");
        JsonArray errorsThresholds = Selectors.getMetric(value, accessorFactory, computeContext, "app.request.errors.thresholds");
        JsonArray stallsThresholds = Selectors.getMetric(value, accessorFactory, computeContext, "app.stalls.errors.thresholds");

        Json jsonAnnotations = json.putArray("annotations");

        if (latencyThresholds != null) {
            jsonAnnotations.addObject()
                    .put("name", "latency-t1")
                    .put("type", "thresholdWarning")
                    .put("y", latencyThresholds.get(0))
                    .end()
                    .addObject()
                    .put("name", "latency-t2")
                    .put("type", "thresholdError")
                    .put("y", latencyThresholds.get(1))
                    .end();
        }

        if (throughputThresholds != null) {
            jsonAnnotations.addObject()
                    .put("name", "throughput-t1")
                    .put("type", "thresholdWarning")
                    .put("y", throughputThresholds.get(0))
                    .end()
                    .addObject()
                    .put("name", "throughput-t2")
                    .put("type", "thresholdError")
                    .put("y", throughputThresholds.get(1))
                    .end();
        }

        if (errorsThresholds != null) {
            jsonAnnotations.addObject()
                    .put("name", "errors-t1")
                    .put("type", "thresholdWarning")
                    .put("y", errorsThresholds.get(0))
                    .end()
                    .addObject()
                    .put("name", "errors-t2")
                    .put("type", "thresholdError")
                    .put("y", errorsThresholds.get(1))
                    .end();
        }

        if (stallsThresholds != null) {
            jsonAnnotations.addObject()
                    .put("name", "stalls-t1")
                    .put("type", "thresholdWarning")
                    .put("y", stallsThresholds.get(0))
                    .end()
                    .addObject()
                    .put("name", "stalls-t2")
                    .put("type", "thresholdError")
                    .put("y", stallsThresholds.get(1))
                    .end();
        }

        Selectors.checkAnomaly(value, accessorFactory, computeContext, "app.request.time", "rate", 1, true, jsonAnnotations);
        Selectors.checkAnomaly(value, accessorFactory, computeContext, "app.request.time", "median", 2, true, jsonAnnotations);
        Selectors.checkAnomaly(value, accessorFactory, computeContext, "app.entryPoint.errors.count", "rate", 3, true, jsonAnnotations);

        return json.toObjectBuilder();
    }

    private Object selectLatency(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), 0, "primary.app.entryPoint", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "app.request.time.histo.percentile(10).value"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "app.request.time.histo.percentile(25).value"))
                        .put("y3", Selectors.getMetric(value, accessorFactory, computeContext, "app.request.time.histo.percentile(50).value"))
                        .put("y4", Selectors.getMetric(value, accessorFactory, computeContext, "app.request.time.histo.percentile(75).value"))
                        .put("y5", Selectors.getMetric(value, accessorFactory, computeContext, "app.request.time.histo.percentile(90).value"));

                return json.toObject();
            }
        });
    }
}
