/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.metrics.jvm;

import java.net.HttpURLConnection;


/**
 * The {@link IHttpConnectionRawRequest} represents a http connection raw request.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IHttpConnectionRawRequest {
    /**
     * Returns url with query string.
     *
     * @return url with query string
     */
    String getUrlWithQueryString();

    /**
     * Returns url.
     *
     * @return url
     */
    String getUrl();

    /**
     * Returns host and port.
     *
     * @return host and port
     */
    String getHostPort();

    /**
     * Returns connection object.
     *
     * @return connection object
     */
    HttpURLConnection getConnection();
}
