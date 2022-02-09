/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.config.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.exametrika.api.component.config.schema.AgentComponentNodeSchemaConfiguration;
import com.exametrika.api.component.config.schema.AgentComponentVersionNodeSchemaConfiguration;
import com.exametrika.api.exadb.jobs.config.model.JobSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.ReferenceFieldSchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.component.config.model.ActionSchemaConfiguration;
import com.exametrika.spi.component.config.model.AlertSchemaConfiguration;
import com.exametrika.spi.component.config.model.GroupDiscoveryStrategySchemaConfiguration;
import com.exametrika.spi.component.config.model.RuleSchemaConfiguration;
import com.exametrika.spi.component.config.model.SelectorSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.ObjectNodeSchemaConfiguration;


/**
 * The {@link AgentComponentSchemaConfiguration} is an agent component schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class AgentComponentSchemaConfiguration extends HealthComponentSchemaConfiguration {
    private final String profilerConfiguration;

    public AgentComponentSchemaConfiguration(String name, Set<ActionSchemaConfiguration> actions,
                                             Set<RuleSchemaConfiguration> rules, Set<? extends SelectorSchemaConfiguration> selectors,
                                             Set<? extends AlertSchemaConfiguration> alerts, List<? extends GroupDiscoveryStrategySchemaConfiguration> groupDiscoveryStrategies,
                                             Set<? extends JobSchemaConfiguration> jobs, String healthComponentType, String profilerConfiguration) {
        super(name, actions, rules, selectors, alerts, groupDiscoveryStrategies, jobs, healthComponentType);

        Assert.notNull(healthComponentType);
        Assert.notNull(profilerConfiguration);

        this.profilerConfiguration = profilerConfiguration;
    }

    public String getProfilerConfiguration() {
        return profilerConfiguration;
    }

    @Override
    public void buildNodeSchemas(Set<ObjectNodeSchemaConfiguration> nodes) {
        List<FieldSchemaConfiguration> fields = new ArrayList<FieldSchemaConfiguration>();
        buildFields(fields);
        nodes.add(new AgentComponentNodeSchemaConfiguration(getName(), getName(), null, fields, this));

        List<FieldSchemaConfiguration> versionFields = new ArrayList<FieldSchemaConfiguration>();
        buildVersionFields(versionFields);
        nodes.add(new AgentComponentVersionNodeSchemaConfiguration(getName() + "Version", getName() + "Version", null, versionFields, this));
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof AgentComponentSchemaConfiguration))
            return false;

        AgentComponentSchemaConfiguration configuration = (AgentComponentSchemaConfiguration) o;
        return super.equals(configuration) && profilerConfiguration.equals(configuration.profilerConfiguration);
    }

    @Override
    public boolean equalsStructured(ComponentSchemaConfiguration newSchema) {
        if (!(newSchema instanceof AgentComponentSchemaConfiguration))
            return false;

        AgentComponentSchemaConfiguration configuration = (AgentComponentSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + profilerConfiguration.hashCode();
    }

    @Override
    protected void buildVersionFields(List<FieldSchemaConfiguration> fields) {
        super.buildVersionFields(fields);

        fields.add(new ReferenceFieldSchemaConfiguration("subComponents", null));
    }
}
