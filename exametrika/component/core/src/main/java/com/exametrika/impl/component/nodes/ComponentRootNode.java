/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.nodes;

import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.api.component.nodes.IGroupComponent;
import com.exametrika.api.component.nodes.IHealthComponent;
import com.exametrika.api.component.nodes.IHostComponent;
import com.exametrika.api.component.nodes.IIncident;
import com.exametrika.api.component.nodes.INodeComponent;
import com.exametrika.api.component.nodes.ITransactionComponent;
import com.exametrika.api.exadb.core.IDumpContext;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.fields.IReferenceField;
import com.exametrika.api.exadb.objectdb.fields.ISingleReferenceField;
import com.exametrika.api.exadb.objectdb.fields.IStructuredBlobField;
import com.exametrika.common.json.IJsonHandler;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.component.schema.ComponentRootNodeSchema;
import com.exametrika.impl.exadb.objectdb.ObjectNodeObject;
import com.exametrika.spi.component.IVersionChangeRecord;
import com.exametrika.spi.exadb.objectdb.INodeObject;


/**
 * The {@link ComponentRootNode} is a component node object.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class ComponentRootNode extends ObjectNodeObject {
    protected static final int BLOB_STORE_FIELD = 0;
    private static final int ROOT_GROUP_FIELD = 1;
    private static final int HOSTS_FIELD = 2;
    private static final int NODES_FIELD = 3;
    private static final int TRANSACTIONS_FIELD = 4;
    private static final int HEALTH_COMPONENTS_FIELD = 5;
    private static final int INCIDENTS_FIELD = 6;
    protected static final int INCIDENT_SEQUENCE_FIELD = 7;
    private static final int VERSION_CHANGES_FIELD = 8;

    public ComponentRootNode(INode node) {
        super(node);
    }

    public IGroupComponent getRootGroup() {
        ISingleReferenceField<IGroupComponent> field = getField(ROOT_GROUP_FIELD);
        return field.get();
    }

    public IStructuredBlobField<IVersionChangeRecord> getVersionChanges() {
        IStructuredBlobField<IVersionChangeRecord> field = getField(VERSION_CHANGES_FIELD);
        return field;
    }

    public Iterable<IHostComponent> getHosts() {
        IReferenceField<IHostComponent> field = getField(HOSTS_FIELD);
        return field;
    }

    public void addHost(IHostComponent component) {
        Assert.notNull(component);

        IReferenceField<IHostComponent> field = getField(HOSTS_FIELD);
        field.add(component);
    }

    public void removeHost(IHostComponent component) {
        Assert.notNull(component);

        IReferenceField<IHostComponent> field = getField(HOSTS_FIELD);
        field.remove(component);
    }

    public Iterable<INodeComponent> getNodes() {
        IReferenceField<INodeComponent> field = getField(NODES_FIELD);
        return field;
    }

    public void addNode(INodeComponent component) {
        Assert.notNull(component);

        IReferenceField<INodeComponent> field = getField(NODES_FIELD);
        field.add(component);
    }

    public void removeNode(INodeComponent component) {
        Assert.notNull(component);

        IReferenceField<INodeComponent> field = getField(NODES_FIELD);
        field.remove(component);
    }

    public Iterable<ITransactionComponent> getTransactions() {
        IReferenceField<ITransactionComponent> field = getField(TRANSACTIONS_FIELD);
        return field;
    }

    public void addTransaction(ITransactionComponent component) {
        Assert.notNull(component);

        IReferenceField<ITransactionComponent> field = getField(TRANSACTIONS_FIELD);
        field.add(component);
    }

    public void removeTransaction(ITransactionComponent component) {
        Assert.notNull(component);

        IReferenceField<ITransactionComponent> field = getField(TRANSACTIONS_FIELD);
        field.remove(component);
    }

    public Iterable<IHealthComponent> getHealthComponents() {
        IReferenceField<IHealthComponent> field = getField(HEALTH_COMPONENTS_FIELD);
        return field;
    }

    public void addHealthComponent(IHealthComponent component) {
        Assert.notNull(component);

        IReferenceField<IHealthComponent> field = getField(HEALTH_COMPONENTS_FIELD);
        field.add(component);
    }

    public void removeHealthComponent(IHealthComponent component) {
        Assert.notNull(component);

        IReferenceField<IHealthComponent> field = getField(HEALTH_COMPONENTS_FIELD);
        field.remove(component);
    }

    public Iterable<IIncident> getIncidents() {
        IReferenceField<IIncident> field = getField(INCIDENTS_FIELD);
        return field;
    }

    public void addIncident(IIncident incident) {
        IReferenceField<IIncident> field = getField(INCIDENTS_FIELD);
        field.add(incident);
    }

    public void removeIncident(IIncident incident) {
        IReferenceField<IIncident> field = getField(INCIDENTS_FIELD);
        field.remove(incident);
    }

    @Override
    public void onCreated(Object primaryKey, Object[] args) {
        ISingleReferenceField<IGroupComponent> field = getField(ROOT_GROUP_FIELD);
        IGroupComponent rootGroup = getSpace().createNode(0, getSchema().getParent().findNode("RootGroup"));
        field.set(rootGroup);
    }

    @Override
    public void dump(IJsonHandler json, IDumpContext context) {
        super.dump(json, context);

        ComponentRootNodeSchema schema = (ComponentRootNodeSchema) getSchema();
        schema.setDeletedComponentFiltered(false);

        json.key("rootGroup");
        json.startObject();
        ((GroupComponentNode) getRootGroup()).dump(json, context);
        json.endObject();

        boolean jsonHosts = false;
        for (IHostComponent component : getHosts()) {
            if (!jsonHosts) {
                json.key("hosts");
                json.startArray();
                jsonHosts = true;
            }

            json.value(ComponentVersionNode.getRefId(component));
        }

        if (jsonHosts)
            json.endArray();

        boolean jsonNodes = false;
        for (INodeComponent component : getNodes()) {
            if (!jsonNodes) {
                json.key("nodes");
                json.startArray();
                jsonNodes = true;
            }

            json.value(ComponentVersionNode.getRefId(component));
        }

        if (jsonNodes)
            json.endArray();

        boolean jsonTransactions = false;
        for (ITransactionComponent component : getTransactions()) {
            if (!jsonTransactions) {
                json.key("transactions");
                json.startArray();
                jsonTransactions = true;
            }

            json.value(ComponentVersionNode.getRefId(component));
        }

        if (jsonTransactions)
            json.endArray();

        boolean jsonHealthComponents = false;
        for (IComponent component : getHealthComponents()) {
            if (!jsonHealthComponents) {
                json.key("healthComponents");
                json.startArray();
                jsonHealthComponents = true;
            }

            json.value(ComponentVersionNode.getRefId(component));
        }

        if (jsonHealthComponents)
            json.endArray();

        boolean jsonIncidents = false;
        for (IIncident alert : getIncidents()) {
            if (!jsonIncidents) {
                json.key("incidents");
                json.startArray();
                jsonIncidents = true;
            }

            json.value(alert.toString());
        }

        if (jsonIncidents)
            json.endArray();

        json.key("components");
        json.startArray();
        for (INodeObject node : getSpace().<INodeObject>getNodes()) {
            if (!(node instanceof IComponent))
                continue;

            if (!context.isNodeTraversed(node.getNode().getId())) {
                json.startObject();
                node.dump(json, context);
                json.endObject();
            }
        }
        json.endArray();

        schema.setDeletedComponentFiltered(true);
    }
}