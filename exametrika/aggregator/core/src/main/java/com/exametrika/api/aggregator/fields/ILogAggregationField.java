/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.fields;

import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.schema.ILogAggregationFieldSchema;
import com.exametrika.api.exadb.objectdb.fields.IStructuredBlobField;
import com.exametrika.common.json.JsonObject;
import com.exametrika.spi.aggregator.IComputeContext;


/**
 * The {@link ILogAggregationField} represents an aggregation log.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface ILogAggregationField extends IAggregationField, IStructuredBlobField<IAggregationRecord> {
    /**
     * Iterator on aggregation records.
     */
    interface IAggregationIterator extends IStructuredIterator<IAggregationRecord> {
        /**
         * Returns field.
         *
         * @return field
         */
        @Override
        ILogAggregationField getField();

        /**
         * Returns compute context of current record. Compute context is shared and must be used immediately.
         *
         * @return compute context
         */
        IComputeContext getComputeContext();

        /**
         * Returns Json representation of current record.
         *
         * @param index           representation index
         * @param includeTime     if true include aggregation time and period
         * @param includeMetadata if true metadata are included
         * @return Json representation of current record
         */
        Object getRepresentation(int index, boolean includeTime, boolean includeMetadata);
    }

    /**
     * Iterable on aggregation records.
     */
    interface IAggregationIterable extends IStructuredIterable<IAggregationRecord> {
        @Override
        IAggregationIterator iterator();
    }

    @Override
    ILogAggregationFieldSchema getSchema();

    /**
     * Returns iterable on all aggregation records.
     *
     * @return iterable on all aggregation records
     */
    @Override
    IAggregationIterable getRecords();

    /**
     * Returns iterable on aggregation records in specified range.
     *
     * @param startId identifier of first record in iterable
     * @param endId   identifier of end record or 0 if iterable is unbounded
     * @return iterable on aggregation records in specified range
     */
    @Override
    IAggregationIterable getRecords(long startId, long endId);

    /**
     * Returns reverse iterable on all aggregation records in current cycle.
     *
     * @return reverse iterable on all aggregation records to the past through cycles
     */
    @Override
    IAggregationIterable getReverseRecords();

    /**
     * Returns reverse iterable on aggregation records in specified range in current cycle.
     *
     * @param startId identifier of first record in iterable
     * @param endId   identifier of last record or 0 if iterable is unbounded
     * @return iterable on aggregation records in specified range to the past through cycles
     */
    @Override
    IAggregationIterable getReverseRecords(long startId, long endId);

    /**
     * Returns current metadata.
     *
     * @return current metadata or null if metadata are not set
     */
    JsonObject getMetadata();

    /**
     * Adds new aggregation record.
     *
     * @param value  aggregation value
     * @param time   end aggregation time
     * @param period aggregation period
     * @return first and last written record identifiers or {0, 0} if no records are written (i.e. specified empty value)
     */
    long[] add(IComponentValue value, long time, long period);
}
