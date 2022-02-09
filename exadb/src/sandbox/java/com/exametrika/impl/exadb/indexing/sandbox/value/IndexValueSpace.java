/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.indexing.sandbox.value;

import com.exametrika.api.exadb.index.IIndexListener;
import com.exametrika.api.exadb.index.IValueConverter;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.rawdb.RawBindInfo;
import com.exametrika.common.rawdb.RawDatabaseException;
import com.exametrika.common.rawdb.IRawPage;
import com.exametrika.common.rawdb.IRawReadRegion;
import com.exametrika.common.rawdb.IRawTransaction;
import com.exametrika.common.rawdb.IRawWriteRegion;
import com.exametrika.common.rawdb.impl.RawHeapReadRegion;
import com.exametrika.common.rawdb.impl.RawPageDeserialization;
import com.exametrika.common.rawdb.impl.RawPageSerialization;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.impl.exadb.core.Constants;
import com.exametrika.impl.exadb.core.Spaces;
import com.exametrika.impl.exadb.indexing.sandbox.IIndexValue;
import com.exametrika.impl.exadb.indexing.sandbox.IIndexValueSpace;
import com.exametrika.spi.exadb.core.ITransactionProvider;


/**
 * The {@link IndexValueSpace} is a key-value space based on index.
 *
 * @param <K> key type
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public abstract class IndexValueSpace<K> implements IIndexValueSpace<K> {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final short MAGIC = 0x171A;// magic(short) + version(byte) + padding(byte) + indexFileIndex(int)
    private static final int INDEX_FILE_INDEX_OFFSET = 4;// + nextBlockIndex(long) + lastFreeAreaPageIndex(long)
    private static final int NEXT_BLOCK_INDEX_OFFSET = 8;
    private static final int LAST_FREE_AREA_PAGE_INDEX_OFFSET = 16;
    private static final int PAGE_HEADER_SIZE = Constants.BLOCK_SIZE;
    private static final int PAGE_HEADER_BLOCK_COUNT = 1;
    private static final short PAGE_MAGIC = 0x171B;// pageMagic(short) + pageNextFreeAreaPageIndex(long) + pageLastFreeAreaPageOffset(int)
    private static final int PAGE_MAGIC_OFFSET = 0;
    private static final int PAGE_NEXT_FREE_AREA_PAGE_INDEX_OFFSET = 2;
    private static final int PAGE_LAST_FREE_AREA_PAGE_OFFSET_OFFSET = 10;
    public static final byte FREE_AREA_MAGIC = 0x1A;// freeAreaMagic(byte) + nextFreeAreaPageOffset(int)
    private static final int FREE_AREA_NEXT_FREE_AREA_PAGE_OFFSET_OFFSET = 1;
    private final int fileIndex;
    private final ITransactionProvider transactionProvider;
    private IRawPage headerPage;

    @Override
    public IIndexValue createValue(int initialSize) {
        return IndexValue.create(this, initialSize);
    }

    public IRawTransaction getTransaction() {
        return transactionProvider.getRawTransaction();
    }

    public int getFileIndex() {
        return fileIndex;
    }

    public long allocateArea(IRawPage preferredPage) {
        long areaBlockIndex = allocateAreaFromPage(preferredPage);
        if (areaBlockIndex > 0)
            return areaBlockIndex;

        IRawWriteRegion region = headerPage.getWriteRegion();
        areaBlockIndex = region.readLong(NEXT_BLOCK_INDEX_OFFSET);

        if (preferredPage == null || Constants.pageIndexByBlockIndex(areaBlockIndex) != preferredPage.getIndex()) {
            long lastFreeAreaPageIndex = region.readLong(LAST_FREE_AREA_PAGE_INDEX_OFFSET);
            while (lastFreeAreaPageIndex != 0) {
                IRawPage lastFreeAreaPage = getTransaction().getPage(fileIndex, lastFreeAreaPageIndex);
                areaBlockIndex = allocateAreaFromPage(lastFreeAreaPage);

                if (areaBlockIndex > 0) {
                    region.writeLong(LAST_FREE_AREA_PAGE_INDEX_OFFSET, lastFreeAreaPageIndex);
                    return areaBlockIndex;
                }

                lastFreeAreaPageIndex = -areaBlockIndex;
            }

            region.writeLong(LAST_FREE_AREA_PAGE_INDEX_OFFSET, 0);
        }

        return allocateBlocks(Constants.COMPLEX_FIELD_AREA_BLOCK_COUNT + 1);
    }

    public void freeArea(IRawPage page, int pageOffset) {
        Assert.isTrue((pageOffset & Constants.BLOCK_MASK) == pageOffset);

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

    public long allocateBlocks(int blockCount) {
        Assert.isTrue(blockCount + PAGE_HEADER_BLOCK_COUNT <= Constants.BLOCKS_PER_PAGE_COUNT);

        IRawWriteRegion region = headerPage.getWriteRegion();
        long nextBlockIndex = region.readLong(NEXT_BLOCK_INDEX_OFFSET);
        int pageOffset = Constants.pageOffsetByBlockIndex(nextBlockIndex);
        int requiredSize = pageOffset + Constants.dataSize(blockCount);
        if (requiredSize > Constants.PAGE_SIZE) {
            IRawPage page = getTransaction().getPage(fileIndex, Constants.pageIndexByBlockIndex(nextBlockIndex));
            while (pageOffset + Constants.COMPLEX_FIELD_AREA_SIZE <= Constants.PAGE_SIZE) {
                freeArea(page, pageOffset);
                pageOffset += Constants.COMPLEX_FIELD_AREA_SIZE;
            }
        }

        long res = nextBlockIndex;
        if (requiredSize >= Constants.PAGE_SIZE) {
            IRawPage page = getTransaction().getPage(fileIndex, Constants.pageIndexByBlockIndex(nextBlockIndex) + 1);
            IRawWriteRegion pageRegion = page.getWriteRegion();
            pageRegion.writeShort(PAGE_MAGIC_OFFSET, PAGE_MAGIC);
            pageRegion.writeLong(PAGE_NEXT_FREE_AREA_PAGE_INDEX_OFFSET, 0);
            pageRegion.writeInt(PAGE_LAST_FREE_AREA_PAGE_OFFSET_OFFSET, 0);

            nextBlockIndex = Constants.blockIndex(page.getIndex(), PAGE_HEADER_SIZE);

            if (requiredSize > Constants.PAGE_SIZE)
                res = nextBlockIndex;
        }

        region.writeLong(NEXT_BLOCK_INDEX_OFFSET, nextBlockIndex + blockCount);

        return res;
    }

    public void clear() {
        IRawPage page = headerPage;
        transactionProvider.getRawTransaction().getFile(fileIndex).truncate(page.getSize());
        writeHeader(page.getReadRegion().readInt(INDEX_FILE_INDEX_OFFSET));
    }

    protected IndexValueSpace(ITransactionProvider transactionProvider, int fileIndex) {
        Assert.notNull(transactionProvider);
        Assert.isTrue(fileIndex != 0);

        this.transactionProvider = transactionProvider;
        this.fileIndex = fileIndex;
        this.headerPage = transactionProvider.getRawTransaction().getPage(fileIndex, 0);
    }

    protected int readHeader() {
        IRawTransaction transaction = transactionProvider.getRawTransaction();
        RawPageDeserialization deserialization = new RawPageDeserialization(transaction, fileIndex, headerPage, 0);

        short magic = deserialization.readShort();
        byte version = deserialization.readByte();
        deserialization.readByte();
        int indexFileIndex = deserialization.readInt();
        deserialization.readLong(); // nextBlockIndex (long) 
        deserialization.readLong(); // lastNodeBlockIndex (long)
        deserialization.readLong(); // lastFreeAreaPageIndex(long)
        deserialization.readLong(); // rootNodeBlockIndex(long)

        if (magic != MAGIC)
            throw new RawDatabaseException(messages.invalidFormat(deserialization.getFileIndex()));
        if (version != Constants.VERSION)
            throw new RawDatabaseException(messages.unsupportedVersion(deserialization.getFileIndex(), version, Constants.VERSION));

        return indexFileIndex;
    }

    protected void writeHeader(int indexFileIndex) {
        RawPageSerialization serialization = new RawPageSerialization(transactionProvider.getRawTransaction(), fileIndex, headerPage, 0);

        serialization.writeShort(MAGIC);
        serialization.writeByte(Constants.VERSION);
        serialization.writeByte((byte) 0);
        serialization.writeInt(indexFileIndex);
        serialization.writeLong(Constants.BLOCKS_PER_PAGE_COUNT + PAGE_HEADER_BLOCK_COUNT);
        serialization.writeLong(0);
        serialization.writeLong(0);
        serialization.writeLong(0);

        IRawPage page = getTransaction().getPage(fileIndex, 1);
        IRawWriteRegion pageRegion = page.getWriteRegion();
        pageRegion.writeShort(PAGE_MAGIC_OFFSET, PAGE_MAGIC);
        pageRegion.writeLong(PAGE_NEXT_FREE_AREA_PAGE_INDEX_OFFSET, 0);
        pageRegion.writeInt(PAGE_LAST_FREE_AREA_PAGE_OFFSET_OFFSET, 0);
    }

    private long allocateAreaFromPage(IRawPage page) {
        if (page == null)
            return 0;

        IRawWriteRegion pageRegion = page.getWriteRegion();
        short value = pageRegion.readShort(PAGE_MAGIC_OFFSET);
        if (value != PAGE_MAGIC)
            throw new RawDatabaseException(messages.invalidFormat(fileIndex));
        int pageLastFreeAreaPageOffset = pageRegion.readInt(PAGE_LAST_FREE_AREA_PAGE_OFFSET_OFFSET);
        if (pageLastFreeAreaPageOffset != 0) {
            if (pageRegion.readByte(pageLastFreeAreaPageOffset) != FREE_AREA_MAGIC)
                throw new RawDatabaseException(messages.invalidFormat(fileIndex));
            int nextFreeAreaPageOffset = pageRegion.readInt(pageLastFreeAreaPageOffset + FREE_AREA_NEXT_FREE_AREA_PAGE_OFFSET_OFFSET);
            pageRegion.writeInt(PAGE_LAST_FREE_AREA_PAGE_OFFSET_OFFSET, nextFreeAreaPageOffset);
            return Constants.blockIndex(page.getIndex(), pageLastFreeAreaPageOffset);
        }

        return -pageRegion.readLong(PAGE_NEXT_FREE_AREA_PAGE_INDEX_OFFSET);
    }

    protected static void bindFile(IRawTransaction transaction, int fileIndex, String filePrefix, int pathIndex) {
        RawBindInfo bindInfo = new RawBindInfo();
        bindInfo.setPathIndex(pathIndex);
        bindInfo.setName(Spaces.getSpaceIndexFileName(filePrefix, fileIndex));
        Assert.checkState(transaction.getDatabase().getConfiguration().getPageTypes().get(0).getPageSize() == Constants.PAGE_SIZE);
        transaction.bindFile(fileIndex, bindInfo);
    }

    protected class IndexValueConverter implements IValueConverter<IIndexValue> {
        @Override
        public ByteArray toByteArray(IIndexValue value) {
            IRawReadRegion region = ((IndexValue) value).getRegion();
            return region.readByteArray(0, region.getLength());
        }

        @Override
        public IIndexValue toValue(ByteArray buffer) {
            return IndexValue.open(IndexValueSpace.this, new RawHeapReadRegion(buffer.getBuffer(), buffer.getOffset(),
                    buffer.getLength()), true);
        }
    }

    protected class IndexValueListener implements IIndexListener<IIndexValue> {
        @Override
        public void onRemoved(IIndexValue value) {
            ((IndexValue) value).onDeleted();
        }

        @Override
        public void onCleared() {
            clear();
        }
    }

    private interface IMessages {
        @DefaultMessage("Invalid format of file ''{0}''.")
        ILocalizedMessage invalidFormat(int fileIndex);

        @DefaultMessage("Unsupported version ''{1}'' of file ''{0}'', expected version - ''{2}''.")
        ILocalizedMessage unsupportedVersion(int fileIndex, int fileVersion, int expectedVersion);
    }
}
