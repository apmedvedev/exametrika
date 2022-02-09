/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.schema;

import java.util.List;

import com.exametrika.api.component.config.schema.IncidentNodeSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.api.exadb.security.IPermission;
import com.exametrika.impl.exadb.objectdb.schema.ObjectNodeSchema;
import com.exametrika.spi.exadb.security.Permissions;


/**
 * The {@link IncidentNodeSchema} represents a schema of incident node.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class IncidentNodeSchema extends ObjectNodeSchema {
    private IPermission editPermission;
    private IPermission deletePermission;

    public IncidentNodeSchema(IncidentNodeSchemaConfiguration configuration, int index, List<IFieldSchema> fields,
                              IDocumentSchema fullTextSchema) {
        super(configuration, index, fields, fullTextSchema);
    }

    @Override
    public void resolveDependencies() {
        super.resolveDependencies();

        editPermission = Permissions.permission(this, "incident:edit", true);
        deletePermission = Permissions.permission(this, "incident:delete", true);
    }

    public IPermission getEditPermission() {
        return editPermission;
    }

    public IPermission getDeletePermission() {
        return deletePermission;
    }
}
