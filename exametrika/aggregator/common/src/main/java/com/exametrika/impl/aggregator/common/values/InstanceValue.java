/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.values;

import java.util.List;

import com.exametrika.api.aggregator.common.values.IInstanceValue;
import com.exametrika.common.json.IJsonCollection;
import com.exametrika.common.json.JsonArrayBuilder;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;


/**
 * The {@link InstanceValue} is a value containing measurements of instance records.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class InstanceValue implements IInstanceValue {
    private final List<InstanceRecord> records;

    public InstanceValue(List<InstanceRecord> records) {
        Assert.notNull(records);

        this.records = Immutables.wrap(records);
    }

    @Override
    public List<InstanceRecord> getRecords() {
        return records;
    }

    @Override
    public IJsonCollection toJson() {
        JsonArrayBuilder records = new JsonArrayBuilder();
        for (InstanceRecord record : this.records)
            records.add(record.toJson());

        JsonObjectBuilder fields = new JsonObjectBuilder();
        fields.put("instanceOf", "instance");
        fields.put("records", records);

        return fields.toJson();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof InstanceValue))
            return false;

        InstanceValue data = (InstanceValue) o;
        return records.equals(data.records);
    }

    @Override
    public int hashCode() {
        return records.hashCode();
    }

    @Override
    public String toString() {
        return toJson().toString();
    }
}
