/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb.fields;

import com.exametrika.common.json.JsonObject;


/**
 * The {@link IJsonRecord} represents an Json record.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IJsonRecord {
    /**
     * Returns value.
     *
     * @return value
     */
    JsonObject getValue();
}
