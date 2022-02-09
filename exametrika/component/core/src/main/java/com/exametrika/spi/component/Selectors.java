/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.component;

import java.util.Collections;
import java.util.Map;
import java.util.SortedSet;

import com.exametrika.api.aggregator.IPeriodName;
import com.exametrika.api.aggregator.IPeriodNameManager;
import com.exametrika.api.aggregator.Location;
import com.exametrika.api.aggregator.common.model.ICallPath;
import com.exametrika.api.aggregator.common.model.MeasurementId;
import com.exametrika.api.aggregator.common.model.NameMeasurementId;
import com.exametrika.api.aggregator.common.model.Names;
import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.common.values.IInstanceRecord;
import com.exametrika.api.aggregator.fields.IAggregationField;
import com.exametrika.api.aggregator.fields.IAggregationRecord;
import com.exametrika.api.aggregator.nodes.IAggregationNode;
import com.exametrika.api.component.IComponentService;
import com.exametrika.api.component.ISelectionService;
import com.exametrika.api.component.ISelectionService.IRepresentationBuilder;
import com.exametrika.api.component.ISelectionService.ISelectionIterator;
import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.api.component.nodes.IComponentVersion;
import com.exametrika.api.component.nodes.IGroupComponent;
import com.exametrika.api.component.nodes.IGroupComponentVersion;
import com.exametrika.api.component.nodes.IHealthComponentVersion;
import com.exametrika.api.component.nodes.IHealthComponentVersion.State;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonArrayBuilder;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Pair;
import com.exametrika.impl.component.nodes.ComponentNode;
import com.exametrika.impl.component.nodes.ComponentVersionNode;
import com.exametrika.impl.component.nodes.GroupComponentNode;
import com.exametrika.impl.component.nodes.GroupComponentVersionNode;
import com.exametrika.impl.component.nodes.HealthComponentVersionNode;
import com.exametrika.spi.aggregator.IComponentAccessor;
import com.exametrika.spi.aggregator.IComponentAccessorFactory;
import com.exametrika.spi.aggregator.IComputeContext;


/**
 * The {@link Selectors} different utility methods for selectors manipulation.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class Selectors {
    private static int COUNTER;

    public enum ComponentState {
        NORMAL,
        MAINTENANCE,
        WARNING,
        ERROR
    }

    public static class StateInfo {
        public JsonArray state;
        public String stateTitle;
        public ComponentState componentState;
    }

    public interface IInstanceContextBuilder {
        void build(JsonObject instance, Json result);
    }

    public static JsonObjectBuilder buildReference(IComponentVersion version) {
        ComponentNode component = (ComponentNode) version.getComponent();
        String componentTypeName = component.getSchema().getConfiguration().getComponent().getName();
        return Json.object()
                .put("title", version.getTitle())
                .put("description", version.getDescription())
                .put("link", "#components/" + componentTypeName.toLowerCase() + "/default/" + component.getScopeId()).toObjectBuilder();
    }

    public static JsonObjectBuilder selectReference(IComponentService componentService, String scopeName) {
        IComponent component = componentService.findComponent(scopeName);
        if (component == null)
            return null;
        ComponentVersionNode version = (ComponentVersionNode) component.get();
        if (version == null)
            return null;

        StateInfo info = Selectors.buildState(version, true, null, null, 0, null);
        JsonObjectBuilder reference = Selectors.buildReference(version);
        reference.put("state", info.state);
        return reference;
    }

    public static JsonObjectBuilder selectReference(IComponentService componentService, long scopeId) {
        IComponent component = componentService.findComponent(scopeId);
        if (component == null)
            return null;
        ComponentVersionNode version = (ComponentVersionNode) component.get();
        if (version == null)
            return null;

        StateInfo info = Selectors.buildState(version, true, null, null, 0, null);
        JsonObjectBuilder reference = Selectors.buildReference(version);
        reference.put("state", info.state);
        return reference;
    }

    public static StateInfo buildState(IComponentVersion version, boolean full, ISelectionService selectionService,
                                       String periodType, long currentTime, String componentType) {
        IHealthComponentVersion.State state = State.NORMAL;
        if (version instanceof IHealthComponentVersion) {
            state = ((IHealthComponentVersion) version).getState();
            if ((state == State.HEALTH_ERROR || state == State.HEALTH_WARNING || state == State.NORMAL) && selectionService != null) {
                String healthIndex = (String) selectionService.buildRepresentation(periodType, currentTime,
                        new Location(version.getComponent().getScopeId(), 0), componentType, 0, new IRepresentationBuilder() {
                            @Override
                            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value,
                                                IComponentAccessorFactory accessorFactory, IComputeContext computeContext) {
                                return Selectors.getMetric(value, accessorFactory, computeContext, "healthIndex");
                            }
                        });
                if (healthIndex == null)
                    state = State.NORMAL;
                else if (healthIndex.equals("error"))
                    state = State.HEALTH_ERROR;
                else if (healthIndex.equals("warning"))
                    state = State.HEALTH_WARNING;
                else
                    state = State.NORMAL;
            }
        }

        if (version instanceof GroupComponentVersionNode) {
            GroupComponentVersionNode groupVersion = (GroupComponentVersionNode) version;
            GroupComponentNode component = (GroupComponentNode) version.getComponent();
            Json states;
            if (full) {
                String componentTypeName = component.getSchema().getConfiguration().getComponent().getName();
                states = Json.array();
                states.add(componentTypeName);
            } else
                states = null;
            ComponentState componentState;
            String stateTitle = "normal";
            if (state == State.UNAVAILABLE || state == State.DELETED) {
                if (full) {
                    states.add("groupError");
                    stateTitle = "unavailable";
                }
                componentState = ComponentState.ERROR;
            } else if (state == State.HEALTH_ERROR) {
                if (full) {
                    states.add("groupError");
                    stateTitle = "failed";
                }
                componentState = ComponentState.ERROR;
            } else if (state == State.HEALTH_WARNING) {
                if (full) {
                    states.add("groupWarning");
                    stateTitle = "warning";
                }
                componentState = ComponentState.WARNING;
            } else if (state == State.MAINTENANCE) {
                if (full) {
                    states.add("groupMaintenance");
                    stateTitle = "maintenance";
                }
                componentState = ComponentState.MAINTENANCE;
            } else {
                if (full) {
                    states.add("group");
                    stateTitle = "normal";
                }
                componentState = ComponentState.NORMAL;
            }

            ComponentState childrenComponentState = ComponentState.NORMAL;
            for (IGroupComponent child : groupVersion.getChildren()) {
                GroupComponentVersionNode childVersion = (GroupComponentVersionNode) child.get();
                if (childVersion != null) {
                    ComponentState childComponentState = buildState(childVersion, false, selectionService, periodType,
                            currentTime, componentType).componentState;
                    if (childComponentState.ordinal() > childrenComponentState.ordinal())
                        childrenComponentState = childComponentState;
                }
            }
            for (IComponent child : groupVersion.getComponents()) {
                HealthComponentVersionNode childVersion = (HealthComponentVersionNode) child.get();
                if (childVersion != null) {
                    ComponentState childComponentState = buildState(childVersion, false, selectionService, periodType,
                            currentTime, componentType).componentState;
                    if (childComponentState.ordinal() > childrenComponentState.ordinal())
                        childrenComponentState = childComponentState;
                }
            }

            if (full) {
                if (childrenComponentState == ComponentState.ERROR)
                    states.add("childError");
                else if (childrenComponentState == ComponentState.WARNING)
                    states.add("childWarning");
                else if (childrenComponentState == ComponentState.MAINTENANCE)
                    states.add("childMaintenance");
            }

            if (childrenComponentState.ordinal() > componentState.ordinal())
                componentState = childrenComponentState;

            StateInfo info = new StateInfo();
            if (full) {
                info.state = states.toArray();
                info.stateTitle = stateTitle;
            }
            info.componentState = componentState;
            return info;
        } else {
            ComponentNode component = (ComponentNode) version.getComponent();


            Json states;
            if (full) {
                states = Json.array();
                String componentTypeName = component.getSchema().getConfiguration().getComponent().getName();
                states.add(componentTypeName);
            } else
                states = null;

            ComponentState componentState = ComponentState.NORMAL;
            String stateTitle = "normal";

            if (version instanceof HealthComponentVersionNode) {
                if (state == State.UNAVAILABLE || state == State.DELETED) {
                    if (full) {
                        states.add("unavailable");
                        stateTitle = "unavailable";
                    }
                    componentState = ComponentState.ERROR;
                } else if (state == State.MAINTENANCE) {
                    if (full) {
                        states.add("maintenance");
                        stateTitle = "maintenance";
                    }
                    componentState = ComponentState.MAINTENANCE;
                } else if (state == State.HEALTH_WARNING) {
                    if (full) {
                        states.add("healthWarning");
                        stateTitle = "warning";
                    }
                    componentState = ComponentState.WARNING;
                } else if (state == State.HEALTH_ERROR) {
                    if (full) {
                        states.add("healthError");
                        stateTitle = "failed";
                    }
                    componentState = ComponentState.ERROR;
                } else {
                    if (full) {
                        states.add("normal");
                        stateTitle = "normal";
                    }
                    componentState = ComponentState.NORMAL;
                }
            } else {
                if (full)
                    states.add("normal");
            }

            StateInfo info = new StateInfo();
            if (full) {
                info.state = states.toArray();
                info.stateTitle = stateTitle;
            }
            info.componentState = componentState;
            return info;
        }
    }

    public static <T> T getMetric(IComponentValue value, IComponentAccessorFactory accessorFactory, IComputeContext computeContext, String metric) {
        IComponentAccessor accessor = accessorFactory.createAccessor(null, null, metric);
        if (accessor != null)
            return (T) accessor.get(value, computeContext);
        else
            return null;
    }

    public static <T> T getMetric(IComponentValue value, IComponentAccessorFactory accessorFactory, IComputeContext computeContext, String metric,
                                  T defaultValue) {
        T result = getMetric(value, accessorFactory, computeContext, metric);
        if (result != null)
            return result;
        else
            return defaultValue;
    }

    public static void buildRelativeMetric(IComponentValue value, IComponentAccessorFactory accessorFactory,
                                           IComputeContext computeContext, String relativeMetric, String absoluteMetric,
                                           String stateMetric, String outMetricName, Json result) {
        buildRelativeMetric(value, accessorFactory, computeContext, relativeMetric, absoluteMetric, null,
                null, null, stateMetric, outMetricName, result);
    }

    public static void buildRelativeMetric(IComponentValue value, IComponentAccessorFactory accessorFactory,
                                           IComputeContext computeContext, String relativeMetric, String absoluteMetric, String minMetric, String maxMetric,
                                           String avgMetric, String stateMetric, String outMetricName, Json result) {
        Number relative = null;
        if (relativeMetric != null)
            relative = getMetric(value, accessorFactory, computeContext, relativeMetric);

        Number absolute = null;
        if (absoluteMetric != null)
            absolute = getMetric(value, accessorFactory, computeContext, absoluteMetric);

        Number min = null;
        if (minMetric != null)
            min = getMetric(value, accessorFactory, computeContext, minMetric);
        Number max = null;
        if (maxMetric != null)
            max = getMetric(value, accessorFactory, computeContext, maxMetric);
        Number avg = null;
        if (avgMetric != null)
            avg = getMetric(value, accessorFactory, computeContext, avgMetric);

        String state = null;
        if (stateMetric != null)
            state = getMetric(value, accessorFactory, computeContext, stateMetric);
        if (state == null)
            state = "normal";

        if (relative != null || absolute != null)
            result.putObject(outMetricName)
                    .putIf("relative", relative != null ? relative.doubleValue() : 0, relative != null)
                    .putIf("absolute", absolute != null ? absolute.doubleValue() : 0, absolute != null)
                    .putIf("min", min != null ? min.doubleValue() : 0, min != null)
                    .putIf("max", max != null ? max.doubleValue() : 0, max != null)
                    .putIf("avg", avg != null ? avg.doubleValue() : 0, avg != null)
                    .put("state", state);
    }

    public static void buildRateMetric(IComponentValue value, IComponentAccessorFactory accessorFactory,
                                       IComputeContext computeContext, String rateMetric, String sumMetric,
                                       String stateMetric, String outMetricName, Json result) {
        buildRateMetric(value, accessorFactory, computeContext, rateMetric, sumMetric, null, null, null, stateMetric, outMetricName, result);
    }

    public static Json buildRateMetric(IComponentValue value, IComponentAccessorFactory accessorFactory,
                                       IComputeContext computeContext, String rateMetric, String sumMetric, String minMetric, String maxMetric,
                                       String avgMetric, String stateMetric, String outMetricName, Json result) {
        Number rate = null;
        if (rateMetric != null)
            rate = getMetric(value, accessorFactory, computeContext, rateMetric);

        Number sum = null;
        if (sumMetric != null)
            sum = getMetric(value, accessorFactory, computeContext, sumMetric);

        Number min = null;
        if (minMetric != null)
            min = getMetric(value, accessorFactory, computeContext, minMetric);
        Number max = null;
        if (maxMetric != null)
            max = getMetric(value, accessorFactory, computeContext, maxMetric);
        Number avg = null;
        if (avgMetric != null)
            avg = getMetric(value, accessorFactory, computeContext, avgMetric);

        String state = null;
        if (stateMetric != null)
            state = getMetric(value, accessorFactory, computeContext, stateMetric);
        if (state == null)
            state = "normal";

        if (rate != null && sum != null)
            return result.putObject(outMetricName)
                    .put("rate", rate.doubleValue())
                    .put("sum", sum.doubleValue())
                    .putIf("min", min != null ? min.doubleValue() : 0, min != null)
                    .putIf("max", max != null ? max.doubleValue() : 0, max != null)
                    .putIf("avg", avg != null ? avg.doubleValue() : 0, avg != null)
                    .put("state", state)
                    .end();
        else
            return null;
    }

    public static void checkAnomaly(IComponentValue value, IComponentAccessorFactory accessorFactory,
                                    IComputeContext computeContext, String metricType, String metric, int seriesIndex, boolean forecast, Json annotations) {
        checkAnomaly(value, accessorFactory, computeContext, metricType, metric, seriesIndex, forecast, null, null, null, annotations);
    }

    public static void checkAnomaly(IComponentValue value, IComponentAccessorFactory accessorFactory,
                                    IComputeContext computeContext, String metricType, String metric, int seriesIndex, boolean forecast, String subType,
                                    Integer y0, Integer y1, Json annotations) {
        String anomalyLevel = (String) Selectors.getMetric(value, accessorFactory, computeContext, metricType + "." +
                (forecast ? "forecast" : "anomaly") + "(" + metric + ").level");
        if (anomalyLevel == null || anomalyLevel.equals("normal"))
            return;

        annotations.addObject()
                .put("name", "a" + seriesIndex)
                .put("seriesName", "y" + seriesIndex)
                .put("type", anomalyLevel.equals("error") ? "anomalyError" : "anomalyWarning")
                .putIf("subType", subType, subType != null)
                .putIf("y0%", y0, y0 != null)
                .putIf("y1%", y1, y1 != null);
    }

    public static Object selectTimedModel(ISelectionService selectionService, long scopeId, long metricId, String componentType,
                                          Map<String, ?> parameters, IRepresentationBuilder builder) {
        String periodType = ((JsonObject) parameters.get("request")).select("timeScale.periodType");

        if (Boolean.TRUE.equals(parameters.get("fullUpdate"))) {
            long start = ((Number) parameters.get("start")).longValue();
            long current = ((Number) parameters.get("current")).longValue();

            Json json = Json.object().put("fullUpdate", true);
            Json jsonRows = json.putArray("rows");

            selectTimed(selectionService, scopeId, metricId, componentType, builder, periodType, start, true, current, true, jsonRows);

            if (Boolean.TRUE.equals(parameters.get("allowIncrementalUpdate"))) {
                long preloadStart = ((Number) parameters.get("preloadStart")).longValue();
                long preloadEnd = ((Number) parameters.get("preloadEnd")).longValue();

                selectTimed(selectionService, scopeId, metricId, componentType, builder, periodType, preloadStart, true, start, false, json.putArray("beforePreload"));
                selectTimed(selectionService, scopeId, metricId, componentType, builder, periodType, current, false, preloadEnd, true, json.putArray("afterPreload"));
            }

            return json.toObject();
        } else {
            Assert.isTrue(Boolean.TRUE.equals(parameters.get("incrementalTimedUpdate")));
            Json json = Json.object().put("incrementalTimedUpdate", true);

            JsonObject beforePreload = (JsonObject) parameters.get("beforePreload");
            if (beforePreload != null) {
                long preloadStart = ((Number) beforePreload.get("start")).longValue();
                long preloadEnd = ((Number) beforePreload.get("end")).longValue();

                selectTimed(selectionService, scopeId, metricId, componentType, builder, periodType, preloadStart, true, preloadEnd, true, json.putArray("beforePreload"));
            }

            JsonObject afterPreload = (JsonObject) parameters.get("afterPreload");
            if (afterPreload != null) {
                long preloadStart = ((Number) afterPreload.get("start")).longValue();
                long preloadEnd = ((Number) afterPreload.get("end")).longValue();

                selectTimed(selectionService, scopeId, metricId, componentType, builder, periodType, preloadStart, true, preloadEnd, true, json.putArray("afterPreload"));
            }

            return json.toObject();
        }
    }

    public static Object selectHistogram(long scopeId, ISelectionService selectionService, String componentType,
                                         final String metricType, long metricId, Map<String, ?> parameters) {
        Assert.isTrue(Boolean.TRUE.equals(parameters.get("fullUpdate")));
        String periodType = ((JsonObject) parameters.get("request")).select("timeScale.periodType");
        long currentTime = ((Number) parameters.get("current")).longValue();
        if (parameters.containsKey("subScopeAsMetricId"))
            metricId = (Long) parameters.get("subScopeId");
        else
            scopeId = parameters.containsKey("subScopeId") ? (Long) parameters.get("subScopeId") : scopeId;

        Json json = Json.object().put("fullUpdate", true);
        final Json jsonRows = json.putArray("rows");

        selectionService.buildRepresentation(periodType, currentTime,
                new Location(scopeId, metricId), componentType, 0, new IRepresentationBuilder() {
                    @Override
                    public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                        IComputeContext computeContext) {
                        JsonArray bins = Selectors.getMetric(value, accessorFactory, computeContext, metricType + ".histo.bins");
                        if (bins == null)
                            return null;

                        JsonArray scale = Selectors.getMetric(value, accessorFactory, computeContext, metricType + ".histo.scale");
                        JsonArrayBuilder builder = new JsonArrayBuilder();
                        for (int i = 0; i < bins.size(); i++)
                            builder.add(Json.object().put("value", scale.get(i)).put("count", bins.get(i)).toObject());
                        long minOob = Selectors.getMetric(value, accessorFactory, computeContext, metricType + ".histo.min-oob");
                        long maxOob = Selectors.getMetric(value, accessorFactory, computeContext, metricType + ".histo.max-oob");
                        JsonObject percentile25 = Selectors.getMetric(value, accessorFactory, computeContext, metricType + ".histo.percentile(25)");
                        JsonObject percentile50 = Selectors.getMetric(value, accessorFactory, computeContext, metricType + ".histo.percentile(50)");
                        JsonObject percentile75 = Selectors.getMetric(value, accessorFactory, computeContext, metricType + ".histo.percentile(75)");

                        Json result = jsonRows.addObject();
                        result.put("bins", builder.toArray())
                                .put("minOutOfBand", minOob)
                                .put("maxOutOfBand", maxOob);

                        Json annotations = result.putArray("annotations");
                        if (percentile25 != null)
                            annotations.addObject().put("name", "p25").put("value", 25).put("type", "percentile").put("xBin", percentile25.get("bin"));
                        if (percentile50 != null)
                            annotations.addObject().put("name", "p50").put("value", 50).put("type", "percentile").put("xBin", percentile50.get("bin"));
                        if (percentile75 != null)
                            annotations.addObject().put("name", "p75").put("value", 75).put("type", "percentile").put("xBin", percentile75.get("bin"));

                        return null;
                    }
                });

        return json.toObject();
    }

    public static Object selectInstances(long scopeId, ISelectionService selectionService, final IComponentService componentService,
                                         final IPeriodNameManager nameManager, String componentType, final String metricType, long metricId,
                                         Map<String, ?> parameters, final IInstanceContextBuilder builder) {
        Assert.isTrue(Boolean.TRUE.equals(parameters.get("fullUpdate")));
        String periodType = ((JsonObject) parameters.get("request")).select("timeScale.periodType");
        long currentTime = ((Number) parameters.get("current")).longValue();

        Json json = Json.object().put("fullUpdate", true);
        final Json jsonRows = json.putArray("rows");

        selectionService.buildRepresentation(periodType, currentTime,
                new Location(scopeId, metricId), componentType, 0, new IRepresentationBuilder() {
                    @Override
                    public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                        IComputeContext computeContext) {
                        SortedSet<IInstanceRecord> instances = Selectors.getMetric(value, accessorFactory, computeContext, metricType + ".instance");
                        if (instances == null)
                            return null;

                        int i = 0;
                        for (IInstanceRecord record : instances) {
                            String scope, location;
                            if (record.getId() instanceof NameMeasurementId) {
                                NameMeasurementId id = (NameMeasurementId) record.getId();
                                scope = id.getScope().toString();
                                location = id.getLocation().toString();
                            } else {
                                MeasurementId id = (MeasurementId) record.getId();

                                IPeriodName scopeName = nameManager.findById(id.getScopeId());
                                if (scopeName != null)
                                    scope = scopeName.getName().toString();
                                else
                                    scope = "";

                                IPeriodName locationName = nameManager.findById(id.getLocationId());
                                if (locationName != null)
                                    location = locationName.getName().toString();
                                else
                                    location = "";
                            }

                            scope = Names.unescape(scope);
                            location = Names.unescape(location);

                            int pos = location.lastIndexOf(ICallPath.SEPARATOR);
                            if (pos != -1)
                                location = location.substring(pos + 1);

                            location = Selectors.removePrefix(location);

                            Json jsonRow = jsonRows.addObject();
                            jsonRow.put("id", i)
                                    .put("elementId", i)
                                    .put("name", location)
                                    .put("scope", selectReference(componentService, scope))
                                    .put("time", record.getTime())
                                    .put("value", record.getValue())
                            ;

                            builder.build(record.getContext(), jsonRow);
                            i++;
                        }

                        return null;
                    }
                });

        return json.toObject();
    }

    public static String removePrefix(String name) {
        if (name.startsWith("threshold:") || name.startsWith("hotspot:") || name.startsWith("app:")) {
            int pos = name.indexOf(":");
            name = name.substring(pos + 1);
        } else if (name.startsWith("db:"))
            name = "All queries";

        return name;
    }

    public static Object selectLog(long scopeId, ISelectionService selectionService, String componentType,
                                   long metricId, Map<String, ?> parameters, IRepresentationBuilder builder) {
        String periodType = ((JsonObject) parameters.get("request")).select("timeScale.periodType");
        Assert.isTrue(Boolean.TRUE.equals(parameters.get("fullUpdate")));
        long start = ((Number) parameters.get("start")).longValue();
        long current = ((Number) parameters.get("current")).longValue();

        ISelectionService.PageInfo pageInfo = null;
        JsonObject element = (JsonObject) parameters.get("pageInfo");
        if (element != null) {
            pageInfo = new ISelectionService.PageInfo(periodType, ((Number) element.get("startTime")).longValue(),
                    ((Number) element.get("currentTime")).longValue(), (String) element.get("firstCycleId"),
                    ((Number) element.get("firstRecordId")).longValue(), (String) element.get("lastCycleId"),
                    ((Number) element.get("lastRecordId")).longValue(),
                    ISelectionService.PageDirection.valueOf(((String) element.get("direction")).toUpperCase()),
                    (Boolean) element.get("firstPage"), (Boolean) element.get("lastPage"), ((Number) element.get("pageIndex")).intValue());
        }

        Json json = Json.object().put("fullUpdate", true);

        Pair<JsonArray, ISelectionService.PageInfo> pair = selectionService.buildPageRecords(periodType, start, current,
                new Location(scopeId, metricId), componentType, pageInfo, 100, 0, builder);

        pageInfo = pair.getValue();
        json.put("rows", pair.getKey());

        if (pageInfo != null)
            json.putObject("queryResult")
                    .put("periodType", pageInfo.periodType)
                    .put("startTime", pageInfo.startTime)
                    .put("currentTime", pageInfo.currentTime)
                    .put("firstCycleId", pageInfo.firstCycleId)
                    .put("firstRecordId", pageInfo.firstRecordId)
                    .put("lastCycleId", pageInfo.lastCycleId)
                    .put("lastRecordId", pageInfo.lastRecordId)
                    .put("direction", pageInfo.direction.toString().toLowerCase())
                    .put("firstPage", pageInfo.firstPage)
                    .put("lastPage", pageInfo.lastPage)
                    .put("pageIndex", pageInfo.pageIndex);

        return json.toObject();
    }

    public static Object selectLog(long scopeId, ISelectionService selectionService, String componentType,
                                   final String metricType, long metricId, Map<String, ?> parameters) {
        return Selectors.selectLog(scopeId, selectionService, componentType, metricId, parameters, new IRepresentationBuilder() {
            @Override
            public Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                IComputeContext computeContext) {
                JsonObject logEvent = Selectors.getMetric(value, accessorFactory, computeContext, metricType);
                if (logEvent != null) {
                    Object exception = logEvent.get("exception", null);
                    if (exception != null)
                        exception = exception.toString();

                    String id = time + ":" + COUNTER++;
                    Json json = Json.object();
                    json.put("time", logEvent.get("time"))
                            .put("id", id)
                            .put("elementId", id)
                            .put("level", logEvent.get("level", null))
                            .put("logger", logEvent.get("logger", null))
                            .put("thread", logEvent.get("thread", null))
                            .put("message", logEvent.get("message", null))
                            .put("transactionId", logEvent.get("transactionId", null))
                            .put("exception", exception);

                    return json.toObject();
                } else
                    return null;
            }
        });
    }

    public static IComponent selectServer(IGroupComponent rootGroup) {
        IGroupComponentVersion rootGroupVersion = (IGroupComponentVersion) rootGroup.get();
        IGroupComponent rootServerGroup = null;
        if (rootGroupVersion != null) {
            for (IGroupComponent child : rootGroupVersion.getChildren()) {
                if (child.getScope().toString().equals("servers")) {
                    rootServerGroup = child;
                    break;
                }
            }
        }
        if (rootServerGroup != null) {
            IGroupComponentVersion rootServerGroupVersion = (IGroupComponentVersion) rootServerGroup.get();
            if (rootServerGroupVersion != null) {
                for (IComponent child : rootServerGroupVersion.getComponents())
                    return child;
            }
        }
        return null;
    }

    public static Object buildRepresentation(IAggregationNode node, int index, IRepresentationBuilder builder) {
        IAggregationField field = node.getAggregationField();
        IComputeContext computeContext = field.getComputeContext();
        return builder.build(computeContext.getTime(), node, field.getValue(false),
                field.getSchema().getRepresentations().get(index).getAccessorFactory(), computeContext);
    }

    private static void selectTimed(ISelectionService selectionService, long scopeId, long metricId, String componentType,
                                    IRepresentationBuilder builder, String periodType, long start, boolean includeStart, long end, boolean includeEnd, Json json) {
        IComponentAccessorFactory accessorFactory = null;
        for (ISelectionIterator it = selectionService.getAggregationRecords(periodType, end,
                new Location(scopeId, metricId), componentType).iterator(); it.hasNext(); ) {
            IAggregationRecord record = it.next();
            if (!includeEnd && record.getTime() == end)
                continue;
            if (record.getTime() < start || (!includeStart && record.getTime() == start))
                break;

            if (accessorFactory == null)
                accessorFactory = it.getField().getSchema().getRepresentations().get(0).getAccessorFactory();

            IComputeContext computeContext = it.getComputeContext();
            Object representation = builder.build(record.getTime(), (IAggregationNode) it.getField().getNode().getObject(),
                    record.getValue(), accessorFactory, computeContext);
            if (representation != null)
                json.add(representation);
        }

        Collections.reverse(json.toArrayBuilder());
    }

    private Selectors() {
    }
}
