/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb;

import java.util.List;

import com.exametrika.api.exadb.fulltext.Sort;


/**
 * The {@link INodeSearchResult} represents a result of query search.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface INodeSearchResult {
    /**
     * Returns total number of hits for the query.
     *
     * @return total number of hits for the query
     */
    int getTotalCount();

    /**
     * Returns sort criteria.
     *
     * @return sort criteria or null if result is not sorted
     */
    Sort getSort();

    /**
     * Returns top hits for the query.
     *
     * @return top hits for the query
     */
    List<INodeSearchResultElement> getTopElements();
}
