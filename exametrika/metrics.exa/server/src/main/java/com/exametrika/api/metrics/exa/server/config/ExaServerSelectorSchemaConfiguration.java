/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.metrics.exa.server.config;

import com.exametrika.api.component.ISelector;
import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.api.component.schema.ISelectorSchema;
import com.exametrika.impl.metrics.exa.server.selectors.ExaServerSelector;
import com.exametrika.spi.component.config.model.SelectorSchemaConfiguration;


/**
 * The {@link ExaServerSelectorSchemaConfiguration} represents a configuration of schema of host selector.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class ExaServerSelectorSchemaConfiguration extends SelectorSchemaConfiguration {
    public ExaServerSelectorSchemaConfiguration(String name) {
        super(name);
    }

    @Override
    public <T extends ISelector> T createSelector(IComponent component, ISelectorSchema schema) {
        return (T) new ExaServerSelector(component, schema);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ExaServerSelectorSchemaConfiguration))
            return false;

        ExaServerSelectorSchemaConfiguration configuration = (ExaServerSelectorSchemaConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}
