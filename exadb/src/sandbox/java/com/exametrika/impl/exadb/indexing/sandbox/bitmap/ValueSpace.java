/**
 * Copyright 2013 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.indexing.sandbox.bitmap;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.rawdb.RawBindInfo;
import com.exametrika.common.rawdb.RawDatabaseException;
import com.exametrika.common.rawdb.IRawBatchControl;
import com.exametrika.common.rawdb.IRawPage;
import com.exametrika.common.rawdb.IRawReadRegion;
import com.exametrika.common.rawdb.IRawTransaction;
import com.exametrika.common.rawdb.IRawWriteRegion;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Numbers;
import com.exametrika.common.utils.Pair;
import com.exametrika.impl.exadb.core.Constants;
import com.exametrika.impl.exadb.core.Spaces;
import com.exametrika.spi.exadb.core.IDataFileAllocator;
import com.exametrika.spi.exadb.core.ITransactionProvider;
import com.ibm.icu.text.MessageFormat;


/**
 * The {@link ValueSpace} is a space of index values.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class ValueSpace {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final short MAGIC = 0x1715;
    private static final int HEADER_SIZE = 32;
    private static final int VERSION_OFFSET = 2;          // magic(short) + version(byte) + padding(byte)
    private static final int NEXT_PAGE_INDEX_OFFSET = 4;  // + nextPageIndex(long) + nextPageOffset(int)
    private static final int NEXT_PAGE_OFFSET_OFFSET = 12;// + elementCount(long) + lastFreeAreaPageIndex(long)
    private static final int ELEMENT_COUNT_OFFSET = 16;
    private static final int LAST_FREE_AREA_PAGE_INDEX_OFFSET = 24;
    private static final short PAGE_MAGIC = 0x1716;// pageMagic(short) + pageNextFreeAreaPageIndex(long) + pageLastFreeAreaPageOffset(int)
    private static final int PAGE_MAGIC_OFFSET = 0;
    private static final int PAGE_NEXT_FREE_AREA_PAGE_INDEX_OFFSET = 2;
    private static final int PAGE_LAST_FREE_AREA_PAGE_OFFSET_OFFSET = 10;
    private static final byte FREE_AREA_HEADER_SIZE = 5;
    private static final byte FREE_AREA_MAGIC = (byte) 0xFA;// freeAreaMagic(byte) + nextFreeAreaPageOffset(int)
    private static final int FREE_AREA_NEXT_FREE_AREA_PAGE_OFFSET_OFFSET = 1;
    private final ITransactionProvider transactionProvider;
    private final IDataFileAllocator fileAllocator;
    private final int pathIndex;
    private final int pageSizeShift;
    private final int pageSizeMask;
    private final int fileIndex;
    private final String filePrefix;
    private final int valueSize;
    private IRawPage headerPage;

    public static ValueSpace create(ITransactionProvider transactionProvider, IDataFileAllocator fileAllocator,
                                    int pathIndex, int fileIndex, String filePrefix, int valueSize) {
        Assert.notNull(transactionProvider);
        Assert.isTrue(Numbers.isPowerOfTwo(valueSize) && valueSize > 0 && valueSize <= HEADER_SIZE && valueSize > FREE_AREA_HEADER_SIZE);

        IRawTransaction transaction = transactionProvider.getRawTransaction();
        bindFile(transaction, fileIndex, filePrefix, pathIndex);

        ValueSpace index = new ValueSpace(transactionProvider, fileAllocator, pathIndex, fileIndex, filePrefix, valueSize);
        index.writeHeader();

        return index;
    }

    public static ValueSpace open(ITransactionProvider transactionProvider, IDataFileAllocator fileAllocator,
                                  int pathIndex, int fileIndex, String filePrefix, int valueSize) {
        Assert.notNull(transactionProvider);
        Assert.isTrue(Numbers.isPowerOfTwo(valueSize) && valueSize > 0 && valueSize <= HEADER_SIZE && valueSize > FREE_AREA_HEADER_SIZE);

        IRawTransaction transaction = transactionProvider.getRawTransaction();
        bindFile(transaction, fileIndex, filePrefix, pathIndex);

        ValueSpace index = new ValueSpace(transactionProvider, fileAllocator, pathIndex, fileIndex, filePrefix, valueSize);
        index.readHeader();

        return index;
    }

    public int getFileIndex() {
        return fileIndex;
    }

    public boolean isEmpty() {
        return getCount() == 0;
    }

    public long getCount() {
        IRawReadRegion region = headerPage.getReadRegion();
        long elementCount = region.readLong(ELEMENT_COUNT_OFFSET);
        return elementCount;
    }

    public ByteArray get(long index) {
        long fileOffset = fileOffsetByIndex(index);
        IRawReadRegion region = transactionProvider.getRawTransaction().getPage(fileIndex, fileOffset >>> pageSizeShift).getReadRegion();
        ByteArray value = region.readByteArray((int) (fileOffset & pageSizeMask), valueSize);
        if ((value.get(0) & 0x80) == 0)
            return value;
        else
            return null;
    }

    public long add(ByteArray value) {
        Assert.notNull(value);
        Assert.isTrue(value.getLength() == valueSize);
        Assert.isTrue((value.get(0) & 0x80) == 0);

        long fileOffset = allocateArea();
        IRawWriteRegion pageRegion = transactionProvider.getRawTransaction().getPage(fileIndex, fileOffset >>> pageSizeShift).getWriteRegion();
        pageRegion.writeByteArray((int) (fileOffset & pageSizeMask), value);

        IRawWriteRegion region = headerPage.getWriteRegion();
        long elementCount = region.readLong(ELEMENT_COUNT_OFFSET);
        region.writeLong(ELEMENT_COUNT_OFFSET, elementCount + 1);

        return indexByFileOffset(fileOffset);
    }

    public void remove(long index) {
        long fileOffset = fileOffsetByIndex(index);
        IRawPage page = transactionProvider.getRawTransaction().getPage(fileIndex, fileOffset >>> pageSizeShift);
        freeArea(page, (int) (fileOffset & pageSizeMask));

        IRawWriteRegion region = headerPage.getWriteRegion();
        long elementCount = region.readLong(ELEMENT_COUNT_OFFSET);
        region.writeLong(ELEMENT_COUNT_OFFSET, elementCount - 1);
    }

    public void clear() {
        IRawPage page = headerPage;
        transactionProvider.getRawTransaction().getFile(fileIndex).truncate(page.getSize());
        writeHeader();
    }

    public Pair<Long, ValueSpace> compact(IRawBatchControl batchControl, Pair<Long, ValueSpace> start,
                                          long compactionThreshold, boolean force) {
        Assert.notNull(batchControl);
        if (isEmpty())
            return null;

        IRawReadRegion region = headerPage.getReadRegion();
        long nextPageIndex = region.readLong(NEXT_PAGE_INDEX_OFFSET);
        long nextPageOffset = region.readLong(NEXT_PAGE_OFFSET_OFFSET);
        long totalCount = indexByFileOffset(nextPageIndex << pageSizeShift + nextPageOffset);

        if (start == null) {
            if (!force) {
                long count = region.readLong(ELEMENT_COUNT_OFFSET);
                if ((double) count / totalCount * 100 > compactionThreshold)
                    return null;
            }

            start = new Pair<Long, ValueSpace>(0l, ValueSpace.create(transactionProvider, fileAllocator,
                    pathIndex, fileAllocator.allocateFile(transactionProvider.getRawTransaction()),
                    filePrefix, valueSize));
        }

        ValueSpace index = start.getValue();

        Pair<Long, ValueSpace> res = null;

        for (long i = start.getKey(); i < totalCount; i++) {
            if (!batchControl.canContinue()) {
                res = new Pair<Long, ValueSpace>(i, index);
                break;
            }

            ByteArray value = get(i);
            if (value != null)
                index.add(value);
        }

        if (res != null)
            return res;
        else
            return new Pair<Long, ValueSpace>(null, index);
    }

    public void delete() {
        IRawTransaction transaction = transactionProvider.getRawTransaction();
        transaction.getFile(fileIndex).delete();
    }

    @Override
    public String toString() {
        return MessageFormat.format("count: {0}", getCount());
    }

    private ValueSpace(ITransactionProvider transactionProvider, IDataFileAllocator fileAllocator,
                       int pathIndex, int fileIndex, String filePrefix, int valueSize) {
        Assert.notNull(transactionProvider);
        Assert.notNull(fileAllocator);
        Assert.isTrue(fileIndex != 0);

        this.transactionProvider = transactionProvider;
        this.fileAllocator = fileAllocator;
        this.fileIndex = fileIndex;
        this.valueSize = valueSize;
        this.pathIndex = pathIndex;
        this.pageSizeShift = Constants.PAGE_SHIFT;
        int pageSizeMask = 1;
        for (int i = 0; i < pageSizeShift - 1; i++)
            pageSizeMask = (pageSizeMask << 1) | 1;
        this.pageSizeMask = pageSizeMask;

        this.filePrefix = filePrefix;
        this.headerPage = transactionProvider.getRawTransaction().getPage(fileIndex, 0);
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
    }

    private void writeHeader() {
        IRawPage page = headerPage;
        IRawWriteRegion region = page.getWriteRegion();
        region.fill(0, page.getSize(), (byte) 0);

        region.writeShort(0, MAGIC);
        region.writeByte(VERSION_OFFSET, Constants.VERSION);
        region.writeInt(NEXT_PAGE_OFFSET_OFFSET, HEADER_SIZE);
    }

    private long allocateArea() {
        IRawWriteRegion region = headerPage.getWriteRegion();
        long lastFreeAreaPageIndex = region.readLong(LAST_FREE_AREA_PAGE_INDEX_OFFSET);
        if (lastFreeAreaPageIndex != 0) {
            IRawPage lastFreeAreaPage = transactionProvider.getRawTransaction().getPage(fileIndex, lastFreeAreaPageIndex);
            return allocateAreaFromPage(region, lastFreeAreaPage);
        }

        return allocateAreaFromEnd();
    }

    private long allocateAreaFromPage(IRawWriteRegion region, IRawPage page) {
        IRawWriteRegion pageRegion = page.getWriteRegion();
        short value = pageRegion.readShort(PAGE_MAGIC_OFFSET);
        if (value != PAGE_MAGIC)
            throw new RawDatabaseException(messages.invalidFormat(fileIndex));

        int pageLastFreeAreaPageOffset = pageRegion.readInt(PAGE_LAST_FREE_AREA_PAGE_OFFSET_OFFSET);

        if (pageRegion.readByte(pageLastFreeAreaPageOffset) != FREE_AREA_MAGIC)
            throw new RawDatabaseException(messages.invalidFormat(fileIndex));

        int nextFreeAreaPageOffset = pageRegion.readInt(pageLastFreeAreaPageOffset + FREE_AREA_NEXT_FREE_AREA_PAGE_OFFSET_OFFSET);
        pageRegion.writeInt(PAGE_LAST_FREE_AREA_PAGE_OFFSET_OFFSET, nextFreeAreaPageOffset);
        if (nextFreeAreaPageOffset == 0) {
            long nextFreeAreaPageIndex = pageRegion.readLong(PAGE_NEXT_FREE_AREA_PAGE_INDEX_OFFSET);
            region.writeLong(LAST_FREE_AREA_PAGE_INDEX_OFFSET, nextFreeAreaPageIndex);
        }
        return page.getIndex() * Constants.PAGE_SIZE + pageLastFreeAreaPageOffset;
    }

    private long allocateAreaFromEnd() {
        IRawWriteRegion region = headerPage.getWriteRegion();
        long nextPageIndex = region.readLong(NEXT_PAGE_INDEX_OFFSET);
        int nextPageOffset = region.readInt(NEXT_PAGE_OFFSET_OFFSET);
        if (nextPageOffset + valueSize == Constants.PAGE_SIZE) {
            nextPageIndex++;
            nextPageOffset = HEADER_SIZE;
            IRawPage page = transactionProvider.getRawTransaction().getPage(fileIndex, nextPageIndex);
            IRawWriteRegion pageRegion = page.getWriteRegion();
            pageRegion.writeShort(PAGE_MAGIC_OFFSET, PAGE_MAGIC);
            pageRegion.writeLong(PAGE_NEXT_FREE_AREA_PAGE_INDEX_OFFSET, 0);
            pageRegion.writeInt(PAGE_LAST_FREE_AREA_PAGE_OFFSET_OFFSET, 0);
            region.writeLong(NEXT_PAGE_INDEX_OFFSET, nextPageIndex);
        }

        region.writeInt(NEXT_PAGE_OFFSET_OFFSET, nextPageOffset + valueSize);

        return nextPageIndex * Constants.PAGE_SIZE + nextPageOffset;
    }

    private void freeArea(IRawPage page, int pageOffset) {
        IRawWriteRegion pageRegion = page.getWriteRegion();
        short value = pageRegion.readShort(PAGE_MAGIC_OFFSET);
        if (value != PAGE_MAGIC)
            throw new RawDatabaseException(messages.invalidFormat(fileIndex));
        int pageLastFreeAreaPageOffset = pageRegion.readInt(PAGE_LAST_FREE_AREA_PAGE_OFFSET_OFFSET);
        pageRegion.writeInt(PAGE_LAST_FREE_AREA_PAGE_OFFSET_OFFSET, pageOffset);

        pageRegion.writeByte(pageOffset, FREE_AREA_MAGIC);
        pageRegion.writeInt(pageOffset + FREE_AREA_NEXT_FREE_AREA_PAGE_OFFSET_OFFSET, pageLastFreeAreaPageOffset);

        if (pageLastFreeAreaPageOffset == 0) {
            IRawWriteRegion region = headerPage.getWriteRegion();
            long lastFreeAreaPageIndex = region.readLong(LAST_FREE_AREA_PAGE_INDEX_OFFSET);
            region.writeLong(LAST_FREE_AREA_PAGE_INDEX_OFFSET, page.getIndex());

            pageRegion.writeLong(PAGE_NEXT_FREE_AREA_PAGE_INDEX_OFFSET, lastFreeAreaPageIndex);
        }
    }

    private long fileOffsetByIndex(long index) {
        long size = index * valueSize;
        int valuesSizePerPage = Constants.PAGE_SIZE - HEADER_SIZE;
        long pageIndex = size / valuesSizePerPage;
        int pageOffset = HEADER_SIZE + (int) (size % valuesSizePerPage);
        return pageIndex << pageSizeShift + pageOffset;
    }

    private long indexByFileOffset(long fileOffset) {
        long pageIndex = fileOffset >>> pageSizeShift;
        int pageOffset = (int) (fileOffset & pageSizeMask);
        int valuesSizePerPage = Constants.PAGE_SIZE - HEADER_SIZE;
        long size = pageIndex * valuesSizePerPage + (pageOffset - HEADER_SIZE);
        return size / valueSize;
    }

    private static void bindFile(IRawTransaction transaction, int fileIndex, String filePrefix, int pathIndex) {
        RawBindInfo bindInfo = new RawBindInfo();
        bindInfo.setPathIndex(pathIndex);
        bindInfo.setName(Spaces.getSpaceIndexFileName(filePrefix, fileIndex));
        transaction.bindFile(fileIndex, bindInfo);
    }

    private interface IMessages {
        @DefaultMessage("Invalid format of file ''{0}''.")
        ILocalizedMessage invalidFormat(int fileIndex);

        @DefaultMessage("Unsupported version ''{1}'' of file ''{0}'', expected version - ''{2}''.")
        ILocalizedMessage unsupportedVersion(int fileIndex, int fileVersion, int expectedVersion);
    }
}