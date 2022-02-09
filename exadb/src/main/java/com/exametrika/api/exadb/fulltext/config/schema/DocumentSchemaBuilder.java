/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.fulltext.config.schema;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.exadb.fulltext.config.schema.AnalyzerSchemaConfiguration;
import com.exametrika.spi.exadb.fulltext.config.schema.FieldSchemaBuilder;
import com.exametrika.spi.exadb.fulltext.config.schema.FieldSchemaConfiguration;


/**
 * The {@link DocumentSchemaBuilder} is a builder of configuration of document.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class DocumentSchemaBuilder {
    private final List<FieldSchemaBuilder> fields = new ArrayList<FieldSchemaBuilder>();
    private AnalyzerSchemaConfiguration analyzer = new StandardAnalyzerSchemaConfiguration();
    private String documentType = "default";
    private int systemFieldCount;

    public DocumentSchemaBuilder documentType(String value) {
        Assert.notNull(value);

        this.documentType = value;
        return this;
    }

    public DocumentSchemaBuilder analyzer(AnalyzerSchemaConfiguration analyzer) {
        Assert.notNull(analyzer);

        this.analyzer = analyzer;
        return this;
    }

    public StringFieldSchemaBuilder stringField(String name) {
        StringFieldSchemaBuilder builder = new StringFieldSchemaBuilder(this, name, true);
        fields.add(builder);
        return builder;
    }

    public StringFieldSchemaBuilder textField(String name) {
        StringFieldSchemaBuilder builder = new StringFieldSchemaBuilder(this, name, false);
        fields.add(builder);
        return builder;
    }

    public NumericFieldSchemaBuilder numericField(String name) {
        NumericFieldSchemaBuilder builder = new NumericFieldSchemaBuilder(this, name);
        fields.add(builder);
        return builder;
    }

    public DocumentSchemaBuilder systemFields(int count) {
        systemFieldCount = count;
        return this;
    }

    public DocumentSchemaConfiguration toConfiguration() {
        List<FieldSchemaConfiguration> fields = new ArrayList<FieldSchemaConfiguration>();
        for (FieldSchemaBuilder field : this.fields)
            fields.add(field.toConfiguration());

        return new DocumentSchemaConfiguration(documentType, fields, analyzer, systemFieldCount);
    }

    public IDocumentSchema toSchema() {
        return toConfiguration().createSchema();
    }
}
