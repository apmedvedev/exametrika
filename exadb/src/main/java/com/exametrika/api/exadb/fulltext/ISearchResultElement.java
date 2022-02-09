/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.fulltext;


/**
 * The {@link ISearchResultElement} represents a single element of result of query search.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface ISearchResultElement {
    /**
     * Returns document with all stored fields.
     *
     * @return document with all stored fields
     */
    IDocument getDocument();
}
