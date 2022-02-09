/**
 * Copyright 2013 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.indexing.sandbox.bitmap;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.rawdb.RawBindInfo;
import com.exametrika.common.rawdb.RawDatabaseException;
import com.exametrika.common.rawdb.IRawPage;
import com.exametrika.common.rawdb.IRawReadRegion;
import com.exametrika.common.rawdb.IRawTransaction;
import com.exametrika.common.rawdb.IRawWriteRegion;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.exadb.core.Constants;
import com.exametrika.impl.exadb.core.Spaces;
import com.exametrika.spi.exadb.core.ITransactionProvider;


/**
 * The {@link BitVectorSpace} is a space of single bit vector.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class BitVectorSpace {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final short MAGIC = 0x1718;
    private static final int HEADER_SIZE = 4;
    private static final int VERSION_OFFSET = 2; // magic(short) + version(byte) + padding(byte)
    private final ITransactionProvider transactionProvider;
    private final int pageSizeShift;
    private final int pageSizeMask;
    private final int fileIndex;
    private IRawPage headerPage;
    private IRawPage appendPage;

    public static BitVectorSpace create(ITransactionProvider transactionProvider, int pathIndex,
                                        int fileIndex, String filePrefix) {
        Assert.notNull(transactionProvider);

        IRawTransaction transaction = transactionProvider.getRawTransaction();
        bindFile(transaction, fileIndex, filePrefix, pathIndex);

        BitVectorSpace index = new BitVectorSpace(transactionProvider, fileIndex);
        index.writeHeader();

        return index;
    }

    public static BitVectorSpace open(ITransactionProvider transactionProvider, int pathIndex,
                                      int fileIndex, String filePrefix) {
        Assert.notNull(transactionProvider);

        IRawTransaction transaction = transactionProvider.getRawTransaction();
        bindFile(transaction, fileIndex, filePrefix, pathIndex);

        BitVectorSpace index = new BitVectorSpace(transactionProvider, fileIndex);
        index.readHeader();

        return index;
    }

    public int getFileIndex() {
        return fileIndex;
    }

    public void add(long index, boolean value) {
        long fileOffset = index >>> 3 + HEADER_SIZE;
        IRawWriteRegion pageRegion = getAppendPage(fileOffset >>> pageSizeShift).getWriteRegion();
        int pageOffset = (int) (fileOffset & pageSizeMask);
        byte v = pageRegion.readByte(pageOffset);
        if (value)
            v |= 1 << (int) (index & 7);
        else
            v &= ~(1 << (int) (index & 7));
        pageRegion.writeByte(pageOffset, v);
    }

    public void remove(long index) {
        long fileOffset = index >>> 3 + HEADER_SIZE;
        IRawWriteRegion pageRegion = transactionProvider.getRawTransaction().getPage(fileIndex, fileOffset >>> pageSizeShift).getWriteRegion();
        int pageOffset = (int) (fileOffset & pageSizeMask);
        byte value = pageRegion.readByte(pageOffset);
        value &= ~(1 << (int) (index & 7));
        pageRegion.writeByte(pageOffset, value);
    }

    public void clear() {
        IRawPage page = headerPage;
        transactionProvider.getRawTransaction().getFile(fileIndex).truncate(page.getSize());
        writeHeader();
    }

    public void delete() {
        IRawTransaction transaction = transactionProvider.getRawTransaction();
        transaction.getFile(fileIndex).delete();
    }

    private BitVectorSpace(ITransactionProvider transactionProvider, int fileIndex) {
        Assert.notNull(transactionProvider);
        Assert.isTrue(fileIndex != 0);

        this.transactionProvider = transactionProvider;
        this.fileIndex = fileIndex;
        this.pageSizeShift = Constants.PAGE_SHIFT;
        int pageSizeMask = 1;
        for (int i = 0; i < pageSizeShift - 1; i++)
            pageSizeMask = (pageSizeMask << 1) | 1;
        this.pageSizeMask = pageSizeMask;
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
        appendPage = headerPage;
    }

    private static void bindFile(IRawTransaction transaction, int fileIndex, String filePrefix, int pathIndex) {
        RawBindInfo bindInfo = new RawBindInfo();
        bindInfo.setPathIndex(pathIndex);
        bindInfo.setName(Spaces.getSpaceIndexFileName(filePrefix, fileIndex));
        transaction.bindFile(fileIndex, bindInfo);
    }

    private IRawPage getAppendPage(long pageIndex) {
        if (appendPage != null && appendPage.getIndex() == pageIndex)
            return appendPage;
        else
            return refreshAppendPage(pageIndex);
    }

    private IRawPage refreshAppendPage(long pageIndex) {
        appendPage = transactionProvider.getRawTransaction().getPage(fileIndex, pageIndex);
        return appendPage;
    }

    private interface IMessages {
        @DefaultMessage("Invalid format of file ''{0}''.")
        ILocalizedMessage invalidFormat(int fileIndex);

        @DefaultMessage("Unsupported version ''{1}'' of file ''{0}'', expected version - ''{2}''.")
        ILocalizedMessage unsupportedVersion(int fileIndex, int fileVersion, int expectedVersion);
    }
}