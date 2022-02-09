/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.nodes;

import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.fields.IAggregationField;
import com.exametrika.api.aggregator.nodes.IAggregationNode;
import com.exametrika.api.aggregator.schema.IAggregationFieldSchema;
import com.exametrika.api.component.nodes.IGroupComponent;
import com.exametrika.api.component.nodes.IHealthComponent;
import com.exametrika.api.component.nodes.IHealthComponentVersion;
import com.exametrika.api.component.nodes.IHealthComponentVersion.State;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.security.IPermission;
import com.exametrika.common.json.Json;
import com.exametrika.impl.component.schema.HealthComponentNodeSchema;
import com.exametrika.spi.aggregator.IComponentAccessor;
import com.exametrika.spi.aggregator.IComponentAccessorFactory;
import com.exametrika.spi.aggregator.IComputeContext;
import com.exametrika.spi.aggregator.IRuleContext;
import com.exametrika.spi.component.IHealthCheck;


/**
 * The {@link HealthComponentNode} is a health component node object.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are not thread safe.
 */
public class HealthComponentNode extends ComponentNode implements IHealthComponent {
    public HealthComponentNode(INode node) {
        super(node);
    }

    public final void setDynamic() {
        if (((HealthComponentVersionNode) getCurrentVersion()).isDynamic())
            return;

        HealthComponentVersionNode node = (HealthComponentVersionNode) addVersion();
        node.setDynamic();

        doSetDynamic();
    }

    public final void clearDynamic() {
        if (!((HealthComponentVersionNode) getCurrentVersion()).isDynamic())
            return;

        HealthComponentVersionNode node = (HealthComponentVersionNode) addVersion();
        node.clearDynamic();

        doClearDynamic();
    }

    public void setUnavailableState() {
        if (!supportsAvailability())
            return;

        HealthComponentVersionNode currentVersion = (HealthComponentVersionNode) getCurrentVersion();
        State oldState = currentVersion.getState();
        if (oldState != State.NORMAL && oldState != State.HEALTH_ERROR && oldState != State.HEALTH_WARNING)
            return;

        HealthComponentVersionNode node = (HealthComponentVersionNode) addVersion();
        node.setState(oldState, State.UNAVAILABLE);

        doSetUnavailableState();
    }

    public void setHealthErrorState() {
        if (!supportsAvailability())
            return;

        HealthComponentVersionNode currentVersion = (HealthComponentVersionNode) getCurrentVersion();
        State oldState = currentVersion.getState();
        if (oldState != State.NORMAL && oldState != State.HEALTH_WARNING)
            return;

        HealthComponentVersionNode node = (HealthComponentVersionNode) addVersion();
        node.setState(oldState, State.HEALTH_ERROR);
    }

    public void setHealthWarningState() {
        if (!supportsAvailability())
            return;

        HealthComponentVersionNode currentVersion = (HealthComponentVersionNode) getCurrentVersion();
        State oldState = currentVersion.getState();
        if (oldState != State.NORMAL && oldState != State.HEALTH_ERROR)
            return;

        HealthComponentVersionNode node = (HealthComponentVersionNode) addVersion();
        node.setState(oldState, State.HEALTH_WARNING);
    }

    public void setNormalState() {
        if (!supportsAvailability())
            return;

        HealthComponentVersionNode currentVersion = (HealthComponentVersionNode) getCurrentVersion();
        State oldState = currentVersion.getState();
        if (oldState != State.UNAVAILABLE && oldState != State.HEALTH_ERROR && oldState != State.HEALTH_WARNING)
            return;

        HealthComponentVersionNode node = (HealthComponentVersionNode) addVersion();
        node.setState(oldState, State.NORMAL);

        doSetNormalState();
    }

    public void setNormalComputedState() {
        if (!supportsAvailability())
            return;

        HealthComponentVersionNode currentVersion = (HealthComponentVersionNode) getCurrentVersion();
        State oldState = currentVersion.getState();
        if (oldState != State.HEALTH_ERROR && oldState != State.HEALTH_WARNING)
            return;

        HealthComponentVersionNode node = (HealthComponentVersionNode) addVersion();
        node.setState(oldState, State.NORMAL);
    }

    @Override
    public void enableMaintenanceMode(String message) {
        if (!supportsAvailability())
            return;

        HealthComponentVersionNode currentVersion = (HealthComponentVersionNode) getCurrentVersion();
        State state = currentVersion.getState();
        if (state == State.CREATED || state == State.DELETED || state == State.MAINTENANCE)
            return;

        IPermission permission = ((HealthComponentNodeSchema) getSchema()).getEditMaintenanceModePermission();
        permission.beginCheck(this);

        HealthComponentVersionNode node = (HealthComponentVersionNode) addVersion();
        node.enableMaintenanceMode(message);
        log("enableMaintenanceMode", Json.object().put("message", message).toObject());

        removeAllIncidents();

        doEnableMaintenanceMode(message);

        permission.endCheck();
    }

    @Override
    public void disableMaintenanceMode() {
        if (!supportsAvailability())
            return;

        HealthComponentVersionNode currentVersion = (HealthComponentVersionNode) getCurrentVersion();
        if (currentVersion.getState() != State.MAINTENANCE)
            return;

        IPermission permission = ((HealthComponentNodeSchema) getSchema()).getEditMaintenanceModePermission();
        permission.beginCheck(this);

        HealthComponentVersionNode node = (HealthComponentVersionNode) addVersion();
        node.disableMaintenanceMode();
        log("disableMaintenanceMode");

        doDisableMaintenanceMode();

        permission.endCheck();
    }

    public void onStateChanged(State oldState, State newState) {
        ensureRuleCache();

        for (IHealthCheck check : healthChecks)
            check.onStateChanged(this, oldState, newState);

        for (IGroupComponent group : getCurrentVersion().getGroups())
            ((GroupComponentNode) group).checkAvailability();
    }

    @Override
    public boolean allowExecution() {
        if (!supportsAvailability())
            return super.allowExecution();
        else
            return super.allowExecution() && ((IHealthComponentVersion) getCurrentVersion()).getState() != State.MAINTENANCE;
    }

    protected void doSetDynamic() {
    }

    protected void doClearDynamic() {
    }

    protected void doSetUnavailableState() {
    }

    protected void doSetNormalState() {
    }

    protected void doEnableMaintenanceMode(String message) {
    }

    protected void doDisableMaintenanceMode() {
    }

    @Override
    protected void doExecuteRules(IAggregationNode aggregationNode, IRuleContext c) {
        IAggregationField aggregationField = aggregationNode.getAggregationField();
        IAggregationFieldSchema aggregationFieldSchema = aggregationField.getSchema();
        IComponentAccessorFactory componentAccessorFactory = aggregationFieldSchema.getRuleRepresentation().getAccessorFactory();
        if (!componentAccessorFactory.hasMetric("healthIndex"))
            return;

        IComponentAccessor accessor = componentAccessorFactory.createAccessor(null, null, "healthIndex");
        IComponentValue value = aggregationField.getValue(false);
        IComputeContext context = aggregationField.getComputeContext();
        String healthIndex = (String) accessor.get(value, context);

        if (healthIndex != null) {
            if (healthIndex.equals("error"))
                setHealthErrorState();
            else if (healthIndex.equals("warning"))
                setHealthWarningState();
            else if (!healthIndex.equals("unknown"))
                setNormalComputedState();
        }
    }

    protected boolean supportsAvailability() {
        return true;
    }
}