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
 * The {@link BTreeIndexSpace} is an elements array of B+ tree leaf node.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class BTreeLeafNodeElementsArray extends BTreeNodeElementsArray {
    public BTreeLeafNodeElementsArray(BTreeIndexSpace space, BTreeNodeOffsetVector offsets, IRawPage page) {
        super(space, offsets, page);
    }

    public ByteArray getValue(int index) {
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

        if (space.isFixedValue())
            return region.readByteArray(keyOffset + keyLength, space.getMaxValueSize());
        else {
            int valueLength = region.readShort(keyOffset + keyLength);
            return region.readByteArray(keyOffset + keyLength + 2, valueLength);
        }
    }

    public <T extends IRawReadRegion> T getValueRegion(int index, boolean readOnly) {
        int offset = offsets.getOffset(index);

        IRawReadRegion region;
        if (readOnly)
            region = page.getReadRegion();
        else
            region = page.getWriteRegion();

        int keyOffset;
        int keyLength;
        if (space.isFixedKey()) {
            keyOffset = offset;
            keyLength = space.getMaxKeySize();
        } else {
            keyOffset = offset + 2;
            keyLength = region.readShort(offset);
        }

        if (space.isFixedValue())
            return region.getRegion(keyOffset + keyLength, space.getMaxValueSize());
        else {
            int valueLength = region.readShort(keyOffset + keyLength);
            return region.getRegion(keyOffset + keyLength + 2, valueLength);
        }
    }

    public Pair<ByteArray, ByteArray> getElement(int index) {
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
        ByteArray value;
        if (space.isFixedValue())
            value = region.readByteArray(keyOffset + keyLength, space.getMaxValueSize());
        else {
            int valueLength = region.readShort(keyOffset + keyLength);
            value = region.readByteArray(keyOffset + keyLength + 2, valueLength);
        }
        return new Pair<ByteArray, ByteArray>(key, value);
    }

    public int getDataSize(int index, int count) {
        Assert.checkState(!space.isFixedKey());

        IRawReadRegion region = page.getReadRegion();

        int dataSize = 0;
        for (int i = index; i < count; i++) {
            int offset = offsets.getOffset(index);
            if (offset == 0)
                continue;

            dataSize += getElementLength(region, offset) + BTreeNodeOffsetVector.OFFSET_ELEMENT_SIZE;
        }

        return dataSize;
    }

    public boolean add(ByteArray key, ByteArray value, boolean bulk) {
        if (!canAdd(key.getLength(), value.getLength()))
            return false;

        IRawWriteRegion region = page.getWriteRegion();

        int elementCount = offsets.getCount();
        int elementLength = getElementLength(key.getLength(), value.getLength());
        int elementSizeDelta = elementLength;

        int elementIndex;
        boolean replace = false;
        if (!bulk) {
            elementIndex = binarySearch(0, elementCount, key);
            if (elementIndex >= 0) {
                replace = true;
                int oldElementLength = getElementLength(region, offsets.getOffset(elementIndex));
                elementSizeDelta = elementLength - oldElementLength;
                offsets.set(elementIndex, 0, 0);
            } else
                elementIndex = -elementIndex - 1;
        } else
            elementIndex = elementCount;

        int elementOffset = allocateElement(key, value.getLength(), elementCount, !replace);
        int valueOffset;
        if (!space.isFixedValue()) {
            valueOffset = elementOffset + elementLength - value.getLength();
            region.writeShort(valueOffset - 2, (short) value.getLength());
        } else
            valueOffset = elementOffset + elementLength - space.getMaxValueSize();

        region.writeByteArray(valueOffset, value);

        int dataSize = region.readShort(DATA_SIZE_OFFSET);
        if (!replace) {
            offsets.insert(elementIndex, elementOffset, BTreeIndexes.getKeyDigest(key));
            region.writeShort(DATA_SIZE_OFFSET, (short) (dataSize + elementLength));
            space.updateLeafDataSize(elementLength + BTreeNodeOffsetVector.OFFSET_ELEMENT_SIZE);
            space.updateLeafElementCount(1);
        } else {
            offsets.set(elementIndex, elementOffset, BTreeIndexes.getKeyDigest(key));
            region.writeShort(DATA_SIZE_OFFSET, (short) (dataSize + elementSizeDelta));
            space.updateLeafDataSize(elementSizeDelta);
        }

        return true;
    }

    public void appendSorted(ByteArray key, ByteArray value) {
        int valueOffset = appendSorted(key, value.getLength());
        IRawWriteRegion region = page.getWriteRegion();
        region.writeByteArray(valueOffset, value);
    }

    public boolean remove(ByteArray key) {
        int elementCount = offsets.getCount();
        int elementIndex = binarySearch(0, elementCount, key);
        if (elementIndex < 0)
            return false;

        onRemoved(elementIndex);

        remove(elementIndex, 1);
        return true;
    }

    public void remove(int index, int count) {
        if (count == 0)
            return;

        IRawWriteRegion region = page.getWriteRegion();
        int dataSize = region.readShort(DATA_SIZE_OFFSET);
        int oldDataSize = dataSize;

        if (space.isFixedKey() && space.isFixedValue())
            dataSize -= count * (space.getMaxKeySize() + space.getMaxValueSize());
        else {
            for (int i = 0; i < count; i++) {
                int elementOffset = offsets.getOffset(index + i);
                dataSize -= getElementLength(region, elementOffset);
            }
        }

        region.writeShort(DATA_SIZE_OFFSET, (short) dataSize);
        space.updateLeafDataSize(dataSize - oldDataSize - count * BTreeNodeOffsetVector.OFFSET_ELEMENT_SIZE);

        offsets.remove(index, count);
    }

    public void copy(BTreeLeafNodeElementsArray source, int sourceIndex, int index, int count, int prefixDeltaLength,
                     ByteArray prefixDelta) {
        if (count == 0)
            return;

        boolean fixedKey = space.isFixedKey();

        IRawWriteRegion region = page.getWriteRegion();

        int dataSize = region.readShort(DATA_SIZE_OFFSET);
        int oldDataSize = dataSize;
        int elementCount = offsets.getCount();

        if (index == 0) {
            int elementsStartOffset = region.readShort(ELEMENTS_START_OFFSET);
            if (BTreeNode.HEADER_SIZE + (elementCount + count) * BTreeNodeOffsetVector.OFFSET_ELEMENT_SIZE > elementsStartOffset)
                compact(elementCount, 0);

            offsets.expand(index, count);
            elementCount += count;
        }

        for (int i = 0; i < count; i++) {
            Pair<ByteArray, ByteArray> pair = source.getElement(i + sourceIndex);

            int elementLength = getElementLength(pair.getKey().getLength(), pair.getValue().getLength()) + prefixDeltaLength;
            ByteArray newKey;
            if (!fixedKey) {
                if (prefixDeltaLength >= 0)
                    newKey = Bytes.combine(prefixDelta, pair.getKey());
                else
                    newKey = pair.getKey().subArray(-prefixDeltaLength);
            } else
                newKey = pair.getKey();

            int elementOffset = allocateElement(newKey, pair.getValue().getLength(), elementCount, index != 0);
            int valueOffset;
            if (!space.isFixedValue()) {
                valueOffset = elementOffset + elementLength - pair.getValue().getLength();
                region.writeShort(valueOffset - 2, (short) pair.getValue().getLength());
            } else
                valueOffset = elementOffset + elementLength - space.getMaxValueSize();

            region.writeByteArray(valueOffset, pair.getValue());
            offsets.set(i + index, elementOffset, BTreeIndexes.getKeyDigest(newKey));
            dataSize += elementLength;

            if (index != 0)
                elementCount++;
        }

        if (index != 0)
            offsets.setCount(elementCount);

        Assert.checkState(BTreeNode.HEADER_SIZE + offsets.getSize() + dataSize <= page.getSize());
        region.writeShort(DATA_SIZE_OFFSET, (short) dataSize);
        space.updateLeafDataSize(dataSize - oldDataSize + count * BTreeNodeOffsetVector.OFFSET_ELEMENT_SIZE);
    }

    public void growPrefix(ByteArray prefix) {
        Assert.checkState(!space.isFixedKey());

        IRawWriteRegion region = page.getWriteRegion();
        int dataSize = region.readShort(DATA_SIZE_OFFSET);
        int count = offsets.getCount();

        Assert.checkState(BTreeNode.HEADER_SIZE + offsets.getSize() + dataSize + count * prefix.getLength() <= page.getSize());

        for (int i = 0; i < count; i++) {
            Pair<ByteArray, ByteArray> element = getElement(i);
            ByteArray key = Bytes.combine(prefix, element.getKey(), true);
            ByteArray value = element.getValue().clone();

            int elementLength = getElementLength(key.getLength(), value.getLength());
            offsets.set(i, 0, 0);

            int elementOffset = allocateElement(key, value.getLength(), count, false);
            int valueOffset;
            if (!space.isFixedValue()) {
                valueOffset = elementOffset + elementLength - value.getLength();
                region.writeShort(valueOffset - 2, (short) value.getLength());
            } else
                valueOffset = elementOffset + elementLength - space.getMaxValueSize();

            region.writeByteArray(valueOffset, value);
            offsets.set(i, elementOffset, BTreeIndexes.getKeyDigest(key));
        }

        region.writeShort(DATA_SIZE_OFFSET, (short) (dataSize + count * prefix.getLength()));
        space.updateLeafDataSize(count * prefix.getLength());
    }

    @Override
    public String toString() {
        ByteArray commonPrefix = getCommonPrefix();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < offsets.getCount(); i++) {
            builder.append("\n");
            Pair<ByteArray, ByteArray> element = getElement(i);
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

        return builder.toString();
    }

    @Override
    protected boolean isFixedValue() {
        return space.isFixedValue();
    }

    @Override
    protected int getMaxValueSize() {
        return space.getMaxValueSize();
    }

    private void onRemoved(int elementIndex) {
        if (space.getListener() != null)
            space.getListener().onRemoved(space.getValueConverter().toValue(getValue(elementIndex)));
    }
}