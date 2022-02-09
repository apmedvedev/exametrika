/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.values;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import com.exametrika.api.aggregator.common.model.IMeasurementId;
import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.api.aggregator.common.values.IInstanceValue;
import com.exametrika.common.json.IJsonCollection;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.CacheSizes;
import com.exametrika.common.utils.Memory;
import com.exametrika.common.utils.Pair;
import com.exametrika.spi.aggregator.common.values.IFieldValueBuilder;
import com.exametrika.spi.aggregator.common.values.config.FieldValueSchemaConfiguration;


/**
 * The {@link InstanceBuilder} is a builder containing measurements of instance records.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class InstanceBuilder implements IFieldValueBuilder, IInstanceValue {
    private static final int CACHE_SIZE = Memory.getShallowSize(InstanceBuilder.class);
    private final SortedSet<InstanceRecord> records;
    private final Map<Pair<IMeasurementId, JsonObject>, InstanceRecord> recordsMap;

    public InstanceBuilder(boolean max) {
        records = new TreeSet(new InstanceRecordComparator(max));
        recordsMap = new HashMap<Pair<IMeasurementId, JsonObject>, InstanceRecord>();
    }

    public InstanceBuilder(boolean max, Collection<InstanceRecord> records) {
        TreeSet set = new TreeSet(new InstanceRecordComparator(max));
        set.addAll(records);

        Map<Pair<IMeasurementId, JsonObject>, InstanceRecord> recordsMap = new HashMap<Pair<IMeasurementId, JsonObject>, InstanceRecord>();
        for (InstanceRecord record : records)
            recordsMap.put(new Pair<IMeasurementId, JsonObject>(record.getId(), record.getContext()), record);

        this.records = set;
        this.recordsMap = recordsMap;
    }

    @Override
    public SortedSet<InstanceRecord> getRecords() {
        return records;
    }

    public Map<Pair<IMeasurementId, JsonObject>, InstanceRecord> getRecordsMap() {
        return recordsMap;
    }

    @Override
    public IJsonCollection toJson() {
        return toValue().toJson();
    }

    @Override
    public void set(IFieldValue value) {
        Assert.notNull(value);

        InstanceValue instanceValue = (InstanceValue) value;

        records.clear();
        records.addAll(instanceValue.getRecords());

        recordsMap.clear();
        for (InstanceRecord record : instanceValue.getRecords())
            recordsMap.put(new Pair<IMeasurementId, JsonObject>(record.getId(), record.getContext()), record);
    }

    @Override
    public InstanceValue toValue() {
        return new InstanceValue(new ArrayList<InstanceRecord>(records));
    }

    @Override
    public void clear() {
        records.clear();
        recordsMap.clear();
    }

    @Override
    public void normalizeEnd(long count) {
    }

    @Override
    public void normalizeDerived(FieldValueSchemaConfiguration fieldSchemaConfiguration, long sum) {
    }

    @Override
    public int getCacheSize() {
        int cacheSize = 0;
        for (InstanceRecord record : records)
            cacheSize += record.getCacheSize();

        return CACHE_SIZE + CacheSizes.getTreeSetCacheSize(records) + CacheSizes.getLinkedHashMapCacheSize(recordsMap) +
                Memory.getShallowSize(Pair.class) * records.size() + cacheSize;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof InstanceBuilder))
            return false;

        InstanceBuilder data = (InstanceBuilder) o;
        return records.equals(data.records);
    }

    @Override
    public int hashCode() {
        return records.hashCode();
    }

    @Override
    public String toString() {
        return toValue().toString();
    }

    private static class InstanceRecordComparator implements Comparator<InstanceRecord> {
        private final boolean max;

        public InstanceRecordComparator(boolean max) {
            this.max = max;
        }

        @Override
        public int compare(InstanceRecord o1, InstanceRecord o2) {
            int res = o1.compareTo(o2);
            return max ? -res : res;
        }
    }
}
