/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.schema;

import java.util.List;

import com.exametrika.api.component.config.model.ComponentSchemaConfiguration;
import com.exametrika.api.component.config.model.GroupComponentSchemaConfiguration;
import com.exametrika.api.component.config.schema.GroupComponentNodeSchemaConfiguration;
import com.exametrika.api.component.nodes.IGroupComponent;
import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.api.exadb.security.IPermission;
import com.exametrika.common.utils.ICondition;
import com.exametrika.impl.exadb.objectdb.schema.NodeSpaceSchema;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.spi.exadb.security.Permissions;


/**
 * The {@link GroupComponentNodeSchema} represents a schema of component node.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class GroupComponentNodeSchema extends HealthComponentNodeSchema {
    private ICondition<IGroupComponent> availabilityCondition;
    private IPermission editGroupsPermission;
    private IPermission editComponentsPermission;
    private IPermission executeGroupActionPermission;
    private IPermission executeGroupSelectorPermission;
    private IPermission editGroupRulesPermission;
    private IPermission editGroupAlertsPermission;

    public GroupComponentNodeSchema(GroupComponentNodeSchemaConfiguration configuration, int index, List<IFieldSchema> fields,
                                    IDocumentSchema fullTextSchema) {
        super(configuration, index, fields, fullTextSchema);
    }

    @Override
    public void resolveDependencies() {
        super.resolveDependencies();

        IDatabaseContext context = ((NodeSpaceSchema) getParent()).getContext();

        ComponentSchemaConfiguration component = getConfiguration().getComponent();

        GroupComponentSchemaConfiguration configuration = (GroupComponentSchemaConfiguration) getConfiguration().getComponent();
        if (configuration.getAvailabilityCondition() != null)
            availabilityCondition = configuration.getAvailabilityCondition().createCondition(context);
        else
            availabilityCondition = null;

        editGroupsPermission = Permissions.permission(this, "component:" + component.getName() + ":edit:groups", true);
        editComponentsPermission = Permissions.permission(this, "component:" + component.getName() + ":edit:components", true);
        executeGroupActionPermission = Permissions.permission(this, "component:" + component.getName() + ":execute:groupAction", true);
        executeGroupSelectorPermission = Permissions.permission(this, "component:" + component.getName() + ":execute:groupSelector", true);
        editGroupRulesPermission = Permissions.permission(this, "component:" + component.getName() + ":edit:groupRules", true);
        editGroupAlertsPermission = Permissions.permission(this, "component:" + component.getName() + ":edit:groupAlerts", true);
    }

    public ICondition<IGroupComponent> getAvailabilityCondition() {
        return availabilityCondition;
    }

    public IPermission getEditGroupsPermission() {
        return editGroupsPermission;
    }

    public IPermission getEditComponentsPermission() {
        return editComponentsPermission;
    }

    public IPermission getExecuteGroupActionPermission() {
        return executeGroupActionPermission;
    }

    public IPermission getExecuteGroupSelectorPermission() {
        return executeGroupSelectorPermission;
    }

    public IPermission getEditGroupRulesPermission() {
        return editGroupRulesPermission;
    }

    public IPermission getEditGroupAlertsPermission() {
        return editGroupAlertsPermission;
    }
}
