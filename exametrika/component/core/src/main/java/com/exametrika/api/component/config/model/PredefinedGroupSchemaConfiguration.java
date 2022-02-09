/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.config.model;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.exametrika.api.exadb.jobs.config.model.JobSchemaConfiguration;
import com.exametrika.common.config.Configuration;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.component.config.model.AlertSchemaConfiguration;
import com.exametrika.spi.component.config.model.RuleSchemaConfiguration;


/**
 * The {@link PredefinedGroupSchemaConfiguration} is an predefined group schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class PredefinedGroupSchemaConfiguration extends Configuration {
    private final String groupType;
    private final String name;
    private final Map<String, PredefinedGroupSchemaConfiguration> groups;
    private final JsonObject options;
    private final JsonObject properties;
    private final Set<String> tags;
    private final Map<String, RuleSchemaConfiguration> rules;
    private final Map<String, AlertSchemaConfiguration> alerts;
    private final Map<String, RuleSchemaConfiguration> groupRules;
    private final Map<String, AlertSchemaConfiguration> groupAlerts;
    private final Map<String, JobSchemaConfiguration> jobs;

    public PredefinedGroupSchemaConfiguration(String groupType, String name, Map<String, PredefinedGroupSchemaConfiguration> groups) {
        this(groupType, name, groups, null, null, Collections.<String>emptySet(), Collections.<RuleSchemaConfiguration>emptySet(),
                Collections.<AlertSchemaConfiguration>emptySet(), Collections.<RuleSchemaConfiguration>emptySet(),
                Collections.<AlertSchemaConfiguration>emptySet(), Collections.<JobSchemaConfiguration>emptySet());
    }

    public PredefinedGroupSchemaConfiguration(String groupType, String name,
                                              Map<String, PredefinedGroupSchemaConfiguration> groups, JsonObject options, JsonObject properties, Set<String> tags,
                                              Set<? extends RuleSchemaConfiguration> rules, Set<? extends AlertSchemaConfiguration> alerts,
                                              Set<? extends RuleSchemaConfiguration> groupRules, Set<? extends AlertSchemaConfiguration> groupAlerts,
                                              Set<? extends JobSchemaConfiguration> jobs) {
        Assert.notNull(groupType);
        Assert.notNull(name);
        Assert.notNull(groups);
        Assert.notNull(tags);
        Assert.notNull(rules);
        Assert.notNull(alerts);
        Assert.notNull(groupRules);
        Assert.notNull(groupAlerts);
        Assert.notNull(jobs);

        this.groupType = groupType;
        this.name = name;
        this.groups = Immutables.wrap(groups);
        this.options = options;
        this.properties = properties;
        this.tags = Immutables.wrap(tags);

        Map<String, RuleSchemaConfiguration> rulesMap = new HashMap<String, RuleSchemaConfiguration>();
        for (RuleSchemaConfiguration rule : rules)
            Assert.isNull(rulesMap.put(rule.getName(), rule));
        this.rules = Immutables.wrap(rulesMap);

        Map<String, RuleSchemaConfiguration> groupRulesMap = new HashMap<String, RuleSchemaConfiguration>();
        for (RuleSchemaConfiguration rule : groupRules)
            Assert.isNull(groupRulesMap.put(rule.getName(), rule));
        this.groupRules = Immutables.wrap(groupRulesMap);

        Map<String, AlertSchemaConfiguration> alertsMap = new HashMap<String, AlertSchemaConfiguration>();
        for (AlertSchemaConfiguration alert : alerts)
            Assert.isNull(alertsMap.put(alert.getName(), alert));
        this.alerts = Immutables.wrap(alertsMap);

        Map<String, AlertSchemaConfiguration> groupAlertsMap = new HashMap<String, AlertSchemaConfiguration>();
        for (AlertSchemaConfiguration alert : groupAlerts)
            Assert.isNull(groupAlertsMap.put(alert.getName(), alert));
        this.groupAlerts = Immutables.wrap(groupAlertsMap);

        Map<String, JobSchemaConfiguration> jobsMap = new HashMap<String, JobSchemaConfiguration>();
        for (JobSchemaConfiguration job : jobs)
            Assert.isNull(jobsMap.put(job.getName(), job));
        this.jobs = Immutables.wrap(jobsMap);
    }

    public String getGroupType() {
        return groupType;
    }

    public String getName() {
        return name;
    }

    public Map<String, PredefinedGroupSchemaConfiguration> getGroups() {
        return groups;
    }

    public JsonObject getOptions() {
        return options;
    }

    public JsonObject getProperties() {
        return properties;
    }

    public Set<String> getTags() {
        return tags;
    }

    public Map<String, RuleSchemaConfiguration> getRules() {
        return rules;
    }

    public Map<String, AlertSchemaConfiguration> getAlerts() {
        return alerts;
    }

    public Map<String, RuleSchemaConfiguration> getGroupRules() {
        return groupRules;
    }

    public Map<String, AlertSchemaConfiguration> getGroupAlerts() {
        return groupAlerts;
    }

    public Map<String, JobSchemaConfiguration> getJobs() {
        return jobs;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof PredefinedGroupSchemaConfiguration))
            return false;

        PredefinedGroupSchemaConfiguration configuration = (PredefinedGroupSchemaConfiguration) o;
        return groupType.equals(configuration.groupType) && name.equals(configuration.name) && groups.equals(configuration.groups) &&
                Objects.equals(options, configuration.options) && Objects.equals(properties, configuration.properties) &&
                tags.equals(configuration.tags) && rules.equals(configuration.rules) &&
                alerts.equals(configuration.alerts) && groupRules.equals(configuration.groupRules) && groupAlerts.equals(configuration.groupAlerts) &&
                jobs.equals(configuration.jobs);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(groupType, name, groups, options, properties, tags, rules, alerts, groupRules, groupAlerts, jobs);
    }

    @Override
    public String toString() {
        return name;
    }
}
