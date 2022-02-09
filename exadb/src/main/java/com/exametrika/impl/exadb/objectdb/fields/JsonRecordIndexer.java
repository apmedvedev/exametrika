/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.fields;

import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;
import com.exametrika.api.exadb.fulltext.schema.IFieldSchema;
import com.exametrika.api.exadb.objectdb.config.schema.JsonBlobFieldSchemaConfiguration;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.exadb.objectdb.fields.IField;
import com.exametrika.spi.exadb.objectdb.fields.IRecordIndexProvider;
import com.exametrika.spi.exadb.objectdb.fields.IRecordIndexer;


/**
 * The {@link JsonRecordIndexer} is a json record indexer.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class JsonRecordIndexer implements IRecordIndexer {
    private final IRecordIndexProvider indexProvider;
    private final IDocumentSchema documentSchema;

    public JsonRecordIndexer(IField field, IRecordIndexProvider indexProvider) {
        Assert.notNull(field);
        Assert.notNull(indexProvider);

        this.indexProvider = indexProvider;

        documentSchema = indexProvider.createDocumentSchema(((JsonBlobFieldSchemaConfiguration) field.getSchema(
        ).getConfiguration()).getDocumentSchemaFactory().createSchema());
    }

    @Override
    public void addRecord(Object record, long id) {
        reindex(record, id);
    }

    @Override
    public void removeRecord(Object record) {
        Assert.supports(false);
    }

    @Override
    public void reindex(Object r, long id) {
        JsonRecord record = (JsonRecord) r;
        JsonObject object = record.getValue();
        Object[] values = new Object[documentSchema.getFields().size()];
        int k = 0;
        for (int i = documentSchema.getConfiguration().getSystemFieldCount(); i < documentSchema.getFields().size(); i++) {
            IFieldSchema field = documentSchema.getFields().get(i);
            values[k] = object.get(field.getConfiguration().getName(), null);
            k++;
        }

        indexProvider.add(documentSchema, id, values);
    }
}
