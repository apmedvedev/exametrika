/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.nodes;

import java.util.List;

import com.exametrika.api.component.nodes.IHealthComponent;
import com.exametrika.api.component.nodes.IHealthComponentVersion;
import com.exametrika.api.exadb.core.IDumpContext;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.fields.INumericField;
import com.exametrika.api.exadb.objectdb.fields.IStringField;
import com.exametrika.common.json.IJsonHandler;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.component.services.HealthService;


/**
 * The {@link HealthComponentVersionNode} is a health component version node object.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are not thread safe.
 */
public class HealthComponentVersionNode extends ComponentVersionNode implements IHealthComponentVersion {
    private static final int HEALTH_WARNING_FLAG = 0x4;
    private static final int HEALTH_ERROR_FLAG = 0x8;
    private static final int UNAVAILABLE_FLAG = 0x10;
    private static final int MAINTENANCE_MODE_FLAG = 0x20;
    private static final int DYNAMIC_FLAG = 0x40;
    private static final int MAINTENANCE_MESSAGE_FIELD = 8;
    private static final int CREATION_TIME_FIELD = 9;
    private static final int START_STOP_TIME_FIELD = 10;

    public HealthComponentVersionNode(INode node) {
        super(node);
    }

    @Override
    public boolean isDynamic() {
        return (getFlags() & DYNAMIC_FLAG) != 0;
    }

    public void setDynamic() {
        addFlags(DYNAMIC_FLAG);
    }

    public void clearDynamic() {
        removeFlags(DYNAMIC_FLAG);
    }

    @Override
    public boolean isHealthy() {
        State state = getState();
        return state == State.NORMAL || state == State.HEALTH_WARNING;
    }

    public void setState(State oldState, State newState) {
        removeFlags(HEALTH_WARNING_FLAG | HEALTH_ERROR_FLAG | UNAVAILABLE_FLAG);

        switch (newState) {
            case NORMAL:
                break;
            case HEALTH_ERROR:
                addFlags(HEALTH_ERROR_FLAG);
                break;
            case HEALTH_WARNING:
                addFlags(HEALTH_WARNING_FLAG);
                break;
            case UNAVAILABLE:
                addFlags(UNAVAILABLE_FLAG);
                break;
            default:
                Assert.error();
        }

        onComponentStateCompleted(oldState, newState, getTime());
    }

    @Override
    public State getState() {
        int flags = getFlags();
        if ((flags & UNAVAILABLE_FLAG) != 0)
            return State.UNAVAILABLE;
        else if ((flags & HEALTH_ERROR_FLAG) != 0)
            return State.HEALTH_ERROR;
        else if ((flags & HEALTH_WARNING_FLAG) != 0)
            return State.HEALTH_WARNING;
        else if ((flags & MAINTENANCE_MODE_FLAG) != 0)
            return State.MAINTENANCE;
        else if ((flags & DELETED_FLAG) != 0)
            return State.DELETED;
        else
            return State.NORMAL;
    }

    @Override
    public String getMaintenanceMessage() {
        IStringField field = getField(MAINTENANCE_MESSAGE_FIELD);
        return field.get();
    }

    public void enableMaintenanceMode(String message) {
        IStringField messageField = getField(MAINTENANCE_MESSAGE_FIELD);
        messageField.set(message);

        State state = getState();

        addFlags(MAINTENANCE_MODE_FLAG);
        removeFlags(HEALTH_WARNING_FLAG | HEALTH_ERROR_FLAG | UNAVAILABLE_FLAG);
        onComponentStateCompleted(state, State.MAINTENANCE, getTime());
    }

    public void disableMaintenanceMode() {
        IStringField messageField = getField(MAINTENANCE_MESSAGE_FIELD);
        messageField.set(null);

        removeFlags(MAINTENANCE_MODE_FLAG);

        if (!isComponentAvailable())
            addFlags(UNAVAILABLE_FLAG);

        onComponentStateCompleted(State.MAINTENANCE, getState(), getTime());
    }

    @Override
    public long getCreationTime() {
        INumericField field = getField(CREATION_TIME_FIELD);
        return field.getLong();
    }

    @Override
    public long getTotalPeriod() {
        INumericField field = getField(CREATION_TIME_FIELD);
        return getSelectionTime() - field.getLong();
    }

    public long getStartStopTime() {
        INumericField field = getField(START_STOP_TIME_FIELD);
        return field.getLong();
    }

    @Override
    public long getStartTime() {
        if (isHealthy()) {
            INumericField field = getField(START_STOP_TIME_FIELD);
            return field.getLong();
        } else
            return 0;
    }

    @Override
    public long getStopTime() {
        if (!isHealthy()) {
            INumericField field = getField(START_STOP_TIME_FIELD);
            return field.getLong();
        } else
            return 0;
    }

    public long getUpDownPeriod() {
        INumericField field = getField(START_STOP_TIME_FIELD);
        return getSelectionTime() - field.getLong();
    }

    @Override
    public long getUpPeriod() {
        if (isHealthy()) {
            INumericField field = getField(START_STOP_TIME_FIELD);
            return getSelectionTime() - field.getLong();
        } else
            return 0;
    }

    @Override
    public long getDownPeriod() {
        if (!isHealthy()) {
            INumericField field = getField(START_STOP_TIME_FIELD);
            return getSelectionTime() - field.getLong();
        } else
            return 0;
    }

    @Override
    public void dump(IJsonHandler json, IDumpContext context) {
        super.dump(json, context);

        if (supportsAvailability()) {
            json.key("state");
            json.value(getState().toString().toLowerCase());
            if (getMaintenanceMessage() != null) {
                json.key("maintenanceMessage");
                json.value(getMaintenanceMessage());
            }
        }
    }

    @Override
    protected void initFirstVersion() {
        super.initFirstVersion();

        if (supportsAvailability()) {
            long time = getTime();
            INumericField creationTimeField = getField(CREATION_TIME_FIELD);
            creationTimeField.setLong(time);

            INumericField startStopTimeField = getField(START_STOP_TIME_FIELD);
            startStopTimeField.setLong(time);

            ComponentRootNode root = getSpace().getRootNode();
            root.addHealthComponent((IHealthComponent) getComponent());

            onComponentCreated();

            if (!isComponentAvailable())
                addFlags(UNAVAILABLE_FLAG);

            onComponentStateCompleted(State.CREATED, getState(), getTime());
        }
    }

    @Override
    protected void doDelete() {
        super.doDelete();

        if (supportsAvailability()) {
            HealthComponentVersionNode prevVersion = (HealthComponentVersionNode) getPreviousVersion();
            if (prevVersion != null)
                prevVersion.onComponentStateCompleted(prevVersion.getState(), State.DELETED, getTime());

            ComponentRootNode root = getSpace().getRootNode();
            root.removeHealthComponent((IHealthComponent) getComponent());

            onComponentDeleted();
        }
    }

    @Override
    protected void copyFields(ComponentVersionNode node) {
        super.copyFields(node);

        if (supportsAvailability()) {
            IStringField nodeMaintenanceMessageField = node.getField(MAINTENANCE_MESSAGE_FIELD);
            nodeMaintenanceMessageField.set(getMaintenanceMessage());

            long creationTime = getCreationTime();
            INumericField startStopTimeField = getField(START_STOP_TIME_FIELD);
            long startStopTime = startStopTimeField.getLong();

            if (creationTime == 0) {
                HealthComponentVersionNode healthNode = (HealthComponentVersionNode) node;
                creationTime = node.getTime();
                startStopTime = creationTime;

                ComponentRootNode root = getSpace().getRootNode();
                root.addHealthComponent((IHealthComponent) getComponent());

                healthNode.onComponentCreated();

                if (!isComponentAvailable())
                    node.addFlags(UNAVAILABLE_FLAG);

                healthNode.onComponentStateCompleted(State.CREATED, healthNode.getState(), creationTime);
            }

            INumericField nodeCreationTimeField = node.getField(CREATION_TIME_FIELD);
            nodeCreationTimeField.setLong(creationTime);

            INumericField nodeStartStopTimeField = node.getField(START_STOP_TIME_FIELD);
            nodeStartStopTimeField.setLong(startStopTime);
        }
    }

    @Override
    protected void buildFlagsList(int flags, List<String> list) {
        super.buildFlagsList(flags, list);
        if ((flags & UNAVAILABLE_FLAG) != 0)
            list.add("unavailable");
        if ((flags & HEALTH_WARNING_FLAG) != 0)
            list.add("healthWarning");
        if ((flags & HEALTH_ERROR_FLAG) != 0)
            list.add("healthError");
        if ((flags & DYNAMIC_FLAG) != 0)
            list.add("dynamic");
        if ((flags & MAINTENANCE_MODE_FLAG) != 0)
            list.add("maintenance");
    }

    protected boolean supportsAvailability() {
        return true;
    }

    protected boolean isComponentAvailable() {
        return true;
    }

    protected void doComponentCreated() {
    }

    protected void doComponentDeleted() {
    }

    private void onComponentCreated() {
        HealthService healthService = getTransaction().findDomainService(HealthService.NAME);
        healthService.onComponentCreated((IHealthComponent) getComponent(), getTime());

        doComponentCreated();
    }

    private void onComponentDeleted() {
        HealthService healthService = getTransaction().findDomainService(HealthService.NAME);
        healthService.onComponentDeleted((IHealthComponent) getComponent(), getTime());

        doComponentDeleted();
    }

    private void onComponentStateCompleted(State oldState, State newState, long endTime) {
        INumericField startStopTimeField = getField(START_STOP_TIME_FIELD);
        long startTime = startStopTimeField.getLong();

        HealthService healthService = getTransaction().findDomainService(HealthService.NAME);
        healthService.onComponentStateCompleted((IHealthComponent) getComponent(), oldState, newState, startTime, endTime);

        if (!isReadOnly())
            startStopTimeField.setLong(endTime);

        HealthComponentNode component = (HealthComponentNode) getComponent();
        component.onStateChanged(oldState, newState);
    }
}