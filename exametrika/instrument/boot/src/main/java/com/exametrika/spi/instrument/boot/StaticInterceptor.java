/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.instrument.boot;


/**
 * The {@link StaticInterceptor} represents a no-op static interceptor class which can be used as a base for other static
 * interceptors.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class StaticInterceptor {
    public static void onLine(int index, int version, Object instance) {
    }

    public static Object onEnter(int index, int version, Object instance, Object[] params) {
        return null;
    }

    public static void onReturnExit(int index, int version, Object param, Object instance, Object retVal) {
    }

    public static void onThrowExit(int index, int version, Object param, Object instance, Throwable exception) {
    }

    public static void onCatch(int index, int version, Object instance, Throwable exception) {
    }

    public static void onMonitorBeforeEnter(int index, int version, Object instance, Object monitor) {
    }

    public static void onMonitorAfterEnter(int index, int version, Object instance, Object monitor) {
    }

    public static void onMonitorBeforeExit(int index, int version, Object instance, Object monitor) {
    }

    public static void onMonitorAfterExit(int index, int version, Object instance, Object monitor) {
    }

    public static Object onCallEnter(int index, int version, Object instance, Object callee, Object[] params) {
        return null;
    }

    public static void onCallReturnExit(int index, int version, Object param, Object instance, Object callee, Object retVal) {
    }

    public static void onCallThrowExit(int index, int version, Object param, Object instance, Object callee, Throwable exception) {
    }

    public static void onThrow(int index, int version, Object instance, Throwable exception) {
    }

    public static void onNewObject(int index, int version, Object instance, Object object) {
    }

    public static void onNewArray(int index, int version, Object instance, Object array) {
    }

    public static void onFieldGet(int index, int version, Object instance, Object fieldOwner, Object fieldValue) {
    }

    public static void onFieldSet(int index, int version, Object instance, Object fieldOwner, Object newFieldValue) {
    }

    public static void onArrayGet(int index, int version, Object instance, Object array, int elementIndex, Object elementValue) {
    }

    public static void onArraySet(int index, int version, Object instance, Object array, int elementIndex, Object newElementValue) {
    }
}
