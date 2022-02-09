/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator;


/**
 * The {@link IBehaviorTypeProvider} represents a provider of behavior types.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IBehaviorTypeProvider {
    String NAME = "component.BehaviorTypeProvider";

    /**
     * Is behavior type with specified type identifier contained in type provider?
     *
     * @param typeId type identifier
     * @return if true behavior type is contained in type provider
     */
    boolean containsBehaviorType(int typeId);

    /**
     * Finds behavior type by type identifier.
     *
     * @param typeId type identifier
     * @return behavior type or null if behavior type is not found
     */
    BehaviorType findBehaviorType(int typeId);

    /**
     * Adds behavior type.
     *
     * @param typeId       type identifier
     * @param behaviorType behavior type
     */
    void addBehaviorType(int typeId, BehaviorType behaviorType);
}
