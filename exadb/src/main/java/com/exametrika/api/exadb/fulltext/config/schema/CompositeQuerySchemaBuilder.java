/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.fulltext.config.schema;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.api.exadb.fulltext.config.schema.CompositeQuerySchemaConfiguration.Occur;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Pair;
import com.exametrika.spi.exadb.fulltext.config.schema.QuerySchemaBuilder;
import com.exametrika.spi.exadb.fulltext.config.schema.QuerySchemaConfiguration;


/**
 * The {@link CompositeQuerySchemaBuilder} is a builder of configuration of composite index query.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class CompositeQuerySchemaBuilder extends QuerySchemaBuilder {
    private final CompositeQuerySchemaBuilder parent;
    private final List<Pair<Occur, QuerySchemaBuilder>> children = new ArrayList<Pair<Occur, QuerySchemaBuilder>>();
    private Occur occur = Occur.MUST;
    private int minimumShouldMatch;
    private float boost = 1.0f;

    public CompositeQuerySchemaBuilder() {
        this.parent = null;
    }

    public CompositeQuerySchemaBuilder(CompositeQuerySchemaBuilder parent) {
        Assert.notNull(parent);

        this.parent = parent;
    }

    public CompositeQuerySchemaBuilder must() {
        occur = Occur.MUST;
        return this;
    }

    public CompositeQuerySchemaBuilder should() {
        occur = Occur.SHOULD;
        return this;
    }

    public CompositeQuerySchemaBuilder mustNot() {
        occur = Occur.MUST_NOT;
        return this;
    }

    public CompositeQuerySchemaBuilder minimumShouldMatch(int value) {
        minimumShouldMatch = value;
        return this;
    }

    public CompositeQuerySchemaBuilder boost(float value) {
        boost = value;
        return this;
    }

    public CompositeQuerySchemaBuilder composite() {
        CompositeQuerySchemaBuilder builder = new CompositeQuerySchemaBuilder(this);
        children.add(new Pair<Occur, QuerySchemaBuilder>(occur, builder));

        return builder;
    }

    public TermQuerySchemaBuilder term(String field, String value) {
        TermQuerySchemaBuilder builder = new TermQuerySchemaBuilder(this, field, value);
        children.add(new Pair<Occur, QuerySchemaBuilder>(occur, builder));

        return builder;
    }

    public ExpressionQuerySchemaBuilder expression(String field, String expression) {
        ExpressionQuerySchemaBuilder builder = new ExpressionQuerySchemaBuilder(this, field, expression);
        children.add(new Pair<Occur, QuerySchemaBuilder>(occur, builder));

        return builder;
    }

    public CompositeQuerySchemaBuilder end() {
        return parent;
    }

    @Override
    public CompositeQuerySchemaConfiguration toConfiguration() {
        List<Pair<Occur, QuerySchemaConfiguration>> queries = new ArrayList<Pair<Occur, QuerySchemaConfiguration>>(children.size());
        for (Pair<Occur, QuerySchemaBuilder> pair : children)
            queries.add(new Pair<Occur, QuerySchemaConfiguration>(pair.getKey(), pair.getValue().toConfiguration()));

        return new CompositeQuerySchemaConfiguration(queries, minimumShouldMatch, boost);
    }
}
