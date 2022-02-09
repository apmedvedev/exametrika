/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.net;

import java.net.InetAddress;
import java.net.InetSocketAddress;


/**
 * The {@link ITcpFactory} is a factory for {@link ITcpServer} and {@link ITcpChannel} implementations.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface ITcpFactory {
    /**
     * Creates a server.
     *
     * @param parameters initialization parameters
     * @return server
     */
    ITcpServer createServer(ITcpServer.Parameters parameters);

    /**
     * Creates a client channel.
     *
     * @param remoteAddress remote address client connects to
     * @param bindAddress   network interface bind address. If null, {@link InetAddress#getLocalHost()} is used
     * @param parameters    initialization parameters
     * @return client
     */
    ITcpChannel createClient(InetSocketAddress remoteAddress, InetAddress bindAddress, ITcpChannel.Parameters parameters);
}
