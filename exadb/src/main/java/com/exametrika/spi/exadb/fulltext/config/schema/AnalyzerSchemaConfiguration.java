/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.fulltext.config.schema;

import com.exametrika.api.exadb.fulltext.IAnalyzer;
import com.exametrika.common.config.Configuration;


/**
 * The {@link AnalyzerSchemaConfiguration} is a configuration of schema of index analyzer.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class AnalyzerSchemaConfiguration extends Configuration {
    public abstract IAnalyzer createAnalyzer();

    public abstract boolean isSortable();
}
