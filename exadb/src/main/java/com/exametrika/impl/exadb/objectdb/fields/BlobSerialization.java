/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.fields;

import com.exametrika.common.io.EndOfStreamException;
import com.exametrika.common.io.IDataSerialization;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.rawdb.IRawPage;
import com.exametrika.common.rawdb.IRawReadRegion;
import com.exametrika.common.rawdb.IRawTransaction;
import com.exametrika.common.rawdb.RawDatabaseException;
import com.exametrika.common.rawdb.impl.RawAbstractBlockSerialization;
import com.exametrika.common.rawdb.impl.RawDbDebug;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.exadb.objectdb.fields.IBlob;
import com.exametrika.spi.exadb.objectdb.fields.IBlobSerialization;


/**
 * The {@link BlobSerialization} is an implementation of {@link IDataSerialization} based on blob pages.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class BlobSerialization extends RawAbstractBlockSerialization implements IBlobSerialization {
    private static final IMessages messages = Messages.get(IMessages.class);
    private final Blob blob;
    private long pageIndex;
    private long nextPageIndex;

    public BlobSerialization(Blob blob) {
        super(0, 0);

        Assert.notNull(blob);
        Assert.isTrue(!blob.getStore().isReadOnly());

        this.blob = blob;
        setPosition(blob.getEndPosition());
    }

    @Override
    public IBlob getBlob() {
        return blob;
    }

    @Override
    public long getPosition() {
        return Blobs.getPosition(blob.getPageSize(), pageIndex,
                blockOffset - (pageIndex == blob.getId() ? Blob.HEADER_SIZE : Blob.PAGE_HEADER_SIZE));
    }

    @Override
    public long getBeginPosition() {
        return blob.getBeginPosition();
    }

    @Override
    public long getEndPosition() {
        return blob.getEndPosition();
    }

    @Override
    public void setPosition(long position) {
        long pageIndex = Blobs.getPageIndex(blob.getPageSize(), position);
        int pageOffset = Blobs.getPageOffset(blob.getPageSize(), position);

        int fileIndex = blob.getSpace().getFileIndex();
        IRawTransaction transaction = blob.getSpace().getTransaction();

        this.pageIndex = pageIndex;
        IRawPage page = transaction.getPage(fileIndex, pageIndex);

        IRawReadRegion region = page.getWriteRegion();

        if (pageIndex != blob.getId()) {
            Assert.isTrue(pageOffset + Blob.PAGE_HEADER_SIZE <= page.getSize());

            short value = region.readShort(0);
            if (value != Blob.PAGE_MAGIC)
                throw new RawDatabaseException(messages.invalidFormat(fileIndex));

            nextPageIndex = region.readLong(Blob.PAGE_NEXT_PAGE_INDEX_OFFSET);

            blockOffset = Blob.PAGE_HEADER_SIZE + pageOffset;
            blockSize = page.getSize();

            setRegion(region);
            Assert.checkState(blockSize <= region.getLength());
        } else {
            Assert.isTrue(pageOffset + Blob.HEADER_SIZE <= page.getSize());

            short value = region.readShort(0);
            if (value != Blob.MAGIC)
                throw new RawDatabaseException(messages.invalidFormat(fileIndex));

            nextPageIndex = region.readLong(Blob.NEXT_PAGE_INDEX_OFFSET);

            blockOffset = Blob.HEADER_SIZE + pageOffset;
            blockSize = page.getSize();

            setRegion(region);
            Assert.checkState(blockSize <= region.getLength());
        }
    }

    @Override
    public void removeRest() {
        Assert.checkState(pageIndex != -1);

        blob.freeRest(getPosition());
        nextPageIndex = 0;
    }

    @Override
    public void updateEndPosition() {
        blob.setEndPosition(getPosition());
    }

    @Override
    protected void nextReadRegion() {
        nextRegion(true);
    }

    @Override
    protected void nextWriteRegion() {
        nextRegion(false);
    }

    @Override
    protected void debug(int offset, int length) {
        RawDbDebug.debug(blob.getSpace().getFileIndex(), pageIndex, offset, length);
    }

    private void nextRegion(boolean read) {
        Assert.checkState(pageIndex != -1);

        if (nextPageIndex == 0) {
            if (read) {
                pageIndex = -1;
                throw new EndOfStreamException();
            } else
                nextPageIndex = blob.allocatePage();
        }

        setPosition(Blobs.getPosition(blob.getPageSize(), nextPageIndex, 0));
    }

    private interface IMessages {
        @DefaultMessage("Invalid format of file ''{0}''.")
        ILocalizedMessage invalidFormat(int fileIndex);
    }
}
