/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator;

import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exametrika.api.aggregator.IPeriod;
import com.exametrika.api.aggregator.IPeriodSpace;
import com.exametrika.api.aggregator.config.schema.IndexedLocationFieldSchemaConfiguration;
import com.exametrika.api.exadb.core.IBatchControl;
import com.exametrika.api.exadb.core.IDumpContext;
import com.exametrika.api.exadb.fulltext.IDocument;
import com.exametrika.api.exadb.fulltext.IFullTextIndex;
import com.exametrika.api.exadb.fulltext.INumericField;
import com.exametrika.api.exadb.fulltext.config.schema.FullTextIndexSchemaConfiguration;
import com.exametrika.api.exadb.index.IIndexManager;
import com.exametrika.api.exadb.index.ISortedIndex;
import com.exametrika.api.exadb.index.IUniqueIndex;
import com.exametrika.api.exadb.index.config.schema.BTreeIndexSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.ByteArrayKeyNormalizerSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.FixedCompositeKeyNormalizerSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.HashIndexSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.IntValueConverterSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.LongValueConverterSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.NumericKeyNormalizerSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.NumericKeyNormalizerSchemaConfiguration.DataType;
import com.exametrika.api.exadb.index.config.schema.TreeIndexSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.config.schema.StructuredBlobFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.StructuredBlobIndexSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.common.io.IDataDeserialization;
import com.exametrika.common.io.IDataSerialization;
import com.exametrika.common.json.IJsonHandler;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.rawdb.IRawPage;
import com.exametrika.common.rawdb.IRawTransaction;
import com.exametrika.common.rawdb.IRawWriteRegion;
import com.exametrika.common.rawdb.RawBindInfo;
import com.exametrika.common.rawdb.RawDatabaseException;
import com.exametrika.common.rawdb.impl.RawPageDeserialization;
import com.exametrika.common.rawdb.impl.RawPageSerialization;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.MapBuilder;
import com.exametrika.common.utils.Pair;
import com.exametrika.common.utils.Times;
import com.exametrika.impl.aggregator.cache.PeriodNodeCache;
import com.exametrika.impl.aggregator.cache.PeriodNodeCacheManager;
import com.exametrika.impl.aggregator.cache.PeriodNodeManager;
import com.exametrika.impl.aggregator.forecast.AnomalyDetectorSpace;
import com.exametrika.impl.aggregator.forecast.ForecasterSpace;
import com.exametrika.impl.aggregator.forecast.IBehaviorTypeIdAllocator;
import com.exametrika.impl.aggregator.index.PeriodNodeFullTextIndex;
import com.exametrika.impl.aggregator.schema.CycleSchema;
import com.exametrika.impl.exadb.core.Constants;
import com.exametrika.impl.exadb.core.Spaces;
import com.exametrika.impl.exadb.core.ops.DumpContext;
import com.exametrika.impl.exadb.objectdb.NodeSpace;
import com.exametrika.impl.exadb.objectdb.cache.NodeCacheManager;
import com.exametrika.impl.exadb.objectdb.cache.NodeObjectCache;
import com.exametrika.impl.exadb.objectdb.index.NodeIndex;
import com.exametrika.impl.exadb.objectdb.schema.StructuredBlobFieldSchema;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.spi.exadb.fulltext.IFullTextDocumentSpace;
import com.exametrika.spi.exadb.index.config.schema.IndexSchemaConfiguration;
import com.exametrika.spi.exadb.index.config.schema.KeyNormalizerSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.INodeObject;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.NodeSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.fields.IField;


/**
 * The {@link PeriodSpace} is a periodic space.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class PeriodSpace extends NodeSpace implements IPeriodSpace, IFullTextDocumentSpace {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final short MAGIC = 0x1701;
    private static final int HEADER_SIZE = 44;
    private static final int CLOSED_OFFSET = 3;
    private static final int CURRENT_PERIOD_BLOCK_INDEX_OFFSET = 4;
    private static final int PERIODS_COUNT_OFFSET = 16;
    private static final int NEXT_BLOCK_INDEX_OFFSET = 20;
    private NodeIndexInfo[] indexes;
    private final Map<String, Integer> indexesMap = new HashMap<String, Integer>();
    private PeriodNodeFullTextIndex fullTextIndex;
    private BlobIndexInfo[] blobIndexes;
    private IRawPage headerPage;
    private final PeriodNodeManager nodeManager;
    private final PeriodNodeCacheManager nodeCacheManager;
    private final PeriodNodeCache nodeCache;
    private final CycleSchema schema;
    private final PeriodCycle cycle;
    private CyclePeriodSpace cyclePeriodSpace;
    private final IIndexManager indexManager;
    private ISortedIndex<Long, Integer> periodIndex;
    private boolean closed;             // magic(short) + version(byte) + closed(byte) + currentPeriodBlockIndex(long)
    private long currentPeriodBlockIndex;// + periodsCapacity(int) + periodsCount(int) + nextBlockIndex(long) + periodIndexId(int)+
    private int periodsCapacity;// + forecasterSpaceFileIndex(int) + anomalyDetectorSpaceFileIndex(int) + fastAnomalyDetectorSpaceFileIndex(int) 
    private int periodsCount;
    private Period currentPeriod;
    private final TIntObjectMap<Period> periods = new TIntObjectHashMap<Period>();
    private final NodeObjectCache nodeObjectCache = new NodeObjectCache();
    private ForecasterSpace forecasterSpace;
    private AnomalyDetectorSpace anomalyDetectorSpace;
    private AnomalyDetectorSpace fastAnomalyDetectorSpace;
    private boolean periodAdded;

    public static PeriodSpace create(IDatabaseContext context, int dataFileIndex, int cycleSpaceFileIndex, CycleSchema schema,
                                     PeriodCycle cycle, int periodsCapacity, PeriodNodeManager nodeManager,
                                     PeriodNodeCacheManager nodeCacheManager, long indexFileIndexesFileOffset, ForecasterSpace forecasterSpace,
                                     AnomalyDetectorSpace anomalyDetectorSpace, AnomalyDetectorSpace fastAnomalyDetectorSpace, IBehaviorTypeIdAllocator typeIdAllocator) {
        Assert.notNull(context);

        IRawTransaction transaction = context.getTransactionProvider().getRawTransaction();
        String filePrefix = getFilePrefix(schema, dataFileIndex);
        bindFile(schema, transaction, dataFileIndex, filePrefix, schema.getParent().getConfiguration().getPathIndex());

        PeriodSpace space = new PeriodSpace(context, dataFileIndex, schema, cycle, filePrefix, nodeManager,
                nodeCacheManager);

        String forecastPrefix = PeriodSpaces.getForecastSpacePrefix(schema.getParent().getParent().getConfiguration().getName(),
                schema.getParent().getConfiguration(), schema.getConfiguration());
        space.onCreated(context, periodsCapacity, indexFileIndexesFileOffset, forecasterSpace, anomalyDetectorSpace,
                fastAnomalyDetectorSpace, typeIdAllocator, forecastPrefix);

        if (!schema.getConfiguration().isNonAggregating())
            space.cyclePeriodSpace = CyclePeriodSpace.create(context, cycleSpaceFileIndex, filePrefix, schema, space);

        return space;
    }

    public static PeriodSpace open(IDatabaseContext context, int dataFileIndex, int cycleSpaceFileIndex, CycleSchema schema,
                                   PeriodCycle cycle, PeriodNodeManager nodeManager, PeriodNodeCacheManager nodeCacheManager,
                                   long indexFileIndexesFileOffset, IBehaviorTypeIdAllocator typeIdAllocator) {
        Assert.notNull(context);

        IRawTransaction transaction = context.getTransactionProvider().getRawTransaction();
        String filePrefix = getFilePrefix(schema, dataFileIndex);
        bindFile(schema, transaction, dataFileIndex, filePrefix, schema.getParent().getConfiguration().getPathIndex());

        PeriodSpace space = new PeriodSpace(context, dataFileIndex, schema, cycle, filePrefix, nodeManager,
                nodeCacheManager);
        String forecastPrefix = PeriodSpaces.getForecastSpacePrefix(schema.getParent().getParent().getConfiguration().getName(),
                schema.getParent().getConfiguration(), schema.getConfiguration());
        space.onOpened(context, typeIdAllocator, indexFileIndexesFileOffset, forecastPrefix);

        if (!schema.getConfiguration().isNonAggregating())
            space.cyclePeriodSpace = CyclePeriodSpace.open(context, cycleSpaceFileIndex, filePrefix, schema, space);

        return space;
    }

    public int getFileIndex() {
        return fileIndex;
    }

    public String getFilePrefix() {
        return filePrefix;
    }

    public NodeObjectCache getNodeObjectCache() {
        return nodeObjectCache;
    }

    public ForecasterSpace getForecasterSpace() {
        return forecasterSpace;
    }

    public AnomalyDetectorSpace getAnomalyDetectorSpace() {
        return anomalyDetectorSpace;
    }

    public AnomalyDetectorSpace getFastAnomalyDetectorSpace() {
        return fastAnomalyDetectorSpace;
    }

    @Override
    public PeriodNodeManager getNodeManager() {
        return nodeManager;
    }

    @Override
    public NodeCacheManager getNodeCacheManager() {
        return nodeCacheManager;
    }

    @Override
    public PeriodNodeCache getNodeCache() {
        return nodeCache;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    public long allocateBlocks(int blockCount) {
        Assert.checkState(!closed);
        Assert.isTrue(blockCount <= Constants.BLOCKS_PER_PAGE_COUNT);

        IRawWriteRegion region = headerPage.getWriteRegion();
        long nextBlockIndex = region.readInt(NEXT_BLOCK_INDEX_OFFSET);
        Assert.isTrue(nextBlockIndex != 0);
        int pageOffset = Constants.pageOffsetByBlockIndex(nextBlockIndex);
        if (pageOffset + Constants.dataSize(blockCount) > Constants.PAGE_SIZE) {
            IRawPage page = getRawTransaction().getPage(fileIndex, Constants.pageIndexByBlockIndex(nextBlockIndex) + 1);
            nextBlockIndex = Constants.blockIndex(page.getIndex(), 0);
        }
        region.writeLong(NEXT_BLOCK_INDEX_OFFSET, nextBlockIndex + blockCount);

        return nextBlockIndex;
    }

    public void addPeriod() {
        addPeriod(null, null);
    }

    public boolean addPeriod(ClosePeriodBatchOperation batch, IBatchControl batchControl) {
        int periodIndex = periodsCount;
        Assert.isTrue(periodIndex < periodsCapacity);
        Assert.checkState(!closed);

        long startTime;
        if (batch != null)
            startTime = batch.getCurrentTime();
        else
            startTime = Times.getCurrentTime();
        if (currentPeriod != null) {
            if (!currentPeriod.close(batch, batchControl, startTime, false))
                return false;

            if (startTime < currentPeriod.getEndTime())
                startTime = currentPeriod.getEndTime();
        }

        long periodBlockIndex = allocateBlocks(Period.HEADER_BLOCK_COUNT);
        Period period = new Period(this, periodIndex, fileIndex, periodBlockIndex, true, startTime);

        periodsCount = periodIndex + 1;
        currentPeriodBlockIndex = periodBlockIndex;

        IRawWriteRegion region = headerPage.getWriteRegion();
        region.writeInt(PERIODS_COUNT_OFFSET, periodsCount);
        region.writeLong(CURRENT_PERIOD_BLOCK_INDEX_OFFSET, currentPeriodBlockIndex);

        currentPeriod = period;
        period.createRootNode();

        periods.put(periodIndex, period);

        long fileOffset = HEADER_SIZE + periodIndex * 8;
        RawPageSerialization serialization = new RawPageSerialization(transactionProvider.getRawTransaction(), fileIndex, Constants.pageIndexByFileOffset(fileOffset),
                Constants.pageOffsetByFileOffset(fileOffset));
        serialization.writeLong(currentPeriodBlockIndex);

        if (this.periodIndex != null)
            getPeriodIndex().add(period.getStartTime(), periodIndex);

        periodAdded = true;
        return true;
    }

    public boolean close(ClosePeriodBatchOperation batch, IBatchControl batchControl, boolean schemaChange) {
        Assert.checkState(!closed);

        long currentTime;
        if (batch != null)
            currentTime = batch.getCurrentTime();
        else
            currentTime = Times.getCurrentTime();

        if (currentPeriod != null) {
            if (!currentPeriod.close(batch, batchControl, currentTime, schemaChange))
                return false;
        }

        if (cyclePeriodSpace != null)
            cyclePeriodSpace.close(currentTime, schemaChange);

        closed = true;
        forecasterSpace = null;
        anomalyDetectorSpace = null;
        fastAnomalyDetectorSpace = null;
        headerPage.getWriteRegion().writeByte(CLOSED_OFFSET, (byte) 1);
        return true;
    }

    public void onTransactionStarted() {
        periodAdded = false;
        if (forecasterSpace != null) {
            forecasterSpace.onTransactionStarted();
            anomalyDetectorSpace.onTransactionStarted();
            fastAnomalyDetectorSpace.onTransactionStarted();
        }
    }

    public void onTransactionCommitted() {
        periodAdded = false;
        if (forecasterSpace != null) {
            forecasterSpace.onTransactionCommitted();
            anomalyDetectorSpace.onTransactionCommitted();
            fastAnomalyDetectorSpace.onTransactionCommitted();
        }
    }

    public boolean onBeforeTransactionRolledBack() {
        return periodAdded;
    }

    public void onTransactionRolledBack() {
        periodAdded = false;
        if (forecasterSpace != null) {
            forecasterSpace.onTransactionRolledBack();
            anomalyDetectorSpace.onTransactionRolledBack();
            fastAnomalyDetectorSpace.onTransactionRolledBack();
        }
    }

    public void unload() {
        nodeCache.release();

        if (periodIndex != null) {
            periodIndex.unload();
            periodIndex = null;
        }

        if (indexes != null) {
            for (int i = 0; i < indexes.length; i++) {
                if (indexes[i].index != null)
                    indexes[i].index.unload();
            }

            indexes = null;
            indexesMap.clear();
        }

        if (blobIndexes != null) {
            for (int i = 0; i < blobIndexes.length; i++) {
                if (blobIndexes[i].index != null)
                    blobIndexes[i].index.unload();
            }

            blobIndexes = null;
        }

        if (fullTextIndex != null) {
            fullTextIndex.unload();
            fullTextIndex = null;
        }

        nodeObjectCache.clear();
    }

    @Override
    public CycleSchema getSchema() {
        return schema;
    }

    @Override
    public long getStartTime() {
        if (cycle != null)
            return cycle.getStartTime();
        else
            return 0;
    }

    @Override
    public long getEndTime() {
        if (!closed || cycle == null)
            return 0;
        else
            return cycle.getEndTime();
    }

    @Override
    public int getPeriodsCount() {
        if (!schema.getConfiguration().isNonAggregating() && transactionProvider.getRawTransaction().isReadOnly() && !closed)
            return periodsCount - 1;
        else
            return periodsCount;
    }


    @Override
    public Period getCyclePeriod() {
        if (!schema.getConfiguration().isNonAggregating())
            return cyclePeriodSpace.getPeriod();
        else
            return currentPeriod;
    }

    @Override
    public Period getCurrentPeriod() {
        Period currentPeriod = null;
        if (!schema.getConfiguration().isNonAggregating() && transactionProvider.getRawTransaction().isReadOnly() && !closed) {
            if (periodsCount > 1)
                currentPeriod = getPeriod(periodsCount - 2);
        } else
            currentPeriod = this.currentPeriod;

        Assert.checkState(currentPeriod != null);

        return currentPeriod;
    }

    @Override
    public Period getPeriod(int periodIndex) {
        if (periodIndex == CyclePeriod.PERIOD_INDEX)
            return getCyclePeriod();

        Period period = periods.get(periodIndex);
        if (period != null)
            return period;

        return readPeriod(periodIndex);
    }

    @Override
    public IPeriod findPeriod(long time) {
        if (this.periodIndex != null) {
            Integer periodIndex = getPeriodIndex().findFloorValue(time, true);
            if (periodIndex == null)
                return null;
            else {
                int periodsCount = getPeriodsCount();
                if (periodIndex >= periodsCount)
                    periodIndex = periodsCount - 1;

                return getPeriod(periodIndex);
            }
        } else if (currentPeriod.getStartTime() <= time)
            return currentPeriod;
        else
            return null;
    }

    @Override
    public PeriodCycle getPreviousCycle() {
        if (cycle != null)
            return cycle.getPreviousCycle();
        else
            return null;
    }

    @Override
    public PeriodNodeFullTextIndex getFullTextIndex() {
        return fullTextIndex;
    }

    @Override
    public void write(IDataSerialization serialization, IDocument document) {
        if (!(document.getContext() instanceof IFullTextDocumentSpace)) {
            long nodeId = ((INumericField) document.getFields().get(0)).get().longValue();
            int periodIndex = ((INumericField) document.getFields().get(1)).get().intValue();
            serialization.writeBoolean(true);
            serialization.writeLong(nodeId);
            serialization.writeInt(periodIndex);
        } else {
            IFullTextDocumentSpace space = document.getContext();
            IField field = document.getContext();
            serialization.writeBoolean(false);

            PeriodNode node = (PeriodNode) field.getNode();
            serialization.writeLong(node.getId());
            serialization.writeInt(node.getPeriod().getPeriodIndex());
            serialization.writeInt(field.getSchema().getIndex());
            space.write(serialization, document);
        }
    }

    @Override
    public void readAndReindex(IDataDeserialization deserialization) {
        if (deserialization.readBoolean()) {
            long nodeId = deserialization.readLong();
            int periodIndex = deserialization.readInt();
            Period period = getPeriod(periodIndex);
            INode node = ((INodeObject) period.findNodeById(nodeId)).getNode();
            fullTextIndex.update(node);
        } else {
            long nodeId = deserialization.readLong();
            int periodIndex = deserialization.readInt();
            int fieldIndex = deserialization.readInt();

            Period period = getPeriod(periodIndex);
            INode node = ((INodeObject) period.findNodeById(nodeId)).getNode();
            IFullTextDocumentSpace space = node.getField(fieldIndex);
            space.readAndReindex(deserialization);
        }
    }

    public NodeIndexInfo getIndex(int i) {
        NodeIndexInfo info = indexes[i];
        if (info.index == null || info.index.isStale()) {
            IUniqueIndex index = indexManager.getIndex(info.id);
            info.index = index;
        }

        return info;
    }

    public Integer findIndex(String indexName) {
        return indexesMap.get(indexName);
    }

    @Override
    public void addIndexValue(IFieldSchema field, Object key, INode node, boolean updateIndex, boolean updateCache) {
        PeriodNode perodNode = (PeriodNode) node;

        NodeIndex<Object, Long> index = perodNode.getPeriod().getIndex(field);
        index.add(key, node, updateIndex, updateCache);
    }

    @Override
    public void updateIndexValue(IFieldSchema field, Object oldKey, Object newKey, INode node) {
        PeriodNode perodNode = (PeriodNode) node;

        NodeIndex<Object, Long> index = perodNode.getPeriod().getIndex(field);
        index.update(oldKey, newKey, node);
    }

    @Override
    public void removeIndexValue(IFieldSchema field, Object key, INode node, boolean updateIndex, boolean updateCache) {
        if (indexes == null)
            return;

        PeriodNode perodNode = (PeriodNode) node;

        NodeIndex<Object, Long> index = perodNode.getPeriod().getIndex(field);
        index.remove(key, node, updateIndex, updateCache);
    }

    public List<String> beginSnapshot() {
        if (fullTextIndex != null)
            return fullTextIndex.beginSnapshot();
        else
            return Collections.emptyList();
    }

    public void endSnapshot() {
        if (fullTextIndex != null)
            fullTextIndex.endSnapshot();
    }

    @Override
    public Map<String, String> getProperties(NodeSchemaConfiguration configuration) {
        String domainName = schema.getParent().getParent().getConfiguration().getName();
        Map<String, String> properties = new MapBuilder<String, String>()
                .put("spaceName", schema.getConfiguration().getName())
                .put("domainName", domainName)
                .put("periodName", schema.getConfiguration().getName())
                .put("nodeName", configuration.getName())
                .put("name", domainName + "." + schema.getParent().getConfiguration().getName() +
                        "." + schema.getConfiguration().getName() + "." + configuration.getName())
                .toMap();

        return properties;
    }

    public void dump(IJsonHandler json, DumpContext context) {
        String periods;
        if (context.getQuery() != null && context.getQuery().contains("periods"))
            periods = context.getQuery().get("periods");
        else
            periods = null;

        if (periods == null || periods.equals("all")) {
            if (schema.getConfiguration().isNonAggregating())
                dump(getCyclePeriod(), json, context);
            else {
                dump(getCyclePeriod(), json, context);
                for (int i = 0; i < getPeriodsCount(); i++)
                    dump(getPeriod(i), json, context);
            }
        } else if (periods.equals("current"))
            dump(getCurrentPeriod(), json, context);
        else if (periods.equals("cycle"))
            dump(getCyclePeriod(), json, context);
        else if (periods.startsWith("last-")) {
            int count = Integer.parseInt(periods.substring("last-".length()));
            if (schema.getConfiguration().isNonAggregating())
                dump(getCyclePeriod(), json, context);
            else {
                for (int i = getPeriodsCount() - 1; i >= 0; i--) {
                    if (count <= 0)
                        break;
                    dump(getPeriod(i), json, context);
                    count--;
                }
            }
        } else
            Assert.error();
    }

    private void dump(Period period, IJsonHandler json, DumpContext context) {
        if (period == null)
            return;

        String periodName;
        if ((context.getFlags() & IDumpContext.DUMP_TIMES) != 0) {
            DateFormat format = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SS");

            periodName = "[" + format.format(new Date(period.getStartTime())) +
                    (period.getEndTime() != 0 ? (", " + format.format(new Date(period.getEndTime())) + "]") : ", <opened>)");
        } else
            periodName = "";

        if (period instanceof CyclePeriod)
            periodName = "cyclePeriod " + periodName;
        else
            periodName = "period " + period.getPeriodIndex() + periodName;

        json.key(periodName);
        json.startObject();
        period.dump(json, context);
        json.endObject();

        schema.getContext().getCacheControl().unloadExcessive();
    }

    @Override
    public String getFieldId(IField field) {
        PeriodNode node = (PeriodNode) field.getNode();
        return node.getPeriod().getPeriodIndex() + ":" + Long.toString(node.getId()) + ":" + field.getSchema().getIndex();
    }

    @Override
    public IUniqueIndex getBlobIndex(StructuredBlobFieldSchema field, int blobIndex) {
        Assert.notNull(field);
        Assert.isTrue(blobIndex < field.getConfiguration().getIndexes().size());
        Assert.isTrue(field.getParent().getParent() == schema);
        return getBlobIndex(field.getBlobIndexTotalIndex(blobIndex));
    }

    @Override
    public String toString() {
        return Spaces.getSpaceDataFileName(filePrefix, fileIndex);
    }

    private PeriodSpace(IDatabaseContext context, int fileIndex, CycleSchema schema, PeriodCycle cycle,
                        String filePrefix, PeriodNodeManager nodeManager, PeriodNodeCacheManager nodeCacheManager) {
        super(context.getTransactionProvider(), fileIndex, filePrefix);

        Assert.notNull(schema);
        Assert.notNull(cycle);
        Assert.notNull(nodeManager);
        Assert.notNull(nodeCacheManager);

        this.schema = schema;
        this.cycle = cycle;
        this.nodeManager = nodeManager;
        this.nodeCacheManager = nodeCacheManager;

        String domainName = schema.getParent().getParent().getConfiguration().getName();
        Pair<String, String> pair = schema.getContext().getCacheCategorizationStrategy().categorize(new MapBuilder<String, String>()
                .put("type", "nodes.data.period")
                .put("spaceName", schema.getParent().getConfiguration().getName())
                .put("periodName", schema.getConfiguration().getName())
                .put("domainName", domainName)
                .put("name", domainName + "." + schema.getParent().getConfiguration().getName() + "." +
                        schema.getConfiguration().getName())
                .toMap());
        this.nodeCache = nodeCacheManager.getNodeCache(pair.getKey(), pair.getValue());
        this.nodeCache.addRef();
        this.headerPage = transactionProvider.getRawTransaction().getPage(fileIndex, 0);
        this.indexManager = context.findTransactionExtension(IIndexManager.NAME);
    }

    private void onCreated(IDatabaseContext context, int periodsCapacity, long indexFileIndexesFileOffset, ForecasterSpace forecasterSpace,
                           AnomalyDetectorSpace anomalyDetectorSpace, AnomalyDetectorSpace fastAnomalyDetectorSpace, IBehaviorTypeIdAllocator typeIdAllocator,
                           String forecastPrefix) {
        closed = false;
        currentPeriodBlockIndex = 0;
        this.periodsCapacity = periodsCapacity;
        periodsCount = 0;

        int periodIndexId = 0;
        if (!schema.getConfiguration().isNonAggregating()) {
            BTreeIndexSchemaConfiguration periodIndexSchema = createPeriodIndexSchemaConfiguration();
            String filePrefix = indexesPath + File.separator + periodIndexSchema.getType();
            periodIndex = indexManager.createIndex(filePrefix, periodIndexSchema);
            periodIndexId = periodIndex.getId();
        }

        if (forecasterSpace == null) {
            IRawTransaction transaction = context.getTransactionProvider().getRawTransaction();
            this.forecasterSpace = ForecasterSpace.create(context, context.getSchemaSpace().allocateFile(transaction),
                    forecastPrefix, schema, Constants.SMALL_MEDIUM_PAGE_TYPE, typeIdAllocator);
            this.anomalyDetectorSpace = AnomalyDetectorSpace.create(context, context.getSchemaSpace().allocateFile(transaction),
                    forecastPrefix, schema, Constants.SMALL_MEDIUM_PAGE_TYPE, typeIdAllocator);
            this.fastAnomalyDetectorSpace = AnomalyDetectorSpace.create(context, context.getSchemaSpace().allocateFile(transaction),
                    forecastPrefix, schema, Constants.NORMAL_PAGE_TYPE, typeIdAllocator);
        } else {
            Assert.notNull(anomalyDetectorSpace);
            Assert.notNull(fastAnomalyDetectorSpace);

            this.forecasterSpace = forecasterSpace;
            this.anomalyDetectorSpace = anomalyDetectorSpace;
            this.fastAnomalyDetectorSpace = fastAnomalyDetectorSpace;
        }

        long firstPeriodBlockIndex = Constants.blockIndex(Constants.alignBlock(HEADER_SIZE + periodsCapacity * 8));
        writeHeader(periodIndexId, firstPeriodBlockIndex);
        createIndexes(indexFileIndexesFileOffset);
    }

    private void onOpened(IDatabaseContext context, IBehaviorTypeIdAllocator typeIdAllocator, long indexFileIndexesFileOffset,
                          String forecastPrefix) {
        readHeader(context, typeIdAllocator, forecastPrefix);
        openIndexes(indexFileIndexesFileOffset);
    }

    private void readHeader(IDatabaseContext context, IBehaviorTypeIdAllocator typeIdAllocator, String forecastPrefix) {
        IRawTransaction transaction = transactionProvider.getRawTransaction();
        RawPageDeserialization deserialization = new RawPageDeserialization(transaction, fileIndex, headerPage, 0);

        short magic = deserialization.readShort();
        byte version = deserialization.readByte();

        if (magic != MAGIC)
            throw new RawDatabaseException(messages.invalidFormat(deserialization.getFileIndex()));
        if (version != Constants.VERSION)
            throw new RawDatabaseException(messages.unsupportedVersion(deserialization.getFileIndex(), version, Constants.VERSION));

        closed = deserialization.readBoolean();
        currentPeriodBlockIndex = deserialization.readLong();
        periodsCapacity = deserialization.readInt();
        periodsCount = deserialization.readInt();
        deserialization.readLong(); // nextBlockIndex (long) 
        int periodIndexId = deserialization.readInt();
        int forecasterSpaceFileIndex = deserialization.readInt();
        int anomalyDetectorSpaceFileIndex = deserialization.readInt();
        int fastAnomalyDetectorSpaceFileIndex = deserialization.readInt();

        if (periodIndexId > 0)
            periodIndex = indexManager.getIndex(periodIndexId);

        if (periodsCount > 0) {
            currentPeriod = new Period(this, periodsCount - 1, fileIndex, currentPeriodBlockIndex, false, 0);
            periods.put(currentPeriod.getPeriodIndex(), currentPeriod);
        }

        if (!closed) {
            forecasterSpace = ForecasterSpace.open(context, forecasterSpaceFileIndex, forecastPrefix, schema, Constants.SMALL_MEDIUM_PAGE_TYPE,
                    typeIdAllocator);
            anomalyDetectorSpace = AnomalyDetectorSpace.open(context, anomalyDetectorSpaceFileIndex, forecastPrefix, schema,
                    Constants.SMALL_MEDIUM_PAGE_TYPE, typeIdAllocator);
            fastAnomalyDetectorSpace = AnomalyDetectorSpace.open(context, fastAnomalyDetectorSpaceFileIndex, forecastPrefix, schema,
                    Constants.NORMAL_PAGE_TYPE, typeIdAllocator);
        }
    }

    private void writeHeader(int periodIndexId, long nextBlockIndex) {
        RawPageSerialization serialization = new RawPageSerialization(transactionProvider.getRawTransaction(), fileIndex, headerPage, 0);

        serialization.writeShort(MAGIC);
        serialization.writeByte(Constants.VERSION);
        serialization.writeBoolean(closed);
        serialization.writeLong(currentPeriodBlockIndex);
        serialization.writeInt(periodsCapacity);
        serialization.writeInt(periodsCount);
        serialization.writeLong(nextBlockIndex);
        serialization.writeInt(periodIndexId);
        serialization.writeInt(forecasterSpace.getFileIndex());
        serialization.writeInt(anomalyDetectorSpace.getFileIndex());
        serialization.writeInt(fastAnomalyDetectorSpace.getFileIndex());
    }

    private Period readPeriod(int periodIndex) {
        Assert.isTrue(periodIndex < periodsCount);

        long fileOffset = HEADER_SIZE + periodIndex * 8;
        RawPageDeserialization deserialization = new RawPageDeserialization(transactionProvider.getRawTransaction(), fileIndex, Constants.pageIndexByFileOffset(fileOffset),
                Constants.pageOffsetByFileOffset(fileOffset));
        long periodBlockIndex = deserialization.readLong();
        Period period = new Period(this, periodIndex, fileIndex, periodBlockIndex, false, 0);
        periods.put(periodIndex, period);
        return period;
    }

    private static void bindFile(CycleSchema schema, IRawTransaction transaction, int fileIndex, String filePrefix, int pathIndex) {
        RawBindInfo bindInfo = new RawBindInfo();
        bindInfo.setPathIndex(pathIndex);
        bindInfo.setName(Spaces.getSpaceDataFileName(filePrefix, fileIndex));
        bindInfo.setFlags(RawBindInfo.DIRECTORY_OWNER);

        String domainName = schema.getParent().getParent().getConfiguration().getName();
        Pair<String, String> pair = schema.getContext().getCacheCategorizationStrategy().categorize(new MapBuilder<String, String>()
                .put("type", "pages.data.period")
                .put("spaceName", schema.getParent().getConfiguration().getName())
                .put("periodName", schema.getConfiguration().getName())
                .put("domainName", domainName)
                .put("name", domainName + "." + schema.getParent().getConfiguration().getName() + "." +
                        schema.getConfiguration().getName())
                .toMap());
        bindInfo.setCategory(pair.getKey());
        bindInfo.setCategoryType(pair.getValue());

        transaction.bindFile(fileIndex, bindInfo);
    }

    public static void createNewIndexes(IIndexManager indexManager, int fileIndex, CycleSchema schema, int skipIndexCount,
                                        int skipBlobIndexCount, List<Integer> newIndexIds, List<Integer> newBlobIndexIds) {
        String filePrefix = getFilePrefix(schema, fileIndex);
        String indexesPath = Spaces.getSpaceIndexesDirName(filePrefix);

        int i = 0;
        Map<String, Pair<NodeSchemaConfiguration, FieldSchemaConfiguration>> indexedFields = new HashMap<String,
                Pair<NodeSchemaConfiguration, FieldSchemaConfiguration>>();

        for (NodeSchemaConfiguration node : schema.getConfiguration().getNodes()) {
            for (FieldSchemaConfiguration field : node.getFields()) {
                if (!field.isIndexed())
                    continue;

                String indexName = field.getIndexName();
                if (indexName == null)
                    indexName = node.getName() + "." + field.getName();

                Pair<NodeSchemaConfiguration, FieldSchemaConfiguration> indexedPair = indexedFields.get(indexName);
                if (indexedPair == null) {
                    indexedFields.put(indexName, new Pair<NodeSchemaConfiguration, FieldSchemaConfiguration>(node, field));
                    if (i >= skipIndexCount) {
                        IndexSchemaConfiguration indexConfiguration = createIndexSchemaConfiguration(schema, node, field);
                        String indexFilePrefix = indexesPath + File.separator + indexConfiguration.getType();
                        IUniqueIndex index = indexManager.createIndex(indexFilePrefix, indexConfiguration);
                        newIndexIds.add(index.getId());
                    }

                    i++;
                } else {
                    IndexSchemaConfiguration indexConfiguration1 = createIndexSchemaConfiguration(schema, indexedPair.getKey(), field);
                    IndexSchemaConfiguration indexConfiguration2 = createIndexSchemaConfiguration(schema, indexedPair.getKey(),
                            indexedPair.getValue());
                    Assert.isTrue(indexConfiguration1.equals(indexConfiguration2));
                    Assert.isTrue(field.isSorted() == indexedPair.getValue().isSorted());
                    Assert.isTrue(field.isCached() == indexedPair.getValue().isCached());
                }
            }
        }

        i = 0;
        Map<String, StructuredBlobIndexSchemaConfiguration> indexedBlobs = new HashMap<String, StructuredBlobIndexSchemaConfiguration>();
        for (NodeSchemaConfiguration node : schema.getConfiguration().getNodes()) {
            for (FieldSchemaConfiguration field : node.getFields()) {
                if (!(field instanceof StructuredBlobFieldSchemaConfiguration) || ((StructuredBlobFieldSchemaConfiguration) field).getIndexes().isEmpty())
                    continue;

                for (StructuredBlobIndexSchemaConfiguration blobIndex : ((StructuredBlobFieldSchemaConfiguration) field).getIndexes()) {
                    String indexName = blobIndex.getIndexName();
                    if (indexName == null)
                        indexName = node.getName() + "." + field.getName() + "." + blobIndex.getName();

                    StructuredBlobIndexSchemaConfiguration indexedBlob = indexedBlobs.get(indexName);
                    if (indexedBlob == null) {
                        indexedBlobs.put(indexName, blobIndex);
                        if (i >= skipBlobIndexCount) {
                            IndexSchemaConfiguration indexConfiguration = createBlobIndexSchemaConfiguration(schema, node, blobIndex);
                            String indexFilePrefix = indexesPath + File.separator + indexConfiguration.getType();
                            IUniqueIndex index = indexManager.createIndex(indexFilePrefix, indexConfiguration);
                            newBlobIndexIds.add(index.getId());
                        }
                        i++;
                    } else
                        Assert.isTrue(blobIndex.equals(indexedBlob));
                }
            }
        }
    }

    private void createIndexes(long indexFileIndexesFileOffset) {
        indexes = new NodeIndexInfo[schema.getConfiguration().getTotalIndexCount()];
        IRawTransaction transaction = schema.getContext().getTransactionProvider().getRawTransaction();

        int i = 0;
        Map<String, Pair<NodeSchemaConfiguration, FieldSchemaConfiguration>> indexedFields = new HashMap<String,
                Pair<NodeSchemaConfiguration, FieldSchemaConfiguration>>();
        List<Integer> indexIds = new ArrayList<Integer>();

        if (schema.getConfiguration().hasFullTextIndex()) {
            FullTextIndexSchemaConfiguration indexConfiguration = new FullTextIndexSchemaConfiguration(schema.getParent().getConfiguration().getName(),
                    schema.getParent().getConfiguration().getAlias(), schema.getParent().getConfiguration().getDescription(),
                    schema.getParent().getConfiguration().getFullTextPathIndex());
            String filePrefix = indexesPath + File.separator + indexConfiguration.getType();
            IFullTextIndex index = indexManager.createIndex(filePrefix, indexConfiguration);
            fullTextIndex = new PeriodNodeFullTextIndex(schema.getContext(), index, index.getId(), this);
            indexIds.add(index.getId());
        }

        for (NodeSchemaConfiguration node : schema.getConfiguration().getNodes()) {
            for (FieldSchemaConfiguration field : node.getFields()) {
                if (!field.isIndexed())
                    continue;

                String indexName = field.getIndexName();
                if (indexName == null)
                    indexName = node.getName() + "." + field.getName();

                Pair<NodeSchemaConfiguration, FieldSchemaConfiguration> indexedPair = indexedFields.get(indexName);
                if (indexedPair == null) {
                    indexedFields.put(indexName, new Pair<NodeSchemaConfiguration, FieldSchemaConfiguration>(node, field));
                    IndexSchemaConfiguration indexConfiguration = createIndexSchemaConfiguration(schema, node, field);
                    String filePrefix = indexesPath + File.separator + indexConfiguration.getType();
                    IUniqueIndex index = indexManager.createIndex(filePrefix, indexConfiguration);
                    indexes[i] = new NodeIndexInfo(index.getId(), field.isSorted(), field.isCached(), index);
                    indexesMap.put(indexName, i);

                    indexIds.add(index.getId());
                    i++;
                } else {
                    IndexSchemaConfiguration indexConfiguration1 = createIndexSchemaConfiguration(schema, indexedPair.getKey(), field);
                    IndexSchemaConfiguration indexConfiguration2 = createIndexSchemaConfiguration(schema, indexedPair.getKey(),
                            indexedPair.getValue());
                    Assert.isTrue(indexConfiguration1.equals(indexConfiguration2));
                    Assert.isTrue(field.isSorted() == indexedPair.getValue().isSorted());
                    Assert.isTrue(field.isCached() == indexedPair.getValue().isCached());
                }
            }
        }

        blobIndexes = new BlobIndexInfo[schema.getConfiguration().getTotalBlobIndexCount()];
        i = 0;
        Map<String, StructuredBlobIndexSchemaConfiguration> indexedBlobs = new HashMap<String, StructuredBlobIndexSchemaConfiguration>();
        for (NodeSchemaConfiguration node : schema.getConfiguration().getNodes()) {
            for (FieldSchemaConfiguration field : node.getFields()) {
                if (!(field instanceof StructuredBlobFieldSchemaConfiguration) || ((StructuredBlobFieldSchemaConfiguration) field).getIndexes().isEmpty())
                    continue;

                for (StructuredBlobIndexSchemaConfiguration blobIndex : ((StructuredBlobFieldSchemaConfiguration) field).getIndexes()) {
                    String indexName = blobIndex.getIndexName();
                    if (indexName == null)
                        indexName = node.getName() + "." + field.getName() + "." + blobIndex.getName();

                    StructuredBlobIndexSchemaConfiguration indexedBlob = indexedBlobs.get(indexName);
                    if (indexedBlob == null) {
                        indexedBlobs.put(indexName, blobIndex);
                        IndexSchemaConfiguration indexConfiguration = createBlobIndexSchemaConfiguration(schema, node, blobIndex);
                        String filePrefix = indexesPath + File.separator + indexConfiguration.getType();
                        IUniqueIndex index = indexManager.createIndex(filePrefix, indexConfiguration);
                        blobIndexes[i] = new BlobIndexInfo(index.getId(), index);
                        indexIds.add(index.getId());
                        i++;
                    } else
                        Assert.isTrue(blobIndex.equals(indexedBlob));
                }
            }
        }

        RawPageSerialization serialization = new RawPageSerialization(transaction,
                0, Constants.pageIndexByFileOffset(indexFileIndexesFileOffset), Constants.pageOffsetByFileOffset(indexFileIndexesFileOffset));
        for (Integer id : indexIds)
            serialization.writeInt(id);
    }

    private void openIndexes(long indexFileIndexesFileOffset) {
        indexes = new NodeIndexInfo[schema.getConfiguration().getTotalIndexCount()];

        IRawTransaction transaction = schema.getContext().getTransactionProvider().getRawTransaction();
        RawPageDeserialization deserialization = new RawPageDeserialization(transaction,
                0, Constants.pageIndexByFileOffset(indexFileIndexesFileOffset), Constants.pageOffsetByFileOffset(indexFileIndexesFileOffset));

        int fullTextIndexId = 0;
        if (schema.getConfiguration().hasFullTextIndex())
            fullTextIndexId = deserialization.readInt();

        int i = 0;
        Set<String> indexNames = new HashSet<String>();
        for (NodeSchemaConfiguration node : schema.getConfiguration().getNodes()) {
            for (FieldSchemaConfiguration field : node.getFields()) {
                if (!field.isIndexed())
                    continue;

                String indexName = field.getIndexName();
                if (indexName == null)
                    indexName = node.getName() + "." + field.getName();

                if (!indexNames.contains(indexName)) {
                    indexNames.add(indexName);
                    indexes[i] = new NodeIndexInfo(deserialization.readInt(), field.isSorted(), field.isCached(), null);
                    indexesMap.put(indexName, i);
                    i++;
                }
            }
        }

        blobIndexes = new BlobIndexInfo[schema.getConfiguration().getTotalBlobIndexCount()];
        i = 0;
        indexNames = new HashSet<String>();
        for (NodeSchemaConfiguration node : schema.getConfiguration().getNodes()) {
            for (FieldSchemaConfiguration field : node.getFields()) {
                if (!(field instanceof StructuredBlobFieldSchemaConfiguration) || ((StructuredBlobFieldSchemaConfiguration) field).getIndexes().isEmpty())
                    continue;

                for (StructuredBlobIndexSchemaConfiguration blobIndex : ((StructuredBlobFieldSchemaConfiguration) field).getIndexes()) {
                    String indexName = blobIndex.getIndexName();
                    if (indexName == null)
                        indexName = node.getName() + "." + field.getName() + "." + blobIndex.getName();

                    if (!indexNames.contains(indexName)) {
                        indexNames.add(indexName);
                        blobIndexes[i] = new BlobIndexInfo(deserialization.readInt(), null);
                        i++;
                    }
                }
            }
        }

        if (schema.getConfiguration().hasFullTextIndex()) {
            fullTextIndex = new PeriodNodeFullTextIndex(schema.getContext(), null, fullTextIndexId, this);
            fullTextIndex.reindex();
        }
    }

    private IUniqueIndex getBlobIndex(int i) {
        BlobIndexInfo info = blobIndexes[i];
        if (info.index == null || info.index.isStale()) {
            IUniqueIndex index = indexManager.getIndex(info.id);
            info.index = index;
        }

        return info.index;
    }

    private BTreeIndexSchemaConfiguration createPeriodIndexSchemaConfiguration() {
        String domainName = schema.getParent().getParent().getConfiguration().getName();
        String domainAlias = schema.getParent().getParent().getConfiguration().getAlias();
        Map<String, String> properties = new MapBuilder<String, String>()
                .put("spaceName", schema.getConfiguration().getName())
                .put("domainName", domainName)
                .put("periodName", schema.getConfiguration().getName())
                .put("name", domainName + "." + schema.getParent().getConfiguration().getName() +
                        "." + schema.getConfiguration().getName() + ".periodIndex")
                .toMap();

        String name = domainName + "." + schema.getParent().getConfiguration().getName() + "." +
                schema.getConfiguration().getName() + ".periodIndex";
        String alias = domainAlias + "." + schema.getParent().getConfiguration().getAlias() + "." +
                schema.getConfiguration().getAlias() + ".periodIndex";

        BTreeIndexSchemaConfiguration periodIndexSchema = new BTreeIndexSchemaConfiguration(name,
                alias, schema.getParent().getConfiguration().getDescription(), schema.getParent().getConfiguration().getPathIndex(), true,
                8, true, 4, new NumericKeyNormalizerSchemaConfiguration(DataType.LONG),
                new IntValueConverterSchemaConfiguration(), true, true, properties);
        return periodIndexSchema;
    }

    private static IndexSchemaConfiguration createIndexSchemaConfiguration(CycleSchema schema, NodeSchemaConfiguration node, FieldSchemaConfiguration field) {
        String domainName = schema.getParent().getParent().getConfiguration().getName();
        String domainAlias = schema.getParent().getParent().getConfiguration().getAlias();
        Map<String, String> properties = new MapBuilder<String, String>()
                .put("spaceName", schema.getConfiguration().getName())
                .put("domainName", domainName)
                .put("periodName", schema.getConfiguration().getName())
                .put("nodeName", node.getName())
                .put("name", domainName + "." + schema.getParent().getConfiguration().getName() +
                        "." + schema.getConfiguration().getName() + "." + node.getName())
                .toMap();

        String namePrefix = domainName + "." + schema.getParent().getConfiguration().getName() + "." +
                schema.getConfiguration().getName() + "." + node.getName() + ".";
        String aliasPrefix = domainAlias + "." + schema.getParent().getConfiguration().getAlias() + "." +
                schema.getConfiguration().getAlias() + "." + node.getAlias() + ".";

        IndexSchemaConfiguration indexSchema = field.createIndexSchemaConfiguration(namePrefix, aliasPrefix, properties);
        if (indexSchema instanceof BTreeIndexSchemaConfiguration) {
            BTreeIndexSchemaConfiguration btreeIndexSchema = (BTreeIndexSchemaConfiguration) indexSchema;

            int maxKeySize;
            KeyNormalizerSchemaConfiguration keyNormalizer;
            if (field instanceof IndexedLocationFieldSchemaConfiguration) {
                maxKeySize = btreeIndexSchema.getMaxKeySize() + 8;
                keyNormalizer = new FixedCompositeKeyNormalizerSchemaConfiguration(Arrays.asList(
                        new NumericKeyNormalizerSchemaConfiguration(DataType.INT), new NumericKeyNormalizerSchemaConfiguration(DataType.INT),
                        btreeIndexSchema.getKeyNormalizer()));
            } else {
                maxKeySize = btreeIndexSchema.getMaxKeySize() + 4;
                keyNormalizer = new FixedCompositeKeyNormalizerSchemaConfiguration(Arrays.asList(
                        new NumericKeyNormalizerSchemaConfiguration(DataType.INT), btreeIndexSchema.getKeyNormalizer()));
            }

            return new BTreeIndexSchemaConfiguration(indexSchema.getName(), indexSchema.getAlias(), indexSchema.getDescription(),
                    indexSchema.getPathIndex(), btreeIndexSchema.isFixedKey(), maxKeySize, btreeIndexSchema.isFixedValue(),
                    btreeIndexSchema.getMaxValueSize(), keyNormalizer, btreeIndexSchema.getValueConverter(),
                    btreeIndexSchema.isSorted(), btreeIndexSchema.isUnique(), btreeIndexSchema.getProperties());
        } else if (indexSchema instanceof TreeIndexSchemaConfiguration) {
            TreeIndexSchemaConfiguration treeIndexSchema = (TreeIndexSchemaConfiguration) indexSchema;

            int maxKeySize = treeIndexSchema.getMaxKeySize() + 4;
            KeyNormalizerSchemaConfiguration keyNormalizer = new FixedCompositeKeyNormalizerSchemaConfiguration(Arrays.asList(
                    new NumericKeyNormalizerSchemaConfiguration(DataType.INT), treeIndexSchema.getKeyNormalizer()));

            return new TreeIndexSchemaConfiguration(indexSchema.getName(), indexSchema.getAlias(), indexSchema.getDescription(),
                    indexSchema.getPathIndex(), treeIndexSchema.isFixedKey(), maxKeySize, treeIndexSchema.isFixedValue(),
                    treeIndexSchema.getMaxValueSize(), keyNormalizer, treeIndexSchema.getValueConverter(),
                    treeIndexSchema.isSorted(), treeIndexSchema.isUnique(), treeIndexSchema.getProperties());
        } else if (indexSchema instanceof HashIndexSchemaConfiguration) {
            HashIndexSchemaConfiguration hashIndexSchema = (HashIndexSchemaConfiguration) indexSchema;

            int maxKeySize = hashIndexSchema.getMaxKeySize() + 4;
            KeyNormalizerSchemaConfiguration keyNormalizer = new FixedCompositeKeyNormalizerSchemaConfiguration(Arrays.asList(
                    new NumericKeyNormalizerSchemaConfiguration(DataType.INT), hashIndexSchema.getKeyNormalizer()));

            return new HashIndexSchemaConfiguration(indexSchema.getName(), indexSchema.getAlias(), indexSchema.getDescription(),
                    indexSchema.getPathIndex(), hashIndexSchema.isFixedKey(), maxKeySize, hashIndexSchema.isFixedValue(),
                    hashIndexSchema.getMaxValueSize(), keyNormalizer, hashIndexSchema.getValueConverter(),
                    hashIndexSchema.getProperties());
        } else
            return Assert.error();
    }

    private static IndexSchemaConfiguration createBlobIndexSchemaConfiguration(CycleSchema schema,
                                                                               NodeSchemaConfiguration node, StructuredBlobIndexSchemaConfiguration configuration) {
        String domainName = schema.getParent().getParent().getConfiguration().getName();
        String domainAlias = schema.getParent().getParent().getConfiguration().getAlias();
        Map<String, String> properties = new MapBuilder<String, String>()
                .put("spaceName", schema.getConfiguration().getName())
                .put("domainName", domainName)
                .put("periodName", schema.getConfiguration().getName())
                .put("nodeName", node.getName())
                .put("name", domainName + "." + schema.getParent().getConfiguration().getName() +
                        "." + schema.getConfiguration().getName() + "." + node.getName())
                .toMap();

        String namePrefix = domainName + "." + schema.getParent().getConfiguration().getName() + "." +
                schema.getConfiguration().getName() + "." + node.getName() + ".";
        String aliasPrefix = domainAlias + "." + schema.getParent().getConfiguration().getAlias() + "." +
                schema.getConfiguration().getAlias() + "." + node.getAlias() + ".";

        int maxKeySize = configuration.getMaxKeySize() + 10;
        KeyNormalizerSchemaConfiguration keyNormalizer = new ByteArrayKeyNormalizerSchemaConfiguration();

        switch (configuration.getIndexType()) {
            case BTREE:
                return new BTreeIndexSchemaConfiguration(namePrefix + configuration.getName(), aliasPrefix + configuration.getAlias(), configuration.getDescription(),
                        configuration.getPathIndex(), configuration.isFixedKey(), maxKeySize,
                        true, 8, keyNormalizer, new LongValueConverterSchemaConfiguration(), configuration.isSorted(), configuration.isUnique(), properties);
            case TREE:
                return new TreeIndexSchemaConfiguration(namePrefix + configuration.getName(), aliasPrefix + configuration.getAlias(), configuration.getDescription(),
                        configuration.getPathIndex(), configuration.isFixedKey(), maxKeySize,
                        true, 8, keyNormalizer, new LongValueConverterSchemaConfiguration(), configuration.isSorted(),
                        configuration.isUnique(), properties);
            case HASH:
                return new HashIndexSchemaConfiguration(namePrefix + configuration.getName(), aliasPrefix + configuration.getAlias(), configuration.getDescription(),
                        configuration.getPathIndex(), configuration.isFixedKey(), maxKeySize,
                        true, 8, keyNormalizer, new LongValueConverterSchemaConfiguration(), properties);
            default:
                return Assert.error();
        }
    }

    private static String getFilePrefix(CycleSchema schema, int fileIndex) {
        return PeriodSpaces.getPeriodSpacePrefix(schema.getParent().getParent().getConfiguration().getName(),
                schema.getParent().getConfiguration(), schema.getConfiguration(), fileIndex);
    }

    private ISortedIndex<Long, Integer> getPeriodIndex() {
        if (!periodIndex.isStale())
            return periodIndex;
        else {
            periodIndex = indexManager.getIndex(periodIndex.getId());
            return periodIndex;
        }
    }

    public static class NodeIndexInfo {
        public final int id;
        public final boolean sorted;
        public final boolean cached;
        public IUniqueIndex<Object, Long> index;

        public NodeIndexInfo(int id, boolean sorted, boolean cached, IUniqueIndex<Object, Long> index) {
            this.id = id;
            this.sorted = sorted;
            this.cached = cached;
            this.index = index;
        }
    }

    public static class BlobIndexInfo {
        public final int id;
        private IUniqueIndex index;

        public BlobIndexInfo(int id, IUniqueIndex index) {
            this.id = id;
            this.index = index;
        }
    }

    private interface IMessages {
        @DefaultMessage("Invalid format of file ''{0}''.")
        ILocalizedMessage invalidFormat(int fileIndex);

        @DefaultMessage("Unsupported version ''{1}'' of file ''{0}'', expected version - ''{2}''.")
        ILocalizedMessage unsupportedVersion(int fileIndex, int fileVersion, int expectedVersion);
    }
}
