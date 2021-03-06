/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.utils;


/**
 * The {@link IVisitor} is a visitor.
 *
 * @param <T> visited element type
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IVisitor<T> {
    /**
     * Visits specified element.
     *
     * @param element element to visit
     * @return if false visiting is canceled after current element
     */
    boolean visit(T element);
}
