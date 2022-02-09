/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.values;

import java.util.List;

import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.api.aggregator.common.values.IStackValue;
import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonArrayBuilder;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;


/**
 * The {@link StackValue} is a measurement data for stack field measurements.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class StackValue implements IStackValue {
    private final List<IFieldValue> inherentFields;
    private final List<IFieldValue> totalFields;

    public StackValue(List<? extends IFieldValue> inherentFields, List<? extends IFieldValue> totalFields) {
        Assert.notNull(inherentFields);
        Assert.notNull(totalFields);

        this.inherentFields = Immutables.wrap(inherentFields);
        this.totalFields = Immutables.wrap(totalFields);
    }

    @Override
    public List<IFieldValue> getInherentFields() {
        return inherentFields;
    }

    @Override
    public List<IFieldValue> getTotalFields() {
        return totalFields;
    }

    @Override
    public JsonObject toJson() {
        JsonObjectBuilder fields = new JsonObjectBuilder();
        fields.put("instanceOf", "stack");
        fields.put("inherent", toJson(inherentFields));
        fields.put("total", toJson(totalFields));
        return fields.toJson();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof StackValue))
            return false;

        StackValue data = (StackValue) o;
        return inherentFields.equals(data.inherentFields) && totalFields.equals(data.totalFields);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(inherentFields, totalFields);
    }

    @Override
    public String toString() {
        return toJson().toString();
    }

    private JsonArray toJson(List<IFieldValue> fieldList) {
        JsonArrayBuilder fields = new JsonArrayBuilder();
        for (IFieldValue field : fieldList)
            fields.add(field.toJson());

        return fields.toJson();
    }
}
