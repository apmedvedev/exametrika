/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.metrics.jvm.server.config.model;

import com.exametrika.api.component.ISelector;
import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.api.component.schema.ISelectorSchema;
import com.exametrika.impl.metrics.jvm.server.selectors.AllJvmNodesSelector;
import com.exametrika.spi.component.config.model.SelectorSchemaConfiguration;

/**
 * The {@link AllJvmNodesSelectorSchemaConfiguration} represents a configuration of schema of all jvm nodes component selector.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class AllJvmNodesSelectorSchemaConfiguration extends SelectorSchemaConfiguration {
    public AllJvmNodesSelectorSchemaConfiguration(String name) {
        super(name);
    }

    @Override
    public <T extends ISelector> T createSelector(IComponent component, ISelectorSchema schema) {
        return (T) new AllJvmNodesSelector(component, schema);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof AllJvmNodesSelectorSchemaConfiguration))
            return false;

        AllJvmNodesSelectorSchemaConfiguration configuration = (AllJvmNodesSelectorSchemaConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}
