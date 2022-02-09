/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.index.config.schema;

import com.exametrika.api.exadb.index.IKeyNormalizer;
import com.exametrika.api.exadb.index.Indexes;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.exadb.index.config.schema.KeyNormalizerSchemaConfiguration;


/**
 * The {@link NumericKeyNormalizerSchemaConfiguration} is a configuration of primitive key normalizer.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class NumericKeyNormalizerSchemaConfiguration extends KeyNormalizerSchemaConfiguration {
    private final DataType dataType;

    public enum DataType {
        BYTE,
        SHORT,
        INT,
        LONG,
        FLOAT,
        DOUBLE
    }

    public NumericKeyNormalizerSchemaConfiguration(DataType dataType) {
        Assert.notNull(dataType);

        this.dataType = dataType;
    }

    public DataType getDataType() {
        return dataType;
    }

    @Override
    public IKeyNormalizer createKeyNormalizer() {
        switch (dataType) {
            case BYTE:
                return Indexes.createByteKeyNormalizer();
            case SHORT:
                return Indexes.createShortKeyNormalizer();
            case INT:
                return Indexes.createIntKeyNormalizer();
            case LONG:
                return Indexes.createLongKeyNormalizer();
            case FLOAT:
                return Indexes.createFloatKeyNormalizer();
            case DOUBLE:
                return Indexes.createDoubleKeyNormalizer();
            default:
                return Assert.error();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof NumericKeyNormalizerSchemaConfiguration))
            return false;

        NumericKeyNormalizerSchemaConfiguration configuration = (NumericKeyNormalizerSchemaConfiguration) o;
        return dataType.equals(configuration.dataType);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(dataType);
    }

    @Override
    public final String toString() {
        return dataType.toString();
    }
}
