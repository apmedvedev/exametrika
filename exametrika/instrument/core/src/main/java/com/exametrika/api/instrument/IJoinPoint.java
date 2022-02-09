/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.instrument;

import java.io.Serializable;

import com.exametrika.api.instrument.config.Pointcut;


/**
 * The {@link IJoinPoint} represents a join point.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IJoinPoint extends Serializable {
    /**
     * Join point kind.
     */
    enum Kind {
        /**
         * Array get.
         */
        ARRAY_GET,

        /**
         * Array set.
         */
        ARRAY_SET,

        /**
         * Field get.
         */
        FIELD_GET,

        /**
         * Field get.
         */
        FIELD_SET,

        /**
         * New array.
         */
        NEW_ARRAY,

        /**
         * New object.
         */
        NEW_OBJECT,

        /**
         * Method call.
         */
        CALL,

        /**
         * Method intercept.
         */
        INTERCEPT,

        /**
         * Catch.
         */
        CATCH,

        /**
         * Throw.
         */
        THROW,

        /**
         * Line.
         */
        LINE,

        /**
         * Before monitor enter.
         */
        BEFORE_MONITOR_ENTER,

        /**
         * After monitor enter.
         */
        AFTER_MONITOR_ENTER,

        /**
         * Before monitor exit.
         */
        BEFORE_MONITOR_EXIT,

        /**
         * After monitor exit.
         */
        AFTER_MONITOR_EXIT
    }

    /**
     * Returns join point kind.
     *
     * @return join point kind
     */
    Kind getKind();

    /**
     * Returns unique within method identifier of joint point.
     *
     * @return unique within method identifier of joint point
     */
    int getId();

    /**
     * Returns identifier of class loader, loaded the class this joint point references to.
     *
     * @return class loader identitifer
     */
    int getClassLoaderId();

    /**
     * Returns intercepted class name of join point.
     *
     * @return intercepted class name of join point
     */
    String getClassName();

    /**
     * Returns intercepted method name of join point.
     *
     * @return intercepted method name of join point
     */
    String getMethodName();

    /**
     * Returns intercepted method signature of join point.
     *
     * @return intercepted method signature of join point
     */
    String getMethodSignature();

    /**
     * Returns ordinal overload number of method in class.
     *
     * @return ordinal overload number of method in class or 0 if method is not overloaded
     */
    int getOverloadNumber();

    /**
     * Returns pointcut of join point.
     *
     * @return pointcut of join point
     */
    Pointcut getPointcut();

    /**
     * Returns called class name of join point (if any).
     *
     * @return called class name of join point or null if there is no any called class
     */
    String getCalledClassName();

    /**
     * Returns called member name of join point (if any).
     *
     * @return called member name of join point or null if there is no any called member
     */
    String getCalledMemberName();

    /**
     * Returns called method signature of join point (if any).
     *
     * @return called method signature of join point or null if there is no any called member
     */
    String getCalledMethodSignature();

    /**
     * Returns source file name of joint point (if any).
     *
     * @return source file name or null if source file name is not available
     */
    String getSourceFileName();

    /**
     * Returns source debug information of joint point (if any).
     *
     * @return source debug information or null if source debug information is not available
     */
    String getSourceDebug();

    /**
     * Returns source line number of join point.
     *
     * @return source line number of join point
     */
    int getSourceLineNumber();

    @Override
    boolean equals(Object o);

    @Override
    int hashCode();
}
