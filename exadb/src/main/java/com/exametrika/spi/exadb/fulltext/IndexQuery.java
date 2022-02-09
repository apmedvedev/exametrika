/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.fulltext;

import org.apache.lucene.search.Query;

import com.exametrika.api.exadb.fulltext.IQuery;
import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;
import com.exametrika.common.utils.Assert;


/**
 * The {@link IndexQuery} is an index query.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class IndexQuery implements IQuery {
    private final IDocumentSchema schema;
    private final Query query;

    public IndexQuery(IDocumentSchema schema, Query query) {
        Assert.notNull(schema);
        Assert.notNull(query);

        this.schema = schema;
        this.query = query;
    }

    @Override
    public IDocumentSchema getSchema() {
        return schema;
    }

    public Query getQuery() {
        return query;
    }

    @Override
    public String toString() {
        return query.toString();
    }
}
