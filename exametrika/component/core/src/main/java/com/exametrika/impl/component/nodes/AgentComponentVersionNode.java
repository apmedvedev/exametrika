/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.nodes;

import com.exametrika.api.component.nodes.IAgentComponentVersion;
import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.api.component.nodes.IHealthComponent;
import com.exametrika.api.exadb.core.IDumpContext;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.fields.IReferenceField;
import com.exametrika.common.json.IJsonHandler;
import com.exametrika.common.json.JsonObject;
import com.exametrika.impl.component.services.HealthService;


/**
 * The {@link AgentComponentVersionNode} is an agent component version node object.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are not thread safe.
 */
public class AgentComponentVersionNode extends HealthComponentVersionNode implements IAgentComponentVersion {
    private static final int SUBCOMPONENTS_FIELD = 11;

    public AgentComponentVersionNode(INode node) {
        super(node);
    }

    @Override
    public String getTitle() {
        JsonObject properties = getProperties();
        if (properties != null) {
            String title = properties.select("nodeProperties?.title?", null);
            if (title != null)
                return title;
        }

        return getComponent().getScope().toString();
    }

    @Override
    public String getDescription() {
        JsonObject properties = getProperties();
        if (properties != null) {
            String description = properties.select("nodeProperties?.description?", null);
            if (description != null)
                return description;
        }

        return "";
    }

    @Override
    public Iterable<IComponent> getSubComponents() {
        IReferenceField<IComponent> field = getField(SUBCOMPONENTS_FIELD);
        return new ComponentIterable<IComponent>(field);
    }

    public void addSubComponent(ComponentNode node) {
        IReferenceField<ComponentNode> field = getField(SUBCOMPONENTS_FIELD);
        field.add(node, node.getCurrentVersion().getDeletionCount());
    }

    public void removeSubComponent(ComponentNode node) {
        IReferenceField<ComponentNode> field = getField(SUBCOMPONENTS_FIELD);
        field.remove(node);
    }

    @Override
    public void dump(IJsonHandler json, IDumpContext context) {
        super.dump(json, context);

        boolean jsonNodes = false;
        for (IComponent node : getSubComponents()) {
            if (!jsonNodes) {
                json.key("subComponents");
                json.startArray();
                jsonNodes = true;
            }

            json.value(getRefId(node));
        }

        if (jsonNodes)
            json.endArray();
    }

    @Override
    protected void copyFields(ComponentVersionNode node) {
        super.copyFields(node);

        IReferenceField<IComponent> nodeSubComponentsField = node.getField(SUBCOMPONENTS_FIELD);
        for (IComponent component : getSubComponents())
            nodeSubComponentsField.add(component, ((ComponentVersionNode) component.getCurrentVersion()).getDeletionCount());
    }

    @Override
    protected boolean supportsAvailability() {
        return true;
    }

    @Override
    protected boolean isComponentAvailable() {
        HealthService availabilityService = getTransaction().findDomainService(HealthService.NAME);
        return availabilityService.isAgentActive((IHealthComponent) getComponent());
    }
}