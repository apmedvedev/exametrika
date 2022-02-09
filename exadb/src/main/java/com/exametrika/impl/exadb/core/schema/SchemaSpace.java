/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.core.schema;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exametrika.api.exadb.core.IDataMigrator;
import com.exametrika.api.exadb.core.IOperation;
import com.exametrika.api.exadb.core.ISchemaOperation;
import com.exametrika.api.exadb.core.ISchemaTransaction;
import com.exametrika.api.exadb.core.ITransaction;
import com.exametrika.api.exadb.core.Operation;
import com.exametrika.api.exadb.core.SchemaOperation;
import com.exametrika.api.exadb.core.config.schema.DatabaseSchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.DomainSchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.ModularDatabaseSchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.ModuleSchemaConfiguration;
import com.exametrika.api.exadb.core.schema.IDatabaseSchema;
import com.exametrika.api.exadb.core.schema.IDomainSchema;
import com.exametrika.api.exadb.core.schema.IDomainServiceSchema;
import com.exametrika.api.exadb.core.schema.ISpaceSchema;
import com.exametrika.common.io.impl.ByteInputStream;
import com.exametrika.common.io.impl.ByteOutputStream;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.rawdb.IRawTransaction;
import com.exametrika.common.rawdb.RawDatabaseException;
import com.exametrika.common.rawdb.impl.RawPageDeserialization;
import com.exametrika.common.rawdb.impl.RawPageSerialization;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.InvalidArgumentException;
import com.exametrika.common.utils.Serializers;
import com.exametrika.common.utils.Times;
import com.exametrika.common.utils.Version;
import com.exametrika.impl.exadb.core.Constants;
import com.exametrika.impl.exadb.core.Database;
import com.exametrika.impl.exadb.core.DatabaseExtensionManager;
import com.exametrika.impl.exadb.core.DomainServiceManager;
import com.exametrika.impl.exadb.core.Spaces;
import com.exametrika.impl.exadb.core.config.schema.ModuleSchemaLoader;
import com.exametrika.impl.exadb.core.tx.Transaction;
import com.exametrika.impl.exadb.core.tx.TransactionManager;
import com.exametrika.spi.exadb.core.IInitialSchemaProvider;
import com.exametrika.spi.exadb.core.ISchemaSpace;
import com.exametrika.spi.exadb.core.ISpaceSchemaControl;
import com.exametrika.spi.exadb.core.config.schema.DomainServiceSchemaConfiguration;
import com.exametrika.spi.exadb.core.config.schema.SpaceSchemaConfiguration;

/**
 * The {@link SchemaSpace} is a space of schemas.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class SchemaSpace implements ISchemaSpace {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final short MAGIC = 0x1717;
    private static final int HEADER_SIZE = 44;
    public static final short SCHEMA_MAGIC = 0x170A;
    public static final short SPACE_SCHEMA_MAGIC = 0x17FF;
    private final Database database;
    private final TransactionManager transactionManager;
    private final ByteArray extensionsDigest;
    private final DatabaseExtensionManager extensionManager;
    private final DomainServiceManager domainServiceManager;
    private volatile SchemaCache schemaCache = new SchemaCache(Collections.<DatabaseSchema>emptyList());
    private boolean opened;
    private IDatabaseSchema newSchema;

    public SchemaSpace(Database database, TransactionManager transactionManager, ByteArray extensionsDigest,
                       DatabaseExtensionManager extensionManager, DomainServiceManager domainServiceManager) {
        Assert.notNull(database);
        Assert.notNull(transactionManager);
        Assert.notNull(extensionsDigest);
        Assert.notNull(extensionManager);
        Assert.notNull(domainServiceManager);

        this.database = database;
        this.transactionManager = transactionManager;
        this.extensionsDigest = extensionsDigest;
        this.extensionManager = extensionManager;
        this.domainServiceManager = domainServiceManager;
    }

    public TransactionManager getTransactionManager() {
        return transactionManager;
    }

    public SchemaCache getSchemaCache() {
        return schemaCache;
    }

    public DatabaseExtensionManager getExtensionManager() {
        return extensionManager;
    }

    public DomainServiceManager getDomainServiceManager() {
        return domainServiceManager;
    }

    public boolean isOpened() {
        return opened;
    }

    @Override
    public Database getDatabase() {
        return database;
    }

    @Override
    public IDatabaseSchema getCurrentSchema() {
        return schemaCache.getCurrentSchema();
    }

    @Override
    public IDatabaseSchema getNewSchema() {
        return newSchema;
    }

    @Override
    public IDatabaseSchema findSchema(long time) {
        return schemaCache.findSchema(time);
    }

    @Override
    public List<IDatabaseSchema> getSchemas() {
        return (List) schemaCache.getSchemas();
    }

    @Override
    public long allocate(IRawTransaction transaction, int size) {
        Assert.notNull(transaction);
        Assert.isTrue(size + 4 < Constants.PAGE_SIZE);

        Header header = readHeader(transaction);

        header.changeCount++;

        long fileOffset = header.nextFileOffset;
        if (Constants.pageOffsetByFileOffset(fileOffset) + size + 4 > Constants.PAGE_SIZE)
            fileOffset = Constants.fileOffset(Constants.pageIndexByFileOffset(fileOffset) + 1, 0);
        header.nextFileOffset = fileOffset + size + 4;
        writeHeader(transaction, header);

        RawPageSerialization serialization = new RawPageSerialization(transaction, 0,
                Constants.pageIndexByFileOffset(fileOffset), Constants.pageOffsetByFileOffset(fileOffset));
        serialization.writeShort(SPACE_SCHEMA_MAGIC);
        serialization.writeShort((short) size);

        return fileOffset + 4;
    }

    @Override
    public int allocateFile(IRawTransaction transaction) {
        Header header = readHeader(transaction);

        header.changeCount++;
        int nextFileIndex = header.nextFileIndex;
        header.nextFileIndex++;

        writeHeader(transaction, header);

        return nextFileIndex;
    }

    public void open(String path) {
        File schemaFile = new File(path, Spaces.SCHEMA_SPACE_FILE_NAME);
        if (!schemaFile.exists() || schemaFile.length() == 0) {
            transactionManager.transactionSync(new Operation(IOperation.FLUSH) {
                @Override
                public void run(ITransaction transaction) {
                    createSchemaSpace(((Transaction) transaction).getTransaction());
                }

                @Override
                public void onCommitted() {
                    opened = true;
                }

                @Override
                public void onRolledBack() {
                }
            });
        } else {
            transactionManager.transactionSync(new Operation(IOperation.FLUSH) {
                @Override
                public void run(ITransaction transaction) {
                    IRawTransaction rawTransaction = ((Transaction) transaction).getTransaction();
                    openSchemaSpace(rawTransaction);

                    database.getContext().getExtensionSpace().open();

                    openDomainServices();
                }

                @Override
                public void onCommitted() {
                }

                @Override
                public void onRolledBack() {
                }
            });

            transactionSync(new SchemaOperation() {
                @Override
                public void run(ISchemaTransaction transaction) {
                    ModularDatabaseSchemaConfiguration configuration = transaction.getConfiguration();

                    Map<String, IDataMigrator> dataMigrators = extensionManager.getDataMigrators();
                    for (ModuleSchemaConfiguration module : extensionManager.getRequiredModules())
                        transaction.addModule(module, dataMigrators);

                    for (ModuleSchemaConfiguration module : extensionManager.getOptionalModules()) {
                        if (configuration.findModule(module.getName()) != null)
                            transaction.addModule(module, dataMigrators);
                    }

                    opened = true;
                }
            });
        }
    }

    public void transaction(ISchemaOperation operation) {
        Assert.notNull(operation);

        transactionManager.transaction(new DbSchemaOperation(operation));
    }

    public void transactionSync(ISchemaOperation operation) {
        Assert.notNull(operation);

        transactionManager.transactionSync(new DbSchemaOperation(operation));
    }

    public void onTimer(IRawTransaction transaction) {
        DatabaseSchema schema = schemaCache.getCurrentSchema();
        if (schema == null)
            return;

        long currentTime = Times.getCurrentTime();

        for (IDomainSchema domainSchema : schema.getDomains()) {
            for (ISpaceSchema spaceSchema : domainSchema.getSpaces())
                ((ISpaceSchemaControl) spaceSchema).onTimer(currentTime);
        }
    }

    private SchemaCache readSchemas(IRawTransaction transaction, int schemasCount) {
        RawPageDeserialization deserialization = new RawPageDeserialization(transaction, 0, 0, HEADER_SIZE);

        List<DatabaseSchema> schemas = new ArrayList<DatabaseSchema>(schemasCount);
        for (int i = 0; i < schemasCount; i++) {
            while (true) {
                short magic = deserialization.readShort();
                if (magic == SCHEMA_MAGIC)
                    break;

                if (magic == 0) {
                    deserialization.setPosition(Constants.pageIndexByFileOffset(deserialization.getFileOffset()) + 1, 0);
                    magic = deserialization.readShort();
                    if (magic != SPACE_SCHEMA_MAGIC)
                        throw new RawDatabaseException(messages.invalidFormat());
                }

                if (magic == SPACE_SCHEMA_MAGIC) {
                    int size = deserialization.readShort();
                    long fileOffset = deserialization.getFileOffset() + size;
                    deserialization.setPosition(Constants.pageIndexByFileOffset(fileOffset),
                            Constants.pageOffsetByFileOffset(fileOffset));
                } else
                    throw new RawDatabaseException(messages.invalidFormat());
            }
            ByteArray data = deserialization.readByteArray();
            ByteInputStream stream = new ByteInputStream(data.getBuffer(), data.getOffset(), data.getLength());
            ModularDatabaseSchemaConfiguration modularDatabaseSchemaConfiguration = Serializers.deserialize(stream,
                    getClass().getClassLoader());
            DatabaseSchemaConfiguration databaseSchemaConfiguration = modularDatabaseSchemaConfiguration.getCombinedSchema();

            long creationTime = deserialization.readLong();
            int version = deserialization.readInt();
            Assert.checkState(schemas.size() + 1 == version);

            int domainsCount = databaseSchemaConfiguration.getDomains().size();
            List<IDomainSchema> domains = new ArrayList<IDomainSchema>(domainsCount);
            for (DomainSchemaConfiguration domainSchemaConfiguration : databaseSchemaConfiguration.getDomains()) {
                long domainCreationTime = deserialization.readLong();
                int domainVersion = deserialization.readInt();

                int spacesCount = domainSchemaConfiguration.getSpaces().size();
                List<ISpaceSchema> spaces = new ArrayList<ISpaceSchema>(spacesCount);
                for (SpaceSchemaConfiguration spaceSchemaConfiguration : domainSchemaConfiguration.getSpaces()) {
                    int spaceVersion = deserialization.readInt();
                    if (spaceVersion < version) {
                        ISpaceSchema spaceSchema = schemas.get(spaceVersion - 1).findDomain(
                                domainSchemaConfiguration.getName()).findSpace(spaceSchemaConfiguration.getName());
                        Assert.checkState(spaceSchema != null);
                        Assert.checkState(spaceSchema.getVersion() == spaceVersion);
                        spaces.add(spaceSchema);
                        continue;
                    }

                    ISpaceSchema spaceSchema = spaceSchemaConfiguration.createSchema(database.getContext(),
                            spaceVersion);
                    ((ISpaceSchemaControl) spaceSchema).read(deserialization);

                    spaces.add(spaceSchema);
                }

                List<IDomainServiceSchema> domainServices = new ArrayList<IDomainServiceSchema>(
                        domainSchemaConfiguration.getDomainServices().size());
                for (DomainServiceSchemaConfiguration domainServiceSchemaConfiguration : domainSchemaConfiguration.getDomainServices()) {
                    DomainServiceSchema domainServiceSchema = (DomainServiceSchema) domainServiceSchemaConfiguration.createSchema(database.getContext());
                    domainServiceSchema.setDomainServiceManager(domainServiceManager);
                    domainServices.add(domainServiceSchema);
                }

                domains.add(new DomainSchema(domainSchemaConfiguration, domainCreationTime, domainVersion, spaces,
                        domainServices));
            }

            schemas.add(new DatabaseSchema(modularDatabaseSchemaConfiguration, creationTime, version, domains,
                    schemas.size() == schemasCount - 1));
        }

        SchemaCache cache = new SchemaCache(schemas);

        if (cache.getCurrentSchema() != null)
            extensionManager.setSchema(cache.getCurrentSchema().getConfiguration());

        return cache;
    }

    private long writeSchema(IRawTransaction transaction, DatabaseSchema schema, long nextFileOffset) {
        RawPageSerialization serialization = new RawPageSerialization(transaction, 0,
                Constants.pageIndexByFileOffset(nextFileOffset), Constants.pageOffsetByFileOffset(nextFileOffset));

        serialization.writeShort(SCHEMA_MAGIC);

        ByteOutputStream stream = new ByteOutputStream();
        Serializers.serialize(stream, schema.getModularConfiguration());
        serialization.writeByteArray(new ByteArray(stream.getBuffer(), 0, stream.getLength()));

        serialization.writeLong(schema.getCreationTime());
        serialization.writeInt(schema.getVersion());

        for (IDomainSchema domain : schema.getDomains()) {
            serialization.writeLong(domain.getCreationTime());
            serialization.writeInt(domain.getVersion());

            for (int k = 0; k < domain.getSpaces().size(); k++) {
                ISpaceSchema spaceSchema = domain.getSpaces().get(k);
                serialization.writeInt(spaceSchema.getVersion());
                if (spaceSchema.getVersion() < schema.getVersion())
                    continue;

                ((ISpaceSchemaControl) spaceSchema).write(serialization);
            }
        }

        return serialization.getFileOffset();
    }

    private Header readHeader(IRawTransaction transaction) {
        RawPageDeserialization deserialization = new RawPageDeserialization(transaction, 0, 0, 0);

        Header header = new Header();
        header.magic = deserialization.readShort();
        header.version = deserialization.readShort();
        header.changeCount = deserialization.readLong();
        header.nextFileIndex = deserialization.readInt();
        header.nextFileOffset = deserialization.readLong();
        header.schemasCount = deserialization.readInt();
        header.extensionsDigest = deserialization.read(16);

        if (header.magic != MAGIC)
            throw new RawDatabaseException(messages.invalidFormat());
        if (header.version != Constants.VERSION)
            throw new RawDatabaseException(messages.unsupportedVersion(header.version, Constants.VERSION));
        if (!header.extensionsDigest.equals(extensionsDigest))
            throw new RawDatabaseException(messages.incompatibleExtensions());

        return header;
    }

    private void writeHeader(IRawTransaction transaction, Header header) {
        RawPageSerialization serialization = new RawPageSerialization(transaction, 0, 0, 0);

        serialization.writeShort(header.magic);
        serialization.writeShort(header.version);
        serialization.writeLong(header.changeCount);
        serialization.writeInt(header.nextFileIndex);
        serialization.writeLong(header.nextFileOffset);
        serialization.writeInt(header.schemasCount);
        serialization.write(header.extensionsDigest);
    }

    private void createSchemaSpace(IRawTransaction transaction) {
        Header header = new Header();
        header.magic = MAGIC;
        header.version = Constants.VERSION;
        header.changeCount = 1;
        header.nextFileIndex = Spaces.RESERVED_FILE_COUNT;
        header.nextFileOffset = HEADER_SIZE;
        header.schemasCount = 0;
        header.extensionsDigest = extensionsDigest;
        writeHeader(transaction, header);

        Set<ModuleSchemaConfiguration> initialModules = extensionManager.getRequiredModules();
        String initialSchemaPath = database.getConfiguration().getInitialSchemaPath();
        ModularDatabaseSchemaConfiguration initialSchema = null;
        if (initialSchemaPath != null) {
            ModuleSchemaLoader loader = new ModuleSchemaLoader();
            initialSchema = loader.loadInitialSchema(initialSchemaPath);
        }

        IInitialSchemaProvider initialSchemaProvider = database.getParameters().initialSchemaProvider;
        if (initialSchema != null || initialSchemaProvider != null || !initialModules.isEmpty()) {
            ModularDatabaseSchemaConfiguration configuration = initialSchema;
            if (configuration == null && initialSchemaProvider != null)
                configuration = initialSchemaProvider.getInitialSchema();
            if (!initialModules.isEmpty()) {
                if (configuration != null) {
                    initialModules.addAll(configuration.getModules());
                    configuration = new ModularDatabaseSchemaConfiguration(configuration.getName(),
                            configuration.getAlias(), configuration.getDescription(), initialModules,
                            configuration.getTimeZone(), configuration.getLocale());
                } else
                    configuration = new ModularDatabaseSchemaConfiguration("db", initialModules);
            }

            DatabaseSchema schema = addSchema(configuration, transaction, null, true);
            schemaCache = schemaCache.addSchema(schema);
        } else
            database.getContext().getExtensionSpace().create();
    }

    private void openSchemaSpace(IRawTransaction transaction) {
        Header header = readHeader(transaction);
        schemaCache = readSchemas(transaction, header.schemasCount);
    }

    private void openDomainServices() {
        DatabaseSchema schema = schemaCache.getCurrentSchema();
        if (schema == null)
            return;

        for (IDomainSchema domainSchema : schema.getDomains()) {
            for (IDomainServiceSchema domainServiceSchema : domainSchema.getDomainServices())
                ((DomainServiceSchema) domainServiceSchema).onOpened();
        }
    }

    private DatabaseSchema addSchema(ModularDatabaseSchemaConfiguration newModularSchemaConfiguration,
                                     IRawTransaction transaction, Map<String, IDataMigrator> dataMigrators, boolean create) {
        DatabaseSchemaConfiguration newSchemaConfiguration = newModularSchemaConfiguration.getCombinedSchema();
        DatabaseSchemaConfiguration oldSchemaConfiguration = null;
        DatabaseSchema oldSchema = null;
        if (schemaCache.getCurrentSchema() != null) {
            oldSchema = schemaCache.getCurrentSchema();
            oldSchemaConfiguration = oldSchema.getConfiguration();

            Assert.isTrue(newSchemaConfiguration.getExtensions().equals(oldSchemaConfiguration.getExtensions()));
        }

        List<ISpaceSchema> newSpaces = new ArrayList<ISpaceSchema>();
        List<ISpaceSchema> removedSpaces = new ArrayList<ISpaceSchema>();
        List<ChangedSpaceInfo> changedSpaces = new ArrayList<ChangedSpaceInfo>();

        List<IDomainServiceSchema> newDomainServices = new ArrayList<IDomainServiceSchema>();
        List<IDomainServiceSchema> removedDomainServices = new ArrayList<IDomainServiceSchema>();
        List<IDomainServiceSchema> changedDomainServices = new ArrayList<IDomainServiceSchema>();

        List<IDomainSchema> domains = new ArrayList<IDomainSchema>();

        long currentTime = Times.getCurrentTime();
        int version = (oldSchema != null) ? (oldSchema.getVersion() + 1) : 1;

        if (oldSchemaConfiguration == null) {
            for (DomainSchemaConfiguration domain : newSchemaConfiguration.getDomains()) {
                List<ISpaceSchema> spaces = new ArrayList<ISpaceSchema>();
                for (SpaceSchemaConfiguration space : domain.getSpaces()) {
                    ISpaceSchema spaceSchema = space.createSchema(database.getContext(), version);
                    spaces.add(spaceSchema);
                }

                List<IDomainServiceSchema> domainServices = new ArrayList<IDomainServiceSchema>();
                for (DomainServiceSchemaConfiguration service : domain.getDomainServices()) {
                    DomainServiceSchema domainServiceSchema = (DomainServiceSchema) service.createSchema(database.getContext());
                    domainServiceSchema.setDomainServiceManager(domainServiceManager);
                    domainServices.add(domainServiceSchema);
                }

                DomainSchema domainSchema = new DomainSchema(domain, currentTime, 1, spaces, domainServices);
                newSpaces.addAll(domainSchema.getSpaces());
                newDomainServices.addAll(domainSchema.getDomainServices());
                domains.add(domainSchema);
            }
        } else {
            Assert.isTrue(oldSchemaConfiguration.getName().equals(newSchemaConfiguration.getName()));

            for (DomainSchemaConfiguration newDomain : newSchemaConfiguration.getDomains()) {
                DomainSchemaConfiguration oldDomain = oldSchemaConfiguration.findDomain(newDomain.getName());
                if (oldDomain != null && oldDomain.equals(newDomain))
                    domains.add(oldSchema.findDomain(newDomain.getName()));
                else if (oldDomain == null) {
                    List<ISpaceSchema> spaces = new ArrayList<ISpaceSchema>();
                    for (SpaceSchemaConfiguration space : newDomain.getSpaces()) {
                        ISpaceSchema spaceSchema = space.createSchema(database.getContext(), version);
                        spaces.add(spaceSchema);
                    }

                    List<IDomainServiceSchema> domainServices = new ArrayList<IDomainServiceSchema>();
                    for (DomainServiceSchemaConfiguration service : newDomain.getDomainServices()) {
                        DomainServiceSchema domainServiceSchema = (DomainServiceSchema) service.createSchema(database.getContext());
                        domainServiceSchema.setDomainServiceManager(domainServiceManager);
                        domainServices.add(domainServiceSchema);
                    }

                    DomainSchema domainSchema = new DomainSchema(newDomain, currentTime, 1, spaces, domainServices);

                    newSpaces.addAll(domainSchema.getSpaces());
                    newDomainServices.addAll(domainSchema.getDomainServices());
                    domains.add(domainSchema);
                } else {
                    IDomainSchema oldDomainSchema = oldSchema.findDomain(newDomain.getName());
                    List<ISpaceSchema> spaces = new ArrayList<ISpaceSchema>();

                    for (SpaceSchemaConfiguration newSpace : newDomain.getSpaces()) {
                        SpaceSchemaConfiguration oldSpace = oldDomain.findSpace(newSpace.getName());
                        if (oldSpace != null && oldSpace.equals(newSpace))
                            spaces.add(oldDomainSchema.findSpace(newSpace.getName()));
                        else if (oldSpace == null) {
                            ISpaceSchema spaceSchema = newSpace.createSchema(database.getContext(), version);
                            spaces.add(spaceSchema);
                            newSpaces.add(spaceSchema);
                        } else {
                            Assert.isTrue(oldSpace.getClass() == newSpace.getClass());

                            if (oldSpace.equalsStructured(newSpace))
                                newSpace.orderNodes(oldSpace);

                            ISpaceSchema spaceSchema = newSpace.createSchema(database.getContext(), version);
                            spaces.add(spaceSchema);

                            ISpaceSchema oldSpaceSchema = oldDomainSchema.findSpace(newSpace.getName());

                            Assert.isTrue(((ISpaceSchemaControl) oldSpaceSchema).isCompatible(spaceSchema,
                                    dataMigrators.get(newDomain.getName() + "." + oldSpace.getName())));

                            changedSpaces.add(new ChangedSpaceInfo(oldSpaceSchema, spaceSchema,
                                    dataMigrators.get(newDomain.getName() + "." + spaceSchema.getConfiguration().getName())));
                        }
                    }

                    for (SpaceSchemaConfiguration oldSpace : oldDomain.getSpaces()) {
                        SpaceSchemaConfiguration newSpace = newDomain.findSpace(oldSpace.getName());
                        if (newSpace == null) {
                            ISpaceSchema oldSpaceSchema = oldDomainSchema.findSpace(oldSpace.getName());
                            removedSpaces.add(oldSpaceSchema);
                        }
                    }

                    List<IDomainServiceSchema> domainServices = new ArrayList<IDomainServiceSchema>();

                    for (DomainServiceSchemaConfiguration newService : newDomain.getDomainServices()) {
                        DomainServiceSchemaConfiguration oldService = oldDomain.findDomainService(newService.getName());
                        if (oldService != null && oldService.equals(newService))
                            domainServices.add(oldDomainSchema.findDomainService(newService.getName()));
                        else if (oldService == null) {
                            DomainServiceSchema domainServiceSchema = (DomainServiceSchema) newService.createSchema(database.getContext());
                            domainServiceSchema.setDomainServiceManager(domainServiceManager);

                            domainServices.add(domainServiceSchema);
                            newDomainServices.add(domainServiceSchema);
                        } else {
                            Assert.isTrue(oldService.isCompatible(newService));
                            DomainServiceSchema newServiceSchema = (DomainServiceSchema) newService.createSchema(database.getContext());
                            newServiceSchema.setDomainServiceManager(domainServiceManager);
                            domainServices.add(newServiceSchema);

                            IDomainServiceSchema oldServiceSchema = oldDomainSchema.findDomainService(newService.getName());
                            newServiceSchema.onBeginChanged(oldServiceSchema);
                            changedDomainServices.add(newServiceSchema);
                        }
                    }

                    for (DomainServiceSchemaConfiguration oldService : oldDomain.getDomainServices()) {
                        DomainServiceSchemaConfiguration newService = newDomain.findDomainService(oldService.getName());
                        if (newService == null) {
                            IDomainServiceSchema oldServiceSchema = oldDomainSchema.findDomainService(oldService.getName());
                            removedDomainServices.add(oldServiceSchema);
                        }
                    }

                    domains.add(new DomainSchema(newDomain, currentTime, oldDomainSchema.getVersion() + 1, spaces,
                            domainServices));
                }
            }

            for (DomainSchemaConfiguration oldDomain : oldSchemaConfiguration.getDomains()) {
                DomainSchemaConfiguration newDomain = newSchemaConfiguration.findDomain(oldDomain.getName());
                if (newDomain == null) {
                    DomainSchema oldDomainSchema = (DomainSchema) oldSchema.findDomain(oldDomain.getName());

                    for (ISpaceSchema oldSpaceSchema : oldDomainSchema.getSpaces())
                        removedSpaces.add(oldSpaceSchema);
                    for (IDomainServiceSchema oldServiceSchema : oldDomainSchema.getDomainServices())
                        removedDomainServices.add(oldServiceSchema);
                }
            }
        }

        DatabaseSchema schema = new DatabaseSchema(newModularSchemaConfiguration, currentTime, version, domains, true);

        newSchema = schema;

        Header header = readHeader(transaction);
        header.changeCount++;
        header.nextFileOffset = writeSchema(transaction, schema, header.nextFileOffset);
        header.schemasCount++;
        writeHeader(transaction, header);

        extensionManager.setSchema(schema.getConfiguration());

        if (create)
            database.getContext().getExtensionSpace().create();

        for (ISpaceSchema space : newSpaces)
            ((ISpaceSchemaControl) space).onCreated();

        for (ISpaceSchema space : removedSpaces)
            ((ISpaceSchemaControl) space).onDeleted();

        for (ChangedSpaceInfo info : changedSpaces)
            ((ISpaceSchemaControl) info.newSchema).onModified(info.oldSchema, info.dataMigrator);
        for (ChangedSpaceInfo info : changedSpaces)
            ((ISpaceSchemaControl) info.newSchema).onAfterModified(info.oldSchema);

        for (IDomainServiceSchema domainService : removedDomainServices)
            ((DomainServiceSchema) domainService).onDeleted();

        for (IDomainServiceSchema domainService : newDomainServices)
            ((DomainServiceSchema) domainService).onCreated();

        for (IDomainServiceSchema domainService : changedDomainServices)
            ((DomainServiceSchema) domainService).onEndChanged();

        return schema;
    }

    private class DbSchemaOperation implements IOperation {
        private final ISchemaOperation operation;
        private DatabaseSchema schema;
        private boolean changed;

        public DbSchemaOperation(ISchemaOperation operation) {
            Assert.notNull(operation);

            this.operation = operation;
        }

        @Override
        public int getOptions() {
            return FLUSH;
        }

        @Override
        public int getSize() {
            return operation.getSize();
        }

        @Override
        public List<String> getBatchLockPredicates() {
            return null;
        }

        @Override
        public void run(ITransaction transaction) {
            ModularDatabaseSchemaConfiguration schemaConfiguration = null;
            if (schemaCache.getCurrentSchema() != null)
                schemaConfiguration = schemaCache.getCurrentSchema().getModularConfiguration();
            else
                schemaConfiguration = new ModularDatabaseSchemaConfiguration("",
                        Collections.<ModuleSchemaConfiguration>emptySet());

            SchemaTransaction schemaTransaction = new SchemaTransaction(transaction, schemaConfiguration);
            operation.run(schemaTransaction);
            if (schemaTransaction.getConfiguration() != schemaConfiguration) {
                changed = true;
                schema = addSchema(schemaTransaction.getConfiguration(), ((Transaction) transaction).getTransaction(),
                        schemaTransaction.getDataMigrators(), false);
            }
        }

        @Override
        public void onCommitted() {
            newSchema = null;

            if (changed) {
                schemaCache = schemaCache.addSchema(schema);

                database.getContext().getCacheControl().clear(true);
            }
        }

        @Override
        public void onRolledBack() {
            newSchema = null;

            if (changed)
                database.getContext().getCacheControl().clear(true);
        }
    }

    private class SchemaTransaction implements ISchemaTransaction {
        private final ITransaction transaction;
        private final String name;
        private String alias;
        private String description;
        private String timeZone;
        private String locale;
        private final Map<String, ModuleSchemaConfiguration> modules;
        private final Map<String, Map<String, IDataMigrator>> dataMigrators = new HashMap<String, Map<String, IDataMigrator>>();
        private ModularDatabaseSchemaConfiguration configuration;

        public SchemaTransaction(ITransaction transaction, ModularDatabaseSchemaConfiguration configuration) {
            Assert.notNull(transaction);
            Assert.notNull(configuration);

            this.transaction = transaction;
            this.name = configuration.getName();
            this.alias = configuration.getAlias();
            this.description = configuration.getDescription();
            this.timeZone = configuration.getTimeZone();
            this.locale = configuration.getLocale();
            Map<String, ModuleSchemaConfiguration> modules = new HashMap<String, ModuleSchemaConfiguration>();
            for (ModuleSchemaConfiguration domain : configuration.getModules())
                modules.put(domain.getName(), domain);
            this.modules = modules;
            this.configuration = configuration;
        }

        public Map<String, IDataMigrator> getDataMigrators() {
            Map<String, IDataMigrator> dataMigrators = new HashMap<String, IDataMigrator>();
            for (Map<String, IDataMigrator> map : this.dataMigrators.values())
                dataMigrators.putAll(map);

            return dataMigrators;
        }

        @Override
        public ModularDatabaseSchemaConfiguration getConfiguration() {
            if (configuration == null)
                configuration = new ModularDatabaseSchemaConfiguration(name, alias, description,
                        new HashSet<ModuleSchemaConfiguration>(modules.values()), timeZone, locale);

            return configuration;
        }

        @Override
        public void setDatabaseAlias(String alias) {
            Assert.notNull(alias);

            this.alias = alias;
            configuration = null;
        }

        @Override
        public void setDatabaseDescription(String description) {
            this.description = description;
            configuration = null;
        }

        @Override
        public void addModule(ModuleSchemaConfiguration schema, Map<String, ? extends IDataMigrator> dataMigrators) {
            Assert.notNull(schema);

            ModuleSchemaConfiguration oldSchema = modules.get(schema.getName());
            if (oldSchema != null) {
                if (oldSchema.getVersion().equals(schema.getVersion())) {
                    Assert.isTrue(oldSchema.equals(schema));
                    return;
                }

                if (!schema.getVersion().isCompatible(oldSchema.getVersion()))
                    throw new InvalidArgumentException(messages.incompatibleVersions(schema.getName(),
                            schema.getVersion(), oldSchema.getVersion()));
            }

            modules.put(schema.getName(), schema);
            configuration = null;

            if (dataMigrators != null)
                this.dataMigrators.put(schema.getName(), (Map) dataMigrators);
        }

        @Override
        public void addModules(String schemaPath, Map<String, ? extends IDataMigrator> dataMigrators) {
            ModuleSchemaLoader loader = new ModuleSchemaLoader(Collections.<String, String>emptyMap(), timeZone,
                    locale);
            Set<ModuleSchemaConfiguration> modules = loader.loadModules(schemaPath);

            for (ModuleSchemaConfiguration module : modules)
                addModule(module, dataMigrators);
        }

        @Override
        public void addExtensionModules(Set<String> names, Map<String, ? extends IDataMigrator> dataMigrators) {
            Set<ModuleSchemaConfiguration> modules = extensionManager.getOptionalModules();
            for (ModuleSchemaConfiguration module : modules) {
                if (names.contains(module.getName()))
                    addModule(module, dataMigrators);
            }
        }

        @Override
        public void removeModule(String name) {
            Assert.notNull(name);

            if (modules.containsKey(name)) {
                modules.remove(name);
                dataMigrators.remove(name);
                configuration = null;
            }
        }

        @Override
        public void removeAllModules() {
            if (!modules.isEmpty()) {
                modules.clear();
                dataMigrators.clear();
                configuration = null;
            }
        }

        @Override
        public <T> T findExtension(String name) {
            return transaction.findExtension(name);
        }
    }

    private static class Header {
        private short magic;
        private short version;
        private long changeCount;
        private int nextFileIndex;
        private long nextFileOffset;
        private int schemasCount;
        private ByteArray extensionsDigest;
    }

    private static class ChangedSpaceInfo {
        private final ISpaceSchema oldSchema;
        private final ISpaceSchema newSchema;
        private final IDataMigrator dataMigrator;

        public ChangedSpaceInfo(ISpaceSchema oldSchema, ISpaceSchema newSchema, IDataMigrator dataMigrator) {
            this.oldSchema = oldSchema;
            this.newSchema = newSchema;
            this.dataMigrator = dataMigrator;
        }
    }

    private interface IMessages {
        @DefaultMessage("Invalid format of schema file.")
        ILocalizedMessage invalidFormat();

        @DefaultMessage("New version ''{1}'' of module ''{0}'' is not compatible with old version ''{2}''.")
        ILocalizedMessage incompatibleVersions(String moduleName, Version version, Version oldVersion);

        @DefaultMessage("Unsupported version ''{0}'' of schema file, expected version - ''{1}''.")
        ILocalizedMessage unsupportedVersion(int fileVersion, int expectedVersion);

        @DefaultMessage("Database extensions are not compatible with extensions of database runtime.")
        ILocalizedMessage incompatibleExtensions();
    }
}
