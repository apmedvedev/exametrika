/**
 * Copyright 2014 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.profiler;

import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;

/**
 * The {@link Request} is a base request.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class Request implements IRequest {
    private final String name;
    private final Object request;
    private JsonObject error;

    public Request(String name, Object request) {
        Assert.notNull(name);

        this.name = name;
        this.request = (request != null ? request : this);
    }

    @Override
    public boolean canMeasure() {
        return true;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getVariant() {
        return 0;
    }

    @Override
    public JsonObject getMetadata() {
        return null;
    }

    @Override
    public JsonObject getParameters() {
        return getMetadata();
    }

    @Override
    public JsonObject getError() {
        return error;
    }

    @Override
    public void setError(JsonObject value) {
        this.error = value;
    }

    @Override
    public Object getRawRequest() {
        return request;
    }

    @Override
    public void end() {
    }

    public String nameNoPrefix(int segmentCount) {
        String name = getName();
        int pos = -1;
        while (segmentCount > 0) {
            pos = name.indexOf(":", pos + 1);
            segmentCount--;
        }

        if (pos != -1)
            name = name.substring(pos + 1);

        return name;
    }

    @Override
    public String toString() {
        return name;
    }
}