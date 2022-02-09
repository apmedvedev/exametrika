/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.values;

import java.util.Map;
import java.util.SortedSet;

import com.exametrika.api.aggregator.common.model.IMeasurementId;
import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.api.aggregator.common.values.IInstanceRecord;
import com.exametrika.api.aggregator.common.values.IInstanceValue;
import com.exametrika.api.aggregator.common.values.config.InstanceValueSchemaConfiguration;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Pair;
import com.exametrika.spi.aggregator.common.values.IAggregationContext;
import com.exametrika.spi.aggregator.common.values.IFieldAggregator;
import com.exametrika.spi.aggregator.common.values.IFieldValueBuilder;


/**
 * The {@link InstanceAggregator} is an implementation of {@link IFieldAggregator} for instance fields.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class InstanceAggregator implements IFieldAggregator {
    private final InstanceValueSchemaConfiguration configuration;

    public InstanceAggregator(InstanceValueSchemaConfiguration configuration) {
        Assert.notNull(configuration);

        this.configuration = configuration;
    }

    @Override
    public void aggregate(IFieldValueBuilder fields, IFieldValue fieldsToAdd, IAggregationContext context) {
        IInstanceValue value = (IInstanceValue) fieldsToAdd;
        InstanceBuilder builder = (InstanceBuilder) fields;
        SortedSet<InstanceRecord> records = builder.getRecords();
        Map<Pair<IMeasurementId, JsonObject>, InstanceRecord> recordsMap = builder.getRecordsMap();

        for (IInstanceRecord record : value.getRecords()) {
            InstanceRecord existingRecord = recordsMap.get(new Pair<IMeasurementId, JsonObject>(record.getId(), record.getContext()));
            if (existingRecord != null) {
                if (compare(existingRecord, record.getValue()) <= 0)
                    continue;

                records.remove(existingRecord);
                existingRecord.setValue(record.getValue());
                records.add(existingRecord);
                continue;
            }

            InstanceRecord last = null;
            if (records.size() >= configuration.getInstanceCount()) {
                last = records.last();
                if (compare(last, record.getValue()) <= 0)
                    continue;
            }

            records.add((InstanceRecord) record);
            recordsMap.put(new Pair<IMeasurementId, JsonObject>(record.getId(), record.getContext()), (InstanceRecord) record);

            if (records.size() > configuration.getInstanceCount()) {
                records.remove(last);
                recordsMap.remove(new Pair<IMeasurementId, JsonObject>(last.getId(), last.getContext()));
            }
        }
    }

    private int compare(InstanceRecord record, long value) {
        if (record.getValue() == value)
            return 0;
        else if (record.getValue() < value)
            return configuration.isMax() ? 1 : -1;
        else
            return configuration.isMax() ? -1 : 1;
    }
}
