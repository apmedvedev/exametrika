/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.values;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.api.aggregator.common.values.INameValue;
import com.exametrika.common.json.IJsonCollection;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.CacheSizes;
import com.exametrika.common.utils.Memory;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.aggregator.common.values.IFieldValueBuilder;
import com.exametrika.spi.aggregator.common.values.IMetricValueBuilder;


/**
 * The {@link NameBuilder} is a measurement value builder for name measurements.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class NameBuilder implements IMetricValueBuilder, INameValue {
    private static final int CACHE_SIZE = Memory.getShallowSize(NameBuilder.class);
    private final List<IFieldValueBuilder> fields;

    public NameBuilder(List<IFieldValueBuilder> fields) {
        Assert.notNull(fields);

        this.fields = fields;
    }

    @Override
    public List<IFieldValueBuilder> getFields() {
        return fields;
    }

    @Override
    public IJsonCollection toJson() {
        return toValue().toJson();
    }

    @Override
    public void set(IMetricValue value) {
        Assert.notNull(value);

        INameValue nameValue = (INameValue) value;
        Assert.isTrue(fields.size() >= nameValue.getFields().size());

        for (int i = 0; i < nameValue.getFields().size(); i++) {
            IFieldValueBuilder builder = fields.get(i);
            builder.set(nameValue.getFields().get(i));
        }
    }

    @Override
    public IMetricValue toValue() {
        List<IFieldValue> fields = new ArrayList<IFieldValue>(this.fields.size());
        for (IFieldValueBuilder field : this.fields)
            fields.add(field.toValue());

        return new NameValue(fields);
    }

    @Override
    public void clear() {
        for (int i = 0; i < fields.size(); i++)
            fields.get(i).clear();
    }

    @Override
    public int getCacheSize() {
        int cacheSize = 0;
        for (int i = 0; i < fields.size(); i++)
            cacheSize += fields.get(i).getCacheSize();

        return CACHE_SIZE + CacheSizes.getArrayListCacheSize(fields) + cacheSize;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof NameBuilder))
            return false;

        NameBuilder data = (NameBuilder) o;
        return fields.equals(data.fields);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(fields);
    }

    @Override
    public String toString() {
        return toValue().toString();
    }
}
