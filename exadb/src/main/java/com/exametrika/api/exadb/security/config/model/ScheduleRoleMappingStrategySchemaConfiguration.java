/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.security.config.model;

import com.exametrika.impl.exadb.security.ScheduleRoleMappingStrategy;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.spi.exadb.security.IRoleMappingStrategy;
import com.exametrika.spi.exadb.security.config.model.RoleMappingStrategySchemaConfiguration;


/**
 * The {@link ScheduleRoleMappingStrategySchemaConfiguration} is a schedule role mapping strategy schema configuration.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ScheduleRoleMappingStrategySchemaConfiguration extends RoleMappingStrategySchemaConfiguration {
    @Override
    public IRoleMappingStrategy createStrategy(IDatabaseContext context) {
        return new ScheduleRoleMappingStrategy(context);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ScheduleRoleMappingStrategySchemaConfiguration))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}