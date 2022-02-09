/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.fulltext.config.schema;


/**
 * The {@link Queries} represents a helper class to build database queries.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class Queries {
    public static CompositeQuerySchemaBuilder composite() {
        return new CompositeQuerySchemaBuilder();
    }

    public static TermQuerySchemaBuilder term(String field, String value) {
        return new TermQuerySchemaBuilder(field, value);
    }

    public static ExpressionQuerySchemaBuilder expression(String field, String expression) {
        return new ExpressionQuerySchemaBuilder(field, expression);
    }

    private Queries() {
    }
}