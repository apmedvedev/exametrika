/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb.schema;

import com.exametrika.common.json.IJsonCollection;
import com.exametrika.common.json.schema.JsonSchema;


/**
 * The {@link IJsonFieldSchema} represents a schema for JSON field.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IJsonFieldSchema extends IFieldSchema {
    /**
     * Returns JSON schema.
     *
     * @return JSON schema
     */
    JsonSchema getJsonSchema();

    /**
     * Validates specified value against JSON schema.
     *
     * @param value value to validate
     */
    void validate(IJsonCollection value);
}
