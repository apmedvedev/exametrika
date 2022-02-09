/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.values;

import java.util.List;

import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.api.aggregator.common.values.INameValue;
import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonArrayBuilder;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;


/**
 * The {@link NameValue} is a measurement value for name measurements.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class NameValue implements INameValue {
    private final List<IFieldValue> fields;

    public NameValue(List<? extends IFieldValue> fields) {
        Assert.notNull(fields);

        this.fields = Immutables.wrap(fields);
    }

    @Override
    public List<IFieldValue> getFields() {
        return fields;
    }

    @Override
    public JsonObject toJson() {
        JsonObjectBuilder fields = new JsonObjectBuilder();
        fields.put("instanceOf", "name");
        fields.put("fields", toJson(this.fields));

        return fields.toJson();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof NameValue))
            return false;

        NameValue data = (NameValue) o;
        return fields.equals(data.fields);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(fields);
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
