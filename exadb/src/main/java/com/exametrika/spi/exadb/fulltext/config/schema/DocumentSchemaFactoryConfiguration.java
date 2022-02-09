/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.fulltext.config.schema;

import com.exametrika.api.exadb.fulltext.config.schema.DocumentSchemaConfiguration;
import com.exametrika.common.config.Configuration;


/**
 * The {@link DocumentSchemaFactoryConfiguration} is a configuration of factory of fulltext document schema.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class DocumentSchemaFactoryConfiguration extends Configuration {
    public abstract DocumentSchemaConfiguration createSchema();
}
