/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.fulltext;

import com.exametrika.api.exadb.fulltext.IDocument;
import com.exametrika.api.exadb.fulltext.ISearchResultElement;
import com.exametrika.common.utils.Assert;


/**
 * The {@link SearchResultElement} is a search result element.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class SearchResultElement implements ISearchResultElement {
    private final FullTextIndex index;
    private final int docId;
    private IDocument document;
    private final int modCount;

    public SearchResultElement(FullTextIndex index, int docId, int modCount) {
        Assert.notNull(index);

        this.index = index;
        this.docId = docId;
        this.modCount = modCount;
    }

    @Override
    public IDocument getDocument() {
        if (document == null)
            document = index.getDocument(docId, modCount);

        return document;
    }
}
