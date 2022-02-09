/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.tasks.impl;


/**
 * The {@link IThreadDataProvider} is a provider of used data attached to thread.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IThreadDataProvider {
    /**
     * Returns used data attached to thread.
     *
     * @return used data attached to thread or null
     */
    Object getData();
}
