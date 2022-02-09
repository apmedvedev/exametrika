/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.common.values;

import com.exametrika.common.json.IJsonCollection;


/**
 * The {@link IFieldValue} represents an immutable measurement child field value.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public interface IFieldValue {
    IJsonCollection toJson();

    @Override
    boolean equals(Object o);

    @Override
    int hashCode();
}
