/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb.config.schema;

import java.util.Set;

import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.common.utils.Memory;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.exadb.core.Constants;
import com.exametrika.impl.exadb.objectdb.fields.SerializableField;
import com.exametrika.impl.exadb.objectdb.fields.SerializableFieldConverter;
import com.exametrika.impl.exadb.objectdb.schema.SerializableFieldSchema;
import com.exametrika.spi.exadb.objectdb.config.schema.ComplexFieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.fields.IFieldConverter;


/**
 * The {@link SerializableFieldSchemaConfiguration} represents a configuration of schema of serializable field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class SerializableFieldSchemaConfiguration extends ComplexFieldSchemaConfiguration {
    private final boolean required;
    private final boolean compressed;
    private final Set<String> allowedClasses;

    public SerializableFieldSchemaConfiguration(String name) {
        this(name, name, null, false, true, null);
    }

    public SerializableFieldSchemaConfiguration(String name, String alias, String description, boolean required, boolean compressed,
                                                Set<String> allowedClasses) {
        super(name, alias, description, Constants.COMPLEX_FIELD_AREA_DATA_SIZE, Memory.getShallowSize(SerializableField.class));

        this.required = required;
        this.compressed = compressed;
        this.allowedClasses = allowedClasses;
    }

    public boolean isRequired() {
        return required;
    }

    public boolean isCompressed() {
        return compressed;
    }

    public Set<String> getAllowedClasses() {
        return allowedClasses;
    }

    @Override
    public IFieldSchema createSchema(int index, int offset, int indexTotalIndex) {
        return new SerializableFieldSchema(this, index, offset);
    }

    @Override
    public boolean isCompatible(FieldSchemaConfiguration newConfiguration) {
        return newConfiguration instanceof SerializableFieldSchemaConfiguration;
    }

    @Override
    public IFieldConverter createConverter(FieldSchemaConfiguration newConfiguration) {
        return new SerializableFieldConverter();
    }

    @Override
    public Object createInitializer() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof SerializableFieldSchemaConfiguration))
            return false;

        SerializableFieldSchemaConfiguration configuration = (SerializableFieldSchemaConfiguration) o;
        return super.equals(configuration) && required == configuration.required && compressed == configuration.compressed &&
                Objects.equals(allowedClasses, configuration.allowedClasses);
    }

    @Override
    public boolean equalsStructured(FieldSchemaConfiguration newSchema) {
        if (!(newSchema instanceof SerializableFieldSchemaConfiguration))
            return false;

        SerializableFieldSchemaConfiguration configuration = (SerializableFieldSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration) && compressed == configuration.compressed;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(required, compressed, allowedClasses);
    }
}
