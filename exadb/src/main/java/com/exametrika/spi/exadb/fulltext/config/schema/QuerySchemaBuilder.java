/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.fulltext.config.schema;

import com.exametrika.api.exadb.fulltext.IQuery;
import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;


/**
 * The {@link QuerySchemaBuilder} is a builder of configuration of index query.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public abstract class QuerySchemaBuilder {
    public abstract QuerySchemaConfiguration toConfiguration();

    public final IQuery toQuery(IDocumentSchema schema) {
        return toConfiguration().createQuery(schema);
    }
}
