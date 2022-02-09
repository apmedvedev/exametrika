/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.component;


/**
 * The {@link IVersionChangeRecord} represents a version change record.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IVersionChangeRecord {
    /**
     * Change type.
     */
    enum Type {
        /**
         * Add to group operation.
         */
        ADD,
        /**
         * Remove from group operation.
         */
        REMOVE,
        /**
         * Non-structured change operation.
         */
        CHANGE
    }

    /**
     * Returns index of component node schema.
     *
     * @return index of component node schema
     */
    int getNodeSchemaIndex();

    /**
     * Returns change time.
     *
     * @return change time
     */
    long getTime();

    /**
     * Returns change type.
     *
     * @return change type
     */
    Type getType();

    /**
     * Returns component scope identifier.
     *
     * @return component scope identifier
     */
    long getScopeId();

    /**
     * Returns group scope identifier where component is added/removed (for add/remove operation only).
     *
     * @return group scope identifier
     */
    long getGroupScopeId();

    /**
     * Returns node identifier of current (change result) version.
     *
     * @return node identifier of current (change result) version
     */
    long getNodeId();

    /**
     * Returns node identifier of previous (before change) version.
     *
     * @return node identifier of previous (before change) version
     */
    long getPreviousVersionNodeId();
}
