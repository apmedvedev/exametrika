/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.index;

import com.exametrika.api.exadb.fulltext.IDocument;
import com.exametrika.api.exadb.fulltext.ISearchResultElement;
import com.exametrika.api.exadb.objectdb.INodeSearchResultElement;
import com.exametrika.common.utils.Assert;


/**
 * The {@link NodeSearchResultElement} implements {@link INodeSearchResultElement}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class NodeSearchResultElement implements INodeSearchResultElement {
    private final NodeFullTextIndex index;
    private final ISearchResultElement element;

    public NodeSearchResultElement(NodeFullTextIndex index, ISearchResultElement element) {
        Assert.notNull(index);
        Assert.notNull(element);

        this.index = index;
        this.element = element;
    }

    @Override
    public <T> T get() {
        IDocument document = element.getDocument();
        return index.getValue(document);
    }
}
