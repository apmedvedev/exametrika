/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.fields;

import com.exametrika.api.exadb.objectdb.config.schema.BlobStoreFieldSchemaConfiguration;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.rawdb.IRawPage;
import com.exametrika.common.rawdb.IRawTransaction;
import com.exametrika.common.rawdb.IRawWriteRegion;
import com.exametrika.common.rawdb.RawDatabaseException;
import com.exametrika.common.rawdb.impl.RawPageDeserialization;
import com.exametrika.common.rawdb.impl.RawPageSerialization;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.exadb.core.Constants;
import com.exametrika.spi.exadb.objectdb.INodeObject;
import com.exametrika.spi.exadb.objectdb.fields.IBlob;


/**
 * The {@link BlobSpace} is a blob space containing blobs of particular blob store.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class BlobSpace {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final short MAGIC = 0x170A;// magic(short) + version(byte) + nextPageIndex(long) + freeBlockFileOffset(long) + freeSpace(long)
    private static final int NEXT_PAGE_INDEX_OFFSET = 3;
    private static final int FREE_BLOCK_FILE_OFFSET_OFFSET = 11;
    private static final int FREE_SPACE_OFFSET = 19;
    private static final short BLOCK_MAGIC = 0x170D;// magic(short) + flags(byte) + padding(5 byte) + nextFreeBlockFileOffset(long)
    private static final int BLOCK_SIZE = 1024;
    public static final int BLOCK_ELEMENT_COUNT = 126;
    private static final int BLOCK_HEADER_SIZE = 16;
    private static final byte BLOCK_FLAG_FULL = 0x1;
    private static final int BLOCK_FLAGS_OFFSET = 2;
    private static final int BLOCK_NEXT_FREE_BLOCK_FILE_OFFSET_OFFSET = 8;
    private final INodeObject node;
    private final int fieldIndex;
    private final int fileIndex;
    private IRawPage headerPage;

    public static BlobSpace create(BlobStoreField store, int fileIndex) {
        BlobSpace space = new BlobSpace(store, fileIndex);
        space.writeHeader();

        return space;
    }

    public static BlobSpace open(BlobStoreField store, int fileIndex) {
        BlobSpace space = new BlobSpace(store, fileIndex);
        space.readHeader();

        return space;
    }

    public int getFileIndex() {
        return fileIndex;
    }

    public BlobStoreField getStore() {
        return node.getNode().getField(fieldIndex);
    }

    public IRawTransaction getTransaction() {
        return node.getNode().getRawTransaction();
    }

    public long getFreeSpace() {
        return headerPage.getReadRegion().readLong(FREE_SPACE_OFFSET);
    }

    public IBlob createBlob() {
        return Blob.create(this);
    }

    public IBlob openBlob(long blobId) {
        return Blob.open(this, blobId);
    }

    public long allocatePage() {
        IRawWriteRegion headerRegion = headerPage.getWriteRegion();

        BlobStoreField store = getStore();
        BlobStoreFieldSchemaConfiguration configuration = (BlobStoreFieldSchemaConfiguration) store.getSchema().getConfiguration();

        long pageIndex = 0;
        if (store.allowDeletion()) {
            long pageCount = configuration.getMaxFileSize() / Constants.PAGE_SIZE;
            long blockCount = pageCount / BLOCK_ELEMENT_COUNT / 64;
            int headerPageCount = (int) ((blockCount + 1) * BLOCK_SIZE / Constants.PAGE_SIZE);
            if (((blockCount + 1) * BLOCK_SIZE % Constants.PAGE_SIZE) != 0)
                headerPageCount++;

            long freeBlockFileOffset = headerRegion.readLong(FREE_BLOCK_FILE_OFFSET_OFFSET);
            if (freeBlockFileOffset == 0)
                throw new RawDatabaseException(messages.blobStoreFull(fileIndex));

            long blockIndex = freeBlockFileOffset / BLOCK_SIZE - 1;

            RawPageSerialization serialization = new RawPageSerialization(getTransaction(), fileIndex, freeBlockFileOffset / headerPage.getSize(),
                    (int) (freeBlockFileOffset % headerPage.getSize()) + BLOCK_HEADER_SIZE);

            boolean full = true;
            for (int i = 0; i < BLOCK_ELEMENT_COUNT; i++) {
                long element = serialization.readLong();
                if (element != -1 && pageIndex == 0) {
                    long mask = 1;
                    for (int k = 0; k < 64; k++) {
                        if ((element & mask) == 0) {
                            element |= mask;
                            pageIndex = headerPageCount + blockIndex * BLOCK_ELEMENT_COUNT * 64 + i * 64 + k;

                            serialization.setPosition(serialization.getPageIndex(), serialization.getPageOffset() - 8);
                            serialization.writeLong(element);
                            break;
                        }

                        mask <<= 1;
                    }
                }

                if (element != -1) {
                    full = false;
                    break;
                }
            }

            Assert.checkState(pageIndex != 0);

            if (full) {
                serialization.setPosition(serialization.getPageIndex(), (int) (freeBlockFileOffset % headerPage.getSize()) + BLOCK_FLAGS_OFFSET);
                serialization.writeByte(BLOCK_FLAG_FULL);
                serialization.readByte();
                serialization.readInt();
                long nextFreeBlockFileOffset = serialization.readLong();
                headerRegion.writeLong(FREE_BLOCK_FILE_OFFSET_OFFSET, nextFreeBlockFileOffset);
            }
        } else {
            pageIndex = headerRegion.readLong(NEXT_PAGE_INDEX_OFFSET);
            if ((pageIndex + 1) * headerPage.getSize() > configuration.getMaxFileSize())
                throw new RawDatabaseException(messages.blobStoreFull(fileIndex));

            headerRegion.writeLong(NEXT_PAGE_INDEX_OFFSET, pageIndex + 1);
        }

        long freeSpace = headerRegion.readLong(FREE_SPACE_OFFSET);
        headerRegion.writeLong(FREE_SPACE_OFFSET, freeSpace - headerPage.getSize());

        return pageIndex;
    }

    public void freePage(long pageIndex) {
        BlobStoreField store = getStore();
        Assert.supports(store.allowDeletion());

        IRawWriteRegion headerRegion = headerPage.getWriteRegion();

        BlobStoreFieldSchemaConfiguration configuration = (BlobStoreFieldSchemaConfiguration) store.getSchema().getConfiguration();
        long pageCount = configuration.getMaxFileSize() / Constants.PAGE_SIZE;
        long blockCount = pageCount / BLOCK_ELEMENT_COUNT / 64;
        int headerPageCount = (int) ((blockCount + 1) * BLOCK_SIZE / Constants.PAGE_SIZE);
        if (((blockCount + 1) * BLOCK_SIZE % Constants.PAGE_SIZE) != 0)
            headerPageCount++;

        pageIndex -= headerPageCount;
        int blockIndex = (int) (pageIndex / BLOCK_ELEMENT_COUNT / 64);
        long blockFileOffset = (blockIndex + 1) * BLOCK_SIZE;
        int elementIndex = (int) ((pageIndex / 64) % BLOCK_ELEMENT_COUNT);
        int bitIndex = (int) (pageIndex % 64);

        IRawPage page = getTransaction().getPage(fileIndex, blockFileOffset / headerPage.getSize());
        IRawWriteRegion region = page.getWriteRegion();
        int blockOffset = (int) (blockFileOffset % page.getSize());
        if ((region.readByte(blockOffset + BLOCK_FLAGS_OFFSET) & BLOCK_FLAG_FULL) != 0) {
            region.writeByte(blockOffset + BLOCK_FLAGS_OFFSET, (byte) 0);

            long freeBlockIndexOffset = headerRegion.readLong(FREE_BLOCK_FILE_OFFSET_OFFSET);
            headerRegion.writeLong(FREE_BLOCK_FILE_OFFSET_OFFSET, blockFileOffset);

            region.writeLong(blockOffset + BLOCK_NEXT_FREE_BLOCK_FILE_OFFSET_OFFSET, freeBlockIndexOffset);
        }

        int elementOffset = blockOffset + BLOCK_HEADER_SIZE + elementIndex * 8;
        long element = region.readLong(elementOffset);
        region.writeLong(elementOffset, element & ~(1 << bitIndex));

        long freeSpace = headerRegion.readLong(FREE_SPACE_OFFSET);
        headerRegion.writeLong(FREE_SPACE_OFFSET, freeSpace + page.getSize());
    }

    private BlobSpace(BlobStoreField store, int fileIndex) {
        Assert.notNull(store);
        Assert.isTrue(fileIndex != 0);

        this.node = store.getNode().getObject();
        this.fieldIndex = store.getSchema().getIndex();
        this.fileIndex = fileIndex;
        this.headerPage = getTransaction().getPage(fileIndex, 0);
    }

    private void readHeader() {
        RawPageDeserialization deserialization = new RawPageDeserialization(getTransaction(), fileIndex, headerPage, 0);

        short magic = deserialization.readShort();
        byte version = deserialization.readByte();

        if (magic != MAGIC)
            throw new RawDatabaseException(messages.invalidFormat(deserialization.getFileIndex()));
        if (version != Constants.VERSION)
            throw new RawDatabaseException(messages.unsupportedVersion(deserialization.getFileIndex(), version, Constants.VERSION));
    }

    private void writeHeader() {
        RawPageSerialization serialization = new RawPageSerialization(getTransaction(), fileIndex, headerPage, 0);

        serialization.writeShort(MAGIC);
        serialization.writeByte(Constants.VERSION);

        BlobStoreField store = getStore();
        BlobStoreFieldSchemaConfiguration configuration = (BlobStoreFieldSchemaConfiguration) store.getSchema().getConfiguration();

        if (store.allowDeletion()) {
            long pageCount = configuration.getMaxFileSize() / Constants.PAGE_SIZE;
            long blockCount = pageCount / BLOCK_ELEMENT_COUNT / 64;
            int headerPageCount = (int) ((blockCount + 1) * BLOCK_SIZE / Constants.PAGE_SIZE);
            if (((blockCount + 1) * BLOCK_SIZE % Constants.PAGE_SIZE) != 0)
                headerPageCount++;
            serialization.writeLong(headerPageCount);
            serialization.writeLong(BLOCK_SIZE);
            serialization.writeLong(configuration.getMaxFileSize() - headerPageCount * Constants.PAGE_SIZE);

            serialization.setPosition(0, BLOCK_SIZE);
            for (int i = 0; i < blockCount; i++) {
                serialization.writeShort(BLOCK_MAGIC);
                serialization.writeByte((byte) 0);
                serialization.writeByte((byte) 0);
                serialization.writeInt(0);
                if (i < blockCount - 1)
                    serialization.writeLong(BLOCK_SIZE * (i + 2));
                else
                    serialization.writeLong(0);

                for (int k = 0; k < BLOCK_ELEMENT_COUNT; k++)
                    serialization.writeLong(0);
            }
        } else {
            serialization.writeLong(1);
            serialization.writeLong(0);
            serialization.writeLong(configuration.getMaxFileSize() - Constants.PAGE_SIZE);
        }
    }

    private interface IMessages {
        @DefaultMessage("Invalid format of file ''{0}''.")
        ILocalizedMessage invalidFormat(int fileIndex);

        @DefaultMessage("Blob store file ''{0}'' is full.")
        ILocalizedMessage blobStoreFull(int fileIndex);

        @DefaultMessage("Unsupported version ''{1}'' of file ''{0}'', expected version - ''{2}''.")
        ILocalizedMessage unsupportedVersion(int fileIndex, int fileVersion, int expectedVersion);
    }
}
