/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb.config.schema;

import java.util.Set;

import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Memory;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.exadb.core.Constants;
import com.exametrika.impl.exadb.objectdb.fields.StringField;
import com.exametrika.impl.exadb.objectdb.fields.StringFieldConverter;
import com.exametrika.impl.exadb.objectdb.schema.StringFieldSchema;
import com.exametrika.spi.exadb.objectdb.config.schema.ComplexFieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.fields.IFieldConverter;


/**
 * The {@link StringFieldSchemaConfiguration} represents a configuration of schema of string field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class StringFieldSchemaConfiguration extends ComplexFieldSchemaConfiguration {
    private final boolean required;
    private final boolean compressed;
    private final int minSize;
    private final int maxSize;
    private final String pattern;
    private final Set<String> enumeration;
    private final String sequenceField;

    public StringFieldSchemaConfiguration(String name, int maxSize) {
        this(name, name, null, false, true, 0, Constants.PAGE_SIZE, null, null, null);
    }

    public StringFieldSchemaConfiguration(String name, String alias, String description,
                                          boolean required, boolean compressed, int minSize, int maxSize, String pattern, Set<String> enumeration, String sequenceField) {
        this(name, alias, description, required, compressed, minSize, maxSize, pattern, enumeration, sequenceField, 0);
    }

    public StringFieldSchemaConfiguration(String name, String alias, String description,
                                          boolean required, boolean compressed, int minSize, int maxSize, String pattern, Set<String> enumeration, String sequenceField,
                                          int cacheSize) {
        super(name, alias, description, Constants.COMPLEX_FIELD_AREA_DATA_SIZE, cacheSize + Memory.getShallowSize(StringField.class));

        Assert.isTrue(minSize <= maxSize);
        Assert.isTrue(maxSize <= Constants.PAGE_SIZE);

        this.required = required;
        this.compressed = compressed;
        this.minSize = minSize;
        this.maxSize = maxSize;
        this.pattern = pattern;
        this.enumeration = Immutables.wrap(enumeration);
        this.sequenceField = sequenceField;
    }

    public boolean isRequired() {
        return required;
    }

    public boolean isCompressed() {
        return compressed;
    }

    public int getMinSize() {
        return minSize;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public String getPattern() {
        return pattern;
    }

    public Set<String> getEnumeration() {
        return enumeration;
    }

    public String getSequenceField() {
        return sequenceField;
    }

    @Override
    public IFieldSchema createSchema(int index, int offset, int indexTotalIndex) {
        return new StringFieldSchema(this, index, offset, indexTotalIndex);
    }

    @Override
    public boolean isCompatible(FieldSchemaConfiguration newConfiguration) {
        return newConfiguration instanceof StringFieldSchemaConfiguration;
    }

    @Override
    public IFieldConverter createConverter(FieldSchemaConfiguration newConfiguration) {
        return new StringFieldConverter();
    }

    @Override
    public Object createInitializer() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof StringFieldSchemaConfiguration))
            return false;

        StringFieldSchemaConfiguration configuration = (StringFieldSchemaConfiguration) o;
        return super.equals(configuration) && required == configuration.required && compressed == configuration.compressed &&
                minSize == configuration.minSize && maxSize == configuration.maxSize &&
                Objects.equals(pattern, configuration.pattern) && Objects.equals(enumeration, configuration.enumeration) &&
                Objects.equals(sequenceField, configuration.sequenceField);
    }

    @Override
    public boolean equalsStructured(FieldSchemaConfiguration newSchema) {
        if (!(newSchema instanceof StringFieldSchemaConfiguration))
            return false;

        StringFieldSchemaConfiguration configuration = (StringFieldSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration) && compressed == configuration.compressed &&
                minSize <= configuration.minSize && maxSize >= configuration.maxSize;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(required, compressed, minSize, maxSize, pattern, enumeration, sequenceField);
    }
}
