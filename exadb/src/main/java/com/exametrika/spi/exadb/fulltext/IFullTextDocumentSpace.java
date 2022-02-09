/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.fulltext;

import com.exametrika.api.exadb.fulltext.IDocument;
import com.exametrika.common.io.IDataDeserialization;
import com.exametrika.common.io.IDataSerialization;


/**
 * The {@link IFullTextDocumentSpace} represents a space actually containing documents indexed by full text index.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IFullTextDocumentSpace {
    /**
     * Writes document identifier to given serialization.
     *
     * @param serialization serialization
     * @param document      document
     */
    void write(IDataSerialization serialization, IDocument document);

    /**
     * Reads document identifier from given deserialization and reindexes addition or removal of found document in full text index.
     *
     * @param deserialization deserialization
     */
    void readAndReindex(IDataDeserialization deserialization);
}
