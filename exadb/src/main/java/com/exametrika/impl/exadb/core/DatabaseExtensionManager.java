/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.core;

import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import com.exametrika.api.exadb.core.IDataMigrator;
import com.exametrika.api.exadb.core.IDatabaseFactory;
import com.exametrika.api.exadb.core.config.DatabaseConfiguration;
import com.exametrika.api.exadb.core.config.schema.DatabaseSchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.ModuleSchemaConfiguration;
import com.exametrika.api.exadb.core.schema.IDatabaseSchema;
import com.exametrika.api.exadb.core.schema.IDomainSchema;
import com.exametrika.api.exadb.core.schema.ISpaceSchema;
import com.exametrika.common.rawdb.RawDatabaseException;
import com.exametrika.common.rawdb.impl.RawDatabase;
import com.exametrika.common.services.Services;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Strings;
import com.exametrika.impl.exadb.core.schema.SchemaSpace;
import com.exametrika.spi.exadb.core.ICacheControl;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.spi.exadb.core.IDatabaseExtension;
import com.exametrika.spi.exadb.core.IExtensionSpace;
import com.exametrika.spi.exadb.core.IPublicExtensionRegistrar;
import com.exametrika.spi.exadb.core.ISpaceSchemaControl;


/**
 * The {@link DatabaseExtensionManager} is a manager of database extensions.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class DatabaseExtensionManager implements ICacheControl, IExtensionSpace {
    private final RawDatabase database;
    private final DomainServiceManager domainServiceManager;
    private final Map<String, IDatabaseExtension> extensions;
    private final IDatabaseFactory.Parameters parameters;
    private volatile Map<String, Object> publicExtensions;
    private Map<String, Object> transactionExtensions;
    private final ByteArray extensionsDigest;
    private SchemaSpace schemaSpace;

    public DatabaseExtensionManager(RawDatabase database, DomainServiceManager domainServiceManager, IDatabaseFactory.Parameters parameters) {
        Assert.notNull(database);
        Assert.notNull(domainServiceManager);
        Assert.notNull(parameters);

        Map<String, IDatabaseExtension> extensions = new LinkedHashMap<String, IDatabaseExtension>();
        List<IDatabaseExtension> list = Services.loadProviders(IDatabaseExtension.class);

        for (IDatabaseExtension extension : list)

            Assert.isTrue(extensions.put(extension.getConfiguration().getName(), extension) == null);

        this.database = database;
        this.domainServiceManager = domainServiceManager;
        this.extensions = extensions;
        this.parameters = parameters;
        this.extensionsDigest = computeExtensionsDigest();
    }

    public void setSchemaSpace(SchemaSpace schemaSpace) {
        Assert.notNull(schemaSpace);
        Assert.checkState(this.schemaSpace == null);

        this.schemaSpace = schemaSpace;
    }

    public ByteArray getExtensionsDigest() {
        return extensionsDigest;
    }

    public Set<ModuleSchemaConfiguration> getRequiredModules() {
        if (Boolean.TRUE.equals(parameters.parameters.get("disableModules")))
            return java.util.Collections.emptySet();

        Set<ModuleSchemaConfiguration> modules = new LinkedHashSet<ModuleSchemaConfiguration>();
        for (IDatabaseExtension extension : extensions.values()) {
            Set<ModuleSchemaConfiguration> extensionModules = extension.getRequiredModules();
            modules.addAll(extensionModules);
        }

        return modules;
    }

    public Set<ModuleSchemaConfiguration> getOptionalModules() {
        if (Boolean.TRUE.equals(parameters.parameters.get("disableModules")))
            return java.util.Collections.emptySet();

        Set<ModuleSchemaConfiguration> modules = new LinkedHashSet<ModuleSchemaConfiguration>();
        for (IDatabaseExtension extension : extensions.values()) {
            Set<ModuleSchemaConfiguration> extensionModules = extension.getOptionalModules();
            modules.addAll(extensionModules);
        }

        return modules;
    }

    public Map<String, IDataMigrator> getDataMigrators() {
        if (Boolean.TRUE.equals(parameters.parameters.get("disableModules")))
            return null;

        Map<String, IDataMigrator> map = null;
        for (IDatabaseExtension extension : extensions.values()) {
            Map<String, IDataMigrator> dataMigrators = extension.getDataMigrators();
            if (dataMigrators != null) {
                if (map == null)
                    map = new HashMap<String, IDataMigrator>();
                map.putAll(dataMigrators);
            }
        }

        return map;
    }

    public void setSchema(DatabaseSchemaConfiguration schema) {
        Assert.notNull(schema);

        for (IDatabaseExtension extension : extensions.values()) {
            if (extension.getSchema() != null)
                extension.setSchema(schema.findExtension(extension.getSchema().getName()));
        }
    }

    public void setConfiguration(DatabaseConfiguration configuration, boolean clearCache) {
        Assert.notNull(configuration);

        for (IDatabaseExtension extension : extensions.values())
            extension.setConfiguration(configuration.getExtensions().get(extension.getConfiguration().getName()), clearCache);
    }

    public void start(IDatabaseContext context) {
        setConfiguration(context.getConfiguration(), false);

        for (IDatabaseExtension extension : extensions.values())
            extension.start(context);

        PublicExtensionRegistrar registrar = new PublicExtensionRegistrar();
        for (IDatabaseExtension extension : extensions.values())
            extension.registerPublicExtensions(registrar);

        publicExtensions = registrar.publicExtensions;
        transactionExtensions = registrar.transactionExtensions;
    }

    public void stop() {
        clear(true);
        publicExtensions = null;
        transactionExtensions = null;

        for (IDatabaseExtension extension : extensions.values())
            extension.stop();
    }

    public void onTimer(long currentTime) {
        for (IDatabaseExtension extension : extensions.values())
            extension.onTimer(currentTime);
    }

    public <T extends IDatabaseExtension> T findExtension(String name) {
        Assert.notNull(name);

        return (T) extensions.get(name);
    }

    public <T> T findPublicExtension(String name) {
        Assert.notNull(name);

        return (T) publicExtensions.get(name);
    }

    public <T> T findTransactionExtension(String name) {
        Assert.notNull(name);

        return (T) transactionExtensions.get(name);
    }

    @Override
    public void validate() {
        for (IDatabaseExtension extension : extensions.values()) {
            ICacheControl cacheControl = extension.getCacheControl();
            if (cacheControl != null)
                cacheControl.validate();
        }
    }

    @Override
    public void onTransactionStarted() {
        for (IDatabaseSchema schema : schemaSpace.getSchemaCache().getSchemas()) {
            for (IDomainSchema domainSchema : schema.getDomains()) {
                for (ISpaceSchema spaceSchema : domainSchema.getSpaces())
                    ((ISpaceSchemaControl) spaceSchema).onTransactionStarted();
            }
        }

        for (IDatabaseExtension extension : extensions.values()) {
            ICacheControl cacheControl = extension.getCacheControl();
            if (cacheControl != null)
                cacheControl.onTransactionStarted();
        }
    }

    @Override
    public void onTransactionCommitted() {
        for (IDatabaseSchema schema : schemaSpace.getSchemaCache().getSchemas()) {
            for (IDomainSchema domainSchema : schema.getDomains()) {
                for (ISpaceSchema spaceSchema : domainSchema.getSpaces())
                    ((ISpaceSchemaControl) spaceSchema).onTransactionCommitted();
            }
        }

        for (IDatabaseExtension extension : extensions.values()) {
            ICacheControl cacheControl = extension.getCacheControl();
            if (cacheControl != null)
                cacheControl.onTransactionCommitted();
        }
    }

    @Override
    public boolean onBeforeTransactionRolledBack() {
        boolean res = false;
        for (IDatabaseSchema schema : schemaSpace.getSchemaCache().getSchemas()) {
            for (IDomainSchema domainSchema : schema.getDomains()) {
                for (ISpaceSchema spaceSchema : domainSchema.getSpaces())
                    res = ((ISpaceSchemaControl) spaceSchema).onBeforeTransactionRolledBack() || res;
            }
        }

        for (IDatabaseExtension extension : extensions.values()) {
            ICacheControl cacheControl = extension.getCacheControl();
            if (cacheControl != null)
                res = cacheControl.onBeforeTransactionRolledBack() || res;
        }

        return res;
    }

    @Override
    public void onTransactionRolledBack() {
        for (IDatabaseSchema schema : schemaSpace.getSchemaCache().getSchemas()) {
            for (IDomainSchema domainSchema : schema.getDomains()) {
                for (ISpaceSchema spaceSchema : domainSchema.getSpaces())
                    ((ISpaceSchemaControl) spaceSchema).onTransactionRolledBack();
            }
        }

        for (IDatabaseExtension extension : extensions.values()) {
            ICacheControl cacheControl = extension.getCacheControl();
            if (cacheControl != null)
                cacheControl.onTransactionRolledBack();
        }
    }

    @Override
    public void flush(boolean full) {
        for (IDatabaseExtension extension : extensions.values()) {
            ICacheControl cacheControl = extension.getCacheControl();
            if (cacheControl != null)
                cacheControl.flush(full);
        }
    }

    @Override
    public void clear(boolean full) {
        if (full) {
            database.clearBatchCache();
            domainServiceManager.clearCaches();

            for (IDatabaseSchema schema : schemaSpace.getSchemaCache().getSchemas()) {
                for (IDomainSchema domainSchema : schema.getDomains()) {
                    for (ISpaceSchema spaceSchema : domainSchema.getSpaces())
                        ((ISpaceSchemaControl) spaceSchema).clearCaches();
                }
            }
        }

        for (IDatabaseExtension extension : extensions.values()) {
            ICacheControl cacheControl = extension.getCacheControl();
            if (cacheControl != null)
                cacheControl.clear(full);
        }
    }

    @Override
    public void unloadExcessive() {
        for (IDatabaseExtension extension : extensions.values()) {
            ICacheControl cacheControl = extension.getCacheControl();
            if (cacheControl != null)
                cacheControl.unloadExcessive();
        }
    }

    @Override
    public void setCachingEnabled(boolean value) {
        for (IDatabaseExtension extension : extensions.values()) {
            ICacheControl cacheControl = extension.getCacheControl();
            if (cacheControl != null)
                cacheControl.setCachingEnabled(value);
        }
    }

    @Override
    public void setMaxCacheSize(String category, long value) {
        for (IDatabaseExtension extension : extensions.values()) {
            ICacheControl cacheControl = extension.getCacheControl();
            if (cacheControl != null)
                cacheControl.setMaxCacheSize(category, value);
        }
    }

    @Override
    public List<String> getFiles() {
        List<String> files = new ArrayList<String>();
        for (IDatabaseExtension extension : extensions.values()) {
            IExtensionSpace extensionSpace = extension.getExtensionSpace();
            if (extensionSpace != null)
                files.addAll(extensionSpace.getFiles());
        }
        return files;
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public void create() {
        Map<Integer, IExtensionSpace> map = new TreeMap<Integer, IExtensionSpace>();
        for (IDatabaseExtension extension : extensions.values()) {
            IExtensionSpace extensionSpace = extension.getExtensionSpace();
            if (extensionSpace != null)
                map.put(extensionSpace.getPriority(), extensionSpace);
        }

        for (IExtensionSpace space : map.values())
            space.create();
    }

    @Override
    public void open() {
        Map<Integer, IExtensionSpace> map = new TreeMap<Integer, IExtensionSpace>();
        for (IDatabaseExtension extension : extensions.values()) {
            IExtensionSpace extensionSpace = extension.getExtensionSpace();
            if (extensionSpace != null)
                map.put(extensionSpace.getPriority(), extensionSpace);
        }

        for (IExtensionSpace space : map.values())
            space.open();
    }

    public String printStatistics() {
        StringBuilder builder = new StringBuilder();

        boolean first = true;
        for (IDatabaseExtension extension : extensions.values()) {
            if (first)
                first = false;
            else
                builder.append('\n');

            builder.append(extension.printStatistics());
        }

        return Strings.indent(builder.toString(), 4);
    }

    private ByteArray computeExtensionsDigest() {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            for (IDatabaseExtension extension : extensions.values()) {
                byte[] buffer = extension.getConfiguration().getName().getBytes("UTF-8");
                digest.update(buffer);
            }

            return new ByteArray(digest.digest());
        } catch (Exception e) {
            throw new RawDatabaseException(e);
        }
    }

    private static class PublicExtensionRegistrar implements IPublicExtensionRegistrar {
        private final Map<String, Object> publicExtensions = new HashMap<String, Object>();
        private final Map<String, Object> transactionExtensions = new HashMap<String, Object>();

        @Override
        public void register(String name, Object extension, boolean requireTransaction) {
            Assert.notNull(name);
            Assert.notNull(extension);

            if (requireTransaction)
                Assert.checkState(transactionExtensions.put(name, extension) == null);
            else
                Assert.checkState(publicExtensions.put(name, extension) == null);
        }
    }
}
