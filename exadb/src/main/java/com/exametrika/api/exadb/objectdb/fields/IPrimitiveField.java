/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb.fields;

import com.exametrika.spi.exadb.objectdb.fields.IField;


/**
 * The {@link IPrimitiveField} represents a primitive node field.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface IPrimitiveField extends IField {
    /**
     * Returns field value.
     *
     * @return field value
     */
    @Override
    <T> T get();

    /**
     * Returns field value.
     *
     * @return field value
     */
    byte getByte();

    /**
     * Returns field value.
     *
     * @return field value
     */
    short getShort();

    /**
     * Returns field value.
     *
     * @return field value
     */
    char getChar();

    /**
     * Returns field value.
     *
     * @return field value
     */
    int getInt();

    /**
     * Returns field value.
     *
     * @return field value
     */
    long getLong();

    /**
     * Returns field value.
     *
     * @return field value
     */
    boolean getBoolean();

    /**
     * Returns field value.
     *
     * @return field value
     */
    float getFloat();

    /**
     * Returns field value.
     *
     * @return field value
     */
    double getDouble();

    /**
     * Sets field value.
     *
     * @param value field value
     */
    void set(Object value);

    /**
     * Sets field value.
     *
     * @param value field value
     */
    void setByte(byte value);

    /**
     * Sets field value.
     *
     * @param value field value
     */
    void setShort(short value);

    /**
     * Sets field value.
     *
     * @param value field value
     */
    void setChar(char value);

    /**
     * Sets field value.
     *
     * @param value field value
     */
    void setInt(int value);

    /**
     * Sets field value.
     *
     * @param value field value
     */
    void setLong(long value);

    /**
     * Sets field value.
     *
     * @param value field value
     */
    void setBoolean(boolean value);

    /**
     * Sets field value.
     *
     * @param value field value
     */
    void setFloat(float value);

    /**
     * Sets field value.
     *
     * @param value field value
     */
    void setDouble(double value);
}
