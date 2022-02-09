/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.fulltext.config.schema;

import com.exametrika.api.exadb.fulltext.IQuery;
import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;
import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.Objects;


/**
 * The {@link QuerySchemaConfiguration} is a configuration of index query.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class QuerySchemaConfiguration extends Configuration {
    protected final float boost;

    public QuerySchemaConfiguration(float boost) {
        this.boost = boost;
    }

    public float getBoost() {
        return boost;
    }

    public abstract IQuery createQuery(IDocumentSchema schema);

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof QuerySchemaConfiguration))
            return false;

        QuerySchemaConfiguration configuration = (QuerySchemaConfiguration) o;
        return boost == configuration.boost;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(boost);
    }

    protected String getBoostString() {
        if (boost != 1.0f)
            return "^" + Float.toString(boost);
        else
            return "";
    }
}
