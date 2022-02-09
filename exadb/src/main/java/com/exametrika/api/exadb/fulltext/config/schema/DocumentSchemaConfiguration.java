/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.fulltext.config.schema;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;
import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.exadb.fulltext.schema.DocumentSchema;
import com.exametrika.spi.exadb.fulltext.config.schema.AnalyzerSchemaConfiguration;
import com.exametrika.spi.exadb.fulltext.config.schema.FieldSchemaConfiguration;


/**
 * The {@link DocumentSchemaConfiguration} is a configuration of index document schema.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class DocumentSchemaConfiguration extends Configuration {
    private final String documentType;
    private final List<FieldSchemaConfiguration> fields;
    private final AnalyzerSchemaConfiguration analyzer;
    private final int systemFieldCount;

    public DocumentSchemaConfiguration(String documentType, List<? extends FieldSchemaConfiguration> fields, AnalyzerSchemaConfiguration analyzer) {
        this(documentType, fields, analyzer, 0);
    }

    public DocumentSchemaConfiguration(String documentType, List<? extends FieldSchemaConfiguration> fields,
                                       AnalyzerSchemaConfiguration analyzer, int systemFieldCount) {
        Assert.notNull(documentType);
        Assert.notNull(fields);
        Assert.notNull(analyzer);

        this.documentType = documentType;
        this.fields = Immutables.wrap(fields);
        this.analyzer = analyzer;
        this.systemFieldCount = systemFieldCount;
    }

    public String getDocumentType() {
        return documentType;
    }

    public List<FieldSchemaConfiguration> getFields() {
        return fields;
    }

    public AnalyzerSchemaConfiguration getAnalyzer() {
        return analyzer;
    }

    public int getSystemFieldCount() {
        return systemFieldCount;
    }

    public <T extends FieldSchemaConfiguration> T findField(String fieldName) {
        Assert.notNull(fieldName);

        for (FieldSchemaConfiguration field : this.fields) {
            if (field.getName().equals(fieldName))
                return (T) field;
        }

        return null;
    }

    public <T extends FieldSchemaConfiguration> List<T> getFields(String fieldName) {
        Assert.notNull(fieldName);

        List<FieldSchemaConfiguration> fields = new ArrayList<FieldSchemaConfiguration>();
        for (FieldSchemaConfiguration field : this.fields) {
            if (field.getName().equals(fieldName))
                fields.add(field);
        }

        return (List) fields;
    }

    public IDocumentSchema createSchema() {
        return new DocumentSchema(this);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof DocumentSchemaConfiguration))
            return false;

        DocumentSchemaConfiguration configuration = (DocumentSchemaConfiguration) o;
        return documentType.equals(configuration.documentType) && fields.equals(configuration.fields) &&
                analyzer.equals(configuration.analyzer);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(documentType, fields, analyzer);
    }

    @Override
    public String toString() {
        return documentType + fields.toString();
    }
}
