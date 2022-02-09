/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.component.config.model;

import com.exametrika.api.component.nodes.IGroupComponent;
import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.ICondition;
import com.exametrika.spi.exadb.core.IDatabaseContext;

/**
 * The {@link GroupAvailabilityConditionSchemaConfiguration} represents a configuration of schema of group availability condition.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class GroupAvailabilityConditionSchemaConfiguration extends Configuration {
    public abstract ICondition<IGroupComponent> createCondition(IDatabaseContext context);
}
