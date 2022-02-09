/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.objectdb.config.schema;

import java.util.Collections;
import java.util.Set;

import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Memory;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.exadb.core.Constants;
import com.exametrika.impl.exadb.objectdb.fields.JsonField;
import com.exametrika.impl.exadb.objectdb.fields.JsonFieldConverter;
import com.exametrika.impl.exadb.objectdb.schema.JsonFieldSchema;
import com.exametrika.spi.exadb.objectdb.config.schema.ComplexFieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.JsonConverterSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.JsonValidatorSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.fields.IFieldConverter;


/**
 * The {@link JsonFieldSchemaConfiguration} represents a configuration of schema of JSON field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class JsonFieldSchemaConfiguration extends ComplexFieldSchemaConfiguration {
    private final String jsonSchema;
    private final Set<? extends JsonValidatorSchemaConfiguration> validators;
    private final Set<? extends JsonConverterSchemaConfiguration> converters;
    private final String typeName;
    private final boolean required;
    private final boolean compressed;

    public JsonFieldSchemaConfiguration(String name) {
        this(name, name, null, null, Collections.<JsonValidatorSchemaConfiguration>emptySet(),
                Collections.<JsonConverterSchemaConfiguration>emptySet(), null, false, true);
    }

    public JsonFieldSchemaConfiguration(String name, String alias, String description, String jsonSchema,
                                        Set<? extends JsonValidatorSchemaConfiguration> validators, Set<? extends JsonConverterSchemaConfiguration> converters,
                                        String typeName, boolean required, boolean compressed) {
        super(name, alias, description, Constants.COMPLEX_FIELD_AREA_DATA_SIZE, Memory.getShallowSize(JsonField.class));

        Assert.notNull(validators);
        Assert.notNull(converters);
        Assert.isTrue((jsonSchema != null) == (typeName != null));

        this.jsonSchema = jsonSchema;
        this.validators = Immutables.wrap(validators);
        this.converters = Immutables.wrap(converters);
        this.typeName = typeName;
        this.required = required;
        this.compressed = compressed;
    }

    public String getJsonSchema() {
        return jsonSchema;
    }

    public Set<? extends JsonValidatorSchemaConfiguration> getValidators() {
        return validators;
    }

    public Set<? extends JsonConverterSchemaConfiguration> getConverters() {
        return converters;
    }

    public String getTypeName() {
        return typeName;
    }

    public boolean isRequired() {
        return required;
    }

    public boolean isCompressed() {
        return compressed;
    }

    @Override
    public IFieldSchema createSchema(int index, int offset, int indexTotalIndex) {
        return new JsonFieldSchema(this, index, offset);
    }

    @Override
    public boolean isCompatible(FieldSchemaConfiguration newConfiguration) {
        return newConfiguration instanceof JsonFieldSchemaConfiguration;
    }

    @Override
    public IFieldConverter createConverter(FieldSchemaConfiguration newConfiguration) {
        return new JsonFieldConverter();
    }

    @Override
    public Object createInitializer() {
        return null;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof JsonFieldSchemaConfiguration))
            return false;

        JsonFieldSchemaConfiguration configuration = (JsonFieldSchemaConfiguration) o;
        return super.equals(configuration) && Objects.equals(jsonSchema, configuration.jsonSchema) &&
                validators.equals(configuration.validators) && converters.equals(configuration.converters) &&
                Objects.equals(typeName, configuration.typeName) && required == configuration.required && compressed == configuration.compressed;
    }

    @Override
    public boolean equalsStructured(FieldSchemaConfiguration newSchema) {
        if (!(newSchema instanceof JsonFieldSchemaConfiguration))
            return false;

        JsonFieldSchemaConfiguration configuration = (JsonFieldSchemaConfiguration) newSchema;
        return super.equalsStructured(configuration) && compressed == configuration.compressed;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(jsonSchema, validators, converters, typeName, required, compressed);
    }
}
