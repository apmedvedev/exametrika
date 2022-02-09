/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.security.config.schema;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.exametrika.api.exadb.security.config.model.RoleSchemaConfiguration;
import com.exametrika.api.exadb.security.config.model.ScheduleRoleMappingStrategySchemaConfiguration;
import com.exametrika.api.exadb.security.config.model.SecuritySchemaConfiguration;
import com.exametrika.common.config.AbstractExtensionLoader;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.config.InvalidConfigurationException;
import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.spi.exadb.security.config.model.CheckPermissionStrategySchemaConfiguration;
import com.exametrika.spi.exadb.security.config.model.RoleMappingStrategySchemaConfiguration;


/**
 * The {@link SecuritySchemaLoader} is a configuration loader for security schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class SecuritySchemaLoader extends AbstractExtensionLoader {
    @Override
    public Object loadExtension(String name, String type, Object object, ILoadContext context) {
        JsonObject element = (JsonObject) object;
        if (type.equals("Security")) {
            Set<RoleSchemaConfiguration> roles = loadRoles((JsonObject) element.get("roles"));
            RoleMappingStrategySchemaConfiguration roleMappingStrategy = loadRoleMappingStrategy(
                    (JsonObject) element.get("roleMappingStrategy", null), context);
            CheckPermissionStrategySchemaConfiguration checkPermissionStrategy = loadCheckPermissionStrategy(
                    (JsonObject) element.get("checkPermissionStrategy", null), context);
            boolean auditEnabled = element.get("auditEnabled");
            return new SecuritySchemaConfiguration(roles, roleMappingStrategy, checkPermissionStrategy, auditEnabled);
        } else
            throw new InvalidConfigurationException();
    }

    private Set<RoleSchemaConfiguration> loadRoles(JsonObject element) {
        Set<RoleSchemaConfiguration> roles = new LinkedHashSet<RoleSchemaConfiguration>();
        for (Map.Entry<String, Object> entry : element) {
            JsonObject child = (JsonObject) entry.getValue();
            roles.add(new RoleSchemaConfiguration(entry.getKey(), JsonUtils.<String>toSet((JsonArray) child.get("permissionPatterns")),
                    (Boolean) child.get("administrator")));
        }

        return roles;
    }

    private RoleMappingStrategySchemaConfiguration loadRoleMappingStrategy(JsonObject element, ILoadContext context) {
        if (element == null)
            return null;

        String type = getType(element);
        if (type.equals("ScheduleRoleMappingStrategy"))
            return new ScheduleRoleMappingStrategySchemaConfiguration();
        else
            return load(null, null, element, context);
    }

    private CheckPermissionStrategySchemaConfiguration loadCheckPermissionStrategy(JsonObject element, ILoadContext context) {
        return load(null, null, element, context);
    }
}
