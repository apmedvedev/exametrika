/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.fulltext;

import org.apache.lucene.search.Filter;

import com.exametrika.api.exadb.fulltext.IFilter;
import com.exametrika.common.utils.Assert;


/**
 * The {@link IndexFilter} is an index filter.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class IndexFilter implements IFilter {
    private final Filter filter;

    public IndexFilter(Filter filter) {
        Assert.notNull(filter);

        this.filter = filter;
    }

    public Filter getFilter() {
        return filter;
    }

    @Override
    public String toString() {
        return filter.toString();
    }
}
