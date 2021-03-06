/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.net;


/**
 * The {@link ITcpRateControllerFactory} is a factory of {@link ITcpRateController}.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface ITcpRateControllerFactory {
    /**
     * Creates rate controller.
     *
     * @param channel channel to bound new controller to
     * @return rate controller
     */
    ITcpRateController createController(ITcpChannel channel);
}
