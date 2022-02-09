/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.fulltext.schema;

import java.io.Reader;
import java.util.Set;

import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.FieldType.NumericType;
import org.apache.lucene.index.FieldInfo.IndexOptions;

import com.exametrika.api.exadb.fulltext.IField;
import com.exametrika.api.exadb.fulltext.config.schema.NumericFieldSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.config.schema.StringFieldSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.config.schema.TextFieldSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.schema.IFieldSchema;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.exadb.fulltext.IndexNumericField;
import com.exametrika.impl.exadb.fulltext.IndexStringField;
import com.exametrika.impl.exadb.fulltext.IndexTextField;
import com.exametrika.spi.exadb.fulltext.config.schema.AnalyzerSchemaConfiguration;
import com.exametrika.spi.exadb.fulltext.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.fulltext.config.schema.FieldSchemaConfiguration.Option;


/**
 * The {@link FieldSchema} is an index field schema.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class FieldSchema implements IFieldSchema {
    private final DocumentSchema document;
    private final FieldSchemaConfiguration configuration;
    private final int index;
    private final FieldType fieldType;

    public FieldSchema(DocumentSchema document, FieldSchemaConfiguration configuration, int index) {
        Assert.notNull(document);
        Assert.notNull(configuration);

        this.document = document;
        this.configuration = configuration;
        this.index = index;
        this.fieldType = createFieldType();
    }

    public final FieldType getFieldType() {
        return fieldType;
    }

    @Override
    public FieldSchemaConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public DocumentSchema getDocument() {
        return document;
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public boolean isSortable() {
        if (configuration instanceof NumericFieldSchemaConfiguration)
            return true;

        AnalyzerSchemaConfiguration analyzer = configuration.getAnalyzer() != null ?
                configuration.getAnalyzer() : document.getConfiguration().getAnalyzer();

        return !configuration.getOptions().contains(Option.TOKENIZED_AND_INDEXED) || analyzer.isSortable();
    }

    @Override
    public IField createField(Object value) {
        if (configuration instanceof NumericFieldSchemaConfiguration)
            return new IndexNumericField(configuration.getName(), 1.0f, (Number) value);
        else if (configuration instanceof StringFieldSchemaConfiguration)
            return new IndexStringField(configuration.getName(), 1.0f, (String) value);
        else if (configuration instanceof TextFieldSchemaConfiguration)
            return new IndexTextField(configuration.getName(), 1.0f, (Reader) value);
        else
            return Assert.error();
    }

    @Override
    public String toString() {
        return configuration.toString();
    }

    private FieldType createFieldType() {
        FieldType fieldType = new FieldType();
        Set<Option> options = configuration.getOptions();
        fieldType.setIndexed(options.contains(Option.INDEXED) || options.contains(Option.TOKENIZED_AND_INDEXED));
        fieldType.setStored(options.contains(Option.STORED));
        fieldType.setTokenized(options.contains(Option.TOKENIZED_AND_INDEXED));
        fieldType.setStoreTermVectors(options.contains(Option.STORE_TERM_VECTORS));
        fieldType.setStoreTermVectorOffsets(options.contains(Option.STORE_TERM_VECTOR_OFFSETS));
        fieldType.setStoreTermVectorPositions(options.contains(Option.STORE_TERM_VECTOR_POSITIONS));
        fieldType.setStoreTermVectorPayloads(options.contains(Option.STORE_TERM_VECTOR_PAYLOADS));
        fieldType.setOmitNorms(options.contains(Option.OMIT_NORMS));

        if (options.contains(Option.INDEX_DOCUMENTS_FREQUENCES_POSITIONS_AND_OFFSETS))
            fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS_AND_OFFSETS);
        else if (options.contains(Option.INDEX_DOCUMENTS_FREQUENCES_AND_POSITIONS))
            fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);
        else if (options.contains(Option.INDEX_DOCUMENTS_AND_FREQUENCES))
            fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS);
        else if (options.contains(Option.INDEX_DOCUMENTS))
            fieldType.setIndexOptions(IndexOptions.DOCS_ONLY);
        else
            fieldType.setIndexOptions(IndexOptions.DOCS_AND_FREQS_AND_POSITIONS);

        if (configuration instanceof NumericFieldSchemaConfiguration) {
            NumericFieldSchemaConfiguration numericConfiguration = (NumericFieldSchemaConfiguration) configuration;
            switch (numericConfiguration.getType()) {
                case INT:
                    fieldType.setNumericType(NumericType.INT);
                    break;
                case LONG:
                    fieldType.setNumericType(NumericType.LONG);
                    break;
                case FLOAT:
                    fieldType.setNumericType(NumericType.FLOAT);
                    break;
                case DOUBLE:
                    fieldType.setNumericType(NumericType.DOUBLE);
                    break;
                default:
                    return Assert.error();
            }

            fieldType.setNumericPrecisionStep(numericConfiguration.getPrecisionStep());
        }

        fieldType.freeze();

        return fieldType;
    }
}
