/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.exa.server.selectors;

import java.util.Map;

import com.exametrika.api.aggregator.IPeriodName;
import com.exametrika.api.aggregator.IPeriodNameManager;
import com.exametrika.api.aggregator.common.model.Names;
import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.nodes.IAggregationNode;
import com.exametrika.api.component.ISelectionService;
import com.exametrika.api.component.ISelectionService.IRepresentationBuilder;
import com.exametrika.api.component.nodes.IAgentComponent;
import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.api.component.nodes.IComponentVersion;
import com.exametrika.api.component.schema.ISelectorSchema;
import com.exametrika.api.metrics.exa.server.nodes.IExaAgentComponentVersion;
import com.exametrika.api.metrics.exa.server.nodes.IExaServerComponent;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.component.nodes.AgentComponentVersionNode;
import com.exametrika.impl.component.selectors.Selector;
import com.exametrika.impl.metrics.exa.server.nodes.ExaServerComponentVersionNode;
import com.exametrika.spi.aggregator.IComponentAccessorFactory;
import com.exametrika.spi.aggregator.IComputeContext;
import com.exametrika.spi.component.Selectors;
import com.exametrika.spi.component.Selectors.StateInfo;


/**
 * The {@link ExaAgentSelector} is an ExaAgent selector.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class ExaAgentSelector extends Selector {
    private final ISelectionService selectionService;
    private IPeriodNameManager nameManager;
    private long messagingMetricId;

    public ExaAgentSelector(IComponent component, ISelectorSchema schema) {
        super(component, schema);

        selectionService = this.schema.getContext().getTransactionProvider().getTransaction().findDomainService(ISelectionService.NAME);
        Assert.notNull(selectionService);
    }

    @Override
    protected Object doSelect(Map<String, ?> parameters) {
        ensureNames();

        String type = (String) parameters.get("type");
        if (type.equals("properties"))
            return selectProperties(parameters);
        else if (type.equals("sendBytes"))
            return selectSendBytes(parameters);
        else if (type.equals("sendBytesHistogram"))
            return Selectors.selectHistogram(component.getScopeId(), selectionService, "exa.messaging", "exa.messaging.send.bytes",
                    messagingMetricId, parameters);
        else if (type.equals("receiveBytes"))
            return selectReceiveBytes(parameters);
        else if (type.equals("receiveBytesHistogram"))
            return Selectors.selectHistogram(component.getScopeId(), selectionService, "exa.messaging", "exa.messaging.receive.bytes",
                    messagingMetricId, parameters);
        else if (type.equals("messagingErrorCount"))
            return selectMessagingErrorCount(parameters);
        else if (type.equals("logErrorCount"))
            return selectLogErrorCount(parameters);
        else if (type.equals("logErrors"))
            return Selectors.selectLog(component.getScopeId(), selectionService, "exa.log.errors", "exa.log.errors", 0, parameters);
        else if (type.equals("instrumentTime"))
            return selectInstrumentTime(parameters);
        else if (type.equals("instrumentTimeHistogram"))
            return Selectors.selectHistogram(component.getScopeId(), selectionService, "exa.instrument", "exa.instrument.time",
                    0, parameters);
        else if (type.equals("beforeInstrumentBytes"))
            return selectBeforeInstrumentBytes(parameters);
        else if (type.equals("beforeInstrumentBytesHistogram"))
            return Selectors.selectHistogram(component.getScopeId(), selectionService, "exa.instrument", "exa.instrument.beforeBytes",
                    0, parameters);
        else if (type.equals("afterInstrumentBytes"))
            return selectAfterInstrumentBytes(parameters);
        else if (type.equals("afterInstrumentBytesHistogram"))
            return Selectors.selectHistogram(component.getScopeId(), selectionService, "exa.instrument", "exa.instrument.afterBytes",
                    0, parameters);
        else if (type.equals("instrumentSkipped"))
            return selectInstrumentSkipped(parameters);
        else if (type.equals("instrumentJoinPoints"))
            return selectInstrumentJoinPoints(parameters);
        else if (type.equals("instrumentErrorsCount"))
            return selectInstrumentErrorsCount(parameters);
        else if (type.equals("instrumentErrors"))
            return Selectors.selectLog(component.getScopeId(), selectionService, "exa.instrument.errors.log", "exa.instrument.errors.log", 0, parameters);
        else
            return Assert.error();
    }

    private Object selectProperties(Map<String, ?> parameters) {
        Assert.isTrue(Boolean.TRUE.equals(parameters.get("fullUpdate")));
        String periodType = ((JsonObject) parameters.get("request")).select("timeScale.periodType");
        long currentTime = ((Number) parameters.get("current")).longValue();

        Json json = Json.object().put("fullUpdate", true);
        Json jsonRows = json.putArray("rows");
        IComponentVersion version = component.get();
        if (version != null) {
            StateInfo info = Selectors.buildState(version, true, null, periodType, currentTime, null);

            Json jsonRow = jsonRows.addObject();
            jsonRow.put("id", component.getScopeId())
                    .put("elementId", component.getScopeId())
                    .put("title", version.getTitle())
                    .put("description", version.getDescription())
                    .put("state", info.state);

            JsonObject properties = version.getProperties();
            if (properties != null) {
                String home = properties.get("home");
                String bootConfigurationPath = properties.get("bootConfigurationPath");
                String serviceConfigurationPath = properties.get("serviceConfigurationPath");
                if (bootConfigurationPath.startsWith(home))
                    bootConfigurationPath = "<home>" + bootConfigurationPath.substring(home.length());
                if (serviceConfigurationPath.startsWith(home))
                    serviceConfigurationPath = "<home>" + serviceConfigurationPath.substring(home.length());

                jsonRow.put("version", properties.get("version"))
                        .put("home", home)
                        .put("bootConfigurationPath", bootConfigurationPath)
                        .put("serviceConfigurationPath", serviceConfigurationPath);
            }

            IAgentComponent parent = ((IExaAgentComponentVersion) version).getParent();
            if (parent != null) {
                AgentComponentVersionNode parentVersion = (AgentComponentVersionNode) parent.get();
                if (parentVersion != null) {
                    StateInfo nodeInfo = Selectors.buildState(parentVersion, true, null, null, 0, null);
                    JsonObjectBuilder reference = Selectors.buildReference(parentVersion);
                    reference.put("state", nodeInfo.state);
                    jsonRow.put("parentComponent", reference);
                }
            }

            IExaServerComponent server = ((IExaAgentComponentVersion) version).getServer();
            if (server != null) {
                ExaServerComponentVersionNode serverVersion = (ExaServerComponentVersionNode) server.get();
                if (serverVersion != null) {
                    Json reference = Json.object()
                            .put("title", serverVersion.getTitle())
                            .put("description", serverVersion.getDescription())
                            .put("state", "ExaServer")
                            .put("link", "#components/exaserver/server/server");
                    jsonRow.put("server", reference.toObject());
                }
            }
        }
        return json.toObject();
    }

    private Object selectSendBytes(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), messagingMetricId, "exa.messaging", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "exa.messaging.send.bytes.rate"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "exa.messaging.send.bytes.std.sum"));

                return json.toObject();
            }
        });
    }

    private Object selectReceiveBytes(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), messagingMetricId, "exa.messaging", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "exa.messaging.receive.bytes.rate"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "exa.messaging.receive.bytes.std.sum"));

                return json.toObject();
            }
        });
    }

    private Object selectMessagingErrorCount(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), messagingMetricId, "exa.messaging", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "exa.messaging.errors.count.std.sum"));

                return json.toObject();
            }
        });
    }

    private Object selectLogErrorCount(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), 0, "exa.log", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "exa.log.errorCount.std.sum"));

                return json.toObject();
            }
        });
    }

    private Object selectInstrumentTime(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), 0, "exa.instrument", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "exa.instrument.time.std.avg"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "exa.instrument.time.std.sum"))
                        .put("y3", Selectors.getMetric(value, accessorFactory, computeContext, "exa.instrument.time.std.count"));

                return json.toObject();
            }
        });
    }

    private Object selectBeforeInstrumentBytes(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), 0, "exa.instrument", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "exa.instrument.beforeBytes.rate"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "exa.instrument.beforeBytes.std.sum"));

                return json.toObject();
            }
        });
    }

    private Object selectAfterInstrumentBytes(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), 0, "exa.instrument", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "exa.instrument.afterBytes.rate"))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "exa.instrument.afterBytes.std.sum"));

                return json.toObject();
            }
        });
    }

    private Object selectInstrumentSkipped(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), 0, "exa.instrument", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "exa.instrument.skipped.std.sum"));

                return json.toObject();
            }
        });
    }

    private Object selectInstrumentJoinPoints(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), 0, "exa.instrument", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "exa.instrument.joinPoints.std.avg"));

                return json.toObject();
            }
        });
    }

    private Object selectInstrumentErrorsCount(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), 0, "exa.instrument", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "exa.instrument.errors.count.std.sum"));

                return json.toObject();
            }
        });
    }

    private void ensureNames() {
        if (messagingMetricId == 0) {
            IComponentVersion version = component.get();
            if (version != null) {
                JsonObject properties = version.getProperties();
                if (properties != null)
                    messagingMetricId = getMetricId("channels." + properties.get("node"));
            }
        }
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
