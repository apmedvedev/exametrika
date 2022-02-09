/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.metrics.jvm;

import java.sql.Connection;

import javax.sql.DataSource;


/**
 * The {@link IJdbcConnectionRawRequest} represents a jdbc connection raw request.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IJdbcConnectionRawRequest {
    /**
     * Returns url.
     *
     * @return url
     */
    String getUrl();

    /**
     * Returns connection.
     *
     * @return connection
     */
    Connection getConnection();

    /**
     * Returns datasource.
     *
     * @return datasource
     */
    DataSource getDataSource();
}
