/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb;

import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Pair;


/**
 * The {@link INodeSortedIndex} represents a sorted node index.
 *
 * @param <K> key type
 * @param <V> node type
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface INodeSortedIndex<K, V> extends INodeIndex<K, V> {
    /**
     * Finds first index element.
     *
     * @return index element as key:value pair or null if index is empty. Returned key is represented in normalized form.
     * Pair is valid until next index modification
     */
    Pair<ByteArray, V> findFirst();

    /**
     * Finds first index value.
     *
     * @return index value or null if index is empty. Value is valid until next index modification
     */
    V findFirstValue();

    /**
     * Finds last index element.
     *
     * @return index element as key:value pair or null if index is empty. Returned key is represented in normalized form.
     * Pair is valid until next index modification
     */
    Pair<ByteArray, V> findLast();

    /**
     * Finds last index value.
     *
     * @return index value or null if index is empty. Value is valid until next index modification
     */
    V findLastValue();

    /**
     * Finds index element whose key is greatest key less than or equal to specified key.
     *
     * @param key       key, if null last index key is used
     * @param inclusive if false finds exactly element with greatest key less than specified key
     * @return index element as key:value pair or null if value is not found. Returned key is represented in normalized form.
     * Pair is valid until next index modification
     */
    Pair<ByteArray, V> findFloor(K key, boolean inclusive);

    /**
     * Finds index value whose key is greatest key less than or equal to specified key.
     *
     * @param key       key, if null last index key is used
     * @param inclusive if false finds exactly value with greatest key less than specified key
     * @return index value or null if value is not found. Value is valid until next index modification
     */
    V findFloorValue(K key, boolean inclusive);

    /**
     * Finds index element whose key is least key greater than or equal to specified key.
     *
     * @param key       key, if null first index key is used
     * @param inclusive if false finds exactly element with least key greater than specified key
     * @return index element as key:value pair or null if element is not found. Returned key is represented in normalized form.
     * Pair is valid until next index modification
     */
    Pair<ByteArray, V> findCeiling(K key, boolean inclusive);

    /**
     * Finds index value whose key is least key greater than or equal to specified key.
     *
     * @param key       key, if null first index key is used
     * @param inclusive if false finds exactly value with least key greater than specified key
     * @return index value or null if value is not found. Value is valid until next index modification
     */
    V findCeilingValue(K key, boolean inclusive);

    /**
     * Finds range of index elements.
     *
     * @param fromKey       start bound of range. If null start bound of range is first index element.
     * @param fromInclusive if true start bound belongs to range, if false start bound does not belong to range
     * @param toKey         end bound of range. If null end bound of range is last index element.
     * @param toInclusive   if true end bound belongs to range, if false end bound does not belong to range
     * @return iterable of index elements as key:value pair which belong to given range. Returned key is represented in normalized form.
     * Iterable is valid until next index modification
     */
    Iterable<Pair<ByteArray, V>> find(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive);

    /**
     * Finds range of index values.
     *
     * @param fromKey       start bound of range. If null start bound of range is first index element.
     * @param fromInclusive if true start bound belongs to range, if false start bound does not belong to range
     * @param toKey         end bound of range. If null end bound of range is last index element.
     * @param toInclusive   if true end bound belongs to range, if false end bound does not belong to range
     * @return iterable of index values which belong to given range. Iterable is valid until next index modification
     */
    Iterable<V> findValues(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive);

    /**
     * Estimates number of elements within specified range. Key distribution statistics is used in estimation process.
     *
     * @param fromKey       start bound of range. If null start bound of range is first index element.
     * @param fromInclusive if true start bound belongs to range, if false start bound does not belong to range
     * @param toKey         end bound of range. If null end bound of range is last index element.
     * @param toInclusive   if true end bound belongs to range, if false end bound does not belong to range
     * @return estimated number of elements within specified range
     */
    long estimate(K fromKey, boolean fromInclusive, K toKey, boolean toInclusive);
}
