/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.fulltext;

import com.exametrika.api.exadb.core.IOperation;
import com.exametrika.api.exadb.core.ITransaction;
import com.exametrika.api.exadb.core.Operation;
import com.exametrika.api.exadb.fulltext.IDocument;
import com.exametrika.api.exadb.fulltext.IFilter;
import com.exametrika.api.exadb.fulltext.IFullTextIndex;
import com.exametrika.api.exadb.fulltext.IQuery;
import com.exametrika.api.exadb.fulltext.ISearchResult;
import com.exametrika.api.exadb.fulltext.Sort;
import com.exametrika.api.exadb.fulltext.config.FullTextIndexConfiguration;
import com.exametrika.api.exadb.fulltext.config.schema.FullTextIndexSchemaConfiguration;
import com.exametrika.api.exadb.index.config.IndexDatabaseExtensionConfiguration;
import com.exametrika.common.compartment.ICompartmentTask;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.rawdb.IRawTransaction;
import com.exametrika.common.rawdb.RawDatabaseException;
import com.exametrika.common.rawdb.impl.RawDatabase;
import com.exametrika.common.time.ITimeService;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Holder;
import com.exametrika.common.utils.HolderManager;
import com.exametrika.common.utils.Threads;
import com.exametrika.impl.exadb.core.DatabaseInterceptor;
import com.exametrika.impl.exadb.fulltext.schema.DocumentSchema;
import com.exametrika.impl.exadb.index.AbstractIndexSpace;
import com.exametrika.impl.exadb.index.IndexDatabaseExtension;
import com.exametrika.impl.exadb.index.IndexManager;
import com.exametrika.spi.exadb.core.IDataFileAllocator;
import com.exametrika.spi.exadb.core.ITransactionProvider;
import com.exametrika.spi.exadb.fulltext.IFullTextDocumentSpace;
import com.exametrika.spi.exadb.fulltext.IFullTextIndexControl;
import com.exametrika.spi.exadb.fulltext.IndexAnalyzer;
import com.exametrika.spi.exadb.fulltext.IndexFilter;
import com.exametrika.spi.exadb.fulltext.IndexQuery;
import com.exametrika.spi.exadb.index.config.schema.IndexSchemaConfiguration;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexCommit;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.IndexWriterConfig.OpenMode;
import org.apache.lucene.index.KeepOnlyLastCommitDeletionPolicy;
import org.apache.lucene.index.SnapshotDeletionPolicy;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * The {@link FullTextIndex} is a full text index implementation.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class FullTextIndex extends AbstractIndexSpace implements IFullTextIndex, IFullTextIndexControl {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(FullTextIndex.class);
    private final ITimeService timeService;
    private final ITransactionProvider transactionProvider;
    private final File path;
    private final HolderManager holderManager;
    private final DocumentIdentifiersSpace space;
    private Holder<IndexWriter> writer;
    private Holder<DirectoryReader> reader;
    private IndexSearcher searcher;
    private int modCount;
    private FullTextIndexConfiguration configuration = new FullTextIndexConfiguration();
    private long lastSearcherUpdateTime;
    private long lastWriterCommitTime;
    private long lastStartWriterCommitTime;
    private IndexCommit snapshot;
    private volatile long lastModifyTime;
    private final int interceptId;

    public static FullTextIndex create(IndexManager indexManager, ITransactionProvider transactionProvider,
                                       IDataFileAllocator dataFileAllocator, ITimeService timeService, FullTextIndexSchemaConfiguration schema, String basePath, String filePrefix) {
        Assert.notNull(indexManager);
        Assert.notNull(transactionProvider);
        Assert.notNull(dataFileAllocator);
        Assert.notNull(timeService);
        Assert.notNull(schema);
        Assert.notNull(basePath);
        Assert.notNull(filePrefix);

        IRawTransaction transaction = transactionProvider.getRawTransaction();
        int fileIndex = dataFileAllocator.allocateFile(transaction);
        dataFileAllocator.allocateFile(transaction);

        return new FullTextIndex(timeService, transactionProvider, indexManager, schema, basePath, fileIndex, filePrefix, true);
    }

    public static FullTextIndex open(IndexManager indexManager, ITransactionProvider transactionProvider,
                                     ITimeService timeService, FullTextIndexSchemaConfiguration schema, String basePath, int fileIndex, String filePrefix) {
        Assert.notNull(indexManager);
        Assert.notNull(transactionProvider);
        Assert.notNull(timeService);
        Assert.notNull(schema);
        Assert.notNull(basePath);
        Assert.notNull(filePrefix);

        return new FullTextIndex(timeService, transactionProvider, indexManager, schema, basePath, fileIndex, filePrefix, false);
    }

    @Override
    public void setConfiguration(IndexDatabaseExtensionConfiguration configuration) {
        this.configuration = configuration.getFullTextIndex();

        if (writer != null)
            writer.get().getConfig().setRAMBufferSizeMB(this.configuration.getBufferSizePerIndex() / (1 << 20));
    }

    public IDocument getDocument(int docId, int modCount) {
        Assert.checkState(this.modCount == modCount);
        Assert.checkState(writer != null);

        try {
            return DocumentSchema.createDocument(searcher.doc(docId));
        } catch (IOException e) {
            throw new RawDatabaseException(e);
        }
    }

    @Override
    public void delete() {
        Assert.checkState(!isStale());
        checkWriteTransaction();

        Assert.checkState(writer != null);
        unload(true);

        space.delete();

        if (indexManager != null)
            indexManager.deleteNonTransactionalIndex(getId(), path, timeService.getCurrentTime() + configuration.getIndexDeleteDelay());
    }

    @Override
    public void deleteFiles() {
        Assert.checkState(!isStale());
        checkWriteTransaction();

        Assert.checkState(writer != null);
        unload(true);

        space.delete();

        if (indexManager != null)
            indexManager.deleteFilesOfNonTransactionalIndex(getId(), path, timeService.getCurrentTime() + configuration.getIndexDeleteDelay());
    }

    @Override
    public void onTimer(long currentTime) {
        if (reader != null && currentTime > lastSearcherUpdateTime + configuration.getSearcherUpdatePeriod())
            updateSearcher();

        if (writer != null && currentTime > lastWriterCommitTime + configuration.getWriterCommitPeriod())
            commitWriter(false);
    }

    @Override
    public void flush(boolean full) {
        if (full)
            commitWriter(true);
    }

    @Override
    public void unload(boolean full) {
        if (full) {
            if (writer != null) {
                for (int i = 0; i < 5000; i++) {
                    if (writer.canClose())
                        break;

                    Threads.sleep(1);
                }

                writer.release();
            }

            if (reader != null)
                reader.release();

            writer = null;
            reader = null;
            searcher = null;
            modCount++;
        }
    }

    @Override
    public void add(IDocument document) {
        Assert.notNull(document);
        Assert.checkState(writer != null);

        boolean interceptResult = DatabaseInterceptor.INSTANCE.onBeforeFullTextAdded(interceptId);

        try {
            IndexDocument indexDocument = (IndexDocument) document;
            writer.get().addDocument(indexDocument.getSchema().createDocument(indexDocument),
                    ((IndexAnalyzer) indexDocument.getSchema().getAnalyzer()).getAnalyzer());

            space.add(document);
            lastModifyTime = timeService.getCurrentTime();
        } catch (IOException e) {
            throw new RawDatabaseException(e);
        } finally {
            if (interceptResult)
                DatabaseInterceptor.INSTANCE.onAfterFullTextAdded(interceptId);
        }
    }

    @Override
    public void update(String field, String value, IDocument document) {
        Assert.notNull(document);
        Assert.checkState(writer != null);

        boolean interceptResult = DatabaseInterceptor.INSTANCE.onBeforeFullTextUpdated(interceptId);

        try {
            IndexDocument indexDocument = (IndexDocument) document;
            writer.get().updateDocument(new Term(field, value), indexDocument.getSchema().createDocument(indexDocument),
                    ((IndexAnalyzer) indexDocument.getSchema().getAnalyzer()).getAnalyzer());

            space.add(document);
            lastModifyTime = timeService.getCurrentTime();
        } catch (IOException e) {
            throw new RawDatabaseException(e);
        } finally {
            if (interceptResult)
                DatabaseInterceptor.INSTANCE.onAfterFullTextUpdated(interceptId);
        }
    }

    @Override
    public void remove(String field, String value) {
        Assert.notNull(field);
        Assert.notNull(value);
        Assert.checkState(writer != null);

        boolean interceptResult = DatabaseInterceptor.INSTANCE.onBeforeFullTextDeleted(interceptId);

        try {
            writer.get().deleteDocuments(new Term(field, value));
            space.remove(field, value);

            lastModifyTime = timeService.getCurrentTime();
        } catch (IOException e) {
            throw new RawDatabaseException(e);
        } finally {
            if (interceptResult)
                DatabaseInterceptor.INSTANCE.onAfterFullTextDeleted(interceptId);
        }
    }

    @Override
    public void remove(IQuery... queries) {
        Assert.notNull(queries);
        Assert.checkState(writer != null);

        final Query[] list = new Query[queries.length];
        for (int i = 0; i < queries.length; i++)
            list[i] = ((IndexQuery) queries[i]).getQuery();

        indexManager.getContext().getCompartment().execute(new Runnable() {
            private final Holder<IndexWriter> writer = FullTextIndex.this.writer;

            {
                writer.addRef();
            }

            @Override
            public void run() {
                try {
                    writer.get().deleteDocuments(list);

                    lastModifyTime = timeService.getCurrentTime();
                } catch (IOException e) {
                    throw new RawDatabaseException(e);
                } finally {
                    writer.release();
                }
            }
        });
    }

    @Override
    public void removeAll() {
        Assert.checkState(writer != null);

        indexManager.getContext().getCompartment().execute(new Runnable() {
            private final Holder<IndexWriter> writer = FullTextIndex.this.writer;

            {
                writer.addRef();
            }

            @Override
            public void run() {
                try {
                    writer.get().deleteAll();

                    lastModifyTime = timeService.getCurrentTime();
                } catch (IOException e) {
                    throw new RawDatabaseException(e);
                } finally {
                    writer.release();
                }
            }
        });
    }

    @Override
    public ISearchResult search(IQuery query, int count) {
        Assert.notNull(query);

        boolean interceptResult = DatabaseInterceptor.INSTANCE.onBeforeFullTextSearched(interceptId);

        ensureSearcher();

        try {
            return SearchResult.createResult(this, searcher.search(((IndexQuery) query).getQuery(), count), modCount);
        } catch (IOException e) {
            throw new RawDatabaseException(e);
        } finally {
            if (interceptResult)
                DatabaseInterceptor.INSTANCE.onAfterFullTextSearched(interceptId);
        }
    }

    @Override
    public ISearchResult search(IQuery query, IFilter filter, int count) {
        Assert.notNull(query);
        Assert.notNull(filter);

        boolean interceptResult = DatabaseInterceptor.INSTANCE.onBeforeFullTextSearched(interceptId);

        ensureSearcher();

        try {
            return SearchResult.createResult(this, searcher.search(((IndexQuery) query).getQuery(), ((IndexFilter) filter).getFilter(),
                    count), modCount);
        } catch (IOException e) {
            throw new RawDatabaseException(e);
        } finally {
            if (interceptResult)
                DatabaseInterceptor.INSTANCE.onAfterFullTextSearched(interceptId);
        }
    }

    @Override
    public ISearchResult search(IQuery query, Sort sort, int count) {
        Assert.notNull(query);
        Assert.notNull(sort);

        boolean interceptResult = DatabaseInterceptor.INSTANCE.onBeforeFullTextSearched(interceptId);

        ensureSearcher();

        try {
            return SearchResult.createResult(this, searcher.search(((IndexQuery) query).getQuery(), count,
                    sort.createSort(query.getSchema())), modCount);
        } catch (IOException e) {
            throw new RawDatabaseException(e);
        } finally {
            if (interceptResult)
                DatabaseInterceptor.INSTANCE.onAfterFullTextSearched(interceptId);
        }
    }

    @Override
    public ISearchResult search(IQuery query, IFilter filter, Sort sort, int count) {
        Assert.notNull(query);
        Assert.notNull(filter);
        Assert.notNull(sort);

        boolean interceptResult = DatabaseInterceptor.INSTANCE.onBeforeFullTextSearched(interceptId);

        ensureSearcher();

        try {
            return SearchResult.createResult(this, searcher.search(((IndexQuery) query).getQuery(), ((IndexFilter) filter).getFilter(),
                    count, sort.createSort(query.getSchema())), modCount);
        } catch (IOException e) {
            throw new RawDatabaseException(e);
        } finally {
            if (interceptResult)
                DatabaseInterceptor.INSTANCE.onAfterFullTextSearched(interceptId);
        }
    }

    @Override
    public List<String> beginSnapshot() {
        Assert.checkState(writer != null);

        flush(true);

        SnapshotDeletionPolicy policy = (SnapshotDeletionPolicy) writer.get().getConfig().getIndexDeletionPolicy();
        try {
            List<String> files = new ArrayList<String>();
            String base = indexManager.getContext().getConfiguration().getPaths().get(schema.getPathIndex());
            files.add(new File(base, space.getFileName()).getPath());

            snapshot = policy.snapshot();
            for (String file : snapshot.getFileNames())
                files.add(new File(path, file).getPath());
            return files;
        } catch (IllegalStateException e) {
            return Collections.emptyList();
        } catch (IOException e) {
            throw new RawDatabaseException(e);
        }
    }

    @Override
    public void endSnapshot() {
        if (snapshot == null)
            return;

        Assert.checkState(writer != null);
        SnapshotDeletionPolicy policy = (SnapshotDeletionPolicy) writer.get().getConfig().getIndexDeletionPolicy();

        try {
            policy.release(snapshot);
            writer.get().deleteUnusedFiles();
        } catch (IOException e) {
            throw new RawDatabaseException(e);
        }
    }

    @Override
    public void setDocumentSpace(IFullTextDocumentSpace space) {
        this.space.setDocumentSpace(space);
    }

    @Override
    public void reindex() {
        space.reindex();
    }

    @Override
    public String toString() {
        return schema.getAlias();
    }

    private FullTextIndex(ITimeService timeService, ITransactionProvider transactionProvider,
                          IndexManager indexManager, IndexSchemaConfiguration schema, String basePath, int fileIndex, String filePrefix, boolean create) {
        super(indexManager, schema, fileIndex);

        this.timeService = timeService;
        this.transactionProvider = transactionProvider;
        this.path = new File(basePath + File.separator + filePrefix + "-" + fileIndex);

        String fileName = filePrefix + "-" + fileIndex + File.separator + "docids-" + (fileIndex + 1) + ".ix";
        if (create)
            this.space = DocumentIdentifiersSpace.create(indexManager.getContext(), this, fileIndex + 1, schema.getPathIndex(), fileName);
        else
            this.space = DocumentIdentifiersSpace.open(indexManager.getContext(), this, fileIndex + 1, schema.getPathIndex(), fileName);

        IndexDatabaseExtension extension = indexManager.getContext().findExtension(IndexDatabaseExtensionConfiguration.NAME);
        Assert.notNull(extension);
        holderManager = extension.getHolderManager();

        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_4_9, new StandardAnalyzer(Version.LUCENE_4_9));
        config.setOpenMode(create ? OpenMode.CREATE : OpenMode.APPEND);
        config.setRAMBufferSizeMB(configuration.getBufferSizePerIndex() / (1 << 20));
        config.setUseCompoundFile(false);
        config.setIndexDeletionPolicy(new SnapshotDeletionPolicy(new KeepOnlyLastCommitDeletionPolicy()));

        try {
            Directory directory = FSDirectory.open(path);
            writer = holderManager.createHolder(new IndexWriter(directory, config));
            lastWriterCommitTime = timeService.getCurrentTime();
            lastStartWriterCommitTime = lastWriterCommitTime;
            lastModifyTime = lastWriterCommitTime - 1;
        } catch (IOException e) {
            throw new RawDatabaseException(e);
        }

        interceptId = ((RawDatabase) indexManager.getContext().getRawDatabase()).getInterceptId();
    }

    private void ensureSearcher() {
        Assert.checkState(writer != null);

        if (searcher == null) {
            boolean interceptResult = DatabaseInterceptor.INSTANCE.onBeforeFullTextSearcherUpdated(interceptId);

            try {
                reader = holderManager.createHolder(DirectoryReader.open(writer.get(), false));
                searcher = new IndexSearcher(reader.get());
                this.lastSearcherUpdateTime = timeService.getCurrentTime();
            } catch (IOException e) {
                throw new RawDatabaseException(e);
            } finally {
                if (interceptResult)
                    DatabaseInterceptor.INSTANCE.onAfterFullTextSearcherUpdated(interceptId);
            }
        }
    }

    private void updateSearcher() {
        lastSearcherUpdateTime = timeService.getCurrentTime();

        final boolean[] interceptResult = new boolean[]{false};
        indexManager.getContext().getCompartment().execute(new ICompartmentTask<Object>() {
            private final Holder<DirectoryReader> reader = FullTextIndex.this.reader;
            private Holder<DirectoryReader> newReader;
            private IndexSearcher newSearcher;

            {
                reader.addRef();
                interceptResult[0] = DatabaseInterceptor.INSTANCE.onBeforeFullTextSearcherUpdated(interceptId);
            }

            @Override
            public Object execute() {
                try {
                    DirectoryReader newReader = DirectoryReader.openIfChanged(reader.get());
                    if (newReader != null) {
                        this.newReader = holderManager.createHolder(newReader);
                        newSearcher = new IndexSearcher(this.newReader.get());

                    }
                } catch (IOException e) {
                    throw new RawDatabaseException(e);
                } finally {
                    reader.release();
                }

                return null;
            }

            @Override
            public boolean isCanceled() {
                return false;
            }

            @Override
            public void onSucceeded(Object result) {
                if (newReader != null) {
                    if (writer != null) {
                        FullTextIndex.this.reader.release();
                        FullTextIndex.this.reader = newReader;
                        searcher = newSearcher;
                        modCount++;
                        lastSearcherUpdateTime = timeService.getCurrentTime();

                        if (logger.isLogEnabled(LogLevel.DEBUG))
                            logger.log(LogLevel.DEBUG, messages.searcherUpdated());
                    } else
                        newReader.release();
                }

                if (interceptResult[0])
                    DatabaseInterceptor.INSTANCE.onAfterFullTextSearcherUpdated(interceptId);
            }

            @Override
            public void onFailed(Throwable error) {
                if (interceptResult[0])
                    DatabaseInterceptor.INSTANCE.onAfterFullTextSearcherUpdated(interceptId);
            }
        });
    }

    private void commitWriter(final boolean force) {
        if (force || lastModifyTime >= lastStartWriterCommitTime) {
            lastStartWriterCommitTime = timeService.getCurrentTime();

            final boolean[] interceptResult = new boolean[]{false};
            ICompartmentTask<Object> task = new ICompartmentTask<Object>() {
                private final Holder<IndexWriter> writer = FullTextIndex.this.writer;

                {
                    interceptResult[0] = DatabaseInterceptor.INSTANCE.onBeforeFullTextCommitted(interceptId);
                    writer.addRef();
                    space.beginCommit();
                }

                @Override
                public Object execute() {
                    try {
                        writer.get().commit();
                    } catch (IOException e) {
                        throw new RawDatabaseException(e);
                    } finally {
                        writer.release();

                        if (force) {
                            space.endCommit();
                            lastWriterCommitTime = timeService.getCurrentTime();

                            if (logger.isLogEnabled(LogLevel.DEBUG))
                                logger.log(LogLevel.DEBUG, messages.indexCommitted());
                        } else {
                            try {
                                indexManager.getContext().getDatabase().transaction(new Operation() {
                                    @Override
                                    public void run(ITransaction transaction) {
                                        space.endCommit();
                                        lastWriterCommitTime = timeService.getCurrentTime();

                                        if (logger.isLogEnabled(LogLevel.DEBUG))
                                            logger.log(LogLevel.DEBUG, messages.indexCommitted());
                                    }
                                });
                            } catch (IllegalStateException e) {
                            }
                        }
                    }

                    return null;
                }

                @Override
                public boolean isCanceled() {
                    return false;
                }

                @Override
                public void onSucceeded(Object result) {
                    if (interceptResult[0])
                        DatabaseInterceptor.INSTANCE.onAfterFullTextCommitted(interceptId);
                }

                @Override
                public void onFailed(Throwable error) {
                    if (interceptResult[0])
                        DatabaseInterceptor.INSTANCE.onAfterFullTextCommitted(interceptId);
                }
            };

            if (!force)
                indexManager.getContext().getCompartment().execute(task);
            else {
                task.execute();
                task.onSucceeded(null);
            }
        }

        lastWriterCommitTime = timeService.getCurrentTime();
    }

    private void checkWriteTransaction() {
        Assert.checkState((transactionProvider.getTransaction().getOptions() & (IOperation.DELAYED_FLUSH | IOperation.READ_ONLY)) == 0);
    }

    private interface IMessages {
        @DefaultMessage("Searcher has been updated.")
        ILocalizedMessage searcherUpdated();

        @DefaultMessage("Index has been committed.")
        ILocalizedMessage indexCommitted();
    }
}
