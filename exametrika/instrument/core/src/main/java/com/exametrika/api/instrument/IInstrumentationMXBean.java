/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.instrument;


/**
 * The {@link IInstrumentationMXBean} represents an instrumentation MBean.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IInstrumentationMXBean {
    /**
     * Name of MXBean of agent.
     */
    final String MBEAN_NAME = "com.exametrika.instrument:type=Instrumentation";

    /**
     * Returns total number of classes transformed by instrumentation service.
     *
     * @return total number of classes transformed by instrumentation service
     */
    int getTotalTransformedClassesCount();

    /**
     * Returns total number of classes skipped by instrumentation service.
     *
     * @return total number of classes skipped by instrumentation service
     */
    int getTotalSkippedClassesCount();

    /**
     * Returns total number of transformation errors.
     *
     * @return total number of transformation errors
     */
    int getTotalTransformationErrorsCount();

    /**
     * Returns total time in milliseconds spent in class transformations.
     *
     * @return total time in milliseconds spent in class transformations
     */
    long getTotalTransformationTime();

    /**
     * Returns initial total size in bytes of classes transformed by instrumentation service.
     *
     * @return initial total size in bytes of classes transformed by instrumentation service
     */
    int getTotalTransformedClassesInitialSize();

    /**
     * Returns resulting total size in bytes of classes transformed by instrumentation service.
     *
     * @return resulting total size in bytes of classes transformed by instrumentation service
     */
    int getTotalTransformedClassesResultingSize();

    /**
     * Returns current count of allocated join points.
     *
     * @return current count of allocated join points
     */
    int getJoinPointCount();
}
