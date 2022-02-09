/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.fields;

import com.exametrika.api.aggregator.common.values.IObjectValue;
import com.exametrika.api.aggregator.config.model.AggregationComponentTypeSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.LogSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.LogAggregationFieldSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;
import com.exametrika.api.exadb.fulltext.schema.IFieldSchema;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.exadb.objectdb.fields.IField;
import com.exametrika.spi.exadb.objectdb.fields.IRecordIndexProvider;
import com.exametrika.spi.exadb.objectdb.fields.IRecordIndexer;


/**
 * The {@link AggregationRecordIndexer} is a aggregation record indexer.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class AggregationRecordIndexer implements IRecordIndexer {
    private final IRecordIndexProvider indexProvider;
    private final IDocumentSchema documentSchema;

    public AggregationRecordIndexer(IField field, IRecordIndexProvider indexProvider) {
        Assert.notNull(field);
        Assert.notNull(indexProvider);

        this.indexProvider = indexProvider;

        AggregationComponentTypeSchemaConfiguration componentType = ((LogAggregationFieldSchemaConfiguration) field.getSchema().getConfiguration()).getComponentType();
        if (componentType.isLog() && ((LogSchemaConfiguration) componentType.getMetricTypes().get(0)).isFullTextIndex()) {
            LogSchemaConfiguration log = (LogSchemaConfiguration) componentType.getMetricTypes().get(0);
            documentSchema = indexProvider.createDocumentSchema(log.getDocument().createSchema());
        } else
            documentSchema = null;
    }

    @Override
    public void addRecord(Object record, long id) {
        indexProvider.add(0, ((AggregationRecord) record).getTime(), id);

        if (documentSchema != null)
            reindex(record, id);
    }

    @Override
    public void removeRecord(Object record) {
        Assert.supports(false);
    }

    @Override
    public void reindex(Object r, long id) {
        AggregationRecord record = (AggregationRecord) r;
        Assert.isTrue(record.getValue().getMetrics().size() == 1);
        IObjectValue value = (IObjectValue) record.getValue().getMetrics().get(0);
        if (value.getObject() instanceof JsonObject) {
            JsonObject object = (JsonObject) value.getObject();
            Object[] values = new Object[documentSchema.getFields().size()];
            int k = 0;
            for (int i = documentSchema.getConfiguration().getSystemFieldCount(); i < documentSchema.getFields().size(); i++) {
                IFieldSchema field = documentSchema.getFields().get(i);
                values[k] = object.get(field.getConfiguration().getName(), null);
                k++;
            }

            indexProvider.add(documentSchema, id, values);
        } else
            Assert.error();
    }
}
