/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.fields;

import com.exametrika.api.exadb.objectdb.fields.IJsonRecord;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;


/**
 * The {@link JsonRecord} is an json record.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class JsonRecord implements IJsonRecord {
    private final JsonObject value;

    public JsonRecord(JsonObject value) {
        Assert.notNull(value);

        this.value = value;
    }

    @Override
    public JsonObject getValue() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof JsonRecord))
            return false;

        JsonRecord record = (JsonRecord) o;
        return value.equals(record.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
