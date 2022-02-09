/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.schema;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.api.exadb.objectdb.schema.INodeSchema;
import com.exametrika.api.exadb.objectdb.schema.IObjectSpaceSchema;
import com.exametrika.api.exadb.objectdb.schema.IReferenceFieldSchema;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.rawdb.RawRollbackException;
import com.exametrika.common.utils.InvalidArgumentException;
import com.exametrika.impl.exadb.objectdb.fields.BlobField;
import com.exametrika.spi.exadb.objectdb.config.schema.BlobFieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.fields.IBlobField;
import com.exametrika.spi.exadb.objectdb.fields.IField;
import com.exametrika.spi.exadb.objectdb.fields.IFieldObject;
import com.exametrika.spi.exadb.objectdb.fields.ISimpleField;
import com.exametrika.spi.exadb.objectdb.schema.IBlobFieldSchema;

/**
 * The {@link BlobFieldSchema} is a blob field schema.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class BlobFieldSchema extends SimpleFieldSchema implements IBlobFieldSchema, IReferenceFieldSchema {
    private static final IMessages messages = Messages.get(IMessages.class);
    private IFieldSchema store;

    public BlobFieldSchema(BlobFieldSchemaConfiguration configuration, int index, int offset) {
        super(configuration, index, offset);
    }

    @Override
    public BlobFieldSchemaConfiguration getConfiguration() {
        return (BlobFieldSchemaConfiguration) configuration;
    }

    @Override
    public IFieldSchema getStore() {
        return store;
    }

    @Override
    public IReferenceFieldSchema getFieldReference() {
        return null;
    }

    @Override
    public Set<INodeSchema> getNodeReferences() {
        return Collections.singleton(store.getParent());
    }

    @Override
    public IObjectSpaceSchema getExternalSpaceSchema() {
        return null;
    }

    @Override
    public void resolveDependencies() {
        List<INodeSchema> nodes = getParent().getParent().getNodes();

        store = null;
        BlobFieldSchemaConfiguration configuration = getConfiguration();
        INodeSchema referenceNodeSchema = null;

        String blobStoreNodeType = "<root>";
        if (configuration.getBlobStoreNodeType() != null) {
            blobStoreNodeType = configuration.getBlobStoreNodeType();
            for (INodeSchema nodeSchema : nodes) {
                if (nodeSchema.getConfiguration().getName().equals(blobStoreNodeType)) {
                    referenceNodeSchema = nodeSchema;
                    break;
                }
            }
        } else {
            referenceNodeSchema = getRootNode();
            if (referenceNodeSchema != null)
                blobStoreNodeType = referenceNodeSchema.getConfiguration().getName();
        }

        if (referenceNodeSchema != null)
            store = referenceNodeSchema.findField(configuration.getBlobStoreFieldName());

        if (store == null)
            throw new InvalidArgumentException(messages.referenceFieldNotFound(
                    parent.getConfiguration().getName() + "." + configuration.getName(), blobStoreNodeType + "." +
                            configuration.getBlobStoreFieldName()));
    }

    @Override
    public IFieldObject createField(IField field) {
        return new BlobField((ISimpleField) field);
    }

    @Override
    public void validate(IField field) {
        if (getConfiguration().isRequired()) {
            IBlobField refField = field.getObject();
            if (refField.get() == null)
                throw new RawRollbackException(messages.valueRequired(this));
        }
    }

    protected INodeSchema getRootNode() {
        return getParent().getParent().getRootNode();
    }

    private interface IMessages {
        @DefaultMessage("Reference field ''{1}'' of field ''{0}'' is not found.")
        ILocalizedMessage referenceFieldNotFound(String fieldName, String referenceFieldName);

        @DefaultMessage("Value of required field ''{0}'' is not set.")
        ILocalizedMessage valueRequired(IFieldSchema schema);
    }
}
