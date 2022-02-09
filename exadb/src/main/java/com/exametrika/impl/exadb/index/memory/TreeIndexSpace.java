/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.index.memory;

import com.exametrika.api.exadb.core.IOperation;
import com.exametrika.api.exadb.core.config.CacheCategoryTypeConfiguration;
import com.exametrika.api.exadb.core.config.DatabaseConfiguration;
import com.exametrika.api.exadb.index.config.schema.TreeIndexSchemaConfiguration;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.rawdb.IRawBatchControl;
import com.exametrika.common.rawdb.IRawPage;
import com.exametrika.common.rawdb.IRawReadRegion;
import com.exametrika.common.rawdb.IRawTransaction;
import com.exametrika.common.rawdb.IRawWriteRegion;
import com.exametrika.common.rawdb.RawBindInfo;
import com.exametrika.common.rawdb.RawDatabaseException;
import com.exametrika.common.rawdb.impl.RawHeapReadRegion;
import com.exametrika.common.rawdb.impl.RawHeapWriteRegion;
import com.exametrika.common.rawdb.impl.RawPageDeserialization;
import com.exametrika.common.rawdb.impl.RawPageSerialization;
import com.exametrika.common.resource.IResourceConsumer;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.CacheSizes;
import com.exametrika.common.utils.MapBuilder;
import com.exametrika.common.utils.Pair;
import com.exametrika.common.utils.Strings;
import com.exametrika.impl.exadb.core.Constants;
import com.exametrika.impl.exadb.core.Spaces;
import com.exametrika.impl.exadb.index.AbstractIndexSpace;
import com.exametrika.impl.exadb.index.IndexManager;
import com.exametrika.spi.exadb.core.IDataFileAllocator;
import com.exametrika.spi.exadb.core.ITransactionProvider;
import com.exametrika.spi.exadb.index.config.schema.IndexSchemaConfiguration;
import com.ibm.icu.text.MessageFormat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * The {@link TreeIndexSpace} is an in-memory tree index implementation.
 *
 * @param <K> key type
 * @param <V> value type
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class TreeIndexSpace<K, V> extends AbstractIndexSpace implements com.exametrika.api.exadb.index.ITreeIndex<K, V>, IResourceConsumer {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(TreeIndexSpace.class);
    private static final short MAGIC = 0x1713;
    private static final int HEADER_SIZE = 48;
    private static final int VERSION_OFFSET = 2;               // magic(short) + version(byte) + padding(byte)
    private static final int FREE_AREA_PAGE_INDEX_OFFSET = 4;  // + freeAreaPageIndex(long) + freeAreaPageOffset(int)
    private static final int FREE_AREA_PAGE_OFFSET_OFFSET = 12;// + dataSize(long) + changeCount(long) + statChangeCount(long)
    private static final int DATA_SIZE_OFFSET = 16;            // + currentStatisticsFileIndex(int) + buildingStatisticsFileIndex(int)
    private static final int CHANGE_COUNT_OFFSET = 24;
    private static final int STAT_CHANGE_COUNT_OFFSET = 32;
    private static final int CURRENT_STATISTICS_FILE_INDEX_OFFSET = 40;
    private static final int BUILDING_STATISTICS_FILE_INDEX_OFFSET = 44;
    private static final byte SERIALIZATION_ADDED = 1;
    private static final byte SERIALIZATION_REMOVED = 2;
    private static final byte SERIALIZATION_END = 0;
    private final ITransactionProvider transactionProvider;
    private final IDataFileAllocator fileAllocator;
    private final int fileIndex;
    private final boolean fixedKey;
    private final int maxKeySize;
    private final boolean fixedValue;
    private final int maxValueSize;
    private final com.exametrika.api.exadb.index.IKeyNormalizer<K> keyNormalizer;
    private final com.exametrika.api.exadb.index.IValueConverter<V> valueConverter;
    private final String filePrefix;
    private final com.exametrika.api.exadb.index.IIndexListener<V> listener;
    private final Map<String, String> properties;
    private final boolean mainIndex;
    private String category;
    private CacheCategoryTypeConfiguration categoryTypeConfiguration;
    private IRawPage headerPage;
    private TreeIndexSpace<ByteArray, Long> currentStatistics;
    private TreeIndexSpace<ByteArray, Long> buildingStatistics;
    private final NavigableMap<ByteArray, ByteArray> elements = new TreeMap<ByteArray, ByteArray>();
    private List<ChangeInfo> changedElements = new ArrayList<ChangeInfo>();
    private volatile long cacheSize;
    private volatile long maxCacheSize;
    private int modCount;

    public static <K, V> TreeIndexSpace create(IndexManager indexManager, IndexSchemaConfiguration schema,
                                               ITransactionProvider transactionProvider, IDataFileAllocator fileAllocator,
                                               int fileIndex, String filePrefix, boolean fixedKey, int maxKeySize, boolean fixedValue, int maxValueSize,
                                               com.exametrika.api.exadb.index.IKeyNormalizer<K> keyNormalizer, com.exametrika.api.exadb.index.IValueConverter<V> valueConverter, boolean createStatistics,
                                               com.exametrika.api.exadb.index.IIndexListener<V> listener, Map<String, String> properties) {
        Assert.notNull(indexManager);
        Assert.notNull(schema);
        Assert.notNull(properties);
        Assert.notNull(transactionProvider);
        Assert.isTrue(maxKeySize <= Constants.PAGE_SIZE / 16);
        Assert.isTrue(maxValueSize <= Constants.PAGE_SIZE / 16);

        IRawTransaction transaction = transactionProvider.getRawTransaction();
        bindFile(transaction, fileIndex, filePrefix, schema.getPathIndex(), indexManager, properties);

        TreeIndexSpace<ByteArray, Long> firstStatistics = null;
        TreeIndexSpace<ByteArray, Long> secondStatistics = null;
        if (createStatistics && ((TreeIndexSchemaConfiguration) schema).isSorted()) {
            Assert.notNull(fileAllocator);

            firstStatistics = TreeIndexSpace.create(indexManager, schema, transactionProvider, fileAllocator,
                    fileAllocator.allocateFile(transactionProvider.getRawTransaction()),
                    filePrefix + "-stat", fixedKey, maxKeySize, true, 8, com.exametrika.api.exadb.index.Indexes.createByteArrayKeyNormalizer(),
                    com.exametrika.api.exadb.index.Indexes.createLongValueConverter(), false, null, properties);
            secondStatistics = TreeIndexSpace.create(indexManager, schema, transactionProvider, fileAllocator,
                    fileAllocator.allocateFile(transactionProvider.getRawTransaction()),
                    filePrefix + "-stat", fixedKey, maxKeySize, true, 8, com.exametrika.api.exadb.index.Indexes.createByteArrayKeyNormalizer(),
                    com.exametrika.api.exadb.index.Indexes.createLongValueConverter(), false, null, properties);
        }

        TreeIndexSpace index = new TreeIndexSpace(createStatistics ? indexManager : null, schema,
                transactionProvider, fileAllocator, fileIndex, fixedKey,
                maxKeySize, fixedValue, maxValueSize, keyNormalizer, valueConverter, filePrefix, listener, properties, createStatistics);
        index.currentStatistics = firstStatistics;
        index.buildingStatistics = secondStatistics;
        index.writeHeader();

        return index;
    }

    public static <K, V> TreeIndexSpace open(IndexManager indexManager, IndexSchemaConfiguration schema,
                                             ITransactionProvider transactionProvider, IDataFileAllocator fileAllocator, int fileIndex, String filePrefix,
                                             boolean fixedKey, int maxKeySize, boolean fixedValue, int maxValueSize,
                                             com.exametrika.api.exadb.index.IKeyNormalizer<K> keyNormalizer, com.exametrika.api.exadb.index.IValueConverter<V> valueConverter, com.exametrika.api.exadb.index.IIndexListener<V> listener, Map<String, String> properties,
                                             boolean mainIndex) {
        Assert.notNull(indexManager);
        Assert.notNull(schema);
        Assert.notNull(properties);
        Assert.notNull(transactionProvider);
        Assert.isTrue(maxKeySize <= Constants.PAGE_SIZE / 16);
        Assert.isTrue(maxValueSize <= Constants.PAGE_SIZE / 16);

        IRawTransaction transaction = transactionProvider.getRawTransaction();
        bindFile(transaction, fileIndex, filePrefix, schema.getPathIndex(), indexManager, properties);

        TreeIndexSpace index = new TreeIndexSpace(mainIndex ? indexManager : null, schema,
                transactionProvider, fileAllocator, fileIndex, fixedKey,
                maxKeySize, fixedValue, maxValueSize, keyNormalizer, valueConverter, filePrefix, listener, properties, mainIndex);
        index.readHeader(filePrefix);

        return index;
    }

    public int getFileIndex() {
        return fileIndex;
    }

    public com.exametrika.api.exadb.index.IKeyNormalizer<K> getKeyNormalizer() {
        return keyNormalizer;
    }

    public com.exametrika.api.exadb.index.IValueConverter<V> getValueConverter() {
        return valueConverter;
    }

    public <T extends IRawReadRegion> T findValueRegion(K key, boolean readOnly) {
        Assert.notNull(key);

        ByteArray byteKey = keyNormalizer.normalize(key);
        ByteArray byteValue = elements.get(byteKey);
        if (byteValue == null)
            return null;

        if (!readOnly) {
            checkWriteTransaction();

            changedElements.add(new ChangeInfo(byteKey, byteValue.clone(), false));
            changedElements.add(new ChangeInfo(byteKey, byteValue, true));
            onModified();

            return (T) new RawHeapWriteRegion(byteValue.getBuffer(), byteValue.getOffset(), byteValue.getLength());
        } else
            return (T) new RawHeapReadRegion(byteValue.getBuffer(), byteValue.getOffset(), byteValue.getLength());
    }

    @Override
    public long getAmount() {
        return cacheSize;
    }

    @Override
    public long getQuota() {
        return maxCacheSize;
    }

    @Override
    public void setQuota(long value) {
        maxCacheSize = value;

        if (mainIndex && maxCacheSize < cacheSize) {
            if (logger.isLogEnabled(LogLevel.WARNING))
                logger.log(LogLevel.WARNING, messages.quotaTooSmall(maxCacheSize, cacheSize));
        }
    }

    @Override
    public boolean isEmpty() {
        Assert.checkState(!isStale());

        return elements.isEmpty();
    }

    @Override
    public long getCount() {
        Assert.checkState(!isStale());

        return elements.size();
    }

    @Override
    public V find(K key) {
        Assert.notNull(key);
        Assert.checkState(!isStale());

        ByteArray byteKey = keyNormalizer.normalize(key);
        ByteArray byteValue = elements.get(byteKey);

        if (byteValue != null)
            return valueConverter.toValue(byteValue);
        else
            return null;
    }

    @Override
    public void add(K key, V value) {
        Assert.notNull(key);
        Assert.notNull(value);
        Assert.checkState(!isStale());
        checkWriteTransaction();

        ByteArray byteKey = keyNormalizer.normalize(key);
        ByteArray byteValue = valueConverter.toByteArray(value);
        add(byteKey, byteValue);

        onModified();
    }

    @Override
    public void remove(K key) {
        Assert.notNull(key);
        Assert.checkState(!isStale());
        checkWriteTransaction();

        ByteArray byteKey = keyNormalizer.normalize(key);

        ByteArray byteValue = elements.remove(byteKey);
        if (byteValue != null) {
            if (listener != null)
                listener.onRemoved(valueConverter.toValue(byteValue));

            changedElements.add(new ChangeInfo(byteKey, byteValue, false));

            onModified();

            if (mainIndex)
                updateCacheSize(byteKey, byteValue, false);
        }
    }

    @Override
    public void clear() {
        Assert.checkState(!isStale());
        checkWriteTransaction();

        if (listener != null)
            listener.onCleared();

        for (Map.Entry<ByteArray, ByteArray> entry : elements.entrySet())
            changedElements.add(new ChangeInfo(entry.getKey(), entry.getValue(), false));

        elements.clear();
        cacheSize = 0;

        onModified();
    }

    @Override
    public ByteArray normalize(K key) {
        Assert.notNull(key);
        Assert.checkState(!isStale());

        return keyNormalizer.normalize(key);
    }

    @Override
    public com.exametrika.api.exadb.index.TreeIndexStatistics getStatistics() {
        Assert.checkState(!isStale());

        int pageSize = headerPage.getSize();
        IRawReadRegion region = headerPage.getReadRegion();

        long dataSize = region.readLong(DATA_SIZE_OFFSET);
        long pageIndex = region.readLong(FREE_AREA_PAGE_INDEX_OFFSET);
        int pageOffset = region.readInt(FREE_AREA_PAGE_OFFSET_OFFSET);
        long elementCount = elements.size();

        double usage = (double) dataSize / (pageIndex * pageSize + pageOffset);
        double elementsPerPage = (double) elementCount / (pageIndex + 1);

        double averageElementSize = (double) dataSize / elementCount;

        return new com.exametrika.api.exadb.index.TreeIndexStatistics(elementCount, pageIndex + 1, dataSize, usage, elementsPerPage, averageElementSize);
    }

    @Override
    public Pair<ByteArray, V> findFirst() {
        Assert.checkState(!isStale());

        if (elements.isEmpty())
            return null;

        Map.Entry<ByteArray, ByteArray> entry = elements.firstEntry();
        return new Pair<ByteArray, V>(entry.getKey(), valueConverter.toValue(entry.getValue()));
    }

    @Override
    public V findFirstValue() {
        Assert.checkState(!isStale());

        if (elements.isEmpty())
            return null;

        Map.Entry<ByteArray, ByteArray> entry = elements.firstEntry();
        return valueConverter.toValue(entry.getValue());
    }

    @Override
    public Pair<ByteArray, V> findLast() {
        Assert.checkState(!isStale());

        if (elements.isEmpty())
            return null;

        Map.Entry<ByteArray, ByteArray> entry = elements.lastEntry();
        return new Pair<ByteArray, V>(entry.getKey(), valueConverter.toValue(entry.getValue()));
    }

    @Override
    public V findLastValue() {
        Assert.checkState(!isStale());

        if (elements.isEmpty())
            return null;

        Map.Entry<ByteArray, ByteArray> entry = elements.lastEntry();
        return valueConverter.toValue(entry.getValue());
    }

    @Override
    public Pair<ByteArray, V> findFloor(K key, boolean inclusive) {
        Assert.checkState(!isStale());

        if (elements.isEmpty())
            return null;

        ByteArray byteKey;
        if (key != null)
            byteKey = keyNormalizer.normalize(key);
        else
            byteKey = elements.lastKey();

        Map.Entry<ByteArray, ByteArray> entry;
        if (inclusive)
            entry = elements.floorEntry(byteKey);
        else
            entry = elements.lowerEntry(byteKey);

        if (entry != null)
            return new Pair<ByteArray, V>(entry.getKey(), valueConverter.toValue(entry.getValue()));
        else
            return null;
    }

    @Override
    public V findFloorValue(K key, boolean inclusive) {
        Assert.checkState(!isStale());

        if (elements.isEmpty())
            return null;

        ByteArray byteKey;
        if (key != null)
            byteKey = keyNormalizer.normalize(key);
        else
            byteKey = elements.lastKey();

        Map.Entry<ByteArray, ByteArray> entry;
        if (inclusive)
            entry = elements.floorEntry(byteKey);
        else
            entry = elements.lowerEntry(byteKey);

        if (entry != null)
            return valueConverter.toValue(entry.getValue());
        else
            return null;
    }

    @Override
    public Pair<ByteArray, V> findCeiling(K key, boolean inclusive) {
        Assert.checkState(!isStale());

        if (elements.isEmpty())
            return null;

        ByteArray byteKey;
        if (key != null)
            byteKey = keyNormalizer.normalize(key);
        else
            byteKey = elements.firstKey();

        Map.Entry<ByteArray, ByteArray> entry;
        if (inclusive)
            entry = elements.ceilingEntry(byteKey);
        else
            entry = elements.higherEntry(byteKey);

        if (entry != null)
            return new Pair<ByteArray, V>(entry.getKey(), valueConverter.toValue(entry.getValue()));
        else
            return null;
    }

    @Override
    public V findCeilingValue(K key, boolean inclusive) {
        Assert.checkState(!isStale());

        if (elements.isEmpty())
            return null;

        ByteArray byteKey;
        if (key != null)
            byteKey = keyNormalizer.normalize(key);
        else
            byteKey = elements.firstKey();

        Map.Entry<ByteArray, ByteArray> entry;
        if (inclusive)
            entry = elements.ceilingEntry(byteKey);
        else
            entry = elements.higherEntry(byteKey);

        if (entry != null)
            return valueConverter.toValue(entry.getValue());
        else
            return null;
    }

    @Override
    public Iterable<Pair<ByteArray, V>> find(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
        Assert.checkState(!isStale());

        if (elements.isEmpty())
            return Collections.emptyList();

        ByteArray from = null;
        if (fromKey != null)
            from = keyNormalizer.normalize(fromKey);
        else
            from = elements.firstKey();

        ByteArray to = null;
        if (toKey != null)
            to = keyNormalizer.normalize(toKey);
        else
            to = elements.lastKey();

        NavigableMap<ByteArray, ByteArray> map = elements.subMap(from, fromInclusive, to, toInclusive);
        return new IndexIterable(map, modCount, false);
    }

    @Override
    public Iterable<V> findValues(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive) {
        Assert.checkState(!isStale());

        if (elements.isEmpty())
            return Collections.emptyList();

        ByteArray from = null;
        if (fromKey != null)
            from = keyNormalizer.normalize(fromKey);
        else
            from = elements.firstKey();

        ByteArray to = null;
        if (toKey != null)
            to = keyNormalizer.normalize(toKey);
        else
            to = elements.lastKey();

        NavigableMap<ByteArray, ByteArray> map = elements.subMap(from, fromInclusive, to, toInclusive);
        return new IndexIterable(map, modCount, true);
    }

    @Override
    public void bulkAdd(Iterable<Pair<K, V>> elements) {
        Assert.notNull(elements);
        Assert.checkState(!isStale());
        checkWriteTransaction();

        for (Pair<K, V> element : elements) {
            ByteArray byteKey = keyNormalizer.normalize(element.getKey());
            ByteArray byteValue = valueConverter.toByteArray(element.getValue());

            add(byteKey, byteValue);
        }

        onModified();
    }

    @Override
    public void delete() {
        Assert.checkState(!isStale());
        checkWriteTransaction();

        if (currentStatistics != null)
            currentStatistics.delete();
        if (buildingStatistics != null)
            buildingStatistics.delete();

        IRawTransaction transaction = transactionProvider.getRawTransaction();
        transaction.getFile(fileIndex).delete();

        unload(true);

        super.delete();
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

        if (elements.isEmpty())
            return null;

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

            startKey = elements.firstKey();
            i = 0;
        } else {
            startKey = startBin.getKey();
            i = startBin.getValue();
        }

        long binSize = (long) (1 / keyRatio * 100);

        long count = getCount();

        for (ByteArray key : elements.tailMap(startKey).keySet()) {
            if (i % binSize == 0 || i == count - 1) {
                if (i < count - 1 && !batchControl.canContinue()) {
                    onModified();
                    return new Pair<ByteArray, Long>(key, i);
                }

                buildingStatistics.add(key, i);
            }

            i++;
        }

        TreeIndexSpace<ByteArray, Long> space = currentStatistics;
        currentStatistics = buildingStatistics;
        buildingStatistics = space;

        buildingStatistics.clear();

        IRawWriteRegion region = headerPage.getWriteRegion();
        long changeCount = region.readLong(CHANGE_COUNT_OFFSET);
        region.writeLong(STAT_CHANGE_COUNT_OFFSET, changeCount);
        region.writeInt(CURRENT_STATISTICS_FILE_INDEX_OFFSET, currentStatistics.getFileIndex());
        region.writeInt(BUILDING_STATISTICS_FILE_INDEX_OFFSET, buildingStatistics.getFileIndex());

        onModified();

        return null;
    }

    public Pair<ByteArray, TreeIndexSpace<K, V>> compact(IRawBatchControl batchControl, Pair<ByteArray, TreeIndexSpace<K, V>> start,
                                                         long compactionThreshold, boolean force) {
        Assert.notNull(batchControl);
        Assert.checkState(!isStale());
        checkWriteTransaction();

        if (elements.isEmpty())
            return null;

        if (start == null) {
            if (!force) {
                com.exametrika.api.exadb.index.TreeIndexStatistics statistics = getStatistics();
                if (statistics.getUsage() * 100 > compactionThreshold)
                    return null;
            }

            start = new Pair<ByteArray, TreeIndexSpace<K, V>>(elements.firstKey(), TreeIndexSpace.create(indexManager,
                    schema, transactionProvider, fileAllocator, fileAllocator.allocateFile(transactionProvider.getRawTransaction()),
                    filePrefix, fixedKey, maxKeySize, fixedValue, maxValueSize, keyNormalizer, valueConverter, true, listener, properties));
        }

        TreeIndexSpace<K, V> index = start.getValue();

        Pair<ByteArray, TreeIndexSpace<K, V>> res = null;

        for (Map.Entry<ByteArray, ByteArray> entry : elements.tailMap(start.getKey()).entrySet()) {
            if (!batchControl.canContinue()) {
                res = new Pair<ByteArray, TreeIndexSpace<K, V>>(entry.getKey(), index);
                break;
            }

            index.add(entry.getKey(), entry.getValue());
        }

        index.onModified();
        index.onTransactionCommitted();

        if (res != null)
            return res;
        else
            return new Pair<ByteArray, TreeIndexSpace<K, V>>(null, index);
    }

    @Override
    public void onTransactionCommitted() {
        IRawPage page = headerPage;
        IRawWriteRegion region = page.getWriteRegion();

        if (elements.isEmpty()) {
            if (listener != null)
                listener.onCleared();

            writeHeader();
            transactionProvider.getRawTransaction().getFile(fileIndex).truncate(page.getSize());

            if (currentStatistics != null)
                currentStatistics.clear();
            if (buildingStatistics != null)
                buildingStatistics.clear();

            changedElements = new ArrayList<ChangeInfo>();

            region.writeLong(DATA_SIZE_OFFSET, 0);

            long changeCount = region.readLong(CHANGE_COUNT_OFFSET);
            region.writeLong(CHANGE_COUNT_OFFSET, changeCount + 1);
        } else if (!changedElements.isEmpty()) {
            long dataSize = region.readLong(DATA_SIZE_OFFSET);

            RawPageSerialization serialization = new RawPageSerialization(transactionProvider.getRawTransaction(), fileIndex,
                    region.readLong(FREE_AREA_PAGE_INDEX_OFFSET), region.readInt(FREE_AREA_PAGE_OFFSET_OFFSET));

            for (ChangeInfo info : changedElements) {
                if (info.added) {
                    serialization.writeByte(SERIALIZATION_ADDED);
                    serialization.writeByteArray(info.key);
                    serialization.writeByteArray(info.value);
                    dataSize += info.key.getLength() + info.value.getLength();
                } else {
                    serialization.writeByte(SERIALIZATION_REMOVED);
                    serialization.writeByteArray(info.key);
                    dataSize -= info.key.getLength() + info.value.getLength();
                }
            }

            changedElements.clear();

            region = headerPage.getWriteRegion();
            region.writeLong(FREE_AREA_PAGE_INDEX_OFFSET, serialization.getPageIndex());
            region.writeLong(FREE_AREA_PAGE_OFFSET_OFFSET, serialization.getPageOffset());
            region.writeLong(DATA_SIZE_OFFSET, dataSize);
            long changeCount = region.readLong(CHANGE_COUNT_OFFSET);
            region.writeLong(CHANGE_COUNT_OFFSET, changeCount + 1);

            serialization.writeByte(SERIALIZATION_END);
        }

        if (currentStatistics != null)
            currentStatistics.onTransactionCommitted();
        if (buildingStatistics != null)
            buildingStatistics.onTransactionCommitted();

        super.onTransactionCommitted();
    }

    @Override
    public void onTransactionRolledBack() {
        if (!changedElements.isEmpty()) {
            for (int i = changedElements.size() - 1; i >= 0; i--) {
                ChangeInfo info = changedElements.get(i);
                if (info.added) {
                    ByteArray value = elements.remove(info.key);
                    if (mainIndex)
                        updateCacheSize(info.key, value, false);
                } else {
                    elements.put(info.key, info.value);
                    if (mainIndex)
                        updateCacheSize(info.key, info.value, true);
                }
            }

            changedElements.clear();

            if (mainIndex && maxCacheSize < cacheSize) {
                if (logger.isLogEnabled(LogLevel.WARNING))
                    logger.log(LogLevel.WARNING, messages.quotaTooSmall(maxCacheSize, cacheSize));
            }
        }

        if (currentStatistics != null)
            currentStatistics.onTransactionRolledBack();
        if (buildingStatistics != null)
            buildingStatistics.onTransactionRolledBack();

        super.onTransactionRolledBack();
    }

    @Override
    public void unload(boolean full) {
        if (full) {
            if (currentStatistics != null)
                currentStatistics.unload(full);
            if (buildingStatistics != null)
                buildingStatistics.unload(full);
            elements.clear();
            changedElements.clear();

            if (mainIndex) {
                indexManager.getContext().getResourceAllocator().unregister(getResourceConsumerName());
                cacheSize = 0;
                setQuota(categoryTypeConfiguration.getInitialCacheSize());
            }
        }
    }

    @Override
    public CacheCategoryTypeConfiguration getCategoryTypeConfiguration() {
        return categoryTypeConfiguration;
    }

    @Override
    public void setCategoryTypeConfiguration(CacheCategoryTypeConfiguration configuration) {
        Assert.notNull(configuration);

        this.categoryTypeConfiguration = configuration;
    }

    @Override
    public String printStatistics() {
        return schema.getName() + ":\n" + Strings.indent(getStatistics().toString(), 4) +
                (mainIndex ? ("\n" + Strings.indent(messages.cacheStatistics(category, categoryTypeConfiguration.getName(),
                        maxCacheSize, cacheSize).toString(), 4)) : "");
    }

    @Override
    public String toString() {
        com.exametrika.api.exadb.index.TreeIndexStatistics statistics = getStatistics();
        return MessageFormat.format("tree index ''{0}''({1})", schema.getAlias(), statistics);
    }

    private TreeIndexSpace(IndexManager indexManager, IndexSchemaConfiguration schema,
                           ITransactionProvider transactionProvider, IDataFileAllocator fileAllocator, int fileIndex,
                           boolean fixedKey, int maxKeySize, boolean fixedValue, int maxValueSize,
                           com.exametrika.api.exadb.index.IKeyNormalizer<K> keyNormalizer, com.exametrika.api.exadb.index.IValueConverter<V> valueConverter, String filePrefix, com.exametrika.api.exadb.index.IIndexListener<V> listener,
                           Map<String, String> properties, boolean mainIndex) {
        super(indexManager, schema, fileIndex);

        Assert.notNull(transactionProvider);
        Assert.notNull(fileAllocator);
        Assert.notNull(keyNormalizer);
        Assert.notNull(valueConverter);
        Assert.isTrue(fileIndex != 0);

        this.transactionProvider = transactionProvider;
        this.fileAllocator = fileAllocator;
        this.fileIndex = fileIndex;
        this.headerPage = transactionProvider.getRawTransaction().getPage(fileIndex, 0);
        this.fixedKey = fixedKey;
        this.maxKeySize = maxKeySize;
        this.fixedValue = fixedValue;
        this.maxValueSize = maxValueSize;
        this.keyNormalizer = keyNormalizer;
        this.valueConverter = valueConverter;
        this.filePrefix = filePrefix;
        this.listener = listener;
        this.properties = properties;
        this.mainIndex = mainIndex;

        if (mainIndex) {
            Pair<String, String> pair = indexManager.getContext().getCacheCategorizationStrategy().categorize(new MapBuilder<String, String>(properties)
                    .put("type", "indexes.tree")
                    .toMap());
            DatabaseConfiguration databaseConfiguration = indexManager.getContext().getConfiguration();

            this.category = pair.getKey();
            if (pair.getValue().isEmpty())
                categoryTypeConfiguration = databaseConfiguration.getDefaultCacheCategoryType();
            else {
                categoryTypeConfiguration = databaseConfiguration.getCacheCategoryTypes().get(pair.getValue());
                Assert.checkState(categoryTypeConfiguration != null);
            }

            this.maxCacheSize = categoryTypeConfiguration.getInitialCacheSize();

            indexManager.getContext().getResourceAllocator().register(getResourceConsumerName(), this);
        }
    }

    private void readHeader(String filePrefix) {
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

        if (currentStatisticsFileIndex != 0) {
            currentStatistics = TreeIndexSpace.open(indexManager, schema, transactionProvider, fileAllocator,
                    currentStatisticsFileIndex, filePrefix + "-stat", fixedKey, maxKeySize, fixedValue, maxValueSize,
                    com.exametrika.api.exadb.index.Indexes.createByteArrayKeyNormalizer(), com.exametrika.api.exadb.index.Indexes.createLongValueConverter(), null, properties, false);
            buildingStatistics = TreeIndexSpace.open(indexManager, schema, transactionProvider, fileAllocator,
                    buildingStatisticsFileIndex, filePrefix + "-stat", fixedKey, maxKeySize, fixedValue, maxValueSize,
                    com.exametrika.api.exadb.index.Indexes.createByteArrayKeyNormalizer(), com.exametrika.api.exadb.index.Indexes.createLongValueConverter(), null, properties, false);
        }

        RawPageDeserialization deserialization = new RawPageDeserialization(transactionProvider.getRawTransaction(), fileIndex,
                0, HEADER_SIZE);

        while (true) {
            byte type = deserialization.readByte();
            if (type == SERIALIZATION_END)
                break;
            else if (type == SERIALIZATION_ADDED) {
                ByteArray key = deserialization.readByteArray();
                ByteArray value = deserialization.readByteArray();
                elements.put(key, value);
                if (mainIndex)
                    updateCacheSize(key, value, true);
            } else if (type == SERIALIZATION_REMOVED) {
                ByteArray key = deserialization.readByteArray();
                ByteArray value = elements.remove(key);
                if (mainIndex)
                    updateCacheSize(key, value, false);
            } else
                Assert.error();
        }

        if (mainIndex && maxCacheSize < cacheSize) {
            if (logger.isLogEnabled(LogLevel.WARNING))
                logger.log(LogLevel.WARNING, messages.quotaTooSmall(maxCacheSize, cacheSize));
        }
    }

    private void writeHeader() {
        IRawPage page = headerPage;
        IRawWriteRegion region = page.getWriteRegion();
        region.fill(0, page.getSize(), (byte) 0);

        region.writeShort(0, MAGIC);
        region.writeByte(VERSION_OFFSET, Constants.VERSION);
        region.writeInt(FREE_AREA_PAGE_OFFSET_OFFSET, HEADER_SIZE);
        region.writeInt(CURRENT_STATISTICS_FILE_INDEX_OFFSET, currentStatistics != null ? currentStatistics.getFileIndex() : 0);
        region.writeInt(BUILDING_STATISTICS_FILE_INDEX_OFFSET, buildingStatistics != null ? buildingStatistics.getFileIndex() : 0);
    }

    private static void bindFile(IRawTransaction transaction, int fileIndex, String filePrefix, int pathIndex,
                                 IndexManager indexManager, Map<String, String> properties) {
        RawBindInfo bindInfo = new RawBindInfo();
        bindInfo.setPathIndex(pathIndex);
        bindInfo.setName(Spaces.getSpaceIndexFileName(filePrefix, fileIndex));

        Pair<String, String> pair = indexManager.getContext().getCacheCategorizationStrategy().categorize(
                new MapBuilder<String, String>(properties)
                        .put("type", "pages.index.tree")
                        .toMap());
        bindInfo.setCategory(pair.getKey());
        bindInfo.setCategoryType(pair.getValue());

        transaction.bindFile(fileIndex, bindInfo);
    }

    private void add(ByteArray byteKey, ByteArray byteValue) {
        Assert.isTrue(!fixedKey || byteKey.getLength() == maxKeySize);
        Assert.isTrue(fixedKey || (byteKey.getLength() >= 0 && byteKey.getLength() <= maxKeySize));
        Assert.isTrue(!fixedValue || byteValue.getLength() == maxValueSize);
        Assert.isTrue(fixedValue || (byteValue.getLength() >= 0 && byteValue.getLength() <= maxValueSize));

        ByteArray oldValue = elements.put(byteKey, byteValue);
        if (oldValue != null) {
            changedElements.add(new ChangeInfo(byteKey, oldValue, false));
            if (mainIndex)
                updateCacheSize(byteKey, oldValue, false);
        }

        changedElements.add(new ChangeInfo(byteKey, byteValue, true));

        if (mainIndex) {
            updateCacheSize(byteKey, byteValue, true);

            if (maxCacheSize < cacheSize) {
                if (logger.isLogEnabled(LogLevel.WARNING))
                    logger.log(LogLevel.WARNING, messages.quotaTooSmall(maxCacheSize, cacheSize));
            }
        }
    }

    private void onModified() {
        modCount++;
    }

    private void updateCacheSize(ByteArray key, ByteArray value, boolean increment) {
        int entryCacheSize = CacheSizes.TREE_MAP_ENTRY_CACHE_SIZE +
                CacheSizes.getByteArrayCacheSize(key) + CacheSizes.getByteArrayCacheSize(value);

        if (increment)
            cacheSize += entryCacheSize;
        else
            cacheSize -= entryCacheSize;
    }

    private void checkWriteTransaction() {
        Assert.checkState((transactionProvider.getTransaction().getOptions() & (IOperation.DELAYED_FLUSH | IOperation.READ_ONLY)) == 0);
    }

    private String getResourceConsumerName() {
        return "heap.indexes." + category + "." + schema.getName() + "-" + fileIndex;
    }

    private class IndexIterable implements Iterable {
        private final NavigableMap<ByteArray, ByteArray> map;
        private final int modCount;
        private final boolean valuesOnly;

        public IndexIterable(NavigableMap<ByteArray, ByteArray> map, int modCount, boolean valuesOnly) {
            this.map = map;
            this.modCount = modCount;
            this.valuesOnly = valuesOnly;
        }

        @Override
        public Iterator iterator() {
            if (!valuesOnly)
                return new IndexIterator(map.entrySet().iterator(), modCount);
            else
                return new IndexValueIterator(map.entrySet().iterator(), modCount);
        }
    }

    private class IndexIterator implements Iterator<Pair<ByteArray, V>> {
        private final Iterator<Map.Entry<ByteArray, ByteArray>> it;
        private final int modCount;

        public IndexIterator(Iterator<Map.Entry<ByteArray, ByteArray>> it, int modCount) {
            Assert.notNull(it);

            this.it = it;
            this.modCount = modCount;
        }

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public Pair<ByteArray, V> next() {
            Assert.checkState(TreeIndexSpace.this.modCount == modCount);
            Assert.checkState(!isStale());

            Map.Entry<ByteArray, ByteArray> res = it.next();
            return new Pair<ByteArray, V>(res.getKey(), valueConverter.toValue(res.getValue()));
        }

        @Override
        public void remove() {
            Assert.supports(false);
        }
    }

    private class IndexValueIterator implements Iterator<V> {
        private final Iterator<Map.Entry<ByteArray, ByteArray>> it;
        private final int modCount;

        public IndexValueIterator(Iterator<Map.Entry<ByteArray, ByteArray>> it, int modCount) {
            Assert.notNull(it);

            this.it = it;
            this.modCount = modCount;
        }

        @Override
        public boolean hasNext() {
            return it.hasNext();
        }

        @Override
        public V next() {
            Assert.checkState(TreeIndexSpace.this.modCount == modCount);
            Assert.checkState(!isStale());

            Map.Entry<ByteArray, ByteArray> res = it.next();
            return valueConverter.toValue(res.getValue());
        }

        @Override
        public void remove() {
            Assert.supports(false);
        }
    }

    private static class ChangeInfo {
        private final ByteArray key;
        private final ByteArray value;
        private final boolean added;

        public ChangeInfo(ByteArray key, ByteArray value, boolean added) {
            this.key = key;
            this.value = value;
            this.added = added;
        }
    }

    private interface IMessages {
        @DefaultMessage("Invalid format of file ''{0}''.")
        ILocalizedMessage invalidFormat(int fileIndex);

        @DefaultMessage("Unsupported version ''{1}'' of file ''{0}'', expected version - ''{2}''.")
        ILocalizedMessage unsupportedVersion(int fileIndex, int fileVersion, int expectedVersion);

        @DefaultMessage("index cache ''{0}:{1}'' - max cache size: {2}, cache size: {3}")
        ILocalizedMessage cacheStatistics(String category, String categoryType, long maxCacheSize, long cacheSize);

        @DefaultMessage("Specified maximum cache size ''{0}'' is too small. Decreasing current cache size ''{1}'' is not supported.")
        ILocalizedMessage quotaTooSmall(long maxCacheSize, long cacheSize);
    }
}
