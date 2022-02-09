/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.instrument.boot;


/**
 * The {@link IInvocation} represents an generic invocation. Invocation is bound to current thread and reused between
 * calls, all field excepting user data are cleared after call.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IInvocation {
    /**
     * Invocation kind.
     */
    enum Kind {
        /**
         * General invocation.
         */
        INTERCEPT,

        /**
         * Method enter.
         */
        ENTER,

        /**
         * Method exit by returning from method.
         */
        RETURN_EXIT,

        /**
         * Method exit by throwing exception.
         */
        THROW_EXIT
    }

    /**
     * Returns invocation kind.
     *
     * @return invocation kind
     */
    Kind getKind();

    /**
     * Returns 'this' instance of intercepted method or <c>null</c> if static method is being intercepted.
     *
     * @return 'this' instance of intercepted method or <c>null</c> if static method is being intercepted
     */
    Object getThis();

    /**
     * Returns called object (if any).
     *
     * @return called object or null if there is no any called object
     */
    Object getObject();

    /**
     * Returns parameters of called method (if any).
     *
     * @return parameters of called method or null if there are no any params of called method
     */
    Object[] getParams();

    /**
     * Returns additional value used in call (return value, new field or array value and so on)
     *
     * @return additional value used in call or null if there is no any additional value
     */
    Object getValue();

    /**
     * Returns exception occured during the call.
     *
     * @return exception occured during the call or null if there is no any exception
     */
    Throwable getException();

    /**
     * Returns element index when accessing array.
     *
     * @return element index or -1 null if there is no acces array
     */
    int getIndex();

    /**
     * Returns user data, bound to this invocation.
     *
     * @return user data, bound to this invocation
     */
    Object getData();

    /**
     * Sets user data to this invocation.
     *
     * @param data user data to set
     */
    void setData(Object data);
}
