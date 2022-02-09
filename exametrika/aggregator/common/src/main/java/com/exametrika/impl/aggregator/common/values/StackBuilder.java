/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.values;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.api.aggregator.common.values.IStackValue;
import com.exametrika.common.json.IJsonCollection;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.CacheSizes;
import com.exametrika.common.utils.Memory;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.aggregator.common.values.IFieldValueBuilder;
import com.exametrika.spi.aggregator.common.values.IMetricValueBuilder;


/**
 * The {@link StackBuilder} is a measurement value builder for stack measurements.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class StackBuilder implements IMetricValueBuilder, IStackValue {
    private static final int CACHE_SIZE = Memory.getShallowSize(StackBuilder.class);
    private final List<IFieldValueBuilder> inherentFields;
    private final List<IFieldValueBuilder> totalFields;

    public StackBuilder(List<IFieldValueBuilder> inherentFields, List<IFieldValueBuilder> totalFields) {
        Assert.notNull(inherentFields);
        Assert.notNull(totalFields);

        this.inherentFields = inherentFields;
        this.totalFields = totalFields;
    }

    @Override
    public List<IFieldValueBuilder> getInherentFields() {
        return inherentFields;
    }

    @Override
    public List<IFieldValueBuilder> getTotalFields() {
        return totalFields;
    }

    @Override
    public IJsonCollection toJson() {
        return toValue().toJson();
    }

    @Override
    public void set(IMetricValue value) {
        Assert.notNull(value);

        IStackValue stackValue = (IStackValue) value;
        Assert.isTrue(inherentFields.size() >= stackValue.getInherentFields().size());
        Assert.isTrue(totalFields.size() >= stackValue.getTotalFields().size());

        for (int i = 0; i < stackValue.getInherentFields().size(); i++) {
            IFieldValueBuilder builder = inherentFields.get(i);
            builder.set(stackValue.getInherentFields().get(i));
        }

        for (int i = 0; i < stackValue.getTotalFields().size(); i++) {
            IFieldValueBuilder builder = totalFields.get(i);
            builder.set(stackValue.getTotalFields().get(i));
        }
    }

    @Override
    public IMetricValue toValue() {
        List<IFieldValue> inherentFields = new ArrayList<IFieldValue>(this.inherentFields.size());
        for (IFieldValueBuilder field : this.inherentFields)
            inherentFields.add(field.toValue());

        List<IFieldValue> totalFields = new ArrayList<IFieldValue>(this.totalFields.size());
        for (IFieldValueBuilder field : this.totalFields)
            totalFields.add(field.toValue());

        return new StackValue(inherentFields, totalFields);
    }

    @Override
    public void clear() {
        for (int i = 0; i < inherentFields.size(); i++)
            inherentFields.get(i).clear();

        for (int i = 0; i < totalFields.size(); i++)
            totalFields.get(i).clear();
    }

    @Override
    public int getCacheSize() {
        int cacheSize = 0;
        for (int i = 0; i < inherentFields.size(); i++)
            cacheSize += inherentFields.get(i).getCacheSize();
        for (int i = 0; i < totalFields.size(); i++)
            cacheSize += totalFields.get(i).getCacheSize();

        return CACHE_SIZE + CacheSizes.getArrayListCacheSize(inherentFields) + CacheSizes.getArrayListCacheSize(totalFields) + cacheSize;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof StackBuilder))
            return false;

        StackBuilder data = (StackBuilder) o;
        return inherentFields.equals(data.inherentFields) && totalFields.equals(data.totalFields);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(inherentFields, totalFields);
    }

    @Override
    public String toString() {
        return toValue().toString();
    }
}
