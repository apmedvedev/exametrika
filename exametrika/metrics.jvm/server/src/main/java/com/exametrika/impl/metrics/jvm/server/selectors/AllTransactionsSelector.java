/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.jvm.server.selectors;

import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.nodes.IAggregationNode;
import com.exametrika.api.component.ISelectionService.IRepresentationBuilder;
import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.api.component.nodes.INodeComponent;
import com.exametrika.api.component.nodes.ITransactionComponentVersion;
import com.exametrika.api.component.schema.ISelectorSchema;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.impl.component.nodes.GroupComponentVersionNode;
import com.exametrika.impl.component.nodes.HealthComponentVersionNode;
import com.exametrika.impl.component.nodes.NodeComponentVersionNode;
import com.exametrika.impl.component.selectors.AllComponentsSelector;
import com.exametrika.spi.aggregator.IComponentAccessorFactory;
import com.exametrika.spi.aggregator.IComputeContext;
import com.exametrika.spi.component.Selectors;
import com.exametrika.spi.component.Selectors.StateInfo;


/**
 * The {@link AllTransactionsSelector} is a all transactions selector.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class AllTransactionsSelector extends AllComponentsSelector {
    public AllTransactionsSelector(IComponent component, ISelectorSchema schema) {
        super(component, schema, "transactions", "primary.app.entryPoint", false);
    }

    @Override
    protected void doBuildComponentGroup(GroupComponentVersionNode version, Json result) {
        buildTransaction(version, "primary.app.entryPoint", result, true, false);
    }

    @Override
    protected void doBuildComponent(HealthComponentVersionNode version, String componentType, Json result, boolean server) {
        buildTransaction(version, componentType, result, false, server);
    }

    private void buildTransaction(HealthComponentVersionNode version, String componentType, final Json result, boolean group, final boolean server) {
        if (!group && version instanceof ITransactionComponentVersion) {
            INodeComponent node = ((ITransactionComponentVersion) version).getPrimaryNode();
            if (node != null) {
                NodeComponentVersionNode nodeVersion = (NodeComponentVersionNode) node.get();
                if (nodeVersion != null) {
                    StateInfo info = Selectors.buildState(nodeVersion, true, null, null, 0, null);
                    JsonObjectBuilder reference = Selectors.buildReference(nodeVersion);
                    reference.put("state", info.state);
                    reference.put("id", node.getScopeId());
                    result.put("node", reference);
                }
            }
        }

        if (buildRepresentation(version, componentType, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Selectors.buildRateMetric(value, accessorFactory, computeContext, "app.request.time.rate",
                        "app.request.time.std.count", "app.request.time.forecast(rate).level", "throughput", result);
                result.put("latency", Selectors.getMetric(value, accessorFactory, computeContext, "app.request.time.histo.percentile(50).value"));
                Selectors.buildRateMetric(value, accessorFactory, computeContext, "app.entryPoint.errors.count.rate",
                        "app.entryPoint.errors.count.std.sum", "app.entryPoint.errors.count.forecast(rate).level", "errors", result);
                result.put("stalls", Selectors.getMetric(value, accessorFactory, computeContext, "app.entryPoint.stalls.count.std.sum"));
                Selectors.buildRateMetric(value, accessorFactory, computeContext, "app.receive.bytes.rate(bytes)",
                        "app.receive.bytes.std.sum", null, "received", result);
                Selectors.buildRateMetric(value, accessorFactory, computeContext, "app.send.bytes.rate(bytes)",
                        "app.send.bytes.std.sum", null, "sent", result);
                result.put("%errors", Selectors.getMetric(value, accessorFactory, computeContext, "app.entryPoint.errors.count.%errors"));
                result.put("%stalls", Selectors.getMetric(value, accessorFactory, computeContext, "app.entryPoint.stalls.count.%stalls"));

                return JsonUtils.EMPTY_OBJECT;
            }
        }) == null)
            result.put("noData", true);

        JsonObject properties = null;
        if (!group)
            properties = version.getProperties();

        if (properties != null) {
            String type = properties.get("type");
            if (type.contains("http")) {
                result.put("type", "httpEntry");
                result.put("typeTitle", "HTTP servlet");
                result.put("app", properties.get("app"));
                String url = properties.get("url", null);
                if (url != null)
                    result.put("url", url);
                String servlet = properties.get("servlet", null);
                if (servlet != null)
                    result.put("servlet", servlet);
            } else if (type.contains("jms")) {
                result.put("type", "jmsEntry");
                result.put("typeTitle", "JMS consumer");
                result.put("destinationType", properties.get("destinationType"));
                result.put("destination", properties.get("destination"));
            } else if (type.contains("method")) {
                result.put("type", "methodEntry");
                result.put("typeTitle", "Method");
            }

            result.put("platform", "jvm");
            result.put("combineType", properties.get("combineType"));
        }
    }
}
