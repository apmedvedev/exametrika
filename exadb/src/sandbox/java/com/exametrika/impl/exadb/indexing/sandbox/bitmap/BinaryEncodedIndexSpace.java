/**
 * Copyright 2013 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.indexing.sandbox.bitmap;

import com.exametrika.api.exadb.index.IKeyNormalizer;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Numbers;
import com.exametrika.spi.exadb.core.IDataFileAllocator;
import com.exametrika.spi.exadb.core.ITransactionProvider;


/**
 * The {@link BinaryEncodedIndexSpace} is a space of binary encoded index.
 *
 * @param <K> key type
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class BinaryEncodedIndexSpace<K> {
    private final ITransactionProvider transactionProvider;
    private final IDataFileAllocator dataFileAllocator;
    private final int pathIndex;
    private final String filePrefix;
    private final IKeyNormalizer<K> keyNormalizer;
    private final boolean allowNulls;
    private final ByteArray nullKey;
    private final int keySize;
    private final BitVectorSpace[] bitVectors = new BitVectorSpace[64];
    private int count;

    public static BinaryEncodedIndexSpace create(ITransactionProvider transactionProvider, IDataFileAllocator dataFileAllocator,
                                                 int pathIndex, String filePrefix, IKeyNormalizer keyNormalizer, int keySize, boolean allowNulls) {
        return new BinaryEncodedIndexSpace(transactionProvider, dataFileAllocator, pathIndex, filePrefix,
                keyNormalizer, keySize, allowNulls);
    }

    public static BinaryEncodedIndexSpace open(ITransactionProvider transactionProvider, IDataFileAllocator dataFileAllocator,
                                               int pathIndex, String filePrefix, IKeyNormalizer keyNormalizer, int keySize, boolean allowNulls,
                                               int[] bitVectorsFileIndexes) {
        Assert.notNull(bitVectorsFileIndexes);

        BinaryEncodedIndexSpace space = new BinaryEncodedIndexSpace(transactionProvider, dataFileAllocator, pathIndex,
                filePrefix, keyNormalizer, keySize, allowNulls);

        space.count = bitVectorsFileIndexes.length;
        for (int i = 0; i < bitVectorsFileIndexes.length; i++)
            space.bitVectors[i] = BitVectorSpace.open(transactionProvider, pathIndex, bitVectorsFileIndexes[i],
                    filePrefix + "-bit" + i / 10 + i % 10);

        return space;
    }

    public void add(long index, K key) {
        ByteArray bitCode;
        if (key == null) {
            Assert.isTrue(allowNulls);
            bitCode = nullKey;
        } else
            bitCode = keyNormalizer.normalize(key);

        Assert.isTrue(bitCode.getLength() == keySize);

        boolean createBitVectors = false;
        for (int i = bitCode.getLength() - 1; i >= 0; i--) {
            int value = bitCode.get(i);
            for (int k = 7; k >= 0; k--) {
                boolean bitValue = (value & (1 << k)) != 0;
                if (!bitValue && !createBitVectors)
                    continue;

                createBitVectors = true;
                int bitIndex = i << 3 + k;
                if (bitVectors[bitIndex] == null) {
                    bitVectors[bitIndex] = BitVectorSpace.create(transactionProvider, pathIndex,
                            dataFileAllocator.allocateFile(transactionProvider.getRawTransaction()),
                            filePrefix + "-bit" + bitIndex / 10 + bitIndex % 10);
                }

                bitVectors[bitIndex].add(index, bitValue);
            }
        }
    }

    public void remove(long index) {
        for (int i = 0; i < count; i++)
            bitVectors[i].remove(index);
    }

    public void clear() {
        for (int i = 0; i < count; i++) {
            bitVectors[i].delete();
            bitVectors[i] = null;
        }

        count = 0;
    }

    public void delete() {
        clear();
    }

    private BinaryEncodedIndexSpace(ITransactionProvider transactionProvider, IDataFileAllocator dataFileAllocator,
                                    int pathIndex, String filePrefix, IKeyNormalizer keyNormalizer, int keySize, boolean allowNulls) {
        Assert.notNull(transactionProvider);
        Assert.notNull(dataFileAllocator);
        Assert.isTrue(Numbers.isPowerOfTwo(keySize) && keySize <= 8);
        Assert.notNull(keyNormalizer);

        this.transactionProvider = transactionProvider;
        this.dataFileAllocator = dataFileAllocator;
        this.pathIndex = pathIndex;
        this.filePrefix = filePrefix;
        this.keyNormalizer = keyNormalizer;
        this.keySize = keySize;
        this.allowNulls = allowNulls;

        byte[] buffer = new byte[keySize];
        buffer[0] = 1;
        this.nullKey = new ByteArray(buffer);
    }
}