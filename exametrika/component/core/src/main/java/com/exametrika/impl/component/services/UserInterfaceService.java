/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.services;

import java.util.HashMap;
import java.util.Map;

import com.exametrika.api.aggregator.IPeriodCycle;
import com.exametrika.api.aggregator.config.model.PeriodTypeSchemaConfiguration;
import com.exametrika.api.aggregator.schema.ICycleSchema;
import com.exametrika.api.aggregator.schema.IPeriodSpaceSchema;
import com.exametrika.api.component.IComponentService;
import com.exametrika.api.component.ISelector;
import com.exametrika.api.component.IUserInterfaceService;
import com.exametrika.api.component.config.model.UserInterfaceComponentSchemaConfiguration;
import com.exametrika.api.component.config.model.UserInterfaceSchemaConfiguration;
import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.api.component.nodes.IGroupComponent;
import com.exametrika.api.exadb.core.IOperationWrapper;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonArrayBuilder;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Times;
import com.exametrika.impl.component.schema.UserInterfaceServiceSchema;
import com.exametrika.spi.component.ITimeSnapshotOperation;
import com.exametrika.spi.component.Selectors;
import com.exametrika.spi.exadb.core.DomainService;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link UserInterfaceService} is a user interface service implementation.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public final class UserInterfaceService extends DomainService implements IUserInterfaceService {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(UserInterfaceService.class);
    private IPeriodSpaceSchema spaceSchema;
    private IComponentService componentService;

    @Override
    public UserInterfaceServiceSchema getSchema() {
        return (UserInterfaceServiceSchema) schema;
    }

    @Override
    public JsonObject getUserInterfaceSchema() {
        UserInterfaceSchemaConfiguration userInterface = getSchema().getComponentModel().getUserInterface();
        JsonArrayBuilder timeScale = new JsonArrayBuilder();
        for (PeriodTypeSchemaConfiguration period : userInterface.getPeriods()) {
            long[] bounds = getPeriodBounds(period);
            timeScale.add(Json.object()
                    .put("name", period.getName())
                    .put("type", !period.isNonAggregating() ? period.getPeriod().getType().toString().toLowerCase() : "second")
                    .put("amount", !period.isNonAggregating() ? (long) period.getPeriod().getAmount() : 1)
                    .put("cyclePeriodCount", !period.isNonAggregating() ? (long) period.getCyclePeriodCount() : 3600)
                    .put("start", bounds != null ? bounds[0] : null)
                    .put("end", bounds != null ? bounds[1] : null)
                    .putIf("nonAggregating", true, period.isNonAggregating())
                    .toObject());
        }

        JsonObjectBuilder components = new JsonObjectBuilder();
        for (UserInterfaceComponentSchemaConfiguration component : userInterface.getComponents().values()) {
            JsonArrayBuilder views = new JsonArrayBuilder();
            for (Map.Entry<String, Object> entry : component.getViews()) {
                JsonObjectBuilder view = buildView("components." + component.getName().toLowerCase() + "." + entry.getKey(),
                        (JsonObject) entry.getValue());
                view.put("name", entry.getKey());
                views.add(view);
            }
            for (int i = 0; i < views.size(); i++) {
                JsonObject view = (JsonObject) views.get(i);
                if (view.get("name").equals(component.getDefaultView())) {
                    views.remove(i);
                    views.add(0, view);
                    break;
                }
            }
            components.put(component.getName().toLowerCase(), views);
        }

        JsonObject userInterfaceSchema = Json.object()
                .put("models", userInterface.getModels())
                .put("navBar", buildView("navBar", userInterface.getNavBar()))
                .put("notifications", userInterface.getNotifications())
                .put("timeScale", timeScale.toJson())
                .put("components", components.toJson())
                .put("views", buildViews("views", userInterface.getViews()))
                .toObject();

        return userInterfaceSchema;
    }

    @Override
    public JsonObject processUpdateRequest(JsonObject request) {
        long currentTime = Times.getCurrentTime();
        JsonObjectBuilder response = new JsonObjectBuilder();

        UserInterfaceSchemaConfiguration userInterfaceSchema = ((UserInterfaceServiceSchema) schema).getComponentModel().getUserInterface();
        Json timeScale = Json.object();
        for (PeriodTypeSchemaConfiguration period : userInterfaceSchema.getPeriods()) {
            long[] bounds = getPeriodBounds(period);
            if (bounds == null)
                continue;

            timeScale.putObject(period.getName())
                    .put("start", bounds[0])
                    .put("end", bounds[1]);
        }

        response.put("timeScale", timeScale.toObject());

        if (request.contains("notificationModels")) {
            setSnapshotTime(currentTime);
            processNotificationModels(request, response, currentTime);
        }
        if (request.contains("models"))
            processModels(request, response);

        return response.toJson();
    }

    @Override
    public void start(IDatabaseContext context) {
        super.start(context);

        componentService = context.getTransactionProvider().getTransaction().findDomainService(IComponentService.NAME);
    }

    @Override
    public void clearCaches() {
        spaceSchema = null;
    }

    private long[] getPeriodBounds(PeriodTypeSchemaConfiguration period) {
        ensureSpaceSchema();

        ICycleSchema cycleSchema = spaceSchema.findCycle(period.getName());
        IPeriodCycle firstCycle = null;
        IPeriodCycle cycle = cycleSchema.getCurrentCycle();
        while (cycle != null && !cycle.isDeleted()) {
            firstCycle = cycle;
            cycle = cycle.getPreviousCycle();
        }

        if (firstCycle == null)
            return null;

        long[] bounds = new long[2];
        bounds[0] = firstCycle.getStartTime();
        if (!period.isNonAggregating())
            bounds[1] = cycleSchema.getCurrentCycle().getSpace().getCurrentPeriod().getEndTime();
        else
            bounds[1] = Times.getCurrentTime();

        return bounds;
    }

    private JsonObjectBuilder buildView(String id, JsonObject view) {
        JsonObjectBuilder builder = new JsonObjectBuilder(view);
        builder.put("id", id);

        JsonObject child = builder.get("child", null);
        if (child != null) {
            String name = view.get("name");

            child = buildView(id + "." + name, child);
            child.put("name", name);
            builder.put("child", child);
        }

        JsonArray children = builder.get("children", null);
        if (children != null) {
            JsonArrayBuilder newChildren = new JsonArrayBuilder();
            for (Object object : children) {
                JsonObject childObject = (JsonObject) object;
                childObject = buildView(id + "." + childObject.get("name"), childObject);
                newChildren.add(childObject);
            }
            builder.put("children", newChildren);
        }

        return builder;
    }

    private JsonObjectBuilder buildViews(String id, JsonObject views) {
        JsonObjectBuilder builder = new JsonObjectBuilder();
        for (Map.Entry<String, Object> entry : views) {
            String name = entry.getKey();
            JsonObjectBuilder view = buildView(id + "." + name, (JsonObject) entry.getValue());
            view.put("name", name);
            builder.put(name, view);
        }

        return builder;
    }

    private void processNotificationModels(JsonObject request, JsonObjectBuilder response, long currentTime) {
        IGroupComponent rootGroup = componentService.getRootGroup();
        JsonArray models = request.get("notificationModels");
        JsonArrayBuilder processedModels = new JsonArrayBuilder();
        for (Object object : models) {
            JsonObject model = (JsonObject) object;
            String selectorName = (String) model.get("selector");
            ISelector selector = rootGroup.createSelector(selectorName);
            Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("lastNotificationUpdateTime", request.get("lastNotificationUpdateTime"));
            parameters.put("currentTime", currentTime);

            JsonObject args = model.get("args", null);
            if (args != null)
                parameters.putAll(args);

            try {
                JsonObjectBuilder result = (JsonObjectBuilder) selector.select(parameters);
                result.put("id", model.get("id"));
                processedModels.add(result.toJson());
            } catch (Throwable e) {
                if (logger.isLogEnabled(LogLevel.ERROR))
                    logger.log(LogLevel.ERROR, messages.selectorFailed(selectorName, JsonUtils.toJson(parameters)), e);
            }
        }

        response.put("notificationModels", processedModels);
        response.put("lastNotificationUpdateTime", currentTime);
    }

    private void processModels(JsonObject request, JsonObjectBuilder response) {
        IGroupComponent rootGroup = componentService.getRootGroup();

        JsonArray models = request.get("models");
        JsonArrayBuilder processedModels = new JsonArrayBuilder();
        for (Object object : models) {
            Map<String, Object> parameters = new HashMap<String, Object>();
            parameters.put("request", request);

            JsonObject model = (JsonObject) object;
            long componentScopeId = 0;
            Object value = model.get("componentScopeId", null);
            if (value instanceof String) {
                if (value.equals("server")) {
                    IComponent server = Selectors.selectServer(rootGroup);
                    if (server != null) {
                        componentScopeId = server.getScopeId();
                        parameters.put("server", true);
                    } else {
                        JsonObjectBuilder processedModel = new JsonObjectBuilder();
                        processedModel.put("id", model.get("id"));
                        processedModel.put("fullUpdate", true);
                        processedModel.put("rows", JsonUtils.EMPTY_ARRAY);
                        processedModels.add(processedModel.toJson());
                        continue;
                    }
                } else
                    componentScopeId = Long.parseLong((String) value);
            } else if (value instanceof Long)
                componentScopeId = (Long) value;

            long current = ((Number) model.get("current")).longValue();
            setSnapshotTime(current);

            String selectorName = model.get("selector");

            Object metricId = model.get("metricId", null);
            if (metricId != null)
                parameters.put("metricId", metricId);

            JsonObject args = model.get("args", null);
            if (args != null)
                parameters.putAll(args);

            if (model.get("fullUpdate", false)) {
                parameters.put("fullUpdate", true);
                parameters.put("start", model.get("start"));
                parameters.put("current", current);
                parameters.put("allowIncrementalUpdate", model.get("allowIncrementalUpdate"));
                parameters.put("preloadStart", model.get("preloadStart"));
                parameters.put("preloadEnd", model.get("preloadEnd"));
            } else if (model.get("incrementalTimedUpdate", false)) {
                parameters.put("incrementalTimedUpdate", true);
                parameters.put("start", model.get("start"));
                parameters.put("current", current);
                parameters.put("beforePreload", model.get("beforePreload", null));
                parameters.put("afterPreload", model.get("afterPreload", null));
            } else if (model.get("incrementalStructuredUpdate", false)) {
                parameters.put("incrementalStructuredUpdate", true);
                parameters.put("current", current);
                parameters.put("beforePreload", model.get("beforePreload", null));
                parameters.put("afterPreload", model.get("afterPreload", null));
            } else
                Assert.error();

            IComponent component;
            if (componentScopeId == 0)
                component = rootGroup;
            else
                component = componentService.findComponent(componentScopeId);

            try {
                Assert.notNull(component);
                ISelector selector = component.createSelector(selectorName);
                JsonObject result = (JsonObject) selector.select(parameters);
                JsonObjectBuilder processedModel = new JsonObjectBuilder();
                processedModel.put("id", model.get("id"));

                if (result.get("fullUpdate", false)) {
                    processedModel.put("fullUpdate", true);
                    processedModel.put("rows", result.get("rows"));
                    processedModel.put("queryResult", result.get("queryResult", null));
                    processedModel.put("beforePreload", result.get("beforePreload", null));
                    processedModel.put("afterPreload", result.get("afterPreload", null));
                } else if (result.get("incrementalTimedUpdate", false)) {
                    processedModel.put("incrementalTimedUpdate", true);
                    processedModel.put("beforePreload", result.get("beforePreload", null));
                    processedModel.put("afterPreload", result.get("afterPreload", null));
                } else if (result.get("incrementalStructuredUpdate", false)) {
                    processedModel.put("incrementalStructuredUpdate", true);
                    processedModel.put("beforePreload", result.get("beforePreload", null));
                    processedModel.put("afterPreload", result.get("afterPreload", null));
                } else
                    Assert.error();

                processedModels.add(processedModel.toJson());
            } catch (Throwable e) {
                if (logger.isLogEnabled(LogLevel.ERROR))
                    logger.log(LogLevel.ERROR, messages.selectorFailed(selectorName, JsonUtils.toJson(parameters)), e);

                JsonObjectBuilder processedModel = new JsonObjectBuilder();
                processedModel.put("id", model.get("id"));
                processedModel.put("fullUpdate", true);
                processedModel.put("rows", JsonUtils.EMPTY_ARRAY);
                processedModels.add(processedModel.toJson());
            }
        }

        if (!processedModels.isEmpty())
            response.put("models", processedModels);
    }

    private void setSnapshotTime(long time) {
        Object operation = context.getTransactionProvider().getTransaction().getOperation();
        if (operation instanceof ITimeSnapshotOperation) {
            ITimeSnapshotOperation snapshotOperation = (ITimeSnapshotOperation) operation;
            snapshotOperation.setTime(time);
        } else if (operation instanceof IOperationWrapper && ((IOperationWrapper) operation).getOperation() instanceof ITimeSnapshotOperation) {
            ITimeSnapshotOperation snapshotOperation = (ITimeSnapshotOperation) ((IOperationWrapper) operation).getOperation();
            snapshotOperation.setTime(time);
        }
    }

    private void ensureSpaceSchema() {
        if (spaceSchema == null) {
            spaceSchema = schema.getParent().getParent().findDomain("aggregation").findSpace("aggregation");
            Assert.notNull(spaceSchema);
        }
    }

    private interface IMessages {
        @DefaultMessage("Selector ''{0}'' has failed. Request parameters:\n{1}")
        ILocalizedMessage selectorFailed(String selectorName, JsonObject requestData);
    }
}
