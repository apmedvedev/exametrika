/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.tests;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * The {@link Tests} implements different utility functions for testing purposes.
 *
 * @author medvedev
 */
public class Tests {
    /**
     * Returns a value of specified field for given object.
     *
     * @param <T>       value type
     * @param o         object field belongs to
     * @param fieldName field name
     * @return field value
     * @throws IllegalAccessException if access to field is denied
     * @throws NoSuchFieldException   if field is not found
     */
    public static <T> T get(Object o, String fieldName) throws IllegalAccessException, NoSuchFieldException {
        Class clazz = o.getClass();
        while (clazz != null) {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.getName().equals(fieldName)) {
                    field.setAccessible(true);
                    return (T) field.get(o);
                }
            }

            clazz = clazz.getSuperclass();
        }

        throw new NoSuchFieldException(fieldName);
    }

    /**
     * Sets a value of specified field for given object.
     *
     * @param o         object field belongs to
     * @param fieldName field name
     * @param value     field value
     * @throws IllegalAccessException if access to field is denied
     * @throws NoSuchFieldException   if field is not found
     */
    public static void set(Object o, String fieldName, Object value) throws IllegalAccessException, NoSuchFieldException {
        Class clazz = o.getClass();
        while (clazz != null) {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.getName().equals(fieldName)) {
                    field.setAccessible(true);
                    field.set(o, value);
                    return;
                }
            }

            clazz = clazz.getSuperclass();
        }

        throw new NoSuchFieldException(fieldName);
    }

    /**
     * Returns a value of specified static field for given class.
     *
     * @param <T>       value type
     * @param clazz     class field belongs to
     * @param fieldName field name
     * @return field value
     * @throws IllegalAccessException if access to field is denied
     * @throws NoSuchFieldException   if field is not found
     */
    public static <T> T getStatic(Class clazz, String fieldName) throws IllegalAccessException, NoSuchFieldException {
        while (clazz != null) {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.getName().equals(fieldName)) {
                    field.setAccessible(true);
                    return (T) field.get(null);
                }
            }

            clazz = clazz.getSuperclass();
        }

        throw new NoSuchFieldException(fieldName);
    }

    /**
     * Returns a value of specified static field for given class.
     *
     * @param clazz     class field belongs to
     * @param fieldName field name
     * @param value     field value
     * @throws IllegalAccessException if access to field is denied
     * @throws NoSuchFieldException   if field is not found
     */
    public static void setStatic(Class clazz, String fieldName, Object value) throws IllegalAccessException, NoSuchFieldException {
        while (clazz != null) {
            for (Field field : clazz.getDeclaredFields()) {
                if (field.getName().equals(fieldName)) {
                    field.setAccessible(true);
                    field.set(null, value);
                    return;
                }
            }

            clazz = clazz.getSuperclass();
        }

        throw new NoSuchFieldException(fieldName);
    }

    /**
     * Invokes given method on specified object instance.
     * @param o object instance
     * @param methodName method name
     * @param args method arguments
     * @param <T> method result type
     * @return result of invocation
     * @throws IllegalAccessException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     */
    public static <T> T invoke(Object o, String methodName, Object ... args) throws IllegalAccessException,
            NoSuchMethodException, InvocationTargetException {
        Class clazz = o.getClass();
        while (clazz != null) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.getName().equals(methodName)) {
                    method.setAccessible(true);
                    return (T) method.invoke(o, args);
                }
            }

            clazz = clazz.getSuperclass();
        }

        throw new NoSuchMethodException(methodName);
    }

    /**
     * Invokes given static method on specified class.
     * @param clazz class
     * @param methodName method name
     * @param args method arguments
     * @param <T> method result type
     * @return result of invocation
     * @throws IllegalAccessException
     * @throws NoSuchMethodException
     * @throws InvocationTargetException
     */
    public static <T> T invokeStatic(Class clazz, String methodName, Object ... args) throws IllegalAccessException,
            NoSuchMethodException, InvocationTargetException {
        while (clazz != null) {
            for (Method method : clazz.getDeclaredMethods()) {
                if (method.getName().equals(methodName)) {
                    method.setAccessible(true);
                    return (T) method.invoke(null, args);
                }
            }

            clazz = clazz.getSuperclass();
        }

        throw new NoSuchMethodException(methodName);
    }
}
