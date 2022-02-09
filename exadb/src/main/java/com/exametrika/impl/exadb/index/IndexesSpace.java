/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.index;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import com.exametrika.api.exadb.index.IIndexManager.IndexInfo;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.io.impl.ByteInputStream;
import com.exametrika.common.io.impl.ByteOutputStream;
import com.exametrika.common.io.impl.Deserialization;
import com.exametrika.common.io.impl.Serialization;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.rawdb.IRawPage;
import com.exametrika.common.rawdb.IRawTransaction;
import com.exametrika.common.rawdb.RawBindInfo;
import com.exametrika.common.rawdb.RawDatabaseException;
import com.exametrika.common.rawdb.impl.RawPageDeserialization;
import com.exametrika.common.rawdb.impl.RawPageSerialization;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.MapBuilder;
import com.exametrika.common.utils.Pair;
import com.exametrika.common.utils.Serializers;
import com.exametrika.impl.exadb.index.IndexManager.DeleteInfo;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.spi.exadb.core.ITransactionProvider;
import com.exametrika.spi.exadb.index.config.schema.IndexSchemaConfiguration;


/**
 * The {@link IndexesSpace} is a indexes space.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class IndexesSpace {
    public static final int INDEXES_SPACE_FILE_INDEX = 1;
    public static final String INDEXES_SPACE_FILE_NAME = "indexes-1.dt";
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final int HEADER_SIZE = 3;
    private static final short MAGIC = 0x170E;// magic(short) + version(byte)
    private static final byte VERSION = 0x1;
    private final ITransactionProvider transactionProvider;
    private final ISerializationRegistry serializationRegistry;
    private final IDatabaseContext context;
    private IRawPage headerPage;

    public static IndexesSpace create(IDatabaseContext context) {
        Assert.notNull(context);

        bindFile(context);
        IndexesSpace space = new IndexesSpace(context);
        space.writeHeader();

        return space;
    }

    public static IndexesSpace open(IDatabaseContext context) {
        Assert.notNull(context);

        bindFile(context);
        IndexesSpace space = new IndexesSpace(context);
        space.readHeader();

        return space;
    }

    public List<String> getFiles() {
        return Arrays.asList(new File(context.getConfiguration().getPaths().get(0), INDEXES_SPACE_FILE_NAME).getPath());
    }

    public void readIndexes(List<IndexInfo> indexes, List<DeleteInfo> deletedIndexes) {
        RawPageDeserialization pageDeserialization = new RawPageDeserialization(transactionProvider.getRawTransaction(),
                INDEXES_SPACE_FILE_INDEX, headerPage, HEADER_SIZE);

        int count = pageDeserialization.readInt();
        for (int i = 0; i < count; i++) {
            ByteArray data = pageDeserialization.readByteArray();
            ByteInputStream stream = new ByteInputStream(data.getBuffer(), data.getOffset(), data.getLength());

            Deserialization deserialization = new Deserialization(serializationRegistry, stream);
            IndexSchemaConfiguration configuration = deserialization.readObject();
            String filePrefix = deserialization.readString();
            int fileIndex = deserialization.readInt();

            indexes.add(new IndexInfo(configuration, filePrefix, fileIndex));
        }

        count = pageDeserialization.readInt();
        for (int i = 0; i < count; i++) {
            String path = pageDeserialization.readString();
            long time = pageDeserialization.readLong();
            deletedIndexes.add(new DeleteInfo(new File(path), time));
        }
    }

    public void writeIndexes(List<? extends IndexInfo> indexes, List<DeleteInfo> deletedIndexes) {
        RawPageSerialization pageSerialization = new RawPageSerialization(transactionProvider.getRawTransaction(),
                INDEXES_SPACE_FILE_INDEX, headerPage, HEADER_SIZE);

        pageSerialization.writeInt(indexes.size());
        for (IndexInfo info : indexes) {
            ByteOutputStream stream = new ByteOutputStream();
            Serialization serialization = new Serialization(serializationRegistry, true, stream);

            serialization.writeObject(info.schema);
            serialization.writeString(info.filePrefix);
            serialization.writeInt(info.id);

            pageSerialization.writeByteArray(new ByteArray(stream.getBuffer(), 0, stream.getLength()));
        }

        pageSerialization.writeInt(deletedIndexes.size());
        for (DeleteInfo info : deletedIndexes) {
            pageSerialization.writeString(info.path.getPath());
            pageSerialization.writeLong(info.time);
        }
    }

    private IndexesSpace(IDatabaseContext context) {
        Assert.notNull(context);

        this.transactionProvider = context.getTransactionProvider();
        this.serializationRegistry = Serializers.createRegistry();
        this.headerPage = transactionProvider.getRawTransaction().getPage(INDEXES_SPACE_FILE_INDEX, 0);
        this.context = context;
    }

    private void readHeader() {
        RawPageDeserialization deserialization = new RawPageDeserialization(transactionProvider.getRawTransaction(),
                INDEXES_SPACE_FILE_INDEX, headerPage, 0);

        short magic = deserialization.readShort();
        byte version = deserialization.readByte();

        if (magic != MAGIC)
            throw new RawDatabaseException(messages.invalidFormat(deserialization.getFileIndex()));
        if (version != VERSION)
            throw new RawDatabaseException(messages.unsupportedVersion(deserialization.getFileIndex(), version, VERSION));
    }

    private void writeHeader() {
        RawPageSerialization serialization = new RawPageSerialization(transactionProvider.getRawTransaction(),
                INDEXES_SPACE_FILE_INDEX, headerPage, 0);

        serialization.writeShort(MAGIC);
        serialization.writeByte(VERSION);
        serialization.writeInt(0);
    }

    private static void bindFile(IDatabaseContext context) {
        RawBindInfo bindInfo = new RawBindInfo();
        bindInfo.setName(INDEXES_SPACE_FILE_NAME);

        Pair<String, String> pair = context.getCacheCategorizationStrategy().categorize(new MapBuilder<String, String>()
                .put("type", "pages.system.indexes")
                .toMap());
        bindInfo.setCategory(pair.getKey());
        bindInfo.setCategoryType(pair.getValue());

        IRawTransaction transaction = context.getTransactionProvider().getRawTransaction();
        transaction.bindFile(INDEXES_SPACE_FILE_INDEX, bindInfo);
    }

    private interface IMessages {
        @DefaultMessage("Invalid format of file ''{0}''.")
        ILocalizedMessage invalidFormat(int fileIndex);

        @DefaultMessage("Unsupported version ''{1}'' of file ''{0}'', expected version - ''{2}''.")
        ILocalizedMessage unsupportedVersion(int fileIndex, int fileVersion, int expectedVersion);
    }
}
