/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.index.btree;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.exametrika.api.exadb.core.IOperation;
import com.exametrika.api.exadb.index.BTreeIndexStatistics;
import com.exametrika.api.exadb.index.IBTreeIndex;
import com.exametrika.api.exadb.index.IIndexListener;
import com.exametrika.api.exadb.index.IKeyNormalizer;
import com.exametrika.api.exadb.index.IValueConverter;
import com.exametrika.api.exadb.index.Indexes;
import com.exametrika.api.exadb.index.config.schema.BTreeIndexSchemaConfiguration;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.rawdb.IRawBatchControl;
import com.exametrika.common.rawdb.IRawPage;
import com.exametrika.common.rawdb.IRawReadRegion;
import com.exametrika.common.rawdb.IRawTransaction;
import com.exametrika.common.rawdb.IRawWriteRegion;
import com.exametrika.common.rawdb.RawBindInfo;
import com.exametrika.common.rawdb.RawDatabaseException;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.MapBuilder;
import com.exametrika.common.utils.Out;
import com.exametrika.common.utils.Pair;
import com.exametrika.common.utils.Strings;
import com.exametrika.impl.exadb.core.Constants;
import com.exametrika.impl.exadb.core.Spaces;
import com.exametrika.impl.exadb.index.AbstractIndexSpace;
import com.exametrika.impl.exadb.index.BloomFilterSpace;
import com.exametrika.impl.exadb.index.IndexManager;
import com.exametrika.impl.exadb.index.btree.BTreeNode.ContextInfo;
import com.exametrika.spi.exadb.core.IDataFileAllocator;
import com.exametrika.spi.exadb.core.ITransactionProvider;
import com.exametrika.spi.exadb.index.config.schema.IndexSchemaConfiguration;
import com.ibm.icu.text.MessageFormat;


/**
 * The {@link BTreeIndexSpace} is a B+ Tree index space.
 *
 * @param <K> key type
 * @param <V> value type
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class BTreeIndexSpace<K, V> extends AbstractIndexSpace implements IBTreeIndex<K, V> {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final short MAGIC = 0x170F;
    private static final int VERSION_OFFSET = 2;// magic(short) + version(byte) + padding(byte) + rootNodePageIndex(long)
    private static final int ROOT_NODE_PAGE_INDEX_OFFSET = 4;//  + freeAreaPageIndex(long) + lastFreePageIndex(long)
    private static final int FREE_AREA_PAGE_INDEX_OFFSET = 12;// + firstLeafNodePageIndex(long) + lastLeafNodePageIndex(long)
    private static final int LAST_FREE_PAGE_INDEX_OFFSET = 20;// + indexHeight(int) + leafElementCount(long) + parentElementCount(long)
    private static final int FIRST_LEAF_NODE_PAGE_INDEX_OFFSET = 28;// + leafNodeCount(long) + parentNodeCount(long) + leafDataSize(long)
    private static final int LAST_LEAF_NODE_PAGE_INDEX_OFFSET = 36;// + parentDataSize(long) + changeCount(long) + statChangeCount(long)
    private static final int INDEX_HEIGHT_OFFSET = 44;             // + currentStatisticsFileIndex(int) + buildingStatisticsFileIndex(int)
    private static final int LEAF_ELEMENT_COUNT_OFFSET = 48;       // + bloomFilterSpaceFileIndex(int
    private static final int PARENT_ELEMENT_COUNT_OFFSET = 56;
    private static final int LEAF_NODE_COUNT_OFFSET = 64;
    private static final int PARENT_NODE_COUNT_OFFSET = 72;
    private static final int LEAF_DATA_SIZE_OFFSET = 80;
    private static final int PARENT_DATA_SIZE_OFFSET = 88;
    private static final int CHANGE_COUNT_OFFSET = 96;
    private static final int STAT_CHANGE_COUNT_OFFSET = 104;
    private static final int CURRENT_STATISTICS_FILE_INDEX_OFFSET = 112;
    private static final int BUILDING_STATISTICS_FILE_INDEX_OFFSET = 116;
    private static final int BLOOM_FILTER_SPACE_FILE_INDEX_OFFSET = 120;
    private static final short FREE_PAGE_MAGIC = 0x1712;// magic(short) + nextFreePageIndex(long)
    private static final int FREE_PAGE_NEXT_FREE_PAGE_INDEX_OFFSET = 2;
    private static final int BLOOM_FILTER_PAGE_TYPE = 0;
    private static final int BLOOM_FILTER_INITIAL_HASH_COUNT = 10;
    private final ITransactionProvider transactionProvider;
    private final int fileIndex;
    private final boolean fixedKey;
    private final int maxKeySize;
    private final boolean fixedValue;
    private final int maxValueSize;
    private final IKeyNormalizer<K> keyNormalizer;
    private final IValueConverter<V> valueConverter;
    private final IIndexListener<V> listener;
    private IRawPage headerPage;
    private int modCount;
    private BTreeIndexSpace<ByteArray, Long> currentStatistics;
    private BTreeIndexSpace<ByteArray, Long> buildingStatistics;
    private BloomFilterSpace bloomFilterSpace;

    public static <K, V> BTreeIndexSpace create(IndexManager indexManager, IndexSchemaConfiguration schema,
                                                ITransactionProvider transactionProvider, IDataFileAllocator fileAllocator,
                                                int fileIndex, String filePrefix, boolean fixedKey, int maxKeySize, boolean fixedValue, int maxValueSize,
                                                IKeyNormalizer<K> keyNormalizer, IValueConverter<V> valueConverter, boolean createStatistics,
                                                IIndexListener<V> listener, Map<String, String> properties, boolean createBloomFilter) {
        Assert.notNull(indexManager);
        Assert.notNull(schema);
        Assert.notNull(properties);

        Assert.notNull(transactionProvider);
        Assert.isTrue(Constants.PAGE_SIZE <= Short.MAX_VALUE);
        Assert.isTrue(maxKeySize <= Constants.PAGE_SIZE / 16);
        Assert.isTrue(maxValueSize <= Constants.PAGE_SIZE / 16);

        IRawTransaction transaction = transactionProvider.getRawTransaction();
        bindFile(transaction, fileIndex, filePrefix, schema.getPathIndex(), indexManager, properties);

        BTreeIndexSpace<ByteArray, Long> firstStatistics = null;
        BTreeIndexSpace<ByteArray, Long> secondStatistics = null;
        if (createStatistics && ((BTreeIndexSchemaConfiguration) schema).isSorted()) {
            Assert.notNull(fileAllocator);

            firstStatistics = BTreeIndexSpace.create(indexManager, schema,
                    transactionProvider, null, fileAllocator.allocateFile(transactionProvider.getRawTransaction()),
                    filePrefix + "-stat", fixedKey, maxKeySize, true, 8, Indexes.createByteArrayKeyNormalizer(),
                    Indexes.createLongValueConverter(), false, null, properties, false);
            secondStatistics = BTreeIndexSpace.create(indexManager, schema,
                    transactionProvider, null, fileAllocator.allocateFile(transactionProvider.getRawTransaction()),
                    filePrefix + "-stat", fixedKey, maxKeySize, true, 8, Indexes.createByteArrayKeyNormalizer(),
                    Indexes.createLongValueConverter(), false, null, properties, false);
        }

        BloomFilterSpace bloomFilterSpace = null;
        if (createBloomFilter)
            bloomFilterSpace = BloomFilterSpace.create(fileAllocator.allocateFile(transactionProvider.getRawTransaction()),
                    filePrefix, schema.getPathIndex(), BLOOM_FILTER_PAGE_TYPE, indexManager.getContext(), properties,
                    BLOOM_FILTER_INITIAL_HASH_COUNT);

        BTreeIndexSpace index = new BTreeIndexSpace(createStatistics ? indexManager : null, schema, transactionProvider, fileIndex, fixedKey,
                maxKeySize, fixedValue, maxValueSize, keyNormalizer, valueConverter, listener);
        index.currentStatistics = firstStatistics;
        index.buildingStatistics = secondStatistics;
        index.bloomFilterSpace = bloomFilterSpace;
        index.writeHeader();

        return index;
    }

    public static <K, V> BTreeIndexSpace open(IndexManager indexManager, IndexSchemaConfiguration schema,
                                              ITransactionProvider transactionProvider, int fileIndex, String filePrefix, boolean fixedKey, int maxKeySize,
                                              boolean fixedValue, int maxValueSize, IKeyNormalizer<K> keyNormalizer, IValueConverter<V> valueConverter,
                                              IIndexListener<V> listener, Map<String, String> properties, boolean mainIndex) {
        Assert.notNull(indexManager);
        Assert.notNull(schema);
        Assert.notNull(properties);

        Assert.notNull(transactionProvider);
        Assert.isTrue(Constants.PAGE_SIZE <= Short.MAX_VALUE);
        Assert.isTrue(maxKeySize <= Constants.PAGE_SIZE / 16);
        Assert.isTrue(maxValueSize <= Constants.PAGE_SIZE / 16);

        IRawTransaction transaction = transactionProvider.getRawTransaction();
        bindFile(transaction, fileIndex, filePrefix, schema.getPathIndex(), indexManager, properties);

        BTreeIndexSpace index = new BTreeIndexSpace(mainIndex ? indexManager : null, schema, transactionProvider, fileIndex, fixedKey,
                maxKeySize, fixedValue, maxValueSize, keyNormalizer, valueConverter, listener);
        index.readHeader(filePrefix, properties);

        return index;
    }

    public int getFileIndex() {
        return fileIndex;
    }

    public boolean isFixedKey() {
        return fixedKey;
    }

    public int getMaxKeySize() {
        return maxKeySize;
    }

    public boolean isFixedValue() {
        return fixedValue;
    }

    public int getMaxValueSize() {
        return maxValueSize;
    }

    public IIndexListener<V> getListener() {
        return listener;
    }

    public IKeyNormalizer<K> getKeyNormalizer() {
        return keyNormalizer;
    }

    public IValueConverter<V> getValueConverter() {
        return valueConverter;
    }

    public BTreeNode getNode(long nodePageIndex) {
        IRawPage page = transactionProvider.getRawTransaction().getPage(fileIndex, nodePageIndex);
        BTreeNode node = (BTreeNode) page.getData();
        if (node == null)
            node = loadNode(page);

        return node;
    }

    public void updateIndexHeight(int value) {
        IRawWriteRegion region = headerPage.getWriteRegion();
        int indexHeight = region.readInt(INDEX_HEIGHT_OFFSET);
        region.writeInt(INDEX_HEIGHT_OFFSET, indexHeight + value);
    }

    public void updateLeafElementCount(long value) {
        IRawWriteRegion region = headerPage.getWriteRegion();
        long elementCount = region.readLong(LEAF_ELEMENT_COUNT_OFFSET);
        region.writeLong(LEAF_ELEMENT_COUNT_OFFSET, elementCount + value);
    }

    public void updateParentElementCount(long value) {
        IRawWriteRegion region = headerPage.getWriteRegion();
        long elementCount = region.readLong(PARENT_ELEMENT_COUNT_OFFSET);
        region.writeLong(PARENT_ELEMENT_COUNT_OFFSET, elementCount + value);
    }

    public void updateLeafNodeCount(long value) {
        IRawWriteRegion region = headerPage.getWriteRegion();
        long nodeCount = region.readLong(LEAF_NODE_COUNT_OFFSET);
        region.writeLong(LEAF_NODE_COUNT_OFFSET, nodeCount + value);
    }

    public void updateParentNodeCount(long value) {
        IRawWriteRegion region = headerPage.getWriteRegion();
        long nodeCount = region.readLong(PARENT_NODE_COUNT_OFFSET);
        region.writeLong(PARENT_NODE_COUNT_OFFSET, nodeCount + value);
    }

    public void updateLeafDataSize(long value) {
        IRawWriteRegion region = headerPage.getWriteRegion();
        long dataSize = region.readLong(LEAF_DATA_SIZE_OFFSET);
        region.writeLong(LEAF_DATA_SIZE_OFFSET, dataSize + value);
    }

    public void updateParentDataSize(long value) {
        IRawWriteRegion region = headerPage.getWriteRegion();
        long dataSize = region.readLong(PARENT_DATA_SIZE_OFFSET);
        region.writeLong(PARENT_DATA_SIZE_OFFSET, dataSize + value);
    }

    public long getFirstLeafNodePageIndex() {
        return headerPage.getReadRegion().readLong(FIRST_LEAF_NODE_PAGE_INDEX_OFFSET);
    }

    public void setFirstLeafNodePageIndex(long pageIndex) {
        headerPage.getWriteRegion().writeLong(FIRST_LEAF_NODE_PAGE_INDEX_OFFSET, pageIndex);
    }

    public long getLastLeafNodePageIndex() {
        return headerPage.getReadRegion().readLong(LAST_LEAF_NODE_PAGE_INDEX_OFFSET);
    }

    public void setLastLeafNodePageIndex(long pageIndex) {
        headerPage.getWriteRegion().writeLong(LAST_LEAF_NODE_PAGE_INDEX_OFFSET, pageIndex);
    }

    public IRawPage allocatePage() {
        IRawTransaction transaction = transactionProvider.getRawTransaction();
        IRawWriteRegion region = headerPage.getWriteRegion();
        long pageIndex = region.readLong(LAST_FREE_PAGE_INDEX_OFFSET);
        if (pageIndex != 0) {
            IRawPage page = transaction.getPage(fileIndex, pageIndex);
            IRawReadRegion pageRegion = page.getReadRegion();
            if (pageRegion.readShort(0) != FREE_PAGE_MAGIC)
                throw new RawDatabaseException(messages.invalidFormat(page.getFile().getIndex()));
            pageIndex = pageRegion.readLong(FREE_PAGE_NEXT_FREE_PAGE_INDEX_OFFSET);
            region.writeLong(LAST_FREE_PAGE_INDEX_OFFSET, pageIndex);
            return page;
        }

        pageIndex = region.readLong(FREE_AREA_PAGE_INDEX_OFFSET);
        region.writeLong(FREE_AREA_PAGE_INDEX_OFFSET, pageIndex + 1);

        return transaction.getPage(fileIndex, pageIndex);
    }

    public void freePage(IRawPage page) {
        IRawWriteRegion region = headerPage.getWriteRegion();
        long lastFreePageIndex = region.readLong(LAST_FREE_PAGE_INDEX_OFFSET);
        region.writeLong(LAST_FREE_PAGE_INDEX_OFFSET, page.getIndex());

        region = page.getWriteRegion();
        region.writeShort(0, FREE_PAGE_MAGIC);
        region.writeLong(FREE_PAGE_NEXT_FREE_PAGE_INDEX_OFFSET, lastFreePageIndex);
    }

    public <T extends IRawReadRegion> T findValueRegion(K key, boolean readOnly) {
        Assert.notNull(key);
        if (!readOnly)
            checkWriteTransaction();

        long rootNodePageIndex = headerPage.getReadRegion().readLong(ROOT_NODE_PAGE_INDEX_OFFSET);
        if (rootNodePageIndex == 0)
            return null;

        ByteArray byteKey = keyNormalizer.normalize(key);
        BTreeLeafNode node = findNode(byteKey, rootNodePageIndex);
        return node.findValueRegion(byteKey, readOnly);
    }

    public void setValueSize(K key, int size) {
        Assert.notNull(key);
        Assert.isTrue(!fixedValue);
        Assert.isTrue(size >= 0 && size <= maxValueSize);
        checkWriteTransaction();

        long rootNodePageIndex = headerPage.getReadRegion().readLong(ROOT_NODE_PAGE_INDEX_OFFSET);
        if (rootNodePageIndex == 0)
            return;

        ByteArray byteKey = keyNormalizer.normalize(key);
        BTreeLeafNode node = findNode(byteKey, rootNodePageIndex);
        ByteArray value = node.find(byteKey);
        if (value != null) {
            byte[] buffer = new byte[size];
            System.arraycopy(value.getBuffer(), value.getOffset(), buffer, 0, Math.min(size, value.getLength()));
            add(byteKey, new ByteArray(buffer), false);
        }
    }

    @Override
    public boolean isEmpty() {
        return getCount() == 0;
    }

    @Override
    public long getCount() {
        Assert.checkState(!isStale());

        IRawReadRegion region = headerPage.getReadRegion();
        long leafElementCount = region.readLong(LEAF_ELEMENT_COUNT_OFFSET);
        return leafElementCount;
    }

    @Override
    public Pair<ByteArray, V> findFirst() {
        Assert.checkState(!isStale());

        BTreeLeafNode node = findFirstNode();
        if (node != null) {
            Pair<ByteArray, ByteArray> pair = node.findFirst();
            return new Pair<ByteArray, V>(pair.getKey(), valueConverter.toValue(pair.getValue()));
        } else
            return null;
    }

    @Override
    public V findFirstValue() {
        Assert.checkState(!isStale());

        BTreeLeafNode node = findFirstNode();
        if (node != null) {
            ByteArray buffer = node.findFirstValue();
            return valueConverter.toValue(buffer);
        } else
            return null;
    }

    @Override
    public Pair<ByteArray, V> findLast() {
        Assert.checkState(!isStale());

        BTreeLeafNode node = findLastNode();
        if (node != null) {
            Pair<ByteArray, ByteArray> pair = node.findLast();
            return new Pair<ByteArray, V>(pair.getKey(), valueConverter.toValue(pair.getValue()));
        } else
            return null;
    }

    @Override
    public V findLastValue() {
        Assert.checkState(!isStale());

        BTreeLeafNode node = findLastNode();
        if (node != null) {
            ByteArray buffer = node.findLastValue();
            return valueConverter.toValue(buffer);
        } else
            return null;
    }

    @Override
    public Pair<ByteArray, V> findFloor(K key, boolean inclusive) {
        Assert.checkState(!isStale());

        return (Pair<ByteArray, V>) findFloor(key, inclusive, true);
    }

    @Override
    public V findFloorValue(K key, boolean inclusive) {
        Assert.checkState(!isStale());

        return (V) findFloor(key, inclusive, false);
    }

    @Override
    public Pair<ByteArray, V> findCeiling(K key, boolean inclusive) {
        Assert.checkState(!isStale());

        return (Pair<ByteArray, V>) findCeiling(key, inclusive, true);
    }

    @Override
    public V findCeilingValue(K key, boolean inclusive) {
        Assert.checkState(!isStale());

        return (V) findCeiling(key, inclusive, false);
    }

    @Override
    public V find(K key) {
        Assert.notNull(key);
        Assert.checkState(!isStale());

        ByteArray byteKey = keyNormalizer.normalize(key);
        if (bloomFilterSpace != null && bloomFilterSpace.isNotContained(byteKey))
            return null;

        long rootNodePageIndex = headerPage.getReadRegion().readLong(ROOT_NODE_PAGE_INDEX_OFFSET);
        if (rootNodePageIndex == 0)
            return null;

        BTreeLeafNode node = findNode(byteKey, rootNodePageIndex);
        ByteArray byteValue = node.find(byteKey);
        if (byteValue != null)
            return valueConverter.toValue(byteValue);
        else
            return null;
    }

    @Override
    public Iterable<Pair<ByteArray, V>> find(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
        Assert.checkState(!isStale());

        return find(fromKey != null ? keyNormalizer.normalize(fromKey) : null, fromInclusive,
                toKey != null ? keyNormalizer.normalize(toKey) : null, toInclusive, false);
    }

    @Override
    public Iterable<V> findValues(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
        Assert.checkState(!isStale());

        return find(fromKey != null ? keyNormalizer.normalize(fromKey) : null, fromInclusive,
                toKey != null ? keyNormalizer.normalize(toKey) : null, toInclusive, true);
    }

    @Override
    public void add(K key, V value) {
        Assert.notNull(key);
        Assert.notNull(value);
        Assert.checkState(!isStale());
        checkWriteTransaction();

        ByteArray byteKey = keyNormalizer.normalize(key);
        ByteArray byteValue = valueConverter.toByteArray(value);
        add(byteKey, byteValue, false);
    }

    @Override
    public void bulkAdd(Iterable<Pair<K, V>> elements) {
        Assert.notNull(elements);
        Assert.checkState(!isStale());
        checkWriteTransaction();

        ByteArray lastByteKey = null;
        for (Pair<K, V> element : elements) {
            ByteArray byteKey = keyNormalizer.normalize(element.getKey());
            ByteArray byteValue = valueConverter.toByteArray(element.getValue());

            if (lastByteKey != null)
                Assert.isTrue(lastByteKey.compareTo(byteKey) <= 0);

            lastByteKey = byteKey;

            add(byteKey, byteValue, true);
        }
    }

    @Override
    public void remove(K key) {
        Assert.notNull(key);
        Assert.checkState(!isStale());
        checkWriteTransaction();

        IRawWriteRegion region = headerPage.getWriteRegion();
        long rootNodePageIndex = region.readLong(ROOT_NODE_PAGE_INDEX_OFFSET);
        if (rootNodePageIndex == 0)
            return;

        ByteArray byteKey = keyNormalizer.normalize(key);
        if (remove(byteKey, null, rootNodePageIndex, -1, new ArrayDeque<ContextInfo>(), null)) {
            BTreeNode node = getNode(rootNodePageIndex);
            if (node instanceof BTreeParentNode) {
                region = headerPage.getWriteRegion();
                region.writeLong(ROOT_NODE_PAGE_INDEX_OFFSET, ((BTreeParentNode) node).getLastChildPageIndex());
                freePage(node.getPage());
                incrementChangeCount(region);
                updateIndexHeight(-1);
                updateParentNodeCount(-1);
            } else
                clear();
        } else
            incrementChangeCount(headerPage.getWriteRegion());
    }

    @Override
    public void clear() {
        Assert.checkState(!isStale());
        checkWriteTransaction();

        if (listener != null)
            listener.onCleared();

        IRawPage page = headerPage;
        transactionProvider.getRawTransaction().getFile(fileIndex).truncate(page.getSize());
        writeHeader();
        incrementChangeCount(page.getWriteRegion());

        if (bloomFilterSpace != null)
            bloomFilterSpace.clear();
    }

    @Override
    public ByteArray normalize(K key) {
        Assert.notNull(key);
        Assert.checkState(!isStale());

        return keyNormalizer.normalize(key);
    }

    @Override
    public long estimate(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
        Assert.checkState(!isStale());

        if (currentStatistics == null || currentStatistics.isEmpty())
            return 0;

        ByteArray from = null;
        if (fromKey != null)
            from = keyNormalizer.normalize(fromKey);

        ByteArray to = null;
        if (toKey != null)
            to = keyNormalizer.normalize(toKey);

        Long start = currentStatistics.findCeilingValue(from, fromInclusive);
        if (start == null)
            start = currentStatistics.findLastValue();

        Long end = currentStatistics.findFloorValue(to, toInclusive);
        if (end == null)
            end = currentStatistics.findFirstValue();

        if (end > start)
            return end - start;
        else
            return 0;
    }

    @Override
    public Pair<ByteArray, Long> rebuildStatistics(IRawBatchControl batchControl, Pair<ByteArray, Long> startBin,
                                                   double keyRatio, long rebuildThreshold, boolean force) {
        if (currentStatistics == null)
            return null;

        Assert.notNull(batchControl);
        Assert.checkState(!isStale());
        checkWriteTransaction();

        ByteArray startKey;
        long i;
        if (startBin == null) {
            if (!force) {
                IRawReadRegion region = headerPage.getReadRegion();
                long changeCount = region.readLong(CHANGE_COUNT_OFFSET);
                long statChangeCount = region.readLong(STAT_CHANGE_COUNT_OFFSET);
                if (statChangeCount != 0 && changeCount - statChangeCount < rebuildThreshold)
                    return null;
            }

            startKey = null;
            i = 0;
        } else {
            startKey = startBin.getKey();
            i = startBin.getValue();
        }

        long binSize = (long) (1 / keyRatio * 100);
        IValueConverter<Long> indexConverter = Indexes.createLongValueConverter();

        long count = getCount();

        for (Pair<ByteArray, V> element : (Iterable<Pair<ByteArray, V>>) find(startKey, true, null, true, false)) {
            if (i % binSize == 0 || i == count - 1) {
                if (i < count - 1 && !batchControl.canContinue())
                    return new Pair<ByteArray, Long>(element.getKey(), i);

                buildingStatistics.add(element.getKey(), indexConverter.toByteArray(i), true);
            }

            i++;
        }

        BTreeIndexSpace<ByteArray, Long> space = currentStatistics;
        currentStatistics = buildingStatistics;
        buildingStatistics = space;

        buildingStatistics.clear();

        IRawWriteRegion region = headerPage.getWriteRegion();
        long changeCount = region.readLong(CHANGE_COUNT_OFFSET);
        region.writeLong(STAT_CHANGE_COUNT_OFFSET, changeCount);
        region.writeInt(CURRENT_STATISTICS_FILE_INDEX_OFFSET, currentStatistics.getFileIndex());
        region.writeInt(BUILDING_STATISTICS_FILE_INDEX_OFFSET, buildingStatistics.getFileIndex());

        return null;
    }

    @Override
    public BTreeIndexStatistics getStatistics() {
        Assert.checkState(!isStale());

        int pageSize = headerPage.getSize() - BTreeNode.HEADER_SIZE;
        IRawReadRegion region = headerPage.getReadRegion();

        int indexHeight = region.readInt(INDEX_HEIGHT_OFFSET);
        long leafDataSize = region.readLong(LEAF_DATA_SIZE_OFFSET);
        long leafNodeCount = region.readLong(LEAF_NODE_COUNT_OFFSET);
        long parentDataSize = region.readLong(PARENT_DATA_SIZE_OFFSET);
        long parentNodeCount = region.readLong(PARENT_NODE_COUNT_OFFSET);
        long leafElementCount = region.readLong(LEAF_ELEMENT_COUNT_OFFSET);
        long parentElementCount = region.readLong(PARENT_ELEMENT_COUNT_OFFSET);

        double leafUsage = (double) leafDataSize / (leafNodeCount * pageSize);
        double parentUsage = (double) parentDataSize / (parentNodeCount * pageSize);
        double totalUsage = (double) (leafDataSize + parentDataSize) /
                ((leafNodeCount + parentNodeCount) * pageSize);
        double elementsPerLeafNode = (double) leafElementCount / leafNodeCount;
        double elementsPerParentNode = (double) parentElementCount / parentNodeCount;

        double averageLeafElementSize = (double) (leafDataSize -
                BTreeNodeOffsetVector.OFFSET_ELEMENT_SIZE * leafElementCount) / leafElementCount;
        double averageParentElementSize = (double) (parentDataSize -
                BTreeNodeOffsetVector.OFFSET_ELEMENT_SIZE * parentElementCount) / parentElementCount;

        return new BTreeIndexStatistics(indexHeight, leafElementCount, parentElementCount, leafNodeCount, parentNodeCount,
                leafNodeCount + parentNodeCount, leafDataSize, parentDataSize, leafDataSize + parentDataSize,
                leafUsage, parentUsage, totalUsage, elementsPerLeafNode, elementsPerParentNode,
                averageLeafElementSize, averageParentElementSize);
    }

    @Override
    public void delete() {
        Assert.checkState(!isStale());
        checkWriteTransaction();

        if (currentStatistics != null)
            currentStatistics.delete();
        if (buildingStatistics != null)
            buildingStatistics.delete();
        if (bloomFilterSpace != null)
            bloomFilterSpace.delete();

        IRawTransaction transaction = transactionProvider.getRawTransaction();
        transaction.getFile(fileIndex).delete();

        super.delete();
    }

    @Override
    public void onTransactionStarted() {
        if (bloomFilterSpace != null)
            bloomFilterSpace.onTransactionStarted();
    }

    @Override
    public void onTransactionCommitted() {
        if (bloomFilterSpace != null)
            bloomFilterSpace.onTransactionCommitted();
    }

    @Override
    public boolean onBeforeTransactionRolledBack() {
        if (bloomFilterSpace != null)
            return bloomFilterSpace.onBeforeTransactionRolledBack();
        else
            return false;
    }

    @Override
    public void onTransactionRolledBack() {
        if (bloomFilterSpace != null)
            bloomFilterSpace.onTransactionRolledBack();
    }

    @Override
    public String printStatistics() {
        return schema.getName() + ":\n" + Strings.indent(getStatistics().toString(), 4);
    }

    @Override
    public String toString() {
        BTreeIndexStatistics statistics = getStatistics();
        IRawReadRegion region = headerPage.getReadRegion();

        String str = MessageFormat.format("b+ tree index({0}\n    " +
                        "free area: {1}, last free: {2}, first leaf: {3}, last leaf: {4})",
                statistics, region.readLong(FREE_AREA_PAGE_INDEX_OFFSET), region.readLong(LAST_FREE_PAGE_INDEX_OFFSET),
                region.readLong(FIRST_LEAF_NODE_PAGE_INDEX_OFFSET), region.readLong(LAST_LEAF_NODE_PAGE_INDEX_OFFSET));

        StringBuilder builder = new StringBuilder(str);
        long rootNodePageIndex = region.readLong(ROOT_NODE_PAGE_INDEX_OFFSET);
        if (rootNodePageIndex != 0)
            builder.append("\n" + Strings.indent(getNode(rootNodePageIndex).toString(0), 4));

        return builder.toString();
    }

    @Override
    public void assertValid() {
        Assert.checkState(!isStale());

        IRawReadRegion region = headerPage.getReadRegion();

        long rootNodePageIndex = region.readLong(ROOT_NODE_PAGE_INDEX_OFFSET);
        long freeAreaPageIndex = region.readLong(FREE_AREA_PAGE_INDEX_OFFSET);
        long lastFreePageIndex = region.readLong(LAST_FREE_PAGE_INDEX_OFFSET);
        long firstLeafPageIndex = region.readLong(FIRST_LEAF_NODE_PAGE_INDEX_OFFSET);
        long lastLeafPageIndex = region.readLong(LAST_LEAF_NODE_PAGE_INDEX_OFFSET);
        long elementCount = region.readLong(LEAF_ELEMENT_COUNT_OFFSET);
        int indexHeight = region.readInt(INDEX_HEIGHT_OFFSET);

        if (rootNodePageIndex == 0) {
            Assert.checkState(freeAreaPageIndex == 1L);
            Assert.checkState(lastFreePageIndex == 0);
            Assert.checkState(firstLeafPageIndex == 0);
            Assert.checkState(lastLeafPageIndex == 0);
            Assert.checkState(elementCount == 0);
            Assert.checkState(indexHeight == 0);
            return;
        }

        Set<Long> leafPages = new HashSet<Long>();
        Set<Long> parentPages = new HashSet<Long>();
        Out<Integer> actualIndexHeight = new Out<Integer>(1);
        Out<Integer> maxIndexHeight = new Out<Integer>(1);
        BTreeNode rootNode = getNode(rootNodePageIndex);
        rootNode.assertValid(null, null, leafPages, parentPages, actualIndexHeight, maxIndexHeight);

        Assert.checkState(maxIndexHeight.value == indexHeight);
        Assert.checkState(rootNode.getPage().getIndex() < freeAreaPageIndex);
        Assert.checkState(firstLeafPageIndex > 0 && firstLeafPageIndex < freeAreaPageIndex);
        Assert.checkState(lastLeafPageIndex > 0 && lastLeafPageIndex < freeAreaPageIndex);

        Set<Long> freePages = new HashSet<Long>();
        IRawTransaction transaction = transactionProvider.getRawTransaction();
        long freePageIndex = lastFreePageIndex;
        while (freePageIndex != 0) {
            freePages.add(freePageIndex);
            IRawPage page = transaction.getPage(fileIndex, freePageIndex);
            Assert.checkState(page.getReadRegion().readShort(0) == FREE_PAGE_MAGIC);
            Assert.checkState(freePageIndex < freeAreaPageIndex);
            freePageIndex = page.getReadRegion().readLong(FREE_PAGE_NEXT_FREE_PAGE_INDEX_OFFSET);
        }

        long actualElementCount = 0;
        Set<Long> listLeafPages = new HashSet<Long>();
        long nextLeafPageIndex = firstLeafPageIndex;
        BTreeLeafNode prevNode = null;
        while (nextLeafPageIndex != 0) {
            Assert.checkState(!freePages.contains(nextLeafPageIndex));
            Assert.checkState(nextLeafPageIndex < freeAreaPageIndex);
            listLeafPages.add(nextLeafPageIndex);
            BTreeLeafNode node = (BTreeLeafNode) getNode(nextLeafPageIndex);
            actualElementCount += node.getElementCount();

            long prevNodePageIndex = node.getPrevNodePageIndex();
            if (prevNode == null)
                Assert.checkState(prevNodePageIndex == 0);
            else {
                Assert.checkState(prevNode.getPage().getIndex() == prevNodePageIndex);

                long nextNodePageIndex = prevNode.getNextNodePageIndex();
                Assert.checkState(nextNodePageIndex != 0);
                Assert.checkState(nextNodePageIndex == node.getPage().getIndex());
            }

            nextLeafPageIndex = node.getNextNodePageIndex();
            prevNode = node;

            if (nextLeafPageIndex == 0)
                Assert.checkState(lastLeafPageIndex == node.getPage().getIndex());
        }

        Assert.checkState(actualElementCount == elementCount);
        Assert.checkState(leafPages.equals(listLeafPages));
        for (Long parentPage : parentPages) {
            Assert.checkState(!freePages.contains(parentPage));
            Assert.checkState(parentPage < freeAreaPageIndex);
        }
    }

    private BTreeIndexSpace(IndexManager indexManager, IndexSchemaConfiguration schema,
                            ITransactionProvider transactionProvider, int fileIndex, boolean fixedKey, int maxKeySize,
                            boolean fixedValue, int maxValueSize, IKeyNormalizer<K> keyNormalizer, IValueConverter<V> valueConverter,
                            IIndexListener<V> listener) {
        super(indexManager, schema, fileIndex);

        Assert.notNull(transactionProvider);
        Assert.notNull(keyNormalizer);
        Assert.notNull(valueConverter);
        Assert.isTrue(fileIndex != 0);

        this.transactionProvider = transactionProvider;
        this.fileIndex = fileIndex;
        this.headerPage = transactionProvider.getRawTransaction().getPage(fileIndex, 0);
        this.fixedKey = fixedKey;
        this.maxKeySize = maxKeySize;
        this.fixedValue = fixedValue;
        this.maxValueSize = maxValueSize;
        this.keyNormalizer = keyNormalizer;
        this.valueConverter = valueConverter;
        this.listener = listener;
    }

    private void readHeader(String filePrefix, Map<String, String> properties) {
        IRawPage page = headerPage;
        IRawReadRegion region = page.getReadRegion();

        short magic = region.readShort(0);
        byte version = region.readByte(VERSION_OFFSET);

        if (magic != MAGIC)
            throw new RawDatabaseException(messages.invalidFormat(fileIndex));
        if (version != Constants.VERSION)
            throw new RawDatabaseException(messages.unsupportedVersion(fileIndex, version, Constants.VERSION));

        int currentStatisticsFileIndex = region.readInt(CURRENT_STATISTICS_FILE_INDEX_OFFSET);
        int buildingStatisticsFileIndex = region.readInt(BUILDING_STATISTICS_FILE_INDEX_OFFSET);
        int bloomFilterSpaceFileIndex = region.readInt(BLOOM_FILTER_SPACE_FILE_INDEX_OFFSET);

        if (currentStatisticsFileIndex != 0) {
            currentStatistics = BTreeIndexSpace.open(indexManager, schema, transactionProvider, currentStatisticsFileIndex,
                    filePrefix + "-stat", fixedKey, maxKeySize, true, 8, Indexes.createByteArrayKeyNormalizer(),
                    Indexes.createLongValueConverter(), null, properties, false);
            buildingStatistics = BTreeIndexSpace.open(indexManager, schema, transactionProvider, buildingStatisticsFileIndex,
                    filePrefix + "-stat", fixedKey, maxKeySize, true, 8, Indexes.createByteArrayKeyNormalizer(),
                    Indexes.createLongValueConverter(), null, properties, false);
        }

        if (bloomFilterSpaceFileIndex != 0)
            bloomFilterSpace = BloomFilterSpace.open(bloomFilterSpaceFileIndex, filePrefix, schema.getPathIndex(),
                    BLOOM_FILTER_PAGE_TYPE, indexManager.getContext(), properties, BLOOM_FILTER_INITIAL_HASH_COUNT);
    }

    private void writeHeader() {
        IRawPage page = headerPage;
        IRawWriteRegion region = page.getWriteRegion();
        region.fill(0, page.getSize(), (byte) 0);

        region.writeShort(0, MAGIC);
        region.writeByte(VERSION_OFFSET, Constants.VERSION);
        region.writeLong(FREE_AREA_PAGE_INDEX_OFFSET, 1l);
        region.writeInt(CURRENT_STATISTICS_FILE_INDEX_OFFSET, currentStatistics != null ? currentStatistics.getFileIndex() : 0);
        region.writeInt(BUILDING_STATISTICS_FILE_INDEX_OFFSET, buildingStatistics != null ? buildingStatistics.getFileIndex() : 0);
        region.writeInt(BLOOM_FILTER_SPACE_FILE_INDEX_OFFSET, bloomFilterSpace != null ? bloomFilterSpace.getFileIndex() : 0);
    }

    private static void bindFile(IRawTransaction transaction, int fileIndex, String filePrefix, int pathIndex,
                                 IndexManager indexManager, Map<String, String> properties) {
        RawBindInfo bindInfo = new RawBindInfo();
        bindInfo.setPathIndex(pathIndex);
        bindInfo.setName(Spaces.getSpaceIndexFileName(filePrefix, fileIndex));

        Pair<String, String> pair = indexManager.getContext().getCacheCategorizationStrategy().categorize(
                new MapBuilder<String, String>(properties)
                        .put("type", "pages.index.btree")
                        .toMap());
        bindInfo.setCategory(pair.getKey());
        bindInfo.setCategoryType(pair.getValue());

        transaction.bindFile(fileIndex, bindInfo);
    }

    private BTreeLeafNode findFirstNode() {
        IRawReadRegion region = headerPage.getReadRegion();

        long firstLeafNodePageIndex = region.readLong(FIRST_LEAF_NODE_PAGE_INDEX_OFFSET);
        if (firstLeafNodePageIndex == 0)
            return null;

        return (BTreeLeafNode) getNode(firstLeafNodePageIndex);
    }

    private BTreeLeafNode findLastNode() {
        IRawReadRegion region = headerPage.getReadRegion();

        long lastLeafNodePageIndex = region.readLong(LAST_LEAF_NODE_PAGE_INDEX_OFFSET);
        if (lastLeafNodePageIndex == 0)
            return null;

        return (BTreeLeafNode) getNode(lastLeafNodePageIndex);
    }

    private BTreeLeafNode findNode(ByteArray key, long nodePageIndex) {
        Assert.isTrue(nodePageIndex != 0);

        BTreeNode node = getNode(nodePageIndex);
        if (node instanceof BTreeParentNode)
            return findNode(key, ((BTreeParentNode) node).find(key, null));
        else
            return (BTreeLeafNode) node;
    }

    private Iterable find(ByteArray fromKey, boolean fromInclusive, ByteArray toKey, boolean toInclusive, boolean valuesOnly) {
        IRawReadRegion region = headerPage.getReadRegion();
        long rootNodePageIndex = region.readLong(ROOT_NODE_PAGE_INDEX_OFFSET);
        if (rootNodePageIndex == 0)
            return Collections.emptyList();

        BTreeLeafNode fromNode, toNode;
        int fromIndex, toIndex;
        if (fromKey != null) {
            fromNode = findNode(fromKey, rootNodePageIndex);
            fromIndex = fromNode.findIndex(fromKey);
            if (fromIndex < 0) {
                fromIndex = -fromIndex - 1;
                fromInclusive = true;
            }
        } else {
            fromNode = findFirstNode();
            fromIndex = 0;
        }

        if (toKey != null) {
            toNode = findNode(toKey, rootNodePageIndex);
            toIndex = toNode.findIndex(toKey);
            if (toIndex < 0) {
                toIndex = -toIndex - 1;
                toInclusive = false;
            }
        } else {
            toNode = findLastNode();
            toIndex = toNode.getElementCount() - 1;
        }

        return new IndexIterable(fromNode, fromIndex, fromInclusive, toNode, toIndex, toInclusive, modCount, valuesOnly);
    }

    private Object findFloor(K key, boolean inclusive, boolean full) {
        IRawReadRegion region = headerPage.getReadRegion();
        long rootNodePageIndex = region.readLong(ROOT_NODE_PAGE_INDEX_OFFSET);
        if (rootNodePageIndex == 0)
            return null;

        BTreeLeafNode node;
        int index;
        if (key != null) {
            ByteArray byteKey = keyNormalizer.normalize(key);
            node = findNode(byteKey, rootNodePageIndex);
            index = node.findIndex(byteKey);
            if (index < 0) {
                index = -index - 1;
                inclusive = false;
            }
        } else {
            node = findLastNode();
            index = node.getElementCount() - 1;
        }

        if (!inclusive)
            index--;

        if (index < 0)
            return null;

        if (full) {
            Pair<ByteArray, ByteArray> element = node.getElement(index);
            return new Pair<ByteArray, V>(element.getKey(), valueConverter.toValue(element.getValue()));
        } else
            return valueConverter.toValue(node.getValue(index));
    }

    private Object findCeiling(K key, boolean inclusive, boolean full) {
        IRawReadRegion region = headerPage.getReadRegion();
        long rootNodePageIndex = region.readLong(ROOT_NODE_PAGE_INDEX_OFFSET);
        if (rootNodePageIndex == 0)
            return null;

        BTreeLeafNode node;
        int index;
        if (key != null) {
            ByteArray byteKey = keyNormalizer.normalize(key);
            node = findNode(byteKey, rootNodePageIndex);
            index = node.findIndex(byteKey);
            if (index < 0) {
                index = -index - 1;
                inclusive = true;
            }
        } else {
            node = findFirstNode();
            index = 0;
        }

        if (!inclusive)
            index++;

        if (index >= node.getElementCount())
            return null;

        if (full) {
            Pair<ByteArray, ByteArray> element = node.getElement(index);
            return new Pair<ByteArray, V>(element.getKey(), valueConverter.toValue(element.getValue()));
        } else
            return valueConverter.toValue(node.getValue(index));
    }

    private void add(ByteArray byteKey, ByteArray byteValue, boolean bulk) {
        Assert.isTrue(!fixedKey || byteKey.getLength() == maxKeySize);
        Assert.isTrue(fixedKey || (byteKey.getLength() >= 0 && byteKey.getLength() <= maxKeySize));
        Assert.isTrue(!fixedValue || byteValue.getLength() == maxValueSize);
        Assert.isTrue(fixedValue || (byteValue.getLength() >= 0 && byteValue.getLength() <= maxValueSize));

        if (bloomFilterSpace != null)
            bloomFilterSpace.add(byteKey);

        IRawWriteRegion region = headerPage.getWriteRegion();
        long rootNodePageIndex = region.readLong(ROOT_NODE_PAGE_INDEX_OFFSET);
        if (rootNodePageIndex == 0) {
            IRawPage page = allocatePage();
            BTreeLeafNode node = BTreeLeafNode.create(this, page);
            node.add(byteKey, byteValue, null, null, bulk);

            region = headerPage.getWriteRegion();
            region.writeLong(ROOT_NODE_PAGE_INDEX_OFFSET, page.getIndex());
            region.writeLong(FIRST_LEAF_NODE_PAGE_INDEX_OFFSET, page.getIndex());
            region.writeLong(LAST_LEAF_NODE_PAGE_INDEX_OFFSET, page.getIndex());
            updateIndexHeight(1);
            return;
        }

        incrementChangeCount(region);
        add(byteKey, byteValue, null, rootNodePageIndex, -1, new ArrayDeque<ContextInfo>(), null, bulk);
    }

    private BTreeParentNode add(ByteArray key, ByteArray value, BTreeParentNode parent, long nodePageIndex, int nodeElementIndex,
                                Deque<ContextInfo> stack, Out<ByteArray> splitKey, boolean bulk) {
        Assert.isTrue(nodePageIndex != 0);

        if (parent != null)
            stack.push(new ContextInfo(parent, nodeElementIndex));

        BTreeNode node = getNode(nodePageIndex);
        BTreeNode newNode;
        Out<ByteArray> childSplitKey = new Out<ByteArray>();
        if (node instanceof BTreeParentNode) {
            BTreeParentNode parentNode = (BTreeParentNode) node;
            Out<Integer> index = new Out<Integer>();
            long childPageIndex;
            if (!bulk)
                childPageIndex = parentNode.find(key, index);
            else {
                childPageIndex = parentNode.getLastChildPageIndex();
                index.value = parentNode.getElementCount();
            }
            newNode = add(key, value, parentNode, childPageIndex, index.value, stack, childSplitKey, bulk);
        } else {
            BTreeLeafNode leafNode = (BTreeLeafNode) node;
            newNode = leafNode.add(key, value, stack, childSplitKey, bulk);
        }

        if (newNode == null) {
            if (parent != null)
                stack.pop();

            return null;
        }

        if (parent == null) {
            IRawPage newPage = allocatePage();
            parent = BTreeParentNode.create(this, newPage, childSplitKey.value, node, newNode);
            headerPage.getWriteRegion().writeLong(ROOT_NODE_PAGE_INDEX_OFFSET, newPage.getIndex());
            updateIndexHeight(1);
            return null;
        }

        stack.pop();

        return parent.add(nodeElementIndex, childSplitKey.value, node.getPage().getIndex(),
                newNode.getPage().getIndex(), stack, splitKey);
    }

    private boolean remove(ByteArray key, BTreeParentNode parent, long nodePageIndex, int nodeElementIndex,
                           Deque<ContextInfo> stack, Out<ByteArray> splitKey) {
        Assert.isTrue(nodePageIndex != 0);

        BTreeNode node = getNode(nodePageIndex);
        if (node instanceof BTreeParentNode) {
            BTreeParentNode parentNode = (BTreeParentNode) node;

            Out<Integer> index = new Out<Integer>();
            long childNodePageIndex = parentNode.find(key, index);

            stack.push(new ContextInfo(parentNode, index.value));
            Out<ByteArray> childSplitKey = new Out<ByteArray>();
            boolean res = remove(key, parentNode, childNodePageIndex, index.value, stack, childSplitKey);
            stack.pop();
            if (!res)
                return false;

            return parentNode.remove(index.value, childSplitKey.value, parent, nodeElementIndex, stack, splitKey);
        } else {
            BTreeLeafNode leafNode = (BTreeLeafNode) node;
            return leafNode.remove(key, parent, nodeElementIndex, stack, splitKey);
        }
    }

    private void incrementChangeCount(IRawWriteRegion region) {
        long changeCount = region.readLong(CHANGE_COUNT_OFFSET);
        region.writeLong(CHANGE_COUNT_OFFSET, changeCount + 1);
        modCount++;
    }

    private BTreeNode loadNode(IRawPage page) {
        short magic = page.getReadRegion().readShort(0);
        if (magic == BTreeParentNode.MAGIC)
            return BTreeParentNode.open(this, page);
        else
            return BTreeLeafNode.open(this, page);
    }

    private void checkWriteTransaction() {
        Assert.checkState((transactionProvider.getTransaction().getOptions() & (IOperation.DELAYED_FLUSH | IOperation.READ_ONLY)) == 0);
    }

    private class IndexIterable implements Iterable {
        private final BTreeLeafNode fromNode;
        private final int fromIndex;
        private final boolean fromInclusive;
        private final BTreeLeafNode toNode;
        private final int toIndex;
        private final boolean toInclusive;
        private final int modCount;
        private final boolean valuesOnly;

        public IndexIterable(BTreeLeafNode fromNode, int fromIndex, boolean fromInclusive, BTreeLeafNode toNode, int toIndex,
                             boolean toInclusive, int modCount, boolean valuesOnly) {
            this.fromNode = fromNode;
            this.fromIndex = fromIndex;
            this.fromInclusive = fromInclusive;
            this.toNode = toNode;
            this.toIndex = toIndex;
            this.toInclusive = toInclusive;
            this.modCount = modCount;
            this.valuesOnly = valuesOnly;
        }

        @Override
        public Iterator iterator() {
            if (!valuesOnly)
                return new IndexIterator(fromNode, fromIndex, fromInclusive, toNode, toIndex, toInclusive, modCount);
            else
                return new IndexValueIterator(fromNode, fromIndex, fromInclusive, toNode, toIndex, toInclusive, modCount);
        }
    }

    private class IndexIterator implements Iterator<Pair<ByteArray, V>> {
        private BTreeLeafNode node;
        private int index;
        private int count;
        private final BTreeLeafNode endNode;
        private final int endIndex;
        private Pair<ByteArray, ByteArray> nextElement;
        private final int modCount;

        public IndexIterator(BTreeLeafNode fromNode, int fromIndex, boolean fromInclusive, BTreeLeafNode toNode, int toIndex,
                             boolean toInclusive, int modCount) {
            Assert.notNull(fromNode);
            Assert.notNull(toNode);

            if (!fromInclusive)
                fromIndex++;
            if (!toInclusive)
                toIndex--;

            this.node = fromNode;
            this.index = fromIndex;
            this.count = fromNode.getElementCount();
            this.endNode = toNode;
            this.endIndex = toIndex;
            this.modCount = modCount;

            nextElement = findNextElement();
        }

        @Override
        public boolean hasNext() {
            return nextElement != null;
        }

        @Override
        public Pair<ByteArray, V> next() {
            Assert.checkState(nextElement != null);
            Assert.checkState(!isStale());

            Pair<ByteArray, ByteArray> res = nextElement;
            nextElement = findNextElement();
            return new Pair<ByteArray, V>(res.getKey(), valueConverter.toValue(res.getValue()));
        }

        @Override
        public void remove() {
            Assert.supports(false);
        }

        private Pair<ByteArray, ByteArray> findNextElement() {
            Assert.checkState(BTreeIndexSpace.this.modCount == modCount);

            if (node == null || index < 0 || (node == endNode && index > endIndex))
                return null;
            if (index >= count) {
                long nextNodePageIndex = node.getNextNodePageIndex();
                if (nextNodePageIndex > 0)
                    node = (BTreeLeafNode) getNode(nextNodePageIndex);
                else {
                    node = null;
                    return null;
                }

                index = 0;
                count = node.getElementCount();

                if (node == endNode && index > endIndex)
                    return null;
            }
            return node.getElement(index++);
        }
    }

    private class IndexValueIterator implements Iterator<V> {
        private BTreeLeafNode node;
        private int index;
        private int count;
        private final BTreeLeafNode endNode;
        private final int endIndex;
        private ByteArray nextElement;
        private final int modCount;

        public IndexValueIterator(BTreeLeafNode fromNode, int fromIndex, boolean fromInclusive, BTreeLeafNode toNode, int toIndex,
                                  boolean toInclusive, int modCount) {
            Assert.notNull(fromNode);
            Assert.notNull(toNode);

            if (!fromInclusive)
                fromIndex++;
            if (!toInclusive)
                toIndex--;

            this.node = fromNode;
            this.index = fromIndex;
            this.count = fromNode.getElementCount();
            this.endNode = toNode;
            this.endIndex = toIndex;
            this.modCount = modCount;

            nextElement = findNextElement();
        }

        @Override
        public boolean hasNext() {
            return nextElement != null;
        }

        @Override
        public V next() {
            Assert.checkState(nextElement != null);
            Assert.checkState(!isStale());

            ByteArray res = nextElement;
            nextElement = findNextElement();
            return valueConverter.toValue(res);
        }

        @Override
        public void remove() {
            Assert.supports(false);
        }

        private ByteArray findNextElement() {
            Assert.checkState(BTreeIndexSpace.this.modCount == modCount);

            if (node == null || (node == endNode && index > endIndex))
                return null;
            if (index >= count) {
                long nextNodePageIndex = node.getNextNodePageIndex();
                if (nextNodePageIndex > 0)
                    node = (BTreeLeafNode) getNode(nextNodePageIndex);
                else {
                    node = null;
                    return null;
                }

                index = 0;
                count = node.getElementCount();

                if (node == endNode && index > endIndex)
                    return null;
            }
            return node.getValue(index++);
        }
    }

    private interface IMessages {
        @DefaultMessage("Invalid format of file ''{0}''.")
        ILocalizedMessage invalidFormat(int fileIndex);

        @DefaultMessage("Unsupported version ''{1}'' of file ''{0}'', expected version - ''{2}''.")
        ILocalizedMessage unsupportedVersion(int fileIndex, int fileVersion, int expectedVersion);
    }
}
