/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.metrics.jvm;

import java.sql.Statement;


/**
 * The {@link IJdbcRawRequest} represents a jdbc raw request.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IJdbcRawRequest {
    /**
     * Returns url.
     *
     * @return url
     */
    String getUrl();

    /**
     * Returns statement object.
     *
     * @return statement object
     */
    Statement getStatement();

    /**
     * Returns query info.
     *
     * @return query info
     */
    JdbcBatchQueryInfo getQuery();
}
