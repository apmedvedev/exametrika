/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.fulltext;

import com.exametrika.api.exadb.fulltext.INumericField;
import com.exametrika.common.utils.Assert;


/**
 * The {@link IndexNumericField} is an index numeric field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class IndexNumericField extends IndexField implements INumericField {
    public static final String PREFIX = "_r";
    private final Number value;

    public IndexNumericField(String name, float boost, Number value) {
        super(name, boost);

        Assert.notNull(value);

        this.value = value;
    }

    @Override
    public Number get() {
        return value;
    }

    @Override
    public String toString() {
        return name + ":" + value;
    }
}
