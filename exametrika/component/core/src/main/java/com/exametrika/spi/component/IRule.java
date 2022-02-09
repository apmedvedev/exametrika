/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.component;


/**
 * The {@link IRule} represents a component rule.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IRule {
    /**
     * Returns rule name.
     *
     * @return rule name
     */
    String getName();
}
