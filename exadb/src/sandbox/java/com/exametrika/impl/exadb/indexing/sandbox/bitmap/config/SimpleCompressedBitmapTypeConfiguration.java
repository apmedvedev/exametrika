/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.indexing.sandbox.bitmap.config;


/**
 * The {@link SimpleCompressedBitmapTypeConfiguration} is a configuration of simple compressed bitmap type.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class SimpleCompressedBitmapTypeConfiguration extends BitmapTypeConfiguration {
    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof SimpleCompressedBitmapTypeConfiguration))
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
