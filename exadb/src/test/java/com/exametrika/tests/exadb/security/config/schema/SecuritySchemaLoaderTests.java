/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.exadb.security.config.schema;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.exametrika.api.exadb.core.config.schema.ModuleSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.IObjectNode;
import com.exametrika.api.exadb.security.config.model.RoleSchemaConfiguration;
import com.exametrika.api.exadb.security.config.model.ScheduleRoleMappingStrategySchemaConfiguration;
import com.exametrika.api.exadb.security.config.model.SecuritySchemaConfiguration;
import com.exametrika.api.exadb.security.config.schema.SecurityServiceSchemaConfiguration;
import com.exametrika.common.config.AbstractExtensionLoader;
import com.exametrika.common.config.IConfigurationLoader.Parameters;
import com.exametrika.common.config.IConfigurationLoaderExtension;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Classes;
import com.exametrika.common.utils.Collections;
import com.exametrika.common.utils.Pair;
import com.exametrika.impl.exadb.core.config.schema.ModuleSchemaLoader;
import com.exametrika.impl.exadb.core.config.schema.SchemaLoadContext;
import com.exametrika.impl.exadb.security.BasePatternCheckPermissionStrategy;
import com.exametrika.impl.exadb.security.BasePrefixCheckPermissionStrategy;
import com.exametrika.impl.exadb.security.config.schema.SecuritySchemaLoader;
import com.exametrika.impl.exadb.security.schema.SecuritySchemaBuilder;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.spi.exadb.security.ICheckPermissionStrategy;
import com.exametrika.spi.exadb.security.config.model.CheckPermissionStrategySchemaConfiguration;

/**
 * The {@link SecuritySchemaLoaderTests} are tests for {@link SecuritySchemaLoader}.
 *
 * @author Medvedev-A
 */
public class SecuritySchemaLoaderTests {
    public static class TestConfigurationExtension implements IConfigurationLoaderExtension {
        @Override
        public Parameters getParameters() {
            Parameters parameters = new Parameters();
            parameters.schemaMappings.put("test.security", new Pair(
                    "classpath:" + Classes.getResourcePath(getClass()) + "/extension.dbschema", false));
            parameters.typeLoaders.put("TestCheckPermissionStrategy", new TestSchemaConfigurationLoader());
            parameters.typeLoaders.put("TestPatternCheckPermissionStrategy", new TestSchemaConfigurationLoader());
            parameters.typeLoaders.put("TestSecurity", new TestSchemaConfigurationLoader());
            return parameters;
        }
    }

    public static class TestSchemaConfigurationLoader extends AbstractExtensionLoader {
        @Override
        public Object loadExtension(String name, String type, Object object, ILoadContext context) {
            JsonObject element = (JsonObject) object;
            if (type.equals("TestCheckPermissionStrategy"))
                return new TestCheckPermissionStrategySchemaConfiguration();
            else if (type.equals("TestPatternCheckPermissionStrategy"))
                return new TestPatternCheckPermissionStrategySchemaConfiguration();
            else if (type.equals("TestSecurity")) {
                SchemaLoadContext loadContext = context.get(ModuleSchemaConfiguration.SCHEMA);
                SecuritySchemaConfiguration securityModelSchema = load(null, "Security", element.get("security"), context);
                SecuritySchemaBuilder securityBuilder = new SecuritySchemaBuilder();
                securityBuilder.buildSchema(securityModelSchema, loadContext.getCurrentModule());
                return null;
            } else
                return Assert.error();
        }
    }

    @Test
    public void testSchemaLoad() throws Throwable {
        ModuleSchemaLoader loader = new ModuleSchemaLoader();
        Map<String, ModuleSchemaConfiguration> modules = getMap(loader.loadModules("classpath:" + getResourcePath() + "/config1.conf"));
        SecurityServiceSchemaConfiguration configuration = (SecurityServiceSchemaConfiguration) modules.get("module1").getSchema().findDomain("system").findDomainService("SecurityService");
        assertThat(configuration.getSecurityModel(), is(new SecuritySchemaConfiguration(Collections.asSet(
                new RoleSchemaConfiguration("role1", Collections.asSet("pattern1", "pattern2"), false),
                new RoleSchemaConfiguration("role2", Collections.<String>asSet(), true)),
                new ScheduleRoleMappingStrategySchemaConfiguration(), new TestCheckPermissionStrategySchemaConfiguration(), true)));
    }

    private Map<String, ModuleSchemaConfiguration> getMap(Set<ModuleSchemaConfiguration> modules) {
        Map<String, ModuleSchemaConfiguration> map = new HashMap<String, ModuleSchemaConfiguration>();
        for (ModuleSchemaConfiguration module : modules)
            map.put(module.getName(), module);

        return map;
    }

    public static class TestCheckPermissionStrategySchemaConfiguration extends CheckPermissionStrategySchemaConfiguration {
        @Override
        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof TestCheckPermissionStrategySchemaConfiguration))
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            return getClass().hashCode();
        }

        @Override
        public ICheckPermissionStrategy createStrategy(IDatabaseContext context) {
            return new TestCheckPermissionStrategy();
        }
    }

    private static class TestCheckPermissionStrategy extends BasePrefixCheckPermissionStrategy {
        @Override
        protected String getObjectLabel(Object object) {
            return (String) ((IObjectNode) object).getKey();
        }
    }

    public static class TestPatternCheckPermissionStrategySchemaConfiguration extends CheckPermissionStrategySchemaConfiguration {
        @Override
        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof TestPatternCheckPermissionStrategySchemaConfiguration))
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            return getClass().hashCode();
        }

        @Override
        public ICheckPermissionStrategy createStrategy(IDatabaseContext context) {
            return new TestPatternCheckPermissionStrategy();
        }
    }

    private static class TestPatternCheckPermissionStrategy extends BasePatternCheckPermissionStrategy {
        @Override
        protected String getObjectLabel(Object object) {
            return (String) ((IObjectNode) object).getKey();
        }
    }

    private static String getResourcePath() {
        String className = SecuritySchemaLoaderTests.class.getName();
        int pos = className.lastIndexOf('.');
        return className.substring(0, pos).replace('.', '/');
    }
}
