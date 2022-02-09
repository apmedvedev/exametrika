/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.core;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.exametrika.api.exadb.core.BatchOperation;
import com.exametrika.api.exadb.core.IBatchOperation;
import com.exametrika.api.exadb.core.IDatabase;
import com.exametrika.api.exadb.core.IDatabaseFactory;
import com.exametrika.api.exadb.core.IOperation;
import com.exametrika.api.exadb.core.ISchemaOperation;
import com.exametrika.api.exadb.core.ITransaction;
import com.exametrika.api.exadb.core.Operation;
import com.exametrika.api.exadb.core.config.CacheCategoryTypeConfiguration;
import com.exametrika.api.exadb.core.config.DatabaseConfiguration;
import com.exametrika.common.compartment.ICompartment;
import com.exametrika.common.compartment.ICompartmentFactory;
import com.exametrika.common.compartment.ICompartmentTimerProcessor;
import com.exametrika.common.compartment.impl.Compartment;
import com.exametrika.common.compartment.impl.CompartmentFactory;
import com.exametrika.common.log.IMarker;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.rawdb.IRawBatchContext;
import com.exametrika.common.rawdb.IRawDatabase;
import com.exametrika.common.rawdb.IRawOperation;
import com.exametrika.common.rawdb.IRawTransaction;
import com.exametrika.common.rawdb.config.RawDatabaseConfiguration;
import com.exametrika.common.rawdb.config.RawDatabaseConfiguration.Flag;
import com.exametrika.common.rawdb.config.RawPageCategoryTypeConfiguration;
import com.exametrika.common.rawdb.config.RawPageTypeConfiguration;
import com.exametrika.common.rawdb.impl.RawClearCacheException;
import com.exametrika.common.rawdb.impl.RawDatabase;
import com.exametrika.common.rawdb.impl.RawTransactionQueue;
import com.exametrika.common.resource.IResourceAllocator;
import com.exametrika.common.time.ITimeService;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Enums;
import com.exametrika.common.utils.Threads;
import com.exametrika.impl.exadb.core.ops.OperationManager;
import com.exametrika.impl.exadb.core.schema.SchemaSpace;
import com.exametrika.impl.exadb.core.tx.DbOperation;
import com.exametrika.impl.exadb.core.tx.Transaction;
import com.exametrika.impl.exadb.core.tx.TransactionManager;
import com.exametrika.spi.exadb.core.ICacheCategorizationStrategy;
import com.exametrika.spi.exadb.core.ICacheControl;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.spi.exadb.core.IDatabaseExtension;
import com.exametrika.spi.exadb.core.IExtensionSpace;
import com.exametrika.spi.exadb.core.ISchemaSpace;
import com.exametrika.spi.exadb.core.ITransactionProvider;


/**
 * The {@link Database} is a exa database.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class Database implements IDatabase, ICompartmentTimerProcessor {
    private final ICompartment compartment;
    private final RawDatabase database;
    private final IDatabaseFactory.Parameters parameters;
    private final TransactionManager transactionManager;
    private final OperationManager operationManager;
    private final SchemaSpace schemaSpace;
    private final IMarker marker;
    private final IRawOperation timerOperation;
    private final DatabaseContext context = new DatabaseContext();
    private final DatabaseExtensionManager extensionManager;
    private final DomainServiceManager domainServiceManager;
    private volatile ICacheCategorizationStrategy cacheCategorizationStrategy;
    private volatile DatabaseConfiguration configuration;
    private volatile DatabaseConfiguration preparedConfiguration;
    private volatile boolean opened;
    private volatile boolean started;
    private volatile boolean closed;

    public Database(IDatabaseFactory.Parameters parameters, DatabaseConfiguration configuration) {
        Assert.notNull(configuration);
        Assert.notNull(parameters);

        this.configuration = configuration;
        this.preparedConfiguration = configuration;
        this.parameters = parameters;
        this.timerOperation = new RawTimerOperation();
        marker = Loggers.getMarker(configuration.getName());

        boolean compartmentOwner = false;
        if (parameters.compartment == null) {
            ICompartmentFactory.Parameters compartmentParameters = new ICompartmentFactory.Parameters();
            compartmentParameters.name = configuration.getName();
            compartmentParameters.queue = new RawTransactionQueue();
            compartmentParameters.dispatchPeriod = configuration.getTimerPeriod();
            compartment = new CompartmentFactory().createCompartment(compartmentParameters);
            compartmentOwner = true;
        } else {
            compartment = parameters.compartment;
            compartmentOwner = parameters.compartmentOwner;
        }

        cacheCategorizationStrategy = configuration.getCacheCategorizationStrategy().createStrategy();

        RawDatabaseConfiguration rawDbConfiguration = createRawDbConfiguration(configuration);

        database = new RawDatabase(rawDbConfiguration, compartment, compartmentOwner, new BatchContext(),
                parameters.resourceAllocator);
        compartment.addTimerProcessor(this);

        domainServiceManager = new DomainServiceManager(configuration);
        extensionManager = new DatabaseExtensionManager(database, domainServiceManager, parameters);

        transactionManager = new TransactionManager(database);

        schemaSpace = new SchemaSpace(this, transactionManager, extensionManager.getExtensionsDigest(), extensionManager,
                domainServiceManager);
        transactionManager.setSchemaSpace(schemaSpace);
        extensionManager.setSchemaSpace(schemaSpace);

        operationManager = new OperationManager(context);
    }

    public IDatabaseFactory.Parameters getParameters() {
        return parameters;
    }

    public IMarker getMarker() {
        return marker;
    }

    public IDatabaseContext getContext() {
        return context;
    }

    @Override
    public boolean isOpened() {
        return opened;
    }

    @Override
    public boolean isClosed() {
        return closed;
    }

    @Override
    public DatabaseConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public void setConfiguration(DatabaseConfiguration configuration) {
        Assert.checkState(opened && !closed);
        Assert.notNull(configuration);

        synchronized (this) {
            if (preparedConfiguration.equals(configuration))
                return;

            Assert.isTrue(preparedConfiguration.isCompatible(configuration));
            preparedConfiguration = configuration;
        }

        transactionSync(new Operation(IOperation.FLUSH) {
            @Override
            public void run(ITransaction transaction) {
                DatabaseConfiguration preparedConfiguration = Database.this.preparedConfiguration;
                DatabaseConfiguration configuration = Database.this.configuration;
                if (preparedConfiguration == configuration)
                    return;

                boolean clearCache = false;
                if (!preparedConfiguration.getCacheCategorizationStrategy().equals(configuration.getCacheCategorizationStrategy())) {
                    cacheCategorizationStrategy = preparedConfiguration.getCacheCategorizationStrategy().createStrategy();
                    clearCache = true;
                }
                RawDatabaseConfiguration rawDbConfiguration = createRawDbConfiguration(preparedConfiguration);
                if (database.setConfigurationInTransaction(rawDbConfiguration, clearCache))
                    clearCache = true;

                Database.this.configuration = preparedConfiguration;

                extensionManager.setConfiguration(preparedConfiguration, clearCache);
                domainServiceManager.setConfiguration(preparedConfiguration, clearCache);

                if (clearCache)
                    throw new RawClearCacheException();
            }
        });
    }

    @Override
    public <T> T findParameter(String name) {
        Assert.notNull(name);

        return (T) parameters.parameters.get(name);
    }

    @Override
    public OperationManager getOperations() {
        return operationManager;
    }

    @Override
    public void open() {
        synchronized (this) {
            if (opened)
                return;

            opened = true;
        }

        try {
            extensionManager.start(context);
            domainServiceManager.start(context);
            database.start();
            operationManager.start();
        } finally {
            started = true;
        }
    }

    @Override
    public void close() {
        synchronized (this) {
            if (!opened || closed)
                return;

            closed = true;
        }

        transactionManager.transactionSync(new Operation(IOperation.FLUSH) {
            @Override
            public void run(ITransaction transaction) {
                context.getCacheControl().flush(true);
            }
        });

        operationManager.stop();
        database.stop();
        domainServiceManager.stop();
        extensionManager.stop();
    }

    @Override
    public void flush() {
        Assert.checkState(opened && !closed);

        transactionSync(new Operation(IOperation.FLUSH) {
            @Override
            public void run(ITransaction transaction) {
                context.getCacheControl().flush(true);
            }
        });
    }

    @Override
    public void clearCaches() {
        Assert.checkState(opened && !closed);

        transactionSync(new Operation(IOperation.FLUSH) {
            @Override
            public void run(ITransaction transaction) {
                throw new RawClearCacheException();
            }
        });
    }

    @Override
    public void transaction(IOperation operation) {
        Assert.notNull(operation);
        if (opened && !closed) {
            checkStarted();
            transactionManager.transaction(operation);
        }
    }

    @Override
    public void transaction(List<IOperation> operations) {
        Assert.notNull(operations);
        if (opened && !closed) {
            checkStarted();
            transactionManager.transaction(operations);
        }
    }

    @Override
    public void transactionSync(IOperation operation) {
        Assert.notNull(operation);
        Assert.checkState(opened && !closed);
        checkStarted();
        transactionManager.transactionSync(operation);
    }

    @Override
    public void transaction(IBatchOperation operation) {
        Assert.notNull(operation);
        if (opened && !closed) {
            checkStarted();
            transactionManager.transaction(operation);
        }
    }

    @Override
    public void transactionSync(IBatchOperation operation) {
        Assert.checkState(opened && !closed);
        transactionManager.transactionSync(operation);
    }

    @Override
    public void transaction(ISchemaOperation operation) {
        Assert.notNull(operation);
        if (opened && !closed) {
            checkStarted();
            schemaSpace.transaction(operation);
        }
    }

    @Override
    public void transactionSync(ISchemaOperation operation) {
        Assert.checkState(opened && !closed);
        checkStarted();
        schemaSpace.transactionSync(operation);
    }

    @Override
    public void onTimer(long currentTime) {
        if (started)
            database.transaction(timerOperation);
    }

    @Override
    public <T> T findExtension(String name) {
        Assert.checkState(opened && !closed);
        return extensionManager.findPublicExtension(name);
    }

    public void printStatistics() {
        database.printStatistics();

        IOperation operation = new Operation(true) {
            @Override
            public void run(ITransaction transaction) {
                System.out.println(extensionManager.printStatistics());
            }
        };

        if (opened && !closed && !((Compartment) compartment).isMainThread())
            transactionSync(operation);
        else
            operation.run(null);
    }

    @Override
    protected void finalize() {
        close();
    }

    private RawDatabaseConfiguration createRawDbConfiguration(DatabaseConfiguration configuration) {
        Map<String, RawPageCategoryTypeConfiguration> pageCategories = new LinkedHashMap<String, RawPageCategoryTypeConfiguration>();
        for (CacheCategoryTypeConfiguration category : configuration.getCacheCategoryTypes().values())
            pageCategories.put(category.getName(), createPageCategoryType(category));

        RawPageCategoryTypeConfiguration defaultPageCategory = createPageCategoryType(configuration.getDefaultCacheCategoryType());
        RawPageTypeConfiguration normalPageType = new RawPageTypeConfiguration("normal", Constants.NORMAL_PAGE_SIZE,
                defaultPageCategory, pageCategories);
        RawPageTypeConfiguration smallPageType = new RawPageTypeConfiguration("small", Constants.SMALL_PAGE_SIZE,
                defaultPageCategory, pageCategories);
        RawPageTypeConfiguration smallMediumPageType = new RawPageTypeConfiguration("smallMedium", Constants.SMALL_MEDIUM_PAGE_SIZE,
                defaultPageCategory, pageCategories);
        RawPageTypeConfiguration mediumPageType = new RawPageTypeConfiguration("medium", Constants.MEDIUM_PAGE_SIZE,
                defaultPageCategory, pageCategories);
        RawPageTypeConfiguration largeMediumPageType = new RawPageTypeConfiguration("largeMedium", Constants.LARGE_MEDIUM_PAGE_SIZE,
                defaultPageCategory, pageCategories);
        RawPageTypeConfiguration largePageType = new RawPageTypeConfiguration("large", Constants.LARGE_PAGE_SIZE,
                defaultPageCategory, pageCategories);
        RawPageTypeConfiguration extraLargePageType = new RawPageTypeConfiguration("extraLarge", Constants.EXTRA_LARGE_PAGE_SIZE,
                defaultPageCategory, pageCategories);

        return new RawDatabaseConfiguration(configuration.getName(), configuration.getPaths(), configuration.getFlushPeriod(),
                configuration.getMaxFlushSize(), configuration.getTimerPeriod(), Long.MAX_VALUE, Enums.of(Flag.NATIVE_MEMORY),
                configuration.getBatchRunPeriod(), configuration.getBatchIdlePeriod(), Arrays.asList(
                normalPageType, smallPageType, smallMediumPageType, mediumPageType, largeMediumPageType,
                largePageType, extraLargePageType), configuration.getResourceAllocator());
    }

    private RawPageCategoryTypeConfiguration createPageCategoryType(CacheCategoryTypeConfiguration type) {
        return new RawPageCategoryTypeConfiguration(type.getName(), type.getInitialCacheSize(), type.getMinCachePercentage(),
                type.getMaxIdlePeriod());
    }

    private void checkStarted() {
        if (started)
            return;

        while (opened && !closed && !started)
            Threads.sleep(100);
    }

    private class RawTimerOperation implements IRawOperation {
        private IRawOperation operation;
        private long lastFlushTime;

        @Override
        public int getOptions() {
            return 0;
        }

        @Override
        public int getSize() {
            return 1;
        }

        @Override
        public List<String> getBatchLockPredicates() {
            if (operation != null)
                return operation.getBatchLockPredicates();
            else
                return null;
        }

        @Override
        public boolean isCompleted() {
            return true;
        }

        @Override
        public void onBeforeStarted(IRawTransaction transaction) {
            if (operation == null)
                operation = new DbOperation(transactionManager, new TimerOperation(), schemaSpace);

            operation.onBeforeStarted(transaction);
        }

        @Override
        public void run(IRawTransaction transaction) {
            operation.run(transaction);
        }

        @Override
        public void validate() {
            operation.validate();
        }

        @Override
        public void onBeforeCommitted() {
            operation.onBeforeCommitted();

            long currentTime = context.getTimeService().getCurrentTime();
            if (lastFlushTime == 0 || currentTime > lastFlushTime + configuration.getFlushPeriod()) {
                context.getCacheControl().flush(false);
                lastFlushTime = currentTime;
            }

            context.getCacheControl().clear(false);
        }

        @Override
        public void onCommitted() {
            operation.onCommitted();
        }

        @Override
        public boolean onBeforeRolledBack() {
            return operation.onBeforeRolledBack();
        }

        @Override
        public void onRolledBack(boolean clearCache) {
            operation.onRolledBack(clearCache);
        }
    }

    private class TimerOperation extends Operation {
        private IOperation timerOperation;

        public TimerOperation() {
            super(IOperation.DISABLE_NODES_UNLOAD);

            timerOperation = parameters.timerOperation;
        }

        @Override
        public void run(ITransaction transaction) {
            if (!schemaSpace.isOpened())
                return;

            if (timerOperation != null)
                timerOperation.run(transaction);

            schemaSpace.onTimer(((Transaction) transaction).getTransaction());
            extensionManager.onTimer(database.getCurrentTime());
            domainServiceManager.onTimer(database.getCurrentTime());
        }

        @Override
        public void onCommitted() {
            if (timerOperation != null)
                timerOperation.onCommitted();
        }

        @Override
        public void onRolledBack() {
            if (timerOperation != null)
                timerOperation.onRolledBack();
        }
    }

    private class BatchContext implements IRawBatchContext {
        @Override
        public UUID getExtensionId() {
            return BatchOperation.EXTENTION_ID;
        }

        @Override
        public <T> T getContext() {
            return (T) context;
        }

        @Override
        public void open() {
            schemaSpace.open(database.getConfiguration().getPaths().get(0));
        }
    }

    private class DatabaseContext implements IDatabaseContext {
        @Override
        public DatabaseConfiguration getConfiguration() {
            return configuration;
        }

        @Override
        public IDatabase getDatabase() {
            return Database.this;
        }

        @Override
        public IRawDatabase getRawDatabase() {
            return database;
        }

        @Override
        public ICompartment getCompartment() {
            return compartment;
        }

        @Override
        public ISchemaSpace getSchemaSpace() {
            return schemaSpace;
        }

        @Override
        public ITransactionProvider getTransactionProvider() {
            return transactionManager;
        }

        @Override
        public ICacheControl getCacheControl() {
            return extensionManager;
        }

        @Override
        public IExtensionSpace getExtensionSpace() {
            return extensionManager;
        }

        @Override
        public ITimeService getTimeService() {
            return database;
        }

        @Override
        public ICacheCategorizationStrategy getCacheCategorizationStrategy() {
            return cacheCategorizationStrategy;
        }

        @Override
        public IResourceAllocator getResourceAllocator() {
            return database.getResourceAllocator();
        }

        @Override
        public <T extends IDatabaseExtension> T findExtension(String name) {
            return extensionManager.findExtension(name);
        }

        @Override
        public <T> T findPublicExtension(String name) {
            return extensionManager.findPublicExtension(name);
        }

        @Override
        public <T> T findTransactionExtension(String name) {
            return extensionManager.findTransactionExtension(name);
        }
    }
}
