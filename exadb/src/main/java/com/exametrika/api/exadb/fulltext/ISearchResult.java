/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.fulltext;

import java.util.List;


/**
 * The {@link ISearchResult} represents a result of query search.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface ISearchResult {
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
    List<ISearchResultElement> getTopElements();
}
