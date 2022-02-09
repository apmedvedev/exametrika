/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.nodes;

import java.util.List;

import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.api.component.nodes.IGroupComponent;
import com.exametrika.api.component.nodes.IGroupComponentVersion;
import com.exametrika.api.exadb.core.IDumpContext;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.fields.IReferenceField;
import com.exametrika.api.exadb.objectdb.fields.ISingleReferenceField;
import com.exametrika.common.json.IJsonHandler;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.component.schema.GroupComponentNodeSchema;


/**
 * The {@link GroupComponentVersionNode} is a group component version node object.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are not thread safe.
 */
public class GroupComponentVersionNode extends HealthComponentVersionNode implements IGroupComponentVersion {
    private static final int PREDEFINED_FLAG = 0x80;
    private static final int PARENT_FIELD = 11;
    private static final int CHILDREN_FIELD = 12;
    private static final int COMPONENTS_FIELD = 13;

    private boolean disableChecks;

    public GroupComponentVersionNode(INode node) {
        super(node);
    }

    @Override
    public boolean isPredefined() {
        return (getFlags() & PREDEFINED_FLAG) != 0;
    }

    public void setPredefined() {
        addFlags(PREDEFINED_FLAG);
    }

    @Override
    public GroupComponentNode getParent() {
        ISingleReferenceField<GroupComponentNode> field = getField(PARENT_FIELD);
        GroupComponentNode parent = field.get();
        Assert.checkState(parent == null || parent.isAccessAlowed());

        return parent;
    }

    @Override
    public Iterable<IGroupComponent> getChildren() {
        IReferenceField<IGroupComponent> field = getField(CHILDREN_FIELD);
        return new ComponentIterable<IGroupComponent>(field);
    }

    @Override
    public Iterable<IComponent> getComponents() {
        IReferenceField<IComponent> field = getField(COMPONENTS_FIELD);
        return new ComponentIterable<IComponent>(field);
    }

    public void addChild(GroupComponentNode child) {
        IReferenceField<IGroupComponent> field = getField(CHILDREN_FIELD);
        field.add(child, child.getCurrentVersion().getDeletionCount());

        GroupComponentVersionNode childVersion = (GroupComponentVersionNode) child.addVersion();

        ISingleReferenceField<GroupComponentNode> parentField = childVersion.getField(PARENT_FIELD);
        Assert.checkState(parentField.get() == null);
        parentField.set((GroupComponentNode) getComponent());
    }

    public void removeChild(GroupComponentNode child) {
        IReferenceField<IGroupComponent> field = getField(CHILDREN_FIELD);
        field.remove(child);
    }

    public void removeAllChildren() {
        IReferenceField<GroupComponentNode> field = getField(CHILDREN_FIELD);
        field.clear();
    }

    public void addComponent(ComponentNode component) {
        IReferenceField<ComponentNode> field = getField(COMPONENTS_FIELD);
        field.add(component, component.getCurrentVersion().getDeletionCount());

        component.addGroup((GroupComponentNode) getComponent());
    }

    public void removeComponent(ComponentNode component) {
        component.removeGroup((GroupComponentNode) getComponent());

        IReferenceField<ComponentNode> field = getField(COMPONENTS_FIELD);
        field.remove(component);
    }

    public void removeAllComponents() {
        for (IComponent component : getComponents())
            ((ComponentNode) component).removeGroup((GroupComponentNode) getComponent());

        IReferenceField<ComponentNode> field = getField(COMPONENTS_FIELD);
        field.clear();
    }

    @Override
    public void dump(IJsonHandler json, IDumpContext context) {
        super.dump(json, context);

        boolean jsonChildren = false;
        for (IGroupComponent group : getChildren()) {
            if (!jsonChildren) {
                json.key("children");
                json.startArray();
                jsonChildren = true;
            }

            json.value(getRefId(group));
        }

        if (jsonChildren)
            json.endArray();

        boolean jsonComponents = false;
        for (IComponent component : getComponents()) {
            if (!jsonComponents) {
                json.key("components");
                json.startArray();
                jsonComponents = true;
            }

            json.value(getRefId(component));
        }

        if (jsonComponents)
            json.endArray();
    }

    @Override
    protected void copyFields(ComponentVersionNode node) {
        if (isDeleted())
            disableChecks = true;

        super.copyFields(node);

        disableChecks = false;

        ISingleReferenceField<GroupComponentNode> nodeParentField = node.getField(PARENT_FIELD);
        nodeParentField.set(getParent());

        IReferenceField<GroupComponentNode> nodeChildrenField = node.getField(CHILDREN_FIELD);
        for (IGroupComponent group : getChildren())
            nodeChildrenField.add((GroupComponentNode) group, ((ComponentVersionNode) group.getCurrentVersion()).getDeletionCount());

        IReferenceField<ComponentNode> nodeComponentsField = node.getField(COMPONENTS_FIELD);
        for (IComponent component : getComponents())
            nodeComponentsField.add((ComponentNode) component, ((ComponentVersionNode) component.getCurrentVersion()).getDeletionCount());
    }

    @Override
    protected void buildFlagsList(int flags, List<String> list) {
        super.buildFlagsList(flags, list);
        if ((flags & PREDEFINED_FLAG) != 0)
            list.add("predefined");
    }

    @Override
    protected boolean supportsAvailability() {
        GroupComponentNode component = (GroupComponentNode) getComponent();
        return component.supportsAvailability();
    }

    @Override
    protected boolean isComponentAvailable() {
        if (disableChecks)
            return true;

        GroupComponentNode component = (GroupComponentNode) getComponent();
        GroupComponentNodeSchema schema = component.getSchema();
        return schema.getAvailabilityCondition().evaluate(component);
    }

    @Override
    protected void initFirstVersion() {
        disableChecks = true;
        super.initFirstVersion();
        disableChecks = false;
    }
}