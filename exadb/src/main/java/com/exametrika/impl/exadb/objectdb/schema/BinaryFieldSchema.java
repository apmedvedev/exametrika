/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.schema;

import com.exametrika.api.exadb.objectdb.config.schema.BinaryFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.fields.IBinaryField;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.common.io.ISerializationRegistry;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.rawdb.RawRollbackException;
import com.exametrika.common.utils.Serializers;
import com.exametrika.impl.exadb.objectdb.fields.BinaryField;
import com.exametrika.spi.exadb.objectdb.fields.IField;
import com.exametrika.spi.exadb.objectdb.fields.IFieldObject;
import com.exametrika.spi.exadb.objectdb.fields.ISimpleField;

/**
 * The {@link BinaryFieldSchema} is a binary blob field schema.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class BinaryFieldSchema extends BlobFieldSchema {
    private static final IMessages messages = Messages.get(IMessages.class);
    private final ISerializationRegistry serializationRegistry;

    public BinaryFieldSchema(BinaryFieldSchemaConfiguration configuration, int index, int offset) {
        super(configuration, index, offset);

        serializationRegistry = Serializers.createRegistry();
    }

    @Override
    public BinaryFieldSchemaConfiguration getConfiguration() {
        return (BinaryFieldSchemaConfiguration) configuration;
    }

    public ISerializationRegistry getSerializationRegistry() {
        return serializationRegistry;
    }

    @Override
    public IFieldObject createField(IField field) {
        return new BinaryField((ISimpleField) field);
    }

    @Override
    public void validate(IField field) {
        if (getConfiguration().isRequired()) {
            IBinaryField refField = field.getObject();
            if (refField.getStore() == null)
                throw new RawRollbackException(messages.valueRequired(this));
        }
    }

    private interface IMessages {
        @DefaultMessage("Value of required field ''{0}'' is not set.")
        ILocalizedMessage valueRequired(IFieldSchema schema);
    }
}
