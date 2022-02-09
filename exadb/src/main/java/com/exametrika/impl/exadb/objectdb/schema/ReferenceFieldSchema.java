/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.schema;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.exametrika.api.exadb.objectdb.config.schema.ReferenceFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.SingleReferenceFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.api.exadb.objectdb.schema.INodeSchema;
import com.exametrika.api.exadb.objectdb.schema.IObjectSpaceSchema;
import com.exametrika.api.exadb.objectdb.schema.IReferenceFieldSchema;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.InvalidArgumentException;
import com.exametrika.impl.exadb.objectdb.fields.ReferenceField;
import com.exametrika.spi.exadb.objectdb.fields.IComplexField;
import com.exametrika.spi.exadb.objectdb.fields.IField;
import com.exametrika.spi.exadb.objectdb.fields.IFieldObject;

/**
 * The {@link ReferenceFieldSchema} is a reference field schema.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class ReferenceFieldSchema extends ComplexFieldSchema implements IReferenceFieldSchema {
    private static final IMessages messages = Messages.get(IMessages.class);
    private IReferenceFieldSchema fieldReference;
    private Set<INodeSchema> nodeReferences;
    private IObjectSpaceSchema externalSpaceSchema;

    public ReferenceFieldSchema(ReferenceFieldSchemaConfiguration configuration, int index, int offset) {
        super(configuration, index, offset);
    }

    @Override
    public ReferenceFieldSchemaConfiguration getConfiguration() {
        return (ReferenceFieldSchemaConfiguration) configuration;
    }

    @Override
    public IReferenceFieldSchema getFieldReference() {
        return fieldReference;
    }

    @Override
    public Set<INodeSchema> getNodeReferences() {
        return nodeReferences;
    }

    @Override
    public IObjectSpaceSchema getExternalSpaceSchema() {
        return externalSpaceSchema;
    }

    @Override
    public void resolveDependencies() {
        fieldReference = null;
        nodeReferences = null;

        List<INodeSchema> nodes;
        if (getConfiguration().getExternalSpaceName() != null) {
            externalSpaceSchema = getRoot().findSchemaById("space:" + getConfiguration().getExternalSpaceName());
            Assert.notNull(externalSpaceSchema);
            nodes = externalSpaceSchema.getNodes();
        } else {
            externalSpaceSchema = null;
            nodes = getParent().getParent().getNodes();
        }

        ReferenceFieldSchemaConfiguration configuration = getConfiguration();
        if (configuration.getFieldReference() != null) {
            String nodeType, fieldName;
            int pos = configuration.getFieldReference().lastIndexOf('.');
            if (pos != -1) {
                nodeType = configuration.getFieldReference().substring(0, pos);
                fieldName = configuration.getFieldReference().substring(pos + 1, configuration.getFieldReference().length());
            } else
                throw new InvalidArgumentException(messages.invalidFieldReference(
                        parent.getConfiguration().getName() + "." + configuration.getName(), configuration.getFieldReference()));

            for (INodeSchema nodeSchema : nodes) {
                if (nodeSchema.getConfiguration().getName().equals(nodeType)) {
                    for (IFieldSchema fieldSchema : nodeSchema.getFields()) {
                        if (fieldSchema.getConfiguration().getName().equals(fieldName)) {
                            fieldReference = (IReferenceFieldSchema) fieldSchema;
                            break;
                        }
                    }
                    break;
                }
            }

            if (fieldReference == null)
                throw new InvalidArgumentException(messages.referenceNotFound(
                        parent.getConfiguration().getName() + "." + configuration.getName(), configuration.getFieldReference()));

            if (fieldReference.getConfiguration() instanceof SingleReferenceFieldSchemaConfiguration) {
                SingleReferenceFieldSchemaConfiguration referenceConfiguration = (SingleReferenceFieldSchemaConfiguration) fieldReference.getConfiguration();
                if (!referenceConfiguration.isBidirectional())
                    throw new InvalidArgumentException(messages.referenceNotBidirectional(
                            parent.getConfiguration().getName() + "." + configuration.getName(), configuration.getFieldReference()));
                if (!referenceConfiguration.getFieldReference().equals(parent.getConfiguration().getName() + "." + configuration.getName()))
                    throw new InvalidArgumentException(messages.bidirectionalReferenceNotCircular(
                            parent.getConfiguration().getName() + "." + configuration.getName(), configuration.getFieldReference()));
            } else {
                ReferenceFieldSchemaConfiguration referenceConfiguration = (ReferenceFieldSchemaConfiguration) fieldReference.getConfiguration();
                if (!referenceConfiguration.isBidirectional())
                    throw new InvalidArgumentException(messages.referenceNotBidirectional(
                            parent.getConfiguration().getName() + "." + configuration.getName(), configuration.getFieldReference()));
                if (!referenceConfiguration.getFieldReference().equals(parent.getConfiguration().getName() + "." + configuration.getName()))
                    throw new InvalidArgumentException(messages.bidirectionalReferenceNotCircular(
                            parent.getConfiguration().getName() + "." + configuration.getName(), configuration.getFieldReference()));
            }
        } else if (configuration.getNodeReferences() != null) {
            for (String referenceType : configuration.getNodeReferences()) {
                INodeSchema nodeReference = null;
                for (INodeSchema nodeSchema : nodes) {
                    if (nodeSchema.getConfiguration().getName().equals(referenceType)) {
                        nodeReference = nodeSchema;
                        break;
                    }
                }

                if (nodeReference != null) {
                    if (nodeReferences == null)
                        nodeReferences = new LinkedHashSet<INodeSchema>();

                    nodeReferences.add(nodeReference);
                } else
                    throw new InvalidArgumentException(messages.referenceNotFound(
                            parent.getConfiguration().getName() + "." + configuration.getName(), referenceType));
            }

            nodeReferences = Immutables.wrap(nodeReferences);
        }
    }

    @Override
    public IFieldObject createField(IField field) {
        return new ReferenceField((IComplexField) field);
    }

    @Override
    public void validate(IField field) {
    }

    private interface IMessages {
        @DefaultMessage("Reference ''{1}'' of field ''{0}'' is not found.")
        ILocalizedMessage referenceNotFound(String fieldName, String reference);

        @DefaultMessage("Reference ''{1}'' of field ''{0}'' is not valid.")
        ILocalizedMessage invalidFieldReference(String fieldName, String reference);

        @DefaultMessage("Value of required field ''{0}'' is not set.")
        ILocalizedMessage valueRequired(IFieldSchema schema);

        @DefaultMessage("Reference ''{1}'' of bidirectional reference field ''{0}'' is not bidirectional.")
        ILocalizedMessage referenceNotBidirectional(String fieldName, String reference);

        @DefaultMessage("Reference ''{1}'' of bidirectional reference field ''{0}'' must reference to the original field ''{0}''.")
        ILocalizedMessage bidirectionalReferenceNotCircular(String fieldName, String reference);
    }
}
