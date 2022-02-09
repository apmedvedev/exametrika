/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.values;

import com.exametrika.api.aggregator.common.model.IMeasurementId;
import com.exametrika.api.aggregator.common.model.MeasurementId;
import com.exametrika.api.aggregator.common.model.Measurements;
import com.exametrika.api.aggregator.common.values.IInstanceRecord;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Memory;
import com.exametrika.common.utils.Numbers;
import com.exametrika.common.utils.Objects;


/**
 * The {@link InstanceRecord} is an instance record.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class InstanceRecord implements IInstanceRecord, Comparable<InstanceRecord> {
    private static final int CACHE_SIZE = Memory.getShallowSize(InstanceRecord.class);
    private final IMeasurementId id;
    private final JsonObject context;
    private long value;
    private final long time;

    public InstanceRecord(IMeasurementId id, JsonObject context, long value, long time) {
        Assert.notNull(id);

        this.id = id;
        this.context = context != null ? context : JsonUtils.EMPTY_OBJECT;
        this.value = value;
        this.time = time;
    }

    @Override
    public IMeasurementId getId() {
        return id;
    }

    @Override
    public JsonObject getContext() {
        return context;
    }

    @Override
    public long getValue() {
        return value;
    }

    public void setValue(long value) {
        this.value = value;
    }

    @Override
    public long getTime() {
        return time;
    }

    @Override
    public JsonObject toJson() {
        JsonObjectBuilder fields = new JsonObjectBuilder();

        fields.put("id", Measurements.toJson(id));
        if (!context.isEmpty())
            fields.put("context", context);
        fields.put("value", value);
        fields.put("time", time);

        return fields.toJson();
    }

    public int getCacheSize() {
        int cacheSize = 0;
        if (context != null)
            cacheSize += context.getCacheSize();
        if (id instanceof MeasurementId)
            cacheSize += ((MeasurementId) id).getCacheSize();

        return CACHE_SIZE + cacheSize;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof InstanceRecord))
            return false;

        InstanceRecord data = (InstanceRecord) o;
        return value == data.value && id.equals(data.id) && context.equals(data.context);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value, id, context);
    }

    @Override
    public String toString() {
        return toJson().toString();
    }

    @Override
    public int compareTo(InstanceRecord o) {
        int res = Numbers.compare(value, o.value);
        if (res != 0)
            return res;

        res = Numbers.compare(id.hashCode(), o.id.hashCode());
        if (res != 0)
            return res;

        res = Numbers.compare(context.hashCode(), o.context.hashCode());
        if (res != 0)
            return res;

        return 0;
    }
}
