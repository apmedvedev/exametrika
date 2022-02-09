/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.nodes;


/**
 * The {@link IHealthComponentVersion} represents a health component version node.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IHealthComponentVersion extends IComponentVersion {
    /**
     * Component state.
     */
    enum State {
        /**
         * Component is created and not yet available.
         */
        CREATED,

        /**
         * Component is normal (healthy and available).
         */
        NORMAL,

        /**
         * Component is available but with health warning.
         */
        HEALTH_WARNING,

        /**
         * Component is available but unhealthy (failed).
         */
        HEALTH_ERROR,

        /**
         * Component is unavailable (failed).
         */
        UNAVAILABLE,

        /**
         * Component is in maintenance mode and not available.
         */
        MAINTENANCE,

        /**
         * Component is deleted and not available.
         */
        DELETED
    }

    /**
     * Is component dynamic or static?
     *
     * @return true if component is dynamic
     */
    boolean isDynamic();

    /**
     * Is component healthy?
     *
     * @return true if component is healthy
     */
    boolean isHealthy();

    /**
     * Returns component state.
     *
     * @return component state
     */
    State getState();

    /**
     * Returns component creation time.
     *
     * @return component creation time
     */
    long getCreationTime();

    /**
     * Returns period since creation time till current selection time.
     *
     * @return period since creation time till current selection time
     */
    long getTotalPeriod();

    /**
     * Returns last start time (time - when component became healthy).
     *
     * @return last start time (time - when component became healthy)
     */
    long getStartTime();

    /**
     * Returns last stop time (time - when component became unhealthy).
     *
     * @return last stop time (time - when component became unhealthy)
     */
    long getStopTime();

    /**
     * Returns period since last start time till current selection time (if component is healthy).
     *
     * @return period since last start time till current selection time (if component is healthy)
     */
    long getUpPeriod();

    /**
     * Returns period since last stop time till current selection time (if component is unhealthy).
     *
     * @return period since last stop time till current selection time (if component is unhealthy)
     */
    long getDownPeriod();

    /**
     * Returns maintenance mode message.
     *
     * @return maintenance mode message or null if component is not in maintenance mode or message is not set
     */
    String getMaintenanceMessage();
}
