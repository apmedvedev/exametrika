/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.aggregator.perfdb.config;

import com.exametrika.api.aggregator.config.PeriodDatabaseExtensionConfiguration;
import com.exametrika.api.exadb.core.config.CacheCategoryTypeConfiguration;
import com.exametrika.api.exadb.core.config.DatabaseConfiguration;
import com.exametrika.api.exadb.core.config.ExpressionCacheCategorizationStrategyConfiguration;
import com.exametrika.api.exadb.fulltext.config.FullTextIndexConfiguration;
import com.exametrika.api.exadb.index.config.IndexDatabaseExtensionConfiguration;
import com.exametrika.api.exadb.objectdb.config.ObjectDatabaseExtensionConfiguration;
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
import com.exametrika.common.resource.config.DynamicFixedAllocationPolicyConfigurationBuilder;
import com.exametrika.common.resource.config.DynamicPercentageAllocationPolicyConfigurationBuilder;
import com.exametrika.common.resource.config.DynamicUniformAllocationPolicyConfigurationBuilder;
import com.exametrika.common.resource.config.FixedAllocationPolicyConfigurationBuilder;
import com.exametrika.common.resource.config.FloatingAllocationPolicyConfiguration;
import com.exametrika.common.resource.config.LimitingAllocationPolicyConfiguration;
import com.exametrika.common.resource.config.MemoryResourceProviderConfiguration;
import com.exametrika.common.resource.config.PercentageAllocationPolicyConfigurationBuilder;
import com.exametrika.common.resource.config.PercentageResourceProviderConfiguration;
import com.exametrika.common.resource.config.ResourceAllocatorConfiguration;
import com.exametrika.common.resource.config.SharedResourceAllocatorConfigurationBuilder;
import com.exametrika.common.resource.config.ThresholdAllocationPolicyConfigurationBuilder;
import com.exametrika.common.resource.config.UniformAllocationPolicyConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Classes;
import com.exametrika.common.utils.Collections;
import com.exametrika.common.utils.MapBuilder;
import com.exametrika.common.utils.Pair;
import com.exametrika.impl.exadb.core.config.DatabaseConfigurationLoader;
import com.exametrika.spi.exadb.core.config.CacheCategorizationStrategyConfiguration;
import com.exametrika.spi.exadb.core.config.DomainServiceConfiguration;
import org.junit.Test;

import java.util.Arrays;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * The {@link DbConfigurationLoaderTests} are tests for {@link DatabaseConfigurationLoader}.
 *
 * @author Medvedev-A
 * @see DatabaseConfigurationLoader
 */
public class DbConfigurationLoaderTests {
    public static class TestDbConfigurationExtension implements IConfigurationLoaderExtension {
        @Override
        public Parameters getParameters() {
            Parameters parameters = new Parameters();
            parameters.schemaMappings.put("test.exadb", new Pair(
                    "classpath:" + Classes.getResourcePath(getClass()) + "/exadb-extension.json", false));
            parameters.elementLoaders.put("db", new TestDbConfigurationLoader());
            parameters.typeLoaders.put("TestDomainService", new TestDbConfigurationLoader());
            parameters.contextFactories.put("test.exadb", new TestDbLoadContext());
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
        private final DatabaseConfiguration database;

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

    public static class TestDomainServiceConfiguration extends DomainServiceConfiguration {
        public TestDomainServiceConfiguration() {
            super("test");
        }

        @Override
        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof TestDomainServiceConfiguration))
                return false;

            TestDomainServiceConfiguration configuration = (TestDomainServiceConfiguration) o;
            return super.equals(configuration);
        }

        @Override
        public int hashCode() {
            return 0;
        }
    }

    public static class TestDbConfigurationLoader extends AbstractElementLoader implements IExtensionLoader {
        @Override
        public void loadElement(JsonObject element, ILoadContext context) {
            TestDbLoadContext loadContext = context.get("test.exadb");
            loadContext.database = load(null, "Database", element, context);
        }

        @Override
        public Object loadExtension(String name, String type, Object element, ILoadContext context) {
            if (type.equals("TestDomainService"))
                return new TestDomainServiceConfiguration();
            else
                return Assert.error();
        }
    }

    @Test
    public void testDbConfigurationLoad() {
        System.setProperty("com.exametrika.home", System.getProperty("java.io.tmpdir"));
        System.setProperty("com.exametrika.workPath", System.getProperty("java.io.tmpdir") + "/work");
        ConfigurationLoader loader = new ConfigurationLoader();
        TestDbConfiguration configuration = loader.loadConfiguration("classpath:" + getResourcePath() + "/config1.conf").get("test.exadb");
        ResourceAllocatorConfiguration resourceAllocator = new SharedResourceAllocatorConfigurationBuilder()
                .setName("test")
                .addPolicy("fixed", new FixedAllocationPolicyConfigurationBuilder()
                        .addQuota("one", 1000)
                        .addQuota("two", 2000)
                        .setOtherPolicy(new UniformAllocationPolicyConfiguration()).toConfiguration())
                .addPolicy("percentage", new PercentageAllocationPolicyConfigurationBuilder()
                        .addQuota("one", 10.5d)
                        .addQuota("two", 20.5d)
                        .setOtherPolicy(new UniformAllocationPolicyConfiguration()).toConfiguration())
                .addPolicy("floating", new FloatingAllocationPolicyConfiguration("floating", 20, 30))
                .addPolicy("threshold", new ThresholdAllocationPolicyConfigurationBuilder()
                        .addThreshold(1000, new FixedAllocationPolicyConfigurationBuilder().toConfiguration())
                        .addThreshold(2000, new PercentageAllocationPolicyConfigurationBuilder().toConfiguration())
                        .toConfiguration())
                .addPolicy("uniform", new UniformAllocationPolicyConfiguration())
                .addPolicy("dynamicFixed", new DynamicFixedAllocationPolicyConfigurationBuilder()
                        .addQuota("one", 1000)
                        .addQuota("two", 2000)
                        .setOtherPolicy(new UniformAllocationPolicyConfiguration()).toConfiguration())
                .addPolicy("dynamicPercentage", new DynamicPercentageAllocationPolicyConfigurationBuilder()
                        .addQuota("one", 10.5d)
                        .addQuota("two", 20.5d)
                        .setOtherPolicy(new UniformAllocationPolicyConfiguration()).toConfiguration())
                .addPolicy("dynamicUniform", new DynamicUniformAllocationPolicyConfigurationBuilder()
                        .setUnderloadedThresholdPercentage(10)
                        .setOverloadedThresholdPercentage(20)
                        .setUnderloadedReservePercentage(30)
                        .setOverloadedReservePercentage(40)
                        .setMinQuota(50).toConfiguration())
                .addPolicy("limiting", new LimitingAllocationPolicyConfiguration(new UniformAllocationPolicyConfiguration(), 75))
                .setDefaultPolicy(new UniformAllocationPolicyConfiguration())
                .setQuotaIncreaseDelay(2000)
                .setInitializePeriod(3000)
                .setTimerPeriod(4000)
                .setAllocationPeriod(5000)
                .setResourceProvider(new PercentageResourceProviderConfiguration(new MemoryResourceProviderConfiguration(false), 50))
                .setDataExchangeFileName("test.dat")
                .setDataExchangePeriod(6000)
                .setStaleAllocatorPeriod(7000)
                .setInitialQuota(8000)
                .toConfiguration();

        CacheCategorizationStrategyConfiguration categorizationStrategy = new ExpressionCacheCategorizationStrategyConfiguration("test");
        CacheCategoryTypeConfiguration defaultCategoryType = new CacheCategoryTypeConfiguration("", 10000000, 90, 600000);
        Map<String, CacheCategoryTypeConfiguration> categoryTypes = new MapBuilder<String, CacheCategoryTypeConfiguration>()
                .put("type1", new CacheCategoryTypeConfiguration("type1", 1, 90, 3))
                .put("type2", new CacheCategoryTypeConfiguration("type2", 10000000, 90, 600000))
                .toMap();

        assertThat(configuration, is(new TestDbConfiguration(
                new DatabaseConfiguration("testDb", Arrays.asList("test path"), "initial path",
                        Collections.asSet(new ObjectDatabaseExtensionConfiguration(11, 12),
                                new PeriodDatabaseExtensionConfiguration(),
                                new IndexDatabaseExtensionConfiguration(7000, new FullTextIndexConfiguration(1, 2, 3, 4))),
                        Collections.asSet(new TestDomainServiceConfiguration()),
                        resourceAllocator, categoryTypes, defaultCategoryType,
                        categorizationStrategy, 3, 33, 14, 200, 800))));
    }

    private static String getResourcePath() {
        String className = DbConfigurationLoaderTests.class.getName();
        int pos = className.lastIndexOf('.');
        return className.substring(0, pos).replace('.', '/');
    }
}
