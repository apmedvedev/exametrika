/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.fulltext.config.schema;

import java.util.Set;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Enums;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.exadb.fulltext.config.schema.FieldSchemaConfiguration;


/**
 * The {@link NumericFieldSchemaConfiguration} is a configuration of numeric index field schema.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class NumericFieldSchemaConfiguration extends FieldSchemaConfiguration {
    private final DataType type;
    private final int precisionStep;

    public enum DataType {
        INT,
        LONG,
        FLOAT,
        DOUBLE
    }

    public NumericFieldSchemaConfiguration(String name, DataType type, boolean stored, boolean indexed) {
        this(name, type, stored, indexed, 4);
    }

    public NumericFieldSchemaConfiguration(String name, DataType type, boolean stored, boolean indexed, int precisionStep) {
        super(name, createOptions(stored, indexed), null);

        Assert.notNull(type);

        this.type = type;
        this.precisionStep = precisionStep;
    }

    public DataType getType() {
        return type;
    }

    public int getPrecisionStep() {
        return precisionStep;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof NumericFieldSchemaConfiguration))
            return false;

        NumericFieldSchemaConfiguration configuration = (NumericFieldSchemaConfiguration) o;
        return super.equals(o) && type == configuration.type && precisionStep == configuration.precisionStep;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(type, precisionStep);
    }

    private static Set<Option> createOptions(boolean stored, boolean indexed) {
        Set<Option> options = Enums.of(Option.INDEX_DOCUMENTS, Option.OMIT_NORMS);
        if (stored)
            options.add(Option.STORED);
        if (stored)
            options.add(Option.INDEXED);
        return options;
    }
}
