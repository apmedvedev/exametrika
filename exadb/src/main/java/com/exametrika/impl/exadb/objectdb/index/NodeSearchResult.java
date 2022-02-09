/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.index;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.api.exadb.fulltext.ISearchResult;
import com.exametrika.api.exadb.fulltext.ISearchResultElement;
import com.exametrika.api.exadb.fulltext.Sort;
import com.exametrika.api.exadb.objectdb.INodeSearchResult;
import com.exametrika.api.exadb.objectdb.INodeSearchResultElement;
import com.exametrika.common.utils.Assert;


/**
 * The {@link NodeSearchResult} implements {@link INodeSearchResult}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class NodeSearchResult implements INodeSearchResult {
    private final NodeFullTextIndex index;
    private final ISearchResult result;

    public NodeSearchResult(NodeFullTextIndex index, ISearchResult result) {
        Assert.notNull(index);
        Assert.notNull(result);

        this.index = index;
        this.result = result;
    }

    @Override
    public int getTotalCount() {
        return result.getTotalCount();
    }

    @Override
    public Sort getSort() {
        return result.getSort();
    }

    @Override
    public List<INodeSearchResultElement> getTopElements() {
        List<ISearchResultElement> elements = result.getTopElements();

        List<INodeSearchResultElement> nodeElements = new ArrayList<INodeSearchResultElement>(elements.size());
        for (ISearchResultElement element : elements)
            nodeElements.add(new NodeSearchResultElement(index, element));

        return nodeElements;
    }
}
