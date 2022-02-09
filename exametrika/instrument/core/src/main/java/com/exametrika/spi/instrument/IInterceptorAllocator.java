/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.instrument;

import com.exametrika.api.instrument.IJoinPoint;


/**
 * The {@link IInterceptorAllocator} represents an interceptor allocator.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IInterceptorAllocator {
    /**
     * Join point info.
     */
    class JoinPointInfo {
        /**
         * Index of joint point.
         */
        public final int index;

        /**
         * Version of joint point.
         */
        public final int version;

        public JoinPointInfo(int index, int version) {
            this.index = index;
            this.version = version;
        }
    }

    /**
     * Allocates new interceptor of specified class for specified join point.
     *
     * @param classLoader class loader. Can be null if class is being loaded by bootstrap class loader
     * @param joinPoint   joint point to intercept
     * @return join point info or null if join point must be skipped
     */
    JoinPointInfo allocate(ClassLoader classLoader, IJoinPoint joinPoint);
}
