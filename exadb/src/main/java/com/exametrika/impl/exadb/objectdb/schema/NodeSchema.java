/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.schema;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.exametrika.api.exadb.core.schema.ISchemaObject;
import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.config.schema.BodyFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.VersionFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.api.exadb.objectdb.schema.INodeSchema;
import com.exametrika.api.exadb.objectdb.schema.INodeSpaceSchema;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.impl.exadb.core.schema.SchemaObject;
import com.exametrika.impl.exadb.objectdb.Node;
import com.exametrika.spi.exadb.objectdb.config.schema.NodeSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.fields.IField;


/**
 * The {@link NodeSchema} represents a schema of node.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class NodeSchema extends SchemaObject implements INodeSchema {
    private final NodeSchemaConfiguration configuration;
    private final int index;
    private INodeSpaceSchema parent;
    private final IFieldSchema primaryField;
    private final IFieldSchema versionField;
    private final IFieldSchema bodyField;
    private final List<IFieldSchema> fields;
    private final Map<String, IFieldSchema> fieldsMap;
    private final Map<String, IFieldSchema> fieldsByAliasMap;
    private final IDocumentSchema fullTextSchema;

    public NodeSchema(NodeSchemaConfiguration configuration, int index, List<IFieldSchema> fields,
                      IDocumentSchema fullTextSchema) {
        super(TYPE);

        Assert.notNull(configuration);
        Assert.notNull(fields);

        this.configuration = configuration;
        this.index = index;

        Map<String, IFieldSchema> fieldsMap = new HashMap<String, IFieldSchema>();
        Map<String, IFieldSchema> fieldsByAliasMap = new HashMap<String, IFieldSchema>();
        IFieldSchema versionField = null;
        IFieldSchema bodyField = null;
        for (IFieldSchema field : fields) {
            Assert.isNull(fieldsMap.put(field.getConfiguration().getName(), field));
            Assert.isNull(fieldsByAliasMap.put(field.getConfiguration().getAlias(), field));

            if (field.getConfiguration() instanceof VersionFieldSchemaConfiguration) {
                Assert.isNull(versionField);
                versionField = field;
            }

            if (field.getConfiguration() instanceof BodyFieldSchemaConfiguration) {
                Assert.isNull(bodyField);
                bodyField = field;
            }
        }

        this.versionField = versionField;
        this.bodyField = bodyField;

        IFieldSchema primaryField = null;
        for (IFieldSchema field : fields) {
            if (field.getConfiguration().isPrimary())
                primaryField = field;
        }
        this.primaryField = primaryField;
        this.fields = Immutables.wrap(fields);
        this.fieldsMap = fieldsMap;
        this.fieldsByAliasMap = fieldsByAliasMap;
        this.fullTextSchema = fullTextSchema;
    }

    public void setParent(INodeSpaceSchema parent, Map<String, ISchemaObject> schemaObjects) {
        Assert.notNull(parent);

        this.parent = parent;
        super.setParent(parent, schemaObjects);

        for (IFieldSchema field : fields)
            ((FieldSchema) field).setParent(this, schemaObjects);
    }

    @Override
    public void resolveDependencies() {
        super.resolveDependencies();

        for (IFieldSchema field : fields)
            ((FieldSchema) field).resolveDependencies();
    }

    @Override
    public NodeSchemaConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public final int getIndex() {
        return index;
    }

    @Override
    public INodeSpaceSchema getParent() {
        return parent;
    }

    @Override
    public final IFieldSchema getPrimaryField() {
        return primaryField;
    }

    @Override
    public IFieldSchema getVersionField() {
        return versionField;
    }

    @Override
    public IFieldSchema getBodyField() {
        return bodyField;
    }

    @Override
    public final List<IFieldSchema> getFields() {
        return fields;
    }

    @Override
    public final IFieldSchema findField(String name) {
        Assert.notNull(name);

        return fieldsMap.get(name);
    }

    @Override
    public final IFieldSchema findFieldByAlias(String alias) {
        Assert.notNull(alias);

        return fieldsByAliasMap.get(alias);
    }

    @Override
    public IDocumentSchema getFullTextSchema() {
        return fullTextSchema;
    }

    @Override
    public void validate(INode node) {
        for (int i = 0; i < fields.size(); i++) {
            IFieldSchema fieldSchema = fields.get(i);
            IField field = ((Node) node).getFieldInstance(i);

            fieldSchema.validate(field);
        }
    }

    @Override
    public Iterable<ISchemaObject> getChildren() {
        return (Iterable) fields;
    }

    @Override
    public Iterable<ISchemaObject> getChildren(String type) {
        Assert.notNull(type);

        if (type.equals(IFieldSchema.TYPE))
            return (Iterable) fields;
        else
            return Collections.emptyList();
    }

    @Override
    public <T extends ISchemaObject> T findChild(String type, String name) {
        Assert.notNull(type);
        Assert.notNull(name);

        if (type.equals(IFieldSchema.TYPE))
            return (T) fieldsMap.get(name);
        else
            return null;
    }

    @Override
    public <T extends ISchemaObject> T findChildByAlias(String type, String alias) {
        Assert.notNull(type);
        Assert.notNull(alias);

        if (type.equals(IFieldSchema.TYPE))
            return (T) fieldsByAliasMap.get(alias);
        else
            return null;
    }
}
