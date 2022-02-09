/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.config.model;

import com.exametrika.impl.component.security.ComponentPrefixCheckPermissionStrategy;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.spi.exadb.security.ICheckPermissionStrategy;
import com.exametrika.spi.exadb.security.config.model.CheckPermissionStrategySchemaConfiguration;


/**
 * The {@link ComponentPrefixCheckPermissionStrategySchemaConfiguration} is an perfix check permission strategy schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class ComponentPrefixCheckPermissionStrategySchemaConfiguration extends CheckPermissionStrategySchemaConfiguration {
    @Override
    public ICheckPermissionStrategy createStrategy(IDatabaseContext context) {
        return new ComponentPrefixCheckPermissionStrategy();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ComponentPrefixCheckPermissionStrategySchemaConfiguration))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return 31 * getClass().hashCode();
    }
}
