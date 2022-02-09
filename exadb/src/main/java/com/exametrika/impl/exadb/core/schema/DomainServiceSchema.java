/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.core.schema;

import java.util.Map;

import com.exametrika.api.exadb.core.schema.IDomainSchema;
import com.exametrika.api.exadb.core.schema.IDomainServiceSchema;
import com.exametrika.api.exadb.core.schema.ISchemaObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.exadb.core.DomainServiceManager;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.spi.exadb.core.IDomainService;
import com.exametrika.spi.exadb.core.config.schema.DomainServiceSchemaConfiguration;


/**
 * The {@link DomainServiceSchema} is a domain service schema.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class DomainServiceSchema extends SchemaObject implements IDomainServiceSchema {
    private DomainServiceManager domainServiceManager;
    private final DomainServiceSchemaConfiguration configuration;
    private IDomainSchema parent;
    private IDomainService service;
    protected final IDatabaseContext context;

    public DomainServiceSchema(IDatabaseContext context, DomainServiceSchemaConfiguration configuration) {
        super(TYPE);

        Assert.notNull(context);
        Assert.notNull(configuration);

        this.context = context;
        this.configuration = configuration;
    }

    public final void setDomainServiceManager(DomainServiceManager domainServiceManager) {
        Assert.notNull(domainServiceManager);
        Assert.isNull(this.domainServiceManager);

        this.domainServiceManager = domainServiceManager;
    }

    public void setParent(IDomainSchema parent, Map<String, ISchemaObject> schemaObjects) {
        Assert.notNull(parent);

        this.parent = parent;
        super.setParent(parent, schemaObjects);
    }

    @Override
    public DomainServiceSchemaConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public final IDomainSchema getParent() {
        return parent;
    }

    @Override
    public final <T> T getService() {
        return (T) service;
    }

    public final void onCreated() {
        service = domainServiceManager.addDomainService(this);
        service.onCreated();
    }

    public final void onDeleted() {
        service.onDeleted();
        service = null;
        domainServiceManager.removeDomainService(this);
    }

    public final void onOpened() {
        service = domainServiceManager.addDomainService(this);
        service.onOpened();
    }

    public final void onBeginChanged(IDomainServiceSchema old) {
        DomainServiceSchema oldSchema = (DomainServiceSchema) old;
        service = oldSchema.service;
        oldSchema.service = null;
    }

    public final void onEndChanged() {
        service.setSchema(this);
    }
}
