/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.fulltext;

import com.exametrika.api.exadb.fulltext.IField;
import com.exametrika.common.utils.Assert;


/**
 * The {@link IndexField} is an index field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public abstract class IndexField implements IField {
    protected final String name;
    private final float boost;

    public IndexField(String name, float boost) {
        Assert.notNull(name);

        this.name = name;
        this.boost = boost;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public float getBoost() {
        return boost;
    }

    @Override
    public String toString() {
        return name;
    }
}
