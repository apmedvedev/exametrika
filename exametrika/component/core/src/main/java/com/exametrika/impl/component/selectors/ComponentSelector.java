/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.selectors;

import java.util.Collections;
import java.util.Map;

import com.exametrika.api.aggregator.Location;
import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.nodes.IAggregationNode;
import com.exametrika.api.aggregator.nodes.INameNode;
import com.exametrika.api.component.ISelectionService;
import com.exametrika.api.component.ISelectionService.IRepresentationBuilder;
import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.api.component.nodes.IComponentVersion;
import com.exametrika.api.component.nodes.IHealthComponent;
import com.exametrika.api.component.nodes.IHealthComponentVersion;
import com.exametrika.api.component.nodes.IHealthComponentVersion.State;
import com.exametrika.api.component.schema.ISelectorSchema;
import com.exametrika.common.json.IJsonCollection;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.aggregator.IComponentAccessorFactory;
import com.exametrika.spi.aggregator.IComputeContext;
import com.exametrika.spi.component.Selectors;


/**
 * The {@link ComponentSelector} is a component selector.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class ComponentSelector extends Selector {
    private final String kpiComponentType;
    protected final ISelectionService selectionService;

    public ComponentSelector(IComponent component, ISelectorSchema schema, String kpiComponentType) {
        super(component, schema);

        Assert.notNull(kpiComponentType);

        this.kpiComponentType = kpiComponentType;
        selectionService = this.schema.getContext().getTransactionProvider().getTransaction().findDomainService(ISelectionService.NAME);
    }

    @Override
    protected Object doSelect(Map<String, ?> parameters) {
        String type = (String) parameters.get("type");
        if (type.equals("timeline"))
            return selectTimeline(parameters);
        else if (type.equals("kpi"))
            return selectKpi(parameters);
        else if (type.equals("properties"))
            return selectProperties(parameters);
        else if (type.equals("healthIndicators"))
            return selectHealthIndicators(parameters);
        else if (type.equals("availabilityTimes"))
            return selectAvailabilityTimes(parameters);
        else if (type.equals("health"))
            return selectHealth(parameters);
        else
            return Assert.error();
    }

    protected abstract JsonObjectBuilder doBuildKpiMetrics(long time, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                                           IComputeContext computeContext);

    protected String getHierarchicalProperty(String name) {
        return null;
    }

    protected JsonArray buildChildrenProperties(String prefix, IJsonCollection collection) {
        if (collection instanceof JsonArray) {
            JsonArray properties = (JsonArray) collection;
            Json json = Json.array();
            int i = 1;
            for (Object property : properties) {
                json.addObject()
                        .put("id", prefix + i)
                        .put("elementId", prefix + i)
                        .put("name", prefix + i)
                        .put("value", property);

                i++;
            }

            return json.toArray();
        } else {
            JsonObject properties = (JsonObject) collection;
            Json json = Json.array();
            for (Map.Entry<String, Object> entry : properties) {
                json.addObject()
                        .put("id", entry.getKey())
                        .put("elementId", entry.getKey())
                        .put("name", entry.getKey())
                        .put("value", entry.getValue());
            }

            return json.toArray();
        }
    }

    protected String getKpiComponentType(Map<String, ?> parameters) {
        if (parameters.containsKey("server"))
            return kpiComponentType + ".server";
        else
            return kpiComponentType;
    }

    private Object selectTimeline(Map<String, ?> parameters) {
        Assert.isTrue(Boolean.TRUE.equals(parameters.get("fullUpdate")));
        long start = ((Number) parameters.get("start")).longValue();
        long current = ((Number) parameters.get("current")).longValue();
        Json json = Json.object().put("fullUpdate", true);
        Json jsonRows = json.putArray("rows");

        IComponentVersion version = this.component.get();
        boolean first = true;
        while (version != null) {
            String stateId;
            if (version instanceof IHealthComponentVersion) {
                State state = ((IHealthComponentVersion) version).getState();
                switch (state) {
                    case CREATED:
                    case DELETED:
                        stateId = null;
                        break;
                    case HEALTH_ERROR:
                    case HEALTH_WARNING:
                    case NORMAL:
                        stateId = "normal";
                        break;
                    case UNAVAILABLE:
                        stateId = "error";
                        break;
                    case MAINTENANCE:
                        stateId = "maintenance";
                        break;
                    default:
                        return Assert.error();
                }
            } else
                stateId = "normal";

            long time = version.getTime();
            if (first) {
                first = false;

                jsonRows.addObject()
                        .put("time", current)
                        .putIf("s1", stateId, stateId != null);
            } else if (version.getTime() < start)
                time = start;

            jsonRows.addObject()
                    .put("time", time)
                    .putIf("s1", stateId, stateId != null);

            if (version.getTime() <= start)
                break;

            version = version.getPreviousVersion();
        }

        Collections.reverse(jsonRows.toArrayBuilder());
        return json.toObject();
    }

    private Object selectKpi(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), 0, getKpiComponentType(parameters), parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                JsonObjectBuilder builder = doBuildKpiMetrics(time, value, accessorFactory, computeContext);
                return builder.toJson();
            }
        });
    }

    private Object selectProperties(Map<String, ?> parameters) {
        Assert.isTrue(Boolean.TRUE.equals(parameters.get("fullUpdate")));
        Json json = Json.object().put("fullUpdate", true);
        Json jsonRows = json.putArray("rows");

        JsonObject properties = null;
        if (parameters.containsKey("server")) {
            String periodType = ((JsonObject) parameters.get("request")).select("timeScale.periodType");
            long currentTime = ((Number) parameters.get("current")).longValue();

            INameNode processNode = (INameNode) selectionService.findNearestAggregationNode(periodType, currentTime,
                    new Location(component.getScopeId(), 0), getKpiComponentType(parameters));
            if (processNode != null)
                properties = processNode.getMetadata();
        } else {
            IComponentVersion version = component.get();
            if (version != null)
                properties = version.getProperties();
        }

        if (properties != null) {
            for (Map.Entry<String, Object> entry : properties) {
                String name = entry.getKey();
                if (name.startsWith("node"))
                    continue;

                String hierarchicalProperty = getHierarchicalProperty(name);
                if (hierarchicalProperty != null) {
                    JsonArray children = buildChildrenProperties(hierarchicalProperty, (IJsonCollection) entry.getValue());
                    jsonRows.addObject()
                            .put("id", name)
                            .put("elementId", name)
                            .put("name", name)
                            .put("children", children);
                } else
                    jsonRows.addObject()
                            .put("id", name)
                            .put("elementId", name)
                            .put("name", name)
                            .put("value", entry.getValue());
            }
        }

        return json.toObject();
    }

    private Object selectHealthIndicators(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), 0, getKpiComponentType(parameters), parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("s2", Selectors.getMetric(value, accessorFactory, computeContext, "anomalyIndex"))
                        .put("s3", Selectors.getMetric(value, accessorFactory, computeContext, "workloadIndex"))
                        .put("s4", Selectors.getMetric(value, accessorFactory, computeContext, "errorsIndex"))
                        .put("s5", Selectors.getMetric(value, accessorFactory, computeContext, "healthIndex"));

                return json.toObjectBuilder();
            }
        });
    }

    private Object selectAvailabilityTimes(Map<String, ?> parameters) {
        return Selectors.selectTimedModel(selectionService, component.getScopeId(), 0, "HealthComponentType", parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                Json json = Json.object();
                json.put("time", time)
                        .put("y1", Selectors.getMetric(value, accessorFactory, computeContext, "upCounter.%up", 0))
                        .put("y2", Selectors.getMetric(value, accessorFactory, computeContext, "downCounter.%down", 0))
                        .put("y3", Selectors.getMetric(value, accessorFactory, computeContext, "failureCounter.%failure", 0))
                        .put("y4", Selectors.getMetric(value, accessorFactory, computeContext, "maintenanceCounter.%maintenance", 0))
                        .put("y5", Selectors.getMetric(value, accessorFactory, computeContext, "upCounter.std.sum", 0))
                        .put("y6", Selectors.getMetric(value, accessorFactory, computeContext, "downCounter.std.sum", 0))
                        .put("y7", Selectors.getMetric(value, accessorFactory, computeContext, "failureCounter.std.sum", 0))
                        .put("y8", Selectors.getMetric(value, accessorFactory, computeContext, "maintenanceCounter.std.sum", 0));

                return json.toObjectBuilder();
            }
        });
    }

    private Object selectHealth(Map<String, ?> parameters) {
        Assert.isTrue(Boolean.TRUE.equals(parameters.get("fullUpdate")));
        String periodType = ((JsonObject) parameters.get("request")).select("timeScale.periodType");
        long currentTime = ((Number) parameters.get("current")).longValue();

        Json json = Json.object().put("fullUpdate", true);
        Json jsonRows = json.putArray("rows");

        final State state;
        if (component instanceof IHealthComponent) {
            IComponentVersion version = component.get();
            if (version != null)
                state = ((IHealthComponentVersion) version).getState();
            else
                state = null;

            if (state == null || state == State.CREATED || state == State.DELETED)
                return json.toObject();
        } else
            state = State.NORMAL;

        final Object[] indexes = new Object[4];
        selectionService.buildRepresentation(periodType, currentTime,
                new Location(component.getScopeId(), 0), getKpiComponentType(parameters), 0, new IRepresentationBuilder() {
                    @Override
                    public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                        IComputeContext computeContext) {
                        if (state == State.NORMAL || state == State.HEALTH_WARNING || state == State.HEALTH_ERROR) {
                            indexes[0] = Selectors.getMetric(value, accessorFactory, computeContext, "anomalyIndex.causes");
                            indexes[1] = Selectors.getMetric(value, accessorFactory, computeContext, "workloadIndex.causes");
                            ;
                            indexes[2] = Selectors.getMetric(value, accessorFactory, computeContext, "errorsIndex.causes");
                            indexes[3] = Selectors.getMetric(value, accessorFactory, computeContext, "healthIndex");
                        }

                        return null;
                    }
                });

        String availabilityIndex = "normal", healthIndex = "normal";
        JsonObject unknownIndex = Json.object().put("state", "unknown").toObject();
        JsonObject anomalyIndex = unknownIndex, workloadIndex = unknownIndex, errorsIndex = unknownIndex;
        switch (state) {
            case UNAVAILABLE:
                availabilityIndex = "error";
                healthIndex = "error";
                break;
            case MAINTENANCE:
                availabilityIndex = "maintenance";
                healthIndex = "maintenance";
                break;
            default:
                if (indexes[0] != null)
                    anomalyIndex = (JsonObject) indexes[0];
                if (indexes[1] != null)
                    workloadIndex = (JsonObject) indexes[1];
                if (indexes[2] != null)
                    errorsIndex = (JsonObject) indexes[2];
                if (indexes[3] != null)
                    healthIndex = (String) indexes[3];
        }

        jsonRows.add(Json.object()
                .putObject("availability")
                .put("state", availabilityIndex)
                .end()
                .put("anomaly", anomalyIndex)
                .put("workload", workloadIndex)
                .put("errors", errorsIndex)
                .putObject("health")
                .put("state", healthIndex)
                .end()
                .toObjectBuilder());

        return json.toObject();
    }
}
