/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.fulltext;


/**
 * The {@link IFullTextIndexControl} represents an full text index control interface.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IFullTextIndexControl {
    /**
     * Sets document space.
     *
     * @param space document space
     */
    void setDocumentSpace(IFullTextDocumentSpace space);

    /**
     * Reindexes those documents which have potentially stale full text index.
     */
    void reindex();
}
