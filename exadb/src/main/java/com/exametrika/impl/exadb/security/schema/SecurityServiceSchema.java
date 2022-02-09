/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.security.schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.exametrika.api.exadb.core.schema.IDomainSchema;
import com.exametrika.api.exadb.core.schema.ISchemaObject;
import com.exametrika.api.exadb.security.IPermission;
import com.exametrika.api.exadb.security.config.model.SecuritySchemaConfiguration;
import com.exametrika.api.exadb.security.config.schema.SecurityServiceSchemaConfiguration;
import com.exametrika.api.exadb.security.schema.ISecurityServiceSchema;
import com.exametrika.impl.exadb.core.schema.DomainServiceSchema;
import com.exametrika.impl.exadb.security.Permission;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.spi.exadb.security.ICheckPermissionStrategy;
import com.exametrika.spi.exadb.security.IRoleMappingStrategy;


/**
 * The {@link SecurityServiceSchema} represents a schema of security service.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class SecurityServiceSchema extends DomainServiceSchema implements ISecurityServiceSchema {
    private IRoleMappingStrategy roleMappingStrategy;
    private ICheckPermissionStrategy checkPermissionStrategy;
    private final List<Permission> permissions = new ArrayList<Permission>();

    public SecurityServiceSchema(IDatabaseContext context, SecurityServiceSchemaConfiguration configuration) {
        super(context, configuration);
    }

    @Override
    public void setParent(IDomainSchema parent, Map<String, ISchemaObject> schemaObjects) {
        super.setParent(parent, schemaObjects);

        permissions.clear();
    }

    @Override
    public SecurityServiceSchemaConfiguration getConfiguration() {
        return (SecurityServiceSchemaConfiguration) super.getConfiguration();
    }

    public List<Permission> getPermissions() {
        return permissions;
    }

    public IRoleMappingStrategy getRoleMappingStrategy() {
        return roleMappingStrategy;
    }

    public ICheckPermissionStrategy getCheckPermissionStrategy() {
        return checkPermissionStrategy;
    }

    @Override
    public IPermission createPermission(String name, boolean auditable) {
        Permission permission = new Permission(name, permissions.size(), auditable);
        permissions.add(permission);
        return permission;
    }

    @Override
    public void resolveDependencies() {
        super.resolveDependencies();

        SecuritySchemaConfiguration configuration = getConfiguration().getSecurityModel();
        if (configuration.getCheckPermissionStrategy() != null)
            checkPermissionStrategy = configuration.getCheckPermissionStrategy().createStrategy(context);
        else
            checkPermissionStrategy = null;
        if (configuration.getRoleMappingStrategy() != null)
            roleMappingStrategy = configuration.getRoleMappingStrategy().createStrategy(context);
        else
            roleMappingStrategy = null;
    }
}
