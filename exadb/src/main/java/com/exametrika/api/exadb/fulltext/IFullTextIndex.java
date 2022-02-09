/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.fulltext;

import java.util.List;

import com.exametrika.api.exadb.index.IIndex;


/**
 * The {@link IFullTextIndex} represents an index.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IFullTextIndex extends IIndex {
    /**
     * Adds document to index.
     *
     * @param document document
     */
    void add(IDocument document);

    /**
     * Updates existing documents from index by term.
     *
     * @param field    term field
     * @param value    term value
     * @param document document
     */
    void update(String field, String value, IDocument document);

    /**
     * Removes documents from index by term.
     *
     * @param field term field
     * @param value term value
     */
    void remove(String field, String value);

    /**
     * Removes documents from index by queries.
     *
     * @param queries list of queries
     */
    void remove(IQuery... queries);

    /**
     * Removes all documents from index.
     */
    void removeAll();

    /**
     * Searches documents. Search result is guaranteed to be valid until next search operation.
     *
     * @param query query
     * @param count maximum number of top results selected
     * @return result
     */
    ISearchResult search(IQuery query, int count);

    /**
     * Searches documents.  Search result is guaranteed to be valid until next search operation.
     *
     * @param query  query
     * @param filter filter
     * @param count  maximum number of top results selected
     * @return result
     */
    ISearchResult search(IQuery query, IFilter filter, int count);

    /**
     * Searches documents. Search result is guaranteed to be valid until next search operation.
     *
     * @param query query
     * @param sort  sort criteria
     * @param count maximum number of top results selected
     * @return result
     */
    ISearchResult search(IQuery query, Sort sort, int count);

    /**
     * Searches documents. Search result is guaranteed to be valid until next search operation.
     *
     * @param query  query
     * @param filter filter
     * @param sort   sort criteria
     * @param count  maximum number of top results selected
     * @return result
     */
    ISearchResult search(IQuery query, IFilter filter, Sort sort, int count);

    /**
     * Begins snapshot.
     *
     * @return list of snapshot file names
     */
    List<String> beginSnapshot();

    /**
     * Ends snapshot.
     */
    void endSnapshot();

    /**
     * Deletes files of fulltext index.
     */
    void deleteFiles();
}
