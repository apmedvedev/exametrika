/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.core;


/**
 * The {@link IPublicExtensionRegistrar} represents a registrar of public database extension.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IPublicExtensionRegistrar {
    /**
     * Registers public extension in registrar.
     *
     * @param name               public extension name
     * @param extension          public extension
     * @param requireTransaction if true published extension requires transaction
     */
    void register(String name, Object extension, boolean requireTransaction);
}
