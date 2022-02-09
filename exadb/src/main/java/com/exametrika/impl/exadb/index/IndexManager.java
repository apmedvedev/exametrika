/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.index;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.exametrika.api.exadb.core.config.DatabaseConfiguration;
import com.exametrika.api.exadb.fulltext.config.FullTextIndexConfiguration;
import com.exametrika.api.exadb.index.IIndexManager;
import com.exametrika.api.exadb.index.config.IndexDatabaseExtensionConfiguration;
import com.exametrika.common.time.ITimeService;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Files;
import com.exametrika.spi.exadb.core.ICacheControl;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.spi.exadb.core.IExtensionSpace;
import com.exametrika.spi.exadb.index.config.schema.IndexSchemaConfiguration;


/**
 * The {@link IndexManager} is an index manager.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class IndexManager implements IIndexManager, ICacheControl, IExtensionSpace {
    private final IDatabaseContext context;
    private final ITimeService timeService;
    private List<ManagerIndexInfo> indexes = Collections.emptyList();
    private IndexesSpace indexesSpace;
    private List<ChangeInfo> changedElements = new ArrayList<ChangeInfo>();
    private List<DeleteInfo> deletedIndexes;
    private List<DeleteInfo> addedDeletedElements = new ArrayList<DeleteInfo>();
    private IndexDatabaseExtensionConfiguration configuration = new IndexDatabaseExtensionConfiguration(60000, new FullTextIndexConfiguration());

    public IndexManager(IDatabaseContext context, IndexDatabaseExtensionConfiguration configuration) {
        Assert.notNull(context);
        Assert.notNull(configuration);

        this.context = context;
        this.configuration = configuration;
        this.timeService = context.getTimeService();
    }

    public IDatabaseContext getContext() {
        return context;
    }

    @Override
    public List<String> getFiles() {
        return getIndexesSpace().getFiles();
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public void create() {
        indexesSpace = IndexesSpace.create(context);
        indexes = new ArrayList<ManagerIndexInfo>();
        deletedIndexes = new LinkedList<DeleteInfo>();
    }

    @Override
    public void open() {
        indexesSpace = IndexesSpace.open(context);

        List<IndexInfo> indexes = new ArrayList<IndexInfo>();
        List<DeleteInfo> deletedIndexes = new LinkedList<DeleteInfo>();
        indexesSpace.readIndexes(indexes, deletedIndexes);
        this.deletedIndexes = deletedIndexes;

        this.indexes = new ArrayList(indexes.size());
        for (IndexInfo info : indexes) {
            ManagerIndexInfo managerInfo = new ManagerIndexInfo(info.schema, info.filePrefix, info.id, null);
            this.indexes.add(managerInfo);
        }
    }

    public ITimeService getTimeService() {
        return timeService;
    }

    public IndexDatabaseExtensionConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(IndexDatabaseExtensionConfiguration configuration) {
        Assert.notNull(configuration);

        this.configuration = configuration;

        for (ManagerIndexInfo info : indexes)
            setConfiguration(info);
    }

    @Override
    public AbstractIndexSpace createIndex(String filePrefix, IndexSchemaConfiguration schema) {
        Assert.notNull(filePrefix);
        Assert.notNull(schema);
        Assert.checkState(!context.getTransactionProvider().getRawTransaction().isReadOnly());

        AbstractIndexSpace index = (AbstractIndexSpace) schema.createIndex(filePrefix, this, context);
        ManagerIndexInfo info = new ManagerIndexInfo(schema, filePrefix, index.getId(), index);
        indexes.add(info);
        setConfiguration(info);

        changedElements.add(new ChangeInfo(info, true));

        return index;
    }

    @Override
    public AbstractIndexSpace getIndex(int id) {
        ManagerIndexInfo info = getIndexById(id);
        if (info.index != null) {
            info.index.refresh();
            return info.index;
        }

        return loadIndex(info);
    }

    @Override
    public ManagerIndexInfo findIndex(int id) {
        for (ManagerIndexInfo info : indexes) {
            if (info.id == id)
                return info;
        }

        return null;
    }

    @Override
    public List<IndexInfo> getIndexes() {
        return new ArrayList<IndexInfo>(indexes);
    }

    public void deleteNonTransactionalIndex(int id, File path, long time) {
        deleteIndex(id);
        addedDeletedElements.add(new DeleteInfo(path, time));
    }

    public void deleteFilesOfNonTransactionalIndex(int id, File path, long time) {
        addedDeletedElements.add(new DeleteInfo(path, time));
    }

    public void deleteIndex(int id) {
        Assert.checkState(!context.getTransactionProvider().getRawTransaction().isReadOnly());

        ManagerIndexInfo info = getIndexById(id);
        Assert.checkState(info.index != null);

        info.index.setStale();
        info.index = null;
        indexes.remove(info);

        changedElements.add(new ChangeInfo(info, false));
    }

    public void unloadIndex(int id) {
        ManagerIndexInfo info = getIndexById(id);
        if (info.index != null) {
            info.index.unload(true);
            info.index.setStale();
            info.index = null;
        }
    }

    @Override
    public void validate() {
    }

    @Override
    public void onTransactionStarted() {
        for (ManagerIndexInfo info : indexes) {
            if (info.index != null)
                info.index.onTransactionStarted();
        }
    }

    @Override
    public void onTransactionCommitted() {
        for (ManagerIndexInfo info : indexes) {
            if (info.index != null)
                info.index.onTransactionCommitted();
        }

        deletedIndexes.addAll(addedDeletedElements);

        if (!changedElements.isEmpty() || !addedDeletedElements.isEmpty())
            getIndexesSpace().writeIndexes(indexes, deletedIndexes);

        changedElements.clear();
        addedDeletedElements.clear();
    }

    @Override
    public boolean onBeforeTransactionRolledBack() {
        boolean res = false;
        for (ManagerIndexInfo info : indexes) {
            if (info.index != null)
                res = info.index.onBeforeTransactionRolledBack() || res;
        }

        return res;
    }

    @Override
    public void onTransactionRolledBack() {
        for (ManagerIndexInfo info : indexes) {
            if (info.index != null)
                info.index.onTransactionRolledBack();
        }

        if (!changedElements.isEmpty()) {
            for (int i = changedElements.size() - 1; i >= 0; i--) {
                ChangeInfo info = changedElements.get(i);
                if (info.added) {
                    if (info.info.index != null) {
                        info.info.index.unload(true);
                        info.info.index.setStale();
                        info.info.index = null;
                    }

                    indexes.remove(info.info);
                } else
                    indexes.add(info.info);
            }
        }

        changedElements.clear();
        addedDeletedElements.clear();
    }

    @Override
    public void flush(boolean full) {
        for (ManagerIndexInfo info : indexes) {
            if (info.index != null)
                info.index.flush(full);
        }
    }

    @Override
    public void clear(boolean full) {
        for (ManagerIndexInfo info : indexes) {
            if (info.index != null) {
                info.index.unload(full);

                if (full) {
                    info.index.setStale();
                    info.index = null;
                }
            }
        }

        if (full)
            indexesSpace = null;
    }

    @Override
    public void unloadExcessive() {
    }

    @Override
    public void setCachingEnabled(boolean value) {
    }

    @Override
    public void setMaxCacheSize(String category, long value) {
    }

    public void onTimer(long currentTime) {
        for (ManagerIndexInfo info : indexes) {
            if (info.index == null)
                continue;

            if (currentTime > info.index.getLastAccessTime() + configuration.getIndexIdlePeriod()) {
                info.index.unload(true);
                info.index.setStale();
                info.index = null;
            } else
                info.index.onTimer(currentTime);
        }

        boolean changed = false;
        for (Iterator<DeleteInfo> it = deletedIndexes.iterator(); it.hasNext(); ) {
            DeleteInfo info = it.next();
            if (currentTime > info.time) {
                it.remove();
                Files.delete(info.path);
                changed = true;
            }
        }

        if (changed)
            getIndexesSpace().writeIndexes(indexes, deletedIndexes);
    }

    public String printStatistics() {
        StringBuilder builder = new StringBuilder();

        boolean first = true;
        for (ManagerIndexInfo info : indexes) {
            if (info.index == null)
                continue;

            if (first)
                first = false;
            else
                builder.append('\n');

            builder.append(info.index.printStatistics());
        }

        return builder.toString();
    }

    private ManagerIndexInfo getIndexById(int id) {
        ManagerIndexInfo info = findIndex(id);
        Assert.notNull(info);
        return info;
    }

    private AbstractIndexSpace loadIndex(ManagerIndexInfo info) {
        info.index = (AbstractIndexSpace) info.schema.openIndex(info.id, info.filePrefix, this, context);
        setConfiguration(info);
        return info.index;
    }

    private void setConfiguration(ManagerIndexInfo info) {
        if (info.index == null)
            return;

        DatabaseConfiguration databaseConfiguration = context.getConfiguration();

        if (info.index.getCategoryTypeConfiguration() != null) {
            if (info.index.getCategoryTypeConfiguration().getName().isEmpty())
                info.index.setCategoryTypeConfiguration(databaseConfiguration.getDefaultCacheCategoryType());
            else
                info.index.setCategoryTypeConfiguration(databaseConfiguration.getCacheCategoryTypes().get(info.index.getCategoryTypeConfiguration().getName()));
        }

        info.index.setConfiguration(configuration);
    }

    private IndexesSpace getIndexesSpace() {
        if (indexesSpace == null)
            indexesSpace = IndexesSpace.open(context);

        return indexesSpace;
    }

    private static class ChangeInfo {
        private final ManagerIndexInfo info;
        private final boolean added;

        public ChangeInfo(ManagerIndexInfo info, boolean added) {
            this.info = info;
            this.added = added;
        }
    }

    static class DeleteInfo {
        final File path;
        final long time;

        public DeleteInfo(File path, long time) {
            this.path = path;
            this.time = time;
        }
    }

    private static class ManagerIndexInfo extends IndexInfo {
        private AbstractIndexSpace index;

        public ManagerIndexInfo(IndexSchemaConfiguration schema, String filePrefix, int id, AbstractIndexSpace index) {
            super(schema, filePrefix, id);

            this.index = index;
        }
    }
}
