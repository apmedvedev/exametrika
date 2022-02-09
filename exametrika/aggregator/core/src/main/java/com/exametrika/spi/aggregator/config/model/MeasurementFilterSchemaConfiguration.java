/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator.config.model;

import com.exametrika.common.config.Configuration;
import com.exametrika.spi.aggregator.IMeasurementFilter;
import com.exametrika.spi.exadb.core.IDatabaseContext;

/**
 * The {@link MeasurementFilterSchemaConfiguration} represents a configuration of schema of measurement filter.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class MeasurementFilterSchemaConfiguration extends Configuration {
    public abstract IMeasurementFilter createFilter(IDatabaseContext context);
}
