/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.ops;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.exametrika.api.aggregator.IPeriodOperationManager;
import com.exametrika.api.aggregator.schema.ICycleSchema;
import com.exametrika.api.aggregator.schema.IPeriodSpaceSchema;
import com.exametrika.api.exadb.core.IOperation;
import com.exametrika.api.exadb.core.ITransaction;
import com.exametrika.api.exadb.core.Operation;
import com.exametrika.api.exadb.core.config.schema.NullArchiveStoreSchemaConfiguration;
import com.exametrika.api.exadb.core.schema.IDatabaseSchema;
import com.exametrika.api.exadb.core.schema.IDomainSchema;
import com.exametrika.api.exadb.core.schema.ISpaceSchema;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Files;
import com.exametrika.common.utils.ICompletionHandler;
import com.exametrika.common.utils.NameFilter;
import com.exametrika.common.utils.SimpleList;
import com.exametrika.common.utils.SyncCompletionHandler;
import com.exametrika.impl.aggregator.PeriodCycle;
import com.exametrika.impl.aggregator.PeriodSpaces;
import com.exametrika.impl.aggregator.cache.PeriodNodeCacheManager;
import com.exametrika.impl.aggregator.schema.CycleSchema;
import com.exametrika.impl.exadb.core.Spaces;
import com.exametrika.impl.exadb.objectdb.NodeSpace;
import com.exametrika.spi.aggregator.IArchivePolicy;
import com.exametrika.spi.aggregator.ITruncationPolicy;
import com.exametrika.spi.aggregator.config.schema.ArchivePolicySchemaConfiguration;
import com.exametrika.spi.aggregator.config.schema.TruncationPolicySchemaConfiguration;
import com.exametrika.spi.exadb.core.IArchiveStore;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.spi.exadb.core.config.schema.ArchiveStoreSchemaConfiguration;


/**
 * The {@link PeriodOperationManager} is a period operation manager.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class PeriodOperationManager implements IPeriodOperationManager {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(PeriodOperationManager.class);
    private final PeriodNodeCacheManager nodeCacheManager;
    private final IDatabaseContext context;
    private final SimpleList<SyncCompletionHandler> handlers = new SimpleList<SyncCompletionHandler>();
    private boolean started;
    private boolean stopped;

    public PeriodOperationManager(PeriodNodeCacheManager nodeCacheManager, IDatabaseContext context) {
        Assert.notNull(nodeCacheManager);
        Assert.notNull(context);

        this.nodeCacheManager = nodeCacheManager;
        this.context = context;
    }

    @Override
    public void archiveCycles(final NameFilter spaceFilter, final List<String> periods,
                              final ArchivePolicySchemaConfiguration archivePolicyConfiguration, final ArchiveStoreSchemaConfiguration archiveStore,
                              ICompletionHandler completionHandler) {
        Assert.notNull(archivePolicyConfiguration);
        Assert.notNull(archiveStore);
        Assert.checkState(context.getDatabase().isOpened() && !context.getDatabase().isClosed());

        final ICompletionHandler usedCompletionHandler;
        SyncCompletionHandler syncCompletionHandler = null;
        synchronized (handlers) {
            Assert.checkState(started && !stopped);

            if (completionHandler != null)
                usedCompletionHandler = completionHandler;
            else {
                syncCompletionHandler = new SyncCompletionHandler(handlers);
                usedCompletionHandler = syncCompletionHandler;
            }
        }

        if (logger.isLogEnabled(LogLevel.INFO))
            logger.log(LogLevel.INFO, messages.archiveStarted(archiveStore));

        context.getDatabase().transaction(new Operation(IOperation.FLUSH) {
            private String name;
            private List<String> paths;
            private List<List<String>> cyclesFileNames = new ArrayList<List<String>>();
            private List<String> cycleIds = new ArrayList<String>();
            private List<PeriodCycle> cycles = new ArrayList<PeriodCycle>();

            @Override
            public List<String> getBatchLockPredicates() {
                return Arrays.asList("db.aggregation");
            }

            @Override
            public void run(ITransaction transaction) {
                IDatabaseSchema schema = transaction.getCurrentSchema();
                if (schema == null)
                    return;

                IArchivePolicy archivePolicy = archivePolicyConfiguration.createPolicy();
                name = schema.getConfiguration().getName();
                paths = transaction.getDatabase().getConfiguration().getPaths();

                boolean isNullStore = archiveStore instanceof NullArchiveStoreSchemaConfiguration;

                for (IDomainSchema domainSchema : schema.getDomains()) {
                    for (ISpaceSchema spaceSchema : domainSchema.getSpaces()) {
                        if (spaceSchema instanceof IPeriodSpaceSchema) {
                            if (spaceFilter != null && !spaceFilter.match(spaceSchema.getConfiguration().getAlias()))
                                continue;

                            for (ICycleSchema cycleSchema : ((IPeriodSpaceSchema) spaceSchema).getCycles()) {
                                if (periods != null && !periods.contains(cycleSchema.getConfiguration().getAlias()))
                                    continue;

                                if (archivePolicy.allow(cycleSchema.getCurrentCycle().getSpace())) {
                                    ((CycleSchema) cycleSchema).addCycle(null, null);
                                    ((CycleSchema) cycleSchema).addPeriod();
                                }

                                PeriodCycle cycle = (PeriodCycle) cycleSchema.getCurrentCycle().getPreviousCycle();
                                while (cycle != null) {
                                    if (cycle.isArchived())
                                        break;

                                    if (!isNullStore) {
                                        List<String> files = new ArrayList<String>();
                                        files.addAll(cycle.beginSnapshot());
                                        files.addAll(PeriodSpaces.getPeriodSpaceFileNames(
                                                context.getConfiguration().getPaths(), cycle.getSchema().getParent().getConfiguration().getPathIndex(),
                                                cycle.getSchema().getParent().getParent().getConfiguration().getName(),
                                                cycle.getSchema().getParent().getConfiguration(),
                                                cycle.getSchema().getConfiguration(), cycle.getDataFileIndex(),
                                                cycle.getCycleSpaceFileIndex(), 0, 0, 0));
                                        cyclesFileNames.add(files);
                                        cycleIds.add(cycle.getId());
                                        cycles.add(cycle);
                                    } else {
                                        cycle.setArchived();

                                        if (logger.isLogEnabled(LogLevel.INFO))
                                            logger.log(LogLevel.INFO, messages.cycleArchived(cycle.getId(), archiveStore));
                                    }

                                    cycle = cycle.getPreviousCycle();
                                }
                            }
                        }
                    }
                }
            }

            @Override
            public void onCommitted() {
                if (cyclesFileNames.isEmpty()) {
                    if (logger.isLogEnabled(LogLevel.INFO))
                        logger.log(LogLevel.INFO, messages.archiveCompleted(archiveStore));

                    usedCompletionHandler.onSucceeded(null);
                    return;
                }

                File temp = Files.createTempDirectory("ops");

                try {
                    List<CycleFiles> cyclesFiles = new ArrayList<CycleFiles>();
                    for (int i = 0; i < cyclesFileNames.size(); i++) {
                        File cycleDir = new File(temp, Integer.toString(i));

                        List<String> files = cyclesFileNames.get(i);

                        CycleFiles cycleFiles = new CycleFiles();
                        cycleFiles.cycleId = cycleIds.get(i);
                        cycleFiles.directory = cycleDir;

                        for (int k = 0; k < paths.size(); k++) {
                            String path = paths.get(k);
                            File destDir = (paths.size() > 1) ? new File(cycleDir, Integer.toString(k)) : cycleDir;

                            List<String> snapshotFiles = new ArrayList<String>();
                            for (String filePath : files) {
                                File file = new File(filePath);
                                if (file.isFile())
                                    snapshotFiles.add(filePath);
                                else if (file.isDirectory())
                                    Spaces.getSpaceFiles(file, snapshotFiles);
                            }

                            for (String file : snapshotFiles) {
                                String relativeFile = Files.relativize(file, path);
                                if (relativeFile == null)
                                    continue;

                                File source = new File(file);
                                File destination = new File(destDir, relativeFile);

                                if (source.exists())
                                    Files.copy(source, destination);
                            }
                        }

                        cyclesFiles.add(cycleFiles);
                    }

                    context.getCompartment().execute(new ArchiveOperation(name, temp, cyclesFiles, archiveStore, usedCompletionHandler));
                } catch (Exception e) {
                    Files.delete(temp);

                    if (logger.isLogEnabled(LogLevel.ERROR))
                        logger.log(LogLevel.ERROR, messages.archiveFailed(archiveStore), e);

                    usedCompletionHandler.onFailed(e);
                }

                for (PeriodCycle cycle : cycles)
                    cycle.endSnapshot();
            }

            @Override
            public void onRolledBack() {
                context.getCacheControl().clear(true);

                if (logger.isLogEnabled(LogLevel.ERROR))
                    logger.log(LogLevel.ERROR, messages.archiveFailed(archiveStore));

                usedCompletionHandler.onFailed(null);

                for (PeriodCycle cycle : cycles)
                    cycle.endSnapshot();
            }
        });

        if (syncCompletionHandler != null)
            syncCompletionHandler.await();
    }

    @Override
    public void truncateCycles(final NameFilter spaceFilter, final List<String> periods,
                               final TruncationPolicySchemaConfiguration truncationPolicyConfiguration, final boolean ignoreRestored,
                               ICompletionHandler completionHandler) {
        Assert.notNull(truncationPolicyConfiguration);
        Assert.checkState(context.getDatabase().isOpened() && !context.getDatabase().isClosed());

        final ICompletionHandler usedCompletionHandler;
        SyncCompletionHandler syncCompletionHandler = null;
        synchronized (handlers) {
            Assert.checkState(started && !stopped);

            if (completionHandler != null)
                usedCompletionHandler = completionHandler;
            else {
                syncCompletionHandler = new SyncCompletionHandler(handlers);
                usedCompletionHandler = syncCompletionHandler;
            }
        }

        if (logger.isLogEnabled(LogLevel.INFO))
            logger.log(LogLevel.INFO, messages.truncationStarted());

        context.getDatabase().transaction(new Operation(IOperation.FLUSH) {
            @Override
            public List<String> getBatchLockPredicates() {
                return Arrays.asList("db.aggregation");
            }

            @Override
            public void run(ITransaction transaction) {
                IDatabaseSchema schema = transaction.getCurrentSchema();
                if (schema == null)
                    return;

                ITruncationPolicy truncationPolicy = truncationPolicyConfiguration.createPolicy();
                List<PeriodCycle> truncatingCycles = new ArrayList<PeriodCycle>();

                for (IDomainSchema domainSchema : schema.getDomains()) {
                    for (ISpaceSchema spaceSchema : domainSchema.getSpaces()) {
                        if (spaceSchema instanceof IPeriodSpaceSchema) {
                            if (spaceFilter != null && !spaceFilter.match(spaceSchema.getConfiguration().getAlias()))
                                continue;

                            if (!(spaceSchema instanceof IPeriodSpaceSchema))
                                Assert.error();

                            for (ICycleSchema cycleSchema : ((IPeriodSpaceSchema) spaceSchema).getCycles()) {
                                if (periods != null && !periods.contains(cycleSchema.getConfiguration().getAlias()))
                                    continue;

                                PeriodCycle cycle = (PeriodCycle) cycleSchema.getCurrentCycle().getPreviousCycle();
                                while (cycle != null) {
                                    if (cycle.isArchived() && !cycle.isDeleted() && (!ignoreRestored || !cycle.isRestored()))
                                        truncatingCycles.add(cycle);

                                    cycle = cycle.getPreviousCycle();
                                }
                            }
                        }
                    }
                }

                Collections.sort(truncatingCycles, new Comparator<PeriodCycle>() {
                    @Override
                    public int compare(PeriodCycle o1, PeriodCycle o2) {
                        Long t1 = o1.getEndTime();
                        Long t2 = o2.getEndTime();
                        return t1.compareTo(t2);
                    }
                });

                Set<NodeSpace> unloadedSpaces = new HashSet<NodeSpace>();
                for (PeriodCycle cycle : truncatingCycles) {
                    if (truncationPolicy.allow(cycle)) {
                        NodeSpace space = cycle.delete();
                        if (space != null)
                            unloadedSpaces.add(space);

                        if (logger.isLogEnabled(LogLevel.INFO))
                            logger.log(LogLevel.INFO, messages.cycleDeleted(cycle.getId()));
                    }
                }

                if (!unloadedSpaces.isEmpty())
                    nodeCacheManager.unloadNodesOfDeletedSpaces(unloadedSpaces);
            }

            @Override
            public void onCommitted() {
                if (logger.isLogEnabled(LogLevel.INFO))
                    logger.log(LogLevel.INFO, messages.truncationCompleted());

                usedCompletionHandler.onSucceeded(null);
            }

            @Override
            public void onRolledBack() {
                if (logger.isLogEnabled(LogLevel.ERROR))
                    logger.log(LogLevel.ERROR, messages.truncationFailed());

                context.getCacheControl().clear(true);

                usedCompletionHandler.onFailed(null);
            }
        });

        if (syncCompletionHandler != null)
            syncCompletionHandler.await();
    }

    @Override
    public void restoreCycles(Set<String> cycleIds, ArchiveStoreSchemaConfiguration archiveStore, ICompletionHandler completionHandler) {
        Assert.notNull(cycleIds);
        Assert.notNull(archiveStore);
        if (cycleIds.isEmpty())
            return;

        Assert.checkState(context.getDatabase().isOpened() && !context.getDatabase().isClosed());
        final ICompletionHandler usedCompletionHandler;
        SyncCompletionHandler syncCompletionHandler = null;
        synchronized (handlers) {
            Assert.checkState(started && !stopped);

            if (completionHandler != null)
                usedCompletionHandler = completionHandler;
            else {
                syncCompletionHandler = new SyncCompletionHandler(handlers);
                usedCompletionHandler = syncCompletionHandler;
            }
        }

        if (logger.isLogEnabled(LogLevel.INFO))
            logger.log(LogLevel.INFO, messages.restoreStarted(archiveStore));

        context.getCompartment().execute(new RestoreOperation(context.getSchemaSpace().getCurrentSchema().getConfiguration().getName(),
                context.getConfiguration().getPaths(), cycleIds, archiveStore, usedCompletionHandler));

        if (syncCompletionHandler != null)
            syncCompletionHandler.await();
    }

    public void start() {
        synchronized (handlers) {
            Assert.checkState(!started);

            started = true;
        }
    }

    public void stop() {
        synchronized (handlers) {
            if (!started || stopped)
                return;

            stopped = true;

            for (SyncCompletionHandler handler : handlers.values())
                handler.cancel();
        }
    }

    private class ArchiveOperation implements Runnable {
        private final String databaseName;
        private final File snapshotPath;
        private final ArchiveStoreSchemaConfiguration archiveStore;
        private final List<CycleFiles> cyclesFiles;
        private final ICompletionHandler completionHandler;

        public ArchiveOperation(String databaseName, File snapshotPath, List<CycleFiles> cyclesFiles,
                                ArchiveStoreSchemaConfiguration archiveStore, ICompletionHandler completionHandler) {
            Assert.notNull(databaseName);
            Assert.notNull(snapshotPath);
            Assert.notNull(cyclesFiles);
            Assert.notNull(archiveStore);

            this.databaseName = databaseName;
            this.snapshotPath = snapshotPath;
            this.cyclesFiles = cyclesFiles;
            this.archiveStore = archiveStore;
            this.completionHandler = completionHandler;
        }

        @Override
        public void run() {
            IArchiveStore archiveStore = this.archiveStore.createStore();

            final Set<String> archivedCycles = new HashSet<String>();

            for (CycleFiles cycle : cyclesFiles) {
                File temp = null;
                try {
                    temp = File.createTempFile("ops", "tmp");
                    Files.zip(cycle.directory, temp);
                    archiveStore.save(databaseName + "-" + cycle.cycleId, temp.getPath());
                    archivedCycles.add(cycle.cycleId);
                } catch (Exception e) {
                    if (logger.isLogEnabled(LogLevel.ERROR))
                        logger.log(LogLevel.ERROR, e);
                } finally {
                    if (temp != null)
                        Files.delete(temp);
                }
            }

            archiveStore.close();

            Files.delete(snapshotPath);

            if (archivedCycles.isEmpty()) {
                if (logger.isLogEnabled(LogLevel.ERROR))
                    logger.log(LogLevel.ERROR, messages.archiveFailed(this.archiveStore));

                if (completionHandler != null)
                    completionHandler.onFailed(null);

                return;
            }

            context.getDatabase().transaction(new Operation() {
                @Override
                public List<String> getBatchLockPredicates() {
                    return Arrays.asList("db.aggregation");
                }

                @Override
                public void run(ITransaction transaction) {
                    IDatabaseSchema schema = transaction.getCurrentSchema();
                    if (schema == null)
                        return;

                    for (IDomainSchema domainSchema : schema.getDomains()) {
                        for (ISpaceSchema spaceSchema : domainSchema.getSpaces()) {
                            if (spaceSchema instanceof IPeriodSpaceSchema) {
                                for (ICycleSchema cycleSchema : ((IPeriodSpaceSchema) spaceSchema).getCycles()) {
                                    PeriodCycle cycle = (PeriodCycle) cycleSchema.getCurrentCycle().getPreviousCycle();
                                    while (cycle != null) {
                                        if (cycle.isArchived())
                                            break;

                                        if (archivedCycles.contains(cycle.getId())) {
                                            cycle.setArchived();

                                            if (logger.isLogEnabled(LogLevel.INFO))
                                                logger.log(LogLevel.INFO, messages.cycleArchived(cycle.getId(), ArchiveOperation.this.archiveStore));
                                        }

                                        cycle = cycle.getPreviousCycle();
                                    }
                                }
                            }
                        }
                    }
                }

                @Override
                public void onCommitted() {
                    if (logger.isLogEnabled(LogLevel.INFO))
                        logger.log(LogLevel.INFO, messages.archiveCompleted(ArchiveOperation.this.archiveStore));

                    if (completionHandler != null)
                        completionHandler.onSucceeded(null);
                }

                @Override
                public void onRolledBack() {
                    if (logger.isLogEnabled(LogLevel.ERROR))
                        logger.log(LogLevel.ERROR, messages.archiveFailed(ArchiveOperation.this.archiveStore));

                    context.getCacheControl().clear(true);

                    if (completionHandler != null)
                        completionHandler.onFailed(null);
                }
            });
        }
    }

    private class RestoreOperation implements Runnable {
        private final String databaseName;
        private final List<File> paths;
        private final Set<String> cycleIds;
        private final ArchiveStoreSchemaConfiguration archiveStore;
        private final ICompletionHandler completionHandler;

        public RestoreOperation(String databaseName, List<String> paths, Set<String> cycleIds, ArchiveStoreSchemaConfiguration archiveStore,
                                ICompletionHandler completionHandler) {
            Assert.notNull(databaseName);
            Assert.notNull(paths);
            Assert.notNull(cycleIds);
            Assert.notNull(archiveStore);

            this.databaseName = databaseName;
            this.paths = new ArrayList<File>();
            for (String path : paths)
                this.paths.add(new File(path));
            this.cycleIds = cycleIds;
            this.archiveStore = archiveStore;
            this.completionHandler = completionHandler;
        }

        @Override
        public void run() {
            IArchiveStore archiveStore = this.archiveStore.createStore();

            File dir = Files.createTempDirectory("ops");
            final Set<String> restoredCycleIds = new HashSet<String>();
            for (String cycleId : cycleIds) {
                File temp = null;
                try {
                    temp = File.createTempFile("ops", "tmp");
                    archiveStore.load(databaseName + "-" + cycleId, temp.getPath());

                    Files.unzip(temp, dir);
                    if (paths.size() == 1)
                        Files.copy(dir, paths.get(0));
                    else {
                        for (int i = 0; i < paths.size(); i++) {
                            File destination = paths.get(i);
                            File source = new File(dir, Integer.toString(i));
                            if (source.exists())
                                Files.copy(source, destination);
                        }
                    }
                    restoredCycleIds.add(cycleId);
                } catch (Exception e) {
                    if (logger.isLogEnabled(LogLevel.ERROR))
                        logger.log(LogLevel.ERROR, e);
                } finally {
                    if (temp != null)
                        Files.delete(temp);
                }

                Files.emptyDir(dir);
            }

            dir.delete();

            archiveStore.close();

            if (restoredCycleIds.isEmpty()) {
                if (logger.isLogEnabled(LogLevel.ERROR))
                    logger.log(LogLevel.ERROR, messages.restoreFailed(this.archiveStore));

                if (completionHandler != null)
                    completionHandler.onFailed(null);

                return;
            }

            context.getDatabase().transaction(new Operation() {
                @Override
                public void run(ITransaction transaction) {
                    IDatabaseSchema schema = transaction.getCurrentSchema();
                    if (schema == null)
                        return;

                    for (IDomainSchema domainSchema : schema.getDomains()) {
                        for (ISpaceSchema spaceSchema : domainSchema.getSpaces()) {
                            if (spaceSchema instanceof IPeriodSpaceSchema) {
                                for (ICycleSchema cycleSchema : ((IPeriodSpaceSchema) spaceSchema).getCycles()) {
                                    PeriodCycle cycle = (PeriodCycle) cycleSchema.getCurrentCycle().getPreviousCycle();
                                    while (cycle != null) {
                                        if (restoredCycleIds.contains(cycle.getId())) {
                                            cycle.setRestored();

                                            if (logger.isLogEnabled(LogLevel.INFO))
                                                logger.log(LogLevel.INFO, messages.cycleRestored(cycle.getId()));
                                        }

                                        cycle = cycle.getPreviousCycle();
                                    }
                                }
                            }
                        }
                    }
                }

                @Override
                public void onCommitted() {
                    if (logger.isLogEnabled(LogLevel.INFO))
                        logger.log(LogLevel.INFO, messages.restoreCompleted(RestoreOperation.this.archiveStore));

                    if (completionHandler != null)
                        completionHandler.onSucceeded(null);
                }

                @Override
                public void onRolledBack() {
                    if (logger.isLogEnabled(LogLevel.ERROR))
                        logger.log(LogLevel.ERROR, messages.restoreFailed(RestoreOperation.this.archiveStore));

                    context.getCacheControl().clear(true);

                    if (completionHandler != null)
                        completionHandler.onFailed(null);
                }
            });
        }
    }

    private static class CycleFiles {
        private String cycleId;
        private File directory;
    }

    private interface IMessages {
        @DefaultMessage("Cycle archiving to ''{0}'' has been started.")
        ILocalizedMessage archiveStarted(ArchiveStoreSchemaConfiguration archiveStore);

        @DefaultMessage("Cycle archiving to ''{0}'' has been completed.")
        ILocalizedMessage archiveCompleted(ArchiveStoreSchemaConfiguration archiveStore);

        @DefaultMessage("Cycle archiving to ''{0}'' has been failed.")
        ILocalizedMessage archiveFailed(ArchiveStoreSchemaConfiguration archiveStore);

        @DefaultMessage("Cycle ''{0}'' has been archived to ''{1}''.")
        ILocalizedMessage cycleArchived(String id, ArchiveStoreSchemaConfiguration archiveStore);

        @DefaultMessage("Cycles truncation has been started.")
        ILocalizedMessage truncationStarted();

        @DefaultMessage("Cycles truncation has been completed.")
        ILocalizedMessage truncationCompleted();

        @DefaultMessage("Cycles truncation has been failed.")
        ILocalizedMessage truncationFailed();

        @DefaultMessage("Cycle ''{0}'' has been deleted.")
        ILocalizedMessage cycleDeleted(String id);

        @DefaultMessage("Cycles restore from ''{0}'' has been started.")
        ILocalizedMessage restoreStarted(ArchiveStoreSchemaConfiguration archiveStore);

        @DefaultMessage("Cycles restore from ''{0}'' has been completed.")
        ILocalizedMessage restoreCompleted(ArchiveStoreSchemaConfiguration archiveStore);

        @DefaultMessage("Cycles restore from ''{0}'' has been failed.")
        ILocalizedMessage restoreFailed(ArchiveStoreSchemaConfiguration archiveStore);

        @DefaultMessage("Cycle ''{0}'' has been restored.")
        ILocalizedMessage cycleRestored(String id);
    }
}
