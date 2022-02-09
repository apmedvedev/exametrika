/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.metrics.jvm.server.config.model;

import com.exametrika.api.component.ISelector;
import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.api.component.schema.ISelectorSchema;
import com.exametrika.impl.metrics.jvm.server.selectors.AllTransactionsSelector;
import com.exametrika.spi.component.config.model.SelectorSchemaConfiguration;

/**
 * The {@link AllTransactionsSelectorSchemaConfiguration} represents a configuration of schema of all transactions component selector.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class AllTransactionsSelectorSchemaConfiguration extends SelectorSchemaConfiguration {
    public AllTransactionsSelectorSchemaConfiguration(String name) {
        super(name);
    }

    @Override
    public <T extends ISelector> T createSelector(IComponent component, ISelectorSchema schema) {
        return (T) new AllTransactionsSelector(component, schema);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof AllTransactionsSelectorSchemaConfiguration))
            return false;

        AllTransactionsSelectorSchemaConfiguration configuration = (AllTransactionsSelectorSchemaConfiguration) o;
        return super.equals(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode();
    }
}
