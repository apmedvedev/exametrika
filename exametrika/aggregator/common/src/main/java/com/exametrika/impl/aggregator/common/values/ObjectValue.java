/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.values;

import com.exametrika.api.aggregator.common.values.IObjectValue;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;


/**
 * The {@link ObjectValue} is a measurement data for object fields.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ObjectValue implements IObjectValue {
    private final Object object;

    public ObjectValue(Object object) {
        Assert.notNull(object);

        this.object = JsonUtils.checkValue(object);
    }

    @Override
    public Object getObject() {
        return object;
    }

    @Override
    public JsonObject toJson() {
        JsonObjectBuilder fields = new JsonObjectBuilder();
        fields.put("instanceOf", "object");
        fields.put("object", object);
        return fields.toJson();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ObjectValue))
            return false;

        ObjectValue data = (ObjectValue) o;
        return object.equals(data.object);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(object);
    }

    @Override
    public String toString() {
        return toJson().toString();
    }
}
