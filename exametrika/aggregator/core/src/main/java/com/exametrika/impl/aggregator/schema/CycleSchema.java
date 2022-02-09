/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.schema;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.exametrika.api.aggregator.IPeriodCycle;
import com.exametrika.api.aggregator.IPeriodNameManager;
import com.exametrika.api.aggregator.config.PeriodDatabaseExtensionConfiguration;
import com.exametrika.api.aggregator.config.schema.PeriodSchemaConfiguration;
import com.exametrika.api.aggregator.schema.IAggregationNodeSchema;
import com.exametrika.api.aggregator.schema.ICycleSchema;
import com.exametrika.api.aggregator.schema.IPeriodSpaceSchema;
import com.exametrika.api.exadb.core.IBatchControl;
import com.exametrika.api.exadb.core.IDumpContext;
import com.exametrika.api.exadb.core.ITransaction;
import com.exametrika.api.exadb.fulltext.config.schema.DocumentSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.config.schema.NumericFieldSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.config.schema.NumericFieldSchemaConfiguration.DataType;
import com.exametrika.api.exadb.fulltext.config.schema.SimpleAnalyzerSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.config.schema.StandardAnalyzerSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.config.schema.StringFieldSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;
import com.exametrika.api.exadb.index.IIndexManager;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.api.exadb.objectdb.schema.INodeSchema;
import com.exametrika.common.rawdb.IRawTransaction;
import com.exametrika.common.rawdb.impl.RawPageDeserialization;
import com.exametrika.common.rawdb.impl.RawPageSerialization;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Enums;
import com.exametrika.common.utils.Times;
import com.exametrika.impl.aggregator.ClosePeriodBatchOperation;
import com.exametrika.impl.aggregator.Period;
import com.exametrika.impl.aggregator.PeriodCycle;
import com.exametrika.impl.aggregator.PeriodDatabaseExtension;
import com.exametrika.impl.aggregator.PeriodSpace;
import com.exametrika.impl.aggregator.cache.PeriodNodeCacheManager;
import com.exametrika.impl.aggregator.cache.PeriodNodeManager;
import com.exametrika.impl.aggregator.forecast.AnomalyDetectorSpace;
import com.exametrika.impl.aggregator.forecast.ForecasterSpace;
import com.exametrika.impl.aggregator.forecast.IBehaviorTypeIdAllocator;
import com.exametrika.impl.aggregator.name.PeriodNameManager;
import com.exametrika.impl.exadb.core.Constants;
import com.exametrika.impl.exadb.jobs.schedule.StandardSchedulePeriod;
import com.exametrika.impl.exadb.objectdb.schema.NodeSpaceSchema;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.spi.exadb.fulltext.config.schema.FieldSchemaConfiguration.Option;
import com.exametrika.spi.exadb.objectdb.config.schema.NodeSchemaConfiguration;

/**
 * The {@link CycleSchema} represents a schema of cycle of periods for specific node space.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class CycleSchema extends NodeSpaceSchema implements ICycleSchema {
    public static final String PERIOD_FIELD_NAME = "exaPeriod";
    public static final int HEADER_SIZE = 8;// cycleFileOffset(long)
    private static final int PERIOD_CLOSE_DELAY = 10000;
    private static final int START_STATE = 0;
    private static final int ADD_CYCLE_STATE = 1;
    private static final int ADD_CYCLE_PERIOD_STATE = 2;
    private static final int ADD_PERIOD_STATE = 3;
    private final int index;
    private final PeriodDatabaseExtension extension;
    private final INodeSchema cyclePeriodRootNode;
    private final StandardSchedulePeriod period;
    private long dataFileOffset;
    private PeriodCycle currentCycle;
    private final Map<String, IAggregationNodeSchema> aggregationNodes;
    private long lastUpdateTime = Times.getCurrentTime();

    public CycleSchema(PeriodSchemaConfiguration configuration, int index, IDatabaseContext context, int version) {
        super(context, configuration, version, ICycleSchema.TYPE);

        Assert.notNull(configuration);
        Assert.notNull(context);

        if (configuration.getCyclePeriodRootNodeType() != null)
            cyclePeriodRootNode = findNode(configuration.getCyclePeriodRootNodeType());
        else
            cyclePeriodRootNode = null;

        Map<String, IAggregationNodeSchema> aggregationNodes = new LinkedHashMap<String, IAggregationNodeSchema>();
        int locationTotalIndex = 0;
        for (INodeSchema node : getNodes()) {
            if (node instanceof IAggregationNodeSchema) {
                IAggregationNodeSchema aggregationNode = (IAggregationNodeSchema) node;
                if (!aggregationNode.getConfiguration().isDerived())
                    aggregationNodes.put(aggregationNode.getConfiguration().getComponentType().getName(), aggregationNode);
            }

            for (IFieldSchema field : node.getFields()) {
                if (field instanceof LocationFieldSchema)
                    ((LocationFieldSchema) field).setLocationTotalIndex(locationTotalIndex++);
            }
        }

        this.index = index;
        this.extension = context.findExtension(PeriodDatabaseExtensionConfiguration.NAME);
        Assert.notNull(this.extension);
        this.period = (StandardSchedulePeriod) configuration.getPeriod().createPeriod();
        this.aggregationNodes = aggregationNodes;
    }

    public PeriodNameManager getNameManager() {
        return extension.getNameManager();
    }

    public PeriodNodeManager getNodeManager() {
        return extension.getNodeManager();
    }

    public PeriodNodeCacheManager getNodeCacheManager() {
        return extension.getNodeCacheManager();
    }

    public void setDataFileOffset(long offset) {
        Assert.isTrue(offset != 0);
        Assert.checkState(dataFileOffset == 0);

        dataFileOffset = offset;
    }

    public PeriodSpace addCycle(ClosePeriodBatchOperation batch, IBatchControl batchControl) {
        getCurrentCycle();

        long prevCycleFileOffset = 0;

        IRawTransaction transaction = context.getTransactionProvider().getRawTransaction();

        ForecasterSpace forecasterSpace = null;
        AnomalyDetectorSpace anomalyDetectorSpace = null;
        AnomalyDetectorSpace fastAnomalyDetectorSpace = null;
        if (currentCycle != null) {
            forecasterSpace = currentCycle.getSpace().getForecasterSpace();
            anomalyDetectorSpace = currentCycle.getSpace().getAnomalyDetectorSpace();
            fastAnomalyDetectorSpace = currentCycle.getSpace().getFastAnomalyDetectorSpace();

            if (!currentCycle.close(batch, batchControl, false))
                return null;

            prevCycleFileOffset = currentCycle.getFileOffset();
        }

        long cycleFileOffset = context.getSchemaSpace().allocate(transaction, PeriodCycle.HEADER_SIZE +
                (getConfiguration().getTotalIndexCount() + getConfiguration().getTotalBlobIndexCount() +
                        (getConfiguration().hasFullTextIndex() ? 1 : 0)) * 4);
        writeCycleFileOffset(transaction, cycleFileOffset);

        int dataFileIndex = context.getSchemaSpace().allocateFile(transaction);
        int cycleSpaceFileIndex = context.getSchemaSpace().allocateFile(transaction);

        currentCycle = PeriodCycle.create(this, cycleFileOffset, prevCycleFileOffset, currentCycle, Times.getCurrentTime(),
                dataFileIndex, cycleSpaceFileIndex);

        IBehaviorTypeIdAllocator typeIdAllocator = context.getTransactionProvider().getTransaction().findExtension(IPeriodNameManager.NAME);
        PeriodSpace space = PeriodSpace.create(context, dataFileIndex, cycleSpaceFileIndex, this, currentCycle,
                getConfiguration().getCyclePeriodCount(), extension.getNodeManager(), extension.getNodeCacheManager(),
                cycleFileOffset + PeriodCycle.HEADER_SIZE, forecasterSpace, anomalyDetectorSpace, fastAnomalyDetectorSpace,
                typeIdAllocator);
        currentCycle.setSpace(space);

        return space;
    }

    public void bindCycle(CycleSchema oldSchema) {
        Assert.checkState(currentCycle == null);

        IRawTransaction rawTransaction = context.getTransactionProvider().getRawTransaction();
        ITransaction transaction = context.getTransactionProvider().getTransaction();

        long cycleFileOffset = oldSchema.readCycleFileOffset(rawTransaction);
        if (getConfiguration().getTotalIndexCount() > oldSchema.getConfiguration().getTotalIndexCount() ||
                getConfiguration().getTotalBlobIndexCount() > oldSchema.getConfiguration().getTotalBlobIndexCount()) {
            long newCycleFileOffset = context.getSchemaSpace().allocate(rawTransaction, PeriodCycle.HEADER_SIZE +
                    (getConfiguration().getTotalIndexCount() + getConfiguration().getTotalBlobIndexCount() +
                            (getConfiguration().hasFullTextIndex() ? 1 : 0)) * 4);
            int dataFileIndex = PeriodCycle.copyHeader(rawTransaction, cycleFileOffset, newCycleFileOffset);

            IIndexManager indexManager = transaction.findExtension(IIndexManager.NAME);
            List<Integer> newIndexIds = new ArrayList<Integer>();
            List<Integer> newBlobIndexIds = new ArrayList<Integer>();
            PeriodSpace.createNewIndexes(indexManager, dataFileIndex, this,
                    oldSchema.getConfiguration().getTotalIndexCount(), oldSchema.getConfiguration().getTotalBlobIndexCount(),
                    newIndexIds, newBlobIndexIds);

            RawPageSerialization oldSerialization = new RawPageSerialization(rawTransaction,
                    0, Constants.pageIndexByFileOffset(cycleFileOffset + PeriodCycle.HEADER_SIZE), Constants.pageOffsetByFileOffset(cycleFileOffset + PeriodCycle.HEADER_SIZE));
            RawPageSerialization newSerialization = new RawPageSerialization(rawTransaction,
                    0, Constants.pageIndexByFileOffset(newCycleFileOffset + PeriodCycle.HEADER_SIZE), Constants.pageOffsetByFileOffset(newCycleFileOffset + PeriodCycle.HEADER_SIZE));

            if (configuration.hasFullTextIndex())
                newSerialization.writeInt(oldSerialization.readInt());

            int k = 0;
            for (int i = 0; i < configuration.getTotalIndexCount(); i++) {
                if (i < oldSchema.getConfiguration().getTotalIndexCount()) {
                    int indexFileIndex = oldSerialization.readInt();
                    newSerialization.writeInt(indexFileIndex);
                } else {
                    newSerialization.writeInt(newIndexIds.get(k));
                    k++;
                }
            }

            k = 0;
            for (int i = 0; i < configuration.getTotalBlobIndexCount(); i++) {
                if (i < oldSchema.getConfiguration().getTotalBlobIndexCount()) {
                    int indexFileIndex = oldSerialization.readInt();
                    newSerialization.writeInt(indexFileIndex);
                } else {
                    newSerialization.writeInt(newBlobIndexIds.get(k));
                    k++;
                }
            }

            cycleFileOffset = newCycleFileOffset;
        }

        writeCycleFileOffset(rawTransaction, cycleFileOffset);
        oldSchema.writeCycleFileOffset(rawTransaction, 0);

        currentCycle = oldSchema.currentCycle;
    }

    public void addPeriod() {
        int cyclePeriodCount = getConfiguration().getCyclePeriodCount();
        PeriodCycle currentCycle = getCurrentCycle();

        PeriodSpace currentSpace = null;
        if (currentCycle != null)
            currentSpace = currentCycle.getSpace();

        if (currentSpace == null || currentSpace.getPeriodsCount() == cyclePeriodCount) {
            currentSpace = addCycle(null, null);
            if (currentSpace == null)
                return;
        }

        currentSpace.addPeriod(null, null);
    }

    public boolean isPeriodReadyToClose(long currentTime) {
        if (getConfiguration().isNonAggregating())
            return false;

        PeriodCycle cycle = getCurrentCycle();
        if (cycle != null) {
            PeriodSpace space = cycle.getSpace();
            Period currentPeriod = space.getCurrentPeriod();
            return period.evaluate(currentPeriod.getStartTime(), currentTime + PERIOD_CLOSE_DELAY);
        }

        return false;
    }

    public int reconcileCurrentPeriod(ClosePeriodBatchOperation batch, IBatchControl batchControl, long currentTime) {
        if (getConfiguration().isNonAggregating())
            return 0;

        int state = batch != null ? batch.getCycleSchemaState() : 0;
        if (state == START_STATE || state == ADD_PERIOD_STATE) {
            PeriodCycle cycle = getCurrentCycle();
            if (cycle != null) {
                PeriodSpace space = cycle.getSpace();
                Period currentPeriod = space.getCurrentPeriod();
                if (state == START_STATE) {
                    if (!period.evaluate(currentPeriod.getStartTime(), currentTime))
                        return 0;

                    if (space.getPeriodsCount() < getConfiguration().getCyclePeriodCount()) {
                        state = ADD_PERIOD_STATE;
                        if (batch != null)
                            batch.setCycleSchemaState(state);
                    }
                }

                if (state == ADD_PERIOD_STATE) {
                    if (!space.addPeriod(batch, batchControl))
                        return 1;

                    if (batch != null)
                        batch.setCycleSchemaState(START_STATE);

                    return 2;
                }
            }
        }

        if (state <= ADD_CYCLE_STATE) {
            if (batch != null)
                batch.setCycleSchemaState(ADD_CYCLE_STATE);

            PeriodSpace space = addCycle(batch, batchControl);
            if (space == null)
                return 1;
        }

        if (state <= ADD_CYCLE_PERIOD_STATE) {
            if (batch != null)
                batch.setCycleSchemaState(ADD_CYCLE_PERIOD_STATE);

            PeriodSpace space = getCurrentCycle().getSpace();
            if (!space.addPeriod(batch, batchControl))
                return 1;
        }

        if (batch != null)
            batch.setCycleSchemaState(START_STATE);

        return 2;
    }

    public void updateNonAggregatingPeriod(long currentTime) {
        Assert.checkState(getConfiguration().isNonAggregating());

        PeriodCycle cycle = getCurrentCycle();
        if (cycle != null) {
            PeriodSpace space = cycle.getSpace();
            Period currentPeriod = space.getCurrentPeriod();
            if (period.evaluate(lastUpdateTime, currentTime)) {
                currentPeriod.updateNonAggregatingPeriod();
                lastUpdateTime = currentTime;
            }
        }
    }

    public void onTransactionStarted() {
        if (currentCycle != null)
            currentCycle.onTransactionStarted();
    }

    public void onTransactionCommitted() {
        if (currentCycle != null)
            currentCycle.onTransactionCommitted();
    }

    public boolean onBeforeTransactionRolledBack() {
        if (currentCycle != null)
            return currentCycle.onBeforeTransactionRolledBack();
        else
            return false;
    }

    public void onTransactionRolledBack() {
        if (currentCycle != null)
            currentCycle.onTransactionRolledBack();
    }

    public void clearCache() {
        if (currentCycle != null) {
            currentCycle.unload();
            currentCycle = null;
        }
    }

    @Override
    public PeriodSchemaConfiguration getConfiguration() {
        return (PeriodSchemaConfiguration) super.getConfiguration();
    }

    @Override
    public int getIndex() {
        return index;
    }

    @Override
    public IPeriodSpaceSchema getParent() {
        return (IPeriodSpaceSchema) super.getParent();
    }

    @Override
    public PeriodCycle getCurrentCycle() {
        if (currentCycle == null)
            currentCycle = readCurrentCycle();

        IRawTransaction transaction = context.getTransactionProvider().getRawTransaction();
        if (currentCycle != null) {
            if (transaction.isReadOnly() && currentCycle.getSpace().getPeriodsCount() == 0)
                return currentCycle.getPreviousCycle();
            else
                return currentCycle;
        }

        return null;
    }

    @Override
    public CycleIterable getCycles() {
        return new CycleIterable(getCurrentCycle());
    }

    @Override
    public IPeriodCycle findCycle(long time) {
        IPeriodCycle cycle = getCurrentCycle();
        while (cycle != null) {
            if (time >= cycle.getStartTime())
                return cycle;

            cycle = cycle.getPreviousCycle();
        }
        return null;
    }

    @Override
    public IPeriodCycle findCycleById(String cycleId) {
        IPeriodCycle cycle = getCurrentCycle();
        while (cycle != null) {
            if (cycle.getId().equals(cycleId))
                return cycle;

            cycle = cycle.getPreviousCycle();
        }
        return null;
    }

    @Override
    public INodeSchema getCyclePeriodRootNode() {
        return cyclePeriodRootNode;
    }

    @Override
    public IAggregationNodeSchema findAggregationNode(String componentType) {
        Assert.notNull(componentType);

        return aggregationNodes.get(componentType);
    }

    public void dump(File path, IDumpContext context) {
        String cycles;
        if (context.getQuery() != null && context.getQuery().contains("cycles"))
            cycles = context.getQuery().get("cycles");
        else
            cycles = null;

        if (cycles == null || cycles.equals("all")) {
            for (IPeriodCycle periodCycle : getCycles())
                ((PeriodCycle) periodCycle).dump(path, context);
        } else if (cycles.equals("current")) {
            if (getCurrentCycle() != null)
                getCurrentCycle().dump(path, context);
        } else if (cycles.startsWith("last-")) {
            int count = Integer.parseInt(cycles.substring("last-".length()));
            for (IPeriodCycle periodCycle : getCycles()) {
                if (count <= 0)
                    break;
                ((PeriodCycle) periodCycle).dump(path, context);
                count--;
            }
        } else
            Assert.error();
    }

    private long readCycleFileOffset(IRawTransaction transaction) {
        RawPageDeserialization deserialization = new RawPageDeserialization(transaction,
                0, Constants.pageIndexByFileOffset(dataFileOffset), Constants.pageOffsetByFileOffset(dataFileOffset));
        return deserialization.readLong();
    }

    private void writeCycleFileOffset(IRawTransaction transaction, long cycleFileOffset) {
        RawPageSerialization serialization = new RawPageSerialization(transaction,
                0, Constants.pageIndexByFileOffset(dataFileOffset), Constants.pageOffsetByFileOffset(dataFileOffset));
        serialization.writeLong(cycleFileOffset);
    }

    private PeriodCycle readCurrentCycle() {
        IRawTransaction transaction = context.getTransactionProvider().getRawTransaction();
        long cycleFileOffset = readCycleFileOffset(transaction);

        if (cycleFileOffset != 0) {
            currentCycle = PeriodCycle.open(this, cycleFileOffset);

            return currentCycle;
        } else
            return null;
    }

    @Override
    protected IDocumentSchema createDocumentSchema(NodeSchemaConfiguration node, List<IFieldSchema> fields) {
        String documentType = node.getDocumentType();
        if (documentType == null)
            documentType = node.getName();

        List<com.exametrika.spi.exadb.fulltext.config.schema.FieldSchemaConfiguration> documentFields =
                new ArrayList<com.exametrika.spi.exadb.fulltext.config.schema.FieldSchemaConfiguration>();
        documentFields.add(new NumericFieldSchemaConfiguration(NODE_ID_FIELD_NAME, DataType.LONG, true, true));
        documentFields.add(new NumericFieldSchemaConfiguration(PERIOD_FIELD_NAME, DataType.INT, true, true));
        documentFields.add(new StringFieldSchemaConfiguration(DOCUMENT_TYPE_FIELD_NAME, Enums.of(Option.INDEXED,
                Option.INDEX_DOCUMENTS, Option.OMIT_NORMS), new SimpleAnalyzerSchemaConfiguration()));
        for (IFieldSchema field : fields) {
            if (field.getConfiguration().isFullTextIndexed())
                documentFields.add(field.getConfiguration().createFullTextSchemaConfiguration(node.getName()));
        }

        if (!documentFields.isEmpty())
            return new DocumentSchemaConfiguration(documentType, documentFields, new StandardAnalyzerSchemaConfiguration(), 3).createSchema();
        else
            return null;
    }

    private class CycleIterable implements Iterable<IPeriodCycle> {
        private PeriodCycle currentCycle;

        public CycleIterable(PeriodCycle currentCycle) {
            this.currentCycle = currentCycle;
        }

        @Override
        public Iterator<IPeriodCycle> iterator() {
            return new CycleIterator(currentCycle);
        }
    }

    private class CycleIterator implements Iterator<IPeriodCycle> {
        private PeriodCycle currentCycle;

        public CycleIterator(PeriodCycle currentCycle) {
            this.currentCycle = currentCycle;
        }

        @Override
        public boolean hasNext() {
            return currentCycle != null;
        }

        @Override
        public IPeriodCycle next() {
            Assert.notNull(currentCycle);

            PeriodCycle res = currentCycle;
            currentCycle = currentCycle.getPreviousCycle();

            return res;
        }

        @Override
        public void remove() {
            Assert.supports(false);
        }
    }
}
