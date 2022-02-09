/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.values;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.api.aggregator.common.model.MeasurementId;
import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.api.aggregator.common.values.IInstanceRecord;
import com.exametrika.api.aggregator.common.values.IInstanceValue;
import com.exametrika.common.io.IDataDeserialization;
import com.exametrika.common.io.IDataSerialization;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonSerializers;
import com.exametrika.impl.aggregator.common.model.DeserializeNameDictionary;
import com.exametrika.impl.aggregator.common.model.MeasurementSerializers;
import com.exametrika.impl.aggregator.common.model.SerializeNameDictionary;
import com.exametrika.spi.aggregator.common.values.IFieldValueSerializer;


/**
 * The {@link InstanceSerializer} is an implementation of {@link IFieldValueSerializer} for {@link InstanceValue}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class InstanceSerializer implements IFieldValueSerializer {
    private final boolean builder;
    private final boolean max;

    public InstanceSerializer(boolean builder, boolean max) {
        this.builder = builder;
        this.max = max;
    }

    @Override
    public void serialize(IDataSerialization serialization, IFieldValue value) {
        SerializeNameDictionary dictionary = serialization.getExtension(SerializeNameDictionary.EXTENTION_ID);

        IInstanceValue instanceValue = (IInstanceValue) value;

        serialization.writeInt(instanceValue.getRecords().size());
        for (IInstanceRecord record : instanceValue.getRecords()) {
            MeasurementSerializers.serializeMeasurementId(serialization, record.getId(), dictionary);
            JsonSerializers.serialize(serialization, record.getContext());
            serialization.writeLong(record.getValue());
            serialization.writeLong(record.getTime());
        }
    }

    @Override
    public IFieldValue deserialize(IDataDeserialization deserialization) {
        DeserializeNameDictionary dictionary = deserialization.getExtension(DeserializeNameDictionary.EXTENTION_ID);

        int count = deserialization.readInt();
        List<InstanceRecord> records = new ArrayList<InstanceRecord>(count);
        for (int i = 0; i < count; i++) {
            MeasurementId id = MeasurementSerializers.deserializeMeasurementId(deserialization, dictionary);
            JsonObject context = JsonSerializers.deserialize(deserialization);
            long value = deserialization.readLong();
            long time = deserialization.readLong();

            InstanceRecord record = new InstanceRecord(id, context, value, time);
            records.add(record);
        }

        if (builder)
            return new InstanceBuilder(max, records);
        else
            return new InstanceValue(records);
    }
}
