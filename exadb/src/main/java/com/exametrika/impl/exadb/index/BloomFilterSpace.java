/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.index;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.lz4.LZ4;
import com.exametrika.common.rawdb.IRawPage;
import com.exametrika.common.rawdb.IRawReadRegion;
import com.exametrika.common.rawdb.IRawTransaction;
import com.exametrika.common.rawdb.IRawWriteRegion;
import com.exametrika.common.rawdb.RawBindInfo;
import com.exametrika.common.rawdb.RawDatabaseException;
import com.exametrika.common.rawdb.impl.RawPageDeserialization;
import com.exametrika.common.rawdb.impl.RawPageSerialization;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Collections;
import com.exametrika.common.utils.MapBuilder;
import com.exametrika.common.utils.Pair;
import com.exametrika.impl.exadb.core.Constants;
import com.exametrika.impl.exadb.core.Spaces;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.spi.exadb.core.ITransactionProvider;


/**
 * The {@link BloomFilterSpace} is a bloom filter space.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class BloomFilterSpace {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final short MAGIC = 0x171E;// magic(short) + version(byte) + filterCount(byte) + currentFilterBitCount(long) + 
    private static final byte VERSION = 0x1;  // filters[max_filter_count](hashCount(byte), fileOffset(long), capacity(long))
    private static final int FILTER_COUNT_OFFSET = 3;
    private static final int CURRENT_FILTER_BIT_COUNT_OFFSET = 4;
    private static final int FILTERS_OFFSET = 12;
    private static final int FILTER_HEADER_SIZE = 17;
    private static final int MAX_FILTER_COUNT = 30;
    private static final int HEADER_SIZE = FILTERS_OFFSET + FILTER_HEADER_SIZE * MAX_FILTER_COUNT;
    private static final int SEED1 = 454103996;
    private static final int SEED2 = -114491475;
    private final ITransactionProvider transactionProvider;
    private final int fileIndex;
    private final int initialHashCount;
    private final int pageSize;
    private final int pageOffsetMask;
    private final int pageShift;
    private final List<IRawPage> pages = new ArrayList<IRawPage>();
    private final List<BloomFilter> filters = new ArrayList<BloomFilter>();
    private BloomFilter currentFilter;
    private int startIndex;
    private boolean cleared;

    public static BloomFilterSpace create(int fileIndex, String filePrefix, int pathIndex, int pageTypeIndex,
                                          IDatabaseContext context, Map<String, String> properties, int initialHashCount) {
        Assert.notNull(filePrefix);
        Assert.notNull(context);
        Assert.notNull(properties);

        ITransactionProvider transactionProvider = context.getTransactionProvider();
        bindFile(transactionProvider.getRawTransaction(), fileIndex, filePrefix, pathIndex, pageTypeIndex, context, properties);
        BloomFilterSpace space = new BloomFilterSpace(transactionProvider, fileIndex, initialHashCount, pageTypeIndex);
        space.writeHeader();

        return space;
    }

    public static BloomFilterSpace open(int fileIndex, String filePrefix, int pathIndex, int pageTypeIndex,
                                        IDatabaseContext context, Map<String, String> properties, int initialHashCount) {
        Assert.notNull(filePrefix);
        Assert.notNull(context);
        Assert.notNull(properties);

        ITransactionProvider transactionProvider = context.getTransactionProvider();
        bindFile(transactionProvider.getRawTransaction(), fileIndex, filePrefix, pathIndex, pageTypeIndex, context, properties);
        BloomFilterSpace space = new BloomFilterSpace(transactionProvider, fileIndex, initialHashCount, pageTypeIndex);
        space.readHeader();

        return space;
    }

    public int getFileIndex() {
        return fileIndex;
    }

    public void onTransactionStarted() {
        startIndex = filters.size();
    }

    public void onTransactionCommitted() {
        startIndex = filters.size();
        cleared = false;
    }

    public boolean onBeforeTransactionRolledBack() {
        return (startIndex == 0 && filters.size() <= 1) || cleared;
    }

    public void onTransactionRolledBack() {
        if (filters.size() <= 1)
            return;

        int count = filters.size();
        for (int i = startIndex; i < count; i++)
            filters.remove(filters.size() - 1);

        if (!filters.isEmpty())
            currentFilter = filters.get(filters.size() - 1);
        else
            currentFilter = null;
    }

    public void add(ByteArray value) {
        int hash1 = LZ4.hash(value.getBuffer(), value.getOffset(), value.getLength(), SEED1);
        int hash2 = LZ4.hash(value.getBuffer(), value.getOffset(), value.getLength(), SEED2);

        if (currentFilter == null || !currentFilter.add(hash1, hash2))
            addFilter();
    }

    public boolean isNotContained(ByteArray value) {
        int hash1 = LZ4.hash(value.getBuffer(), value.getOffset(), value.getLength(), SEED1);
        int hash2 = LZ4.hash(value.getBuffer(), value.getOffset(), value.getLength(), SEED2);

        for (int i = 0; i < filters.size(); i++) {
            BloomFilter filter = filters.get(i);
            if (!filter.isNotContained(hash1, hash2))
                return false;
        }

        return true;
    }

    public void clear() {
        cleared = true;
        pages.clear();
        filters.clear();
        currentFilter = null;
        transactionProvider.getRawTransaction().getFile(fileIndex).truncate(pageSize);
        writeHeader();
    }

    public void delete() {
        IRawTransaction transaction = transactionProvider.getRawTransaction();
        transaction.getFile(fileIndex).delete();
    }

    private BloomFilterSpace(ITransactionProvider transactionProvider, int fileIndex, int initialHashCount,
                             int pageTypeIndex) {
        Assert.notNull(transactionProvider);

        this.transactionProvider = transactionProvider;
        this.fileIndex = fileIndex;
        this.initialHashCount = initialHashCount;
        this.pageSize = Constants.pageSizes[pageTypeIndex];
        this.pageOffsetMask = Constants.pageOffsetMasks[pageTypeIndex];
        this.pageShift = Constants.pageShifts[pageTypeIndex];
    }

    private void readHeader() {
        RawPageDeserialization deserialization = new RawPageDeserialization(transactionProvider.getRawTransaction(),
                fileIndex, getPage(0), 0);

        short magic = deserialization.readShort();
        byte version = deserialization.readByte();

        if (magic != MAGIC)
            throw new RawDatabaseException(messages.invalidFormat(deserialization.getFileIndex()));
        if (version != VERSION)
            throw new RawDatabaseException(messages.unsupportedVersion(deserialization.getFileIndex(), version, VERSION));

        int filterCount = deserialization.readByte();
        deserialization.readLong();// currentFilterBitCount
        for (int i = 0; i < filterCount; i++) {
            int hashCount = deserialization.readByte();
            long fileOffset = deserialization.readLong();
            long capacity = deserialization.readLong();

            BloomFilter filter = new BloomFilter(fileOffset, hashCount, capacity);
            filters.add(filter);
            currentFilter = filter;
        }
    }

    private void writeHeader() {
        RawPageSerialization serialization = new RawPageSerialization(transactionProvider.getRawTransaction(),
                fileIndex, getPage(0), 0);

        serialization.writeShort(MAGIC);
        serialization.writeByte(VERSION);

        addFilter();
    }

    private void addFilter() {
        if (filters.size() == MAX_FILTER_COUNT)
            return;

        int hashCount;
        long fileOffset;
        long capacity;
        if (currentFilter == null) {
            hashCount = initialHashCount;
            capacity = pageSize << 3;
            fileOffset = HEADER_SIZE;
        } else {
            hashCount = currentFilter.hashCount + 1;
            capacity = currentFilter.capacity << 1;
            fileOffset = currentFilter.fileOffset + currentFilter.hashCount * (currentFilter.capacity >>> 3);
        }

        RawPageSerialization serialization = new RawPageSerialization(transactionProvider.getRawTransaction(),
                fileIndex, getPage(0), FILTER_COUNT_OFFSET);
        serialization.writeByte((byte) (filters.size() + 1));
        serialization.writeLong(0);

        serialization = new RawPageSerialization(transactionProvider.getRawTransaction(),
                fileIndex, getPage(0), FILTERS_OFFSET + filters.size() * FILTER_HEADER_SIZE);
        serialization.writeByte((byte) hashCount);
        serialization.writeLong(fileOffset);
        serialization.writeLong(capacity);

        BloomFilter filter = new BloomFilter(fileOffset, hashCount, capacity);
        filters.add(filter);
        currentFilter = filter;

        long endFilterOffset = fileOffset + (capacity >>> 3) * hashCount - 1;
        int endFilterPageIndex = (int) (endFilterOffset >>> pageShift);
        getPage(endFilterPageIndex).getWriteRegion();
    }

    private static void bindFile(IRawTransaction transaction, int fileIndex, String filePrefix, int pathIndex, int pageTypeIndex,
                                 IDatabaseContext context, Map<String, String> properties) {
        RawBindInfo bindInfo = new RawBindInfo();
        bindInfo.setPathIndex(pathIndex);
        bindInfo.setName(Spaces.getSpaceIndexFileName(filePrefix + "-bf", fileIndex));
        bindInfo.setPageTypeIndex(pageTypeIndex);

        Pair<String, String> pair = context.getCacheCategorizationStrategy().categorize(
                new MapBuilder<String, String>(properties)
                        .put("type", "pages.index.btree.bloom")
                        .toMap());
        bindInfo.setCategory(pair.getKey());
        bindInfo.setCategoryType(pair.getValue());

        transaction.bindFile(fileIndex, bindInfo);
    }

    private IRawPage getPage(int pageIndex) {
        IRawPage page = null;
        if (pageIndex < pages.size())
            page = pages.get(pageIndex);

        if (page != null)
            return page;
        else
            return refreshPage(pageIndex);
    }

    private IRawPage refreshPage(int pageIndex) {
        IRawPage page = transactionProvider.getRawTransaction().getPage(fileIndex, pageIndex);
        Collections.set((ArrayList) pages, pageIndex, page);
        return page;
    }

    private class BloomFilter {
        private final long fileOffset;
        private final int hashCount;
        private final long capacity;

        public BloomFilter(long fileOffset, int hashCount, long capacity) {
            this.fileOffset = fileOffset;
            this.hashCount = hashCount;
            this.capacity = capacity;
        }

        public boolean add(int hash1, int hash2) {
            boolean overflow = false;
            long filterSliceSize = capacity >>> 3;
            for (int i = 0; i < hashCount; i++) {
                long hash = hash1 + i * hash2;
                long bitIndex = hash & (capacity - 1);
                long fileOffset = (bitIndex >>> 3) + this.fileOffset + i * filterSliceSize;

                int pageIndex = (int) (fileOffset >>> pageShift);
                int pageOffset = (int) (fileOffset & pageOffsetMask);
                int byteOffset = (int) (bitIndex & 7);

                IRawPage page = getPage(pageIndex);
                IRawWriteRegion region = page.getWriteRegion();
                byte mask = (byte) (1 << byteOffset);
                byte b = region.readByte(pageOffset);
                if (i == 0 && (b & mask) == 0)
                    overflow = incrementBitCount();

                b |= mask;
                region.writeByte(pageOffset, b);
            }

            return !overflow;
        }

        public boolean isNotContained(int hash1, int hash2) {
            long filterSliceSize = capacity >>> 3;
            for (int i = 0; i < hashCount; i++) {
                long hash = hash1 + i * hash2;
                long bitIndex = hash & (capacity - 1);
                long fileOffset = (bitIndex >>> 3) + this.fileOffset + i * filterSliceSize;

                int pageIndex = (int) (fileOffset >>> pageShift);
                int pageOffset = (int) (fileOffset & pageOffsetMask);
                int byteOffset = (int) (bitIndex & 7);

                IRawPage page = getPage(pageIndex);
                IRawReadRegion region = page.getReadRegion();
                byte mask = (byte) (1 << byteOffset);
                byte b = region.readByte(pageOffset);
                if ((b & mask) == 0)
                    return true;
            }

            return false;
        }

        private boolean incrementBitCount() {
            IRawPage page = getPage(0);
            IRawWriteRegion region = page.getWriteRegion();

            long count = region.readLong(CURRENT_FILTER_BIT_COUNT_OFFSET);
            count++;
            region.writeLong(CURRENT_FILTER_BIT_COUNT_OFFSET, count);

            return count >= capacity / 2;
        }
    }

    private interface IMessages {
        @DefaultMessage("Invalid format of file ''{0}''.")
        ILocalizedMessage invalidFormat(int fileIndex);

        @DefaultMessage("Unsupported version ''{1}'' of file ''{0}'', expected version - ''{2}''.")
        ILocalizedMessage unsupportedVersion(int fileIndex, int fileVersion, int expectedVersion);
    }
}
