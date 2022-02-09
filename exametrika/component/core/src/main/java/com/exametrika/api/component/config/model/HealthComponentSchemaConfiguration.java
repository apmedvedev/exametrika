/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.config.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.exametrika.api.component.config.schema.HealthComponentNodeSchemaConfiguration;
import com.exametrika.api.component.config.schema.HealthComponentVersionNodeSchemaConfiguration;
import com.exametrika.api.exadb.jobs.config.model.JobSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.NumericFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.PrimitiveFieldSchemaConfiguration.DataType;
import com.exametrika.api.exadb.objectdb.config.schema.StringFieldSchemaConfiguration;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.component.config.model.ActionSchemaConfiguration;
import com.exametrika.spi.component.config.model.AlertSchemaConfiguration;
import com.exametrika.spi.component.config.model.GroupDiscoveryStrategySchemaConfiguration;
import com.exametrika.spi.component.config.model.RuleSchemaConfiguration;
import com.exametrika.spi.component.config.model.SelectorSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.ObjectNodeSchemaConfiguration;


/**
 * The {@link HealthComponentSchemaConfiguration} is a health component schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class HealthComponentSchemaConfiguration extends ComponentSchemaConfiguration {
    private final String healthComponentType;

    public HealthComponentSchemaConfiguration(String name, Set<ActionSchemaConfiguration> actions,
                                              Set<RuleSchemaConfiguration> rules, Set<? extends SelectorSchemaConfiguration> selectors,
                                              Set<? extends AlertSchemaConfiguration> alerts,
                                              List<? extends GroupDiscoveryStrategySchemaConfiguration> groupDiscoveryStrategies,
                                              Set<? extends JobSchemaConfiguration> jobs, String healthComponentType) {
        super(name, actions, rules, selectors, alerts, groupDiscoveryStrategies, jobs);

        this.healthComponentType = healthComponentType;
    }

    public String getHealthComponentType() {
        return healthComponentType;
    }

    @Override
    public void buildNodeSchemas(Set<ObjectNodeSchemaConfiguration> nodes) {
        List<FieldSchemaConfiguration> fields = new ArrayList<FieldSchemaConfiguration>();
        buildFields(fields);
        nodes.add(new HealthComponentNodeSchemaConfiguration(getName(), getName(), null, fields, this));

        List<FieldSchemaConfiguration> versionFields = new ArrayList<FieldSchemaConfiguration>();
        buildVersionFields(versionFields);
        nodes.add(new HealthComponentVersionNodeSchemaConfiguration(getName() + "Version", getName() + "Version", null, versionFields, this));
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof HealthComponentSchemaConfiguration))
            return false;

        HealthComponentSchemaConfiguration configuration = (HealthComponentSchemaConfiguration) o;
        return super.equals(configuration) && Objects.equals(healthComponentType, configuration.healthComponentType);
    }

    @Override
    public boolean equalsStructured(ComponentSchemaConfiguration newSchema) {
        if (!(newSchema instanceof HealthComponentSchemaConfiguration))
            return false;

        HealthComponentSchemaConfiguration configuration = (HealthComponentSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(healthComponentType);
    }

    @Override
    protected void buildVersionFields(List<FieldSchemaConfiguration> fields) {
        super.buildVersionFields(fields);

        fields.add(new StringFieldSchemaConfiguration("maintenanceMessage", 256));
        fields.add(new NumericFieldSchemaConfiguration("creationTime", DataType.LONG));
        fields.add(new NumericFieldSchemaConfiguration("startStopTime", DataType.LONG));
    }
}
