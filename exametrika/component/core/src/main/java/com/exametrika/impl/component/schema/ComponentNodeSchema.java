/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.schema;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.exametrika.api.component.config.model.ComponentSchemaConfiguration;
import com.exametrika.api.component.config.schema.ActionLogNodeSchemaConfiguration;
import com.exametrika.api.component.config.schema.ComponentNodeSchemaConfiguration;
import com.exametrika.api.component.schema.IActionSchema;
import com.exametrika.api.component.schema.ISelectorSchema;
import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.api.exadb.objectdb.schema.IObjectNodeSchema;
import com.exametrika.api.exadb.objectdb.schema.IObjectSpaceSchema;
import com.exametrika.api.exadb.security.IPermission;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.exadb.objectdb.schema.NodeSpaceSchema;
import com.exametrika.impl.exadb.objectdb.schema.ObjectNodeSchema;
import com.exametrika.spi.component.IAlert;
import com.exametrika.spi.component.IGroupDiscoveryStrategy;
import com.exametrika.spi.component.IRule;
import com.exametrika.spi.component.config.model.ActionSchemaConfiguration;
import com.exametrika.spi.component.config.model.AlertSchemaConfiguration;
import com.exametrika.spi.component.config.model.GroupDiscoveryStrategySchemaConfiguration;
import com.exametrika.spi.component.config.model.RuleSchemaConfiguration;
import com.exametrika.spi.component.config.model.SelectorSchemaConfiguration;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.spi.exadb.security.Permissions;


/**
 * The {@link ComponentNodeSchema} represents a schema of component node.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class ComponentNodeSchema extends ObjectNodeSchema {
    private ComponentVersionNodeSchema version;
    private IObjectNodeSchema actionLog;
    private IFieldSchema indexField;
    private Map<String, IActionSchema> actions;
    private List<IRule> rules;
    private Map<String, ISelectorSchema> selectors;
    private List<IAlert> alerts;
    private List<IGroupDiscoveryStrategy> groupDiscoveryStrategies;
    private IPermission viewPermission;
    private IPermission editOptionsPermission;
    private IPermission editRulesPermission;
    private IPermission editAlertsPermission;
    private IPermission editJobsPermission;
    private IPermission cancelJobsPermission;
    private IPermission editTagsPermission;
    private IPermission deletePermission;

    public ComponentNodeSchema(ComponentNodeSchemaConfiguration configuration, int index, List<IFieldSchema> fields,
                               IDocumentSchema fullTextSchema) {
        super(configuration, index, fields, fullTextSchema);

        indexField = fields.get(0);
    }

    @Override
    public ComponentNodeSchemaConfiguration getConfiguration() {
        return (ComponentNodeSchemaConfiguration) super.getConfiguration();
    }

    @Override
    public void resolveDependencies() {
        super.resolveDependencies();

        IDatabaseContext context = ((NodeSpaceSchema) getParent()).getContext();

        ComponentSchemaConfiguration component = getConfiguration().getComponent();
        IObjectSpaceSchema spaceSchema = (IObjectSpaceSchema) getParent();
        version = spaceSchema.findNode(getConfiguration().getName() + "Version");
        Assert.notNull(version);

        actionLog = spaceSchema.findNode(ActionLogNodeSchemaConfiguration.NAME);
        Assert.notNull(actionLog);

        actions = new LinkedHashMap<String, IActionSchema>();
        for (ActionSchemaConfiguration action : component.getActions().values())
            actions.put(action.getName(), action.createSchema(this, context));

        rules = new ArrayList<IRule>();
        for (RuleSchemaConfiguration ruleConfiguration : component.getRules().values()) {
            if (!ruleConfiguration.isEnabled())
                continue;

            IRule rule = ruleConfiguration.createRule(context);
            rules.add(rule);
        }

        selectors = new LinkedHashMap<String, ISelectorSchema>();
        for (SelectorSchemaConfiguration selector : component.getSelectors().values())
            selectors.put(selector.getName(), selector.createSchema(this, context));

        alerts = new ArrayList<IAlert>();
        for (AlertSchemaConfiguration alertConfiguration : component.getAlerts().values()) {
            if (!alertConfiguration.isEnabled())
                continue;

            IAlert alert = alertConfiguration.createAlert(context);
            alerts.add(alert);
        }

        groupDiscoveryStrategies = new ArrayList<IGroupDiscoveryStrategy>();
        for (GroupDiscoveryStrategySchemaConfiguration strategyConfiguration : component.getGroupDiscoveryStrategies()) {
            IGroupDiscoveryStrategy strategy = strategyConfiguration.createStrategy(context);
            groupDiscoveryStrategies.add(strategy);
        }

        viewPermission = Permissions.permission(this, "component:" + component.getName() + ":view", false);
        editOptionsPermission = Permissions.permission(this, "component:" + component.getName() + ":edit:options", true);
        editRulesPermission = Permissions.permission(this, "component:" + component.getName() + ":edit:rules", true);
        editAlertsPermission = Permissions.permission(this, "component:" + component.getName() + ":edit:alerts", true);
        editJobsPermission = Permissions.permission(this, "component:" + component.getName() + ":edit:jobs", true);
        cancelJobsPermission = Permissions.permission(this, "component:" + component.getName() + ":execute:job:cancel", true);
        editTagsPermission = Permissions.permission(this, "component:" + component.getName() + ":edit:tags", true);
        deletePermission = Permissions.permission(this, "component:" + component.getName() + ":delete", true);
    }

    public ComponentVersionNodeSchema getVersion() {
        return version;
    }

    public IObjectNodeSchema getActionLog() {
        return actionLog;
    }

    public IFieldSchema getIndexField() {
        return indexField;
    }

    public Map<String, IActionSchema> getActions() {
        return actions;
    }

    public List<IRule> getRules() {
        return rules;
    }

    public Map<String, ISelectorSchema> getSelectors() {
        return selectors;
    }

    public List<IAlert> getAlerts() {
        return alerts;
    }

    public List<IGroupDiscoveryStrategy> getGroupDiscoveryStrategies() {
        return groupDiscoveryStrategies;
    }

    public IPermission getViewPermission() {
        return viewPermission;
    }

    public IPermission getEditOptionsPermission() {
        return editOptionsPermission;
    }

    public IPermission getEditRulesPermission() {
        return editRulesPermission;
    }

    public IPermission getEditAlertsPermission() {
        return editAlertsPermission;
    }

    public IPermission getEditJobsPermission() {
        return editJobsPermission;
    }

    public IPermission getCancelJobsPermission() {
        return cancelJobsPermission;
    }

    public IPermission getEditTagsPermission() {
        return editTagsPermission;
    }

    public IPermission getDeletePermission() {
        return deletePermission;
    }
}
