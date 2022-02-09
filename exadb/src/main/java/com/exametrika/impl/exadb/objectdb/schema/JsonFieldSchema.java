/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.schema;

import java.util.HashMap;
import java.util.Map;

import com.exametrika.api.exadb.objectdb.config.schema.JsonFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.fields.IJsonField;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.api.exadb.objectdb.schema.IJsonFieldSchema;
import com.exametrika.common.json.IJsonCollection;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonSerializers;
import com.exametrika.common.json.schema.IJsonConverter;
import com.exametrika.common.json.schema.IJsonValidator;
import com.exametrika.common.json.schema.JsonMetaSchemaFactory;
import com.exametrika.common.json.schema.JsonSchema;
import com.exametrika.common.json.schema.JsonSchemaLoader;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.rawdb.RawRollbackException;
import com.exametrika.impl.exadb.objectdb.fields.JsonField;
import com.exametrika.spi.exadb.objectdb.config.schema.JsonConverterSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.JsonValidatorSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.fields.IComplexField;
import com.exametrika.spi.exadb.objectdb.fields.IField;
import com.exametrika.spi.exadb.objectdb.fields.IFieldObject;

/**
 * The {@link JsonFieldSchema} is a JSON field schema.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class JsonFieldSchema extends ComplexFieldSchema implements IJsonFieldSchema {
    private static final IMessages messages = Messages.get(IMessages.class);
    private final JsonSchema jsonSchema;

    public JsonFieldSchema(JsonFieldSchemaConfiguration configuration, int index, int offset) {
        super(configuration, index, offset);

        Map<String, IJsonValidator> validators = new HashMap<String, IJsonValidator>();
        for (JsonValidatorSchemaConfiguration validator : configuration.getValidators())
            validators.put(validator.getName(), validator.createValidator());

        Map<String, IJsonConverter> converters = new HashMap<String, IJsonConverter>();
        for (JsonConverterSchemaConfiguration converter : configuration.getConverters())
            converters.put(converter.getName(), converter.createConverter());

        JsonSchema metaSchema = new JsonMetaSchemaFactory().createMetaSchema(validators.keySet());
        JsonSchemaLoader loader = new JsonSchemaLoader(validators, converters, true);

        if (configuration.getJsonSchema() != null) {
            JsonObject object = JsonSerializers.read(configuration.getJsonSchema(), false);
            metaSchema.validate(object, "schema");
            jsonSchema = loader.loadSchema(object);
        } else
            jsonSchema = null;
    }

    @Override
    public JsonFieldSchemaConfiguration getConfiguration() {
        return (JsonFieldSchemaConfiguration) configuration;
    }

    @Override
    public IFieldObject createField(IField field) {
        return new JsonField((IComplexField) field);
    }

    @Override
    public JsonSchema getJsonSchema() {
        return jsonSchema;
    }

    @Override
    public void validate(IJsonCollection value) {
        if (jsonSchema != null)
            jsonSchema.validate(value, getConfiguration().getTypeName());
    }

    @Override
    public void validate(IField field) {
        if (getConfiguration().isRequired()) {
            IJsonField jsonField = field.getObject();
            if (jsonField.get() == null)
                throw new RawRollbackException(messages.valueRequired(this));
        }
    }

    private interface IMessages {
        @DefaultMessage("Value of required field ''{0}'' is not set.")
        ILocalizedMessage valueRequired(IFieldSchema schema);
    }
}
