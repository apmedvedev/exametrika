/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.nodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.exametrika.api.component.IAction;
import com.exametrika.api.component.ISelector;
import com.exametrika.api.component.config.model.GroupComponentSchemaConfiguration;
import com.exametrika.api.component.nodes.IHealthComponentVersion.State;
import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.api.component.nodes.IGroupComponent;
import com.exametrika.api.component.nodes.IGroupComponentVersion;
import com.exametrika.api.component.nodes.IIncident;
import com.exametrika.api.exadb.core.IDumpContext;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.fields.ISerializableField;
import com.exametrika.api.exadb.security.IPermission;
import com.exametrika.common.json.IJsonHandler;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.component.actions.GroupAction;
import com.exametrika.impl.component.schema.GroupComponentNodeSchema;
import com.exametrika.impl.component.selectors.GroupSelector;
import com.exametrika.impl.exadb.objectdb.schema.NodeSpaceSchema;
import com.exametrika.spi.component.IVersionChangeRecord;
import com.exametrika.spi.component.config.model.AlertSchemaConfiguration;
import com.exametrika.spi.component.config.model.RuleSchemaConfiguration;


/**
 * The {@link GroupComponentNode} is a group component node object.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are not thread safe.
 */
public class GroupComponentNode extends HealthComponentNode implements IGroupComponent {
    private static final int GROUP_RULES_FIELD = 9;
    private static final int GROUP_ALERTS_FIELD = 10;

    public GroupComponentNode(INode node) {
        super(node);
    }

    @Override
    public GroupComponentNodeSchema getSchema() {
        return (GroupComponentNodeSchema) super.getSchema();
    }

    public void setPredefined() {
        if (((GroupComponentVersionNode) getCurrentVersion()).isPredefined())
            return;

        GroupComponentVersionNode node = (GroupComponentVersionNode) addVersion();
        node.setPredefined();
    }

    @Override
    public void addChild(IGroupComponent child) {
        Assert.isTrue(!child.getCurrentVersion().isDeleted());
        GroupComponentSchemaConfiguration componentType = (GroupComponentSchemaConfiguration) getSchema().getConfiguration().getComponent();
        Assert.isTrue(componentType.isAllowGroups());
        if (componentType.getGroupTypes() != null)
            Assert.isTrue(componentType.getGroupTypes().contains(((GroupComponentNode) child).getSchema().getConfiguration().getComponent().getName()));

        IPermission permission = getSchema().getEditGroupsPermission();
        permission.beginCheck(this);

        GroupComponentVersionNode node = (GroupComponentVersionNode) addVersion();
        node.addChild((GroupComponentNode) child);

        checkAvailability();

        addVersionChange((GroupComponentNode) child, this, IVersionChangeRecord.Type.ADD);

        permission.endCheck();
    }

    @Override
    public void removeChild(IGroupComponent child) {
        IPermission permission = getSchema().getEditGroupsPermission();
        permission.beginCheck(this);

        child.delete();

        GroupComponentVersionNode node = (GroupComponentVersionNode) addVersion();
        node.removeChild((GroupComponentNode) child);

        checkAvailability();
        checkDynamicAutoDeletion();

        permission.endCheck();
    }

    @Override
    public void removeAllChildren() {
        IPermission permission = getSchema().getEditGroupsPermission();
        permission.beginCheck(this);

        for (IGroupComponent component : ((GroupComponentVersionNode) getCurrentVersion()).getChildren())
            component.delete();

        GroupComponentVersionNode node = (GroupComponentVersionNode) addVersion();
        node.removeAllChildren();

        checkAvailability();
        checkDynamicAutoDeletion();

        permission.endCheck();
    }

    @Override
    public void addComponent(IComponent component) {
        Assert.isTrue(!component.getCurrentVersion().isDeleted());
        GroupComponentSchemaConfiguration componentType = (GroupComponentSchemaConfiguration) getSchema().getConfiguration().getComponent();
        Assert.isTrue(componentType.isAllowComponents());
        if (componentType.getComponentTypes() != null)
            Assert.isTrue(componentType.getComponentTypes().contains(((ComponentNode) component).getSchema().getConfiguration().getComponent().getName()));

        IPermission permission = getSchema().getEditComponentsPermission();
        permission.beginCheck(this);

        GroupComponentVersionNode node = (GroupComponentVersionNode) addVersion();
        node.addComponent((ComponentNode) component);

        checkAvailability();

        permission.endCheck();
    }

    @Override
    public void removeComponent(IComponent component) {
        IPermission permission = getSchema().getEditComponentsPermission();
        permission.beginCheck(this);

        GroupComponentVersionNode node = (GroupComponentVersionNode) addVersion();
        node.removeComponent((ComponentNode) component);

        checkAvailability();
        checkDynamicAutoDeletion();

        permission.endCheck();
    }

    @Override
    public void removeAllComponents() {
        IPermission permission = getSchema().getEditComponentsPermission();
        permission.beginCheck(this);

        GroupComponentVersionNode node = (GroupComponentVersionNode) addVersion();
        node.removeAllComponents();

        checkAvailability();
        checkDynamicAutoDeletion();

        permission.endCheck();
    }

    @Override
    public <T extends IAction> T createGroupAction(String name, boolean recursive) {
        IPermission permission = getSchema().getExecuteGroupActionPermission();
        permission.beginCheck(this);

        T result = (T) new GroupAction(this, name, recursive, ((NodeSpaceSchema) getSpace().getSchema()).getContext());

        permission.endCheck();
        return result;
    }

    @Override
    public <T extends ISelector> T createGroupSelector(String name) {
        IPermission permission = getSchema().getExecuteGroupSelectorPermission();
        permission.beginCheck(this);

        T result = (T) new GroupSelector(this, name, ((NodeSpaceSchema) getSpace().getSchema()).getContext());

        permission.endCheck();
        return result;
    }

    @Override
    public Iterable<RuleSchemaConfiguration> getGroupRules() {
        ISerializableField<List<RuleSchemaConfiguration>> field = getField(GROUP_RULES_FIELD);
        List<RuleSchemaConfiguration> list = field.get();
        if (list != null)
            return list;
        else
            return Collections.emptyList();
    }

    @Override
    public RuleSchemaConfiguration findGroupRule(String ruleName) {
        Assert.notNull(ruleName);

        ISerializableField<List<RuleSchemaConfiguration>> field = getField(GROUP_RULES_FIELD);
        List<RuleSchemaConfiguration> rules = field.get();
        if (rules != null) {
            for (int i = 0; i < rules.size(); i++) {
                RuleSchemaConfiguration rule = rules.get(i);
                if (rule.getName().equals(ruleName))
                    return rule;
            }
        }

        return null;
    }

    @Override
    public void addGroupRule(RuleSchemaConfiguration ruleConfiguration) {
        Assert.notNull(ruleConfiguration);

        IPermission permission = getSchema().getEditGroupRulesPermission();
        permission.beginCheck(this);

        ISerializableField<List<RuleSchemaConfiguration>> field = getField(GROUP_RULES_FIELD);
        List<RuleSchemaConfiguration> rules = field.get();
        if (rules == null)
            rules = new ArrayList<RuleSchemaConfiguration>();

        boolean set = false;
        for (int i = 0; i < rules.size(); i++) {
            RuleSchemaConfiguration rule = rules.get(i);
            if (rule.getName().equals(ruleConfiguration.getName())) {
                rules.set(i, ruleConfiguration);
                set = true;
                break;
            }
        }

        if (!set)
            rules.add(ruleConfiguration);

        field.set(rules);

        invalidateComponentRuleCaches();

        permission.endCheck();
    }

    @Override
    public void removeGroupRule(String ruleName) {
        Assert.notNull(ruleName);

        IPermission permission = getSchema().getEditGroupRulesPermission();
        permission.beginCheck(this);

        ISerializableField<List<RuleSchemaConfiguration>> field = getField(GROUP_RULES_FIELD);
        List<RuleSchemaConfiguration> rules = field.get();
        if (rules != null) {
            for (int i = 0; i < rules.size(); i++) {
                RuleSchemaConfiguration rule = rules.get(i);
                if (rule.getName().equals(ruleName)) {
                    rules.remove(i);
                    break;
                }
            }
        }

        field.set(rules);

        invalidateComponentRuleCaches();

        permission.endCheck();
    }

    @Override
    public void removeAllGroupRules() {
        IPermission permission = getSchema().getEditGroupRulesPermission();
        permission.beginCheck(this);

        ISerializableField<List<RuleSchemaConfiguration>> field = getField(GROUP_RULES_FIELD);
        field.set(null);

        invalidateComponentRuleCaches();

        permission.endCheck();
    }

    @Override
    public Iterable<AlertSchemaConfiguration> getGroupAlerts() {
        ISerializableField<List<AlertSchemaConfiguration>> field = getField(GROUP_ALERTS_FIELD);
        List<AlertSchemaConfiguration> list = field.get();
        if (list != null)
            return list;
        else
            return Collections.emptyList();
    }

    @Override
    public AlertSchemaConfiguration findGroupAlert(String alertName) {
        Assert.notNull(alertName);

        ISerializableField<List<AlertSchemaConfiguration>> field = getField(GROUP_ALERTS_FIELD);
        List<AlertSchemaConfiguration> alerts = field.get();
        if (alerts != null) {
            for (int i = 0; i < alerts.size(); i++) {
                AlertSchemaConfiguration alert = alerts.get(i);
                if (alert.getName().equals(alertName))
                    return alert;
            }
        }

        return null;
    }

    @Override
    public void addGroupAlert(AlertSchemaConfiguration alertConfiguration) {
        Assert.notNull(alertConfiguration);

        IPermission permission = getSchema().getEditGroupAlertsPermission();
        permission.beginCheck(this);

        ISerializableField<List<AlertSchemaConfiguration>> field = getField(GROUP_ALERTS_FIELD);
        List<AlertSchemaConfiguration> alerts = field.get();
        if (alerts == null)
            alerts = new ArrayList<AlertSchemaConfiguration>();

        boolean set = false;
        for (int i = 0; i < alerts.size(); i++) {
            AlertSchemaConfiguration alert = alerts.get(i);
            if (alert.getName().equals(alertConfiguration.getName())) {
                alerts.set(i, alertConfiguration);
                set = true;
                break;
            }
        }

        if (!set)
            alerts.add(alertConfiguration);

        field.set(alerts);

        invalidateComponentRuleCaches();

        permission.endCheck();
    }

    @Override
    public void removeGroupAlert(String alertName) {
        Assert.notNull(alertName);

        IPermission permission = getSchema().getEditGroupAlertsPermission();
        permission.beginCheck(this);

        ISerializableField<List<AlertSchemaConfiguration>> field = getField(GROUP_ALERTS_FIELD);
        List<AlertSchemaConfiguration> alerts = field.get();
        if (alerts != null) {
            for (int i = 0; i < alerts.size(); i++) {
                AlertSchemaConfiguration alert = alerts.get(i);
                if (alert.getName().equals(alertName)) {
                    alerts.remove(i);
                    break;
                }
            }
        }

        field.set(alerts);

        invalidateComponentRuleCaches();

        permission.endCheck();
    }

    @Override
    public void removeAllGroupAlerts() {
        IPermission permission = getSchema().getEditGroupAlertsPermission();
        permission.beginCheck(this);

        ISerializableField<List<AlertSchemaConfiguration>> field = getField(GROUP_ALERTS_FIELD);
        field.set(null);

        invalidateComponentRuleCaches();

        permission.endCheck();
    }

    @Override
    public void addToIncidentGroups(IIncident incident) {
        super.addToIncidentGroups(incident);

        GroupComponentNode parent = ((GroupComponentVersionNode) getCurrentVersion()).getParent();
        if (parent != null)
            parent.addToIncidentGroups(incident);
    }

    @Override
    public void enableMaintenanceMode(String message) {
        if (supportsAvailability())
            super.enableMaintenanceMode(message);
        else
            doEnableMaintenanceMode(message);
    }

    @Override
    public void disableMaintenanceMode() {
        if (supportsAvailability())
            super.disableMaintenanceMode();
        else
            doDisableMaintenanceMode();
    }

    @Override
    public void delete() {
        Assert.checkState(!((IGroupComponentVersion) getCurrentVersion()).isPredefined());
        super.delete();
    }

    public void deletePredefined() {
        super.delete();
    }

    @Override
    public void onStateChanged(State oldState, State newState) {
        super.onStateChanged(oldState, newState);

        GroupComponentNode parent = ((GroupComponentVersionNode) getCurrentVersion()).getParent();
        if (parent != null)
            parent.checkAvailability();
    }

    public void checkAvailability() {
        if (!supportsAvailability())
            return;

        if (((GroupComponentVersionNode) getCurrentVersion()).isComponentAvailable())
            setNormalState();
        else
            setUnavailableState();
    }

    public void checkDynamicAutoDeletion() {
        GroupComponentVersionNode currentVersion = (GroupComponentVersionNode) getCurrentVersion();
        if (!currentVersion.isDynamic())
            return;

        if (!currentVersion.getChildren().iterator().hasNext() && !currentVersion.getComponents().iterator().hasNext())
            super.delete();
    }

    @Override
    public void dump(IJsonHandler json, IDumpContext context) {
        super.dump(json, context);

        boolean jsonGroupRules = false;
        for (RuleSchemaConfiguration rule : getGroupRules()) {
            if (!jsonGroupRules) {
                json.key("groupRules");
                json.startArray();
                jsonGroupRules = true;
            }

            json.value(rule.toString());
        }

        if (jsonGroupRules)
            json.endArray();

        boolean jsonGroupAlerts = false;
        for (AlertSchemaConfiguration alert : getGroupAlerts()) {
            if (!jsonGroupAlerts) {
                json.key("groupAlerts");
                json.startArray();
                jsonGroupAlerts = true;
            }

            json.value(alert.toString());
        }

        if (jsonGroupAlerts)
            json.endArray();
    }

    @Override
    protected boolean supportsAvailability() {
        GroupComponentNodeSchema schema = getSchema();
        return schema.getAvailabilityCondition() != null;
    }

    @Override
    protected void doEnableMaintenanceMode(String message) {
        GroupComponentVersionNode currentVersion = (GroupComponentVersionNode) getCurrentVersion();
        for (IGroupComponent child : currentVersion.getChildren())
            child.enableMaintenanceMode(message);
    }

    @Override
    protected void doDisableMaintenanceMode() {
        GroupComponentVersionNode currentVersion = (GroupComponentVersionNode) getCurrentVersion();
        for (IGroupComponent child : currentVersion.getChildren())
            child.disableMaintenanceMode();
    }

    @Override
    protected void doBeforeDelete() {
        super.doBeforeDelete();

        invalidateComponentRuleCaches();
    }

    @Override
    protected void doAfterDelete(ComponentVersionNode version) {
        super.doAfterDelete(version);

        GroupComponentVersionNode currentVersion = (GroupComponentVersionNode) version;
        if (currentVersion.getParent() != null)
            currentVersion.getParent().checkDynamicAutoDeletion();

        for (IGroupComponent child : currentVersion.getChildren())
            child.delete();
    }

    @Override
    protected List<GroupComponentNode> getParentGroups(ComponentVersionNode v) {
        GroupComponentVersionNode version = (GroupComponentVersionNode) v;
        if (version.getParent() != null)
            return Collections.singletonList(version.getParent());
        else
            return Collections.singletonList(null);
    }

    private void invalidateComponentRuleCaches() {
        for (IComponent component : ((IGroupComponentVersion) getCurrentVersion()).getComponents())
            ((ComponentNode) component).invalidateRuleCache();
    }
}