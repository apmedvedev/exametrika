/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.fulltext;

import java.util.List;

import com.exametrika.api.exadb.fulltext.IDocument;
import com.exametrika.api.exadb.fulltext.IField;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.impl.exadb.fulltext.schema.DocumentSchema;


/**
 * The {@link IndexDocument} is a index document.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class IndexDocument implements IDocument {
    private final DocumentSchema schema;
    private final List<IField> fields;
    private final Object context;

    public IndexDocument(Object context, DocumentSchema schema, List<? extends IField> fields) {
        Assert.notNull(fields);

        this.schema = schema;
        this.fields = Immutables.wrap(fields);
        this.context = context;
    }

    DocumentSchema getSchema() {
        return schema;
    }

    @Override
    public Object getContext() {
        return context;
    }

    @Override
    public List<IField> getFields() {
        return fields;
    }

    @Override
    public <T extends IField> T findField(String fieldName) {
        Assert.notNull(fieldName);

        for (IField field : this.fields) {
            if (field.getName().equals(fieldName))
                return (T) field;
        }

        return null;
    }

    @Override
    public String toString() {
        return fields.toString();
    }
}
