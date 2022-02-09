/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.fulltext;

import java.io.Reader;

import com.exametrika.api.exadb.fulltext.ITextField;
import com.exametrika.common.utils.Assert;


/**
 * The {@link IndexTextField} is an index text field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class IndexTextField extends IndexField implements ITextField {
    private final Reader value;

    public IndexTextField(String name, float boost, Reader value) {
        super(name, boost);

        Assert.notNull(value);

        this.value = value;
    }

    @Override
    public Reader get() {
        return value;
    }
}
