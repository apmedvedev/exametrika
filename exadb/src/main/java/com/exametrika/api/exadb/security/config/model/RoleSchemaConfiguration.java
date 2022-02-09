/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.security.config.model;

import java.util.LinkedHashSet;
import java.util.Set;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.exadb.core.config.schema.SchemaConfiguration;


/**
 * The {@link RoleSchemaConfiguration} is a role schema configuration.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public final class RoleSchemaConfiguration extends SchemaConfiguration {
    private final Set<String> permissionPatterns;
    private final boolean administrator;

    public RoleSchemaConfiguration(String name, Set<String> permissionPatterns, boolean administrator) {
        super(name, name, null);

        Assert.notNull(permissionPatterns);

        this.permissionPatterns = Immutables.wrap(permissionPatterns);
        this.administrator = administrator;
    }

    public Set<String> getPermissionPatterns() {
        return permissionPatterns;
    }

    public boolean isAdministrator() {
        return administrator;
    }

    @Override
    public <T extends SchemaConfiguration> T combine(T schema) {
        RoleSchemaConfiguration role = (RoleSchemaConfiguration) schema;
        Set<String> permissionPatterns = new LinkedHashSet<String>(this.permissionPatterns);
        permissionPatterns.addAll(role.permissionPatterns);

        return (T) new RoleSchemaConfiguration(getName(), permissionPatterns,
                combine(administrator, role.administrator));
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof RoleSchemaConfiguration))
            return false;

        RoleSchemaConfiguration configuration = (RoleSchemaConfiguration) o;
        return super.equals(configuration) && permissionPatterns.equals(configuration.permissionPatterns) &&
                administrator == configuration.administrator;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(permissionPatterns, administrator);
    }
}