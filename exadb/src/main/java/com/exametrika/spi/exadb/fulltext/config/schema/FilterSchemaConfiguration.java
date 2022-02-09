/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.fulltext.config.schema;

import com.exametrika.api.exadb.fulltext.IFilter;
import com.exametrika.common.config.Configuration;


/**
 * The {@link FilterSchemaConfiguration} is a configuration of schema of index filter.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class FilterSchemaConfiguration extends Configuration {
    public abstract IFilter createFilter();
}
