/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.name;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.exametrika.api.aggregator.IPeriodName;
import com.exametrika.api.aggregator.Location;
import com.exametrika.api.aggregator.common.model.ICallPath;
import com.exametrika.api.aggregator.common.model.IMetricName;
import com.exametrika.api.aggregator.common.model.IName;
import com.exametrika.api.aggregator.common.model.IScopeName;
import com.exametrika.api.aggregator.config.schema.LocationKeyNormalizerSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.NameSpaceSchemaConfiguration;
import com.exametrika.api.exadb.core.IOperation;
import com.exametrika.api.exadb.index.IIndexManager;
import com.exametrika.api.exadb.index.IUniqueIndex;
import com.exametrika.api.exadb.index.config.schema.BTreeIndexSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.LongValueConverterSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.StringKeyNormalizerSchemaConfiguration;
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
import com.exametrika.impl.aggregator.common.model.CallPath;
import com.exametrika.impl.aggregator.common.model.MeasurementSerializers;
import com.exametrika.impl.aggregator.common.model.MetricName;
import com.exametrika.impl.exadb.core.Constants;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.spi.exadb.core.ITransactionProvider;


/**
 * The {@link PeriodNameSpace} is a name space.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class PeriodNameSpace {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final int NAME_SPACE_FILE_INDEX = 2;
    private static final String NAME_SPACE_FILE_NAME = "period-ns-2.dt";
    private static final String NAME_SPACE_FILE_PREFIX = "period-ns-2-indexes/btree";
    private static final String NAME_SPACE_DIR = "period-ns-2-indexes";
    private static final short MAGIC = 0x1703;
    private static final int HEADER_SIZE = 24;           // + magic(short) + version(byte) + padding(byte) + nextBlockIndex(long) +
    private static final int NEXT_BLOCK_INDEX_OFFSET = 4;//   nameIndexId(int) + callPathIndexId(int) + nextTypeId(int)
    private static final int NEXT_TYPE_ID_OFFSET = 20;
    private final ITransactionProvider transactionProvider;
    private final PeriodNameCache nameCache;
    private final int fileIndex;
    private final IIndexManager indexManager;
    private final IDatabaseContext context;
    private final NameSpaceSchemaConfiguration configuration;
    private IUniqueIndex<String, Long> nameIndex;
    private IUniqueIndex<Location, Long> callPathIndex;
    private IRawPage headerPage;

    public static PeriodNameSpace create(IDatabaseContext context, PeriodNameCache nameCache, NameSpaceSchemaConfiguration configuration) {
        Assert.notNull(context);
        Assert.notNull(nameCache);
        Assert.notNull(configuration);

        IRawTransaction transaction = context.getTransactionProvider().getRawTransaction();

        bindFile(context, transaction, NAME_SPACE_FILE_INDEX, configuration.getNameSpacePathIndex());

        PeriodNameSpace space = new PeriodNameSpace(context, NAME_SPACE_FILE_INDEX, nameCache, configuration);
        space.onCreated(configuration);

        return space;
    }

    public static PeriodNameSpace open(IDatabaseContext context, PeriodNameCache nameCache, NameSpaceSchemaConfiguration configuration) {
        Assert.notNull(context);
        Assert.notNull(nameCache);
        Assert.notNull(configuration);

        IRawTransaction transaction = context.getTransactionProvider().getRawTransaction();

        bindFile(context, transaction, NAME_SPACE_FILE_INDEX, configuration.getNameSpacePathIndex());

        PeriodNameSpace space = new PeriodNameSpace(context, NAME_SPACE_FILE_INDEX, nameCache, configuration);
        space.onOpened();

        return space;
    }

    public List<String> getFiles() {
        return Arrays.asList(new File(context.getConfiguration().getPaths().get(configuration.getNameSpacePathIndex()), NAME_SPACE_FILE_NAME).getPath(),
                new File(context.getConfiguration().getPaths().get(configuration.getNameIndexPathIndex()), NAME_SPACE_DIR).getPath());
    }

    public PeriodName findById(long id) {
        if (id == 0)
            return null;

        PeriodName name = nameCache.findById(id);
        if (name != null)
            return name;

        return readById(id);
    }

    public PeriodName findByName(IName n) {
        if (n.isEmpty())
            return null;

        PeriodName name = nameCache.findByName(n);
        if (name != null)
            return name;

        return readByName(n);
    }

    public PeriodName addName(IName n) {
        if (n.isEmpty())
            return null;

        Assert.notNull(n);

        IRawTransaction transaction = transactionProvider.getRawTransaction();
        Assert.checkState(!transaction.isReadOnly());
        Assert.checkState((transactionProvider.getTransaction().getOptions() & IOperation.DELAYED_FLUSH) == 0);

        PeriodName name = findByName(n);
        if (name != null)
            return name;

        long parentId = 0;
        long metricId = 0;
        if (n instanceof ICallPath) {
            ICallPath callPath = (ICallPath) n;
            if (!callPath.getParent().isEmpty())
                parentId = addName(callPath.getParent()).getId();

            metricId = addName(callPath.getLastSegment()).getId();
        }

        long nextBlockIndex = headerPage.getReadRegion().readLong(NEXT_BLOCK_INDEX_OFFSET);
        RawPageSerialization serialization = new RawPageSerialization(transaction, fileIndex, Constants.pageIndexByBlockIndex(nextBlockIndex),
                Constants.pageOffsetByBlockIndex(nextBlockIndex));
        long id = nextBlockIndex;

        if (n instanceof IScopeName) {
            serialization.writeByte((byte) 1);
            MeasurementSerializers.serializeScopeName(serialization, (IScopeName) n);
            getNameIndex().add(n.toString(), id);
        } else if (n instanceof IMetricName) {
            serialization.writeByte((byte) 2);
            MeasurementSerializers.serializeMetricName(serialization, (IMetricName) n);
            getNameIndex().add(n.toString(), id);
        } else if (n instanceof ICallPath) {
            serialization.writeByte((byte) 3);
            serialization.writeLong(parentId);
            serialization.writeLong(metricId);
            getCallPathIndex().add(new Location(parentId, metricId), id);
        } else
            Assert.error();

        headerPage.getWriteRegion().writeLong(NEXT_BLOCK_INDEX_OFFSET, Constants.blockIndex(
                Constants.alignBlock(serialization.getFileOffset())));
        serialization.writeByte((byte) 0);

        name = new PeriodName(nameCache, n, id, true);

        nameCache.addName(name, true);

        return name;
    }

    public int allocateTypeId() {
        IRawWriteRegion region = headerPage.getWriteRegion();
        int typeId = region.readInt(NEXT_TYPE_ID_OFFSET);
        region.writeInt(NEXT_TYPE_ID_OFFSET, typeId + 1);
        return typeId;
    }

    private PeriodNameSpace(IDatabaseContext context, int fileIndex, PeriodNameCache nameCache, NameSpaceSchemaConfiguration configuration) {
        Assert.notNull(context);
        Assert.isTrue(fileIndex != 0);

        this.transactionProvider = context.getTransactionProvider();
        this.fileIndex = fileIndex;
        this.headerPage = transactionProvider.getRawTransaction().getPage(fileIndex, 0);
        this.nameCache = nameCache;
        this.indexManager = context.findTransactionExtension(IIndexManager.NAME);
        this.context = context;
        this.configuration = configuration;
    }

    private void readHeader() {
        RawPageDeserialization deserialization = new RawPageDeserialization(transactionProvider.getRawTransaction(),
                fileIndex, headerPage, 0);

        short magic = deserialization.readShort();
        byte version = deserialization.readByte();
        deserialization.readByte();
        deserialization.readLong();

        if (magic != MAGIC)
            throw new RawDatabaseException(messages.invalidFormat(deserialization.getFileIndex()));
        if (version != Constants.VERSION)
            throw new RawDatabaseException(messages.unsupportedVersion(deserialization.getFileIndex(), version, Constants.VERSION));

        int nameIndexId = deserialization.readInt();
        int callPathIndexId = deserialization.readInt();

        nameIndex = indexManager.getIndex(nameIndexId);
        callPathIndex = indexManager.getIndex(callPathIndexId);
    }

    private void writeHeader(int nameIndexId, int callPathIndexId) {
        RawPageSerialization serialization = new RawPageSerialization(transactionProvider.getRawTransaction(), fileIndex,
                headerPage, 0);

        serialization.writeShort(MAGIC);
        serialization.writeByte(Constants.VERSION);
        serialization.writeBoolean(false);
        serialization.writeLong(Constants.blockIndex(HEADER_SIZE) + 1);
        serialization.writeInt(nameIndexId);
        serialization.writeInt(callPathIndexId);
        serialization.writeInt(1);// nextTypeId
    }

    private PeriodName readById(long id) {
        RawPageDeserialization deserialization = new RawPageDeserialization(transactionProvider.getRawTransaction(), fileIndex,
                Constants.pageIndexByBlockIndex(id), Constants.pageOffsetByBlockIndex(id));

        byte type = deserialization.readByte();

        IName n;
        switch (type) {
            case 0:
                return null;
            case 1:
                n = MeasurementSerializers.deserializeScopeName(deserialization);
                break;
            case 2:
                n = MeasurementSerializers.deserializeMetricName(deserialization);
                break;
            case 3:
                long parentId = deserialization.readLong();
                long metricId = deserialization.readLong();
                CallPath parent = parentId != 0 ? (CallPath) findById(parentId).getName() : CallPath.root();

                n = CallPath.get(parent, (MetricName) findById(metricId).getName());
                break;
            default:
                return Assert.error();
        }

        PeriodName name = new PeriodName(nameCache, n, id, false);
        nameCache.addName(name, false);

        return name;
    }

    private PeriodName readByName(IName n) {
        Long id = null;
        if (n instanceof IMetricName || n instanceof IScopeName)
            id = getNameIndex().find(n.toString());
        else if (n instanceof ICallPath) {
            ICallPath callPath = (ICallPath) n;

            long parentId = 0;
            if (!callPath.getParent().isEmpty()) {
                IPeriodName name = findByName(callPath.getParent());
                if (name == null)
                    return null;
                parentId = name.getId();
            }

            IPeriodName name = findByName(callPath.getLastSegment());
            if (name == null)
                return null;

            long metricId = name.getId();

            id = getCallPathIndex().find(new Location(parentId, metricId));
        }

        if (id == null)
            return null;

        PeriodName name = new PeriodName(nameCache, n, id, false);
        nameCache.addName(name, false);

        return name;
    }

    private static void bindFile(IDatabaseContext context, IRawTransaction transaction, int fileIndex, int pathIndex) {
        RawBindInfo bindInfo = new RawBindInfo();
        bindInfo.setPathIndex(pathIndex);
        bindInfo.setName(NAME_SPACE_FILE_NAME);
        Pair<String, String> pair = context.getCacheCategorizationStrategy().categorize(new MapBuilder<String, String>()
                .put("type", "pages.names.period")
                .toMap());
        bindInfo.setCategory(pair.getKey());
        bindInfo.setCategoryType(pair.getValue());
        transaction.bindFile(fileIndex, bindInfo);
    }

    private void onCreated(NameSpaceSchemaConfiguration schema) {
        Map<String, String> properties = new MapBuilder<String, String>().put("name", "names.period").toMap();
        BTreeIndexSchemaConfiguration nameIndexSchema = new BTreeIndexSchemaConfiguration(schema.getName() + ".nameIndex",
                schema.getAlias() + ".nameIndex", schema.getDescription(), schema.getNameIndexPathIndex(), false,
                schema.getMaxNameSize() * 2, true, 8, new StringKeyNormalizerSchemaConfiguration(),
                new LongValueConverterSchemaConfiguration(), false, true, properties);
        nameIndex = indexManager.createIndex(NAME_SPACE_FILE_PREFIX, nameIndexSchema);

        BTreeIndexSchemaConfiguration callPathIndexSchema = new BTreeIndexSchemaConfiguration(schema.getName() + ".callPathIndex",
                schema.getAlias() + ".callPathIndex", schema.getDescription(), schema.getNameIndexPathIndex(), true,
                16, true, 8, new LocationKeyNormalizerSchemaConfiguration(), new LongValueConverterSchemaConfiguration(), false, true,
                properties);
        callPathIndex = indexManager.createIndex(NAME_SPACE_FILE_PREFIX, callPathIndexSchema);

        writeHeader(nameIndex.getId(), callPathIndex.getId());
    }

    private void onOpened() {
        readHeader();
    }

    private IUniqueIndex<String, Long> getNameIndex() {
        if (!nameIndex.isStale())
            return nameIndex;
        else {
            nameIndex = indexManager.getIndex(nameIndex.getId());
            return nameIndex;
        }
    }

    private IUniqueIndex<Location, Long> getCallPathIndex() {
        if (!callPathIndex.isStale())
            return callPathIndex;
        else {
            callPathIndex = indexManager.getIndex(callPathIndex.getId());
            return callPathIndex;
        }
    }

    private interface IMessages {
        @DefaultMessage("Invalid format of file ''{0}''.")
        ILocalizedMessage invalidFormat(int fileIndex);

        @DefaultMessage("Unsupported version ''{1}'' of file ''{0}'', expected version - ''{2}''.")
        ILocalizedMessage unsupportedVersion(int fileIndex, int fileVersion, int expectedVersion);
    }
}