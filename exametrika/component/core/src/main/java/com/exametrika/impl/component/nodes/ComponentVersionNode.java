/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.nodes;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.api.component.config.model.ComponentSchemaConfiguration;
import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.api.component.nodes.IComponentVersion;
import com.exametrika.api.component.nodes.IGroupComponent;
import com.exametrika.api.exadb.core.IDumpContext;
import com.exametrika.api.exadb.core.IOperationWrapper;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.INodeSortedIndex;
import com.exametrika.api.exadb.objectdb.IObjectSpace;
import com.exametrika.api.exadb.objectdb.fields.IJsonField;
import com.exametrika.api.exadb.objectdb.fields.INumericField;
import com.exametrika.api.exadb.objectdb.fields.IReferenceField;
import com.exametrika.api.exadb.objectdb.fields.ISingleReferenceField;
import com.exametrika.api.exadb.objectdb.schema.IObjectNodeSchema;
import com.exametrika.common.json.IJsonHandler;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonSerializers;
import com.exametrika.common.utils.Times;
import com.exametrika.impl.component.fields.IndexedVersionField;
import com.exametrika.impl.component.fields.VersionTime;
import com.exametrika.impl.component.schema.ComponentNodeSchema;
import com.exametrika.impl.component.schema.ComponentVersionNodeSchema;
import com.exametrika.impl.exadb.objectdb.ObjectNodeObject;
import com.exametrika.spi.component.ITimeSnapshotOperation;


/**
 * The {@link ComponentVersionNode} is a component version node object.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are not thread safe.
 */
public class ComponentVersionNode extends ObjectNodeObject implements IComponentVersion {
    private static final int READONLY_FLAG = 0x1;
    protected static final int DELETED_FLAG = 0x2;
    private static final int FLAGS_FIELD = 0;
    private static final int FLAGS_MASK = 0xFF;
    private static final int DELETION_COUNT_MASK = 0xFFFFFF00;
    private static final int TIME_FIELD = 1;
    private static final int INDEX_FIELD = 2;
    private static final int OPTIONS_FIELD = 3;
    private static final int PROPERTIES_FIELD = 4;
    private static final int COMPONENT_FIELD = 5;
    private static final int PREVIOUS_VERSION_FIELD = 6;
    private static final int GROUPS_FIELD = 7;
    private boolean allowModify = true;

    public ComponentVersionNode(INode node) {
        super(node);
    }

    @Override
    public final boolean allowModify() {
        return allowModify || ((getFlags() & READONLY_FLAG) == 0);
    }

    @Override
    public final boolean allowDeletion() {
        return false;
    }

    @Override
    public final ComponentVersionNodeSchema getSchema() {
        return (ComponentVersionNodeSchema) super.getSchema();
    }

    @Override
    public ComponentSchemaConfiguration getConfiguration() {
        return getSchema().getConfiguration().getComponent();
    }

    @Override
    public String getTitle() {
        JsonObject properties = getProperties();
        if (properties != null) {
            String title = properties.get("title", null);
            if (title != null)
                return title;
        }

        return getComponent().getScope().toString();
    }

    @Override
    public String getDescription() {
        JsonObject properties = getProperties();
        if (properties != null) {
            String description = properties.get("description", null);
            if (description != null)
                return description;
        }

        return "";
    }

    @Override
    public final boolean isDeleted() {
        return (getFlags() & DELETED_FLAG) != 0;
    }

    public final void setDeleted() {
        doDelete();

        int deletionCount = 0;
        ComponentVersionNode prevVersion = (ComponentVersionNode) getPreviousVersion();
        if (prevVersion != null)
            deletionCount = prevVersion.getDeletionCount() + 1;

        setFlags((deletionCount << 8) | DELETED_FLAG);
    }

    public final int getDeletionCount() {
        INumericField field = getField(FLAGS_FIELD);
        return (field.getInt() & DELETION_COUNT_MASK) >> 8;
    }

    @Override
    public final long getTime() {
        INumericField field = getField(TIME_FIELD);
        return field.getLong();
    }

    @Override
    public final JsonObject getOptions() {
        IJsonField field = getField(OPTIONS_FIELD);
        return field.get();
    }

    public final void setOptions(JsonObject value) {
        IJsonField field = getField(OPTIONS_FIELD);
        field.set(value);
    }

    @Override
    public final JsonObject getProperties() {
        IJsonField field = getField(PROPERTIES_FIELD);
        return field.get();
    }

    public final void setProperties(JsonObject value) {
        IJsonField field = getField(PROPERTIES_FIELD);
        field.set(value);
    }

    @Override
    public final ComponentNode getComponent() {
        ISingleReferenceField<ComponentNode> field = getField(COMPONENT_FIELD);
        return field.get();
    }

    @Override
    public final IComponentVersion getPreviousVersion() {
        ISingleReferenceField<IComponentVersion> field = getField(PREVIOUS_VERSION_FIELD);
        return field.get();
    }

    @Override
    public Iterable<IGroupComponent> getGroups() {
        IReferenceField<IGroupComponent> field = getField(GROUPS_FIELD);
        return new ComponentIterable<IGroupComponent>(field);
    }

    public void addGroup(GroupComponentNode group) {
        IReferenceField<IGroupComponent> field = getField(GROUPS_FIELD);
        field.add(group, group.getCurrentVersion().getDeletionCount());
    }

    public void removeGroup(GroupComponentNode group) {
        IReferenceField<IGroupComponent> field = getField(GROUPS_FIELD);
        field.remove(group);
    }

    public final ComponentVersionNode copy(boolean copyFields) {
        IObjectNodeSchema schema = getSchema();
        IObjectSpace space = getSpace();

        ComponentVersionNode node = space.createNode(null, schema);
        initVersion(node);

        if (copyFields)
            copyFields(node);

        return node;
    }

    public final ComponentVersionNode readSnapshotVersion(long time) {
        IndexedVersionField field = getField(INDEX_FIELD);
        INodeSortedIndex<VersionTime, ComponentVersionNode> index = getSpace().getIndex(field.getSchema());
        ComponentVersionNode snapshot = index.findFloorValue(new VersionTime(getComponent().getId(), time), true);
        if (snapshot != null && snapshot.getComponent() == getComponent())
            return snapshot;
        else
            return null;
    }

    @Override
    public final void onCreated(Object primaryKey, Object[] args) {
        if (args.length != 0) {
            ComponentNode component = (ComponentNode) args[0];
            ISingleReferenceField<IComponent> componentField = getField(COMPONENT_FIELD);
            componentField.set(component);

            component.setCurrentVersion(this);

            long currentTime = getTransaction().getTime();
            INumericField timeField = getField(TIME_FIELD);
            timeField.setLong(currentTime);

            IndexedVersionField indexField = getField(INDEX_FIELD);
            indexField.set(component.getId(), currentTime);

            initFirstVersion();
        }
    }

    @Override
    public final void onOpened() {
        allowModify = false;
    }

    @Override
    public void onBeforeMigrated(Object primaryKey) {
        allowModify = true;
    }

    @Override
    public final void onMigrated() {
        IndexedVersionField indexField = getField(INDEX_FIELD);
        indexField.set(getComponent().getId(), getTime());
        allowModify = false;
    }

    @Override
    public final void onBeforeFlush() {
        allowModify = true;
        addFlags(READONLY_FLAG);
    }

    @Override
    public final void onAfterFlush() {
        allowModify = false;
    }

    @Override
    public String toString() {
        ComponentNodeSchema schema = getComponent().getSchema();
        ComponentSchemaConfiguration componentType = schema.getConfiguration().getComponent();

        List<String> flagsList = new ArrayList<String>();
        buildFlagsList(getFlags(), flagsList);
        String value = "\ncomponent:" + componentType.getName() + ", flags:" + flagsList.toString();
        return getComponent().getScope().toString() + " - " + super.toString() + value;
    }

    @Override
    public void dump(IJsonHandler json, IDumpContext context) {
        super.dump(json, context);

        json.key("scope");
        json.value(getComponent().getScope());
        json.key("component");
        json.value(getSchema().getConfiguration().getComponent().getName());

        boolean includeTime = (context.getFlags() & IDumpContext.DUMP_TIMES) != 0;

        List<String> flagsList = new ArrayList<String>();
        buildFlagsList(getFlags(), flagsList);
        if (!flagsList.isEmpty()) {
            json.key("flags");
            json.value(flagsList.toString());
        }
        if (includeTime) {
            json.key("time");
            json.value(getTime());
        }

        JsonObject options = getOptions();
        if (options != null) {
            json.key("options");
            JsonSerializers.write(json, options);
        }

        JsonObject properties = getProperties();
        if (properties != null) {
            json.key("properties");
            JsonSerializers.write(json, properties);
        }

        boolean jsonGroups = false;
        for (IGroupComponent group : getGroups()) {
            if (!jsonGroups) {
                json.key("groups");
                json.startArray();
                jsonGroups = true;
            }

            json.value(getRefId(group));
        }

        if (jsonGroups)
            json.endArray();
    }

    protected static String getRefId(IComponent node) {
        return "scope:" + node.getScope().toString() + " (" + ((ComponentNode) node).getSpace().toString() + ")";
    }

    protected void initFirstVersion() {
        getComponent().createInitialJobs();
    }

    protected void doDelete() {
    }

    protected void copyFields(ComponentVersionNode node) {
        INumericField nodeFlagsField = node.getField(FLAGS_FIELD);
        INumericField flagsField = getField(FLAGS_FIELD);
        nodeFlagsField.setInt(flagsField.getInt() & ~(READONLY_FLAG | DELETED_FLAG));

        IJsonField nodeOptionsField = node.getField(OPTIONS_FIELD);
        IJsonField nodePropertiesField = node.getField(PROPERTIES_FIELD);

        if (!isDeleted()) {
            nodeOptionsField.set(getOptions());
            nodePropertiesField.set(getProperties());
        } else {
            ComponentVersionNode prevVersion = (ComponentVersionNode) getPreviousVersion();
            nodeOptionsField.set(prevVersion.getOptions());
            nodePropertiesField.set(prevVersion.getProperties());

            getComponent().createInitialJobs();
        }

        IReferenceField<IGroupComponent> nodeGroupsField = node.getField(GROUPS_FIELD);
        for (IGroupComponent group : getGroups())
            nodeGroupsField.add(group, ((ComponentVersionNode) group.getCurrentVersion()).getDeletionCount());
    }

    protected void buildFlagsList(int flags, List<String> list) {
        if ((flags & READONLY_FLAG) != 0)
            list.add("readonly");
        if ((flags & DELETED_FLAG) != 0)
            list.add("deleted");
    }

    protected final int getFlags() {
        INumericField field = getField(FLAGS_FIELD);
        return field.getInt() & FLAGS_MASK;
    }

    protected final void setFlags(int value) {
        INumericField field = getField(FLAGS_FIELD);
        field.setInt(value);
    }

    protected final void addFlags(int value) {
        INumericField field = getField(FLAGS_FIELD);
        int flags = field.getInt();
        flags |= value;
        field.setInt(flags);
    }

    protected final void removeFlags(int value) {
        INumericField field = getField(FLAGS_FIELD);
        int flags = field.getInt();
        flags &= ~value;
        field.setInt(flags);
    }

    protected final long getSelectionTime() {
        Object operation = getNode().getTransaction().getOperation();
        if (operation instanceof ITimeSnapshotOperation) {
            ITimeSnapshotOperation snapshotOperation = (ITimeSnapshotOperation) operation;
            return snapshotOperation.getTime();
        } else if (operation instanceof IOperationWrapper && ((IOperationWrapper) operation).getOperation() instanceof ITimeSnapshotOperation) {
            ITimeSnapshotOperation snapshotOperation = (ITimeSnapshotOperation) ((IOperationWrapper) operation).getOperation();
            return snapshotOperation.getTime();
        } else
            return Times.getCurrentTime();
    }

    private void initVersion(ComponentVersionNode node) {
        INumericField nodeTimeField = node.getField(TIME_FIELD);
        long time = getTime();
        long currentTime = getTransaction().getTime();
        if (currentTime <= time)
            currentTime = time + 1;
        nodeTimeField.setLong(currentTime);

        ISingleReferenceField<IComponent> nodeComponentField = node.getField(COMPONENT_FIELD);
        nodeComponentField.set(getComponent());

        ISingleReferenceField<IComponentVersion> nodePreviousVersionField = node.getField(PREVIOUS_VERSION_FIELD);
        nodePreviousVersionField.set(this);

        IndexedVersionField nodeIndexField = node.getField(INDEX_FIELD);
        nodeIndexField.set(getComponent().getId(), currentTime);
    }
}