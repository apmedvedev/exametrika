/**
 * Copyright 2013 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.indexing.sandbox.bitmap;

import com.exametrika.api.exadb.index.IKeyNormalizer;
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
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Numbers;
import com.exametrika.impl.exadb.core.Constants;
import com.exametrika.impl.exadb.core.Spaces;
import com.exametrika.spi.exadb.core.ITransactionProvider;


/**
 * The {@link ProjectionIndexSpace} is a space of projection index.
 *
 * @param <K> key type
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class ProjectionIndexSpace<K> {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final short MAGIC = 0x1719;
    private static final int HEADER_SIZE = 4;
    private static final int VERSION_OFFSET = 2; // magic(short) + version(byte) + padding(byte)
    private final ITransactionProvider transactionProvider;
    private final int pageSizeShift;
    private final int pageSizeMask;
    private final int fileIndex;
    private final IKeyNormalizer<K> keyNormalizer;
    private final int keySize;
    private final ByteArray removedKey;
    private final ByteArray nullKey;
    private final boolean allowNulls;
    private int startIndex;
    private IRawPage headerPage;
    private IRawPage appendPage;

    public static ProjectionIndexSpace create(ITransactionProvider transactionProvider, int pathIndex,
                                              int fileIndex, String filePrefix, IKeyNormalizer keyNormalizer, int keySize, boolean allowNulls) {
        Assert.notNull(transactionProvider);
        Assert.isTrue(Numbers.isPowerOfTwo(keySize) && keySize < Constants.PAGE_SIZE);

        IRawTransaction transaction = transactionProvider.getRawTransaction();
        bindFile(transaction, fileIndex, filePrefix, pathIndex);

        ProjectionIndexSpace index = new ProjectionIndexSpace(transactionProvider, fileIndex, keyNormalizer,
                keySize, allowNulls);
        index.writeHeader();

        return index;
    }

    public static ProjectionIndexSpace open(ITransactionProvider transactionProvider, int pathIndex,
                                            int fileIndex, String filePrefix, IKeyNormalizer keyNormalizer, int keySize, boolean allowNulls) {
        Assert.notNull(transactionProvider);
        Assert.isTrue(Numbers.isPowerOfTwo(keySize) && keySize < Constants.PAGE_SIZE);

        IRawTransaction transaction = transactionProvider.getRawTransaction();
        bindFile(transaction, fileIndex, filePrefix, pathIndex);

        ProjectionIndexSpace index = new ProjectionIndexSpace(transactionProvider, fileIndex, keyNormalizer,
                keySize, allowNulls);
        index.readHeader();

        return index;
    }

    public int getFileIndex() {
        return fileIndex;
    }

    public void add(long index, K key) {
        ByteArray bitCode;
        if (key == null) {
            Assert.isTrue(allowNulls);
            bitCode = nullKey;
        } else
            bitCode = keyNormalizer.normalize(key);

        long fileOffset = index * keySize + startIndex;
        IRawWriteRegion pageRegion = getAppendPage(fileOffset >>> pageSizeShift).getWriteRegion();
        int pageOffset = (int) (fileOffset & pageSizeMask);
        pageRegion.writeByteArray(pageOffset, bitCode);
    }

    public void remove(long index) {
        long fileOffset = index * keySize + startIndex;
        IRawWriteRegion pageRegion = transactionProvider.getRawTransaction().getPage(fileIndex, fileOffset >>> pageSizeShift).getWriteRegion();
        int pageOffset = (int) (fileOffset & pageSizeMask);
        pageRegion.writeByteArray(pageOffset, removedKey);
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

    private ProjectionIndexSpace(ITransactionProvider transactionProvider, int fileIndex,
                                 IKeyNormalizer keyNormalizer, int keySize, boolean allowNulls) {
        Assert.notNull(transactionProvider);
        Assert.isTrue(fileIndex != 0);
        Assert.notNull(keyNormalizer);
        Assert.isTrue(keySize > 0);

        this.transactionProvider = transactionProvider;
        this.fileIndex = fileIndex;
        this.pageSizeShift = Constants.PAGE_SHIFT;
        int pageSizeMask = 1;
        for (int i = 0; i < pageSizeShift - 1; i++)
            pageSizeMask = (pageSizeMask << 1) | 1;
        this.pageSizeMask = pageSizeMask;
        this.keyNormalizer = keyNormalizer;
        this.keySize = keySize;
        this.removedKey = new ByteArray(new byte[keySize]);
        this.headerPage = transactionProvider.getRawTransaction().getPage(fileIndex, 0);
        this.startIndex = HEADER_SIZE > keySize ? HEADER_SIZE : keySize;

        byte[] buffer = new byte[keySize];
        buffer[keySize - 1] = 1;
        this.nullKey = new ByteArray(buffer);
        this.allowNulls = allowNulls;
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