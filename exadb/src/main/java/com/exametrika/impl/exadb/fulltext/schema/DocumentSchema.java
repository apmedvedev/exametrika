/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.fulltext.schema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.DoubleField;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FloatField;
import org.apache.lucene.document.IntField;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.IndexableField;

import com.exametrika.api.exadb.fulltext.IAnalyzer;
import com.exametrika.api.exadb.fulltext.IDocument;
import com.exametrika.api.exadb.fulltext.IField;
import com.exametrika.api.exadb.fulltext.config.schema.DocumentSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.config.schema.NumericFieldSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;
import com.exametrika.api.exadb.fulltext.schema.IFieldSchema;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.impl.exadb.fulltext.IndexDocument;
import com.exametrika.impl.exadb.fulltext.IndexField;
import com.exametrika.impl.exadb.fulltext.IndexNumericField;
import com.exametrika.impl.exadb.fulltext.IndexStringField;
import com.exametrika.impl.exadb.fulltext.IndexTextField;
import com.exametrika.spi.exadb.fulltext.IndexAnalyzer;
import com.exametrika.spi.exadb.fulltext.config.schema.FieldSchemaConfiguration;


/**
 * The {@link DocumentSchemaConfiguration} is a configuration of index document.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class DocumentSchema implements IDocumentSchema {
    private final DocumentSchemaConfiguration configuration;
    private final List<IFieldSchema> fields;
    private final IAnalyzer analyzer;

    public DocumentSchema(DocumentSchemaConfiguration configuration) {
        Assert.notNull(configuration);

        Map<String, Analyzer> fieldAnalyzers = null;
        List<IFieldSchema> fields = new ArrayList<IFieldSchema>();
        for (int i = 0; i < configuration.getFields().size(); i++) {
            FieldSchemaConfiguration field = configuration.getFields().get(i);
            fields.add(field.createSchema(this, i));

            if (field.getAnalyzer() != null) {
                if (fieldAnalyzers == null)
                    fieldAnalyzers = new HashMap<String, Analyzer>();

                fieldAnalyzers.put(field.getName(), ((IndexAnalyzer) field.getAnalyzer().createAnalyzer()).getAnalyzer());
            }
        }

        IAnalyzer analyzer;
        if (fieldAnalyzers == null)
            analyzer = configuration.getAnalyzer().createAnalyzer();
        else
            analyzer = new IndexAnalyzer(new PerFieldAnalyzerWrapper(((IndexAnalyzer) configuration.getAnalyzer().createAnalyzer()).getAnalyzer(),
                    fieldAnalyzers));

        this.configuration = configuration;
        this.fields = Immutables.wrap(fields);
        this.analyzer = analyzer;
    }

    @Override
    public DocumentSchemaConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public List<IFieldSchema> getFields() {
        return fields;
    }

    @Override
    public IAnalyzer getAnalyzer() {
        return analyzer;
    }

    @Override
    public IFieldSchema findField(String fieldName) {
        Assert.notNull(fieldName);

        for (IFieldSchema field : this.fields) {
            if (field.getConfiguration().getName().equals(fieldName))
                return field;
        }

        return null;
    }

    @Override
    public IDocument createDocument(Object... values) {
        List<IField> fields = new ArrayList<IField>();
        for (int i = 0; i < this.fields.size(); i++) {
            IFieldSchema field = this.fields.get(i);
            fields.add(field.createField(values[i]));
        }

        return new IndexDocument(null, this, fields);
    }

    @Override
    public IDocument createDocument(List<? extends Object> values) {
        return createDocument(null, values);
    }

    @Override
    public IDocument createDocument(Object context, List<? extends Object> values) {
        List<IField> fields = new ArrayList<IField>();
        for (int i = 0; i < this.fields.size(); i++) {
            IFieldSchema field = this.fields.get(i);
            fields.add(field.createField(values.get(i)));
        }

        return new IndexDocument(context, this, fields);
    }

    @Override
    public String toString() {
        return configuration.toString();
    }

    public Document createDocument(IndexDocument document) {
        Document doc = new Document();
        for (int i = 0; i < fields.size(); i++) {
            IField field = document.getFields().get(i);
            FieldSchema fieldSchema = (FieldSchema) fields.get(i);

            if (field instanceof IndexNumericField) {
                IndexNumericField numericField = (IndexNumericField) field;
                NumericFieldSchemaConfiguration configuration = (NumericFieldSchemaConfiguration) fieldSchema.getConfiguration();

                String name = configuration.getName() + IndexNumericField.PREFIX;
                Field indexedField;
                switch (configuration.getType()) {
                    case INT:
                        indexedField = new IntField(name, numericField.get().intValue(),
                                fieldSchema.getFieldType());
                        break;
                    case LONG:
                        indexedField = new LongField(name, numericField.get().longValue(),
                                fieldSchema.getFieldType());
                        break;
                    case FLOAT:
                        indexedField = new FloatField(name, numericField.get().floatValue(),
                                fieldSchema.getFieldType());
                        break;
                    case DOUBLE:
                        indexedField = new DoubleField(name, numericField.get().doubleValue(),
                                fieldSchema.getFieldType());
                        break;
                    default:
                        indexedField = Assert.error();
                }

                doc.add(indexedField);
                doc.add(new StringField(configuration.getName(), numericField.get().toString(), Store.NO));
            } else if (field instanceof IndexStringField) {
                IndexStringField stringField = (IndexStringField) field;
                doc.add(new Field(fieldSchema.getConfiguration().getName(), stringField.get(),
                        fieldSchema.getFieldType()));
            } else if (field instanceof IndexTextField) {
                IndexTextField textField = (IndexTextField) field;
                doc.add(new Field(fieldSchema.getConfiguration().getName(), textField.get(),
                        fieldSchema.getFieldType()));
            } else
                Assert.error();
        }

        return doc;
    }

    public static IDocument createDocument(Document document) {
        List<IndexField> fields = new ArrayList<IndexField>();
        for (IndexableField field : document)
            fields.add(createField(field));

        return new IndexDocument(null, null, fields);
    }

    private static IndexField createField(IndexableField field) {
        if (field.numericValue() != null)
            return new IndexNumericField(field.name().substring(0, field.name().length() - IndexNumericField.PREFIX.length()),
                    field.boost(), field.numericValue());
        else if (field.stringValue() != null)
            return new IndexStringField(field.name(), field.boost(), field.stringValue());
        else if (field.readerValue() != null)
            return new IndexTextField(field.name(), field.boost(), field.readerValue());
        else
            return Assert.error();
    }
}
