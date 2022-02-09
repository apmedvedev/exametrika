/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.host.server.selectors;

import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.nodes.IAggregationNode;
import com.exametrika.api.component.ISelectionService.IRepresentationBuilder;
import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.api.component.nodes.IHostComponentVersion;
import com.exametrika.api.component.nodes.INodeComponent;
import com.exametrika.api.component.schema.ISelectorSchema;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonArrayBuilder;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.impl.component.nodes.GroupComponentVersionNode;
import com.exametrika.impl.component.nodes.HealthComponentVersionNode;
import com.exametrika.impl.component.selectors.AllComponentsSelector;
import com.exametrika.spi.aggregator.IComponentAccessorFactory;
import com.exametrika.spi.aggregator.IComputeContext;
import com.exametrika.spi.component.Selectors;


/**
 * The {@link AllHostsSelector} is an all hosts selector.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class AllHostsSelector extends AllComponentsSelector {
    public AllHostsSelector(IComponent component, ISelectorSchema schema) {
        super(component, schema, "hosts", "host.kpi", true);
    }

    @Override
    protected void doBuildComponentGroup(GroupComponentVersionNode version, Json result) {
        buildHost(version, "host.kpi", result, true, false);
    }

    @Override
    protected void doBuildComponent(HealthComponentVersionNode version, String componentType, Json result, boolean server) {
        buildHost(version, componentType, result, false, server);
    }

    private void buildHost(HealthComponentVersionNode version, String componentType, final Json result, boolean group, final boolean server) {
        if (!group && version instanceof IHostComponentVersion) {
            JsonArrayBuilder nodes = new JsonArrayBuilder();
            for (INodeComponent node : ((IHostComponentVersion) version).getNodes())
                nodes.add(Long.toString(node.getScopeId()));

            result.put("nodes", nodes.toJson());
        }

        final JsonObject[] metadata = new JsonObject[1];
        if (buildRepresentation(version, componentType, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                if (server)
                    metadata[0] = aggregationNode.getMetadata();

                Selectors.buildRelativeMetric(value, accessorFactory, computeContext, "host.cpu.used.%used", null,
                        "host.cpu.used.forecast(%used).level", "metrics.cpu", result);
                Selectors.buildRelativeMetric(value, accessorFactory, computeContext, "host.memory.used.%used",
                        "host.memory.used.std.avg", "host.memory.used.forecast(%used).level", "metrics.memory", result);
                Selectors.buildRelativeMetric(value, accessorFactory, computeContext, "host.swap.used.%used",
                        "host.swap.used.std.avg", "host.swap.used.forecast(%used).level", "metrics.swap", result);
                Selectors.buildRateMetric(value, accessorFactory, computeContext, "host.net.received.rate(bytes)",
                        "host.net.received.std.sum", "host.net.received.forecast(rate(bytes)).level", "metrics.net.received", result);
                Selectors.buildRateMetric(value, accessorFactory, computeContext, "host.net.sent.rate(bytes)",
                        "host.net.sent.std.sum", "host.net.sent.forecast(rate(bytes)).level", "metrics.net.sent", result);
                Selectors.buildRateMetric(value, accessorFactory, computeContext, "host.disk.read.rate(bytes)",
                        "host.disk.read.std.sum", null, "metrics.disk.read", result);
                Selectors.buildRateMetric(value, accessorFactory, computeContext, "host.disk.write.rate(bytes)",
                        "host.disk.write.std.sum", null, "metrics.disk.written", result);

                return JsonUtils.EMPTY_OBJECT;
            }
        }) == null)
            result.put("noData", true);

        JsonObject properties = null;
        if (server)
            properties = metadata[0];
        else if (!group)
            properties = version.getProperties();

        if (properties != null) {
            result.put("os", properties.get("description") + " (" + properties.get("machine") + ")");
            result.put("cpu", properties.get("cpu"));
            result.put("memory", properties.get("memory"));
            result.put("disk", properties.get("disk"));
            result.put("network", properties.get("network"));
            result.put("swap", properties.get("swap"));
        }
    }
}
