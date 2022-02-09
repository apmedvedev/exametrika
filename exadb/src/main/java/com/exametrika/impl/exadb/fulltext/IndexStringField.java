/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.fulltext;

import com.exametrika.api.exadb.fulltext.IStringField;
import com.exametrika.common.utils.Strings;


/**
 * The {@link IndexStringField} is an index string field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class IndexStringField extends IndexField implements IStringField {
    private final String value;

    public IndexStringField(String name, float boost, String value) {
        super(name, boost);

        this.value = Strings.notNull(value);
    }

    @Override
    public String get() {
        return value;
    }

    @Override
    public String toString() {
        return name + ":" + value;
    }
}
