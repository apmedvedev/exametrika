/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.schema;

import java.util.HashSet;
import java.util.Set;

import com.exametrika.api.exadb.objectdb.config.schema.VariableStructuredBlobFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.fields.IVariableStructuredBlobField;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.rawdb.RawRollbackException;
import com.exametrika.common.utils.Classes;
import com.exametrika.common.utils.Serializers;
import com.exametrika.impl.exadb.objectdb.fields.VariableStructuredBlobField;
import com.exametrika.spi.exadb.objectdb.fields.IField;
import com.exametrika.spi.exadb.objectdb.fields.IFieldObject;
import com.exametrika.spi.exadb.objectdb.fields.ISimpleField;

/**
 * The {@link VariableStructuredBlobFieldSchema} is a variable structured blob field schema.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class VariableStructuredBlobFieldSchema extends BlobFieldSchema {
    private static final IMessages messages = Messages.get(IMessages.class);
    private final ISerializationRegistry serializationRegistry;
    private final Set<Class> allowedClasses;
    private final Class mainClass;

    public VariableStructuredBlobFieldSchema(VariableStructuredBlobFieldSchemaConfiguration configuration, int index, int offset) {
        super(configuration, index, offset);

        if (configuration.hasSerializationRegistry())
            serializationRegistry = Serializers.createRegistry();
        else
            serializationRegistry = null;

        if (configuration.getAllowedClasses() != null) {
            Set<Class> allowedClasses = new HashSet<Class>();
            for (String allowedClass : configuration.getAllowedClasses())
                allowedClasses.add(Classes.forName(allowedClass));
            this.allowedClasses = allowedClasses;

            if (this.allowedClasses.size() == 1)
                mainClass = this.allowedClasses.iterator().next();
            else
                mainClass = null;
        } else {
            allowedClasses = null;
            mainClass = null;
        }
    }

    public ISerializationRegistry getSerializationRegistry() {
        return serializationRegistry;
    }

    public Set<Class> getAllowedClasses() {
        return allowedClasses;
    }

    public Class getMainClass() {
        return mainClass;
    }

    @Override
    public VariableStructuredBlobFieldSchemaConfiguration getConfiguration() {
        return (VariableStructuredBlobFieldSchemaConfiguration) configuration;
    }

    @Override
    public IFieldObject createField(IField field) {
        return new VariableStructuredBlobField((ISimpleField) field);
    }

    @Override
    public void validate(IField field) {
        if (getConfiguration().isRequired()) {
            IVariableStructuredBlobField refField = field.getObject();
            if (refField.getStore() == null)
                throw new RawRollbackException(messages.valueRequired(this));
        }
    }

    private interface IMessages {
        @DefaultMessage("Value of required field ''{0}'' is not set.")
        ILocalizedMessage valueRequired(IFieldSchema schema);
    }
}
