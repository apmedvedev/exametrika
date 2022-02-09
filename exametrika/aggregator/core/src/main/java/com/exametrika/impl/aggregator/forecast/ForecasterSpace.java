/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.forecast;

import java.util.ArrayList;

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
import com.exametrika.common.utils.Collections;
import com.exametrika.common.utils.MapBuilder;
import com.exametrika.common.utils.Pair;
import com.exametrika.impl.aggregator.PeriodSpaces;
import com.exametrika.impl.aggregator.forecast.Forecaster.Parameters;
import com.exametrika.impl.aggregator.schema.CycleSchema;
import com.exametrika.impl.exadb.core.Constants;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.spi.exadb.core.ITransactionProvider;


/**
 * The {@link ForecasterSpace} is a forecast space.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class ForecasterSpace implements IForecasterSpace {
    public static final int HEADER_SIZE = 8;
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final short MAGIC = 0x171F;// magic(short) + version(byte) + nextPageIndex(int) + padding(byte)
    private static final int NEXT_PAGE_INDEX_OFFSET = 3;
    private final int fileIndex;
    private IRawPage headerPage;
    private ITransactionProvider transactionProvider;
    private final ArrayList<Forecaster> forecasters = new ArrayList<Forecaster>();
    private final Dtw state = new Dtw(Forecaster.FORECAST_WINDOW_SIZE, Forecaster.FORECAST_WARPING_BAND, Forecaster.FORECAST_MAX_ELEMENT_COUNT);
    private final IBehaviorTypeIdAllocator typeIdAllocator;
    private int startIndex;

    public static ForecasterSpace create(IDatabaseContext context, int fileIndex, String filePrefix, CycleSchema schema, int pageTypeIndex,
                                         IBehaviorTypeIdAllocator typeIdAllocator) {
        Assert.notNull(context);
        Assert.notNull(filePrefix);
        Assert.notNull(schema);

        IRawTransaction transaction = context.getTransactionProvider().getRawTransaction();
        bindFile(schema, transaction, fileIndex, filePrefix, schema.getParent().getConfiguration().getPathIndex(), pageTypeIndex);

        ForecasterSpace space = new ForecasterSpace(context.getTransactionProvider(), fileIndex, typeIdAllocator);
        space.writeHeader();

        return space;
    }

    public static ForecasterSpace open(IDatabaseContext context, int fileIndex, String filePrefix, CycleSchema schema, int pageTypeIndex,
                                       IBehaviorTypeIdAllocator typeIdAllocator) {
        Assert.notNull(context);
        Assert.notNull(filePrefix);
        Assert.notNull(schema);

        IRawTransaction transaction = context.getTransactionProvider().getRawTransaction();
        bindFile(schema, transaction, fileIndex, filePrefix, schema.getParent().getConfiguration().getPathIndex(), pageTypeIndex);

        ForecasterSpace space = new ForecasterSpace(context.getTransactionProvider(), fileIndex, typeIdAllocator);
        space.readHeader();

        return space;
    }

    public int getFileIndex() {
        return fileIndex;
    }

    @Override
    public Forecaster createForecaster(Parameters parameters) {
        IRawWriteRegion headerRegion = headerPage.getWriteRegion();

        int pageIndex = headerRegion.readInt(NEXT_PAGE_INDEX_OFFSET);
        headerRegion.writeInt(NEXT_PAGE_INDEX_OFFSET, pageIndex + 1);

        Forecaster forecaster = Forecaster.create(transactionProvider.getRawTransaction().getPage(fileIndex, pageIndex),
                parameters, state, typeIdAllocator);
        Assert.isNull(Collections.get(forecasters, pageIndex));
        Collections.set(forecasters, pageIndex, forecaster);
        return forecaster;
    }

    @Override
    public Forecaster openForecaster(int id, Parameters parameters) {
        Forecaster forecaster = Collections.get(forecasters, id);
        if (forecaster == null) {
            forecaster = Forecaster.open(transactionProvider.getRawTransaction().getPage(fileIndex, id),
                    parameters, state, typeIdAllocator);
            Collections.set(forecasters, id, forecaster);
        }
        return forecaster;
    }

    public void onTransactionStarted() {
        startIndex = forecasters.size();
    }

    public void onTransactionCommitted() {
        startIndex = forecasters.size();
    }

    public void onTransactionRolledBack() {
        int count = forecasters.size();
        for (int i = startIndex; i < count; i++)
            forecasters.remove(forecasters.size() - 1);
    }

    private ForecasterSpace(ITransactionProvider transactionProvider, int fileIndex, IBehaviorTypeIdAllocator typeIdAllocator) {
        Assert.isTrue(fileIndex != 0);
        Assert.notNull(typeIdAllocator);

        this.transactionProvider = transactionProvider;
        this.fileIndex = fileIndex;
        this.headerPage = transactionProvider.getRawTransaction().getPage(fileIndex, 0);
        this.typeIdAllocator = typeIdAllocator;
    }

    private static void bindFile(CycleSchema schema, IRawTransaction transaction, int fileIndex, String filePrefix, int pathIndex,
                                 int pageTypeIndex) {
        RawBindInfo bindInfo = new RawBindInfo();
        bindInfo.setPathIndex(pathIndex);
        bindInfo.setName(PeriodSpaces.getForecastSpaceFileName(filePrefix, fileIndex));
        bindInfo.setPageTypeIndex(pageTypeIndex);

        String domainName = schema.getParent().getParent().getConfiguration().getName();
        Pair<String, String> pair = schema.getContext().getCacheCategorizationStrategy().categorize(new MapBuilder<String, String>()
                .put("type", "pages.data.period.forecast")
                .put("spaceName", schema.getParent().getConfiguration().getName())
                .put("periodName", schema.getConfiguration().getName())
                .put("domainName", domainName)
                .put("name", domainName + "." + schema.getParent().getConfiguration().getName() + "." +
                        schema.getConfiguration().getName() + ".forecast")
                .toMap());
        bindInfo.setCategory(pair.getKey());
        bindInfo.setCategoryType(pair.getValue());

        transaction.bindFile(fileIndex, bindInfo);
    }

    private void readHeader() {
        RawPageDeserialization deserialization = new RawPageDeserialization(transactionProvider.getRawTransaction(), fileIndex, headerPage, 0);

        short magic = deserialization.readShort();
        byte version = deserialization.readByte();

        if (magic != MAGIC)
            throw new RawDatabaseException(messages.invalidFormat(deserialization.getFileIndex()));
        if (version != Constants.VERSION)
            throw new RawDatabaseException(messages.unsupportedVersion(deserialization.getFileIndex(), version, Constants.VERSION));
    }

    private void writeHeader() {
        RawPageSerialization serialization = new RawPageSerialization(transactionProvider.getRawTransaction(), fileIndex, headerPage, 0);

        serialization.writeShort(MAGIC);
        serialization.writeByte(Constants.VERSION);

        serialization.writeInt(0);
    }

    private interface IMessages {
        @DefaultMessage("Invalid format of file ''{0}''.")
        ILocalizedMessage invalidFormat(int fileIndex);

        @DefaultMessage("Unsupported version ''{1}'' of file ''{0}'', expected version - ''{2}''.")
        ILocalizedMessage unsupportedVersion(int fileIndex, int fileVersion, int expectedVersion);
    }
}
