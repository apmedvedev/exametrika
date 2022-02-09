/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.objectdb.fields;


/**
 * The {@link IComplexField} represents a complex node field.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IComplexField extends IField {
    /**
     * Returns mode of autoremoval of unused field areas.
     *
     * @return true if unused field areas are autoremoved during field serialization/deserialization in write transaction
     */
    boolean getAutoRemoveUnusedAreas();

    /**
     * Sets mode of autoremoval of unused field areas, where unused field areas are autoremoved during field
     * serialization/deserialization in write transaction
     */
    void setAutoRemoveUnusedAreas();

    /**
     * Creates field serialization object.
     *
     * @return field serialization object
     */
    IFieldSerialization createSerialization();

    /**
     * Creates field deserialization object.
     *
     * @return field deserialization object
     */
    IFieldDeserialization createDeserialization();

    /**
     * Refreshes internal position of field's node in cache in order to prevent node unloading.
     */
    void refresh();
}
