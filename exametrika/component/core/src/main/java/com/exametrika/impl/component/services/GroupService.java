/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.services;

import java.util.ArrayList;
import java.util.Map;

import com.exametrika.api.aggregator.IPeriodName;
import com.exametrika.api.aggregator.IPeriodNameManager;
import com.exametrika.api.aggregator.common.model.IScopeName;
import com.exametrika.api.aggregator.common.model.Names;
import com.exametrika.api.component.config.model.ComponentModelSchemaConfiguration;
import com.exametrika.api.component.config.model.PredefinedGroupSchemaConfiguration;
import com.exametrika.api.component.nodes.IGroupComponent;
import com.exametrika.api.exadb.jobs.config.model.JobSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.IObjectSpace;
import com.exametrika.api.exadb.objectdb.schema.IObjectNodeSchema;
import com.exametrika.api.exadb.objectdb.schema.IObjectSpaceSchema;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.component.nodes.ComponentRootNode;
import com.exametrika.impl.component.nodes.GroupComponentNode;
import com.exametrika.impl.component.schema.GroupServiceSchema;
import com.exametrika.spi.component.config.model.AlertSchemaConfiguration;
import com.exametrika.spi.component.config.model.RuleSchemaConfiguration;
import com.exametrika.spi.exadb.core.DomainService;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link GroupService} is a group service implementation.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public final class GroupService extends DomainService {
    public static final String NAME = "component.GroupService";
    private IObjectSpaceSchema spaceSchema;

    public void setSchema(ComponentModelSchemaConfiguration oldComponentModel, ComponentModelSchemaConfiguration newComponentModel) {
        Map<String, PredefinedGroupSchemaConfiguration> oldGroups = null;
        if (oldComponentModel != null)
            oldGroups = oldComponentModel.getGroups();

        ensureSpace();
        IObjectSpace space = spaceSchema.getSpace();
        IPeriodNameManager nameManager = space.getTransaction().findExtension(IPeriodNameManager.NAME);
        synchronizeGroups(space, nameManager, ((ComponentRootNode) space.getRootNode()).getRootGroup(), oldGroups,
                newComponentModel.getGroups());
    }

    @Override
    public void start(IDatabaseContext context) {
        super.start(context);

        ComponentModelSchemaConfiguration componentModel = ((GroupServiceSchema) this.schema).getComponentModel();
        setSchema(null, componentModel);
    }

    @Override
    public void clearCaches() {
        spaceSchema = null;
    }

    private void synchronizeGroups(IObjectSpace space, IPeriodNameManager nameManager, IGroupComponent parent,
                                   Map<String, PredefinedGroupSchemaConfiguration> oldGroups, Map<String, PredefinedGroupSchemaConfiguration> newGroups) {
        if (oldGroups != null) {
            for (PredefinedGroupSchemaConfiguration oldGroup : oldGroups.values()) {
                PredefinedGroupSchemaConfiguration newGroup = newGroups.get(oldGroup.getName());
                if (newGroup != null) {
                    Assert.isTrue(oldGroup.getGroupType().equals(newGroup.getGroupType()));
                    continue;
                }

                IPeriodName name = nameManager.findByName(Names.getScope(getScope(parent, oldGroup.getName())));
                if (name == null)
                    continue;

                IObjectNodeSchema schema = space.getSchema().findNode(oldGroup.getGroupType());
                GroupComponentNode node = space.findNode(name.getId(), schema);
                if (node == null)
                    continue;

                node.deletePredefined();
            }
        }

        for (PredefinedGroupSchemaConfiguration group : newGroups.values()) {
            IPeriodName name = nameManager.addName(Names.getScope(getScope(parent, group.getName())));
            IObjectNodeSchema schema = space.getSchema().findNode(group.getGroupType());
            boolean exists = space.containsNode(name.getId(), schema);
            GroupComponentNode node = space.findOrCreateNode(name.getId(), schema);

            if (!exists || node.getCurrentVersion().isDeleted()) {
                node.setPredefined();
                parent.addChild(node);
            }

            node.setOptions(group.getOptions());
            node.setTags(new ArrayList<String>(group.getTags()));
            for (RuleSchemaConfiguration rule : group.getRules().values())
                node.addRule(rule);
            for (AlertSchemaConfiguration alert : group.getAlerts().values())
                node.addAlert(alert);
            for (RuleSchemaConfiguration rule : group.getGroupRules().values())
                node.addGroupRule(rule);
            for (AlertSchemaConfiguration alert : group.getGroupAlerts().values())
                node.addGroupAlert(alert);
            for (JobSchemaConfiguration job : group.getJobs().values())
                node.addJob(job);

            Map<String, PredefinedGroupSchemaConfiguration> childOldGroups = null;
            if (oldGroups != null) {
                PredefinedGroupSchemaConfiguration oldGroup = oldGroups.get(group.getName());
                if (oldGroup != null)
                    childOldGroups = oldGroup.getGroups();
            }

            synchronizeGroups(space, nameManager, node, childOldGroups, group.getGroups());
        }
    }

    private String getScope(IGroupComponent parent, String name) {
        IScopeName parentScope = parent.getScope();
        if (parentScope.isEmpty())
            return name;
        else
            return parentScope.toString() + "." + name;
    }

    private void ensureSpace() {
        if (spaceSchema == null) {
            spaceSchema = schema.getParent().findSpace("component");
            Assert.notNull(spaceSchema);
        }
    }
}
