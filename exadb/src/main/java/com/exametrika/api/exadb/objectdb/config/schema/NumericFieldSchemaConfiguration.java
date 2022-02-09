/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb.config.schema;

import java.util.LinkedHashSet;
import java.util.Set;

import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.exadb.objectdb.schema.NumericFieldSchema;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;


/**
 * The {@link NumericFieldSchemaConfiguration} represents a configuration of schema of numeric field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class NumericFieldSchemaConfiguration extends PrimitiveFieldSchemaConfiguration {
    private final Number min;
    private final Number max;
    private final Set<Number> enumeration;
    private final String sequenceField;

    public NumericFieldSchemaConfiguration(String name, DataType dataType) {
        this(name, name, null, dataType, null, null, null, null);
    }

    public NumericFieldSchemaConfiguration(String name, String alias, String description, DataType dataType,
                                           Number min, Number max, Set<? extends Number> enumeration, String sequenceField) {
        this(name, alias, description, dataType, min, max, enumeration, sequenceField, 0);
    }

    public NumericFieldSchemaConfiguration(String name, String alias, String description, DataType dataType,
                                           Number min, Number max, Set<? extends Number> enumeration, String sequenceField, int cacheSize) {
        super(name, alias, description, dataType, cacheSize);

        Assert.isTrue(dataType != DataType.BOOLEAN && dataType != DataType.CHAR);

        this.min = cast(dataType, min);
        this.max = cast(dataType, max);
        if (enumeration != null) {
            Set<Number> set = new LinkedHashSet<Number>(enumeration.size());
            for (Number value : enumeration)
                set.add(cast(dataType, value));

            enumeration = set;
        }
        this.enumeration = Immutables.wrap(enumeration);
        this.sequenceField = sequenceField;
    }

    public Number getMin() {
        return min;
    }

    public Number getMax() {
        return max;
    }

    public Set<Number> getEnumeration() {
        return enumeration;
    }

    public String getSequenceField() {
        return sequenceField;
    }

    @Override
    public IFieldSchema createSchema(int index, int offset, int indexTotalIndex) {
        return new NumericFieldSchema(this, index, offset, indexTotalIndex);
    }

    @Override
    public Object createInitializer() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof NumericFieldSchemaConfiguration))
            return false;

        NumericFieldSchemaConfiguration configuration = (NumericFieldSchemaConfiguration) o;
        return super.equals(configuration) && Objects.equals(min, configuration.min) && Objects.equals(max, configuration.max) &&
                Objects.equals(enumeration, configuration.enumeration) && Objects.equals(sequenceField, configuration.sequenceField);
    }

    @Override
    public boolean equalsStructured(FieldSchemaConfiguration newSchema) {
        if (!(newSchema instanceof NumericFieldSchemaConfiguration))
            return false;

        NumericFieldSchemaConfiguration configuration = (NumericFieldSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(min, max, enumeration, sequenceField);
    }

    private Number cast(DataType dataType, Number value) {
        if (value == null)
            return null;

        switch (dataType) {
            case BYTE:
                return value.byteValue();
            case SHORT:
                return value.shortValue();
            case INT:
                return value.intValue();
            case LONG:
                return value.longValue();
            case FLOAT:
                return value.floatValue();
            case DOUBLE:
                return value.doubleValue();
            default:
                return Assert.error();
        }
    }
}
