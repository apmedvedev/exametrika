/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.nodes;

import com.exametrika.api.component.IAction;
import com.exametrika.api.component.ISelector;
import com.exametrika.spi.component.config.model.AlertSchemaConfiguration;
import com.exametrika.spi.component.config.model.RuleSchemaConfiguration;


/**
 * The {@link IGroupComponent} represents a group component node.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IGroupComponent extends IHealthComponent {
    /**
     * Adds child group.
     *
     * @param child child group
     */
    void addChild(IGroupComponent child);

    /**
     * Removes child group.
     *
     * @param child child group
     */
    void removeChild(IGroupComponent child);

    /**
     * Removes all children.
     */
    void removeAllChildren();

    /**
     * Adds component.
     *
     * @param component component
     */
    void addComponent(IComponent component);

    /**
     * Removes component.
     *
     * @param component component
     */
    void removeComponent(IComponent component);

    /**
     * Removes all components.
     */
    void removeAllComponents();

    /**
     * Creates group action being executed on components of this group, which support action with specified name.
     *
     * @param <T>       action type
     * @param name      action name
     * @param recursive if true action is also performed on subgroups
     * @return action instance
     */
    <T extends IAction> T createGroupAction(String name, boolean recursive);

    /**
     * Creates group selector being executed on components of this group, which support selector with specified name.
     *
     * @param <T>  action type
     * @param name selector name
     * @return selector instance
     */
    <T extends ISelector> T createGroupSelector(String name);

    /**
     * Returns group component rules (bound to all components of the group).
     *
     * @return component rules
     */
    Iterable<RuleSchemaConfiguration> getGroupRules();

    /**
     * Finds group component rule.
     *
     * @param ruleName rule name
     * @return rule or null if rule with specified name is not found
     */
    RuleSchemaConfiguration findGroupRule(String ruleName);

    /**
     * Adds group component rule
     *
     * @param ruleConfiguration group component rule configuration
     */
    void addGroupRule(RuleSchemaConfiguration ruleConfiguration);

    /**
     * Removes group component rule
     *
     * @param ruleName group component rule name
     */
    void removeGroupRule(String ruleName);

    /**
     * Removes all group component rules
     */
    void removeAllGroupRules();

    /**
     * Returns group component alerts (bound to all components of the group).
     *
     * @return component alert
     */
    Iterable<AlertSchemaConfiguration> getGroupAlerts();

    /**
     * Finds group component alert.
     *
     * @param alertName alert name
     * @return alert or null if alert with specified name is not found
     */
    AlertSchemaConfiguration findGroupAlert(String alertName);

    /**
     * Adds group component alert
     *
     * @param alertConfiguration group component alert configuration
     */
    void addGroupAlert(AlertSchemaConfiguration alertConfiguration);

    /**
     * Removes group component alert
     *
     * @param alertName group component alert name
     */
    void removeGroupAlert(String alertName);

    /**
     * Removes all group component alerts
     */
    void removeAllGroupAlerts();
}
