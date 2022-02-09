/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.config.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exametrika.api.component.config.schema.ComponentNodeSchemaConfiguration;
import com.exametrika.api.component.config.schema.ComponentVersionNodeSchemaConfiguration;
import com.exametrika.api.component.config.schema.IndexedVersionFieldSchemaConfiguration;
import com.exametrika.api.exadb.jobs.config.model.JobSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.IndexType;
import com.exametrika.api.exadb.objectdb.config.schema.IndexedNumericFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.JsonFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.NumericFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.NumericSequenceFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.PrimitiveFieldSchemaConfiguration.DataType;
import com.exametrika.api.exadb.objectdb.config.schema.ReferenceFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.SerializableFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.SingleReferenceFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.TagFieldSchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.component.config.model.ActionSchemaConfiguration;
import com.exametrika.spi.component.config.model.AlertSchemaConfiguration;
import com.exametrika.spi.component.config.model.GroupDiscoveryStrategySchemaConfiguration;
import com.exametrika.spi.component.config.model.RuleSchemaConfiguration;
import com.exametrika.spi.component.config.model.SelectorSchemaConfiguration;
import com.exametrika.spi.exadb.core.config.schema.SchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.ObjectNodeSchemaConfiguration;


/**
 * The {@link ComponentSchemaConfiguration} is a component schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class ComponentSchemaConfiguration extends SchemaConfiguration {
    private final Map<String, ActionSchemaConfiguration> actions;
    private final Map<String, RuleSchemaConfiguration> rules;
    private final Map<String, SelectorSchemaConfiguration> selectors;
    private final Map<String, AlertSchemaConfiguration> alerts;
    private final List<GroupDiscoveryStrategySchemaConfiguration> groupDiscoveryStrategies;
    private final Map<String, JobSchemaConfiguration> jobs;

    public ComponentSchemaConfiguration(String name, Set<? extends ActionSchemaConfiguration> actions,
                                        Set<? extends RuleSchemaConfiguration> rules, Set<? extends SelectorSchemaConfiguration> selectors,
                                        Set<? extends AlertSchemaConfiguration> alerts, List<? extends GroupDiscoveryStrategySchemaConfiguration> groupDiscoveryStrategies,
                                        Set<? extends JobSchemaConfiguration> jobs) {
        super(name, name, null);

        Assert.notNull(name);
        Assert.notNull(actions);
        Assert.notNull(rules);
        Assert.notNull(selectors);
        Assert.notNull(alerts);
        Assert.notNull(groupDiscoveryStrategies);
        Assert.notNull(jobs);

        Map<String, ActionSchemaConfiguration> actionsMap = new HashMap<String, ActionSchemaConfiguration>();
        for (ActionSchemaConfiguration action : actions)
            Assert.isNull(actionsMap.put(action.getName(), action));
        this.actions = Immutables.wrap(actionsMap);

        Map<String, RuleSchemaConfiguration> rulesMap = new HashMap<String, RuleSchemaConfiguration>();
        for (RuleSchemaConfiguration rule : rules)
            Assert.isNull(rulesMap.put(rule.getName(), rule));
        this.rules = Immutables.wrap(rulesMap);

        Map<String, SelectorSchemaConfiguration> selectorsMap = new HashMap<String, SelectorSchemaConfiguration>();
        for (SelectorSchemaConfiguration selector : selectors)
            Assert.isNull(selectorsMap.put(selector.getName(), selector));
        this.selectors = Immutables.wrap(selectorsMap);

        Map<String, AlertSchemaConfiguration> alertsMap = new HashMap<String, AlertSchemaConfiguration>();
        for (AlertSchemaConfiguration alert : alerts)
            Assert.isNull(alertsMap.put(alert.getName(), alert));
        this.alerts = Immutables.wrap(alertsMap);

        this.groupDiscoveryStrategies = Immutables.wrap(groupDiscoveryStrategies);

        Map<String, JobSchemaConfiguration> jobsMap = new HashMap<String, JobSchemaConfiguration>();
        for (JobSchemaConfiguration job : jobs)
            Assert.isNull(jobsMap.put(job.getName(), job));
        this.jobs = Immutables.wrap(jobsMap);
    }

    public Map<String, ActionSchemaConfiguration> getActions() {
        return actions;
    }

    public Map<String, RuleSchemaConfiguration> getRules() {
        return rules;
    }

    public Map<String, AlertSchemaConfiguration> getAlerts() {
        return alerts;
    }

    public Map<String, SelectorSchemaConfiguration> getSelectors() {
        return selectors;
    }

    public List<GroupDiscoveryStrategySchemaConfiguration> getGroupDiscoveryStrategies() {
        return groupDiscoveryStrategies;
    }

    public Map<String, JobSchemaConfiguration> getJobs() {
        return jobs;
    }

    public void buildNodeSchemas(Set<ObjectNodeSchemaConfiguration> nodes) {
        List<FieldSchemaConfiguration> fields = new ArrayList<FieldSchemaConfiguration>();
        buildFields(fields);
        nodes.add(new ComponentNodeSchemaConfiguration(getName(), getName(), null, fields, this));

        List<FieldSchemaConfiguration> versionFields = new ArrayList<FieldSchemaConfiguration>();
        buildVersionFields(versionFields);
        nodes.add(new ComponentVersionNodeSchemaConfiguration(getName() + "Version", getName() + "Version", null, versionFields, this));
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ComponentSchemaConfiguration))
            return false;

        ComponentSchemaConfiguration configuration = (ComponentSchemaConfiguration) o;
        return super.equals(configuration) && actions.equals(configuration.actions) && rules.equals(configuration.rules) &&
                selectors.equals(configuration.selectors) && alerts.equals(configuration.alerts) &&
                groupDiscoveryStrategies.equals(configuration.groupDiscoveryStrategies) &&
                jobs.equals(configuration.jobs);
    }

    public boolean equalsStructured(ComponentSchemaConfiguration newSchema) {
        return getName().equals(newSchema.getName());
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(actions, rules, selectors, alerts, groupDiscoveryStrategies, jobs);
    }

    protected void buildFields(List<FieldSchemaConfiguration> fields) {
        fields.add(new IndexedNumericFieldSchemaConfiguration("scope", "scope", null, DataType.LONG, null, null, null, null, 0,
                IndexType.BTREE, true, true, false, true, true, false, "componentIndex", null));
        fields.add(new SingleReferenceFieldSchemaConfiguration("currentVersion", null));
        fields.add(new NumericSequenceFieldSchemaConfiguration("logSequence", 1, 1, null));
        fields.add(new SingleReferenceFieldSchemaConfiguration("actionLog", null));
        fields.add(new SerializableFieldSchemaConfiguration("rules"));
        fields.add(new SerializableFieldSchemaConfiguration("alerts"));
        fields.add(new ReferenceFieldSchemaConfiguration("incidents", null));
        fields.add(new ReferenceFieldSchemaConfiguration("jobs", null, "system.jobs"));
        fields.add(new TagFieldSchemaConfiguration("tags"));
    }

    protected void buildVersionFields(List<FieldSchemaConfiguration> fields) {
        fields.add(new NumericFieldSchemaConfiguration("flags", DataType.INT));
        fields.add(new NumericFieldSchemaConfiguration("time", DataType.LONG));
        fields.add(new IndexedVersionFieldSchemaConfiguration("index"));
        fields.add(new JsonFieldSchemaConfiguration("options"));
        fields.add(new JsonFieldSchemaConfiguration("properties"));
        fields.add(new SingleReferenceFieldSchemaConfiguration("component", null));
        fields.add(new SingleReferenceFieldSchemaConfiguration("previousVersion", null));
        fields.add(new ReferenceFieldSchemaConfiguration("groups", null));
    }
}
