/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.metrics.host.server.config.model;

import com.exametrika.api.component.ISelector;
import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.api.component.schema.ISelectorSchema;
import com.exametrika.impl.metrics.host.server.selectors.AllHostsSelector;
import com.exametrika.spi.component.config.model.SelectorSchemaConfiguration;

/**
 * The {@link AllHostsSelectorSchemaConfiguration} represents a configuration of schema of all hosts component selector.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class AllHostsSelectorSchemaConfiguration extends SelectorSchemaConfiguration {
    public AllHostsSelectorSchemaConfiguration(String name) {
        super(name);
    }

    @Override
    public <T extends ISelector> T createSelector(IComponent component, ISelectorSchema schema) {
        return (T) new AllHostsSelector(component, schema);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof AllHostsSelectorSchemaConfiguration))
            return false;

        AllHostsSelectorSchemaConfiguration configuration = (AllHostsSelectorSchemaConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}
