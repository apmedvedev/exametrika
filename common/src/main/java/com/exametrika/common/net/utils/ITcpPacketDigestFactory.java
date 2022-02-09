/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.net.utils;


/**
 * The {@link ITcpPacketDigestFactory} represents a TCP packet digest factory.
 *
 * @param <T> message type
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface ITcpPacketDigestFactory<T> {
    /**
     * Creates packet digest.
     *
     * @param message message
     * @return digest
     */
    Object createDigest(T message);
}
