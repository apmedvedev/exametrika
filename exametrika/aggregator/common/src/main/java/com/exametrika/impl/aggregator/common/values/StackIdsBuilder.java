/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.values;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.api.aggregator.common.values.IStackIdsValue;
import com.exametrika.common.json.IJsonCollection;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.CacheSizes;
import com.exametrika.common.utils.Memory;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.aggregator.common.values.IMetricValueBuilder;


/**
 * The {@link StackIdsBuilder} is a measurement value builder for stackIds measurements.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class StackIdsBuilder implements IMetricValueBuilder, IStackIdsValue {
    private static final int CACHE_SIZE = Memory.getShallowSize(StackIdsBuilder.class);
    private Set<UUID> ids;

    public StackIdsBuilder() {
    }

    public StackIdsBuilder(Set<UUID> ids) {
        this.ids = ids;
    }

    @Override
    public Set<UUID> getIds() {
        return ids;
    }

    @Override
    public IJsonCollection toJson() {
        return toValue().toJson();
    }

    @Override
    public void set(IMetricValue value) {
        Assert.notNull(value);

        IStackIdsValue idsValue = (IStackIdsValue) value;
        if (idsValue.getIds() != null)
            ids = new LinkedHashSet<UUID>(idsValue.getIds());
        else
            ids = null;
    }

    public void addIds(Set<UUID> ids) {
        if (ids == null)
            return;

        if (this.ids == null)
            this.ids = new LinkedHashSet<UUID>();

        this.ids.addAll(ids);
    }

    @Override
    public IMetricValue toValue() {
        Set<UUID> ids = null;
        if (this.ids != null)
            ids = new LinkedHashSet<UUID>(this.ids);
        return new StackIdsValue(ids);
    }

    @Override
    public void clear() {
        ids = null;
    }

    @Override
    public int getCacheSize() {
        return CACHE_SIZE + (ids != null ? (CacheSizes.getLinkedHashSetCacheSize(ids) + CacheSizes.UUID_CACHE_SIZE * ids.size()) : 0);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof StackIdsBuilder))
            return false;

        StackIdsBuilder data = (StackIdsBuilder) o;
        return Objects.equals(ids, data.ids);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(ids);
    }

    @Override
    public String toString() {
        return toValue().toString();
    }
}
