/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.schema;

import java.util.Set;

import com.exametrika.api.exadb.objectdb.config.schema.NumericFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.fields.IPrimitiveField;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.rawdb.RawRollbackException;
import com.exametrika.common.utils.InvalidArgumentException;
import com.exametrika.spi.exadb.objectdb.fields.IField;


/**
 * The {@link NumericFieldSchema} is a numeric field schema.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class NumericFieldSchema extends PrimitiveFieldSchema {
    private static final IMessages messages = Messages.get(IMessages.class);
    private IFieldSchema sequenceField;

    public NumericFieldSchema(NumericFieldSchemaConfiguration configuration, int index, int offset, int indexTotalIndex) {
        super(configuration, index, offset, indexTotalIndex);
    }

    @Override
    public IFieldSchema getSequenceField() {
        return sequenceField;
    }

    @Override
    public NumericFieldSchemaConfiguration getConfiguration() {
        return (NumericFieldSchemaConfiguration) configuration;
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
    public void validate(IField field) {
        NumericFieldSchemaConfiguration configuration = getConfiguration();

        IPrimitiveField numericField = field.getObject();
        Number value = numericField.get();

        if (configuration.getMin() != null && ((Comparable) value).compareTo(configuration.getMin()) == -1)
            throw new RawRollbackException(messages.valueTooSmall(value, this));
        if (configuration.getMax() != null && ((Comparable) value).compareTo(configuration.getMax()) == 1)
            throw new RawRollbackException(messages.valueTooBig(value, this));
        if (configuration.getEnumeration() != null && !configuration.getEnumeration().contains(value))
            throw new RawRollbackException(messages.valueNotMatchEnumeration(value, this, configuration.getEnumeration()));
    }

    private interface IMessages {
        @DefaultMessage("Numeric sequence field ''{1}'' of root node referenced from field ''{0}'' is not found.")
        ILocalizedMessage sequenceFieldNotFound(IFieldSchema schema, String fieldName);

        @DefaultMessage("Value ''{0}'' of field ''{1}'' is too small.")
        ILocalizedMessage valueTooSmall(Number value, IFieldSchema schema);

        @DefaultMessage("Value ''{0}'' of field ''{1}'' is too big.")
        ILocalizedMessage valueTooBig(Number value, IFieldSchema schema);

        @DefaultMessage("Value ''{0}'' of field ''{1}'' is not contained in enumeration ''{2}''.")
        ILocalizedMessage valueNotMatchEnumeration(Number value, IFieldSchema schema, Set<Number> enumeration);
    }
}
