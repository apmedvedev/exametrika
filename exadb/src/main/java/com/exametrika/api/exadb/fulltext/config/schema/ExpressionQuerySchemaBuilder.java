/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.fulltext.config.schema;

import java.util.Locale;
import java.util.TimeZone;

import com.exametrika.common.utils.Assert;
import com.exametrika.spi.exadb.fulltext.config.schema.QuerySchemaBuilder;


/**
 * The {@link ExpressionQuerySchemaBuilder} is a builder of configuration of index expression query.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class ExpressionQuerySchemaBuilder extends QuerySchemaBuilder {
    private final CompositeQuerySchemaBuilder parent;
    private final String field;
    private final String expression;
    private Locale locale;
    private TimeZone timeZone;
    private float boost = 1.0f;

    public ExpressionQuerySchemaBuilder(String field, String expression) {
        Assert.notNull(field);
        Assert.notNull(expression);

        this.parent = null;
        this.field = field;
        this.expression = expression;
    }

    public ExpressionQuerySchemaBuilder(CompositeQuerySchemaBuilder parent, String field, String expression) {
        Assert.notNull(parent);
        Assert.notNull(field);
        Assert.notNull(expression);

        this.parent = parent;
        this.field = field;
        this.expression = expression;
    }

    public ExpressionQuerySchemaBuilder locale(Locale value) {
        locale = value;
        return this;
    }

    public ExpressionQuerySchemaBuilder timeZone(TimeZone value) {
        timeZone = value;
        return this;
    }

    public ExpressionQuerySchemaBuilder boost(float value) {
        boost = value;
        return this;
    }

    public CompositeQuerySchemaBuilder end() {
        return parent;
    }

    @Override
    public ExpressionQuerySchemaConfiguration toConfiguration() {
        return new ExpressionQuerySchemaConfiguration(field, expression, locale, timeZone, boost);
    }
}
