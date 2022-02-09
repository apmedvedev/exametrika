/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.index.memory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import sun.misc.Unsafe;

import com.exametrika.api.exadb.core.IOperation;
import com.exametrika.api.exadb.core.config.CacheCategoryTypeConfiguration;
import com.exametrika.api.exadb.core.config.DatabaseConfiguration;
import com.exametrika.api.exadb.index.HashIndexStatistics;
import com.exametrika.api.exadb.index.IHashIndex;
import com.exametrika.api.exadb.index.IIndexListener;
import com.exametrika.api.exadb.index.IKeyNormalizer;
import com.exametrika.api.exadb.index.IValueConverter;
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

/**
 * The {@link HashIndexSpace} is an in-memory hash index implementation.
 *
 * @param <K> key type
 * @param <V> value type
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class HashIndexSpace<K, V> extends AbstractIndexSpace implements IHashIndex<K, V>, IResourceConsumer {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(HashIndexSpace.class);
    private static final short MAGIC = 0x1714;
    private static final int HEADER_SIZE = 24;
    private static final int VERSION_OFFSET = 2;               // magic(short) + version(byte) + padding(byte)
    private static final int FREE_AREA_PAGE_INDEX_OFFSET = 4;  // + freeAreaPageIndex(long) + freeAreaPageOffset(int)
    private static final int FREE_AREA_PAGE_OFFSET_OFFSET = 12;// + dataSize(long)
    private static final int DATA_SIZE_OFFSET = 16;
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
    private final IKeyNormalizer<K> keyNormalizer;
    private final IValueConverter<V> valueConverter;
    private final String filePrefix;
    private final IIndexListener<V> listener;
    private final Map<String, String> properties;
    private String category;
    private CacheCategoryTypeConfiguration categoryTypeConfiguration;
    private IRawPage headerPage;
    private final Map<ByteArray, ByteArray> elements = new LinkedHashMap<ByteArray, ByteArray>();
    private List<ChangeInfo> changedElements = new ArrayList<ChangeInfo>();
    private volatile long cacheSize;
    private volatile long maxCacheSize;

    public static class CompactionInfo implements Serializable {
        public final transient HashIndexSpace index;
        public final int fileIndex;
        public final ByteArray key;
        public final transient Iterator<Entry<ByteArray, ByteArray>> it;

        public CompactionInfo(HashIndexSpace index, int fileIndex, ByteArray key, Iterator<Map.Entry<ByteArray, ByteArray>> it) {
            this.index = index;
            this.fileIndex = fileIndex;
            this.key = key;
            this.it = it;
        }
    }

    public static <K, V> HashIndexSpace create(IndexManager indexManager, IndexSchemaConfiguration schema,
                                               ITransactionProvider transactionProvider, IDataFileAllocator fileAllocator,
                                               int fileIndex, String filePrefix, boolean fixedKey, int maxKeySize, boolean fixedValue, int maxValueSize,
                                               IKeyNormalizer<K> keyNormalizer, IValueConverter<V> valueConverter, IIndexListener<V> listener,
                                               Map<String, String> properties) {
        Assert.notNull(indexManager);
        Assert.notNull(schema);
        Assert.notNull(properties);
        Assert.notNull(transactionProvider);
        Assert.isTrue(maxKeySize <= Constants.PAGE_SIZE / 16);
        Assert.isTrue(maxValueSize <= Constants.PAGE_SIZE / 16);

        IRawTransaction transaction = transactionProvider.getRawTransaction();
        bindFile(transaction, fileIndex, filePrefix, schema.getPathIndex(), indexManager, properties);

        HashIndexSpace index = new HashIndexSpace(indexManager, schema,
                transactionProvider, fileAllocator, fileIndex, fixedKey,
                maxKeySize, fixedValue, maxValueSize, keyNormalizer, valueConverter, filePrefix, listener, properties);
        index.writeHeader();

        return index;
    }

    public static <K, V> HashIndexSpace open(IndexManager indexManager, IndexSchemaConfiguration schema,
                                             ITransactionProvider transactionProvider, IDataFileAllocator fileAllocator, int fileIndex, String filePrefix,
                                             boolean fixedKey, int maxKeySize, boolean fixedValue, int maxValueSize,
                                             IKeyNormalizer<K> keyNormalizer, IValueConverter<V> valueConverter, IIndexListener<V> listener,
                                             Map<String, String> properties) {
        Assert.notNull(indexManager);
        Assert.notNull(schema);
        Assert.notNull(transactionProvider);
        Assert.notNull(properties);
        Assert.isTrue(maxKeySize <= Constants.PAGE_SIZE / 16);
        Assert.isTrue(maxValueSize <= Constants.PAGE_SIZE / 16);

        IRawTransaction transaction = transactionProvider.getRawTransaction();
        bindFile(transaction, fileIndex, filePrefix, schema.getPathIndex(), indexManager, properties);

        HashIndexSpace index = new HashIndexSpace(indexManager, schema,
                transactionProvider, fileAllocator, fileIndex, fixedKey,
                maxKeySize, fixedValue, maxValueSize, keyNormalizer, valueConverter, filePrefix, listener, properties);
        index.readHeader();

        return index;
    }

    public int getFileIndex() {
        return fileIndex;
    }

    public IKeyNormalizer<K> getKeyNormalizer() {
        return keyNormalizer;
    }

    public IValueConverter<V> getValueConverter() {
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

        if (maxCacheSize < cacheSize) {
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
    }

    @Override
    public ByteArray normalize(K key) {
        Assert.notNull(key);
        Assert.checkState(!isStale());

        return keyNormalizer.normalize(key);
    }

    @Override
    public HashIndexStatistics getStatistics() {
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

        return new HashIndexStatistics(elementCount, pageIndex + 1, dataSize, usage, elementsPerPage, averageElementSize);
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
    }

    @Override
    public void delete() {
        Assert.checkState(!isStale());
        checkWriteTransaction();

        IRawTransaction transaction = transactionProvider.getRawTransaction();
        transaction.getFile(fileIndex).delete();

        unload(true);

        super.delete();
    }

    public CompactionInfo compact(IRawBatchControl batchControl, CompactionInfo start, long compactionThreshold, boolean force) {
        Assert.notNull(batchControl);
        Assert.checkState(!isStale());
        checkWriteTransaction();

        if (elements.isEmpty())
            return null;

        HashIndexSpace<K, V> index;
        Iterator<Map.Entry<ByteArray, ByteArray>> it;
        if (start == null) {
            if (!force) {
                HashIndexStatistics statistics = getStatistics();
                if (statistics.getUsage() * 100 > compactionThreshold)
                    return null;
            }

            index = HashIndexSpace.create(indexManager, schema, transactionProvider, fileAllocator,
                    fileAllocator.allocateFile(transactionProvider.getRawTransaction()), filePrefix, fixedKey, maxKeySize,
                    fixedValue, maxValueSize, keyNormalizer, valueConverter, listener, properties);
            it = elements.entrySet().iterator();
        } else if (start.index == null) {
            index = HashIndexSpace.open(indexManager, schema, transactionProvider, fileAllocator, start.fileIndex,
                    filePrefix, fixedKey, maxKeySize, fixedValue, maxValueSize, keyNormalizer, valueConverter, listener, properties);
            for (it = elements.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<ByteArray, ByteArray> entry = it.next();
                if (entry.getKey().equals(start.key))
                    break;
            }
        } else {
            index = start.index;
            it = start.it;
        }

        CompactionInfo res = null;
        while (it.hasNext()) {
            Map.Entry<ByteArray, ByteArray> entry = it.next();
            index.add(entry.getKey(), entry.getValue());

            if (!batchControl.canContinue()) {
                res = new CompactionInfo(index, index.getFileIndex(), entry.getKey(), it);
                break;
            }
        }

        index.onTransactionCommitted();

        if (res != null)
            return res;
        else
            return new CompactionInfo(index, index.getFileIndex(), null, null);
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

            changedElements = new ArrayList<ChangeInfo>();

            region.writeLong(DATA_SIZE_OFFSET, 0l);
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

            serialization.writeByte(SERIALIZATION_END);
        }

        super.onTransactionCommitted();
    }

    @Override
    public void onTransactionRolledBack() {
        if (!changedElements.isEmpty()) {
            for (int i = changedElements.size() - 1; i >= 0; i--) {
                ChangeInfo info = changedElements.get(i);
                if (info.added) {
                    ByteArray value = elements.remove(info.key);
                    updateCacheSize(info.key, value, false);
                } else {
                    elements.put(info.key, info.value);
                    updateCacheSize(info.key, info.value, true);
                }
            }

            changedElements.clear();

            if (maxCacheSize < cacheSize) {
                if (logger.isLogEnabled(LogLevel.WARNING))
                    logger.log(LogLevel.WARNING, messages.quotaTooSmall(maxCacheSize, cacheSize));
            }
        }

        super.onTransactionRolledBack();
    }

    @Override
    public void unload(boolean full) {
        if (full) {
            elements.clear();
            changedElements.clear();

            indexManager.getContext().getResourceAllocator().unregister(getResourceConsumerName());
            cacheSize = 0;
            setQuota(categoryTypeConfiguration.getInitialCacheSize());
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
                "\n" + Strings.indent(messages.cacheStatistics(category, categoryTypeConfiguration.getName(), maxCacheSize, cacheSize).toString(), 4);
    }

    @Override
    public String toString() {
        HashIndexStatistics statistics = getStatistics();
        return MessageFormat.format("tree index ''{0}''({1})", schema.getAlias(), statistics);
    }

    private HashIndexSpace(IndexManager indexManager, IndexSchemaConfiguration schema,
                           ITransactionProvider transactionProvider, IDataFileAllocator fileAllocator, int fileIndex,
                           boolean fixedKey, int maxKeySize, boolean fixedValue, int maxValueSize,
                           IKeyNormalizer<K> keyNormalizer, IValueConverter<V> valueConverter, String filePrefix, IIndexListener<V> listener,
                           Map<String, String> properties) {
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

        Pair<String, String> pair = indexManager.getContext().getCacheCategorizationStrategy().categorize(new MapBuilder<String, String>(properties)
                .put("type", "indexes.hash")
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

    private void readHeader() {
        IRawPage page = headerPage;
        IRawReadRegion region = page.getReadRegion();

        short magic = region.readShort(0);
        byte version = region.readByte(VERSION_OFFSET);

        if (magic != MAGIC)
            throw new RawDatabaseException(messages.invalidFormat(fileIndex));
        if (version != Constants.VERSION)
            throw new RawDatabaseException(messages.unsupportedVersion(fileIndex, version, Constants.VERSION));

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
                updateCacheSize(key, value, true);
            } else if (type == SERIALIZATION_REMOVED) {
                ByteArray key = deserialization.readByteArray();
                ByteArray value = elements.remove(key);
                updateCacheSize(key, value, false);
            } else
                Assert.error();
        }

        if (maxCacheSize < cacheSize) {
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
    }

    private static void bindFile(IRawTransaction transaction, int fileIndex, String filePrefix, int pathIndex,
                                 IndexManager indexManager, Map<String, String> properties) {
        RawBindInfo bindInfo = new RawBindInfo();
        bindInfo.setPathIndex(pathIndex);
        bindInfo.setName(Spaces.getSpaceIndexFileName(filePrefix, fileIndex));

        Pair<String, String> pair = indexManager.getContext().getCacheCategorizationStrategy().categorize(
                new MapBuilder<String, String>(properties)
                        .put("type", "pages.index.hash")
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
            updateCacheSize(byteKey, oldValue, false);
        }

        changedElements.add(new ChangeInfo(byteKey, byteValue, true));
        updateCacheSize(byteKey, byteValue, true);

        if (maxCacheSize < cacheSize) {
            if (logger.isLogEnabled(LogLevel.WARNING))
                logger.log(LogLevel.WARNING, messages.quotaTooSmall(maxCacheSize, cacheSize));
        }
    }

    private void updateCacheSize(ByteArray key, ByteArray value, boolean increment) {
        int entryCacheSize = CacheSizes.LINKED_HASH_MAP_ENTRY_CACHE_SIZE +
                CacheSizes.getByteArrayCacheSize(key) + CacheSizes.getByteArrayCacheSize(value) + Unsafe.ADDRESS_SIZE;

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
