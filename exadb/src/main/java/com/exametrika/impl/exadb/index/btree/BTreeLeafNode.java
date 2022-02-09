/**
 * Copyright 2013 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.index.btree;

import java.util.Deque;
import java.util.Set;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.rawdb.IRawPage;
import com.exametrika.common.rawdb.IRawReadRegion;
import com.exametrika.common.rawdb.IRawWriteRegion;
import com.exametrika.common.rawdb.RawDatabaseException;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Bytes;
import com.exametrika.common.utils.Out;
import com.exametrika.common.utils.Pair;
import com.ibm.icu.text.MessageFormat;

/**
 * The {@link BTreeIndexSpace} is a B+ Tree leaf node.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class BTreeLeafNode extends BTreeNode {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final short MAGIC = 0x1711;// elements[[fixedKeySize = false -> keyLength(short)] + key[keyLength] + 
    private static final int LEAF_PREV_NODE_PAGE_INDEX_OFFSET = 6;// [fixedValueSize = false -> valueLength(short)] + 
    private static final int LEAF_NEXT_NODE_PAGE_INDEX_OFFSET = 14;// value(valueLength)] + commonPrefix
    private final BTreeNodeOffsetVector offsets;
    private final BTreeLeafNodeElementsArray elements;

    public static BTreeLeafNode create(BTreeIndexSpace space, IRawPage page) {
        BTreeLeafNode node = new BTreeLeafNode(space, page);
        node.writeHeader();
        space.updateLeafNodeCount(1);

        return node;
    }

    public static BTreeLeafNode open(BTreeIndexSpace space, IRawPage page) {
        BTreeLeafNode node = new BTreeLeafNode(space, page);
        node.readHeader();

        return node;
    }

    public long getPrevNodePageIndex() {
        IRawReadRegion region = page.getReadRegion();
        return region.readLong(LEAF_PREV_NODE_PAGE_INDEX_OFFSET);
    }

    public long getNextNodePageIndex() {
        IRawReadRegion region = page.getReadRegion();
        return region.readLong(LEAF_NEXT_NODE_PAGE_INDEX_OFFSET);
    }

    public int getElementCount() {
        return offsets.getCount();
    }

    public Pair<ByteArray, ByteArray> getElement(int index) {
        Pair<ByteArray, ByteArray> element = elements.getElement(index);
        ByteArray commonPrefix = elements.getCommonPrefix();
        if (!commonPrefix.isEmpty())
            return new Pair<ByteArray, ByteArray>(Bytes.combine(commonPrefix, element.getKey()), element.getValue());
        else
            return element;
    }

    public ByteArray getValue(int index) {
        return elements.getValue(index);
    }

    public <T extends IRawReadRegion> T findValueRegion(ByteArray key, boolean readOnly) {
        int elementIndex = elements.binarySearch(0, offsets.getCount(), key.subArray(offsets.getCommonPrefixLength()));
        if (elementIndex < 0)
            return null;

        return elements.getValueRegion(elementIndex, readOnly);
    }

    public int findIndex(ByteArray key) {
        return elements.binarySearch(0, offsets.getCount(), key.subArray(offsets.getCommonPrefixLength()));
    }

    public Pair<ByteArray, ByteArray> findFirst() {
        Pair<ByteArray, ByteArray> element = elements.getElement(0);
        ByteArray commonPrefix = elements.getCommonPrefix();
        if (!commonPrefix.isEmpty())
            return new Pair<ByteArray, ByteArray>(Bytes.combine(commonPrefix, element.getKey()), element.getValue());
        else
            return element;
    }

    public ByteArray findFirstValue() {
        return elements.getValue(0);
    }

    public Pair<ByteArray, ByteArray> findLast() {
        Pair<ByteArray, ByteArray> element = elements.getElement(offsets.getCount() - 1);
        ByteArray commonPrefix = elements.getCommonPrefix();
        if (commonPrefix.isEmpty())
            return new Pair<ByteArray, ByteArray>(Bytes.combine(commonPrefix, element.getKey()), element.getValue());
        else
            return element;
    }

    public ByteArray findLastValue() {
        return elements.getValue(offsets.getCount() - 1);
    }

    public ByteArray find(ByteArray key) {
        int elementIndex = elements.binarySearch(0, offsets.getCount(), key.subArray(offsets.getCommonPrefixLength()));
        if (elementIndex < 0)
            return null;

        return elements.getValue(elementIndex);
    }

    public BTreeLeafNode add(ByteArray key, ByteArray value, Deque<ContextInfo> stack, Out<ByteArray> splitKey, boolean bulk) {
        BTreeLeafNode splitNode = null;
        if (!elements.add(key.subArray(offsets.getCommonPrefixLength()), value, bulk))
            splitNode = split(key, value, stack, splitKey, bulk);

        return splitNode;
    }

    public boolean remove(ByteArray key, BTreeParentNode parent, int nodeElementIndex, Deque<ContextInfo> stack, Out<ByteArray> splitKey) {
        if (!elements.remove(key.subArray(offsets.getCommonPrefixLength())))
            return false;

        space.updateLeafElementCount(-1);

        if (parent == null)
            return offsets.getCount() == 0;

        if (!isUnderflow(page, elements, offsets))
            return false;

        if (!canMerge(parent, nodeElementIndex))
            return redistribute(parent, nodeElementIndex, stack, splitKey);

        splitKey.value = null;

        merge(parent, nodeElementIndex);

        return true;
    }

    @Override
    public String toString() {
        return toString(0);
    }

    @Override
    public String toString(int indent) {
        IRawReadRegion region = page.getReadRegion();
        int elementCount = offsets.getCount();
        String str = MessageFormat.format("leaf[{0}](data size: {1}, elements start: {2}, prev node: {3}, next node: {4}," +
                        " count: {5}, common prefix: {6}\n{7})\nelements:",
                page.getIndex(), elements.getDataSize(), elements.getStart(),
                region.readLong(LEAF_PREV_NODE_PAGE_INDEX_OFFSET), region.readLong(LEAF_NEXT_NODE_PAGE_INDEX_OFFSET), elementCount,
                elements.getCommonPrefix(), offsets);

        return str + elements.toString();
    }

    @Override
    public void assertValid(ByteArray prevNodeKey, ByteArray nodeKey, Set<Long> leafPages, Set<Long> parentPages,
                            Out<Integer> actualIndexHeight, Out<Integer> maxIndexHeight) {
        Assert.checkState(leafPages.add(page.getIndex()));
        IRawReadRegion region = page.getReadRegion();
        Assert.checkState(region.readShort(0) == MAGIC);

        int elementCount = offsets.getCount();

        int dataSize = elements.getDataSize();
        Assert.checkState(dataSize > 0 && HEADER_SIZE + offsets.getSize() + dataSize <= page.getSize());

        int elementsStartOffset = elements.getStart();
        Assert.checkState(elementsStartOffset >= HEADER_SIZE + offsets.getSize() && elementsStartOffset <= page.getSize());

        int elementsLength = 0;
        ByteArray commonPrefix = elements.getCommonPrefix();
        ByteArray prevKey = null;
        for (int i = 0; i < elementCount; i++) {
            int elementOffset = offsets.getOffset(i);
            int keyOffset, keyLength;
            if (space.isFixedKey()) {
                keyOffset = elementOffset;
                keyLength = space.getMaxKeySize();
            } else {
                keyLength = region.readShort(elementOffset);
                keyOffset = elementOffset + 2;
                Assert.checkState(keyLength <= space.getMaxKeySize());
            }

            int elementLength = elements.getElementLength(region, elementOffset);

            Assert.checkState(elementOffset >= elementsStartOffset);
            Assert.checkState(elementOffset + elementLength <= page.getSize());
            elementsLength += elementLength;

            ByteArray key = region.readByteArray(keyOffset, keyLength);
            Assert.checkState(BTreeIndexes.getKeyDigest(key) == offsets.getKeyDigest(i));
            key = Bytes.combine(commonPrefix, key);

            if (i == 0 && prevNodeKey != null)
                Assert.checkState(prevNodeKey.compareTo(key) < 0);

            if (prevKey != null)
                Assert.checkState(prevKey.compareTo(key) < 0);

            prevKey = key;
        }

        Assert.checkState(dataSize == elementsLength + offsets.getCommonPrefixLength());
        Assert.checkState((offsets.getCommonPrefixLength() == 0) == (offsets.getCommonPrefixOffset() == 0));
        Assert.checkState(!space.isFixedKey() || offsets.getCommonPrefixOffset() == 0);

        if (nodeKey != null)
            Assert.checkState(prevKey.compareTo(nodeKey) <= 0);

        long prevNodePageIndex = region.readLong(LEAF_PREV_NODE_PAGE_INDEX_OFFSET);
        if (prevNodePageIndex == 0)
            Assert.checkState(space.getFirstLeafNodePageIndex() == page.getIndex());
        else {
            BTreeLeafNode prevNode = (BTreeLeafNode) space.getNode(prevNodePageIndex);
            Assert.checkState(prevNode.getPage().getReadRegion().readLong(LEAF_NEXT_NODE_PAGE_INDEX_OFFSET) == page.getIndex());
        }

        long nextNodePageIndex = region.readLong(LEAF_NEXT_NODE_PAGE_INDEX_OFFSET);
        if (nextNodePageIndex == 0)
            Assert.checkState(space.getLastLeafNodePageIndex() == page.getIndex());
        else {
            BTreeLeafNode nextNode = (BTreeLeafNode) space.getNode(nextNodePageIndex);
            Assert.checkState(nextNode.getPage().getReadRegion().readLong(LEAF_PREV_NODE_PAGE_INDEX_OFFSET) == page.getIndex());
        }
    }

    private BTreeLeafNode(BTreeIndexSpace space, IRawPage page) {
        super(space, page);

        offsets = new BTreeNodeOffsetVector(page);
        elements = new BTreeLeafNodeElementsArray(space, offsets, page);
    }

    private void readHeader() {
        if (page.getReadRegion().readShort(0) != MAGIC)
            throw new RawDatabaseException(messages.invalidFormat(page.getFile().getIndex()));
    }

    private void writeHeader() {
        IRawWriteRegion region = page.getWriteRegion();

        region.writeShort(0, MAGIC);
        region.writeLong(LEAF_PREV_NODE_PAGE_INDEX_OFFSET, 0l);
        region.writeLong(LEAF_NEXT_NODE_PAGE_INDEX_OFFSET, 0l);
        offsets.create();
        elements.create();
    }

    private boolean canMerge(BTreeParentNode parent, int nodeElementIndex) {
        BTreeLeafNode sibling = (BTreeLeafNode) getSibling(parent, nodeElementIndex);

        int dataSize = elements.getDataSize();
        ByteArray commonPrefix = elements.getCommonPrefix();
        int siblingDataSize = sibling.elements.getDataSize();
        int siblingElementCount = sibling.offsets.getCount();
        ByteArray siblingCommonPrefix = sibling.elements.getCommonPrefix();
        int newCommonPrefixLength = BTreeIndexes.getCommonPrefixLength(commonPrefix, siblingCommonPrefix);
        int elementSizeDelta = commonPrefix.getLength() - newCommonPrefixLength;
        int siblingElementSizeDelta = siblingCommonPrefix.getLength() - newCommonPrefixLength;

        int maxDataSize = page.getSize() - HEADER_SIZE - offsets.getSize();
        int elementCount = offsets.getCount();

        return ((dataSize + siblingDataSize <= (1 + FILL_FACTOR) / 2 * maxDataSize) &&
                (HEADER_SIZE + (elementCount + siblingElementCount) * BTreeNodeOffsetVector.OFFSET_ELEMENT_SIZE + (dataSize + siblingDataSize) +
                        elementSizeDelta * elementCount + siblingElementSizeDelta * siblingElementCount <= page.getSize()));
    }

    private BTreeLeafNode split(ByteArray key, ByteArray value, Deque<ContextInfo> stack, Out<ByteArray> splitKey, boolean bulk) {
        IRawPage newPage = space.allocatePage();
        BTreeLeafNode newNode = BTreeLeafNode.create(space, newPage);

        addToNodeList(newNode);

        int dataSize = elements.getDataSize();
        int elementCount = offsets.getCount();
        int splitElementIndex = getAddSplitElementIndex(elements, (double) dataSize / 2, splitKey);

        ByteArray oldCommonPrefix = elements.getCommonPrefix().clone();
        ByteArray firstCommonPrefix = ByteArray.EMPTY;
        ByteArray secondCommonPrefix = ByteArray.EMPTY;
        if (!space.isFixedKey()) {
            Pair<ByteArray, ByteArray> bounds = getBounds(stack);

            firstCommonPrefix = BTreeIndexes.getCommonPrefix(bounds.getKey(), splitKey.value).clone();
            secondCommonPrefix = BTreeIndexes.getCommonPrefix(splitKey.value, bounds.getValue()).clone();

            if (!secondCommonPrefix.isEmpty())
                newNode.elements.setCommonPrefix(secondCommonPrefix);
        }

        boolean keyAppended = false;
        boolean replaced = false;
        boolean allowAppendKeyToNewNode = key.compareTo(splitKey.value) > 0;
        for (int i = splitElementIndex + 1; i < elementCount; i++) {
            Pair<ByteArray, ByteArray> element = elements.getElement(i);
            ByteArray oldKey = element.getKey().subArray(secondCommonPrefix.getLength() - oldCommonPrefix.getLength());

            boolean replace = false;
            if (allowAppendKeyToNewNode && !keyAppended && key.subArray(oldCommonPrefix.getLength()).compareTo(element.getKey()) <= 0) {
                ByteArray newKey = key.subArray(secondCommonPrefix.getLength());
                replace = oldKey.equals(newKey);
                replaced = replace;
                newNode.elements.appendSorted(newKey, value);
                keyAppended = true;
            }

            if (!replace)
                newNode.elements.appendSorted(oldKey, element.getValue());
        }

        elements.remove(splitElementIndex + 1, elementCount - splitElementIndex - 1);

        if (firstCommonPrefix.getLength() != oldCommonPrefix.getLength()) {
            elements.truncatePrefix(firstCommonPrefix.getLength() - oldCommonPrefix.getLength());
            elements.setCommonPrefix(firstCommonPrefix);
        }

        if (!keyAppended) {
            if (allowAppendKeyToNewNode) {
                newNode.elements.appendSorted(key.subArray(secondCommonPrefix.getLength()), value);
                keyAppended = true;
            } else
                elements.add(key.subArray(firstCommonPrefix.getLength()), value, bulk);
        }

        if (keyAppended && !replaced)
            space.updateLeafElementCount(1);

        return newNode;
    }

    private boolean redistribute(BTreeParentNode parent, int nodeElementIndex, Deque<ContextInfo> stack,
                                 Out<ByteArray> splitKey) {
        BTreeLeafNode sibling = (BTreeLeafNode) getSibling(parent, nodeElementIndex);

        int elementCount = offsets.getCount();
        int dataSize = elements.getDataSize();
        IRawWriteRegion siblingRegion = sibling.getPage().getWriteRegion();
        int siblingDataSize = sibling.elements.getDataSize();
        int siblingElementCount = sibling.offsets.getCount();

        if (nodeElementIndex > 0) {
            int splitElementIndex = getRemoveSplitElementIndex(sibling.elements, siblingRegion, (double) (dataSize + siblingDataSize) / 2,
                    siblingDataSize, siblingElementCount, dataSize, elementCount, true);
            if (splitElementIndex < 0)
                return false;

            boolean canRedistribute = false;
            ByteArray prefixDelta = ByteArray.EMPTY;
            int prefixDeltaLength = 0;
            ByteArray oldSiblingCommonPrefix = ByteArray.EMPTY;
            ByteArray newSiblingCommonPrefix = ByteArray.EMPTY;
            for (; splitElementIndex < siblingElementCount - 1; splitElementIndex++) {
                ByteArray oldKey = parent.getKey(nodeElementIndex - 1);
                ByteArray newKey = sibling.elements.getKey(splitElementIndex);
                if (parent.getFreeSpace() < newKey.getLength() + sibling.offsets.getCommonPrefixLength() - oldKey.getLength())
                    return false;

                splitKey.value = newKey.clone();

                if (!space.isFixedKey()) {
                    ByteArray oldCommonPrefix = elements.getCommonPrefix().clone();
                    oldSiblingCommonPrefix = sibling.elements.getCommonPrefix().clone();

                    splitKey.value = Bytes.combine(oldSiblingCommonPrefix, splitKey.value);

                    Pair<ByteArray, ByteArray> bounds = getBounds(stack);
                    ContextInfo info = stack.pop();
                    stack.push(new ContextInfo(info.parent, info.nodeElemenIndex - 1));
                    Pair<ByteArray, ByteArray> siblingBounds = getBounds(stack);

                    newSiblingCommonPrefix = BTreeIndexes.getCommonPrefix(siblingBounds.getKey(), splitKey.value).clone();
                    ByteArray newCommonPrefix = BTreeIndexes.getCommonPrefix(splitKey.value, bounds.getValue()).clone();

                    prefixDeltaLength = oldSiblingCommonPrefix.getLength() - newCommonPrefix.getLength();

                    if (elementCount * (oldCommonPrefix.getLength() - newCommonPrefix.getLength()) +
                            prefixDeltaLength * (siblingElementCount - splitElementIndex - 1) +
                            sibling.elements.getDataSize(splitElementIndex + 1, siblingElementCount - splitElementIndex - 1) > elements.getFreeSpace())
                        continue;

                    if (newCommonPrefix.getLength() < oldCommonPrefix.getLength()) {
                        elements.setCommonPrefix(newCommonPrefix);
                        elements.growPrefix(oldCommonPrefix.subArray(newCommonPrefix.getLength()));
                    }

                    if (prefixDeltaLength > 0)
                        prefixDelta = oldSiblingCommonPrefix.subArray(newCommonPrefix.getLength());
                }

                canRedistribute = true;
                break;
            }

            if (!canRedistribute)
                return false;

            elements.copy(sibling.elements, splitElementIndex + 1, 0, siblingElementCount - splitElementIndex - 1,
                    prefixDeltaLength, prefixDelta);
            sibling.elements.remove(splitElementIndex + 1, siblingElementCount - splitElementIndex - 1);

            if (!space.isFixedKey()) {
                if (newSiblingCommonPrefix.getLength() > oldSiblingCommonPrefix.getLength()) {
                    sibling.elements.setCommonPrefix(newSiblingCommonPrefix);
                    sibling.elements.truncatePrefix(newSiblingCommonPrefix.getLength() - oldSiblingCommonPrefix.getLength());
                }
            }
        } else {
            int splitElementIndex = getRemoveSplitElementIndex(sibling.elements, siblingRegion, (double) (siblingDataSize - dataSize) / 2,
                    siblingDataSize, siblingElementCount, dataSize, elementCount, false);
            if (splitElementIndex < 0)
                return false;

            ByteArray prefixDelta = ByteArray.EMPTY;
            int prefixDeltaLength = 0;
            ByteArray oldSiblingCommonPrefix = ByteArray.EMPTY;
            ByteArray newSiblingCommonPrefix = ByteArray.EMPTY;
            boolean canRedistribute = false;
            for (; splitElementIndex >= 0; splitElementIndex--) {
                ByteArray oldKey = parent.getKey(nodeElementIndex);
                ByteArray newKey = sibling.elements.getKey(splitElementIndex);
                if (parent.getFreeSpace() < newKey.getLength() + sibling.offsets.getCommonPrefixLength() - oldKey.getLength())
                    return false;

                splitKey.value = newKey.clone();

                if (!space.isFixedKey()) {
                    ByteArray oldCommonPrefix = elements.getCommonPrefix().clone();
                    oldSiblingCommonPrefix = sibling.elements.getCommonPrefix().clone();

                    splitKey.value = Bytes.combine(oldSiblingCommonPrefix, splitKey.value);

                    Pair<ByteArray, ByteArray> bounds = getBounds(stack);
                    ContextInfo info = stack.pop();
                    stack.push(new ContextInfo(info.parent, info.nodeElemenIndex + 1));
                    Pair<ByteArray, ByteArray> siblingBounds = getBounds(stack);

                    ByteArray newCommonPrefix = BTreeIndexes.getCommonPrefix(bounds.getKey(), splitKey.value).clone();
                    newSiblingCommonPrefix = BTreeIndexes.getCommonPrefix(splitKey.value, siblingBounds.getValue()).clone();

                    prefixDeltaLength = oldSiblingCommonPrefix.getLength() - newCommonPrefix.getLength();

                    if (elementCount * (oldCommonPrefix.getLength() - newCommonPrefix.getLength()) +
                            prefixDeltaLength * (splitElementIndex + 1) +
                            sibling.elements.getDataSize(0, splitElementIndex + 1) > elements.getFreeSpace())
                        continue;

                    if (newCommonPrefix.getLength() < oldCommonPrefix.getLength()) {
                        elements.setCommonPrefix(newCommonPrefix);
                        elements.growPrefix(oldCommonPrefix.subArray(newCommonPrefix.getLength()));
                    }

                    if (prefixDeltaLength > 0)
                        prefixDelta = oldSiblingCommonPrefix.subArray(newCommonPrefix.getLength());
                }
                canRedistribute = true;
                break;
            }

            if (!canRedistribute)
                return false;

            elements.copy(sibling.elements, 0, elementCount, splitElementIndex + 1, prefixDeltaLength, prefixDelta);
            sibling.elements.remove(0, splitElementIndex + 1);

            if (!space.isFixedKey()) {
                if (newSiblingCommonPrefix.getLength() > oldSiblingCommonPrefix.getLength()) {
                    sibling.elements.setCommonPrefix(newSiblingCommonPrefix);
                    sibling.elements.truncatePrefix(newSiblingCommonPrefix.getLength() - oldSiblingCommonPrefix.getLength());
                }
            }
        }

        return true;
    }

    private void merge(BTreeParentNode parent, int nodeElementIndex) {
        BTreeLeafNode sibling = (BTreeLeafNode) getSibling(parent, nodeElementIndex);
        ByteArray prefixDelta = ByteArray.EMPTY;
        if (!space.isFixedKey()) {
            ByteArray commonPrefix = elements.getCommonPrefix().clone();
            ByteArray siblingCommonPrefix = sibling.elements.getCommonPrefix().clone();
            ByteArray newCommonPrefix = BTreeIndexes.getCommonPrefix(commonPrefix, siblingCommonPrefix);

            if (newCommonPrefix.getLength() != siblingCommonPrefix.getLength()) {
                sibling.elements.setCommonPrefix(newCommonPrefix);
                sibling.elements.growPrefix(siblingCommonPrefix.subArray(newCommonPrefix.getLength()));
            }

            prefixDelta = commonPrefix.subArray(newCommonPrefix.getLength());
        }

        int elementCount = offsets.getCount();
        int siblingElementCount = sibling.offsets.getCount();

        if (nodeElementIndex > 0)
            sibling.elements.copy(elements, 0, siblingElementCount, elementCount, prefixDelta.getLength(), prefixDelta);
        else
            sibling.elements.copy(elements, 0, 0, elementCount, prefixDelta.getLength(), prefixDelta);

        removeFromNodeList(nodeElementIndex, sibling);
        space.freePage(page);
        space.updateLeafNodeCount(-1);
    }

    private void addToNodeList(BTreeLeafNode newNode) {
        IRawWriteRegion region = page.getWriteRegion();

        long nextNodePageIndex = region.readLong(LEAF_NEXT_NODE_PAGE_INDEX_OFFSET);
        if (nextNodePageIndex != 0) {
            BTreeLeafNode nextNode = (BTreeLeafNode) space.getNode(nextNodePageIndex);
            nextNode.getPage().getWriteRegion().writeLong(LEAF_PREV_NODE_PAGE_INDEX_OFFSET, newNode.getPage().getIndex());
        } else
            space.setLastLeafNodePageIndex(newNode.getPage().getIndex());

        region.writeLong(LEAF_NEXT_NODE_PAGE_INDEX_OFFSET, newNode.getPage().getIndex());
        newNode.getPage().getWriteRegion().writeLong(LEAF_PREV_NODE_PAGE_INDEX_OFFSET, page.getIndex());
        newNode.getPage().getWriteRegion().writeLong(LEAF_NEXT_NODE_PAGE_INDEX_OFFSET, nextNodePageIndex);
    }

    private void removeFromNodeList(int nodeElementIndex, BTreeLeafNode sibling) {
        IRawWriteRegion region = page.getWriteRegion();

        BTreeLeafNode prevNode, nextNode;
        long prevNodePageIndex, nextNodePageIndex;
        if (nodeElementIndex == 0) {
            prevNodePageIndex = region.readLong(LEAF_PREV_NODE_PAGE_INDEX_OFFSET);
            if (prevNodePageIndex != 0)
                prevNode = (BTreeLeafNode) space.getNode(prevNodePageIndex);
            else
                prevNode = null;

            nextNode = sibling;
            nextNodePageIndex = nextNode.getPage().getIndex();
        } else {
            nextNodePageIndex = region.readLong(LEAF_NEXT_NODE_PAGE_INDEX_OFFSET);
            if (nextNodePageIndex != 0)
                nextNode = (BTreeLeafNode) space.getNode(nextNodePageIndex);
            else
                nextNode = null;

            prevNode = sibling;
            prevNodePageIndex = prevNode.getPage().getIndex();
        }

        if (prevNode != null)
            prevNode.getPage().getWriteRegion().writeLong(LEAF_NEXT_NODE_PAGE_INDEX_OFFSET, nextNodePageIndex);
        else
            space.setFirstLeafNodePageIndex(nextNodePageIndex);

        if (nextNode != null)
            nextNode.getPage().getWriteRegion().writeLong(LEAF_PREV_NODE_PAGE_INDEX_OFFSET, prevNodePageIndex);
        else
            space.setLastLeafNodePageIndex(prevNodePageIndex);
    }

    private interface IMessages {
        @DefaultMessage("Invalid format of file ''{0}''.")
        ILocalizedMessage invalidFormat(int fileIndex);

        @DefaultMessage("Unsupported version ''{1}'' of file ''{0}'', expected version - ''{2}''.")
        ILocalizedMessage unsupportedVersion(int fileIndex, int fileVersion, int expectedVersion);
    }
}