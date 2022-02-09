/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.core;

import com.exametrika.api.exadb.core.schema.IDomainServiceSchema;
import com.exametrika.spi.exadb.core.config.DomainServiceConfiguration;


/**
 * The {@link DomainService} is an abstract domain service implementation.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class DomainService implements IDomainService {
    protected IDomainServiceSchema schema;
    protected IDatabaseContext context;

    @Override
    public IDomainServiceSchema getSchema() {
        return schema;
    }

    @Override
    public void setSchema(IDomainServiceSchema schema) {
        this.schema = schema;
    }

    @Override
    public DomainServiceConfiguration getConfiguration() {
        return null;
    }

    @Override
    public void setConfiguration(DomainServiceConfiguration configuration, boolean clearCache) {
    }

    @Override
    public void start(IDatabaseContext context) {
        this.context = context;
    }

    @Override
    public void stop() {
    }

    @Override
    public void onCreated() {
    }

    @Override
    public void onOpened() {
    }

    @Override
    public void onDeleted() {
    }

    @Override
    public void onTimer(long currentTime) {
    }

    @Override
    public void clearCaches() {
    }
}
