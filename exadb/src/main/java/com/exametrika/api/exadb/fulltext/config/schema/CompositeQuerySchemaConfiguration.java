/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.fulltext.config.schema;

import java.util.List;

import org.apache.lucene.search.BooleanQuery;

import com.exametrika.api.exadb.fulltext.IQuery;
import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;
import com.exametrika.common.utils.Pair;
import com.exametrika.spi.exadb.fulltext.IndexQuery;
import com.exametrika.spi.exadb.fulltext.config.schema.QuerySchemaConfiguration;


/**
 * The {@link CompositeQuerySchemaConfiguration} is a configuration of composite index query.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class CompositeQuerySchemaConfiguration extends QuerySchemaConfiguration {
    private final List<Pair<Occur, QuerySchemaConfiguration>> queries;
    private final int minimumShouldMatch;

    public enum Occur {
        MUST,

        SHOULD,

        MUST_NOT
    }

    public CompositeQuerySchemaConfiguration(List<Pair<Occur, QuerySchemaConfiguration>> queries, int minimumShouldMatch, float boost) {
        super(boost);

        Assert.notNull(queries);

        this.queries = Immutables.wrap(queries);
        this.minimumShouldMatch = minimumShouldMatch;
    }

    public List<Pair<Occur, QuerySchemaConfiguration>> getQueries() {
        return queries;
    }

    public int getMinimumShouldMatch() {
        return minimumShouldMatch;
    }

    @Override
    public IQuery createQuery(IDocumentSchema schema) {
        BooleanQuery query = new BooleanQuery();
        for (Pair<Occur, QuerySchemaConfiguration> pair : queries)
            query.add(((IndexQuery) pair.getValue().createQuery(schema)).getQuery(), createOccur(pair.getKey()));
        query.setBoost(boost);
        query.setMinimumNumberShouldMatch(minimumShouldMatch);

        return new IndexQuery(schema, query);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof CompositeQuerySchemaConfiguration))
            return false;

        CompositeQuerySchemaConfiguration configuration = (CompositeQuerySchemaConfiguration) o;
        return super.equals(o) && queries.equals(configuration.queries) && minimumShouldMatch == configuration.minimumShouldMatch;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(queries, minimumShouldMatch);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();

        if (boost != 1.0f || minimumShouldMatch > 0)
            builder.append('(');

        boolean first = true;
        for (Pair<Occur, QuerySchemaConfiguration> pair : queries) {
            if (first)
                first = false;
            else
                builder.append(' ');

            if (pair.getKey() == Occur.MUST)
                builder.append('+');
            else if (pair.getKey() == Occur.MUST_NOT)
                builder.append('-');

            if (pair.getValue() instanceof CompositeQuerySchemaConfiguration || pair.getValue() instanceof ExpressionQuerySchemaConfiguration) {
                builder.append('(');
                builder.append(pair.getValue());
                builder.append(')');
            } else
                builder.append(pair.getValue());
        }

        if (boost != 1.0f || minimumShouldMatch > 0) {
            builder.append(')');

            if (minimumShouldMatch > 0) {
                builder.append('~');
                builder.append(Integer.toString(minimumShouldMatch));
            }
            builder.append(getBoostString());
        }
        return builder.toString();
    }

    private org.apache.lucene.search.BooleanClause.Occur createOccur(Occur occur) {
        switch (occur) {
            case MUST:
                return org.apache.lucene.search.BooleanClause.Occur.MUST;
            case SHOULD:
                return org.apache.lucene.search.BooleanClause.Occur.SHOULD;
            case MUST_NOT:
                return org.apache.lucene.search.BooleanClause.Occur.MUST_NOT;
            default:
                return Assert.error();
        }
    }
}
