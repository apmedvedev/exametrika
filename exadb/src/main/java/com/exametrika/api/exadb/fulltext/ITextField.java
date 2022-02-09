/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.fulltext;

import java.io.Reader;


/**
 * The {@link ITextField} represents an index text field.
 *
 * @author AndreyM
 * @threadsafety Implementations of this interface and its methods are not thread safe.
 */
public interface ITextField extends IField {
    /**
     * Returns field value.
     *
     * @return field value
     */
    Reader get();
}
