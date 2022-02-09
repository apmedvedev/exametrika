/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.jvm.server.selectors;

import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.nodes.IAggregationNode;
import com.exametrika.api.component.ISelectionService.IRepresentationBuilder;
import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.api.component.nodes.IHostComponent;
import com.exametrika.api.component.nodes.INodeComponentVersion;
import com.exametrika.api.component.schema.ISelectorSchema;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.impl.component.nodes.GroupComponentVersionNode;
import com.exametrika.impl.component.nodes.HealthComponentVersionNode;
import com.exametrika.impl.component.nodes.HostComponentVersionNode;
import com.exametrika.impl.component.selectors.AllComponentsSelector;
import com.exametrika.spi.aggregator.IComponentAccessorFactory;
import com.exametrika.spi.aggregator.IComputeContext;
import com.exametrika.spi.component.Selectors;
import com.exametrika.spi.component.Selectors.StateInfo;


/**
 * The {@link AllJvmNodesSelector} is a jvm all nodes selector.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class AllJvmNodesSelector extends AllComponentsSelector {
    public AllJvmNodesSelector(IComponent component, ISelectorSchema schema) {
        super(component, schema, "jvmNodes", "jvm.kpi", true);
    }

    @Override
    protected void doBuildComponentGroup(GroupComponentVersionNode version, Json result) {
        buildNode(version, "jvm.kpi", result, true, false);
    }

    @Override
    protected void doBuildComponent(HealthComponentVersionNode version, String componentType, Json result, boolean server) {
        buildNode(version, componentType, result, false, server);
    }

    private void buildNode(HealthComponentVersionNode version, String componentType, final Json result, boolean group, final boolean server) {
        if (!group && version instanceof INodeComponentVersion) {
            IHostComponent host = ((INodeComponentVersion) version).getHost();
            if (host != null) {
                HostComponentVersionNode hostVersion = (HostComponentVersionNode) host.get();
                if (hostVersion != null) {
                    StateInfo info = Selectors.buildState(hostVersion, true, null, null, 0, null);
                    JsonObjectBuilder reference = Selectors.buildReference(hostVersion);
                    reference.put("state", info.state);
                    result.put("host", reference);
                }
            }
        }

        final JsonObject[] metadata = new JsonObject[1];
        if (buildRepresentation(version, componentType, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                if (server)
                    metadata[0] = aggregationNode.getMetadata();

                Selectors.buildRelativeMetric(value, accessorFactory, computeContext, "host.process.cpu.user.%user", null,
                        null, "cpu", result);
                Selectors.buildRelativeMetric(value, accessorFactory, computeContext, "host.process.memory.resident.%resident",
                        "host.process.memory.resident.std.avg", null, "memory", result);
                Selectors.buildRateMetric(value, accessorFactory, computeContext, "host.process.memory.minorFaults.rate",
                        "host.process.memory.minorFaults.std.sum", null, "minorFaults", result);
                Selectors.buildRateMetric(value, accessorFactory, computeContext, "host.process.memory.majorFaults.rate",
                        "host.process.memory.majorFaults.std.sum", null, "majorFaults", result);

                result.put("threads", Selectors.getMetric(value, accessorFactory, computeContext, "host.process.threads.std.avg"));
                result.put("fileDescriptors", Selectors.getMetric(value, accessorFactory, computeContext, "host.process.fd.std.avg"));

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
            result.put("vmName", properties.get("vmName"));
            result.put("vmVersion", properties.get("vmVersion"));
            result.put("vmVendor", properties.get("vmVendor"));
            result.put("vmHome", properties.get("vmHome"));

            JsonArray args = properties.get("args");
            StringBuilder argsStr = new StringBuilder();
            boolean first = true;
            for (Object arg : args) {
                if (first) {
                    first = false;
                    continue;
                } else
                    argsStr.append(" ");

                argsStr.append(arg);
            }

            result.put("pid", properties.get("id"))
                    .put("parentId", properties.get("parentId"))
                    .put("command", properties.get("command"))
                    .put("args", argsStr)
                    .put("user", properties.get("user"))
                    .put("priority", properties.get("priority"));
        }
    }
}
