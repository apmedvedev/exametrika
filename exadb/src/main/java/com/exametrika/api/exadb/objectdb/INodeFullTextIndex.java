/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb;

import com.exametrika.api.exadb.fulltext.IQuery;
import com.exametrika.api.exadb.fulltext.Sort;


/**
 * The {@link INodeFullTextIndex} represents a node fulltext index.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface INodeFullTextIndex {
    /**
     * Searches documents. Search result is guaranteed to be valid until next search operation.
     *
     * @param query query
     * @param count maximum number of top results selected
     * @return result
     */
    INodeSearchResult search(IQuery query, int count);

    /**
     * Searches documents. Search result is guaranteed to be valid until next search operation.
     *
     * @param query query
     * @param sort  sort criteria
     * @param count maximum number of top results selected
     * @return result
     */
    INodeSearchResult search(IQuery query, Sort sort, int count);

}
