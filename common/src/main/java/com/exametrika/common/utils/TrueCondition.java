/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;

/**
 * The {@link TrueCondition} is true condition.
 *
 * @param <T> condition type
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class TrueCondition<T> implements ICondition<T> {
    @Override
    public boolean evaluate(T value) {
        return true;
    }
}
