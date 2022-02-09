/**
 * Copyright 2013 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.index.btree;

import com.exametrika.common.rawdb.IRawPage;
import com.exametrika.common.rawdb.IRawReadRegion;
import com.exametrika.common.rawdb.IRawWriteRegion;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Bytes;
import com.exametrika.common.utils.Pair;
import com.exametrika.common.utils.Strings;


/**
 * The {@link BTreeIndexSpace} is an elements array of B+ tree [arent node.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class BTreeParentNodeElementsArray extends BTreeNodeElementsArray {
    public static final int VALUE_SIZE = 8;
    private static final int LAST_CHILD_PAGE_INDEX_OFFSET = 6;

    public BTreeParentNodeElementsArray(BTreeIndexSpace space, BTreeNodeOffsetVector offsets, IRawPage page) {
        super(space, offsets, page);
    }

    public long getLastChildPageIndex() {
        return page.getReadRegion().readLong(LAST_CHILD_PAGE_INDEX_OFFSET);
    }

    public void setLastChildPageIndex(long pageIndex) {
        page.getWriteRegion().writeLong(LAST_CHILD_PAGE_INDEX_OFFSET, pageIndex);
    }

    @Override
    public void create() {
        super.create();

        page.getWriteRegion().writeLong(LAST_CHILD_PAGE_INDEX_OFFSET, 0);
    }

    public int getValueOffset(int index) {
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

        return keyOffset + keyLength;
    }

    public long getValue(int index) {
        int offset = getValueOffset(index);
        IRawReadRegion region = page.getReadRegion();
        return region.readLong(offset);
    }

    public Pair<ByteArray, Long> getElement(int index) {
        int offset = offsets.getOffset(index);
        if (offset == 0)
            return null;

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

        ByteArray key = region.readByteArray(keyOffset, keyLength);
        long value = region.readLong(keyOffset + keyLength);
        return new Pair<ByteArray, Long>(key, value);
    }

    public int getDataSize(int index, int count) {
        Assert.checkState(!space.isFixedKey());

        IRawReadRegion region = page.getReadRegion();

        int dataSize = 0;
        for (int i = index; i < count; i++) {
            int offset = offsets.getOffset(index);
            if (offset == 0)
                continue;

            dataSize += region.readShort(offset) + VALUE_SIZE + 2 + BTreeNodeOffsetVector.OFFSET_ELEMENT_SIZE;
        }

        return dataSize;
    }

    public boolean add(int index, ByteArray childSplitKey, long firstChildPageIndex, long secondChildPageIndex) {
        if (!canAdd(childSplitKey.getLength(), VALUE_SIZE))
            return false;

        boolean fixedKey = space.isFixedKey();
        int maxKeySize = space.getMaxKeySize();

        int keyLength = childSplitKey.getLength();
        IRawWriteRegion region = page.getWriteRegion();
        int dataSize = region.readShort(DATA_SIZE_OFFSET);
        int elementCount = offsets.getCount();

        int elementLength;
        if (!fixedKey)
            elementLength = keyLength + VALUE_SIZE + 2;
        else
            elementLength = maxKeySize + VALUE_SIZE;

        int elementOffset = allocateElement(childSplitKey, VALUE_SIZE, elementCount, true);
        region.writeLong(elementOffset + elementLength - VALUE_SIZE, firstChildPageIndex);

        region.writeShort(DATA_SIZE_OFFSET, (short) (dataSize + elementLength));
        space.updateParentDataSize(elementLength + BTreeNodeOffsetVector.OFFSET_ELEMENT_SIZE);

        if (index < elementCount) {
            offsets.insert(index, elementOffset, BTreeIndexes.getKeyDigest(childSplitKey));

            int valueOffset = getValueOffset(index + 1);
            region.writeLong(valueOffset, secondChildPageIndex);
        } else {
            offsets.add(elementOffset, BTreeIndexes.getKeyDigest(childSplitKey));
            region.writeLong(LAST_CHILD_PAGE_INDEX_OFFSET, secondChildPageIndex);
        }

        return true;
    }

    public void appendSorted(ByteArray key, long childPageIndex) {
        int valueOffset = appendSorted(key, VALUE_SIZE);
        IRawWriteRegion region = page.getWriteRegion();
        region.writeLong(valueOffset, childPageIndex);
    }

    public void set(int index, ByteArray newKey) {
        IRawWriteRegion region = page.getWriteRegion();
        if (space.isFixedKey()) {
            int elementOffset = offsets.getOffset(index);
            region.writeByteArray(elementOffset, newKey);
            offsets.set(index, elementOffset, BTreeIndexes.getKeyDigest(newKey));
            return;
        }

        Pair<ByteArray, Long> element = getElement(index);
        int elementLength = newKey.getLength() + VALUE_SIZE + 2;
        int oldElementLength = element.getKey().getLength() + VALUE_SIZE + 2;

        int dataSize = region.readShort(DATA_SIZE_OFFSET);
        int elementCount = offsets.getCount();

        Assert.checkState(BTreeNode.HEADER_SIZE + offsets.getSize() + dataSize + elementLength - oldElementLength <= page.getSize());

        offsets.set(index, 0, 0);

        int elementOffset = allocateElement(newKey, VALUE_SIZE, elementCount, false);
        region.writeLong(elementOffset + elementLength - VALUE_SIZE, element.getValue());

        offsets.set(index, elementOffset, BTreeIndexes.getKeyDigest(newKey));
        region.writeShort(DATA_SIZE_OFFSET, (short) (dataSize + elementLength - oldElementLength));
        space.updateParentDataSize(elementLength - oldElementLength);
    }

    public void copy(BTreeParentNodeElementsArray source, int sourceIndex, Pair<ByteArray, Long> sourceLastChild,
                     int index, Pair<ByteArray, Long> lastChild, int count, int prefixDeltaLength, ByteArray prefixDelta) {
        if (count == 0)
            return;

        IRawWriteRegion region = page.getWriteRegion();

        int dataSize = region.readShort(DATA_SIZE_OFFSET);
        int oldDataSize = dataSize;
        int elementCount = offsets.getCount();
        int sourceElementCount = source.offsets.getCount();
        int sourceLastIndex = sourceIndex + count - 1;
        if (index == 0)
            count++;
        boolean copyLastChild = false;
        if (index != 0 && sourceIndex + count == sourceElementCount) {
            count++;
            copyLastChild = true;
        }

        if (index == 0) {
            int elementsStartOffset = region.readShort(ELEMENTS_START_OFFSET);
            if (BTreeNode.HEADER_SIZE + (elementCount + count) * BTreeNodeOffsetVector.OFFSET_ELEMENT_SIZE > elementsStartOffset)
                compact(elementCount, 0);

            offsets.expand(index, count);
            elementCount += count;
        }

        for (int i = 0; i < count; i++) {
            Pair<ByteArray, Long> pair;
            if (index == 0 && i == count - 1)
                pair = sourceLastChild;
            else if (index != 0 && i == 0) {
                pair = lastChild;
                sourceIndex--;
            } else
                pair = source.getElement(i + sourceIndex);

            int elementLength;
            ByteArray newKey;
            if (!space.isFixedKey()) {
                if (pair != lastChild) {
                    elementLength = pair.getKey().getLength() + VALUE_SIZE + 2 + prefixDeltaLength;

                    if (prefixDeltaLength >= 0)
                        newKey = Bytes.combine(prefixDelta, pair.getKey());
                    else
                        newKey = pair.getKey().subArray(-prefixDeltaLength);
                } else {
                    elementLength = pair.getKey().getLength() + VALUE_SIZE + 2;
                    newKey = pair.getKey();
                }
            } else {
                elementLength = space.getMaxKeySize() + VALUE_SIZE;
                newKey = pair.getKey();
            }

            int elementOffset = allocateElement(newKey, VALUE_SIZE, elementCount, index != 0);
            region.writeLong(elementOffset + elementLength - VALUE_SIZE, pair.getValue());
            offsets.set(i + index, elementOffset, BTreeIndexes.getKeyDigest(newKey));
            dataSize += elementLength;

            if (index != 0)
                elementCount++;
        }

        if (index != 0) {
            if (copyLastChild)
                region.writeLong(LAST_CHILD_PAGE_INDEX_OFFSET, source.getLastChildPageIndex());
            else {
                Pair<ByteArray, Long> pair = source.getElement(sourceLastIndex);
                region.writeLong(LAST_CHILD_PAGE_INDEX_OFFSET, pair.getValue());
            }

            offsets.setCount(elementCount);
        }

        Assert.checkState(BTreeNode.HEADER_SIZE + offsets.getSize() + dataSize <= page.getSize());
        region.writeShort(DATA_SIZE_OFFSET, (short) dataSize);
        space.updateParentDataSize(dataSize - oldDataSize + count * BTreeNodeOffsetVector.OFFSET_ELEMENT_SIZE);
    }

    public void remove(int index) {
        IRawWriteRegion region = page.getWriteRegion();
        int elementCount = offsets.getCount();
        int dataSize = region.readShort(DATA_SIZE_OFFSET);
        boolean fixedKey = space.isFixedKey();
        int maxKeySize = space.getMaxKeySize();

        if (index < elementCount) {
            if (index > 0) {
                int valueOffset = getValueOffset(index - 1);
                long pageIndex = region.readLong(valueOffset);
                valueOffset = getValueOffset(index);
                region.writeLong(valueOffset, pageIndex);
                index--;
            }

            int elementLength;
            if (!fixedKey) {
                int elementOffset = offsets.getOffset(index);
                int keyLength = region.readShort(elementOffset);
                elementLength = keyLength + VALUE_SIZE + 2;
            } else
                elementLength = maxKeySize + VALUE_SIZE;

            region.writeShort(DATA_SIZE_OFFSET, (short) (dataSize - elementLength));
            space.updateParentDataSize(-elementLength - BTreeNodeOffsetVector.OFFSET_ELEMENT_SIZE);
            offsets.remove(index, 1);
        } else {
            Assert.checkState(elementCount > 0);
            int elementOffset = offsets.getOffset(index - 1);

            long pageIndex;
            int elementLength;
            if (!fixedKey) {
                int keyLength = region.readShort(elementOffset);
                elementLength = keyLength + VALUE_SIZE + 2;
                pageIndex = region.readLong(elementOffset + keyLength + 2);
            } else {
                elementLength = maxKeySize + VALUE_SIZE;
                pageIndex = region.readLong(elementOffset + maxKeySize);
            }

            region.writeShort(DATA_SIZE_OFFSET, (short) (dataSize - elementLength));
            space.updateParentDataSize(-elementLength - BTreeNodeOffsetVector.OFFSET_ELEMENT_SIZE);
            offsets.remove(elementCount - 1, 1);
            region.writeLong(LAST_CHILD_PAGE_INDEX_OFFSET, pageIndex);
        }
    }

    public void remove(int index, int count) {
        if (count == 0)
            return;

        IRawWriteRegion region = page.getWriteRegion();
        int elementCount = offsets.getCount();
        int dataSize = region.readShort(DATA_SIZE_OFFSET);
        int oldDataSize = dataSize;

        if (index + count == elementCount) {
            index--;
            count++;
        }

        if (space.isFixedKey())
            dataSize -= count * (space.getMaxKeySize() + VALUE_SIZE);
        else {
            for (int i = 0; i < count; i++) {
                int elementOffset = offsets.getOffset(index + i);
                int keyLength = region.readShort(elementOffset);
                dataSize -= keyLength + 2 + VALUE_SIZE;
            }
        }

        if (index + count == elementCount) {
            long value = getValue(index);
            region.writeLong(LAST_CHILD_PAGE_INDEX_OFFSET, value);
        }

        region.writeShort(DATA_SIZE_OFFSET, (short) dataSize);
        space.updateParentDataSize(dataSize - oldDataSize - count * BTreeNodeOffsetVector.OFFSET_ELEMENT_SIZE);
        offsets.remove(index, count);
    }

    public void growPrefix(ByteArray prefix) {
        Assert.checkState(!space.isFixedKey());

        IRawWriteRegion region = page.getWriteRegion();
        int dataSize = region.readShort(DATA_SIZE_OFFSET);
        int count = offsets.getCount();

        Assert.checkState(BTreeNode.HEADER_SIZE + offsets.getSize() + dataSize + count * prefix.getLength() <= page.getSize());

        for (int i = 0; i < count; i++) {
            Pair<ByteArray, Long> element = getElement(i);
            ByteArray key = Bytes.combine(prefix, element.getKey(), true);

            int elementLength = key.getLength() + VALUE_SIZE + 2;

            offsets.set(i, 0, 0);

            int elementOffset = allocateElement(key, VALUE_SIZE, count, false);
            region.writeLong(elementOffset + elementLength - VALUE_SIZE, element.getValue());

            offsets.set(i, elementOffset, BTreeIndexes.getKeyDigest(key));
        }

        region.writeShort(DATA_SIZE_OFFSET, (short) (dataSize + count * prefix.getLength()));
        space.updateParentDataSize(count * prefix.getLength());
    }

    @Override
    public String toString() {
        ByteArray commonPrefix = getCommonPrefix();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < offsets.getCount(); i++) {
            builder.append("\n");
            Pair<ByteArray, Long> element = getElement(i);
            if (element != null) {
                ByteArray key;
                if (commonPrefix != null)
                    key = Bytes.combine(commonPrefix, element.getKey());
                else
                    key = element.getKey();
                builder.append(Strings.indent(key.toString() + ":" + element.getValue().toString(), 4));
            } else
                builder.append(Strings.indent("(null)", 4));
        }

        builder.append("\n    last:" + getLastChildPageIndex());

        return builder.toString();
    }

    @Override
    protected boolean isFixedValue() {
        return true;
    }

    @Override
    protected int getMaxValueSize() {
        return VALUE_SIZE;
    }
}