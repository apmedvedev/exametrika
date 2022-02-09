/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.services;

import com.exametrika.api.aggregator.IPeriod;
import com.exametrika.api.aggregator.IPeriodCycle;
import com.exametrika.api.aggregator.IPeriodSpace;
import com.exametrika.api.aggregator.Location;
import com.exametrika.api.aggregator.fields.IAggregationField;
import com.exametrika.api.aggregator.fields.IAggregationRecord;
import com.exametrika.api.aggregator.fields.ILogAggregationField;
import com.exametrika.api.aggregator.fields.ILogAggregationField.IAggregationIterable;
import com.exametrika.api.aggregator.fields.ILogAggregationField.IAggregationIterator;
import com.exametrika.api.aggregator.nodes.IAggregationNode;
import com.exametrika.api.aggregator.schema.IAggregationNodeSchema;
import com.exametrika.api.aggregator.schema.ICycleSchema;
import com.exametrika.api.aggregator.schema.IPeriodAggregationFieldSchema;
import com.exametrika.api.aggregator.schema.IPeriodSpaceSchema;
import com.exametrika.api.component.ISelectionService;
import com.exametrika.api.exadb.objectdb.INodeNonUniqueSortedIndex;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonArrayBuilder;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Pair;
import com.exametrika.spi.aggregator.IComponentAccessorFactory;
import com.exametrika.spi.aggregator.IComputeContext;
import com.exametrika.spi.exadb.core.DomainService;


/**
 * The {@link SelectionService} is a selection service implementation.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public final class SelectionService extends DomainService implements ISelectionService {
    private IPeriodSpaceSchema spaceSchema;

    @Override
    public IPeriodSpaceSchema getSpaceSchema() {
        ensureSpaceSchema();
        return spaceSchema;
    }

    @Override
    public IAggregationNode findAggregationNode(String periodType, long time, Location location, String componentType) {
        Assert.notNull(periodType);
        Assert.notNull(location);
        Assert.notNull(componentType);

        ensureSpaceSchema();

        ICycleSchema cycleSchema = spaceSchema.findCycle(periodType);
        Assert.notNull(cycleSchema);

        IAggregationNodeSchema nodeSchema = cycleSchema.findAggregationNode(componentType);
        if (nodeSchema == null)
            return null;

        IPeriodCycle cycle = cycleSchema.findCycle(time);
        if (cycle == null)
            return null;

        IPeriodSpace space = cycle.getSpace();
        if (space == null)
            return null;
        IPeriod period = space.findPeriod(time);
        if (period == null)
            return null;
        if (time - period.getEndTime() > 10000)
            return null;

        return period.findNode(location, nodeSchema);
    }

    @Override
    public IAggregationNode findNearestAggregationNode(String periodType, long time, Location location, String componentType) {
        ISelectionIterator it = getReverseAggregationRecords(periodType, time, location, componentType, true).iterator();
        if (it.hasNext()) {
            it.next();
            return findAggregationNode(periodType, it.get().getTime() - 1000, location, componentType);
        } else
            return findAggregationNode(periodType, time, location, componentType);
    }

    @Override
    public Object findRepresentation(String periodType, long time, Location location, String componentType, int index) {
        Assert.notNull(periodType);
        Assert.notNull(location);
        Assert.notNull(componentType);

        ensureSpaceSchema();

        ICycleSchema cycleSchema = spaceSchema.findCycle(periodType);
        if (cycleSchema.getConfiguration().isNonAggregating()) {
            ISelectionIterator it = getAggregationRecords(periodType, time, location, componentType).iterator();
            if (it.hasNext()) {
                it.next();
                return it.getRepresentation(index, false, false);
            } else
                return null;
        } else {
            IAggregationNode node = findAggregationNode(periodType, time, location, componentType);
            if (node != null)
                return node.getAggregationField().getRepresentation(index, false, false);
            else
                return null;
        }
    }

    @Override
    public Object buildRepresentation(String periodType, long time, Location location, String componentType, int index,
                                      IRepresentationBuilder builder) {
        Assert.notNull(periodType);
        Assert.notNull(location);
        Assert.notNull(componentType);
        Assert.notNull(builder);

        ensureSpaceSchema();

        ICycleSchema cycleSchema = spaceSchema.findCycle(periodType);
        if (cycleSchema.getConfiguration().isNonAggregating()) {
            ISelectionIterator it = getAggregationRecords(periodType, time, location, componentType).iterator();
            if (it.hasNext()) {
                it.next();
                return builder.build(it.get().getTime(), (IAggregationNode) it.getField().getNode().getObject(), it.get().getValue(),
                        it.getField().getSchema().getRepresentations().get(index).getAccessorFactory(), it.getComputeContext());
            } else
                return null;
        } else {
            IAggregationNode node = findAggregationNode(periodType, time, location, componentType);
            if (node != null) {
                IAggregationField field = node.getAggregationField();
                IComputeContext computeContext = field.getComputeContext();
                return builder.build(computeContext.getTime(), node, field.getValue(false),
                        field.getSchema().getRepresentations().get(index).getAccessorFactory(), computeContext);
            } else
                return null;
        }
    }

    @Override
    public ISelectionIterable getAggregationRecords(String periodType, long time, Location location, String componentType) {
        return getReverseAggregationRecords(periodType, time, location, componentType, false);
    }

    @Override
    public Pair<JsonArray, PageInfo> buildPageRecords(String periodType, long startTime, long currentTime, Location location,
                                                      String componentType, PageInfo pageInfo, int recordCount, int index, IRepresentationBuilder builder) {
        PageDirection direction = PageDirection.FIRST;
        if (pageInfo != null) {
            if (!pageInfo.periodType.equals(periodType) || pageInfo.startTime != startTime || pageInfo.currentTime != currentTime)
                pageInfo = null;
            else
                direction = pageInfo.direction;
        }

        ISelectionIterator it;
        int pageIndex;
        switch (direction) {
            case FIRST:
                it = getReverseAggregationRecords(periodType, currentTime, location, componentType, false).iterator();
                pageIndex = 1;
                break;
            case PREVIOUS: {
                ISelectionIterable iterable = getPageRecords(periodType, location, componentType, pageInfo.firstCycleId, pageInfo.firstRecordId, false);
                if (iterable == null)
                    return buildPageRecords(periodType, startTime, currentTime, location, componentType, null, recordCount, index, builder);
                else {
                    it = iterable.iterator();
                    if (it.hasNext())
                        it.next();
                }
                pageIndex = pageInfo.pageIndex - 1;
                break;
            }
            case NEXT: {
                ISelectionIterable iterable = getPageRecords(periodType, location, componentType, pageInfo.lastCycleId, pageInfo.lastRecordId, true);
                if (iterable == null)
                    return buildPageRecords(periodType, startTime, currentTime, location, componentType, null, recordCount, index, builder);
                else {
                    it = iterable.iterator();
                    if (it.hasNext())
                        it.next();
                }
                pageIndex = pageInfo.pageIndex + 1;
                break;
            }
            case LAST:
                it = getDirectAggregationRecords(periodType, startTime, location, componentType).iterator();
                pageIndex = -1;
                break;
            default:
                return Assert.error();
        }

        JsonArrayBuilder rows = new JsonArrayBuilder();
        IComponentAccessorFactory accessorFactory = null;
        boolean firstPage = direction == PageDirection.FIRST, lastPage = direction == PageDirection.LAST;

        String firstCycleId = null, lastCycleId = null;
        long firstRecordId = 0, lastRecordId = 0;

        int i = 0;
        boolean first = true;
        while (i < recordCount) {
            if (!it.hasNext()) {
                if (direction == PageDirection.LAST || direction == PageDirection.PREVIOUS)
                    firstPage = true;
                else
                    lastPage = true;

                break;
            }

            IAggregationRecord record = it.next();
            if (direction == PageDirection.LAST || direction == PageDirection.PREVIOUS) {
                if (record.getTime() > currentTime) {
                    firstPage = true;
                    break;
                }

                if (first) {
                    first = false;
                    lastCycleId = ((SelectionIterator) it).getCycle().getId();
                    lastRecordId = ((SelectionIterator) it).getRecordId();
                }
            } else {
                if (record.getTime() < startTime) {
                    lastPage = true;
                    break;
                }

                if (first) {
                    first = false;
                    firstCycleId = ((SelectionIterator) it).getCycle().getId();
                    firstRecordId = ((SelectionIterator) it).getRecordId();
                }
            }

            if (accessorFactory == null)
                accessorFactory = it.getField().getSchema().getRepresentations().get(0).getAccessorFactory();

            IComputeContext computeContext = it.getComputeContext();
            Object representation = builder.build(record.getTime(), (IAggregationNode) it.getField().getNode().getObject(),
                    record.getValue(), accessorFactory, computeContext);
            if (representation != null)
                rows.add(representation);

            i++;
        }

        if (direction == PageDirection.LAST || direction == PageDirection.PREVIOUS) {
            java.util.Collections.reverse(rows);

            if (!first) {
                firstCycleId = ((SelectionIterator) it).getCycle().getId();
                firstRecordId = ((SelectionIterator) it).getRecordId();
            }
        } else if (!first) {
            lastCycleId = ((SelectionIterator) it).getCycle().getId();
            lastRecordId = ((SelectionIterator) it).getRecordId();
        }

        if (pageIndex == 1)
            firstPage = true;
        if (pageIndex == -1 || (direction == PageDirection.FIRST && firstPage && rows.size() < recordCount))
            lastPage = true;

        PageInfo newPageInfo = null;
        if (!first)
            newPageInfo = new PageInfo(periodType, startTime, currentTime, firstCycleId, firstRecordId, lastCycleId,
                    lastRecordId, direction, firstPage, lastPage, pageIndex);
        return new Pair<JsonArray, PageInfo>(rows.toJson(), newPageInfo);
    }

    @Override
    public void clearCaches() {
        spaceSchema = null;
    }

    private ISelectionIterable getReverseAggregationRecords(String periodType, long time, Location location, String componentType, boolean greater) {
        Assert.notNull(periodType);
        Assert.notNull(location);
        Assert.notNull(componentType);

        ensureSpaceSchema();

        ICycleSchema cycleSchema = spaceSchema.findCycle(periodType);
        Assert.notNull(cycleSchema);
        IAggregationNodeSchema nodeSchema = cycleSchema.findAggregationNode(componentType);
        if (nodeSchema == null)
            return new ReverseSelectionIterable();

        if (!cycleSchema.getConfiguration().isNonAggregating()) {
            IFieldSchema aggregationLog = ((IPeriodAggregationFieldSchema) nodeSchema.getAggregationField()).getAggregationLog();
            if (aggregationLog == null)
                return new ReverseSelectionIterable();
            nodeSchema = (IAggregationNodeSchema) aggregationLog.getParent();
        }

        IPeriodCycle cycle = cycleSchema.findCycle(time);
        if (cycle == null)
            return new ReverseSelectionIterable();

        IPeriodSpace space = cycle.getSpace();
        if (space == null)
            return new ReverseSelectionIterable(cycle, location, nodeSchema, null);

        IAggregationNode node = space.getCyclePeriod().findNode(location, nodeSchema);
        if (node == null)
            return new ReverseSelectionIterable(cycle, location, nodeSchema, null);

        ILogAggregationField aggregationField = (ILogAggregationField) node.getAggregationField();
        INodeNonUniqueSortedIndex<Long, Pair<Long, IAggregationRecord>> index =
                (INodeNonUniqueSortedIndex<Long, Pair<Long, IAggregationRecord>>) aggregationField.getIndex(0);

        Pair<Long, IAggregationRecord> pair;
        if (greater)
            pair = index.findCeilingValue(time, true);
        else
            pair = index.findFloorValue(time, true);

        if (pair == null)
            return new ReverseSelectionIterable(cycle, location, nodeSchema, null);

        return new ReverseSelectionIterable(cycle, location, nodeSchema, aggregationField.getReverseRecords(pair.getKey(), 0));
    }

    private ISelectionIterable getDirectAggregationRecords(String periodType, long time, Location location, String componentType) {
        Assert.notNull(periodType);
        Assert.notNull(location);
        Assert.notNull(componentType);

        ensureSpaceSchema();

        ICycleSchema cycleSchema = spaceSchema.findCycle(periodType);
        Assert.notNull(cycleSchema);
        IAggregationNodeSchema nodeSchema = cycleSchema.findAggregationNode(componentType);
        if (nodeSchema == null)
            return new DirectSelectionIterable();

        if (!cycleSchema.getConfiguration().isNonAggregating()) {
            IFieldSchema aggregationLog = ((IPeriodAggregationFieldSchema) nodeSchema.getAggregationField()).getAggregationLog();
            if (aggregationLog == null)
                return new DirectSelectionIterable();
            nodeSchema = (IAggregationNodeSchema) aggregationLog.getParent();
        }

        IPeriodCycle cycle = cycleSchema.findCycle(time);
        if (cycle == null)
            return new DirectSelectionIterable();

        IPeriodSpace space = cycle.getSpace();
        if (space == null)
            return new DirectSelectionIterable(cycle, location, nodeSchema, null);

        IAggregationNode node = space.getCyclePeriod().findNode(location, nodeSchema);
        if (node == null)
            return new DirectSelectionIterable(cycle, location, nodeSchema, null);

        ILogAggregationField aggregationField = (ILogAggregationField) node.getAggregationField();
        INodeNonUniqueSortedIndex<Long, Pair<Long, IAggregationRecord>> index =
                (INodeNonUniqueSortedIndex<Long, Pair<Long, IAggregationRecord>>) aggregationField.getIndex(0);

        Pair<Long, IAggregationRecord> pair = index.findCeilingValue(time, true);
        if (pair == null)
            return new DirectSelectionIterable(cycle, location, nodeSchema, null);

        return new DirectSelectionIterable(cycle, location, nodeSchema, aggregationField.getRecords(pair.getKey(), 0));
    }

    private ISelectionIterable getPageRecords(String periodType, Location location, String componentType, String cycleId,
                                              long recordId, boolean reverse) {
        Assert.notNull(periodType);
        Assert.notNull(location);
        Assert.notNull(componentType);

        ensureSpaceSchema();

        ICycleSchema cycleSchema = spaceSchema.findCycle(periodType);
        Assert.notNull(cycleSchema);
        IAggregationNodeSchema nodeSchema = cycleSchema.findAggregationNode(componentType);
        if (nodeSchema == null)
            return null;

        if (!cycleSchema.getConfiguration().isNonAggregating()) {
            IFieldSchema aggregationLog = ((IPeriodAggregationFieldSchema) nodeSchema.getAggregationField()).getAggregationLog();
            if (aggregationLog == null)
                return null;
            nodeSchema = (IAggregationNodeSchema) aggregationLog.getParent();
        }

        IPeriodCycle cycle = cycleSchema.findCycleById(cycleId);
        if (cycle == null)
            return null;

        IPeriodSpace space = cycle.getSpace();
        if (space == null)
            return null;

        IAggregationNode node = space.getCyclePeriod().findNode(location, nodeSchema);
        if (node == null)
            return null;

        ILogAggregationField aggregationField = (ILogAggregationField) node.getAggregationField();

        if (reverse)
            return new ReverseSelectionIterable(cycle, location, nodeSchema, aggregationField.getReverseRecords(recordId, 0));
        else
            return new DirectSelectionIterable(cycle, location, nodeSchema, aggregationField.getRecords(recordId, 0));
    }

    private void ensureSpaceSchema() {
        if (spaceSchema == null) {
            spaceSchema = schema.getParent().getParent().findDomain("aggregation").findSpace("aggregation");
            Assert.notNull(spaceSchema);
        }
    }

    private abstract class SelectionIterator implements ISelectionIterator {
        protected final Location location;
        protected final IAggregationNodeSchema nodeSchema;
        protected IPeriodCycle cycle;
        protected IPeriodCycle currentCycle;
        protected IAggregationIterator iterator;
        protected IAggregationIterator currentIterator;

        public SelectionIterator() {
            cycle = null;
            currentCycle = null;
            location = null;
            nodeSchema = null;
            iterator = null;
            currentIterator = null;
        }

        public SelectionIterator(IPeriodCycle cycle, Location location, IAggregationNodeSchema nodeSchema,
                                 IAggregationIterator iterator) {
            this.cycle = cycle;
            this.currentCycle = cycle;
            this.location = location;
            this.nodeSchema = nodeSchema;
            this.iterator = iterator;
            this.currentIterator = iterator;

            findNext();
        }

        public IPeriodCycle getCycle() {
            return currentCycle;
        }

        public long getRecordId() {
            return currentIterator.getId();
        }

        @Override
        public ILogAggregationField getField() {
            if (currentIterator != null)
                return currentIterator.getField();
            else
                return null;
        }

        @Override
        public IAggregationRecord get() {
            if (currentIterator != null)
                return currentIterator.get();
            else
                return null;
        }

        @Override
        public IComputeContext getComputeContext() {
            if (currentIterator != null)
                return currentIterator.getComputeContext();
            else
                return null;
        }

        @Override
        public Object getRepresentation(int index, boolean includeTime, boolean includeMetadata) {
            if (currentIterator != null)
                return currentIterator.getRepresentation(index, includeTime, includeMetadata);
            else
                return null;
        }

        @Override
        public boolean hasNext() {
            if (iterator != null)
                return iterator.hasNext();
            else
                return false;
        }

        @Override
        public IAggregationRecord next() {
            Assert.checkState(iterator != null);

            IAggregationRecord record = iterator.next();
            currentIterator = iterator;
            currentCycle = cycle;
            findNext();
            return record;
        }

        protected abstract void findNext();
    }

    private class ReverseSelectionIterable implements ISelectionIterable {
        private final IPeriodCycle cycle;
        private final Location location;
        private final IAggregationNodeSchema nodeSchema;
        private final IAggregationIterable iterable;

        public ReverseSelectionIterable() {
            cycle = null;
            location = null;
            nodeSchema = null;
            iterable = null;
        }

        public ReverseSelectionIterable(IPeriodCycle cycle, Location location, IAggregationNodeSchema nodeSchema,
                                        IAggregationIterable iterable) {
            Assert.notNull(cycle);
            Assert.notNull(location);
            Assert.notNull(nodeSchema);

            this.cycle = cycle;
            this.location = location;
            this.nodeSchema = nodeSchema;
            this.iterable = iterable;
        }

        @Override
        public ISelectionIterator iterator() {
            if (cycle != null)
                return new ReverseSelectionIterator(cycle, location, nodeSchema, iterable != null ? iterable.iterator() : null);
            else
                return new ReverseSelectionIterator();
        }
    }

    private class ReverseSelectionIterator extends SelectionIterator {
        public ReverseSelectionIterator() {
        }

        public ReverseSelectionIterator(IPeriodCycle cycle, Location location, IAggregationNodeSchema nodeSchema,
                                        IAggregationIterator iterator) {
            super(cycle, location, nodeSchema, iterator);
        }

        @Override
        protected void findNext() {
            if (iterator != null && iterator.hasNext())
                return;

            iterator = null;

            while (true) {
                cycle = cycle.getPreviousCycle();
                if (cycle == null)
                    break;

                IPeriodSpace space = cycle.getSpace();
                if (space == null)
                    continue;
                IAggregationNode node = space.getCyclePeriod().findNode(location, nodeSchema);
                if (node == null)
                    continue;

                ILogAggregationField aggregationField = (ILogAggregationField) node.getAggregationField();
                IAggregationIterator iterator = aggregationField.getReverseRecords().iterator();
                if (iterator.hasNext()) {
                    this.iterator = iterator;
                    break;
                }
            }
        }
    }

    private class DirectSelectionIterable implements ISelectionIterable {
        private final IPeriodCycle cycle;
        private final Location location;
        private final IAggregationNodeSchema nodeSchema;
        private final IAggregationIterable iterable;

        public DirectSelectionIterable() {
            cycle = null;
            location = null;
            nodeSchema = null;
            iterable = null;
        }

        public DirectSelectionIterable(IPeriodCycle cycle, Location location, IAggregationNodeSchema nodeSchema,
                                       IAggregationIterable iterable) {
            Assert.notNull(cycle);
            Assert.notNull(location);
            Assert.notNull(nodeSchema);

            this.cycle = cycle;
            this.location = location;
            this.nodeSchema = nodeSchema;
            this.iterable = iterable;
        }

        @Override
        public ISelectionIterator iterator() {
            if (cycle != null)
                return new DirectSelectionIterator(cycle, location, nodeSchema, iterable != null ? iterable.iterator() : null);
            else
                return new DirectSelectionIterator();
        }
    }

    private class DirectSelectionIterator extends SelectionIterator {
        public DirectSelectionIterator() {
        }

        public DirectSelectionIterator(IPeriodCycle cycle, Location location, IAggregationNodeSchema nodeSchema,
                                       IAggregationIterator iterator) {
            super(cycle, location, nodeSchema, iterator);
        }

        @Override
        protected void findNext() {
            if (iterator != null && iterator.hasNext())
                return;

            iterator = null;

            while (true) {
                cycle = findNextCycle(cycle);
                if (cycle == null)
                    break;

                IPeriodSpace space = cycle.getSpace();
                if (space == null)
                    continue;
                IAggregationNode node = space.getCyclePeriod().findNode(location, nodeSchema);
                if (node == null)
                    continue;

                ILogAggregationField aggregationField = (ILogAggregationField) node.getAggregationField();
                IAggregationIterator iterator = aggregationField.getRecords().iterator();
                if (iterator.hasNext()) {
                    this.iterator = iterator;
                    break;
                }
            }
        }

        private IPeriodCycle findNextCycle(IPeriodCycle baseCycle) {
            for (IPeriodCycle cycle : cycle.getSchema().getCycles()) {
                if (cycle.getPreviousCycle() == baseCycle)
                    return cycle;
            }

            return null;
        }
    }
}
