/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.selectors;

import java.util.Map;

import com.exametrika.api.aggregator.Location;
import com.exametrika.api.component.ISelectionService;
import com.exametrika.api.component.ISelectionService.IRepresentationBuilder;
import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.api.component.nodes.IGroupComponent;
import com.exametrika.api.component.nodes.IGroupComponentVersion;
import com.exametrika.api.component.schema.ISelectorSchema;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonArrayBuilder;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Strings;
import com.exametrika.impl.component.nodes.ComponentVersionNode;
import com.exametrika.impl.component.nodes.GroupComponentNode;
import com.exametrika.impl.component.nodes.GroupComponentVersionNode;
import com.exametrika.impl.component.nodes.HealthComponentNode;
import com.exametrika.impl.component.nodes.HealthComponentVersionNode;
import com.exametrika.spi.component.Selectors;
import com.exametrika.spi.component.Selectors.StateInfo;


/**
 * The {@link AllComponentsSelector} is a selector of all components of specific type.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class AllComponentsSelector extends Selector {
    protected final ISelectionService selectionService;
    private final String rootGroupScope;
    private final String kpiComponentType;
    private final boolean selectServer;
    protected String periodType;
    protected long currentTime;

    public AllComponentsSelector(IComponent component, ISelectorSchema schema, String rootGroupScope, String kpiComponentType,
                                 boolean selectServer) {
        super(component, schema);

        Assert.notNull(rootGroupScope);
        Assert.notNull(kpiComponentType);

        this.rootGroupScope = rootGroupScope;
        this.kpiComponentType = kpiComponentType;
        this.selectServer = selectServer;
        selectionService = this.schema.getContext().getTransactionProvider().getTransaction().findDomainService(ISelectionService.NAME);
    }

    @Override
    protected Object doSelect(Map<String, ?> parameters) {
        Assert.isTrue(Boolean.TRUE.equals(parameters.get("fullUpdate")));

        periodType = ((JsonObject) parameters.get("request")).select("timeScale.periodType");
        currentTime = ((Number) parameters.get("current")).longValue();

        Json json = Json.object().put("fullUpdate", true);

        IGroupComponentVersion version = (IGroupComponentVersion) component.get();
        IGroupComponent rootGroup = null;
        if (version != null) {
            for (IGroupComponent child : version.getChildren()) {
                if (child.getScope().toString().equals(rootGroupScope)) {
                    rootGroup = child;
                    break;
                }
            }
        }

        Json jsonRows = json.putArray("rows");
        if (rootGroup != null) {
            IGroupComponentVersion rootGroupVersion = (IGroupComponentVersion) rootGroup.get();
            if (rootGroupVersion != null) {
                for (IGroupComponent child : rootGroupVersion.getChildren()) {
                    GroupComponentVersionNode childVersion = (GroupComponentVersionNode) child.get();
                    if (childVersion != null)
                        buildComponentGroup(childVersion, jsonRows.addObject());
                }
                for (IComponent child : rootGroupVersion.getComponents()) {
                    HealthComponentVersionNode childVersion = (HealthComponentVersionNode) child.get();
                    if (childVersion != null)
                        buildComponent(null, kpiComponentType, 0, childVersion, jsonRows.addObject(), false);
                }
            }
        }

        if (selectServer) {
            IComponent server = Selectors.selectServer((IGroupComponent) component);
            if (server != null) {
                HealthComponentVersionNode serverVersion = (HealthComponentVersionNode) server.get();
                if (serverVersion != null)
                    buildComponent("server", kpiComponentType + ".server", 0, serverVersion, jsonRows.addObject(), true);
            }
        }
        return json.toObject();
    }

    protected void doBuildComponentGroup(GroupComponentVersionNode version, Json result) {
    }

    protected void doBuildComponent(HealthComponentVersionNode version, String componentType, Json result, boolean server) {
    }

    protected JsonObject buildRepresentation(ComponentVersionNode version, String componentType, IRepresentationBuilder builder) {
        return (JsonObject) selectionService.buildRepresentation(periodType, currentTime,
                new Location(version.getComponent().getScopeId(), 0), componentType, 0, builder);
    }

    private void buildComponentGroup(GroupComponentVersionNode version, Json result) {
        GroupComponentNode component = (GroupComponentNode) version.getComponent();
        StateInfo info = Selectors.buildState(version, true, selectionService, periodType, currentTime, kpiComponentType);

        result.put("id", Long.toString(component.getScopeId()))
                .put("elementId", Long.toString(component.getScopeId()))
                .put("group", true)
                .putIf("dynamic", true, version.isDynamic())
                .put("state", info.state)
                .put("stateTitle", info.stateTitle)
                .putIf("maintenanceMessage", version.getMaintenanceMessage(), !Strings.isEmpty(version.getMaintenanceMessage()))
                .put("title", Selectors.buildReference(version))
                .putIf("tags", JsonUtils.toJson(component.getTags()), component.getTags() != null)
                .put("upDownPeriod", version.getUpDownPeriod())
                .put("startStopTime", version.getStartStopTime())
                .put("totalPeriod", version.getTotalPeriod())
                .put("creationTime", version.getCreationTime());

        doBuildComponentGroup(version, result);

        Json children = result.putArray("children");
        for (IGroupComponent child : version.getChildren()) {
            GroupComponentVersionNode childVersion = (GroupComponentVersionNode) child.get();
            if (childVersion != null)
                buildComponentGroup(childVersion, children.addObject());
        }
        for (IComponent child : version.getComponents()) {
            HealthComponentVersionNode childVersion = (HealthComponentVersionNode) child.get();
            if (childVersion != null)
                buildComponent(null, kpiComponentType, component.getScopeId(), childVersion, children.addObject(), false);
        }
    }

    private void buildComponent(String elementId, String kpiComponentType, long groupScopeId, HealthComponentVersionNode version,
                                Json result, boolean server) {
        HealthComponentNode component = (HealthComponentNode) version.getComponent();
        StateInfo info = Selectors.buildState(version, true, selectionService, periodType, currentTime, kpiComponentType);

        JsonArrayBuilder groups = new JsonArrayBuilder();
        for (IGroupComponent group : version.getGroups())
            groups.add(Long.toString(group.getScopeId()));

        result.put("id", Long.toString(groupScopeId) + "-" + component.getScopeId())
                .put("elementId", elementId == null ? Long.toString(component.getScopeId()) : elementId)
                .put("state", info.state)
                .put("stateTitle", info.stateTitle)
                .putIf("maintenanceMessage", version.getMaintenanceMessage(), !Strings.isEmpty(version.getMaintenanceMessage()))
                .putIf("dynamic", true, version.isDynamic())
                .put("title", Selectors.buildReference(version))
                .putIf("tags", JsonUtils.toJson(component.getTags()), component.getTags() != null)
                .put("groups", groups.toJson())
                .put("upDownPeriod", version.getUpDownPeriod())
                .put("startStopTime", version.getStartStopTime())
                .put("totalPeriod", version.getTotalPeriod())
                .put("creationTime", version.getCreationTime());

        doBuildComponent(version, kpiComponentType, result, server);
    }
}
