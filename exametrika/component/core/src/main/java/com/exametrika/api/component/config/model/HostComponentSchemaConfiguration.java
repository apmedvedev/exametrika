/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.config.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.exametrika.api.component.config.schema.HostComponentNodeSchemaConfiguration;
import com.exametrika.api.component.config.schema.HostComponentVersionNodeSchemaConfiguration;
import com.exametrika.api.exadb.jobs.config.model.JobSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.ReferenceFieldSchemaConfiguration;
import com.exametrika.spi.component.config.model.ActionSchemaConfiguration;
import com.exametrika.spi.component.config.model.AlertSchemaConfiguration;
import com.exametrika.spi.component.config.model.GroupDiscoveryStrategySchemaConfiguration;
import com.exametrika.spi.component.config.model.RuleSchemaConfiguration;
import com.exametrika.spi.component.config.model.SelectorSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.ObjectNodeSchemaConfiguration;


/**
 * The {@link HostComponentSchemaConfiguration} is a host component schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class HostComponentSchemaConfiguration extends AgentComponentSchemaConfiguration {
    public HostComponentSchemaConfiguration(String name, Set<ActionSchemaConfiguration> actions,
                                            Set<RuleSchemaConfiguration> rules, Set<? extends SelectorSchemaConfiguration> selectors,
                                            Set<? extends AlertSchemaConfiguration> alerts, List<? extends GroupDiscoveryStrategySchemaConfiguration> groupDiscoveryStrategies,
                                            Set<? extends JobSchemaConfiguration> jobs, String healthComponentType, String profilerConfiguration) {
        super(name, actions, rules, selectors, alerts, groupDiscoveryStrategies, jobs, healthComponentType, profilerConfiguration);
    }

    @Override
    public void buildNodeSchemas(Set<ObjectNodeSchemaConfiguration> nodes) {
        List<FieldSchemaConfiguration> fields = new ArrayList<FieldSchemaConfiguration>();
        buildFields(fields);
        nodes.add(new HostComponentNodeSchemaConfiguration(getName(), getName(), null, fields, this));

        List<FieldSchemaConfiguration> versionFields = new ArrayList<FieldSchemaConfiguration>();
        buildVersionFields(versionFields);
        nodes.add(new HostComponentVersionNodeSchemaConfiguration(getName() + "Version", getName() + "Version", null, versionFields, this));
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof HostComponentSchemaConfiguration))
            return false;

        HostComponentSchemaConfiguration configuration = (HostComponentSchemaConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public boolean equalsStructured(ComponentSchemaConfiguration newSchema) {
        if (!(newSchema instanceof HostComponentSchemaConfiguration))
            return false;

        HostComponentSchemaConfiguration configuration = (HostComponentSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }

    @Override
    protected void buildVersionFields(List<FieldSchemaConfiguration> fields) {
        super.buildVersionFields(fields);

        fields.add(new ReferenceFieldSchemaConfiguration("nodes", null));
    }
}
