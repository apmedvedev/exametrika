/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.schema;

import java.util.List;

import com.exametrika.api.component.config.schema.HealthComponentNodeSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.api.exadb.security.IPermission;
import com.exametrika.spi.exadb.security.Permissions;


/**
 * The {@link HealthComponentNodeSchema} represents a schema of health component node.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class HealthComponentNodeSchema extends ComponentNodeSchema {
    private IPermission editMaintenanceModePermission;

    public HealthComponentNodeSchema(HealthComponentNodeSchemaConfiguration configuration, int index, List<IFieldSchema> fields,
                                     IDocumentSchema fullTextSchema) {
        super(configuration, index, fields, fullTextSchema);
    }

    @Override
    public void resolveDependencies() {
        super.resolveDependencies();

        editMaintenanceModePermission = Permissions.permission(this, "component:" +
                getConfiguration().getComponent().getName() + ":edit:maintenanceMode", true);
    }

    public IPermission getEditMaintenanceModePermission() {
        return editMaintenanceModePermission;
    }
}
