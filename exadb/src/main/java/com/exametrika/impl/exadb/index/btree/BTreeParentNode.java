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
import com.exametrika.common.utils.Strings;
import com.ibm.icu.text.MessageFormat;

public final class BTreeParentNode extends BTreeNode {
    private static final IMessages messages = Messages.get(IMessages.class);
    public static final short MAGIC = 0x1710;// elements[[fixedKeySize = false -> keyLength(short)] + key[keyLength] + childPageIndex(long)] +
    private final BTreeNodeOffsetVector offsets;// commonPrefix
    private final BTreeParentNodeElementsArray elements;

    public static BTreeParentNode create(BTreeIndexSpace space, IRawPage page,
                                         ByteArray splitKey, BTreeNode firstNode, BTreeNode secondNode) {
        BTreeParentNode node = new BTreeParentNode(space, page);
        node.writeHeader(splitKey, firstNode, secondNode);
        space.updateParentNodeCount(1);

        return node;
    }

    public static BTreeParentNode open(BTreeIndexSpace space, IRawPage page) {
        BTreeParentNode node = new BTreeParentNode(space, page);
        node.readHeader();

        return node;
    }

    public int getElementCount() {
        return offsets.getCount();
    }

    public long getLastChildPageIndex() {
        return elements.getLastChildPageIndex();
    }

    public BTreeNode getChild(int elementIndex) {
        int elementCount = offsets.getCount();
        if (elementCount == 0)
            return null;

        if (elementIndex < elementCount) {
            Pair<ByteArray, Long> pair = elements.getElement(elementIndex);
            return space.getNode(pair.getValue());
        } else {
            long lastChildPageIndex = elements.getLastChildPageIndex();
            return space.getNode(lastChildPageIndex);
        }
    }

    public ByteArray getKey(int index) {
        ByteArray commonPrefix = elements.getCommonPrefix();
        ByteArray key = elements.getKey(index);
        if (!commonPrefix.isEmpty())
            return Bytes.combine(elements.getCommonPrefix(), key);
        else
            return key;
    }

    public boolean isLastChild(int index) {
        return offsets.getCount() == index;
    }

    public int getFreeSpace() {
        return elements.getFreeSpace();
    }

    public long find(ByteArray key, Out<Integer> index) {
        key = key.subArray(offsets.getCommonPrefixLength());

        int elementCount = offsets.getCount();
        int elementIndex = elements.binarySearch(0, elementCount, key);
        if (elementIndex < 0)
            elementIndex = -elementIndex - 1;

        if (index != null)
            index.value = elementIndex;

        if (elementIndex == elementCount)
            return elements.getLastChildPageIndex();
        else
            return elements.getValue(elementIndex);
    }

    public BTreeParentNode add(int index, ByteArray childSplitKey, long firstChildPageIndex, long secondChildPageIndex,
                               Deque<ContextInfo> stack, Out<ByteArray> splitKey) {
        BTreeParentNode splitNode = null;
        if (!elements.add(index, childSplitKey.subArray(offsets.getCommonPrefixLength()), firstChildPageIndex, secondChildPageIndex))
            splitNode = split(index, childSplitKey, firstChildPageIndex, secondChildPageIndex, stack, splitKey);

        space.updateParentElementCount(1);
        return splitNode;
    }

    public boolean remove(int childNodeElementIndex, ByteArray childSplitKey, BTreeParentNode parent, int nodeElementIndex,
                          Deque<ContextInfo> stack, Out<ByteArray> splitKey) {
        if (childSplitKey != null) {
            replaceKey(childNodeElementIndex, childSplitKey);
            return false;
        }

        elements.remove(childNodeElementIndex);

        space.updateParentElementCount(-1);

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
        int elementCount = offsets.getCount();
        String str = MessageFormat.format("parent[{0}](data size: {1}, elements start: {2}, count: {3}, common prefix: {4}\n{5})\nelements:",
                page.getIndex(), elements.getDataSize(), elements.getStart(), elementCount, elements.getCommonPrefix(), offsets);

        StringBuilder builder = new StringBuilder(str);
        builder.append(elements);

        builder.append("\nchildren:");

        for (int i = 0; i < elementCount; i++) {
            Pair<ByteArray, Long> element = elements.getElement(i);
            BTreeNode node = space.getNode(element.getValue());
            builder.append("\n" + Strings.indent(node.toString(indent + 4), 4));
        }

        BTreeNode node = space.getNode(elements.getLastChildPageIndex());
        builder.append("\n" + Strings.indent("last " + node.toString(indent + 4), 4));

        return builder.toString();
    }

    @Override
    public void assertValid(ByteArray prevNodeKey, ByteArray nodeKey, Set<Long> leafPages, Set<Long> parentPages,
                            Out<Integer> actualIndexHeight, Out<Integer> maxIndexHeight) {
        Assert.checkState(parentPages.add(page.getIndex()));
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
            int keyOffset, keyLength, elementLength;
            if (space.isFixedKey()) {
                keyOffset = elementOffset;
                keyLength = space.getMaxKeySize();
                elementLength = space.getMaxKeySize() + BTreeParentNodeElementsArray.VALUE_SIZE;
            } else {
                keyLength = region.readShort(elementOffset);
                keyOffset = elementOffset + 2;
                Assert.checkState(keyLength <= space.getMaxKeySize());
                elementLength = keyLength + BTreeParentNodeElementsArray.VALUE_SIZE + 2;
            }

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

        prevKey = prevNodeKey;
        for (int i = 0; i < elementCount; i++) {
            Pair<ByteArray, Long> pair = elements.getElement(i);
            ByteArray currentNodeKey = pair.getKey();
            currentNodeKey = Bytes.combine(commonPrefix, currentNodeKey);

            BTreeNode node = space.getNode(pair.getValue());

            actualIndexHeight.value++;
            if (maxIndexHeight.value < actualIndexHeight.value)
                maxIndexHeight.value = actualIndexHeight.value;

            node.assertValid(prevKey, currentNodeKey, leafPages, parentPages, actualIndexHeight, maxIndexHeight);

            actualIndexHeight.value--;
            prevKey = currentNodeKey;
        }

        long lastChildPageIndex = elements.getLastChildPageIndex();
        BTreeNode node = space.getNode(lastChildPageIndex);

        actualIndexHeight.value++;
        if (maxIndexHeight.value < actualIndexHeight.value)
            maxIndexHeight.value = actualIndexHeight.value;

        node.assertValid(prevKey, nodeKey, leafPages, parentPages, actualIndexHeight, maxIndexHeight);

        actualIndexHeight.value--;
    }

    private BTreeParentNode(BTreeIndexSpace space, IRawPage page) {
        super(space, page);

        offsets = new BTreeNodeOffsetVector(page);
        elements = new BTreeParentNodeElementsArray(space, offsets, page);
    }

    private void readHeader() {
        if (page.getReadRegion().readShort(0) != MAGIC)
            throw new RawDatabaseException(messages.invalidFormat(page.getFile().getIndex()));
    }

    private void writeHeader(ByteArray splitKey, BTreeNode firstNode, BTreeNode secondNode) {
        IRawWriteRegion region = page.getWriteRegion();

        region.writeShort(0, MAGIC);

        elements.create();
        offsets.create();

        if (firstNode != null && secondNode != null) {
            elements.appendSorted(splitKey, firstNode.getPage().getIndex());
            elements.setLastChildPageIndex(secondNode.getPage().getIndex());
        }
    }

    private void replaceKey(int childNodeElementIndex, ByteArray childSplitKey) {
        childSplitKey = childSplitKey.subArray(offsets.getCommonPrefixLength());
        if (childNodeElementIndex > 0)
            elements.set(childNodeElementIndex - 1, childSplitKey);
        else
            elements.set(0, childSplitKey);
    }

    private boolean canMerge(BTreeParentNode parent, int nodeElementIndex) {
        BTreeParentNode sibling = (BTreeParentNode) getSibling(parent, nodeElementIndex);

        int elementCount = offsets.getCount();
        Assert.checkState(elementCount > 0);

        int dataSize = elements.getDataSize();
        int valueSize = BTreeParentNodeElementsArray.VALUE_SIZE;

        int siblingDataSize = sibling.elements.getDataSize();
        int siblingElementCount = sibling.offsets.getCount();

        ByteArray commonPrefix = elements.getCommonPrefix();
        ByteArray siblingCommonPrefix = sibling.elements.getCommonPrefix();
        int newCommonPrefixLength = BTreeIndexes.getCommonPrefixLength(commonPrefix, siblingCommonPrefix);
        int elementSizeDelta = commonPrefix.getLength() - newCommonPrefixLength;
        int siblingElementSizeDelta = siblingCommonPrefix.getLength() - newCommonPrefixLength;

        ByteArray lastChildKey;
        if (nodeElementIndex > 0)
            lastChildKey = parent.getKey(nodeElementIndex - 1);
        else
            lastChildKey = parent.getKey(nodeElementIndex);

        int lastChildKeyLength = lastChildKey.getLength() - newCommonPrefixLength;

        int maxDataSize = getMaxDataSize(page, offsets);

        return ((dataSize + siblingDataSize + (lastChildKey.getLength() + valueSize + 2 + BTreeNodeOffsetVector.OFFSET_ELEMENT_SIZE) <=
                (1 + FILL_FACTOR) / 2 * maxDataSize) &&
                (HEADER_SIZE + (elementCount + siblingElementCount) * BTreeNodeOffsetVector.OFFSET_ELEMENT_SIZE + (dataSize + siblingDataSize) +
                        elementSizeDelta * elementCount + siblingElementSizeDelta * siblingElementCount +
                        (lastChildKeyLength + valueSize + 2 + BTreeNodeOffsetVector.OFFSET_ELEMENT_SIZE) <= page.getSize()));
    }

    private BTreeParentNode split(int index, ByteArray childSplitKey, long firstChildPageIndex, long secondChildPageIndex,
                                  Deque<ContextInfo> stack, Out<ByteArray> splitKey) {
        IRawPage newPage = space.allocatePage();
        BTreeParentNode newNode = BTreeParentNode.create(space, newPage, null, null, null);

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
        boolean allowAppendKeyToNewNode = index > splitElementIndex;
        for (int i = splitElementIndex + 1; i < elementCount; i++) {
            Pair<ByteArray, Long> element = elements.getElement(i);

            if (allowAppendKeyToNewNode && index == i) {
                newNode.elements.appendSorted(childSplitKey.subArray(secondCommonPrefix.getLength()), firstChildPageIndex);
                newNode.elements.appendSorted(element.getKey().subArray(secondCommonPrefix.getLength() - oldCommonPrefix.getLength()),
                        secondChildPageIndex);
                keyAppended = true;
            } else
                newNode.elements.appendSorted(element.getKey().subArray(secondCommonPrefix.getLength() - oldCommonPrefix.getLength()),
                        element.getValue());
        }

        newNode.elements.setLastChildPageIndex(elements.getLastChildPageIndex());

        elements.remove(splitElementIndex + 1, elementCount - splitElementIndex - 1);

        if (firstCommonPrefix.getLength() != oldCommonPrefix.getLength()) {
            elements.truncatePrefix(firstCommonPrefix.getLength() - oldCommonPrefix.getLength());
            elements.setCommonPrefix(firstCommonPrefix);
        }

        if (!keyAppended) {
            if (allowAppendKeyToNewNode) {
                newNode.elements.appendSorted(childSplitKey.subArray(secondCommonPrefix.getLength()), firstChildPageIndex);
                newNode.elements.setLastChildPageIndex(secondChildPageIndex);
            } else
                elements.add(index, childSplitKey.subArray(firstCommonPrefix.getLength()), firstChildPageIndex, secondChildPageIndex);
        }

        return newNode;
    }

    private boolean redistribute(BTreeParentNode parent, int nodeElementIndex, Deque<ContextInfo> stack, Out<ByteArray> splitKey) {
        boolean fixedKey = space.isFixedKey();
        int maxKeySize = space.getMaxKeySize();

        int elementCount = offsets.getCount();
        int dataSize = elements.getDataSize();
        int valueSize = BTreeParentNodeElementsArray.VALUE_SIZE;

        int siblingNodeElementIndex = nodeElementIndex > 0 ? nodeElementIndex - 1 : nodeElementIndex + 1;
        BTreeParentNode sibling = (BTreeParentNode) parent.getChild(siblingNodeElementIndex);
        Assert.checkState(sibling != null);

        IRawWriteRegion siblingRegion = sibling.getPage().getWriteRegion();
        int siblingDataSize = sibling.elements.getDataSize();
        int siblingElementCount = sibling.offsets.getCount();

        ByteArray lastChildKey;
        if (nodeElementIndex > 0)
            lastChildKey = parent.getKey(siblingNodeElementIndex);
        else
            lastChildKey = parent.getKey(nodeElementIndex);

        if (nodeElementIndex > 0) {
            int siblingLastElementLength;
            if (fixedKey)
                siblingLastElementLength = maxKeySize + valueSize;
            else
                siblingLastElementLength = lastChildKey.getLength() + valueSize + 2;

            int splitElementIndex = getRemoveSplitElementIndex(sibling.elements, siblingRegion, (double) (dataSize + siblingDataSize) / 2, siblingDataSize,
                    siblingElementCount, dataSize - siblingLastElementLength, elementCount - 1, true);

            if (splitElementIndex == -1)
                return false;

            boolean canRedistribute = false;
            ByteArray prefixDelta = ByteArray.EMPTY;
            int prefixDeltaLength = 0;
            ByteArray oldSiblingCommonPrefix = ByteArray.EMPTY;
            ByteArray newSiblingCommonPrefix = ByteArray.EMPTY;
            for (; splitElementIndex < siblingElementCount - 1; splitElementIndex++) {
                ByteArray oldKey = lastChildKey;
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
                            sibling.elements.getDataSize(splitElementIndex + 1, siblingElementCount - splitElementIndex - 1) +
                            (lastChildKey.getLength() - newCommonPrefix.getLength()) > elements.getFreeSpace())
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

            elements.copy(sibling.elements, splitElementIndex + 1, new Pair<ByteArray, Long>(lastChildKey.subArray(oldSiblingCommonPrefix.getLength()),
                    sibling.getLastChildPageIndex()), 0, null, siblingElementCount - splitElementIndex - 1, prefixDeltaLength, prefixDelta);
            sibling.elements.remove(splitElementIndex + 1, siblingElementCount - splitElementIndex - 1);

            if (!space.isFixedKey()) {
                if (newSiblingCommonPrefix.getLength() > oldSiblingCommonPrefix.getLength()) {
                    sibling.elements.setCommonPrefix(newSiblingCommonPrefix);
                    sibling.elements.truncatePrefix(newSiblingCommonPrefix.getLength() - oldSiblingCommonPrefix.getLength());
                }
            }
        } else {
            int lastElementLength;
            if (fixedKey)
                lastElementLength = maxKeySize + valueSize;
            else
                lastElementLength = lastChildKey.getLength() + valueSize + 2;

            int splitElementIndex = getRemoveSplitElementIndex(sibling.elements, siblingRegion, (double) (siblingDataSize - dataSize) / 2, siblingDataSize,
                    siblingElementCount, dataSize - lastElementLength, elementCount - 1, false);

            if (splitElementIndex == -1)
                return false;

            ByteArray prefixDelta = ByteArray.EMPTY;
            int prefixDeltaLength = 0;
            ByteArray oldSiblingCommonPrefix = ByteArray.EMPTY;
            ByteArray newSiblingCommonPrefix = ByteArray.EMPTY;
            ByteArray newCommonPrefix = ByteArray.EMPTY;
            boolean canRedistribute = false;
            for (; splitElementIndex >= 0; splitElementIndex--) {
                ByteArray oldKey = lastChildKey;
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

                    newCommonPrefix = BTreeIndexes.getCommonPrefix(bounds.getKey(), splitKey.value).clone();
                    newSiblingCommonPrefix = BTreeIndexes.getCommonPrefix(splitKey.value, siblingBounds.getValue()).clone();

                    prefixDeltaLength = oldSiblingCommonPrefix.getLength() - newCommonPrefix.getLength();

                    if (elementCount * (oldCommonPrefix.getLength() - newCommonPrefix.getLength()) +
                            prefixDeltaLength * (splitElementIndex + 1) +
                            sibling.elements.getDataSize(0, splitElementIndex + 1) +
                            (lastChildKey.getLength() - newCommonPrefix.getLength()) > elements.getFreeSpace())
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

            elements.copy(sibling.elements, 0, null, elementCount, new Pair<ByteArray, Long>(lastChildKey.subArray(newCommonPrefix.getLength()),
                    getLastChildPageIndex()), splitElementIndex + 1, prefixDeltaLength, prefixDelta);
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
        BTreeParentNode sibling = (BTreeParentNode) getSibling(parent, nodeElementIndex);
        ByteArray prefixDelta = ByteArray.EMPTY;
        int commonPrefixLength = 0;
        int newCommonPrefixLength = 0;
        if (!space.isFixedKey()) {
            ByteArray commonPrefix = elements.getCommonPrefix().clone();
            commonPrefixLength = commonPrefix.getLength();
            ByteArray siblingCommonPrefix = sibling.elements.getCommonPrefix().clone();
            ByteArray newCommonPrefix = BTreeIndexes.getCommonPrefix(commonPrefix, siblingCommonPrefix);
            newCommonPrefixLength = newCommonPrefix.getLength();

            if (newCommonPrefix.getLength() != siblingCommonPrefix.getLength()) {
                sibling.elements.setCommonPrefix(newCommonPrefix);
                sibling.elements.growPrefix(siblingCommonPrefix.subArray(newCommonPrefix.getLength()));
            }

            prefixDelta = commonPrefix.subArray(newCommonPrefix.getLength());
        }

        int elementCount = offsets.getCount();
        int siblingElementCount = sibling.offsets.getCount();

        ByteArray lastChildKey;
        if (nodeElementIndex > 0)
            lastChildKey = parent.getKey(nodeElementIndex - 1);
        else
            lastChildKey = parent.getKey(nodeElementIndex);

        if (nodeElementIndex > 0)
            sibling.elements.copy(elements, 0, null, siblingElementCount, new Pair<ByteArray, Long>(lastChildKey.subArray(newCommonPrefixLength),
                    sibling.getLastChildPageIndex()), elementCount, prefixDelta.getLength(), prefixDelta);
        else
            sibling.elements.copy(elements, 0, new Pair<ByteArray, Long>(lastChildKey.subArray(commonPrefixLength),
                    getLastChildPageIndex()), 0, null, elementCount, prefixDelta.getLength(), prefixDelta);

        space.freePage(page);
        space.updateParentNodeCount(-1);
    }

    private interface IMessages {
        @DefaultMessage("Invalid format of file ''{0}''.")
        ILocalizedMessage invalidFormat(int fileIndex);

        @DefaultMessage("Unsupported version ''{1}'' of file ''{0}'', expected version - ''{2}''.")
        ILocalizedMessage unsupportedVersion(int fileIndex, int fileVersion, int expectedVersion);
    }
}