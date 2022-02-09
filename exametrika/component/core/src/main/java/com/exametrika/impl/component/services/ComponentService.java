/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.services;

import com.exametrika.api.aggregator.IPeriodName;
import com.exametrika.api.aggregator.IPeriodNameManager;
import com.exametrika.api.aggregator.common.model.Names;
import com.exametrika.api.component.IComponentService;
import com.exametrika.api.component.config.model.ComponentModelSchemaConfiguration;
import com.exametrika.api.component.nodes.IBehaviorType;
import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.api.component.nodes.IGroupComponent;
import com.exametrika.api.component.nodes.IIncident;
import com.exametrika.api.exadb.core.schema.IDomainServiceSchema;
import com.exametrika.api.exadb.objectdb.INodeIndex;
import com.exametrika.api.exadb.objectdb.schema.INodeSchema;
import com.exametrika.api.exadb.objectdb.schema.IObjectNodeSchema;
import com.exametrika.api.exadb.objectdb.schema.IObjectSpaceSchema;
import com.exametrika.api.exadb.security.IPermission;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.component.nodes.BehaviorTypeNode;
import com.exametrika.impl.component.nodes.ComponentNode;
import com.exametrika.impl.component.nodes.ComponentRootNode;
import com.exametrika.impl.component.nodes.GroupComponentNode;
import com.exametrika.impl.component.nodes.IncidentIterable;
import com.exametrika.impl.component.schema.ComponentServiceSchema;
import com.exametrika.spi.component.IAgentSchemaUpdater;
import com.exametrika.spi.exadb.core.DomainService;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link ComponentService} is a component service implementation.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ComponentService extends DomainService implements IComponentService {
    private IObjectSpaceSchema spaceSchema;
    private INodeIndex<Long, ComponentNode> componentIndex;
    private IObjectNodeSchema behaviorTypeNodeSchema;
    private IPeriodNameManager nameManager;
    private boolean started;

    public IObjectSpaceSchema getObjectSpaceSchema() {
        ensureSpace();
        return spaceSchema;
    }

    @Override
    public ComponentServiceSchema getSchema() {
        return (ComponentServiceSchema) schema;
    }

    @Override
    public void setSchema(IDomainServiceSchema schema) {
        ComponentModelSchemaConfiguration oldComponentModel = null;
        if (this.schema != null)
            oldComponentModel = ((ComponentServiceSchema) this.schema).getConfiguration().getComponentModel();

        super.setSchema(schema);

        if (started) {
            ComponentModelSchemaConfiguration newComponentModel = ((ComponentServiceSchema) schema).getConfiguration().getComponentModel();

            HealthService healthService = context.getTransactionProvider().getTransaction().findDomainService(HealthService.NAME);
            healthService.setSchema(newComponentModel);

            GroupService groupService = context.getTransactionProvider().getTransaction().findDomainService(GroupService.NAME);
            groupService.setSchema(oldComponentModel, newComponentModel);

            if (!Objects.equals(oldComponentModel, newComponentModel)) {
                IAgentSchemaUpdater schemaUpdater = context.getDatabase().findParameter(IAgentSchemaUpdater.NAME);
                if (schemaUpdater != null)
                    schemaUpdater.onSchemaChanged(newComponentModel);
            }
        }
    }

    @Override
    public IGroupComponent getRootGroup() {
        ensureSpace();

        ComponentRootNode root = spaceSchema.getSpace().getRootNode();
        GroupComponentNode rootGroup = (GroupComponentNode) root.getRootGroup();
        Assert.checkState(rootGroup.isAccessAlowed());
        return rootGroup;
    }

    @Override
    public Iterable<IIncident> getIncidents() {
        ensureSpace();

        ComponentRootNode root = spaceSchema.getSpace().getRootNode();
        return new IncidentIterable(root.getIncidents());
    }

    @Override
    public IComponent findComponent(String scopeName) {
        ensureSpace();

        IPeriodName name = nameManager.findByName(Names.getScope(scopeName));
        if (name == null)
            return null;

        ComponentNode component = componentIndex.find(name.getId());
        if (component != null && component.isAccessAlowed())
            return component;
        else
            return null;
    }

    @Override
    public IComponent findComponent(long scopeId) {
        ensureSpace();

        ComponentNode component = componentIndex.find(scopeId);
        if (component != null && component.isAccessAlowed())
            return component;
        else
            return null;
    }

    @Override
    public IGroupComponent createGroup(String scopeName, String groupType) {
        IPermission permission = getSchema().getCreateGroupsPermission();
        permission.beginCheck(null);
        try {
            ensureSpace();

            INodeSchema groupSchema = spaceSchema.findNode(groupType);
            Assert.notNull(groupSchema);

            IPeriodName name = nameManager.addName(Names.getScope(scopeName));
            return spaceSchema.getSpace().createNode(name.getId(), groupSchema);
        } finally {
            permission.endCheck();
        }
    }

    @Override
    public IBehaviorType findBehaviorType(long typeId) {
        ensureSpace();

        INodeIndex<Long, BehaviorTypeNode> index = spaceSchema.getSpace().getIndex(behaviorTypeNodeSchema.getPrimaryField());
        BehaviorTypeNode behaviorType = index.find(typeId);
        if (behaviorType != null && behaviorType.isAccessAlowed())
            return behaviorType;
        else
            return null;
    }

    @Override
    public void clearCaches() {
        spaceSchema = null;
        nameManager = null;
        componentIndex = null;
        behaviorTypeNodeSchema = null;
    }

    @Override
    public void start(IDatabaseContext context) {
        super.start(context);

        started = true;
    }

    private void ensureSpace() {
        if (spaceSchema == null) {
            spaceSchema = schema.getParent().findSpace("component");
            Assert.notNull(spaceSchema);

            behaviorTypeNodeSchema = spaceSchema.findNode("BehaviorType");
            Assert.notNull(behaviorTypeNodeSchema);

            nameManager = context.getTransactionProvider().getTransaction().findExtension(IPeriodNameManager.NAME);
            Assert.notNull(nameManager);

            componentIndex = spaceSchema.getSpace().findIndex("componentIndex");
            Assert.checkState(componentIndex != null);
        }
        ;
    }
}
