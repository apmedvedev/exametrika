/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.values;

import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.api.aggregator.common.values.IObjectValue;
import com.exametrika.common.json.IJsonCollection;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Memory;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.aggregator.common.values.IMetricValueBuilder;


/**
 * The {@link ObjectBuilder} is a measurement data for object fields.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class ObjectBuilder implements IMetricValueBuilder, IObjectValue {
    private static final int CACHE_SIZE = Memory.getShallowSize(ObjectBuilder.class);
    private Object object;

    public ObjectBuilder() {
        this("");
    }

    public ObjectBuilder(Object object) {
        Assert.notNull(object);

        this.object = JsonUtils.checkValue(object);
    }

    @Override
    public Object getObject() {
        return object;
    }

    public void setObject(Object object) {
        Assert.notNull(object);

        this.object = JsonUtils.checkValue(object);
    }

    @Override
    public IJsonCollection toJson() {
        return toValue().toJson();
    }

    @Override
    public void set(IMetricValue value) {
        Assert.notNull(value);

        IObjectValue objectValue = (IObjectValue) value;

        this.object = objectValue.getObject();
    }

    @Override
    public IMetricValue toValue() {
        return new ObjectValue(object);
    }

    @Override
    public void clear() {
    }

    @Override
    public int getCacheSize() {
        return CACHE_SIZE + JsonUtils.getCacheSize(object);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ObjectBuilder))
            return false;

        ObjectBuilder data = (ObjectBuilder) o;
        return object.equals(data.object);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(object);
    }

    @Override
    public String toString() {
        return toValue().toString();
    }
}
