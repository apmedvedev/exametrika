/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.fulltext;

import java.util.Arrays;
import java.util.List;

import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortField.Type;

import com.exametrika.api.exadb.fulltext.FieldSort.Kind;
import com.exametrika.api.exadb.fulltext.config.schema.NumericFieldSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.config.schema.StringFieldSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.config.schema.TextFieldSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;
import com.exametrika.api.exadb.fulltext.schema.IFieldSchema;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.impl.exadb.fulltext.IndexNumericField;


/**
 * The {@link Sort} is a sort criteria.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class Sort {
    private List<FieldSort> fields;

    public Sort(Kind kind) {
        this.fields = Immutables.wrap(Arrays.asList(new FieldSort(kind)));
    }

    public Sort(Kind kind, boolean ascending) {
        this.fields = Immutables.wrap(Arrays.asList(new FieldSort(kind, ascending)));
    }

    public Sort(String field) {
        this.fields = Immutables.wrap(Arrays.asList(new FieldSort(field)));
    }

    public Sort(String field, boolean ascending) {
        this.fields = Immutables.wrap(Arrays.asList(new FieldSort(field, ascending)));
    }

    public Sort(FieldSort... fields) {
        this.fields = Immutables.wrap(Arrays.asList(fields));
    }

    public List<FieldSort> getFields() {
        return fields;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof Sort))
            return false;

        Sort sort = (Sort) o;
        return fields.equals(sort.fields);
    }

    @Override
    public int hashCode() {
        return fields.hashCode();
    }

    @Override
    public String toString() {
        return fields.toString();
    }

    public org.apache.lucene.search.Sort createSort(IDocumentSchema schema) {
        SortField[] fieldSorts = new SortField[fields.size()];
        for (int i = 0; i < fields.size(); i++)
            fieldSorts[i] = createFieldSort(schema, fields.get(i));

        return new org.apache.lucene.search.Sort(fieldSorts);
    }

    private SortField createFieldSort(IDocumentSchema schema, FieldSort fieldSort) {
        if (fieldSort.getKind() == Kind.RELEVANCE)
            return new SortField(null, Type.SCORE, !fieldSort.isAscending());
        else if (fieldSort.getKind() == Kind.DOCUMENT)
            return new SortField(null, Type.DOC, !fieldSort.isAscending());
        else {
            Assert.isTrue(fieldSort.getKind() == Kind.FIELD);

            IFieldSchema fieldSchema = schema.findField(fieldSort.getField());
            Assert.notNull(fieldSchema);
            Assert.isTrue(fieldSchema.isSortable());

            Type type;

            String field;
            if (fieldSchema.getConfiguration() instanceof StringFieldSchemaConfiguration ||
                    (fieldSchema.getConfiguration() instanceof TextFieldSchemaConfiguration)) {
                type = Type.STRING;
                field = fieldSort.getField();
            } else if (fieldSchema.getConfiguration() instanceof NumericFieldSchemaConfiguration) {
                NumericFieldSchemaConfiguration numericConfiguration = (NumericFieldSchemaConfiguration) fieldSchema.getConfiguration();
                switch (numericConfiguration.getType()) {
                    case INT:
                        type = Type.INT;
                        break;
                    case LONG:
                        type = Type.LONG;
                        break;
                    case FLOAT:
                        type = Type.FLOAT;
                        break;
                    case DOUBLE:
                        type = Type.DOUBLE;
                        break;
                    default:
                        return Assert.error();
                }

                field = fieldSort.getField() + IndexNumericField.PREFIX;
            } else
                return Assert.error();

            return new SortField(field, type, !fieldSort.isAscending());
        }
    }
}
