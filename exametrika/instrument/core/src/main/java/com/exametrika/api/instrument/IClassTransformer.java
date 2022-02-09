/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.instrument;

import java.util.Set;


/**
 * The {@link IClassTransformer} represents a transformer of classes.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IClassTransformer {
    /**
     * Retransforms unconditionally classes with specified names.
     *
     * @param classNames names of retransformed classes or null if all classes must be retransformed
     */
    void retransformClasses(Set<String> classNames);
}
