/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.schema;

import java.util.List;

import com.exametrika.api.component.config.schema.BehaviorTypeNodeSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.api.exadb.security.IPermission;
import com.exametrika.impl.exadb.objectdb.schema.ObjectNodeSchema;
import com.exametrika.spi.exadb.security.Permissions;


/**
 * The {@link BehaviorTypeNodeSchema} represents a schema of behavior type node.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class BehaviorTypeNodeSchema extends ObjectNodeSchema {
    private IPermission viewPermission;
    private IPermission editPermission;
    private IPermission deletePermission;

    public BehaviorTypeNodeSchema(BehaviorTypeNodeSchemaConfiguration configuration, int index, List<IFieldSchema> fields,
                                  IDocumentSchema fullTextSchema) {
        super(configuration, index, fields, fullTextSchema);
    }

    @Override
    public void resolveDependencies() {
        super.resolveDependencies();

        viewPermission = Permissions.permission(this, "behaviorType:view", true);
        editPermission = Permissions.permission(this, "behaviorType:edit", true);
        deletePermission = Permissions.permission(this, "behaviorType:delete", true);
    }

    public IPermission getViewPermission() {
        return viewPermission;
    }

    public IPermission getEditPermission() {
        return editPermission;
    }

    public IPermission getDeletePermission() {
        return deletePermission;
    }
}
