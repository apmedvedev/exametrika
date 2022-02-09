/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.schema;

import java.util.Set;
import java.util.regex.Pattern;

import com.exametrika.api.exadb.objectdb.config.schema.StringFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.fields.IStringField;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.rawdb.RawRollbackException;
import com.exametrika.common.utils.InvalidArgumentException;
import com.exametrika.common.utils.Strings;
import com.exametrika.impl.exadb.objectdb.fields.StringField;
import com.exametrika.spi.exadb.objectdb.fields.IComplexField;
import com.exametrika.spi.exadb.objectdb.fields.IField;
import com.exametrika.spi.exadb.objectdb.fields.IFieldObject;

/**
 * The {@link StringFieldSchema} is a string field schema.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class StringFieldSchema extends ComplexFieldSchema implements IFieldSchema {
    private static final IMessages messages = Messages.get(IMessages.class);
    private final int indexTotalIndex;
    private Pattern pattern;
    private IFieldSchema sequenceField;

    public StringFieldSchema(StringFieldSchemaConfiguration configuration, int index, int offset, int indexTotalIndex) {
        super(configuration, index, offset);

        this.indexTotalIndex = indexTotalIndex;
    }

    public IFieldSchema getSequenceField() {
        return sequenceField;
    }

    @Override
    public int getIndexTotalIndex() {
        return indexTotalIndex;
    }

    @Override
    public StringFieldSchemaConfiguration getConfiguration() {
        return (StringFieldSchemaConfiguration) configuration;
    }

    @Override
    public void resolveDependencies() {
        super.resolveDependencies();

        sequenceField = null;
        String sequenceField = getConfiguration().getSequenceField();
        if (sequenceField != null) {
            this.sequenceField = parent.getParent().getRootNode().findField(sequenceField);
            if (this.sequenceField == null)
                throw new InvalidArgumentException(messages.sequenceFieldNotFound(this, sequenceField));
        }
    }

    @Override
    public IFieldObject createField(IField field) {
        return new StringField((IComplexField) field);
    }

    @Override
    public void validate(IField field) {
        StringFieldSchemaConfiguration configuration = getConfiguration();

        IStringField stringField = field.getObject();
        String value = stringField.get();

        if (configuration.isRequired() && value == null)
            throw new RawRollbackException(messages.valueRequired(this));
        if (value != null && value.length() < configuration.getMinSize())
            throw new RawRollbackException(messages.valueTooShort(value, this));
        if (value != null && value.length() > configuration.getMaxSize())
            throw new RawRollbackException(messages.valueTooLong(value, this));
        if (value != null && getConfiguration().getPattern() != null) {
            if (pattern == null)
                compile();

            if (!pattern.matcher(value).matches())
                throw new RawRollbackException(messages.valueNotMatchPattern(value, this, configuration.getPattern()));
        }
        if (value != null && configuration.getEnumeration() != null && !configuration.getEnumeration().contains(value))
            throw new RawRollbackException(messages.valueNotMatchEnumeration(value, this, configuration.getEnumeration()));
    }

    private void compile() {
        if (getConfiguration().getPattern() != null)
            pattern = Strings.createFilterPattern(getConfiguration().getPattern(), false);
    }

    private interface IMessages {
        @DefaultMessage("String sequence field ''{1}'' of root node referenced from field ''{0}'' is not found.")
        ILocalizedMessage sequenceFieldNotFound(IFieldSchema schema, String fieldName);

        @DefaultMessage("Value of required field ''{0}'' is not set.")
        ILocalizedMessage valueRequired(IFieldSchema schema);

        @DefaultMessage("Value ''{0}'' of field ''{1}'' is too short.")
        ILocalizedMessage valueTooShort(String value, IFieldSchema schema);

        @DefaultMessage("Value ''{0}'' of field ''{1}'' is too long.")
        ILocalizedMessage valueTooLong(String value, IFieldSchema schema);

        @DefaultMessage("Value ''{0}'' of field ''{1}'' does not match pattern ''{2}''.")
        ILocalizedMessage valueNotMatchPattern(String value, IFieldSchema schema, String pattern);

        @DefaultMessage("Value ''{0}'' of field ''{1}'' is not contained in enumeration ''{2}''.")
        ILocalizedMessage valueNotMatchEnumeration(String value, IFieldSchema schema, Set<String> enumeration);
    }
}
