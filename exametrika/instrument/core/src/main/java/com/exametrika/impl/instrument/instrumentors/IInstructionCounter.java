/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.instrument.instrumentors;


/**
 * The {@link IInstructionCounter} is used to count instructions from the beginning of the current method.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IInstructionCounter {
    /**
     * Returns number of instructions visited from the beginning of the current method.
     *
     * @return number of instructions visited from the beginning of the current method
     */
    int getCount();
}
