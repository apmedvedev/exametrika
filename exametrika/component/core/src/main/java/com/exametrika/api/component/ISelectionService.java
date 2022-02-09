/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component;

import java.util.Iterator;

import com.exametrika.api.aggregator.Location;
import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.fields.IAggregationRecord;
import com.exametrika.api.aggregator.fields.ILogAggregationField;
import com.exametrika.api.aggregator.nodes.IAggregationNode;
import com.exametrika.api.aggregator.schema.IPeriodSpaceSchema;
import com.exametrika.common.json.JsonArray;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Pair;
import com.exametrika.spi.aggregator.IComponentAccessorFactory;
import com.exametrika.spi.aggregator.IComputeContext;


/**
 * The {@link ISelectionService} represents a selection service.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface ISelectionService {
    String NAME = "component.SelectionService";

    /**
     * Returns period space schema;
     *
     * @return period space schema
     */
    IPeriodSpaceSchema getSpaceSchema();

    /**
     * Iterator on selected records.
     */
    interface ISelectionIterator extends Iterator<IAggregationRecord> {
        /**
         * Returns field.
         *
         * @return field
         */
        ILogAggregationField getField();

        /**
         * Returns value of current record.
         *
         * @return value of current record
         */
        IAggregationRecord get();

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
     * Iterable on selected records.
     */
    interface ISelectionIterable extends Iterable<IAggregationRecord> {
        @Override
        ISelectionIterator iterator();
    }

    /**
     * Representation builder is used to build value representation.
     */
    public interface IRepresentationBuilder {
        /**
         * Called when specified value representation is being built.
         *
         * @param time            time
         * @param aggregationNode aggregation node
         * @param value           value
         * @param accessorFactory accessor factory
         * @param computeContext  compute context
         * @return value representation or null if representation is not found
         */
        Object build(long time, IAggregationNode aggregationNode, IComponentValue value, IComponentAccessorFactory accessorFactory,
                     IComputeContext computeContext);
    }

    /**
     * Page direction.
     */
    enum PageDirection {
        /**
         * First record.
         */
        FIRST,
        /**
         * Previous record.
         */
        PREVIOUS,
        /**
         * Next record.
         */
        NEXT,
        /**
         * Last record.
         */
        LAST
    }

    /**
     * Page information.
     */
    class PageInfo {
        /**
         * Period type.
         */
        public final String periodType;
        /**
         * Selection start time.
         */
        public final long startTime;
        /**
         * Selection current time.
         */
        public final long currentTime;
        /**
         * First cycle identifier.
         */
        public final String firstCycleId;
        /**
         * First record identifier.
         */
        public final long firstRecordId;
        /**
         * Last cycle identifier.
         */
        public final String lastCycleId;
        /**
         * Last record identifier.
         */
        public final long lastRecordId;
        /**
         * Direction record identifier.
         */
        public final PageDirection direction;
        /**
         * Is page first?
         */
        public final boolean firstPage;
        /**
         * Is page last?
         */
        public final boolean lastPage;
        /**
         * Page index. Negative values means offsets from last page.
         */
        public final int pageIndex;

        public PageInfo(String periodType, long startTime, long currentTime, String firstCycleId, long firstRecordId,
                        String lastCycleId, long lastRecordId, PageDirection direction, boolean firstPage, boolean lastPage, int pageIndex) {
            Assert.notNull(periodType);
            Assert.notNull(direction);

            this.periodType = periodType;
            this.startTime = startTime;
            this.currentTime = currentTime;
            this.firstCycleId = firstCycleId;
            this.firstRecordId = firstRecordId;
            this.lastCycleId = lastCycleId;
            this.lastRecordId = lastRecordId;
            this.direction = direction;
            this.firstPage = firstPage;
            this.lastPage = lastPage;
            this.pageIndex = pageIndex;
        }
    }

    /**
     * Finds aggregation node in period of specified type whose start time is less or equal to specified time
     * and end time is greater or equal to specified time minus 10 seconds.
     *
     * @param periodType    period type
     * @param time          moment of time
     * @param location      node location
     * @param componentType node component type
     * @return aggregation node or null if aggregation node is not found for specified moment of time
     */
    IAggregationNode findAggregationNode(String periodType, long time, Location location, String componentType);

    /**
     * Finds aggregation node in past nearest to a given moment of time.
     *
     * @param periodType    period type
     * @param time          moment of time
     * @param location      node location
     * @param componentType node component type
     * @return aggregation node or null if aggregation node is not found
     */
    IAggregationNode findNearestAggregationNode(String periodType, long time, Location location, String componentType);

    /**
     * Finds aggregation node representation in period of specified type for a given moment of time.
     *
     * @param periodType    period type
     * @param time          moment of time
     * @param location      node location
     * @param componentType node component type
     * @param index         representation index
     * @return aggregation node representation or null if aggregation node representation is not found for specified moment of time
     */
    Object findRepresentation(String periodType, long time, Location location, String componentType, int index);

    /**
     * Builds aggregation node representation in period of specified type for a given moment of time.
     *
     * @param periodType    period type
     * @param time          moment of time
     * @param location      node location
     * @param componentType node component type
     * @param index         representation index
     * @param builder       representation builder
     * @return representation or null if aggregation node representation is not found for specified moment of time
     */
    Object buildRepresentation(String periodType, long time, Location location, String componentType, int index,
                               IRepresentationBuilder builder);

    /**
     * Returns aggregation records for specified period type and time interval in reverse creation order (last record comes first).
     *
     * @param periodType    period type
     * @param time          start selection time
     * @param location      node location
     * @param componentType node component type
     * @return returns iterable on aggregation records in reverse creation order
     */
    ISelectionIterable getAggregationRecords(String periodType, long time, Location location, String componentType);

    /**
     * Builds page records.
     *
     * @param periodType    period type
     * @param startTime     start time
     * @param currentTime   current time
     * @param location      location
     * @param componentType node component type
     * @param recordCount   record count
     * @param pageInfo      page info or null if page is selected first time
     * @param index         representation index
     * @param builder       representation builder
     * @return pair of array of page records in reverse creation order and selected page info
     */
    Pair<JsonArray, PageInfo> buildPageRecords(String periodType, long startTime, long currentTime, Location location, String componentType,
                                               PageInfo pageInfo, int recordCount, int index, IRepresentationBuilder builder);
}
