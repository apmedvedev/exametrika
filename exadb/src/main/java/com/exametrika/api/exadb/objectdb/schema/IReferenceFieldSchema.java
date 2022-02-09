/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb.schema;

import java.util.Set;


/**
 * The {@link IReferenceFieldSchema} represents a schema for reference field.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IReferenceFieldSchema extends IFieldSchema {
    /**
     * Returns field schema this schema references to. Used in bidirectional reference fields only.
     *
     * @return referenced field schema or null if reference field is not bidirectional
     */
    IReferenceFieldSchema getFieldReference();

    /**
     * Returns node type references this schema can reference. Used in unidirectional reference fields only.
     *
     * @return node type references this schema can reference
     */
    Set<INodeSchema> getNodeReferences();

    /**
     * Returns external space schema.
     *
     * @return external space schema or null if reference is internal
     */
    IObjectSpaceSchema getExternalSpaceSchema();
}
