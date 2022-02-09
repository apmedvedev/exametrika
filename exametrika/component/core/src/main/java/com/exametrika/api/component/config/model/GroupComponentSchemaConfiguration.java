/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.config.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.exametrika.api.component.config.schema.GroupComponentNodeSchemaConfiguration;
import com.exametrika.api.component.config.schema.GroupComponentVersionNodeSchemaConfiguration;
import com.exametrika.api.exadb.jobs.config.model.JobSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.ReferenceFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.SerializableFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.SingleReferenceFieldSchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.component.config.model.ActionSchemaConfiguration;
import com.exametrika.spi.component.config.model.AlertSchemaConfiguration;
import com.exametrika.spi.component.config.model.GroupAvailabilityConditionSchemaConfiguration;
import com.exametrika.spi.component.config.model.GroupDiscoveryStrategySchemaConfiguration;
import com.exametrika.spi.component.config.model.RuleSchemaConfiguration;
import com.exametrika.spi.component.config.model.SelectorSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.ObjectNodeSchemaConfiguration;


/**
 * The {@link GroupComponentSchemaConfiguration} is a group component schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class GroupComponentSchemaConfiguration extends HealthComponentSchemaConfiguration {
    private boolean allowComponents;
    private boolean allowGroups;
    private Set<String> componentTypes;
    private Set<String> groupTypes;
    private boolean aggregationGroup;
    private GroupAvailabilityConditionSchemaConfiguration availabilityCondition;

    public GroupComponentSchemaConfiguration(String name) {
        this(name, Collections.<ActionSchemaConfiguration>emptySet(),
                Collections.<RuleSchemaConfiguration>emptySet(), Collections.<SelectorSchemaConfiguration>emptySet(),
                Collections.<AlertSchemaConfiguration>emptySet(), Collections.<GroupDiscoveryStrategySchemaConfiguration>emptyList(),
                Collections.<JobSchemaConfiguration>emptySet(), true, true, null, null, false, null, null);
    }

    public GroupComponentSchemaConfiguration(String name, Set<ActionSchemaConfiguration> actions,
                                             Set<RuleSchemaConfiguration> rules, Set<? extends SelectorSchemaConfiguration> selectors,
                                             Set<? extends AlertSchemaConfiguration> alerts, List<? extends GroupDiscoveryStrategySchemaConfiguration> groupDiscoveryStrategies,
                                             Set<? extends JobSchemaConfiguration> jobs, boolean allowComponents, boolean allowGroups, Set<String> componentTypes,
                                             Set<String> groupTypes, boolean aggregationGroup, GroupAvailabilityConditionSchemaConfiguration availabilityCondition,
                                             String healthComponentType) {
        super(name, actions, rules, selectors, alerts, groupDiscoveryStrategies, jobs, healthComponentType);

        Assert.isTrue((availabilityCondition == null) == (healthComponentType == null));

        this.allowComponents = allowComponents;
        this.allowGroups = allowGroups;
        this.componentTypes = componentTypes;
        this.groupTypes = groupTypes;
        this.aggregationGroup = aggregationGroup;
        this.availabilityCondition = availabilityCondition;
    }

    public boolean isAllowComponents() {
        return allowComponents;
    }

    public boolean isAllowGroups() {
        return allowGroups;
    }

    public Set<String> getComponentTypes() {
        return componentTypes;
    }

    public Set<String> getGroupTypes() {
        return groupTypes;
    }

    public boolean isAggregationGroup() {
        return aggregationGroup;
    }

    public GroupAvailabilityConditionSchemaConfiguration getAvailabilityCondition() {
        return availabilityCondition;
    }

    @Override
    public void buildNodeSchemas(Set<ObjectNodeSchemaConfiguration> nodes) {
        List<FieldSchemaConfiguration> fields = new ArrayList<FieldSchemaConfiguration>();
        buildFields(fields);
        nodes.add(new GroupComponentNodeSchemaConfiguration(getName(), getName(), null, fields, this));

        List<FieldSchemaConfiguration> versionFields = new ArrayList<FieldSchemaConfiguration>();
        buildVersionFields(versionFields);
        nodes.add(new GroupComponentVersionNodeSchemaConfiguration(getName() + "Version", getName() + "Version", null, versionFields, this));
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof GroupComponentSchemaConfiguration))
            return false;

        GroupComponentSchemaConfiguration configuration = (GroupComponentSchemaConfiguration) o;
        return super.equals(configuration) && allowComponents == configuration.allowComponents &&
                allowGroups == configuration.allowGroups && Objects.equals(componentTypes, configuration.componentTypes) &&
                Objects.equals(groupTypes, configuration.groupTypes) && aggregationGroup == configuration.aggregationGroup &&
                Objects.equals(availabilityCondition, configuration.availabilityCondition);
    }

    @Override
    public boolean equalsStructured(ComponentSchemaConfiguration newSchema) {
        if (!(newSchema instanceof GroupComponentSchemaConfiguration))
            return false;

        GroupComponentSchemaConfiguration configuration = (GroupComponentSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(allowComponents, allowGroups, componentTypes, groupTypes,
                aggregationGroup, availabilityCondition);
    }

    @Override
    protected void buildFields(List<FieldSchemaConfiguration> fields) {
        super.buildFields(fields);

        fields.add(new SerializableFieldSchemaConfiguration("groupRules"));
        fields.add(new SerializableFieldSchemaConfiguration("groupAlerts"));
    }

    @Override
    protected void buildVersionFields(List<FieldSchemaConfiguration> fields) {
        super.buildVersionFields(fields);

        fields.add(new SingleReferenceFieldSchemaConfiguration("parent", null));
        fields.add(new ReferenceFieldSchemaConfiguration("children", null));
        fields.add(new ReferenceFieldSchemaConfiguration("components", null));
    }
}
