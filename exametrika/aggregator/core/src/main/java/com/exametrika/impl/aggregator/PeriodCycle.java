/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.exametrika.api.aggregator.IPeriodCycle;
import com.exametrika.api.aggregator.IPeriodNameManager;
import com.exametrika.api.exadb.core.IBatchControl;
import com.exametrika.api.exadb.core.IDumpContext;
import com.exametrika.common.json.JsonWriter;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.rawdb.IRawTransaction;
import com.exametrika.common.rawdb.RawBindInfo;
import com.exametrika.common.rawdb.RawDatabaseException;
import com.exametrika.common.rawdb.impl.RawPageDeserialization;
import com.exametrika.common.rawdb.impl.RawPageSerialization;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.Pair;
import com.exametrika.common.utils.Times;
import com.exametrika.impl.aggregator.forecast.IBehaviorTypeIdAllocator;
import com.exametrika.impl.aggregator.schema.CycleSchema;
import com.exametrika.impl.exadb.core.Constants;
import com.exametrika.impl.exadb.core.Spaces;
import com.exametrika.impl.exadb.core.ops.DumpContext;


/**
 * The {@link PeriodCycle} is a period cycle.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class PeriodCycle implements IPeriodCycle {
    public static final int HEADER_SIZE = 35;
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final byte ARCHIVED = 0x1;
    private static final byte DELETED = 0x2;
    private static final byte RESTORED = 0x4;
    public static final short MAGIC = 0x1709;// magic(short) + flags(byte) + prevCycleFileOffset(long) + startTime(long)
    private final CycleSchema schema;        // endTime(long) + dataFileIndex(int) + cycleSpaceFileIndex(int)
    private final long fileOffset;
    private byte flags;
    private long prevCycleFileOffset;
    private long startTime;
    private long endTime;
    private int dataFileIndex;
    private int cycleSpaceFileIndex;
    private String id;
    private PeriodSpace space;
    private PeriodCycle previousCycle;

    public static PeriodCycle create(CycleSchema schema, long fileOffset, long prevCycleFileOffset, PeriodCycle previousCycle,
                                     long startTime, int dataFileIndex, int cycleSpaceFileIndex) {
        PeriodCycle cycle = new PeriodCycle(schema, fileOffset);

        cycle.flags = 0;
        cycle.prevCycleFileOffset = prevCycleFileOffset;
        cycle.previousCycle = previousCycle;
        cycle.startTime = startTime;
        cycle.endTime = 0;
        cycle.dataFileIndex = dataFileIndex;
        cycle.cycleSpaceFileIndex = cycleSpaceFileIndex;
        cycle.id = PeriodSpaces.getPeriodSpacePrefix(schema.getParent().getParent().getConfiguration().getName(), schema.getParent().getConfiguration(),
                schema.getConfiguration(), dataFileIndex);

        cycle.writeHeader();

        return cycle;
    }

    public static PeriodCycle open(CycleSchema schema, long fileOffset) {
        PeriodCycle cycle = new PeriodCycle(schema, fileOffset);
        cycle.readHeader();

        return cycle;
    }

    public int getDataFileIndex() {
        return dataFileIndex;
    }

    public int getCycleSpaceFileIndex() {
        return cycleSpaceFileIndex;
    }

    public int getForecastSpaceFileIndex() {
        getSpace();
        if (space != null && space.getForecasterSpace() != null)
            return space.getForecasterSpace().getFileIndex();
        else
            return 0;
    }

    public int getAnomalyDetectorSpaceFileIndex() {
        getSpace();
        if (space != null && space.getAnomalyDetectorSpace() != null)
            return space.getAnomalyDetectorSpace().getFileIndex();
        else
            return 0;
    }

    public int getFastAnomalyDetectorSpaceFileIndex() {
        getSpace();
        if (space != null && space.getFastAnomalyDetectorSpace() != null)
            return space.getFastAnomalyDetectorSpace().getFileIndex();
        else
            return 0;
    }

    public long getFileOffset() {
        return fileOffset;
    }

    public void setSpace(PeriodSpace space) {
        Assert.notNull(space);
        Assert.isNull(this.space);

        this.space = space;
    }

    public void setArchived() {
        flags |= ARCHIVED;
        writeHeader();
    }

    public void setRestored() {
        flags |= RESTORED;
        flags &= ~DELETED;
        writeHeader();
    }

    public boolean close(ClosePeriodBatchOperation batch, IBatchControl batchControl, boolean schemaChange) {
        PeriodSpace space = getSpace();
        if (!space.close(batch, batchControl, schemaChange))
            return false;

        if (space.getCurrentPeriod() != null)
            endTime = space.getCurrentPeriod().getEndTime();
        else if (batch != null)
            endTime = batch.getCurrentTime();
        else
            endTime = Times.getCurrentTime();

        writeHeader();

        return true;
    }

    public void onTransactionStarted() {
        if (space != null)
            space.onTransactionStarted();
    }

    public void onTransactionCommitted() {
        if (space != null)
            space.onTransactionCommitted();
    }

    public boolean onBeforeTransactionRolledBack() {
        if (space != null)
            return space.onBeforeTransactionRolledBack();
        else
            return false;
    }

    public void onTransactionRolledBack() {
        if (space != null)
            space.onTransactionRolledBack();
    }

    public void unload() {
        if (space != null)
            space.unload();
    }

    public PeriodSpace delete() {
        if (isDeleted())
            return null;

        getSpace();

        if (space.getFullTextIndex() != null)
            space.getFullTextIndex().getIndex().deleteFiles();

        PeriodSpace res = space;
        res.unload();
        space = null;

        IRawTransaction transaction = schema.getContext().getTransactionProvider().getRawTransaction();
        Assert.checkState(!transaction.isReadOnly());

        transaction.getFile(dataFileIndex).delete();
        transaction.getFile(cycleSpaceFileIndex).delete();

        removeSpaceFiles(transaction, Spaces.getSpaceFilesDirName(id));
        removeSpaceFiles(transaction, Spaces.getSpaceIndexesDirName(id));

        flags |= DELETED;
        writeHeader();

        return res;
    }

    @Override
    public CycleSchema getSchema() {
        return schema;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public boolean isArchived() {
        return (flags & ARCHIVED) != 0;
    }

    @Override
    public boolean isDeleted() {
        return (flags & DELETED) != 0;
    }

    @Override
    public boolean isRestored() {
        return (flags & RESTORED) != 0;
    }

    @Override
    public long getStartTime() {
        return startTime;
    }

    @Override
    public long getEndTime() {
        return endTime;
    }

    @Override
    public PeriodCycle getPreviousCycle() {
        if (previousCycle == null && prevCycleFileOffset != 0)
            previousCycle = open(schema, prevCycleFileOffset);

        return previousCycle;
    }

    @Override
    public PeriodSpace getSpace() {
        if (space != null)
            return space;
        else
            return openSpace();
    }

    public List<String> beginSnapshot() {
        if (space != null)
            return space.beginSnapshot();
        else
            return Collections.emptyList();
    }

    public void endSnapshot() {
        if (space != null)
            space.endSnapshot();
    }

    public void dump(File path, IDumpContext context) {
        PeriodSpace space = getSpace();
        if (space == null)
            return;

        String filePrefix = PeriodSpaces.getPeriodSpacePrefix(schema.getParent().getParent().getConfiguration().getName(),
                schema.getParent().getConfiguration(), schema.getConfiguration(), dataFileIndex) + ".json";
        Writer writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(new File(path, filePrefix)));

            JsonWriter jsonWriter = new JsonWriter(writer, 4);
            jsonWriter.startText();
            jsonWriter.startObject();

            space.dump(jsonWriter, (DumpContext) context);

            jsonWriter.endObject();
            jsonWriter.endText();
        } catch (IOException e) {
            Exceptions.wrapAndThrow(e);
        } finally {
            IOs.close(writer);
        }
    }

    private PeriodCycle(CycleSchema schema, long fileOffset) {
        Assert.notNull(schema);

        this.schema = schema;
        this.fileOffset = fileOffset;
    }

    public static int copyHeader(IRawTransaction transaction, long srcFileOffset, long destFileOffset) {
        RawPageDeserialization deserialization = new RawPageDeserialization(transaction,
                0, Constants.pageIndexByFileOffset(srcFileOffset), Constants.pageOffsetByFileOffset(srcFileOffset));

        short magic = deserialization.readShort();

        if (magic != MAGIC)
            throw new RawDatabaseException(messages.invalidFormat(deserialization.getFileIndex()));

        byte flags = deserialization.readByte();
        long prevCycleFileOffset = deserialization.readLong();
        long startTime = deserialization.readLong();
        long endTime = deserialization.readLong();
        int dataFileIndex = deserialization.readInt();
        int cycleSpaceFileIndex = deserialization.readInt();

        RawPageSerialization serialization = new RawPageSerialization(transaction,
                0, Constants.pageIndexByFileOffset(destFileOffset), Constants.pageOffsetByFileOffset(destFileOffset));
        serialization.writeShort(MAGIC);
        serialization.writeByte(flags);
        serialization.writeLong(prevCycleFileOffset);
        serialization.writeLong(startTime);
        serialization.writeLong(endTime);
        serialization.writeInt(dataFileIndex);
        serialization.writeInt(cycleSpaceFileIndex);

        return dataFileIndex;
    }

    private void writeHeader() {
        RawPageSerialization serialization = new RawPageSerialization(schema.getContext().getTransactionProvider().getRawTransaction(),
                0, Constants.pageIndexByFileOffset(fileOffset), Constants.pageOffsetByFileOffset(fileOffset));
        serialization.writeShort(MAGIC);
        serialization.writeByte(flags);
        serialization.writeLong(prevCycleFileOffset);
        serialization.writeLong(startTime);
        serialization.writeLong(endTime);
        serialization.writeInt(dataFileIndex);
        serialization.writeInt(cycleSpaceFileIndex);
    }

    private void readHeader() {
        RawPageDeserialization deserialization = new RawPageDeserialization(schema.getContext().getTransactionProvider().getRawTransaction(),
                0, Constants.pageIndexByFileOffset(fileOffset), Constants.pageOffsetByFileOffset(fileOffset));

        short magic = deserialization.readShort();

        if (magic != MAGIC)
            throw new RawDatabaseException(messages.invalidFormat(deserialization.getFileIndex()));

        flags = deserialization.readByte();
        prevCycleFileOffset = deserialization.readLong();
        startTime = deserialization.readLong();
        endTime = deserialization.readLong();
        dataFileIndex = deserialization.readInt();
        cycleSpaceFileIndex = deserialization.readInt();
        id = PeriodSpaces.getPeriodSpacePrefix(schema.getParent().getParent().getConfiguration().getName(), schema.getParent().getConfiguration(),
                schema.getConfiguration(), dataFileIndex);
    }

    private PeriodSpace openSpace() {
        if (!isDeleted()) {
            IBehaviorTypeIdAllocator typeIdAllocator = schema.getContext().getTransactionProvider().getTransaction(
            ).findExtension(IPeriodNameManager.NAME);
            space = PeriodSpace.open(schema.getContext(), dataFileIndex, cycleSpaceFileIndex,
                    schema, this, schema.getNodeManager(), schema.getNodeCacheManager(), fileOffset + HEADER_SIZE, typeIdAllocator);
            return space;
        } else
            return null;
    }

    private void removeSpaceFiles(IRawTransaction transaction, String dir) {
        List<String> paths = schema.getContext().getConfiguration().getPaths();

        for (int i = 0; i < paths.size(); i++) {
            String path = paths.get(i);
            File file = new File(path, dir);
            if (file.exists()) {
                List<Pair<String, Integer>> spaceFiles = new ArrayList<Pair<String, Integer>>();
                Spaces.getSpaceFiles(file, path, spaceFiles);
                for (Pair<String, Integer> pair : spaceFiles) {
                    RawBindInfo info = new RawBindInfo();
                    info.setName(pair.getKey());
                    info.setPathIndex(i);
                    info.setFlags(RawBindInfo.TEMPORARY);
                    transaction.bindFile(pair.getValue(), info).delete();
                }
            }
        }
    }

    private interface IMessages {
        @DefaultMessage("Invalid format of file ''{0}''.")
        ILocalizedMessage invalidFormat(int fileIndex);
    }
}
