/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.values;

import java.util.Set;
import java.util.UUID;

import com.exametrika.api.aggregator.common.values.IStackIdsValue;
import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonArrayBuilder;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;


/**
 * The {@link StackIdsValue} is a measurement value for StackIds measurements.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class StackIdsValue implements IStackIdsValue {
    private final Set<UUID> ids;

    public StackIdsValue(Set<UUID> ids) {
        this.ids = Immutables.wrap(ids);
    }

    @Override
    public Set<UUID> getIds() {
        return ids;
    }

    @Override
    public JsonObject toJson() {
        JsonObjectBuilder fields = new JsonObjectBuilder();
        fields.put("instanceOf", "stackIds");
        if (ids != null)
            fields.put("ids", toJson(ids));

        return fields.toJson();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof StackIdsValue))
            return false;

        StackIdsValue data = (StackIdsValue) o;
        return Objects.equals(ids, data.ids);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(ids);
    }

    @Override
    public String toString() {
        return toJson().toString();
    }

    private JsonArray toJson(Set<UUID> ids) {
        JsonArrayBuilder builder = new JsonArrayBuilder();
        for (UUID field : ids)
            builder.add(field.toString());

        return builder.toJson();
    }
}
