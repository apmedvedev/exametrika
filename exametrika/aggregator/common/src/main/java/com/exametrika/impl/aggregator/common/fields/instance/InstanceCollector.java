/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.fields.instance;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import com.exametrika.api.aggregator.common.meters.config.InstanceFieldConfiguration;
import com.exametrika.api.aggregator.common.model.IMeasurementIdProvider;
import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Numbers;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.aggregator.common.values.InstanceRecord;
import com.exametrika.impl.aggregator.common.values.InstanceValue;
import com.exametrika.spi.aggregator.common.fields.IInstanceContextProvider;
import com.exametrika.spi.aggregator.common.meters.IFieldCollector;


/**
 * The {@link InstanceCollector} is an implementation of {@link IFieldCollector} for instance long fields.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class InstanceCollector implements IFieldCollector {
    private final InstanceFieldConfiguration configuration;
    private final IMeasurementIdProvider idProvider;
    private final IInstanceContextProvider contextProvider;
    private final SortedSet<Record> records;
    private final Map<JsonObject, Record> recordsMap = new HashMap<JsonObject, Record>();
    private final boolean max;

    public InstanceCollector(InstanceFieldConfiguration configuration, IMeasurementIdProvider idProvider, IInstanceContextProvider contextProvider) {
        Assert.notNull(configuration);
        Assert.notNull(idProvider);
        Assert.notNull(contextProvider);

        this.configuration = configuration;
        this.idProvider = idProvider;
        this.contextProvider = contextProvider;
        max = configuration.isMax();
        records = new TreeSet(new RecordComparator(max));
    }

    @Override
    public void update(long value) {
        JsonObject context = contextProvider.getContext();
        if (context == null)
            context = JsonUtils.EMPTY_OBJECT;
        Record record = recordsMap.get(context);
        if (record != null) {
            if (compare(record, value) <= 0)
                return;

            records.remove(record);
            record.value = value;
            records.add(record);
            return;
        }

        Record last = null;
        if (records.size() >= configuration.getInstanceCount()) {
            last = records.last();
            if (compare(last, value) <= 0)
                return;
        }

        record = new Record(context, value);
        records.add(record);
        recordsMap.put(record.context, record);

        if (records.size() > configuration.getInstanceCount()) {
            records.remove(last);
            recordsMap.remove(last.context);
        }
    }

    @Override
    public IFieldValue extract(long count, double approximationMultiplier, boolean clear) {
        List<InstanceRecord> records = new ArrayList<InstanceRecord>(this.records.size());
        for (Record record : this.records)
            records.add(new InstanceRecord(idProvider.get(), record.context, record.value, contextProvider.getExtractionTime()));

        IFieldValue value = new InstanceValue(records);

        if (clear) {
            this.records.clear();
            this.recordsMap.clear();
        }

        return value;
    }

    private int compare(Record record, long value) {
        if (record.value == value)
            return 0;
        else if (record.value < value)
            return max ? 1 : -1;
        else
            return max ? -1 : 1;
    }

    private static class Record implements Comparable<Record> {
        private final JsonObject context;
        private long value;

        public Record(JsonObject context, long value) {
            this.context = context;
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof Record))
                return false;

            Record data = (Record) o;
            return value == data.value && Objects.equals(context, data.context);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(value, context);
        }

        @Override
        public int compareTo(Record o) {
            int res = Numbers.compare(value, o.value);
            if (res != 0)
                return res;

            res = Numbers.compare(context.hashCode(), o.context.hashCode());
            if (res != 0)
                return res;

            return 0;
        }
    }

    private static class RecordComparator implements Comparator<Record> {
        private final boolean max;

        public RecordComparator(boolean max) {
            this.max = max;
        }

        @Override
        public int compare(Record o1, Record o2) {
            int res = o1.compareTo(o2);
            return max ? -res : res;
        }
    }
}
