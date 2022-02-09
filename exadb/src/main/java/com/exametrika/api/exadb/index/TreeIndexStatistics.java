/**
 * Copyright 2013 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.index;

import java.text.MessageFormat;


/**
 * The {@link TreeIndexStatistics} is an in-memory tree index statistics.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class TreeIndexStatistics {
    private final long elementCount;
    private final long pageCount;
    private final long dataSize;
    private final double usage;
    private final double elementsPerPage;
    private final double averageElementSize;

    public TreeIndexStatistics(long elementCount, long pageCount, long dataSize, double usage, double elementsPerPage,
                               double averageElementSize) {
        this.elementCount = elementCount;
        this.pageCount = pageCount;
        this.dataSize = dataSize;
        this.usage = usage;
        this.elementsPerPage = elementsPerPage;
        this.averageElementSize = averageElementSize;
    }

    public long getElementCount() {
        return elementCount;
    }

    public long getPageCount() {
        return pageCount;
    }

    public long getDataSize() {
        return dataSize;
    }

    public double getUsage() {
        return usage;
    }

    public double getElementsPerPage() {
        return elementsPerPage;
    }

    public double getAverageElementSize() {
        return averageElementSize;
    }

    @Override
    public String toString() {
        return MessageFormat.format("element count: {0}, page count: {1}, data size: {2}, usage: {3,number,percent}, " +
                        "elements per page: {4}, average element size: {5}", elementCount, pageCount, dataSize, usage, elementsPerPage,
                averageElementSize);
    }
}