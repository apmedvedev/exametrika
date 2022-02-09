/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.core.schema;

import java.util.Collections;
import java.util.Map;

import com.exametrika.api.exadb.core.schema.IDatabaseSchema;
import com.exametrika.api.exadb.core.schema.ISchemaObject;
import com.exametrika.common.utils.Assert;


/**
 * The {@link SchemaObject} is a base schema object.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public abstract class SchemaObject implements ISchemaObject {
    private final String type;
    private IDatabaseSchema root;
    private String id;
    private String qualifiedName;
    private String qualifiedAlias;

    public SchemaObject(String type) {
        Assert.notNull(type);

        this.type = type;

        if (this instanceof DatabaseSchema) {
            root = (IDatabaseSchema) this;
            id = IDatabaseSchema.TYPE;
            qualifiedAlias = "";
            qualifiedName = "";
        }
    }

    public void setParent(ISchemaObject parent, Map<String, ISchemaObject> schemaObjects) {
        Assert.notNull(parent);
        Assert.notNull(parent.getRoot());
        Assert.notNull(parent.getQualifiedName());

        this.root = parent.getRoot();
        if (parent.getParent() != null) {
            this.qualifiedName = parent.getQualifiedName() + "." + getConfiguration().getName();
            this.qualifiedAlias = parent.getQualifiedAlias() + "." + getConfiguration().getAlias();
        } else {
            this.qualifiedName = getConfiguration().getName();
            this.qualifiedAlias = getConfiguration().getAlias();
        }

        id = type + ":" + qualifiedName;
        schemaObjects.put(id, this);
    }

    public void resolveDependencies() {
    }

    @Override
    public final String getId() {
        return id;
    }

    @Override
    public final String getQualifiedName() {
        return qualifiedName;
    }

    @Override
    public final String getQualifiedAlias() {
        return qualifiedAlias;
    }

    @Override
    public final String getType() {
        return type;
    }

    @Override
    public final IDatabaseSchema getRoot() {
        return root;
    }

    @Override
    public Iterable<ISchemaObject> getChildren() {
        return Collections.emptyList();
    }

    @Override
    public Iterable<ISchemaObject> getChildren(String type) {
        return Collections.emptyList();
    }

    @Override
    public <T extends ISchemaObject> T findChild(String type, String name) {
        return null;
    }

    @Override
    public <T extends ISchemaObject> T findChildByAlias(String type, String alias) {
        return null;
    }

    @Override
    public final String toString() {
        return type + ":" + qualifiedAlias;
    }
}
