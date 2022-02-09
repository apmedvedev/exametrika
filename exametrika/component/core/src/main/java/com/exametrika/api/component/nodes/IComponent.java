/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.nodes;

import java.util.List;

import com.exametrika.api.aggregator.common.model.IScopeName;
import com.exametrika.api.component.IAction;
import com.exametrika.api.component.ISelector;
import com.exametrika.api.component.config.model.ComponentSchemaConfiguration;
import com.exametrika.api.exadb.jobs.IJob;
import com.exametrika.api.exadb.jobs.config.model.JobSchemaConfiguration;
import com.exametrika.common.json.JsonObject;
import com.exametrika.spi.component.config.model.AlertSchemaConfiguration;
import com.exametrika.spi.component.config.model.RuleSchemaConfiguration;


/**
 * The {@link IComponent} represents a component node.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IComponent {
    /**
     * Returns component schema configuration.
     *
     * @return component schema configuration
     */
    ComponentSchemaConfiguration getConfiguration();

    /**
     * Returns persistent identifier of scope of component.
     *
     * @return persistent identifier of scope of component
     */
    long getScopeId();

    /**
     * Returns scope name of component.
     *
     * @return scope name of component
     */
    IScopeName getScope();

    /**
     * Returns component title.
     *
     * @return component title
     */
    String getTitle();

    /**
     * Returns component description.
     *
     * @return component description
     */
    String getDescription();

    /**
     * Returns time snapshot version if time snapshot operation is active or current version.
     *
     * @return time snapshot version if time snapshot operation is active or current version or null
     * if suitable version is not found.
     */
    IComponentVersion get();

    /**
     * Returns current component version.
     *
     * @return current component version
     */
    IComponentVersion getCurrentVersion();

    /**
     * Returns action log.
     *
     * @return action log or null if actions are not executed yet
     */
    IActionLog getActionLog();

    /**
     * Sets component options.
     *
     * @param value component options
     */
    void setOptions(JsonObject value);

    /**
     * Creates action instance bound to this component.
     *
     * @param <T>  action type
     * @param name action name
     * @return action instance
     */
    <T extends IAction> T createAction(String name);

    /**
     * Creates selector instance bound to this component.
     *
     * @param <T>  selector type
     * @param name selector name
     * @return selector instance
     */
    <T extends ISelector> T createSelector(String name);

    /**
     * Returns component rules.
     *
     * @return component rules
     */
    Iterable<RuleSchemaConfiguration> getRules();

    /**
     * Finds component rule.
     *
     * @param ruleName rule name
     * @return rule or null if rule with specified name is not found
     */
    RuleSchemaConfiguration findRule(String ruleName);

    /**
     * Adds component rule.
     *
     * @param ruleConfiguration component rule configuration
     */
    void addRule(RuleSchemaConfiguration ruleConfiguration);

    /**
     * Removes component rule.
     *
     * @param ruleName component rule name
     */
    void removeRule(String ruleName);

    /**
     * Removes all component rules.
     */
    void removeAllRules();

    /**
     * Returns component alerts.
     *
     * @return component alerts
     */
    Iterable<AlertSchemaConfiguration> getAlerts();

    /**
     * Finds component alert.
     *
     * @param alertName alert name
     * @return alert or null if alert with specified name is not found
     */
    AlertSchemaConfiguration findAlert(String alertName);

    /**
     * Adds component alert.
     *
     * @param alertConfiguration component alert configuration
     */
    void addAlert(AlertSchemaConfiguration alertConfiguration);

    /**
     * Removes component alert.
     *
     * @param alertName component alert name
     */
    void removeAlert(String alertName);

    /**
     * Removes all component alerts.
     */
    void removeAllAlerts();

    /**
     * Returns component incidents.
     *
     * @return component incidents
     */
    Iterable<IIncident> getIncidents();

    /**
     * Returns component jobs.
     *
     * @return component jobs
     */
    Iterable<IJob> getJobs();

    /**
     * Finds component job.
     *
     * @param jobName job name
     * @return job or null if job with specified name is not found
     */
    IJob findJob(String jobName);

    /**
     * Adds component job or replaces existing job configuration. Job name must be unique across all jobs of all components.
     *
     * @param jobConfiguration component job configuration
     * @return job
     */
    IJob addJob(JobSchemaConfiguration jobConfiguration);

    /**
     * Removes component job.
     *
     * @param jobName component job name
     */
    void removeJob(String jobName);

    /**
     * Removes all component jobs.
     */
    void removeAllJobs();

    /**
     * Marks component as deleted.
     */
    void delete();

    /**
     * Returns tags.
     *
     * @return tags or null if tags are not set
     */
    List<String> getTags();

    /**
     * Sets tags.
     *
     * @param tags tags or null if tags are cleared
     */
    void setTags(List<String> tags);
}
