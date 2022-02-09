/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.fulltext.config.schema;

import java.util.Locale;
import java.util.TimeZone;

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;

import com.exametrika.api.exadb.fulltext.IQuery;
import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;
import com.exametrika.common.rawdb.RawDatabaseException;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.exadb.fulltext.IndexQueryParser;
import com.exametrika.spi.exadb.fulltext.IndexQuery;
import com.exametrika.spi.exadb.fulltext.config.schema.QuerySchemaConfiguration;


/**
 * The {@link ExpressionQuerySchemaConfiguration} is a configuration of index expression query.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ExpressionQuerySchemaConfiguration extends QuerySchemaConfiguration {
    private final String field;
    private final String expression;
    private final Locale locale;
    private final TimeZone timeZone;

    public ExpressionQuerySchemaConfiguration(String field, String expression, Locale locale, TimeZone timeZone, float boost) {
        super(boost);

        Assert.notNull(field);
        Assert.notNull(expression);

        this.field = field;
        this.expression = expression;
        this.locale = locale;
        this.timeZone = timeZone;
    }

    public String getField() {
        return field;
    }

    public String getExpression() {
        return expression;
    }

    public Locale getLocale() {
        return locale;
    }

    public TimeZone getTimeZone() {
        return timeZone;
    }

    @Override
    public IQuery createQuery(IDocumentSchema schema) {
        IndexQueryParser parser = new IndexQueryParser(schema, field);

        if (locale != null)
            parser.setLocale(locale);
        if (timeZone != null)
            parser.setTimeZone(timeZone);

        try {
            Query query = parser.parse(expression);
            query.setBoost(boost);

            return new IndexQuery(schema, query);
        } catch (ParseException e) {
            throw new RawDatabaseException(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ExpressionQuerySchemaConfiguration))
            return false;

        ExpressionQuerySchemaConfiguration configuration = (ExpressionQuerySchemaConfiguration) o;
        return super.equals(o) && field.equals(configuration.field) && expression.equals(configuration.expression) &&
                Objects.equals(locale, configuration.locale) &&
                Objects.equals(timeZone, configuration.timeZone);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(field, expression, locale, timeZone);
    }

    @Override
    public String toString() {
        if (boost == 1.0f)
            return expression;
        else
            return '(' + expression + ')' + getBoostString();
    }
}
