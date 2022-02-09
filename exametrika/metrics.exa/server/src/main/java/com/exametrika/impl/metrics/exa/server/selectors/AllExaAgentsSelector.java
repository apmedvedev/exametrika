/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.exa.server.selectors;

import com.exametrika.api.aggregator.IPeriodName;
import com.exametrika.api.aggregator.IPeriodNameManager;
import com.exametrika.api.aggregator.Location;
import com.exametrika.api.aggregator.common.model.Names;
import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.nodes.IAggregationNode;
import com.exametrika.api.component.ISelectionService.IRepresentationBuilder;
import com.exametrika.api.component.nodes.IAgentComponent;
import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.api.component.schema.ISelectorSchema;
import com.exametrika.api.metrics.exa.server.nodes.IExaAgentComponentVersion;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.component.nodes.AgentComponentVersionNode;
import com.exametrika.impl.component.nodes.GroupComponentVersionNode;
import com.exametrika.impl.component.nodes.HealthComponentVersionNode;
import com.exametrika.impl.component.selectors.AllComponentsSelector;
import com.exametrika.spi.aggregator.IComponentAccessorFactory;
import com.exametrika.spi.aggregator.IComputeContext;
import com.exametrika.spi.component.Selectors;
import com.exametrika.spi.component.Selectors.StateInfo;


/**
 * The {@link AllExaAgentsSelector} is a jvm all exa agents selector.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class AllExaAgentsSelector extends AllComponentsSelector {
    private IPeriodNameManager nameManager;

    public AllExaAgentsSelector(IComponent component, ISelectorSchema schema) {
        super(component, schema, "agents", "exa.agent", false);
    }

    @Override
    protected void doBuildComponentGroup(GroupComponentVersionNode version, Json result) {
        buildAgent(version, "exa.agent", result, true);
    }

    @Override
    protected void doBuildComponent(HealthComponentVersionNode version, String componentType, Json result, boolean server) {
        buildAgent(version, componentType, result, false);
    }

    private void buildAgent(HealthComponentVersionNode version, String componentType, final Json result, boolean group) {
        if (!group && version instanceof IExaAgentComponentVersion) {
            IAgentComponent parent = ((IExaAgentComponentVersion) version).getParent();
            if (parent != null) {
                AgentComponentVersionNode parentVersion = (AgentComponentVersionNode) parent.get();
                if (parentVersion != null) {
                    StateInfo info = Selectors.buildState(parentVersion, true, null, null, 0, null);
                    JsonObjectBuilder reference = Selectors.buildReference(parentVersion);
                    reference.put("state", info.state);
                    result.put("parentComponent", reference);
                }
            }
        }

        final JsonObject properties;
        if (!group)
            properties = version.getProperties();
        else
            properties = null;

        if (properties != null) {
            String home = properties.get("home");
            String bootConfigurationPath = properties.get("bootConfigurationPath");
            String serviceConfigurationPath = properties.get("serviceConfigurationPath");
            if (bootConfigurationPath.startsWith(home))
                bootConfigurationPath = "<home>" + bootConfigurationPath.substring(home.length());
            if (serviceConfigurationPath.startsWith(home))
                serviceConfigurationPath = "<home>" + serviceConfigurationPath.substring(home.length());

            result.put("version", properties.get("version"))
                    .put("home", home)
                    .put("bootConfigurationPath", bootConfigurationPath)
                    .put("serviceConfigurationPath", serviceConfigurationPath);
        }

        long messagingMetricId = getMessagingMetricId(version);
        JsonObject resMessaging = (JsonObject) selectionService.buildRepresentation(periodType, currentTime,
                new Location(version.getComponent().getScopeId(), messagingMetricId), "exa.messaging", 0, new IRepresentationBuilder() {
                    @Override
                    public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                        IComputeContext computeContext) {
                        Selectors.buildRateMetric(value, accessorFactory, computeContext, "exa.messaging.send.bytes.rate",
                                "exa.messaging.send.bytes.std.sum", null, "sent", result);
                        Selectors.buildRateMetric(value, accessorFactory, computeContext, "exa.messaging.receive.bytes.rate",
                                "exa.messaging.receive.bytes.std.sum", null, "received", result);

                        return JsonUtils.EMPTY_OBJECT;
                    }
                });

        JsonObject resLog = buildRepresentation(version, "exa.log", new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                result.put("logErrors", Selectors.getMetric(value, accessorFactory, computeContext, "exa.log.errorCount"));

                return null;
            }
        });

        if (resLog == null && resMessaging == null)
            result.put("noData", true);
    }

    private long getMessagingMetricId(HealthComponentVersionNode version) {
        JsonObject properties = version.getProperties();
        if (properties != null)
            return getMetricId("channels." + properties.get("node"));
        else
            return 0;
    }

    private long getMetricId(String metricName) {
        if (nameManager == null) {
            nameManager = this.schema.getContext().getTransactionProvider().getTransaction().findExtension(IPeriodNameManager.NAME);
            Assert.notNull(nameManager);
        }

        IPeriodName name = nameManager.findByName(Names.getMetric(metricName));
        if (name != null)
            return name.getId();
        else
            return 0;
    }
}
