/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.fulltext.config.schema;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;

import com.exametrika.api.exadb.fulltext.IQuery;
import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.exadb.fulltext.IndexQuery;
import com.exametrika.spi.exadb.fulltext.config.schema.QuerySchemaConfiguration;


/**
 * The {@link TermQuerySchemaConfiguration} is a configuration of index term query.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class TermQuerySchemaConfiguration extends QuerySchemaConfiguration {
    private final String field;
    private final String value;

    public TermQuerySchemaConfiguration(String field, String value, float boost) {
        super(boost);

        Assert.notNull(field);
        Assert.notNull(value);

        this.field = field;
        this.value = value;
    }

    public String getField() {
        return field;
    }

    public String getValue() {
        return value;
    }

    @Override
    public IQuery createQuery(IDocumentSchema schema) {
        Query query = new TermQuery(new Term(field, value));
        query.setBoost(boost);
        return new IndexQuery(schema, query);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof TermQuerySchemaConfiguration))
            return false;

        TermQuerySchemaConfiguration configuration = (TermQuerySchemaConfiguration) o;
        return super.equals(o) && field.equals(configuration.field) && value.equals(configuration.value);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(field, value);
    }

    @Override
    public String toString() {
        return field + ":" + value + getBoostString();
    }
}
