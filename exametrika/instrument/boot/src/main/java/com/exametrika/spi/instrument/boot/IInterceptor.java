/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.instrument.boot;


/**
 * The {@link IInterceptor} represents a static interceptor interface.
 *
 * @author Medvedev-A
 * @threadsafety Implementations of this interface and its methods are thread safe.
 */
public interface IInterceptor {
    /**
     * Is measuring suspended in current thread.
     *
     * @return true if measuring is suspended in current thread.
     */
    boolean isSuspended();

    /**
     * Called on line interception.
     *
     * @param index    join point index
     * @param version  join point version
     * @param instance intercepted object instance
     */
    void onLine(int index, int version, Object instance);

    /**
     * Called on method enter interception.
     *
     * @param index    join point index
     * @param version  join point version
     * @param instance intercepted object instance
     * @param params   method params or null if method params are not intercepted
     * @return user defined parameter or null
     */
    Object onEnter(int index, int version, Object instance, Object[] params);

    /**
     * Called on method exit interception.
     *
     * @param index    join point index
     * @param version  join point version
     * @param param    user defined parameter or null
     * @param instance intercepted object instance
     * @param retVal   method return value or null if method return value is not intercepted
     */
    void onReturnExit(int index, int version, Object param, Object instance, Object retVal);

    /**
     * Called on method exit by exception interception.
     *
     * @param index     join point index
     * @param version   join point version
     * @param param     user defined parameter or null
     * @param instance  intercepted object instance
     * @param exception method exception or null if method exception is not intercepted
     */
    void onThrowExit(int index, int version, Object param, Object instance, Throwable exception);

    /**
     * Called on catch exception interception.
     *
     * @param index     join point index
     * @param version   join point version
     * @param instance  intercepted object instance
     * @param exception intercepted exception
     */
    void onCatch(int index, int version, Object instance, Throwable exception);

    /**
     * Called on monitor before enter interception.
     *
     * @param index    join point index
     * @param version  join point version
     * @param instance intercepted object instance
     * @param monitor  intercepted monitor
     */
    void onMonitorBeforeEnter(int index, int version, Object instance, Object monitor);

    /**
     * Called on monitor after enter interception.
     *
     * @param index    join point index
     * @param version  join point version
     * @param instance intercepted object instance
     * @param monitor  intercepted monitor
     */
    void onMonitorAfterEnter(int index, int version, Object instance, Object monitor);

    /**
     * Called on monitor before exit interception.
     *
     * @param index    join point index
     * @param version  join point version
     * @param instance intercepted object instance
     * @param monitor  intercepted monitor
     */
    void onMonitorBeforeExit(int index, int version, Object instance, Object monitor);

    /**
     * Called on monitor after exit interception.
     *
     * @param index    join point index
     * @param version  join point version
     * @param instance intercepted object instance
     * @param monitor  intercepted monitor
     */
    void onMonitorAfterExit(int index, int version, Object instance, Object monitor);

    /**
     * Called on method call enter interception.
     *
     * @param index    join point index
     * @param version  join point version
     * @param instance intercepted object instance
     * @param callee   intercepted callee
     * @param params   called method params or null if method params are not intercepted
     * @return user defined parameter or null
     */
    Object onCallEnter(int index, int version, Object instance, Object callee, Object[] params);

    /**
     * Called on method call exit interception.
     *
     * @param index    join point index
     * @param version  join point version
     * @param param    user defined parameter or null
     * @param instance intercepted object instance
     * @param callee   intercepted callee
     * @param retVal   called method return value or null if method return value is not intercepted
     */
    void onCallReturnExit(int index, int version, Object param, Object instance, Object callee, Object retVal);

    /**
     * Called on method call exit by exception interception.
     *
     * @param index     join point index
     * @param version   join point version
     * @param param     user defined parameter or null
     * @param instance  intercepted object instance
     * @param callee    intercepted callee
     * @param exception called method exception or null if method exception is not intercepted
     */
    void onCallThrowExit(int index, int version, Object param, Object instance, Object callee, Throwable exception);

    /**
     * Called on throw exception interception.
     *
     * @param index     join point index
     * @param version   join point version
     * @param instance  intercepted object instance
     * @param exception intercepted exception
     */
    void onThrow(int index, int version, Object instance, Throwable exception);

    /**
     * Called on new object interception.
     *
     * @param index    join point index
     * @param version  join point version
     * @param instance intercepted object instance
     * @param object   intercepted object
     */
    void onNewObject(int index, int version, Object instance, Object object);

    /**
     * Called on new array interception.
     *
     * @param index    join point index
     * @param version  join point version
     * @param instance intercepted object instance
     * @param array    intercepted array
     */
    void onNewArray(int index, int version, Object instance, Object array);

    /**
     * Called on field get interception.
     *
     * @param index      join point index
     * @param version    join point version
     * @param instance   intercepted object instance
     * @param fieldOwner field owner instance or null if field owner is not intercepted
     * @param fieldValue field value or null if field value is not intercepted
     */
    void onFieldGet(int index, int version, Object instance, Object fieldOwner, Object fieldValue);

    /**
     * Called on field set interception.
     *
     * @param index         join point index
     * @param version       join point version
     * @param instance      intercepted object instance
     * @param fieldOwner    field owner instance or null if field owner is not intercepted
     * @param newFieldValue new field value or null if field value is not intercepted
     */
    void onFieldSet(int index, int version, Object instance, Object fieldOwner, Object newFieldValue);

    /**
     * Called on array get interception.
     *
     * @param index        join point index
     * @param version      join point version
     * @param instance     intercepted object instance
     * @param array        array instance or null if array instance is not intercepted
     * @param elementIndex index of array
     * @param elementValue element value or null if element value is not intercepted
     */
    void onArrayGet(int index, int version, Object instance, Object array, int elementIndex, Object elementValue);

    /**
     * Called on array set interception.
     *
     * @param index           join point index
     * @param version         join point version
     * @param instance        intercepted object instance
     * @param array           array instance or null if array instance is not intercepted
     * @param elementIndex    index of array
     * @param newElementValue new element value or null if element value is not intercepted
     */
    void onArraySet(int index, int version, Object instance, Object array, int elementIndex, Object newElementValue);

    /**
     * Logs internal interceptor message.
     *
     * @param message message to log
     */
    void log(String message);

    /**
     * Logs internal interceptor exception.
     *
     * @param exception exception to log
     */
    void logError(Throwable exception);
}
