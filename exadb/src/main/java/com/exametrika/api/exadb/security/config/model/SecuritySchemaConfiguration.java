/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.security.config.model;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.exadb.core.config.schema.SchemaConfiguration;
import com.exametrika.spi.exadb.security.config.model.CheckPermissionStrategySchemaConfiguration;
import com.exametrika.spi.exadb.security.config.model.RoleMappingStrategySchemaConfiguration;


/**
 * The {@link SecuritySchemaConfiguration} is a security schema configuration.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public final class SecuritySchemaConfiguration extends SchemaConfiguration {
    public static final String SCHEMA = "com.exametrika.exadb.security-1.0";
    private final Set<RoleSchemaConfiguration> roles;
    private final Map<String, RoleSchemaConfiguration> rolesMap;
    private final RoleMappingStrategySchemaConfiguration roleMappingStrategy;
    private final CheckPermissionStrategySchemaConfiguration checkPermissionStrategy;
    private final boolean auditEnabled;

    public SecuritySchemaConfiguration(Set<RoleSchemaConfiguration> roles, RoleMappingStrategySchemaConfiguration roleMappingStrategy,
                                       CheckPermissionStrategySchemaConfiguration checkPermissionStrategy, boolean auditEnabled) {
        super("SecurityModel", "SecurityModel", "Security model schema");
        Assert.notNull(roles);

        this.roles = Immutables.wrap(roles);
        this.roleMappingStrategy = roleMappingStrategy;
        this.checkPermissionStrategy = checkPermissionStrategy;
        this.auditEnabled = auditEnabled;

        Map<String, RoleSchemaConfiguration> rolesMap = new LinkedHashMap<String, RoleSchemaConfiguration>();
        for (RoleSchemaConfiguration role : roles)
            Assert.isNull(rolesMap.put(role.getName(), role));

        this.rolesMap = rolesMap;
    }

    public Set<RoleSchemaConfiguration> getRoles() {
        return roles;
    }

    public RoleSchemaConfiguration findRole(String roleName) {
        return rolesMap.get(roleName);
    }

    public RoleMappingStrategySchemaConfiguration getRoleMappingStrategy() {
        return roleMappingStrategy;
    }

    public CheckPermissionStrategySchemaConfiguration getCheckPermissionStrategy() {
        return checkPermissionStrategy;
    }

    public boolean isAuditEnabled() {
        return auditEnabled;
    }

    @Override
    public <T extends SchemaConfiguration> T combine(T schema) {
        SecuritySchemaConfiguration securitySchema = (SecuritySchemaConfiguration) schema;

        Set<RoleSchemaConfiguration> roles = new LinkedHashSet<RoleSchemaConfiguration>();
        Map<String, RoleSchemaConfiguration> rolesMap = new LinkedHashMap<String, RoleSchemaConfiguration>(this.rolesMap);
        for (RoleSchemaConfiguration role : securitySchema.getRoles())
            roles.add(combine(role, rolesMap));
        roles.addAll(rolesMap.values());

        return (T) new SecuritySchemaConfiguration(roles, combine(roleMappingStrategy, securitySchema.roleMappingStrategy),
                combine(checkPermissionStrategy, securitySchema.checkPermissionStrategy), combine(auditEnabled, securitySchema.auditEnabled));
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof SecuritySchemaConfiguration))
            return false;

        SecuritySchemaConfiguration configuration = (SecuritySchemaConfiguration) o;
        return super.equals(configuration) && roles.equals(configuration.roles) &&
                Objects.equals(roleMappingStrategy, configuration.roleMappingStrategy) &&
                Objects.equals(checkPermissionStrategy, configuration.checkPermissionStrategy) && auditEnabled == configuration.auditEnabled;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(roles, roleMappingStrategy, checkPermissionStrategy, auditEnabled);
    }
}