/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb.schema;

import com.exametrika.api.exadb.core.schema.ISchemaObject;
import com.exametrika.common.rawdb.RawRollbackException;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.fields.IField;
import com.exametrika.spi.exadb.objectdb.fields.IFieldObject;


/**
 * The {@link IFieldSchema} represents a schema for field of node.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IFieldSchema extends ISchemaObject {
    String TYPE = "field";

    /**
     * Returns configuration.
     *
     * @return configuration
     */
    @Override
    FieldSchemaConfiguration getConfiguration();

    /**
     * Returns node schema.
     *
     * @return node schema
     */
    @Override
    INodeSchema getParent();

    /**
     * Returns index of schema in node's field schemas.
     *
     * @return index of schema in node's field schemas
     */
    int getIndex();

    /**
     * Returns field offset from the beginning of node.
     *
     * @return field offset from the beginning of node
     */
    int getOffset();

    /**
     * Returns total space index of indexed field.
     *
     * @return total space index of indexed field or -1 if field is not indexed
     */
    int getIndexTotalIndex();

    /**
     * Creates a field object instance by creating typed logical field implementation based on specified physical field.
     *
     * @param field physical field
     * @return typed logical field implementation
     */
    IFieldObject createField(IField field);

    /**
     * Validates field consistency.
     *
     * @param field field to check
     * @throws RawRollbackException or any other exception if field is invalid
     */
    void validate(IField field);
}
