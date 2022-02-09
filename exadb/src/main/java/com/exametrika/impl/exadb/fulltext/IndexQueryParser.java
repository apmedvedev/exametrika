/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.fulltext;

import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.NumericRangeQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Version;

import com.exametrika.api.exadb.fulltext.config.schema.NumericFieldSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;
import com.exametrika.api.exadb.fulltext.schema.IFieldSchema;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.exadb.fulltext.IndexAnalyzer;

/**
 * The {@link IndexQueryParser} is a index query parser.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class IndexQueryParser extends QueryParser {
    private final IDocumentSchema schema;

    public IndexQueryParser(IDocumentSchema schema, String field) {
        super(Version.LUCENE_4_9, field, ((IndexAnalyzer) schema.getAnalyzer()).getAnalyzer());

        Assert.notNull(schema);

        this.schema = schema;
    }

    @Override
    protected Query getRangeQuery(String field, String part1, String part2, boolean startInclusive, boolean endInclusive)
            throws ParseException {
        IFieldSchema fieldSchema = schema.findField(field);
        if (fieldSchema == null)
            throw new ParseException("Schema of field \'" + field + "\' is not defined.");

        if (getLowercaseExpandedTerms()) {
            part1 = part1 == null ? null : part1.toLowerCase(getLocale());
            part2 = part2 == null ? null : part2.toLowerCase(getLocale());
        }

        if (fieldSchema.getConfiguration() instanceof NumericFieldSchemaConfiguration) {
            field += IndexNumericField.PREFIX;
            NumericFieldSchemaConfiguration numericConfiguration = (NumericFieldSchemaConfiguration) fieldSchema.getConfiguration();
            try {
                switch (numericConfiguration.getType()) {
                    case INT:
                        return NumericRangeQuery.newIntRange(field, numericConfiguration.getPrecisionStep(), Integer.parseInt(part1),
                                Integer.parseInt(part2), startInclusive, endInclusive);
                    case LONG:
                        return NumericRangeQuery.newLongRange(field, numericConfiguration.getPrecisionStep(), Long.parseLong(part1),
                                Long.parseLong(part2), startInclusive, endInclusive);
                    case FLOAT:
                        return NumericRangeQuery.newFloatRange(field, numericConfiguration.getPrecisionStep(), Float.parseFloat(part1),
                                Float.parseFloat(part2), startInclusive, endInclusive);
                    case DOUBLE:
                        return NumericRangeQuery.newDoubleRange(field, numericConfiguration.getPrecisionStep(), Double.parseDouble(part1),
                                Double.parseDouble(part2), startInclusive, endInclusive);
                    default:
                        return Assert.error();
                }
            } catch (NumberFormatException e) {
                throw new ParseException(e.getMessage());
            }
        } else
            return newRangeQuery(field, part1, part2, startInclusive, endInclusive);
    }
}
