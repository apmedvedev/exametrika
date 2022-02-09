/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.instrument;

import java.lang.instrument.ClassFileTransformer;


/**
 * The {@link IClassTransformerExtension} represents a class transformer extension.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IClassTransformerExtension extends ClassFileTransformer {
    /**
     * Sets start or attach mode of java agent.
     *
     * @param value if true java agent is started in attach mode
     */
    void setAttached(boolean value);
}
