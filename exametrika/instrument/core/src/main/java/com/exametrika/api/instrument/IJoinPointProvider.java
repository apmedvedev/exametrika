/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.instrument;

import java.util.List;


/**
 * The {@link IJoinPointProvider} represents a provider of join points.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IJoinPointProvider {
    /**
     * Join point entry.
     */
    class JoinPointEntry {
        /**
         * Index of joint point.
         */
        public final int index;

        /**
         * Version of joint point.
         */
        public final int version;

        /**
         * Join point.
         */
        public final IJoinPoint joinPoint;

        public JoinPointEntry(int index, int version, IJoinPoint joinPoint) {
            this.index = index;
            this.version = version;
            this.joinPoint = joinPoint;
        }
    }

    /**
     * Returns current count of allocated join points.
     *
     * @return current count of allocated join points
     */
    int getJoinPointCount();

    /**
     * Finds join point by index and version.
     *
     * @param index   join point index
     * @param version join point version or -1 if joint point version is not checked
     * @return join point or null if join point is not found
     */
    IJoinPoint findJoinPoint(int index, int version);

    /**
     * Finds join points by given intercepted class name and method name.
     *
     * @param className        class name
     * @param methodName       method name
     * @param interceptorClass interceptorClass
     * @return list of found join points
     */
    List<JoinPointEntry> findJoinPoints(String className, String methodName, Class interceptorClass);
}
