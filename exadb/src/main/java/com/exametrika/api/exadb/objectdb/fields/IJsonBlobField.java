/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb.fields;

import com.exametrika.api.exadb.objectdb.schema.IJsonBlobFieldSchema;


/**
 * The {@link IJsonBlobField} represents an json blob field.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IJsonBlobField extends IStructuredBlobField<IJsonRecord> {
    @Override
    IJsonBlobFieldSchema getSchema();
}
