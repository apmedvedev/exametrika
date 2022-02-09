/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.schema;

import java.util.HashSet;
import java.util.Set;

import com.exametrika.api.exadb.objectdb.config.schema.SerializableFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.fields.ISerializableField;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.rawdb.RawRollbackException;
import com.exametrika.common.utils.Classes;
import com.exametrika.common.utils.Serializers;
import com.exametrika.impl.exadb.objectdb.fields.SerializableField;
import com.exametrika.spi.exadb.objectdb.fields.IComplexField;
import com.exametrika.spi.exadb.objectdb.fields.IField;
import com.exametrika.spi.exadb.objectdb.fields.IFieldObject;

/**
 * The {@link SerializableFieldSchema} is a serializable field schema.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class SerializableFieldSchema extends ComplexFieldSchema implements IFieldSchema {
    private static final IMessages messages = Messages.get(IMessages.class);
    private final ISerializationRegistry serializationRegistry;
    private final Set<Class> allowedClasses;

    public SerializableFieldSchema(SerializableFieldSchemaConfiguration configuration, int index, int offset) {
        super(configuration, index, offset);

        serializationRegistry = Serializers.createRegistry();

        if (configuration.getAllowedClasses() != null) {
            Set<Class> allowedClasses = new HashSet<Class>();
            for (String allowedClass : configuration.getAllowedClasses())
                allowedClasses.add(Classes.forName(allowedClass));
            this.allowedClasses = allowedClasses;
        } else
            allowedClasses = null;
    }

    public ISerializationRegistry getSerializationRegistry() {
        return serializationRegistry;
    }

    public Set<Class> getAllowedClasses() {
        return allowedClasses;
    }

    @Override
    public SerializableFieldSchemaConfiguration getConfiguration() {
        return (SerializableFieldSchemaConfiguration) configuration;
    }

    @Override
    public IFieldObject createField(IField field) {
        return new SerializableField((IComplexField) field);
    }

    @Override
    public void validate(IField field) {
        if (getConfiguration().isRequired()) {
            ISerializableField serializableField = field.getObject();
            if (serializableField.get() == null)
                throw new RawRollbackException(messages.valueRequired(this));
        }
    }

    private interface IMessages {
        @DefaultMessage("Value of required field ''{0}'' is not set.")
        ILocalizedMessage valueRequired(IFieldSchema schema);
    }
}
