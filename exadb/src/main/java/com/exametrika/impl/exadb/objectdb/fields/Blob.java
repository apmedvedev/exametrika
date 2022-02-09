/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.fields;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.rawdb.RawDatabaseException;
import com.exametrika.common.rawdb.IRawPage;
import com.exametrika.common.rawdb.IRawTransaction;
import com.exametrika.common.rawdb.IRawWriteRegion;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.exadb.objectdb.fields.IBlob;
import com.exametrika.spi.exadb.objectdb.fields.IBlobDeserialization;
import com.exametrika.spi.exadb.objectdb.fields.IBlobSerialization;
import com.exametrika.spi.exadb.objectdb.fields.IBlobStoreField;


/**
 * The {@link Blob} is a blob.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class Blob implements IBlob {
    private static final IMessages messages = Messages.get(IMessages.class);
    public static final int HEADER_SIZE = 26;
    public static final short MAGIC = 0x170B;// magic(short) + nextPageIndex(long) + lastPageIndex(long) + endPosition(long)
    public static final int NEXT_PAGE_INDEX_OFFSET = 2;
    private static final int LAST_PAGE_INDEX_OFFSET = 10;
    private static final int END_POSITION_OFFSET = 18;
    public static final int PAGE_HEADER_SIZE = 10;
    public static final short PAGE_MAGIC = 0x170C;// magic(short) + nextPageIndex(long)
    public static final int PAGE_NEXT_PAGE_INDEX_OFFSET = 2;
    private final long pageIndex;
    private final int pageSize;
    private final BlobSpace space;
    private IRawPage headerPage;
    private boolean deleted;

    public static Blob create(BlobSpace space) {
        Assert.notNull(space);

        long pageIndex = space.allocatePage();
        Blob blob = new Blob(space, pageIndex);
        blob.writeHeader();

        return blob;
    }

    public static Blob open(BlobSpace space, long pageIndex) {
        Blob blob = new Blob(space, pageIndex);
        blob.readHeader();

        return blob;
    }

    public BlobSpace getSpace() {
        return space;
    }

    public int getPageSize() {
        return pageSize;
    }

    @Override
    public long getBeginPosition() {
        return Blobs.getPosition(pageSize, pageIndex, 0);
    }

    @Override
    public long getEndPosition() {
        return headerPage.getReadRegion().readLong(END_POSITION_OFFSET);
    }

    public void setEndPosition(long position) {
        headerPage.getWriteRegion().writeLong(END_POSITION_OFFSET, position);
    }

    public long allocatePage() {
        IRawTransaction transaction = space.getTransaction();
        long pageIndex = space.allocatePage();

        IRawPage page = transaction.getPage(space.getFileIndex(), pageIndex);
        IRawWriteRegion region = page.getWriteRegion();
        region.writeShort(0, PAGE_MAGIC);
        region.writeLong(PAGE_NEXT_PAGE_INDEX_OFFSET, 0);

        page = headerPage;
        region = page.getWriteRegion();

        long lastPageIndex = region.readLong(LAST_PAGE_INDEX_OFFSET);
        region.writeLong(LAST_PAGE_INDEX_OFFSET, pageIndex);

        if (lastPageIndex == this.pageIndex)
            region.writeLong(NEXT_PAGE_INDEX_OFFSET, pageIndex);
        else {
            page = transaction.getPage(space.getFileIndex(), lastPageIndex);
            region = page.getWriteRegion();
            region.writeLong(PAGE_NEXT_PAGE_INDEX_OFFSET, pageIndex);
        }

        return pageIndex;
    }

    public void freeRest(long endPosition) {
        Assert.supports(space.getStore().allowDeletion());

        if (endPosition == getEndPosition())
            return;

        IRawTransaction transaction = space.getTransaction();

        long pageIndex = Blobs.getPageIndex(pageSize, endPosition);

        headerPage.getWriteRegion().writeLong(LAST_PAGE_INDEX_OFFSET, pageIndex);
        headerPage.getWriteRegion().writeLong(END_POSITION_OFFSET, endPosition);

        long nextPageIndex;
        if (pageIndex == this.pageIndex) {
            if (!deleted) {
                IRawWriteRegion region = headerPage.getWriteRegion();
                nextPageIndex = region.readLong(NEXT_PAGE_INDEX_OFFSET);
                region.writeLong(NEXT_PAGE_INDEX_OFFSET, 0);
            } else
                nextPageIndex = headerPage.getReadRegion().readLong(NEXT_PAGE_INDEX_OFFSET);
        } else {
            IRawPage page = transaction.getPage(space.getFileIndex(), pageIndex);
            IRawWriteRegion region = page.getWriteRegion();
            nextPageIndex = region.readLong(PAGE_NEXT_PAGE_INDEX_OFFSET);
            region.writeLong(PAGE_NEXT_PAGE_INDEX_OFFSET, 0);
        }

        while (nextPageIndex != 0) {
            pageIndex = nextPageIndex;

            IRawPage page = transaction.getPage(space.getFileIndex(), pageIndex);
            nextPageIndex = page.getReadRegion().readLong(PAGE_NEXT_PAGE_INDEX_OFFSET);

            space.freePage(pageIndex);
        }
    }

    @Override
    public long getId() {
        return pageIndex;
    }

    @Override
    public IBlobStoreField getStore() {
        return space.getStore();
    }

    @Override
    public IBlobSerialization createSerialization() {
        Assert.checkState(!deleted);

        return new BlobSerialization(this);
    }

    @Override
    public IBlobDeserialization createDeserialization() {
        Assert.checkState(!deleted);

        return new BlobDeserialization(this);
    }

    @Override
    public void delete() {
        Assert.supports(space.getStore().allowDeletion());

        if (deleted)
            return;

        deleted = true;
        freeRest(Blobs.getPosition(pageSize, pageIndex, 0));
        space.freePage(pageIndex);
    }

    @Override
    public String toString() {
        return headerPage.toString();
    }

    private Blob(BlobSpace space, long pageIndex) {
        Assert.notNull(space);
        Assert.isTrue(pageIndex != 0);

        this.space = space;
        this.pageIndex = pageIndex;
        this.headerPage = space.getTransaction().getPage(space.getFileIndex(), pageIndex);
        this.pageSize = headerPage.getSize();
    }

    private void writeHeader() {
        IRawPage page = headerPage;

        IRawWriteRegion region = page.getWriteRegion();
        region.writeShort(0, MAGIC);
        region.writeLong(NEXT_PAGE_INDEX_OFFSET, 0);
        region.writeLong(LAST_PAGE_INDEX_OFFSET, pageIndex);
        region.writeLong(END_POSITION_OFFSET, Blobs.getPosition(pageSize, pageIndex, 0));
    }

    private void readHeader() {
        IRawPage page = headerPage;
        if (page.getReadRegion().readShort(0) != MAGIC)
            throw new RawDatabaseException(messages.invalidFormat(space.getFileIndex()));
    }

    private interface IMessages {
        @DefaultMessage("Invalid format of file ''{0}''.")
        ILocalizedMessage invalidFormat(int fileIndex);
    }
}
