/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.component.config;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.exametrika.api.component.config.AlertServiceConfiguration;
import com.exametrika.api.component.config.MailAlertChannelConfiguration;
import com.exametrika.api.exadb.core.config.DatabaseConfiguration;
import com.exametrika.common.config.AbstractElementLoader;
import com.exametrika.common.config.Configuration;
import com.exametrika.common.config.ConfigurationLoader;
import com.exametrika.common.config.IConfigurationFactory;
import com.exametrika.common.config.IConfigurationLoader.Parameters;
import com.exametrika.common.config.IConfigurationLoaderExtension;
import com.exametrika.common.config.IContextFactory;
import com.exametrika.common.config.IExtensionLoader;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Classes;
import com.exametrika.common.utils.Pair;
import com.exametrika.impl.exadb.core.config.DatabaseConfigurationLoader;
import com.exametrika.spi.exadb.core.config.DomainServiceConfiguration;

/**
 * The {@link ComponentConfigurationLoaderTests} are tests for {@link DatabaseConfigurationLoader}.
 *
 * @author Medvedev-A
 * @see DatabaseConfigurationLoader
 */
public class ComponentConfigurationLoaderTests {
    public static class TestDbConfigurationExtension implements IConfigurationLoaderExtension {
        @Override
        public Parameters getParameters() {
            Parameters parameters = new Parameters();
            parameters.schemaMappings.put("test.exadb", new Pair(
                    "classpath:" + Classes.getResourcePath(getClass()) + "/exadb-extension.json", false));
            parameters.elementLoaders.put("db", new TestDbConfigurationLoader());
            parameters.contextFactories.put(DatabaseConfiguration.SCHEMA, new TestDbLoadContext());
            parameters.topLevelElements.put("db", new Pair("TestDatabase", false));

            return parameters;
        }
    }

    public static class TestDbLoadContext implements IContextFactory, IConfigurationFactory {
        private DatabaseConfiguration database;

        @Override
        public Object createConfiguration(ILoadContext context) {
            return new TestDbConfiguration(database);
        }

        @Override
        public IConfigurationFactory createContext() {
            return new TestDbLoadContext();
        }
    }

    public static class TestDbConfiguration extends Configuration {
        public final DatabaseConfiguration database;

        public TestDbConfiguration(DatabaseConfiguration database) {
            this.database = database;
        }

        @Override
        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof TestDbConfiguration))
                return false;

            TestDbConfiguration configuration = (TestDbConfiguration) o;
            return database.equals(configuration.database);
        }

        @Override
        public int hashCode() {
            return 0;
        }
    }

    public static class TestDbConfigurationLoader extends AbstractElementLoader implements IExtensionLoader {
        @Override
        public void loadElement(JsonObject element, ILoadContext context) {
            TestDbLoadContext loadContext = context.get(DatabaseConfiguration.SCHEMA);
            loadContext.database = load(null, "Database", element, context);
        }

        @Override
        public Object loadExtension(String name, String type, Object element, ILoadContext context) {
            return Assert.error();
        }
    }

    @Test
    public void testComponentConfigurationLoad() {
        System.setProperty("com.exametrika.home", System.getProperty("java.io.tmpdir"));
        System.setProperty("com.exametrika.workPath", System.getProperty("java.io.tmpdir") + "/work");
        ConfigurationLoader loader = new ConfigurationLoader();
        TestDbConfiguration configuration = loader.loadConfiguration("classpath:" + getResourcePath() + "/config1.conf").get(DatabaseConfiguration.SCHEMA);
        AlertServiceConfiguration alertServiceConfiguration = new AlertServiceConfiguration(10, java.util.Collections.singletonMap("mail",
                new MailAlertChannelConfiguration("mail", "host", 123, "userName", "password", true, "senderName", "senderAddress", 1000)));
        assertThat(configuration.database.getDomainServices().get("component.AlertService"), is((DomainServiceConfiguration) alertServiceConfiguration));
    }

    private static String getResourcePath() {
        String className = ComponentConfigurationLoaderTests.class.getName();
        int pos = className.lastIndexOf('.');
        return className.substring(0, pos).replace('.', '/');
    }
}
