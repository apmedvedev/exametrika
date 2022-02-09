/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.fields;

import com.exametrika.api.exadb.objectdb.fields.IBlobFieldInitializer;
import com.exametrika.spi.exadb.objectdb.INodeObject;


/**
 * The {@link BlobFieldInitializer} is an initializer of blob field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class BlobFieldInitializer implements IBlobFieldInitializer {
    private INodeObject store;

    public INodeObject getStore() {
        return store;
    }

    @Override
    public <T> void setStore(T store) {
        this.store = (INodeObject) store;
    }
}
