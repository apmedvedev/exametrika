/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.nodes;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;

import java.util.Arrays;
import java.util.List;

import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.api.component.nodes.IComponentVersion;
import com.exametrika.api.component.nodes.IHealthComponentVersion;
import com.exametrika.api.component.nodes.IIncident;
import com.exametrika.api.component.nodes.IIncidentGroup;
import com.exametrika.api.exadb.core.IDumpContext;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.fields.INumericField;
import com.exametrika.api.exadb.objectdb.fields.IReferenceField;
import com.exametrika.api.exadb.objectdb.fields.ISerializableField;
import com.exametrika.api.exadb.objectdb.fields.ISingleReferenceField;
import com.exametrika.api.exadb.objectdb.fields.IStringField;
import com.exametrika.api.exadb.objectdb.fields.ITagField;
import com.exametrika.api.exadb.security.IPermission;
import com.exametrika.common.json.IJsonHandler;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonSerializers;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Strings;
import com.exametrika.impl.component.schema.IncidentNodeSchema;
import com.exametrika.impl.exadb.objectdb.ObjectNodeObject;
import com.exametrika.spi.aggregator.common.meters.MeterExpressions;
import com.exametrika.spi.component.IAlert;
import com.exametrika.spi.component.config.model.AlertSchemaConfiguration;
import com.exametrika.spi.component.schema.IAlertChannelSchema;


/**
 * The {@link IncidentNode} is an incident node object.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are not thread safe.
 */
public class IncidentNode extends ObjectNodeObject implements IIncident {
    private static final int ON_STATE = 1;
    private static final int OFF_STATE = 2;
    private static final int STATUS_STATE = 3;
    private static final int ID_FIELD = 0;
    private static final int NAME_FIELD = 1;
    private static final int MESSAGE_FIELD = 2;
    private static final int CREATION_TIME_FIELD = 3;
    private static final int LAST_NOTIFICATION_TIMES_FIELD = 4;
    private static final int REF_COUNT_FIELD = 5;
    private static final int COMPONENT_FIELD = 6;
    private static final int GROUPS_FIELD = 7;
    private static final int TAGS_FIELD = 8;
    private boolean resolved;

    public IncidentNode(INode node) {
        super(node);
    }

    public boolean isResolved() {
        return resolved;
    }

    public void setResolved(boolean resolved) {
        this.resolved = resolved;
    }

    @Override
    public IncidentNodeSchema getSchema() {
        return (IncidentNodeSchema) super.getSchema();
    }

    @Override
    public int getIncidentId() {
        INumericField field = getField(ID_FIELD);
        return field.getInt();
    }

    @Override
    public String getName() {
        IStringField field = getField(NAME_FIELD);
        return field.get();
    }

    @Override
    public String getMessage() {
        IStringField field = getField(MESSAGE_FIELD);
        return field.get();
    }

    @Override
    public IAlert getAlert() {
        return getComponent().findAlertSchema(getName());
    }

    @Override
    public long getCreationTime() {
        INumericField field = getField(CREATION_TIME_FIELD);
        return field.getLong();
    }

    public long[] getLastNotificationTimes() {
        ISerializableField<long[]> field = getField(LAST_NOTIFICATION_TIMES_FIELD);
        return field.get();
    }

    public void setLastNotificationTime(int index, long time) {
        ISerializableField<long[]> field = getField(LAST_NOTIFICATION_TIMES_FIELD);
        long[] times = field.get();
        times[index] = time;
        field.setModified();
    }

    @Override
    public ComponentNode getComponent() {
        ISingleReferenceField<ComponentNode> field = getField(COMPONENT_FIELD);
        return field.get();
    }

    @Override
    public String getState() {
        IComponentVersion version = getComponent().getCurrentVersion();
        if (version instanceof IHealthComponentVersion) {
            IHealthComponentVersion.State state = ((IHealthComponentVersion) version).getState();
            return state.toString().toLowerCase().replace('_', ' ');
        }
        return "";
    }

    @Override
    public List<String> getTags() {
        ITagField field = getField(TAGS_FIELD);
        return field.get();
    }

    @Override
    public void setTags(List<String> tags) {
        IPermission permission = getSchema().getEditPermission();
        permission.beginCheck(this);

        ITagField field = getField(TAGS_FIELD);
        field.set(tags);

        permission.endCheck();
    }

    public int getRefCount() {
        INumericField field = getField(REF_COUNT_FIELD);
        return field.getInt();
    }

    public TIntList getParentIds() {
        TIntList list = new TIntArrayList();
        if (getRefCount() != 0) {
            for (IIncidentGroup group : getGroups())
                list.add(group.getIncidentId());
        } else
            list.add(0);

        return list;
    }

    public Iterable<IIncidentGroup> getGroups() {
        IReferenceField<IIncidentGroup> groupsField = getField(GROUPS_FIELD);
        return groupsField;
    }

    public void addGroup(IIncidentGroup group) {
        INumericField field = getField(REF_COUNT_FIELD);
        int refCount = field.getInt();
        field.set(refCount + 1);

        IReferenceField groupsField = getField(GROUPS_FIELD);
        groupsField.add(group);
    }

    public void removeGroup(IIncidentGroup group, boolean resolved) {
        IReferenceField groupsField = getField(GROUPS_FIELD);
        groupsField.remove(group);

        INumericField field = getField(REF_COUNT_FIELD);
        int refCount = field.getInt();
        if (refCount > 1)
            field.set(refCount - 1);
        else
            delete(resolved);
    }

    public void init(AlertSchemaConfiguration schema, long creationTime, ComponentNode component) {
        IStringField nameField = getField(NAME_FIELD);
        nameField.set(schema.getName());

        INumericField creationTimeField = getField(CREATION_TIME_FIELD);
        creationTimeField.setLong(creationTime);

        long[] times = new long[schema.getChannels().size()];
        Arrays.fill(times, creationTime);

        ISerializableField<long[]> lastNotificationTimesField = getField(LAST_NOTIFICATION_TIMES_FIELD);
        lastNotificationTimesField.set(times);

        ISingleReferenceField<IComponent> componentField = getField(COMPONENT_FIELD);
        componentField.set(component);

        component.addIncident(this);
    }

    @Override
    public final void delete() {
        delete(false);
    }

    @Override
    public final void delete(boolean resolved) {
        IPermission permission = getSchema().getDeletePermission();
        permission.beginCheck(this);

        doDelete(resolved);

        IReferenceField<IncidentGroupNode> groupsField = getField(GROUPS_FIELD);
        for (IncidentGroupNode group : groupsField)
            group.removeChild(this, resolved);

        getComponent().removeIncident(this, resolved);

        super.delete();

        permission.endCheck();
    }

    @Override
    public void dump(IJsonHandler json, IDumpContext context) {
        super.dump(json, context);

        json.key("incidentId");
        json.value(getIncidentId());
        json.key("name");
        json.value(getName());
        json.key("refCount");
        json.value(getRefCount());

        if ((context.getFlags() & IDumpContext.DUMP_TIMES) != 0) {
            json.key("creationTime");
            json.value(getCreationTime());
            json.key("lastNotificationTimes");
            JsonSerializers.write(json, JsonUtils.toJson(getLastNotificationTimes()));
        }

        List<String> tags = getTags();
        if (tags != null) {
            json.key("tags");
            JsonSerializers.write(json, JsonUtils.toJson(tags));
        }
    }

    public void logOn() {
        getComponent().log("alert", Json.object().put("name", getName()).put("state", "on").put("message",
                buildMessage(ON_STATE, false)).toObject());
    }

    public void logOff(boolean resolved) {
        getComponent().log("alert", Json.object().put("name", getName()).put("state", "off").put("message",
                buildMessage(OFF_STATE, resolved)).toObject());
    }

    public void logStatus() {
        getComponent().log("alert", Json.object().put("name", getName()).put("state", "status").put("message",
                buildMessage(STATUS_STATE, false)).toObject());
    }

    protected void doDelete(boolean resolved) {
    }

    private String buildMessage(int state, boolean resolved) {
        String message = "";
        this.resolved = resolved;
        IAlert alert = getAlert();
        if (alert != null) {
            IAlertChannelSchema channel = alert.getChannels().get(0);
            switch (state) {
                case ON_STATE:
                    message = channel.getOnTemplate().execute(this, MeterExpressions.getRuntimeContext());
                    break;
                case OFF_STATE:
                    if (channel.getOffTemplate() != null)
                        message = channel.getOffTemplate().execute(this, MeterExpressions.getRuntimeContext());
                    break;
                case STATUS_STATE:
                    if (channel.getStatusTemplate() != null)
                        message = channel.getStatusTemplate().execute(this, MeterExpressions.getRuntimeContext());
                    break;
                default:
                    return Assert.error();
            }
        }

        if (!Strings.isEmpty(message)) {
            IStringField field = getField(MESSAGE_FIELD);
            field.set(message);
        }

        return message;
    }

    @Override
    public String toString() {
        return getName() + " - " + super.toString();
    }
}