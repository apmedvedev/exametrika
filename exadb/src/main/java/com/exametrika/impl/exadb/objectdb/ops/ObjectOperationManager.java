/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.ops;

import com.exametrika.api.exadb.core.IOperation;
import com.exametrika.api.exadb.core.ITransaction;
import com.exametrika.api.exadb.core.Operation;
import com.exametrika.api.exadb.core.schema.IDatabaseSchema;
import com.exametrika.api.exadb.core.schema.IDomainSchema;
import com.exametrika.api.exadb.core.schema.ISpaceSchema;
import com.exametrika.api.exadb.objectdb.IObjectOperationManager;
import com.exametrika.api.exadb.objectdb.schema.IObjectSpaceSchema;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.rawdb.IRawTransaction;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.CompletionHandler;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.common.utils.ICompletionHandler;
import com.exametrika.common.utils.SimpleList;
import com.exametrika.common.utils.SyncCompletionHandler;
import com.exametrika.impl.exadb.objectdb.NodeSpace;
import com.exametrika.impl.exadb.objectdb.ObjectSpace;
import com.exametrika.impl.exadb.objectdb.cache.ObjectNodeCacheManager;
import com.exametrika.impl.exadb.objectdb.schema.ObjectSpaceSchema;
import com.exametrika.spi.exadb.core.IDatabaseContext;

import java.util.HashSet;
import java.util.Set;


/**
 * The {@link ObjectOperationManager} is a object operation manager.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ObjectOperationManager implements IObjectOperationManager {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(ObjectOperationManager.class);
    private final ObjectNodeCacheManager nodeCacheManager;
    private final IDatabaseContext context;
    private final SimpleList<SyncCompletionHandler> handlers = new SimpleList<SyncCompletionHandler>();
    private boolean started;
    private boolean stopped;

    public ObjectOperationManager(ObjectNodeCacheManager nodeCacheManager, IDatabaseContext context) {
        Assert.notNull(nodeCacheManager);
        Assert.notNull(context);

        this.nodeCacheManager = nodeCacheManager;
        this.context = context;
    }

    @Override
    public void compact(ICompletionHandler completionHandler) {
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
            logger.log(LogLevel.INFO, messages.compactionStarted());

        doCompact(new CompletionHandler() {
            @Override
            public boolean isCanceled() {
                return usedCompletionHandler.isCanceled();
            }

            @Override
            public void onSucceeded(Object value) {
                if (logger.isLogEnabled(LogLevel.INFO))
                    logger.log(LogLevel.INFO, messages.compactionCompleted());

                usedCompletionHandler.onSucceeded(value);
            }

            @Override
            public void onFailed(Throwable error) {
                if (logger.isLogEnabled(LogLevel.ERROR))
                    logger.log(LogLevel.ERROR, messages.compactionFailed(), error);

                usedCompletionHandler.onFailed(error);
            }
        });

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

    private void doCompact(final ICompletionHandler completionHandler) {
        context.getDatabase().transaction(new Operation(IOperation.FLUSH) {
            private Exception exception;

            @Override
            public void run(ITransaction transaction) {
                try {
                    Set<NodeSpace> unloadedSpaces = new HashSet<NodeSpace>();
                    compact(context.getTransactionProvider().getRawTransaction(), unloadedSpaces);

                    if (!unloadedSpaces.isEmpty())
                        nodeCacheManager.unloadNodesOfDeletedSpaces(unloadedSpaces);
                } catch (Exception e) {
                    exception = e;

                    Exceptions.wrapAndThrow(e);
                }
            }

            @Override
            public void onCommitted() {
                if (completionHandler != null)
                    completionHandler.onSucceeded(null);
            }

            @Override
            public void onRolledBack() {
                context.getCacheControl().clear(true);

                if (completionHandler != null)
                    completionHandler.onFailed(exception);
            }
        });
    }

    private void compact(IRawTransaction transaction, Set<NodeSpace> unloadedSpaces) {
        IDatabaseSchema schema = context.getSchemaSpace().getCurrentSchema();
        if (schema == null)
            return;

        for (IDomainSchema domainSchema : schema.getDomains()) {
            for (ISpaceSchema spaceSchema : domainSchema.getSpaces()) {
                if (spaceSchema instanceof IObjectSpaceSchema) {
                    ObjectSpaceSchema objectSpaceSchema = (ObjectSpaceSchema) spaceSchema;
                    ObjectSpace space = objectSpaceSchema.compact();
                    if (space != null)
                        unloadedSpaces.add(space);
                }
            }
        }
    }

    private interface IMessages {
        @DefaultMessage("Database compaction has been started.")
        ILocalizedMessage compactionStarted();

        @DefaultMessage("Database compaction has been completed.")
        ILocalizedMessage compactionCompleted();

        @DefaultMessage("Database compaction has been failed.")
        ILocalizedMessage compactionFailed();
    }
}
