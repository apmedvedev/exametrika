/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator.common.meters.config;

import com.exametrika.common.config.Configuration;
import com.exametrika.spi.aggregator.common.meters.IFieldFactory;
import com.exametrika.spi.aggregator.common.values.config.FieldValueSchemaConfiguration;


/**
 * The {@link FieldConfiguration} is a configuration of fields.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class FieldConfiguration extends Configuration {
    public abstract IFieldFactory createFactory();

    public abstract FieldValueSchemaConfiguration getSchema();
}
