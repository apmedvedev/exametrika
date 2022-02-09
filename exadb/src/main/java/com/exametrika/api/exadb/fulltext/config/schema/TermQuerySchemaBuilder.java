/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.fulltext.config.schema;

import com.exametrika.common.utils.Assert;
import com.exametrika.spi.exadb.fulltext.config.schema.QuerySchemaBuilder;


/**
 * The {@link TermQuerySchemaBuilder} is a builder of configuration of term index query.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class TermQuerySchemaBuilder extends QuerySchemaBuilder {
    private final CompositeQuerySchemaBuilder parent;
    private final String field;
    private final String value;
    private float boost = 1.0f;

    public TermQuerySchemaBuilder(String field, String value) {
        Assert.notNull(field);
        Assert.notNull(value);

        this.parent = null;
        this.field = field;
        this.value = value;
    }

    public TermQuerySchemaBuilder(CompositeQuerySchemaBuilder parent, String field, String value) {
        Assert.notNull(parent);
        Assert.notNull(field);
        Assert.notNull(value);

        this.parent = parent;
        this.field = field;
        this.value = value;
    }

    public TermQuerySchemaBuilder boost(float value) {
        boost = value;
        return this;
    }

    public CompositeQuerySchemaBuilder end() {
        return parent;
    }

    @Override
    public TermQuerySchemaConfiguration toConfiguration() {
        return new TermQuerySchemaConfiguration(field, value, boost);
    }
}
