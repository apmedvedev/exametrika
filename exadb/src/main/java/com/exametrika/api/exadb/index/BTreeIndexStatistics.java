/**
 * Copyright 2013 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.index;

import java.text.MessageFormat;


/**
 * The {@link BTreeIndexStatistics} is a B+ Tree index statistics.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class BTreeIndexStatistics {
    private final int indexHeight;
    private final long leafElementCount;
    private final long parentElementCount;
    private final long leafNodeCount;
    private final long parentNodeCount;
    private final long totalNodeCount;
    private final long leafDataSize;
    private final long parentDataSize;
    private final long totalDataSize;
    private final double leafUsage;
    private final double parentUsage;
    private final double totalUsage;
    private final double elementsPerLeafNode;
    private final double elementsPerParentNode;
    private final double averageLeafElementSize;
    private final double averageParentElementSize;

    public BTreeIndexStatistics(int indexHeight, long leafElementCount, long parentElementCount, long leafNodeCount,
                                long parentNodeCount, long totalNodeCount, long leafDataSize, long parentDataSize, long totalDataSize,
                                double leafUsage, double parentUsage, double totalUsage, double elementsPerLeafNode, double elementsPerParentNode,
                                double averageLeafElementSize, double averageParentElementSize) {
        this.indexHeight = indexHeight;
        this.leafElementCount = leafElementCount;
        this.parentElementCount = parentElementCount;
        this.leafNodeCount = leafNodeCount;
        this.parentNodeCount = parentNodeCount;
        this.totalNodeCount = totalNodeCount;
        this.leafDataSize = leafDataSize;
        this.parentDataSize = parentDataSize;
        this.totalDataSize = totalDataSize;
        this.leafUsage = leafUsage;
        this.parentUsage = parentUsage;
        this.totalUsage = totalUsage;
        this.elementsPerLeafNode = elementsPerLeafNode;
        this.elementsPerParentNode = elementsPerParentNode;
        this.averageLeafElementSize = averageLeafElementSize;
        this.averageParentElementSize = averageParentElementSize;
    }

    public int getIndexHeight() {
        return indexHeight;
    }

    public long getLeafElementCount() {
        return leafElementCount;
    }

    public long getParentElementCount() {
        return parentElementCount;
    }

    public long getLeafNodeCount() {
        return leafNodeCount;
    }

    public long getParentNodeCount() {
        return parentNodeCount;
    }

    public long getTotalNodeCount() {
        return totalNodeCount;
    }

    public long getLeafDataSize() {
        return leafDataSize;
    }

    public long getParentDataSize() {
        return parentDataSize;
    }

    public long getTotalDataSize() {
        return totalDataSize;
    }

    public double getLeafUsage() {
        return leafUsage;
    }

    public double getParentUsage() {
        return parentUsage;
    }

    public double getTotalUsage() {
        return totalUsage;
    }

    public double getElementsPerLeafNode() {
        return elementsPerLeafNode;
    }

    public double getElementsPerParentNode() {
        return elementsPerParentNode;
    }

    public double getAverageLeafElementSize() {
        return averageLeafElementSize;
    }

    public double getAverageParentElementSize() {
        return averageParentElementSize;
    }

    @Override
    public String toString() {
        return MessageFormat.format("height: {0}, leaf element count: {1}, parent element count: {2}, " +
                        "leaf node count: {3}, parent node count: {4}, total node count: {5}\n    " +
                        "leaf data size: {6}, parent data size: {7}, total data size: {8}\n    " +
                        "leaf usage: {9,number,percent}, parent usage: {10,number,percent}, total usage: {11,number,percent}\n    " +
                        "elements per leaf node: {12}, elements per parent node: {13}\n    " +
                        "average leaf element size: {14}, average parent element size: {15}", indexHeight,
                leafElementCount, parentElementCount,
                leafNodeCount, parentNodeCount, totalNodeCount,
                leafDataSize, parentDataSize, totalDataSize,
                leafUsage, parentUsage, totalUsage,
                elementsPerLeafNode, elementsPerParentNode,
                averageLeafElementSize, averageParentElementSize);
    }
}