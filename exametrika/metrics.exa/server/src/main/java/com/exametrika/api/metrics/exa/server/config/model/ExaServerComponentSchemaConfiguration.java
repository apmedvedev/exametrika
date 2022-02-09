/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.metrics.exa.server.config.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.exametrika.api.component.config.model.ComponentSchemaConfiguration;
import com.exametrika.api.component.config.model.HealthComponentSchemaConfiguration;
import com.exametrika.api.exadb.jobs.config.model.JobSchemaConfiguration;
import com.exametrika.api.metrics.exa.server.config.schema.ExaServerComponentNodeSchemaConfiguration;
import com.exametrika.api.metrics.exa.server.config.schema.ExaServerComponentVersionNodeSchemaConfiguration;
import com.exametrika.spi.component.config.model.ActionSchemaConfiguration;
import com.exametrika.spi.component.config.model.AlertSchemaConfiguration;
import com.exametrika.spi.component.config.model.GroupDiscoveryStrategySchemaConfiguration;
import com.exametrika.spi.component.config.model.RuleSchemaConfiguration;
import com.exametrika.spi.component.config.model.SelectorSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.ObjectNodeSchemaConfiguration;


/**
 * The {@link ExaServerComponentSchemaConfiguration} is a exa server component schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class ExaServerComponentSchemaConfiguration extends HealthComponentSchemaConfiguration {
    public ExaServerComponentSchemaConfiguration(String name, Set<ActionSchemaConfiguration> actions,
                                                 Set<RuleSchemaConfiguration> rules, Set<? extends SelectorSchemaConfiguration> selectors,
                                                 Set<? extends AlertSchemaConfiguration> alerts, List<? extends GroupDiscoveryStrategySchemaConfiguration> groupDiscoveryStrategies,
                                                 Set<? extends JobSchemaConfiguration> jobs, String healthComponentType) {
        super(name, actions, rules, selectors, alerts, groupDiscoveryStrategies, jobs, healthComponentType);
    }

    @Override
    public void buildNodeSchemas(Set<ObjectNodeSchemaConfiguration> nodes) {
        List<FieldSchemaConfiguration> fields = new ArrayList<FieldSchemaConfiguration>();
        buildFields(fields);
        nodes.add(new ExaServerComponentNodeSchemaConfiguration(getName(), getName(), null, fields, this));

        List<FieldSchemaConfiguration> versionFields = new ArrayList<FieldSchemaConfiguration>();
        buildVersionFields(versionFields);
        nodes.add(new ExaServerComponentVersionNodeSchemaConfiguration(getName() + "Version", getName() + "Version", null, versionFields, this));
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ExaServerComponentSchemaConfiguration))
            return false;

        ExaServerComponentSchemaConfiguration configuration = (ExaServerComponentSchemaConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public boolean equalsStructured(ComponentSchemaConfiguration newSchema) {
        if (!(newSchema instanceof ExaServerComponentSchemaConfiguration))
            return false;

        ExaServerComponentSchemaConfiguration configuration = (ExaServerComponentSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}
