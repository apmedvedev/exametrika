/**
 * Copyright 2013 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.index.btree;

import java.util.Deque;
import java.util.Set;

import com.exametrika.common.rawdb.IRawPage;
import com.exametrika.common.rawdb.IRawReadRegion;
import com.exametrika.common.rawdb.RawPageData;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Bytes;
import com.exametrika.common.utils.Out;
import com.exametrika.common.utils.Pair;

/**
 * The {@link BTreeIndexSpace} is an abstract B+ Tree node.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public abstract class BTreeNode extends RawPageData {
    public static final int HEADER_SIZE = 28;       // magic(short) + dataSize(short) + elementsStartOffset(short) +
    protected static final int SPLIT_DELTA = 5;     // leaf(prevNodePageIndex(long) + nextNodePageIndex(long)) /  
    protected static final double FILL_FACTOR = 0.8;// parent(lastChildPageIndex(long) + padding(long)) + 
    protected final BTreeIndexSpace space;          // commonPrefixLength(short) + commonPrefixOffset(short) + 
    protected final IRawPage page;                     // elementCount(short) + elementOffsets[short * maxElementCount]

    public static class ContextInfo {
        public final BTreeParentNode parent;
        public final int nodeElemenIndex;

        public ContextInfo(BTreeParentNode parent, int nodeElemenIndex) {
            this.parent = parent;
            this.nodeElemenIndex = nodeElemenIndex;
        }
    }

    public BTreeNode(BTreeIndexSpace space, IRawPage page) {
        Assert.notNull(space);
        Assert.notNull(page);

        this.space = space;

        page.setData(this);
        this.page = page;
    }

    public IRawPage getPage() {
        return page;
    }

    public abstract String toString(int indent);

    public abstract void assertValid(ByteArray prevNodeKey, ByteArray nodeKey, Set<Long> leafPages, Set<Long> parentPages,
                                     Out<Integer> actualIndexHeight, Out<Integer> maxIndexHeight);

    protected static int getMaxDataSize(IRawPage page, BTreeNodeOffsetVector offsets) {
        return page.getSize() - HEADER_SIZE - offsets.getSize();
    }

    protected static boolean isUnderflow(IRawPage page, BTreeNodeElementsArray elements, BTreeNodeOffsetVector offsets) {
        return (elements.getDataSize() < FILL_FACTOR * getMaxDataSize(page, offsets) / 2);
    }

    protected static int getSiblingNodeElementIndex(int nodeElementIndex) {
        return nodeElementIndex > 0 ? nodeElementIndex - 1 : nodeElementIndex + 1;
    }

    protected static BTreeNode getSibling(BTreeParentNode parent, int nodeElementIndex) {
        int siblingNodeElementIndex = getSiblingNodeElementIndex(nodeElementIndex);
        BTreeNode sibling = parent.getChild(siblingNodeElementIndex);
        Assert.checkState(sibling != null);
        return sibling;
    }

    protected final int getAddSplitElementIndex(BTreeNodeElementsArray elements, double splitDataSize, Out<ByteArray> splitKey) {
        IRawReadRegion region = page.getReadRegion();
        int elementCount = elements.offsets.getCount();
        boolean child = this instanceof BTreeLeafNode;
        Assert.isTrue(elementCount > 1);
        int splitElementIndex = -1;
        int elementDataSize = 0;
        if (space.isFixedKey() && elements.isFixedValue()) {
            int elementLength = space.getMaxKeySize() + elements.getMaxValueSize();
            splitElementIndex = (int) Math.ceil(splitDataSize / elementLength) - 1;
            elementDataSize = (splitElementIndex + 1) * elementLength;
            if (!child)
                elementDataSize -= elementLength;

            splitKey.value = elements.getKey(splitElementIndex).clone();
            return splitElementIndex;
        }

        for (int i = 0; i < elementCount - 1; i++) {
            int elementOffset = elements.offsets.getOffset(i);
            int currentElementLength = elements.getElementLength(region, elementOffset);
            elementDataSize += currentElementLength;
            if (i == elementCount - 2 || elementDataSize >= splitDataSize) {
                splitElementIndex = i;
                if (!child)
                    elementDataSize -= currentElementLength;
                break;
            }
        }

        Assert.checkState(splitElementIndex >= 0);

        if (child) {
            int splitDelta = (int) (SPLIT_DELTA * Math.log(elementCount) / Math.log(256));
            int start = Math.max(0, splitElementIndex - splitDelta);
            int end = Math.min(elementCount, splitElementIndex + splitDelta);

            int separatorIndex = -1;
            ByteArray separator = null;
            ByteArray prevKey = null;
            int centerDistance = 0;
            for (int i = start; i < end; i++) {
                ByteArray key = elements.getKey(i);

                if (i == start) {
                    separator = key;
                    separatorIndex = i;
                    centerDistance = Math.abs(i - splitElementIndex);
                } else {
                    ByteArray nextSeparator = BTreeIndexes.getSeparator(prevKey, key);
                    if (nextSeparator.getLength() < separator.getLength() ||
                            (nextSeparator.getLength() == separator.getLength() && Math.abs(i - splitElementIndex) < centerDistance)) {
                        separator = nextSeparator;
                        separatorIndex = i - 1;
                        centerDistance = Math.abs(i - splitElementIndex);
                    }
                }

                prevKey = key;
            }

            Assert.checkState(separatorIndex != -1 && separator != null);
            splitKey.value = Bytes.combine(elements.getCommonPrefix(), separator, true);

            return separatorIndex;
        } else {
            splitKey.value = Bytes.combine(elements.getCommonPrefix(), elements.getKey(splitElementIndex), true);
            return splitElementIndex;
        }
    }

    protected final int getRemoveSplitElementIndex(BTreeNodeElementsArray sourceElements, IRawReadRegion sourceRegion,
                                                   double splitDataSize, int sourceDataSize, int sourceElementCount, int destinationDataSize, int destinationElementCount, boolean direct) {
        if (sourceElementCount < 2)
            return -1;

        if (space.isFixedKey() && sourceElements.isFixedValue()) {
            int elementLength = space.getMaxKeySize() + sourceElements.getMaxValueSize();
            int destinationFreeElementCount = (page.getSize() - HEADER_SIZE - destinationElementCount *
                    BTreeNodeOffsetVector.OFFSET_ELEMENT_SIZE - destinationDataSize) / elementLength;

            if (direct)
                return Math.max((int) Math.ceil(splitDataSize / elementLength) - 1, sourceElementCount - destinationFreeElementCount - 1);
            else
                return Math.min((int) splitDataSize / elementLength - 1, destinationFreeElementCount - 1);
        } else {
            int destinationFreeDataSize = page.getSize() - HEADER_SIZE - destinationElementCount *
                    BTreeNodeOffsetVector.OFFSET_ELEMENT_SIZE - destinationDataSize;

            if (direct) {
                for (int i = sourceElementCount - 1; i >= 0; i--) {
                    int elementOffset = sourceElements.offsets.getOffset(i);
                    int currentElementLength = sourceElements.getElementLength(sourceRegion, elementOffset);
                    destinationFreeDataSize -= currentElementLength;
                    sourceDataSize -= currentElementLength;
                    if (destinationFreeDataSize < 0 || sourceDataSize < splitDataSize) {
                        if (i == sourceElementCount - 1)
                            return -1;
                        else
                            return i;
                    }
                }
            } else {
                int dataSize = 0;
                for (int i = 0; i < sourceElementCount; i++) {
                    int elementOffset = sourceElements.offsets.getOffset(i);
                    int currentElementLength = sourceElements.getElementLength(sourceRegion, elementOffset);
                    destinationFreeDataSize -= currentElementLength;
                    dataSize += currentElementLength;

                    if (destinationFreeDataSize < 0 || dataSize > splitDataSize) {
                        if (i == 0)
                            return -1;
                        else
                            return i - 1;
                    }
                }
            }
        }

        return -1;
    }

    protected Pair<ByteArray, ByteArray> getBounds(Deque<ContextInfo> stack) {
        ByteArray startBound = ByteArray.EMPTY;
        ByteArray endBound = ByteArray.EMPTY;
        for (ContextInfo info : stack) {
            if (startBound.isEmpty()) {
                if (info.nodeElemenIndex > 0)
                    startBound = info.parent.getKey(info.nodeElemenIndex - 1);
            }

            if (endBound.isEmpty()) {
                if (!info.parent.isLastChild(info.nodeElemenIndex))
                    endBound = info.parent.getKey(info.nodeElemenIndex);
            }

            if (!startBound.isEmpty() && !endBound.isEmpty())
                break;
        }

        return new Pair<ByteArray, ByteArray>(startBound, endBound);
    }
}