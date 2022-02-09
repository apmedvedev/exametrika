/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb.config.schema;

import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.common.utils.Memory;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.exadb.core.Constants;
import com.exametrika.impl.exadb.objectdb.fields.BodyField;
import com.exametrika.impl.exadb.objectdb.fields.BodyFieldConverter;
import com.exametrika.impl.exadb.objectdb.schema.BodyFieldSchema;
import com.exametrika.spi.exadb.objectdb.config.schema.ComplexFieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.fields.IFieldConverter;


/**
 * The {@link BodyFieldSchemaConfiguration} represents a configuration of schema of node body field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class BodyFieldSchemaConfiguration extends ComplexFieldSchemaConfiguration {
    private final boolean compressed;

    public BodyFieldSchemaConfiguration(String name) {
        this(name, name, null, true);
    }

    public BodyFieldSchemaConfiguration(String name, String alias, String description, boolean compressed) {
        super(name, alias, description, Constants.COMPLEX_FIELD_AREA_DATA_SIZE, Memory.getShallowSize(BodyField.class));

        this.compressed = compressed;
    }

    public boolean isCompressed() {
        return compressed;
    }

    @Override
    public IFieldSchema createSchema(int index, int offset, int indexTotalIndex) {
        return new BodyFieldSchema(this, index, offset);
    }

    @Override
    public boolean isCompatible(FieldSchemaConfiguration newConfiguration) {
        return newConfiguration instanceof BodyFieldSchemaConfiguration;
    }

    @Override
    public IFieldConverter createConverter(FieldSchemaConfiguration newConfiguration) {
        return new BodyFieldConverter();
    }

    @Override
    public Object createInitializer() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof BodyFieldSchemaConfiguration))
            return false;

        BodyFieldSchemaConfiguration configuration = (BodyFieldSchemaConfiguration) o;
        return super.equals(configuration) && compressed == configuration.compressed;
    }

    @Override
    public boolean equalsStructured(FieldSchemaConfiguration newSchema) {
        if (!(newSchema instanceof BodyFieldSchemaConfiguration))
            return false;

        BodyFieldSchemaConfiguration configuration = (BodyFieldSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration) && compressed == configuration.compressed;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(compressed);
    }
}
