/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.config;

import java.io.Serializable;


/**
 * The {@link Configuration} is an abstract configuration element.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class Configuration implements Serializable {
    @Override
    public abstract boolean equals(Object o);

    @Override
    public abstract int hashCode();
}
