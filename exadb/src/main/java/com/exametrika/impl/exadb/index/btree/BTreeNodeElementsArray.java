/**
 * Copyright 2013 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.index.btree;

import java.util.Arrays;
import java.util.Comparator;

import com.exametrika.common.rawdb.IRawPage;
import com.exametrika.common.rawdb.IRawReadRegion;
import com.exametrika.common.rawdb.IRawWriteRegion;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;


/**
 * The {@link BTreeIndexSpace} is an elements array of B+ tree node.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public abstract class BTreeNodeElementsArray {
    protected static final int DATA_SIZE_OFFSET = 2;
    protected static final int ELEMENTS_START_OFFSET = 4;
    protected final BTreeIndexSpace space;
    protected final BTreeNodeOffsetVector offsets;
    protected final IRawPage page;

    public BTreeNodeElementsArray(BTreeIndexSpace space, BTreeNodeOffsetVector offsets, IRawPage page) {
        Assert.notNull(space);
        Assert.notNull(offsets);
        Assert.notNull(page);

        this.space = space;
        this.offsets = offsets;
        this.page = page;
    }

    public int getStart() {
        return page.getReadRegion().readShort(ELEMENTS_START_OFFSET);
    }

    public int getDataSize() {
        return page.getReadRegion().readShort(DATA_SIZE_OFFSET);
    }

    public int getFreeSpace() {
        IRawReadRegion region = page.getReadRegion();
        return page.getSize() - BTreeNode.HEADER_SIZE - offsets.getSize() - region.readShort(DATA_SIZE_OFFSET);
    }

    public void create() {
        IRawWriteRegion region = page.getWriteRegion();
        region.writeShort(DATA_SIZE_OFFSET, (short) 0);
        region.writeShort(ELEMENTS_START_OFFSET, (short) page.getSize());
    }

    public ByteArray getKey(int index) {
        int offset = offsets.getOffset(index);
        IRawReadRegion region = page.getReadRegion();

        int keyOffset;
        int keyLength;
        if (space.isFixedKey()) {
            keyOffset = offset;
            keyLength = space.getMaxKeySize();
        } else {
            keyOffset = offset + 2;
            keyLength = region.readShort(offset);
        }

        return region.readByteArray(keyOffset, keyLength);
    }

    public int getElementLength(IRawReadRegion region, int elementOffset) {
        int currentElementLength;
        if (!space.isFixedKey())
            currentElementLength = region.readShort(elementOffset) + 2;
        else
            currentElementLength = space.getMaxKeySize();

        if (!isFixedValue())
            currentElementLength += region.readShort(elementOffset + currentElementLength) + 2;
        else
            currentElementLength += getMaxValueSize();

        return currentElementLength;
    }

    public ByteArray getCommonPrefix() {
        if (space.isFixedKey())
            return ByteArray.EMPTY;

        int offset = offsets.getCommonPrefixOffset();
        if (offset == 0)
            return ByteArray.EMPTY;

        int length = offsets.getCommonPrefixLength();
        IRawReadRegion region = page.getReadRegion();
        return region.readByteArray(offset, length);
    }

    public void setCommonPrefix(ByteArray prefix) {
        Assert.checkState(!space.isFixedKey());
        int length = offsets.getCommonPrefixLength();
        IRawWriteRegion region = page.getWriteRegion();

        offsets.setCommonPrefixOffset(0);

        int dataSize = region.readShort(DATA_SIZE_OFFSET) - length + prefix.getLength();
        Assert.checkState(BTreeNode.HEADER_SIZE + offsets.getSize() + dataSize <= page.getSize());
        region.writeShort(DATA_SIZE_OFFSET, (short) dataSize);
        if (this instanceof BTreeLeafNodeElementsArray)
            space.updateLeafDataSize(prefix.getLength() - length);
        else
            space.updateParentDataSize(prefix.getLength() - length);

        int offset;
        if (!prefix.isEmpty()) {
            offset = allocate(prefix.getLength(), offsets.getCount(), false);
            region.writeByteArray(offset, prefix);
        } else
            offset = 0;

        offsets.setCommonPrefixLength(prefix.getLength());
        offsets.setCommonPrefixOffset(offset);
    }

    public void truncatePrefix(int lengthDelta) {
        Assert.checkState(!space.isFixedKey());

        IRawWriteRegion region = page.getWriteRegion();
        int count = offsets.getCount();

        for (int i = 0; i < count; i++) {
            ByteArray key = getKey(i).subArray(lengthDelta);
            int keyDigest = BTreeIndexes.getKeyDigest(key);
            int offset = offsets.getOffset(i);
            int keyLength = region.readShort(offset) - lengthDelta;
            offset += lengthDelta;
            region.writeShort(offset, (short) keyLength);

            offsets.set(i, offset, keyDigest);
        }

        int dataSize = region.readShort(DATA_SIZE_OFFSET);
        region.writeShort(DATA_SIZE_OFFSET, (short) (dataSize - count * lengthDelta));
        if (this instanceof BTreeLeafNodeElementsArray)
            space.updateLeafDataSize(-count * lengthDelta);
        else
            space.updateParentDataSize(-count * lengthDelta);
    }

    public boolean canAdd(int keySize, int valueSize) {
        return BTreeNode.HEADER_SIZE + offsets.getSize() + BTreeNodeOffsetVector.OFFSET_ELEMENT_SIZE + getDataSize() +
                getElementLength(keySize, valueSize) <= page.getSize();
    }

    public int binarySearch(int fromIndex, int toIndex, ByteArray key) {
        int keyDigest = BTreeIndexes.getKeyDigest(key);

        int low = fromIndex;
        int high = toIndex - 1;

        while (low <= high) {
            int middle = (low + high) >>> 1;
            int res = compare(key, keyDigest, middle);
            if (res < 0)
                low = middle + 1;
            else if (res > 0)
                high = middle - 1;
            else
                return middle;
        }
        return -(low + 1);
    }

    public int allocateElement(ByteArray key, int valueSize, int elementCount, boolean add) {
        IRawWriteRegion region = page.getWriteRegion();

        int keyLength = key.getLength();
        int elementLength = getElementLength(key.getLength(), valueSize);
        int elementOffset = allocate(elementLength, elementCount, add);

        int keyOffset;
        if (!space.isFixedKey()) {
            region.writeShort(elementOffset, (short) keyLength);
            keyOffset = elementOffset + 2;
        } else
            keyOffset = elementOffset;

        region.writeByteArray(keyOffset, key);
        return elementOffset;
    }

    public int allocate(int length, int elementCount, boolean add) {
        IRawWriteRegion region = page.getWriteRegion();

        int incrementCount = add ? 1 : 0;
        int offset = region.readShort(ELEMENTS_START_OFFSET) - length;
        if (offset < BTreeNode.HEADER_SIZE + (elementCount + incrementCount) * BTreeNodeOffsetVector.OFFSET_ELEMENT_SIZE)
            offset = compact(elementCount, length);
        else
            region.writeShort(ELEMENTS_START_OFFSET, (short) offset);

        Assert.checkState(offset >= BTreeNode.HEADER_SIZE + (elementCount + incrementCount) * BTreeNodeOffsetVector.OFFSET_ELEMENT_SIZE);

        return offset;
    }

    public int compact(int elementCount, int elementLengh) {
        IRawWriteRegion region = page.getWriteRegion();
        ElementInfo[] elements = new ElementInfo[elementCount + 1];
        for (int i = 0; i < elementCount; i++) {
            ElementInfo element = new ElementInfo();
            element.index = i;
            element.offset = offsets.getOffset(i);
            elements[i] = element;
        }

        ElementInfo element = new ElementInfo();
        element.index = -1;
        element.offset = offsets.getCommonPrefixOffset();
        elements[elementCount] = element;

        Arrays.sort(elements, new Comparator<ElementInfo>() {
            @Override
            public int compare(ElementInfo o1, ElementInfo o2) {
                if (o1.offset < o2.offset)
                    return 1;
                else if (o1.offset == o2.offset)
                    return 0;
                else
                    return -1;
            }
        });

        int lastOffset = page.getSize();
        int count = elements.length;
        for (int i = 0; i < count; i++) {
            int elementOffset = elements[i].offset;
            if (elementOffset == 0)
                continue;

            boolean commonPrefix = elements[i].index == -1;

            int currentElementLength;
            if (!commonPrefix)
                currentElementLength = getElementLength(region, elementOffset);
            else
                currentElementLength = offsets.getCommonPrefixLength();

            lastOffset -= currentElementLength;

            if (elementOffset < lastOffset) {
                if (commonPrefix) {
                    region.copy(elementOffset, lastOffset, currentElementLength);
                    offsets.setCommonPrefixOffset(lastOffset);
                } else if (space.isFixedKey() && isFixedValue()) {
                    if (elements[count - 1].offset != 0) {
                        region.copy(elements[count - 1].offset, lastOffset, currentElementLength);
                        int index = elements[count - 1].index;
                        offsets.set(index, lastOffset, offsets.getKeyDigest(index));
                    } else
                        lastOffset += currentElementLength;

                    count--;
                    i--;
                } else {
                    region.copy(elementOffset, lastOffset, currentElementLength);
                    int index = elements[i].index;
                    offsets.set(index, lastOffset, offsets.getKeyDigest(index));
                }
            }
        }

        lastOffset -= elementLengh;
        region.writeShort(ELEMENTS_START_OFFSET, (short) lastOffset);
        return lastOffset;
    }

    protected abstract boolean isFixedValue();

    protected abstract int getMaxValueSize();

    protected int appendSorted(ByteArray key, int valueSize) {
        Assert.isTrue(!space.isFixedKey() || key.getLength() == space.getMaxKeySize());

        int keyLength = key.getLength();
        IRawWriteRegion region = page.getWriteRegion();
        int dataSize = region.readShort(DATA_SIZE_OFFSET);
        int elementCount = offsets.getCount();

        int elementLength = getElementLength(key.getLength(), valueSize);

        Assert.checkState(BTreeNode.HEADER_SIZE + (elementCount + 1) * BTreeNodeOffsetVector.OFFSET_ELEMENT_SIZE +
                dataSize + elementLength <= page.getSize());

        int elementOffset = region.readShort(ELEMENTS_START_OFFSET) - elementLength;
        Assert.checkState(elementOffset >= BTreeNode.HEADER_SIZE + elementCount * BTreeNodeOffsetVector.OFFSET_ELEMENT_SIZE);

        int keyOffset;
        if (!space.isFixedKey()) {
            region.writeShort(elementOffset, (short) keyLength);
            keyOffset = elementOffset + 2;
        } else
            keyOffset = elementOffset;

        region.writeByteArray(keyOffset, key);

        offsets.add(elementOffset, BTreeIndexes.getKeyDigest(key));
        region.writeShort(DATA_SIZE_OFFSET, (short) (dataSize + elementLength));
        if (this instanceof BTreeLeafNodeElementsArray)
            space.updateLeafDataSize(elementLength + BTreeNodeOffsetVector.OFFSET_ELEMENT_SIZE);
        else
            space.updateParentDataSize(elementLength + BTreeNodeOffsetVector.OFFSET_ELEMENT_SIZE);
        region.writeShort(ELEMENTS_START_OFFSET, (short) (elementOffset));

        if (isFixedValue())
            return keyOffset + keyLength;
        else {
            region.writeShort(keyOffset + keyLength, (short) valueSize);
            return keyOffset + keyLength + 2;
        }
    }

    protected int getElementLength(int keySize, int valueSize) {
        int elementLength;
        if (!space.isFixedKey())
            elementLength = keySize + 2;
        else
            elementLength = space.getMaxKeySize();

        if (!isFixedValue())
            elementLength += valueSize + 2;
        else
            elementLength += getMaxValueSize();

        return elementLength;
    }

    private int compare(ByteArray key, int keyDigest, int index) {
        int elementKeyDigest = offsets.getKeyDigest(index);

        if (elementKeyDigest < keyDigest)
            return -1;
        else if (elementKeyDigest > keyDigest)
            return 1;
        else
            return getKey(index).compareTo(key);
    }

    private static class ElementInfo {
        private int index;
        private int offset;
    }
}