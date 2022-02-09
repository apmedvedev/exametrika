/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.rawdb.IRawPage;
import com.exametrika.common.rawdb.IRawTransaction;
import com.exametrika.common.rawdb.IRawWriteRegion;
import com.exametrika.common.rawdb.RawBindInfo;
import com.exametrika.common.rawdb.RawDatabaseException;
import com.exametrika.common.rawdb.impl.RawPageDeserialization;
import com.exametrika.common.rawdb.impl.RawPageSerialization;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.MapBuilder;
import com.exametrika.common.utils.Pair;
import com.exametrika.impl.aggregator.schema.CycleSchema;
import com.exametrika.impl.exadb.core.Constants;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.spi.exadb.core.ITransactionProvider;


/**
 * The {@link CyclePeriodSpace} is a space for cycle period.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class CyclePeriodSpace {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final short MAGIC = 0x171D;
    private static final int HEADER_SIZE = 12;
    private static final int CLOSED_OFFSET = 3;
    private static final int NEXT_BLOCK_INDEX_OFFSET = 4;
    private final ITransactionProvider transactionProvider;
    private final PeriodSpace periodSpace;
    private final int fileIndex;
    private final String fileName;
    private IRawPage headerPage;
    private boolean closed;// magic(short) + version(byte) + closed(byte) + nextBlockIndex(long)
    private CyclePeriod period;

    public static CyclePeriodSpace create(IDatabaseContext context, int fileIndex, String filePrefix, CycleSchema schema,
                                          PeriodSpace periodSpace) {
        Assert.notNull(context);
        Assert.notNull(filePrefix);
        Assert.notNull(schema);
        Assert.notNull(periodSpace);

        String fileName = PeriodSpaces.getCycleSpaceFileName(filePrefix, fileIndex);
        IRawTransaction transaction = context.getTransactionProvider().getRawTransaction();
        bindFile(schema, transaction, fileIndex, fileName, schema.getParent().getConfiguration().getPathIndex());

        CyclePeriodSpace space = new CyclePeriodSpace(context, fileIndex, fileName, periodSpace);
        space.onCreated();

        return space;
    }

    public static CyclePeriodSpace open(IDatabaseContext context, int fileIndex, String filePrefix, CycleSchema schema,
                                        PeriodSpace periodSpace) {
        Assert.notNull(context);
        Assert.notNull(filePrefix);
        Assert.notNull(schema);
        Assert.notNull(periodSpace);

        String fileName = PeriodSpaces.getCycleSpaceFileName(filePrefix, fileIndex);

        IRawTransaction transaction = context.getTransactionProvider().getRawTransaction();
        bindFile(schema, transaction, fileIndex, fileName, schema.getParent().getConfiguration().getPathIndex());

        CyclePeriodSpace space = new CyclePeriodSpace(context, fileIndex, fileName, periodSpace);
        space.onOpened();
        return space;
    }

    public PeriodSpace getPeriodSpace() {
        return periodSpace;
    }

    public int getFileIndex() {
        return fileIndex;
    }

    public String getFileName() {
        return fileName;
    }

    public Period getPeriod() {
        return period;
    }

    public long allocateBlocks(int blockCount) {
        Assert.checkState(!closed);
        Assert.isTrue(blockCount <= Constants.BLOCKS_PER_PAGE_COUNT);

        IRawWriteRegion region = headerPage.getWriteRegion();
        long nextBlockIndex = region.readInt(NEXT_BLOCK_INDEX_OFFSET);
        int pageOffset = Constants.pageOffsetByBlockIndex(nextBlockIndex);
        if (pageOffset + Constants.dataSize(blockCount) > Constants.PAGE_SIZE) {
            IRawPage page = transactionProvider.getRawTransaction().getPage(fileIndex, Constants.pageIndexByBlockIndex(nextBlockIndex) + 1);
            nextBlockIndex = Constants.blockIndex(page.getIndex(), 0);
        }
        region.writeLong(NEXT_BLOCK_INDEX_OFFSET, nextBlockIndex + blockCount);

        return nextBlockIndex;
    }

    public void close(long currentTime, boolean schemaChange) {
        Assert.checkState(!closed);

        period.close(null, null, currentTime, schemaChange);
        closed = true;

        headerPage.getWriteRegion().writeByte(CLOSED_OFFSET, (byte) 1);
    }

    private CyclePeriodSpace(IDatabaseContext context, int fileIndex, String fileName, PeriodSpace periodSpace) {
        this.transactionProvider = context.getTransactionProvider();
        this.fileIndex = fileIndex;
        this.fileName = fileName;
        this.periodSpace = periodSpace;
        this.headerPage = transactionProvider.getRawTransaction().getPage(fileIndex, 0);
    }

    private void onCreated() {
        closed = false;
        long firstPeriodBlockIndex = Constants.blockIndex(Constants.alignBlock(HEADER_SIZE));
        writeHeader(firstPeriodBlockIndex);
        createPeriod();
    }

    private void onOpened() {
        readHeader();
        readPeriod();
    }

    private void readHeader() {
        IRawTransaction transaction = transactionProvider.getRawTransaction();
        RawPageDeserialization deserialization = new RawPageDeserialization(transaction, fileIndex, headerPage, 0);

        short magic = deserialization.readShort();
        byte version = deserialization.readByte();

        if (magic != MAGIC)
            throw new RawDatabaseException(messages.invalidFormat(deserialization.getFileIndex()));
        if (version != Constants.VERSION)
            throw new RawDatabaseException(messages.unsupportedVersion(deserialization.getFileIndex(), version, Constants.VERSION));

        closed = deserialization.readBoolean();
        deserialization.readLong(); // nextBlockIndex (long) 
    }

    private void writeHeader(long nextBlockIndex) {
        RawPageSerialization serialization = new RawPageSerialization(transactionProvider.getRawTransaction(), fileIndex, headerPage, 0);

        serialization.writeShort(MAGIC);
        serialization.writeByte(Constants.VERSION);
        serialization.writeBoolean(closed);
        serialization.writeLong(nextBlockIndex);
    }

    private void createPeriod() {
        Assert.checkState(!closed);

        long periodBlockIndex = allocateBlocks(Period.HEADER_BLOCK_COUNT);
        period = new CyclePeriod(this, fileIndex, periodBlockIndex, true);
        period.createRootNode();
    }

    private void readPeriod() {
        long firstPeriodBlockIndex = Constants.blockIndex(Constants.alignBlock(HEADER_SIZE));
        period = new CyclePeriod(this, fileIndex, firstPeriodBlockIndex, false);
    }

    private static void bindFile(CycleSchema schema, IRawTransaction transaction, int fileIndex, String fileName, int pathIndex) {
        RawBindInfo bindInfo = new RawBindInfo();
        bindInfo.setPathIndex(pathIndex);
        bindInfo.setName(fileName);

        String domainName = schema.getParent().getParent().getConfiguration().getName();
        Pair<String, String> pair = schema.getContext().getCacheCategorizationStrategy().categorize(new MapBuilder<String, String>()
                .put("type", "pages.data.period.cycle")
                .put("spaceName", schema.getParent().getConfiguration().getName())
                .put("periodName", schema.getConfiguration().getName())
                .put("domainName", domainName)
                .put("name", domainName + "." + schema.getParent().getConfiguration().getName() + "." +
                        schema.getConfiguration().getName() + ".cycle")
                .toMap());
        bindInfo.setCategory(pair.getKey());
        bindInfo.setCategoryType(pair.getValue());

        transaction.bindFile(fileIndex, bindInfo);
    }

    private interface IMessages {
        @DefaultMessage("Invalid format of file ''{0}''.")
        ILocalizedMessage invalidFormat(int fileIndex);

        @DefaultMessage("Unsupported version ''{1}'' of file ''{0}'', expected version - ''{2}''.")
        ILocalizedMessage unsupportedVersion(int fileIndex, int fileVersion, int expectedVersion);
    }
}
