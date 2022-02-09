/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.schema;

import com.exametrika.api.exadb.objectdb.config.schema.TextFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.fields.ITextField;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.rawdb.RawRollbackException;
import com.exametrika.impl.exadb.objectdb.fields.TextField;
import com.exametrika.spi.exadb.objectdb.fields.IField;
import com.exametrika.spi.exadb.objectdb.fields.IFieldObject;
import com.exametrika.spi.exadb.objectdb.fields.ISimpleField;

/**
 * The {@link TextFieldSchema} is a text blob field schema.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class TextFieldSchema extends BlobFieldSchema {
    private static final IMessages messages = Messages.get(IMessages.class);

    public TextFieldSchema(TextFieldSchemaConfiguration configuration, int index, int offset) {
        super(configuration, index, offset);
    }

    @Override
    public TextFieldSchemaConfiguration getConfiguration() {
        return (TextFieldSchemaConfiguration) configuration;
    }

    @Override
    public IFieldObject createField(IField field) {
        return new TextField((ISimpleField) field);
    }

    @Override
    public void validate(IField field) {
        if (getConfiguration().isRequired()) {
            ITextField refField = field.getObject();
            if (refField.getStore() == null)
                throw new RawRollbackException(messages.valueRequired(this));
        }
    }

    private interface IMessages {
        @DefaultMessage("Value of required field ''{0}'' is not set.")
        ILocalizedMessage valueRequired(IFieldSchema schema);
    }
}
