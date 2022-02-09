/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.component.factory.threadlocal;

/**
 * The {@link IThreadComponentListener} is a listener for thread component events.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IThreadComponentListener {
    /**
     * Called when thread-local components must be created.
     */
    void onCreateThreadComponents();

    /**
     * Called when thread-local components must be destroyed.
     */
    void onDestroyThreadComponents();
}
