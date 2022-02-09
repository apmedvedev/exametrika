/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.config.model;

import com.exametrika.api.component.ISelector;
import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.api.component.schema.ISelectorSchema;
import com.exametrika.impl.component.selectors.AllIncidentsSelector;
import com.exametrika.spi.component.config.model.SelectorSchemaConfiguration;

/**
 * The {@link AllIncidentsSelectorSchemaConfiguration} represents a configuration of schema of all incidents component selector.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class AllIncidentsSelectorSchemaConfiguration extends SelectorSchemaConfiguration {
    public AllIncidentsSelectorSchemaConfiguration(String name) {
        super(name);
    }

    @Override
    public <T extends ISelector> T createSelector(IComponent component, ISelectorSchema schema) {
        return (T) new AllIncidentsSelector(component, schema);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof AllIncidentsSelectorSchemaConfiguration))
            return false;

        AllIncidentsSelectorSchemaConfiguration configuration = (AllIncidentsSelectorSchemaConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}
