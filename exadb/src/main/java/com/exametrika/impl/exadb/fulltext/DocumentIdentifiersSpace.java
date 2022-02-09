/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.fulltext;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.api.exadb.fulltext.IDocument;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.rawdb.IRawPage;
import com.exametrika.common.rawdb.IRawReadRegion;
import com.exametrika.common.rawdb.IRawTransaction;
import com.exametrika.common.rawdb.IRawWriteRegion;
import com.exametrika.common.rawdb.RawBindInfo;
import com.exametrika.common.rawdb.RawDatabaseException;
import com.exametrika.common.rawdb.impl.RawPageDeserialization;
import com.exametrika.common.rawdb.impl.RawPageSerialization;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.MapBuilder;
import com.exametrika.common.utils.Pair;
import com.exametrika.impl.exadb.core.Constants;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.spi.exadb.core.ITransactionProvider;
import com.exametrika.spi.exadb.fulltext.IFullTextDocumentSpace;


/**
 * The {@link DocumentIdentifiersSpace} is a space containing identifiers of documents indexed but not yet committed by full text index.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class DocumentIdentifiersSpace {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final int HEADER_SIZE = 19;
    private static final short MAGIC = 0x171C;// magic(short) + version(byte) + nextFileOffset(long) + documentsCount(long)
    private static final byte VERSION = 0x1;
    private static final int NEXT_FILE_OFFSET_OFFSET = 3;
    private static final int DOCUMENTS_COUNT_OFFSET = 11;
    private final ITransactionProvider transactionProvider;
    private final FullTextIndex index;
    private final int fileIndex;
    private final String fileName;
    private IFullTextDocumentSpace space;
    private boolean beginCommit;
    private List<CommitQueueElement> commitQueue = new ArrayList<CommitQueueElement>();
    private IRawPage headerPage;
    private boolean locked;
    private boolean deleted;

    public static DocumentIdentifiersSpace create(IDatabaseContext context, FullTextIndex index, int fileIndex, int pathIndex, String fileName) {
        Assert.notNull(context);
        Assert.notNull(fileName);
        Assert.notNull(index);

        bindFile(context, fileIndex, pathIndex, fileName);
        DocumentIdentifiersSpace space = new DocumentIdentifiersSpace(context.getTransactionProvider(), index, fileIndex, fileName);
        space.writeHeader();

        return space;
    }

    public static DocumentIdentifiersSpace open(IDatabaseContext context, FullTextIndex index, int fileIndex, int pathIndex, String fileName) {
        Assert.notNull(context);
        Assert.notNull(fileName);

        bindFile(context, fileIndex, pathIndex, fileName);
        DocumentIdentifiersSpace space = new DocumentIdentifiersSpace(context.getTransactionProvider(), index, fileIndex, fileName);
        space.readHeader();

        return space;
    }

    public String getFileName() {
        return fileName;
    }

    public void add(IDocument document) {
        if (locked || deleted || space == null)
            return;

        Assert.notNull(document);

        IRawWriteRegion region = headerPage.getWriteRegion();
        long fileOffset = region.readLong(NEXT_FILE_OFFSET_OFFSET);
        long documentsCount = region.readLong(DOCUMENTS_COUNT_OFFSET);

        RawPageSerialization pageSerialization = new RawPageSerialization(transactionProvider.getRawTransaction(),
                fileIndex, Constants.pageIndexByFileOffset(fileOffset), Constants.pageOffsetByFileOffset(fileOffset));
        pageSerialization.writeBoolean(true);
        space.write(pageSerialization, document);
        region.writeLong(NEXT_FILE_OFFSET_OFFSET, pageSerialization.getFileOffset());
        region.writeLong(DOCUMENTS_COUNT_OFFSET, documentsCount + 1);

        if (beginCommit)
            commitQueue.add(new CommitQueueElement(document));
    }

    public void remove(String field, String value) {
        if (locked || deleted || space == null)
            return;

        Assert.notNull(field);
        Assert.notNull(value);

        IRawWriteRegion region = headerPage.getWriteRegion();
        long fileOffset = region.readLong(NEXT_FILE_OFFSET_OFFSET);
        long documentsCount = region.readLong(DOCUMENTS_COUNT_OFFSET);

        RawPageSerialization pageSerialization = new RawPageSerialization(transactionProvider.getRawTransaction(),
                fileIndex, Constants.pageIndexByFileOffset(fileOffset), Constants.pageOffsetByFileOffset(fileOffset));
        pageSerialization.writeBoolean(false);
        pageSerialization.writeString(field);
        pageSerialization.writeString(value);
        region.writeLong(NEXT_FILE_OFFSET_OFFSET, pageSerialization.getFileOffset());
        region.writeLong(DOCUMENTS_COUNT_OFFSET, documentsCount + 1);

        if (beginCommit)
            commitQueue.add(new CommitQueueElement(field, value));
    }

    public void beginCommit() {
        if (deleted || space == null)
            return;

        beginCommit = true;
    }

    public void endCommit() {
        if (deleted || space == null)
            return;

        beginCommit = false;

        long fileOffset = HEADER_SIZE;

        RawPageSerialization pageSerialization = new RawPageSerialization(transactionProvider.getRawTransaction(),
                fileIndex, Constants.pageIndexByFileOffset(fileOffset), Constants.pageOffsetByFileOffset(fileOffset));

        for (CommitQueueElement element : commitQueue) {
            if (element.document != null) {
                pageSerialization.writeBoolean(true);
                space.write(pageSerialization, element.document);
            } else {
                pageSerialization.writeBoolean(false);
                pageSerialization.writeString(element.field);
                pageSerialization.writeString(element.value);
            }
        }

        IRawWriteRegion region = headerPage.getWriteRegion();
        region.writeLong(DOCUMENTS_COUNT_OFFSET, commitQueue.size());
        region.writeLong(NEXT_FILE_OFFSET_OFFSET, pageSerialization.getFileOffset());
        commitQueue.clear();
    }

    public void delete() {
        deleted = true;
        IRawTransaction transaction = transactionProvider.getRawTransaction();
        transaction.getFile(fileIndex).delete();
    }

    public void setDocumentSpace(IFullTextDocumentSpace space) {
        Assert.notNull(space);
        Assert.checkState(this.space == null);

        this.space = space;
    }

    public void reindex() {
        Assert.checkState(space != null);

        IRawReadRegion region = headerPage.getReadRegion();
        long documentsCount = region.readLong(DOCUMENTS_COUNT_OFFSET);

        RawPageDeserialization pageDeserialization = new RawPageDeserialization(transactionProvider.getRawTransaction(),
                fileIndex, headerPage, HEADER_SIZE);

        locked = true;

        for (long i = 0; i < documentsCount; i++) {
            if (pageDeserialization.readBoolean())
                space.readAndReindex(pageDeserialization);
            else {
                String field = pageDeserialization.readString();
                String value = pageDeserialization.readString();
                index.remove(field, value);
            }
        }

        locked = false;
    }

    private DocumentIdentifiersSpace(ITransactionProvider transactionProvider, FullTextIndex index, int fileIndex, String fileName) {
        Assert.notNull(transactionProvider);

        this.transactionProvider = transactionProvider;
        this.index = index;
        this.headerPage = transactionProvider.getRawTransaction().getPage(fileIndex, 0);
        this.fileIndex = fileIndex;
        this.fileName = fileName;
    }

    private void readHeader() {
        RawPageDeserialization deserialization = new RawPageDeserialization(transactionProvider.getRawTransaction(),
                fileIndex, headerPage, 0);

        short magic = deserialization.readShort();
        byte version = deserialization.readByte();

        if (magic != MAGIC)
            throw new RawDatabaseException(messages.invalidFormat(deserialization.getFileIndex()));
        if (version != VERSION)
            throw new RawDatabaseException(messages.unsupportedVersion(deserialization.getFileIndex(), version, VERSION));
    }

    private void writeHeader() {
        RawPageSerialization serialization = new RawPageSerialization(transactionProvider.getRawTransaction(),
                fileIndex, headerPage, 0);

        serialization.writeShort(MAGIC);
        serialization.writeByte(VERSION);
        serialization.writeLong(HEADER_SIZE);
        serialization.writeLong(0);
    }

    private static void bindFile(IDatabaseContext context, int fileIndex, int pathIndex, String fileName) {
        RawBindInfo bindInfo = new RawBindInfo();
        bindInfo.setPathIndex(pathIndex);
        bindInfo.setName(fileName);

        Pair<String, String> pair = context.getCacheCategorizationStrategy().categorize(new MapBuilder<String, String>()
                .put("type", "pages.index.fulltext")
                .toMap());
        bindInfo.setCategory(pair.getKey());
        bindInfo.setCategoryType(pair.getValue());

        IRawTransaction transaction = context.getTransactionProvider().getRawTransaction();
        transaction.bindFile(fileIndex, bindInfo);
    }

    private static class CommitQueueElement {
        private final IDocument document;
        private final String field;
        private final String value;

        public CommitQueueElement(IDocument document) {
            this.document = document;
            this.field = null;
            this.value = null;
        }

        public CommitQueueElement(String field, String value) {
            this.document = null;
            this.field = field;
            this.value = value;
        }
    }

    private interface IMessages {
        @DefaultMessage("Invalid format of file ''{0}''.")
        ILocalizedMessage invalidFormat(int fileIndex);

        @DefaultMessage("Unsupported version ''{1}'' of file ''{0}'', expected version - ''{2}''.")
        ILocalizedMessage unsupportedVersion(int fileIndex, int fileVersion, int expectedVersion);
    }
}
