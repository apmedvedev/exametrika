/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.metrics.host.server.config.model;

import com.exametrika.impl.metrics.host.server.selectors.HostSelector;

import com.exametrika.api.component.ISelector;
import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.api.component.schema.ISelectorSchema;
import com.exametrika.spi.component.config.model.SelectorSchemaConfiguration;

/**
 * The {@link HostSelectorSchemaConfiguration} represents a configuration of schema of host selector.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class HostSelectorSchemaConfiguration extends SelectorSchemaConfiguration {
    public HostSelectorSchemaConfiguration(String name) {
        super(name);
    }

    @Override
    public <T extends ISelector> T createSelector(IComponent component, ISelectorSchema schema) {
        return (T) new HostSelector(component, schema);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof HostSelectorSchemaConfiguration))
            return false;

        HostSelectorSchemaConfiguration configuration = (HostSelectorSchemaConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}
