/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb.config.schema;

import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Memory;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.exadb.objectdb.fields.PrimitiveField;
import com.exametrika.impl.exadb.objectdb.fields.PrimitiveFieldConverter;
import com.exametrika.impl.exadb.objectdb.schema.PrimitiveFieldSchema;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.SimpleFieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.fields.IFieldConverter;


/**
 * The {@link PrimitiveFieldSchemaConfiguration} represents a configuration of schema of primitive field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class PrimitiveFieldSchemaConfiguration extends SimpleFieldSchemaConfiguration {
    private final DataType dataType;

    public enum DataType {
        BYTE,
        CHAR,
        SHORT,
        INT,
        LONG,
        BOOLEAN,
        FLOAT,
        DOUBLE
    }

    public PrimitiveFieldSchemaConfiguration(String name, DataType dataType) {
        this(name, name, null, dataType);
    }

    public PrimitiveFieldSchemaConfiguration(String name, String alias, String description, DataType dataType) {
        this(name, alias, description, dataType, 0);
    }

    public PrimitiveFieldSchemaConfiguration(String name, String alias, String description, DataType dataType, int cacheSize) {
        super(name, alias, description, getSize(dataType), cacheSize + getCacheSize(dataType));

        Assert.notNull(dataType);

        this.dataType = dataType;
    }

    public DataType getDataType() {
        return dataType;
    }

    @Override
    public IFieldSchema createSchema(int index, int offset, int indexTotalIndex) {
        return new PrimitiveFieldSchema(this, index, offset, indexTotalIndex);
    }

    @Override
    public boolean isCompatible(FieldSchemaConfiguration newConfiguration) {
        return newConfiguration instanceof PrimitiveFieldSchemaConfiguration;
    }

    @Override
    public IFieldConverter createConverter(FieldSchemaConfiguration newConfiguration) {
        return new PrimitiveFieldConverter();
    }

    @Override
    public Object createInitializer() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof PrimitiveFieldSchemaConfiguration))
            return false;

        PrimitiveFieldSchemaConfiguration configuration = (PrimitiveFieldSchemaConfiguration) o;
        return super.equals(configuration) && dataType == configuration.dataType;
    }

    @Override
    public boolean equalsStructured(FieldSchemaConfiguration newSchema) {
        if (!(newSchema instanceof PrimitiveFieldSchemaConfiguration))
            return false;

        PrimitiveFieldSchemaConfiguration configuration = (PrimitiveFieldSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration) && dataType == configuration.dataType;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(dataType);
    }

    private static int getSize(DataType dataType) {
        switch (dataType) {
            case BYTE:
                return 1;
            case CHAR:
                return 2;
            case SHORT:
                return 2;
            case INT:
                return 4;
            case LONG:
                return 8;
            case BOOLEAN:
                return 1;
            case FLOAT:
                return 4;
            case DOUBLE:
                return 8;
            default:
                return Assert.error();
        }
    }

    private static int getCacheSize(DataType dataType) {
        switch (dataType) {
            case BYTE:
                return Memory.getShallowSize(PrimitiveField.ByteField.class);
            case CHAR:
                return Memory.getShallowSize(PrimitiveField.CharField.class);
            case SHORT:
                return Memory.getShallowSize(PrimitiveField.ShortField.class);
            case INT:
                return Memory.getShallowSize(PrimitiveField.IntField.class);
            case LONG:
                return Memory.getShallowSize(PrimitiveField.LongField.class);
            case BOOLEAN:
                return Memory.getShallowSize(PrimitiveField.BooleanField.class);
            case FLOAT:
                return Memory.getShallowSize(PrimitiveField.FloatField.class);
            case DOUBLE:
                return Memory.getShallowSize(PrimitiveField.DoubleField.class);
            default:
                return Assert.error();
        }
    }
}
