/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.instrument;


/**
 * The {@link IReentrancyListener} represents a reentrancy listener.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IReentrancyListener {
    /**
     * Called when class transform is entered.
     *
     * @return call handle
     */
    Object onTransformEntered();

    /**
     * Called when class transform is exited.
     *
     * @param param call handle
     */
    void onTransformExited(Object param);
}
