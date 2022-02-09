/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.schema;

import java.util.UUID;

import com.exametrika.api.exadb.objectdb.config.schema.UuidFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.fields.IUuidField;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.rawdb.RawRollbackException;
import com.exametrika.impl.exadb.objectdb.fields.UuidField;
import com.exametrika.spi.exadb.objectdb.fields.IField;
import com.exametrika.spi.exadb.objectdb.fields.IFieldObject;
import com.exametrika.spi.exadb.objectdb.fields.ISimpleField;


/**
 * The {@link UuidFieldSchema} is a UUID field schema.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class UuidFieldSchema extends SimpleFieldSchema implements IFieldSchema {
    private static final IMessages messages = Messages.get(IMessages.class);
    private final int indexTotalIndex;

    public UuidFieldSchema(UuidFieldSchemaConfiguration configuration, int index, int offset, int indexTotalIndex) {
        super(configuration, index, offset);

        this.indexTotalIndex = indexTotalIndex;
    }

    @Override
    public int getIndexTotalIndex() {
        return indexTotalIndex;
    }

    @Override
    public UuidFieldSchemaConfiguration getConfiguration() {
        return (UuidFieldSchemaConfiguration) configuration;
    }

    @Override
    public IFieldObject createField(IField field) {
        return new UuidField((ISimpleField) field);
    }

    @Override
    public void validate(IField field) {
        UuidFieldSchemaConfiguration configuration = getConfiguration();

        IUuidField uuidField = field.getObject();
        UUID value = uuidField.get();

        if (configuration.isRequired() && value == null)
            throw new RawRollbackException(messages.valueRequired(this));
    }

    private interface IMessages {
        @DefaultMessage("Value of required field ''{0}'' is not set.")
        ILocalizedMessage valueRequired(IFieldSchema schema);
    }
}
