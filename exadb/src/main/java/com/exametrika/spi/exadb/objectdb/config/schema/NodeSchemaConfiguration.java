/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.exadb.objectdb.config.schema;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.api.exadb.objectdb.schema.INodeSchema;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.InvalidArgumentException;
import com.exametrika.common.utils.Memory;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.exadb.core.Constants;
import com.exametrika.impl.exadb.objectdb.Node;
import com.exametrika.spi.exadb.core.config.schema.SchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.INodeObject;
import com.exametrika.spi.exadb.objectdb.fields.IField;

/**
 * The {@link NodeSchemaConfiguration} represents a configuration of schema of node.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class NodeSchemaConfiguration extends SchemaConfiguration {
    private static final IMessages messages = Messages.get(IMessages.class);
    private final List<FieldSchemaConfiguration> fields;
    private final Map<String, FieldSchemaConfiguration> fieldsMap;
    private final Map<String, FieldSchemaConfiguration> fieldsByAliasMap;
    private final int size;
    private final int cacheSize;
    private final boolean fullTextIndexed;
    private final boolean hasFullTextIndex;
    private final String documentType;

    public NodeSchemaConfiguration(String name, List<? extends FieldSchemaConfiguration> fields) {
        this(name, name, null, fields, null);
    }

    public NodeSchemaConfiguration(String name, String alias, String description, List<? extends FieldSchemaConfiguration> fields,
                                   String documentType) {
        super(name, alias, description);

        Assert.notNull(fields);

        List<FieldSchemaConfiguration> fieldList = new ArrayList<FieldSchemaConfiguration>(fields);
        Map<String, FieldSchemaConfiguration> fieldsMap = new HashMap<String, FieldSchemaConfiguration>();
        Map<String, FieldSchemaConfiguration> fieldsByAliasMap = new HashMap<String, FieldSchemaConfiguration>();
        for (FieldSchemaConfiguration field : fields) {
            Assert.isNull(fieldsMap.put(field.getName(), field));
            Assert.isNull(fieldsByAliasMap.put(field.getAlias(), field));

            List<FieldSchemaConfiguration> additionalFields = field.getAdditionalFields();
            if (additionalFields != null) {
                for (FieldSchemaConfiguration additionalField : additionalFields) {
                    fieldList.add(additionalField);
                    Assert.isNull(fieldsMap.put(additionalField.getName(), additionalField));
                    Assert.isNull(fieldsByAliasMap.put(additionalField.getAlias(), additionalField));
                }
            }
        }

        int size = Node.HEADER_SIZE;
        int cacheSize = Memory.getShallowSize(getNodeClass()) + Memory.getShallowSize(IField[].class, fieldList.size());
        FieldSchemaConfiguration primaryField = null;
        boolean fullTextIndexed = false;
        boolean hasFullTextIndex = false;
        for (FieldSchemaConfiguration field : fieldList) {
            if (field.isPrimary()) {
                if (primaryField == null)
                    primaryField = field;
                else
                    throw new InvalidArgumentException(messages.multiplePrimaryKeys(name));
            }

            size += field.getSize();
            cacheSize += field.getCacheSize() + 4;

            if (field.isFullTextIndexed())
                fullTextIndexed = true;
            if (field.hasFullTextIndex())
                hasFullTextIndex = true;
        }

        Assert.isTrue(size <= Constants.MAX_NODE_SIZE);

        this.fields = Immutables.wrap(fieldList);
        this.fieldsMap = fieldsMap;
        this.fieldsByAliasMap = fieldsByAliasMap;
        this.size = size;
        this.cacheSize = cacheSize;
        this.fullTextIndexed = fullTextIndexed;
        this.hasFullTextIndex = hasFullTextIndex;
        this.documentType = documentType;
    }

    public final int getSize() {
        return size;
    }

    public final int getCacheSize() {
        return cacheSize;
    }

    public final List<FieldSchemaConfiguration> getFields() {
        return fields;
    }

    public final FieldSchemaConfiguration findField(String name) {
        Assert.notNull(name);

        return fieldsMap.get(name);
    }

    public final FieldSchemaConfiguration findFieldByAlias(String alias) {
        Assert.notNull(alias);

        return fieldsByAliasMap.get(alias);
    }

    public String getDocumentType() {
        return documentType;
    }

    public boolean isFullTextIndexed() {
        return fullTextIndexed;
    }

    public boolean hasFullTextIndex() {
        return hasFullTextIndex;
    }

    public abstract INodeSchema createSchema(int index, List<IFieldSchema> fields, IDocumentSchema documentSchema);

    public abstract INodeObject createNode(INode node);

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof NodeSchemaConfiguration))
            return false;

        NodeSchemaConfiguration configuration = (NodeSchemaConfiguration) o;
        return super.equals(configuration) && fields.equals(configuration.fields) && Objects.equals(documentType, configuration.documentType);
    }

    public boolean equalsStructured(NodeSchemaConfiguration newSchema) {
        if (fields.size() != newSchema.fields.size())
            return false;
        for (int i = 0; i < fields.size(); i++) {
            if (!fields.get(i).equalsStructured(newSchema.fields.get(i)))
                return false;
        }

        return getName().equals(newSchema.getName()) && Objects.equals(documentType, newSchema.documentType);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(fields, documentType);
    }

    protected abstract Class getNodeClass();

    private interface IMessages {
        @DefaultMessage("Multiple primary keys are defined in node schema ''{0}''.")
        ILocalizedMessage multiplePrimaryKeys(String name);
    }
}
