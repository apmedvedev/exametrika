/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.profiler;

import com.exametrika.common.json.JsonObject;


/**
 * The {@link IRequest} is a request representation.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IRequest {
    /**
     * Can request be measured?
     *
     * @return true if request can be measured
     */
    boolean canMeasure();

    /**
     * Returns name of request if request can be measured.
     *
     * @return name of request
     */
    String getName();

    /**
     * Returns index of variant of mapping strategy used to create request.
     *
     * @return index of variant of mapping strategy used to create request or 0 if request is not composite
     */
    int getVariant();

    /**
     * Returns request metadata if request can be measured or null if request does not have metadata.
     *
     * @return request metadata if request can be measured or null if request does not have metadata
     */
    JsonObject getMetadata();

    /**
     * Returns detailed request instance parameters if request can be measured or null if request does not have parameters.
     *
     * @return detailed request instance parameters if request can be measured or null if request does not have parameters
     */
    JsonObject getParameters();

    /**
     * Returns raw request.
     *
     * @param <T> request type
     * @return raw request
     */
    <T> T getRawRequest();

    /**
     * Returns error associated with request.
     *
     * @return error associated with request
     */
    JsonObject getError();

    /**
     * Sets error associated with request.
     *
     * @param value error associated with request
     */
    void setError(JsonObject value);

    /**
     * Called when request has ended.
     */
    void end();
}
