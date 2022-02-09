/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.core.ops;

import com.exametrika.api.exadb.core.IDumpContext;
import com.exametrika.api.exadb.core.IOperation;
import com.exametrika.api.exadb.core.IOperationManager;
import com.exametrika.api.exadb.core.ITransaction;
import com.exametrika.api.exadb.core.Operation;
import com.exametrika.api.exadb.core.schema.IDatabaseSchema;
import com.exametrika.api.exadb.core.schema.IDomainSchema;
import com.exametrika.api.exadb.core.schema.ISpaceSchema;
import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.CompletionHandler;
import com.exametrika.common.utils.Files;
import com.exametrika.common.utils.ICompletionHandler;
import com.exametrika.common.utils.SimpleList;
import com.exametrika.common.utils.SyncCompletionHandler;
import com.exametrika.impl.exadb.core.Spaces;
import com.exametrika.spi.exadb.core.IArchiveStore;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.spi.exadb.core.ISpaceSchemaControl;
import com.exametrika.spi.exadb.core.config.schema.ArchiveStoreSchemaConfiguration;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * The {@link OperationManager} is an operation manager.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class OperationManager implements IOperationManager {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(OperationManager.class);
    private final IDatabaseContext context;
    private final SimpleList<SyncCompletionHandler> handlers = new SimpleList<SyncCompletionHandler>();
    private boolean started;
    private boolean stopped;

    public OperationManager(IDatabaseContext context) {
        Assert.notNull(context);

        this.context = context;
    }

    @Override
    public void snapshot(final String snapshotDirectoryPath, ICompletionHandler completionHandler) {
        Assert.notNull(snapshotDirectoryPath);
        Assert.checkState(context.getDatabase().isOpened() && !context.getDatabase().isClosed());
        if (context.getSchemaSpace().getCurrentSchema() == null)
            return;

        new File(snapshotDirectoryPath).mkdirs();

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
            logger.log(LogLevel.INFO, messages.snapshotStarted(snapshotDirectoryPath));

        snapshot(new ZipOperation(new File(snapshotDirectoryPath,
                context.getSchemaSpace().getCurrentSchema().getConfiguration().getName() + "-" + createArchiveName() + ".zip"),
                new CompletionHandler() {
                    @Override
                    public boolean isCanceled() {
                        return usedCompletionHandler.isCanceled();
                    }

                    @Override
                    public void onSucceeded(Object value) {
                        if (logger.isLogEnabled(LogLevel.INFO))
                            logger.log(LogLevel.INFO, messages.snapshotCompleted(snapshotDirectoryPath));

                        usedCompletionHandler.onSucceeded(value);
                    }

                    @Override
                    public void onFailed(Throwable error) {
                        if (logger.isLogEnabled(LogLevel.ERROR))
                            logger.log(LogLevel.ERROR, messages.snapshotFailed(snapshotDirectoryPath), error);

                        usedCompletionHandler.onFailed(error);
                    }
                }));

        if (syncCompletionHandler != null)
            syncCompletionHandler.await();
    }

    @Override
    public void dump(final String dumpDirectoryPath, IDumpContext dumpContext, ICompletionHandler completionHandler) {
        Assert.notNull(dumpDirectoryPath);
        Assert.notNull(dumpContext);
        Assert.checkState(context.getDatabase().isOpened() && !context.getDatabase().isClosed());
        if (context.getSchemaSpace().getCurrentSchema() == null)
            return;

        new File(dumpDirectoryPath).mkdirs();

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
            logger.log(LogLevel.INFO, messages.dumpStarted(dumpDirectoryPath));

        dump(dumpContext, new DumpOperation(dumpDirectoryPath,
                new CompletionHandler() {
                    @Override
                    public boolean isCanceled() {
                        return usedCompletionHandler.isCanceled();
                    }

                    @Override
                    public void onSucceeded(Object value) {
                        if (logger.isLogEnabled(LogLevel.INFO))
                            logger.log(LogLevel.INFO, messages.dumpCompleted(dumpDirectoryPath));

                        usedCompletionHandler.onSucceeded(value);
                    }

                    @Override
                    public void onFailed(Throwable error) {
                        if (logger.isLogEnabled(LogLevel.ERROR))
                            logger.log(LogLevel.ERROR, messages.dumpFailed(dumpDirectoryPath), error);

                        usedCompletionHandler.onFailed(error);
                    }
                }, (dumpContext.getFlags() & IDumpContext.COMPRESS) != 0));

        if (syncCompletionHandler != null)
            syncCompletionHandler.await();
    }

    @Override
    public void backup(final ArchiveStoreSchemaConfiguration archiveStore, ICompletionHandler completionHandler) {
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
            logger.log(LogLevel.INFO, messages.backupStarted(archiveStore));

        snapshot(new BackupOperation(archiveStore, new CompletionHandler() {
            @Override
            public boolean isCanceled() {
                return usedCompletionHandler.isCanceled();
            }

            @Override
            public void onSucceeded(Object value) {
                if (logger.isLogEnabled(LogLevel.INFO))
                    logger.log(LogLevel.INFO, messages.backupCompleted(archiveStore));

                usedCompletionHandler.onSucceeded(value);
            }

            @Override
            public void onFailed(Throwable error) {
                if (logger.isLogEnabled(LogLevel.ERROR))
                    logger.log(LogLevel.ERROR, messages.backupFailed(archiveStore), error);

                usedCompletionHandler.onFailed(error);
            }
        }));

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

    private void snapshot(final SnapshotOperation operation) {
        context.getDatabase().transaction(new Operation(IOperation.FLUSH) {
            private String name;
            private List<String> paths;
            private List<String> files = new ArrayList<String>();
            private IDatabaseSchema schema;

            @Override
            public List<String> getBatchLockPredicates() {
                return Arrays.asList("db");
            }

            @Override
            public void run(ITransaction transaction) {
                schema = transaction.getCurrentSchema();
                if (schema == null)
                    return;

                name = schema.getConfiguration().getName();
                paths = transaction.getDatabase().getConfiguration().getPaths();
                files.addAll(Spaces.getSystemFiles(paths.get(0)));
                files.addAll(context.getExtensionSpace().getFiles());

                for (IDomainSchema domainSchema : schema.getDomains()) {
                    for (ISpaceSchema spaceSchema : domainSchema.getSpaces())
                        files.addAll(((ISpaceSchemaControl) spaceSchema).beginSnapshot());
                }
            }

            @Override
            public void onCommitted() {
                if (files.isEmpty()) {
                    operation.onSucceeded(null);
                    return;
                }

                File temp = Files.createTempDirectory("ops");

                try {
                    for (int k = 0; k < paths.size(); k++) {
                        String path = paths.get(k);
                        File destDir = (paths.size() > 1) ? new File(temp, Integer.toString(k)) : temp;

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

                    operation.databaseName = name;
                    operation.snapshotPath = temp;
                    context.getCompartment().execute(operation);
                } catch (Exception e) {
                    Files.delete(temp);

                    operation.onFailed(e);
                }

                if (schema != null) {
                    for (IDomainSchema domainSchema : schema.getDomains()) {
                        for (ISpaceSchema spaceSchema : domainSchema.getSpaces())
                            ((ISpaceSchemaControl) spaceSchema).endSnapshot();
                    }
                }
            }

            @Override
            public void onRolledBack() {
                operation.onFailed(null);

                if (schema != null) {
                    for (IDomainSchema domainSchema : schema.getDomains()) {
                        for (ISpaceSchema spaceSchema : domainSchema.getSpaces())
                            ((ISpaceSchemaControl) spaceSchema).endSnapshot();
                    }
                }
            }
        });
    }

    private void dump(final IDumpContext dumpContext, final SnapshotOperation operation) {
        final Set<String> spaces;
        if (dumpContext.getQuery() != null && dumpContext.getQuery().contains("spaces"))
            spaces = JsonUtils.toSet((JsonArray) dumpContext.getQuery().get("spaces"));
        else
            spaces = null;

        context.getDatabase().transaction(new Operation(IOperation.READ_ONLY | IOperation.DISABLE_NODES_UNLOAD) {
            @Override
            public void run(ITransaction transaction) {
                IDatabaseSchema schema = transaction.getCurrentSchema();
                if (schema == null)
                    return;

                operation.databaseName = schema.getConfiguration().getName();
                operation.snapshotPath = Files.createTempDirectory("ops");

                if (dumpContext.getQuery() != null && dumpContext.getQuery().contains("queries")) {
                    JsonObject queries = dumpContext.getQuery().get("queries");
                    for (Map.Entry<String, Object> entry : queries) {
                        File path = new File(operation.snapshotPath, entry.getKey());
                        path.mkdirs();

                        IDumpContext subDumpContext = new DumpContext(dumpContext.getFlags(), (JsonObject) entry.getValue());
                        dump(schema, spaces, subDumpContext, path);
                    }
                } else
                    dump(schema, spaces, dumpContext, operation.snapshotPath);
            }

            @Override
            public void onCommitted() {
                context.getCompartment().execute(operation);
            }

            @Override
            public void onRolledBack() {
                if (operation.snapshotPath != null)
                    Files.delete(operation.snapshotPath);

                operation.onFailed(null);
            }
        });
    }

    private void dump(IDatabaseSchema schema, Set<String> spaces, IDumpContext dumpContext,
                      File path) {
        for (IDomainSchema domainSchema : schema.getDomains()) {
            for (ISpaceSchema spaceSchema : domainSchema.getSpaces()) {
                if (spaces != null && !spaces.contains(spaceSchema.getConfiguration().getAlias()))
                    continue;

                ((ISpaceSchemaControl) spaceSchema).dump(path, dumpContext);
            }
        }
    }

    private static String createArchiveName() {
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SS");
        return format.format(new Date());
    }

    private static class SnapshotOperation implements Runnable {
        protected final ICompletionHandler completionHandler;
        protected String databaseName;
        protected File snapshotPath;

        public SnapshotOperation(ICompletionHandler completionHandler) {
            this.completionHandler = completionHandler;
        }

        @Override
        public void run() {
            Files.delete(snapshotPath);
        }

        protected void onSucceeded(Object result) {
            if (completionHandler != null)
                completionHandler.onSucceeded(result);
        }

        protected void onFailed(Exception exception) {
            if (completionHandler != null)
                completionHandler.onFailed(exception);
        }
    }

    private static class ZipOperation extends SnapshotOperation {
        private final File destination;

        public ZipOperation(File destination, ICompletionHandler completionHandler) {
            super(completionHandler);

            Assert.notNull(destination);

            this.destination = destination;
        }

        @Override
        public void run() {
            File temp = null;
            try {
                temp = File.createTempFile("ops", "tmp");
                Files.zip(snapshotPath, temp);
                Files.move(temp, destination);

                onSucceeded(null);
            } catch (Exception e) {
                if (temp != null)
                    Files.delete(temp);

                onFailed(e);
            }

            super.run();
        }
    }

    private class DumpOperation extends SnapshotOperation {
        private final String destination;
        private boolean compress;

        public DumpOperation(String destination, ICompletionHandler completionHandler, boolean compress) {
            super(completionHandler);

            Assert.notNull(destination);

            this.destination = destination;
            this.compress = compress;
        }

        @Override
        public void run() {
            if (compress) {
                File destinationFile = new File(destination,
                        context.getSchemaSpace().getCurrentSchema().getConfiguration().getName() + "-" + createArchiveName() + ".zip");
                File temp = null;
                try {
                    temp = File.createTempFile("ops", "tmp");
                    Files.zip(snapshotPath, temp);
                    Files.move(temp, destinationFile);

                    onSucceeded(null);
                } catch (Exception e) {
                    if (temp != null)
                        Files.delete(temp);

                    onFailed(e);
                }
            } else {
                Files.move(snapshotPath, new File(destination));
                onSucceeded(null);
            }

            super.run();
        }
    }

    private static class BackupOperation extends SnapshotOperation {
        private ArchiveStoreSchemaConfiguration archiveStore;

        public BackupOperation(ArchiveStoreSchemaConfiguration archiveStore, ICompletionHandler completionHandler) {
            super(completionHandler);

            Assert.notNull(archiveStore);

            this.archiveStore = archiveStore;
        }

        @Override
        public void run() {
            IArchiveStore archiveStore = this.archiveStore.createStore();

            File temp = null;
            try {
                temp = File.createTempFile("ops", "tmp");
                Files.zip(snapshotPath, temp);
                archiveStore.save(databaseName + "-" + createArchiveName(), temp.getPath());

                onSucceeded(null);
            } catch (Exception e) {
                onFailed(e);
            } finally {
                if (temp != null)
                    Files.delete(temp);
            }

            archiveStore.close();

            super.run();
        }
    }

    private interface IMessages {
        @DefaultMessage("Snapshot ''{0}'' has been started.")
        ILocalizedMessage snapshotStarted(String snapshotDirectoryPath);

        @DefaultMessage("Snapshot ''{0}'' has been completed.")
        ILocalizedMessage snapshotCompleted(String snapshotDirectoryPath);

        @DefaultMessage("Snapshot ''{0}'' has been failed.")
        ILocalizedMessage snapshotFailed(String snapshotDirectoryPath);

        @DefaultMessage("Dump ''{0}'' has been started.")
        ILocalizedMessage dumpStarted(String dumpDirectoryPath);

        @DefaultMessage("Dump ''{0}'' has been completed.")
        ILocalizedMessage dumpCompleted(String dumpDirectoryPath);

        @DefaultMessage("Dump ''{0}'' has been failed.")
        ILocalizedMessage dumpFailed(String dumpDirectoryPath);

        @DefaultMessage("Database backup ''{0}'' has been started.")
        ILocalizedMessage backupStarted(ArchiveStoreSchemaConfiguration archiveStore);

        @DefaultMessage("Database backup ''{0}'' has been completed.")
        ILocalizedMessage backupCompleted(ArchiveStoreSchemaConfiguration archiveStore);

        @DefaultMessage("Database backup ''{0}'' has been failed.")
        ILocalizedMessage backupFailed(ArchiveStoreSchemaConfiguration archiveStore);
    }
}
