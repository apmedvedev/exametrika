/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.metrics.jvm;


/**
 * The {@link IHttpServletRawRequest} represents a http servlet raw request.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IHttpServletRawRequest {
    /**
     * Returns servlet context name.
     *
     * @return servlet context name
     */
    String getContextName();

    /**
     * Returns url.
     *
     * @return url
     */
    String getUrl();

    /**
     * Returns url with query string.
     *
     * @return url with query string
     */
    String getUrlWithQueryString();

    /**
     * Returns query string.
     *
     * @return query string
     */
    String getQueryString();

    /**
     * Returns servlet.
     *
     * @return servlet
     */
    Object getServlet();

    /**
     * Returns servlet request.
     *
     * @return servlet request
     */
    Object getRequest();

    /**
     * Returns servlet response.
     *
     * @return servlet response
     */
    Object getResponse();
}
