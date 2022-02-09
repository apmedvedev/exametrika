/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.core;

import java.util.LinkedHashMap;
import java.util.Map;

import com.exametrika.api.exadb.core.config.DatabaseConfiguration;
import com.exametrika.api.exadb.core.schema.IDomainServiceSchema;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.spi.exadb.core.IDomainService;


/**
 * The {@link DomainServiceManager} is a manager of domain services.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class DomainServiceManager {
    private DatabaseConfiguration configuration;
    private final Map<String, IDomainService> domainServices = new LinkedHashMap<String, IDomainService>();
    private IDatabaseContext context;

    public DomainServiceManager(DatabaseConfiguration configuration) {
        Assert.notNull(configuration);

        this.configuration = configuration;
    }

    public void setConfiguration(DatabaseConfiguration configuration, boolean clearCache) {
        Assert.notNull(configuration);

        this.configuration = configuration;

        for (IDomainService service : domainServices.values())
            service.setConfiguration(configuration.getDomainServices().get(getQualifiedName(service.getSchema())), clearCache);
    }

    public void start(IDatabaseContext context) {
        Assert.notNull(context);

        this.context = context;
        this.configuration = context.getConfiguration();
    }

    public void stop() {
        for (IDomainService service : domainServices.values())
            service.stop();
    }

    public void onTimer(long currentTime) {
        for (IDomainService service : domainServices.values())
            service.onTimer(currentTime);
    }

    public <T> T findDomainService(String name) {
        Assert.notNull(name);

        return (T) domainServices.get(name);
    }

    public IDomainService addDomainService(IDomainServiceSchema schema) {
        Assert.notNull(schema);

        IDomainService service = schema.getConfiguration().createService();
        service.setSchema(schema);

        domainServices.put(getQualifiedName(schema), service);
        service.setConfiguration(configuration.getDomainServices().get(getQualifiedName(schema)), false);
        service.start(context);

        return service;
    }

    public void removeDomainService(IDomainServiceSchema schema) {
        Assert.notNull(schema);

        IDomainService service = domainServices.remove(getQualifiedName(schema));
        service.stop();
    }

    public void clearCaches() {
        for (IDomainService service : domainServices.values())
            service.clearCaches();
    }

    private String getQualifiedName(IDomainServiceSchema schema) {
        return schema.getParent().getConfiguration().getName() + "." + schema.getConfiguration().getName();
    }
}
