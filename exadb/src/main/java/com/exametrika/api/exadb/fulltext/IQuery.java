/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.fulltext;

import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;


/**
 * The {@link IQuery} represents an index query.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IQuery {
    IDocumentSchema getSchema();
}
