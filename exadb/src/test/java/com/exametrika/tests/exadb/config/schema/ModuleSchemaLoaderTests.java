/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.exadb.config.schema;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;

import com.exametrika.api.exadb.core.config.schema.DatabaseSchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.DomainSchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.ModularDatabaseSchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.ModuleDependencySchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.ModuleSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.BTreeIndexSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.ByteArrayKeyNormalizerSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.ByteArrayValueConverterSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.CollatorKeyNormalizerSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.CompositeKeyNormalizerSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.DescendingKeyNormalizerSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.FixedCompositeKeyNormalizerSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.FixedStringKeyNormalizerSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.HashIndexSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.LongValueConverterSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.NumericKeyNormalizerSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.StringKeyNormalizerSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.TreeIndexSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.UuidKeyNormalizerSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.BinaryFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.BlobStoreFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.BodyFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.CollatorSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.CollatorSchemaConfiguration.Strength;
import com.exametrika.api.exadb.objectdb.config.schema.ComputedFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.FileFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.FileFieldSchemaConfiguration.PageType;
import com.exametrika.api.exadb.objectdb.config.schema.IndexFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.IndexType;
import com.exametrika.api.exadb.objectdb.config.schema.IndexedNumericFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.IndexedStringFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.IndexedUuidFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.JsonFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.NumericFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.NumericSequenceFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.ObjectSpaceSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.PrimitiveFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.PrimitiveFieldSchemaConfiguration.DataType;
import com.exametrika.api.exadb.objectdb.config.schema.ReferenceFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.SerializableFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.SingleReferenceFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.StringFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.StringSequenceFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.StructuredBlobFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.StructuredBlobIndexSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.TagFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.TextFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.UuidFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.VersionFieldSchemaConfiguration;
import com.exametrika.common.config.AbstractExtensionLoader;
import com.exametrika.common.config.IConfigurationLoader.Parameters;
import com.exametrika.common.config.IConfigurationLoaderExtension;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Classes;
import com.exametrika.common.utils.Collections;
import com.exametrika.common.utils.MapBuilder;
import com.exametrika.common.utils.Pair;
import com.exametrika.common.utils.Version;
import com.exametrika.impl.exadb.core.config.DatabaseConfigurationLoader;
import com.exametrika.impl.exadb.core.config.schema.ModuleSchemaLoader;
import com.exametrika.spi.exadb.core.IDomainService;
import com.exametrika.spi.exadb.core.config.schema.DatabaseExtensionSchemaConfiguration;
import com.exametrika.spi.exadb.core.config.schema.DomainServiceSchemaConfiguration;
import com.exametrika.spi.exadb.core.config.schema.SpaceSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.JsonConverterSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.JsonValidatorSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.NodeSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.ObjectNodeSchemaConfiguration;
import com.exametrika.tests.exadb.StructuredBlobTests.TestRecordIndexerSchemaConfiguration;

/**
 * The {@link ModuleSchemaLoaderTests} are tests for {@link ModuleSchemaLoader}.
 *
 * @author Medvedev-A
 * @see DatabaseConfigurationLoader
 */
@Ignore
public class ModuleSchemaLoaderTests {
    public static class TestConfigurationExtension implements IConfigurationLoaderExtension {
        @Override
        public Parameters getParameters() {
            Parameters parameters = new Parameters();
            parameters.schemaMappings.put("test.exadb", new Pair(
                    "classpath:" + Classes.getResourcePath(getClass()) + "/extension.dbschema", false));
            parameters.typeLoaders.put("TestDomainService", new TestSchemaConfigurationLoader());
            parameters.typeLoaders.put("TestNode", new TestSchemaConfigurationLoader());
            parameters.typeLoaders.put("TestPeriodNode", new TestSchemaConfigurationLoader());
            parameters.typeLoaders.put("TestRecordIndexer", new TestSchemaConfigurationLoader());

            return parameters;
        }
    }

    public static class TestDomainServiceSchemaConfiguration extends DomainServiceSchemaConfiguration {
        public TestDomainServiceSchemaConfiguration() {
            super("test");
        }

        @Override
        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof TestDomainServiceSchemaConfiguration))
                return false;

            TestDomainServiceSchemaConfiguration configuration = (TestDomainServiceSchemaConfiguration) o;
            return super.equals(configuration);
        }

        @Override
        public int hashCode() {
            return 0;
        }

        @Override
        public IDomainService createService() {
            return null;
        }
    }

    public static class TestNodeSchemaConfiguration extends ObjectNodeSchemaConfiguration {
        public TestNodeSchemaConfiguration(String name, String alias, String description,
                                           List<? extends FieldSchemaConfiguration> fields) {
            super(name, alias, description, fields, null);
        }

        @Override
        public boolean equals(Object o) {
            if (o == this)
                return true;
            if (!(o instanceof TestNodeSchemaConfiguration))
                return false;

            TestNodeSchemaConfiguration configuration = (TestNodeSchemaConfiguration) o;
            return super.equals(configuration);
        }

        @Override
        public int hashCode() {
            return 0;
        }
    }

    public static class TestSchemaConfigurationLoader extends AbstractExtensionLoader {
        @Override
        public Object loadExtension(String name, String type, Object object, ILoadContext context) {
            JsonObject element = (JsonObject) object;
            if (type.equals("TestDomainService"))
                return new TestDomainServiceSchemaConfiguration();
            else if (type.equals("TestNode")) {
                String alias = element.get("alias", name);
                String description = element.get("description", null);

                JsonObject fieldsElement = element.get("fields");
                List<FieldSchemaConfiguration> fields = new ArrayList<FieldSchemaConfiguration>();
                for (Map.Entry<String, Object> entry : fieldsElement)
                    fields.add((FieldSchemaConfiguration) load(entry.getKey(), null, (JsonObject) entry.getValue(), context));

                return new TestNodeSchemaConfiguration(name, alias, description, fields);
            } else if (type.equals("TestRecordIndexer"))
                return new TestRecordIndexerSchemaConfiguration();
            else
                return Assert.error();
        }
    }

    @Test
    public void testSchemaLoad() throws Throwable {
        ModuleSchemaLoader loader = new ModuleSchemaLoader();
        Set<ModuleSchemaConfiguration> modules = loader.loadModules("classpath:" + getResourcePath() + "/config1.conf");

        checkModules(modules, null);
    }

    @Test
    public void testIntialSchemaLoad() throws Throwable {
        ModuleSchemaLoader loader = new ModuleSchemaLoader();
        ModularDatabaseSchemaConfiguration configuration = loader.loadInitialSchema("classpath:" + getResourcePath() + "/config2.conf");

        assertThat(configuration.getName(), is("testDb"));
        assertThat(configuration.getAlias(), is("testDbAlias"));
        assertThat(configuration.getDescription(), is("testDbDescription"));
        assertThat(configuration.getLocale(), is("locale"));
        assertThat(configuration.getTimeZone(), is("timeZone"));
        checkModules(configuration.getModules(), "timeZone");
    }

    private void checkModules(Collection<ModuleSchemaConfiguration> modules, String timeZone) {
        BodyFieldSchemaConfiguration field1 = new BodyFieldSchemaConfiguration("field1", "field1", null, false);
        ComputedFieldSchemaConfiguration field2 = new ComputedFieldSchemaConfiguration("field2", "field2", null, "a+b");
        FileFieldSchemaConfiguration field3 = new FileFieldSchemaConfiguration("field3", "field3", null, 1, 2, "dir",
                PageType.LARGE, true, new MapBuilder().put("key1", "value1").put("key2", "value2").toMap());
        BlobStoreFieldSchemaConfiguration field4 = new BlobStoreFieldSchemaConfiguration("field4", "field4", null, 0,
                Long.MAX_VALUE, null, PageType.NORMAL, false, java.util.Collections.<String, String>emptyMap(), true);
        JsonFieldSchemaConfiguration field5 = new JsonFieldSchemaConfiguration("field5", "field5", null, "schema",
                Collections.<JsonValidatorSchemaConfiguration>asSet(), Collections.<JsonConverterSchemaConfiguration>asSet(), "type", true, false);
        SerializableFieldSchemaConfiguration field6 = new SerializableFieldSchemaConfiguration("field6", "field6", null,
                true, false, Collections.asSet("class1", "class2"));
        StringFieldSchemaConfiguration field7 = new StringFieldSchemaConfiguration("field7", "field7", null, true, false,
                1, 2, "pattern", Collections.asSet("value1", "value2"), "sequence");
        PrimitiveFieldSchemaConfiguration field8 = new PrimitiveFieldSchemaConfiguration("field8", DataType.BOOLEAN);
        NumericFieldSchemaConfiguration field9 = new NumericFieldSchemaConfiguration("field9", "field9", null, DataType.LONG,
                Long.valueOf(100), Long.valueOf(1000), Collections.asSet(Long.valueOf(200), Long.valueOf(300)), "sequence");
        NumericSequenceFieldSchemaConfiguration field10 = new NumericSequenceFieldSchemaConfiguration("field10", "field10", null, 100, 10, null);
        StringSequenceFieldSchemaConfiguration field11 = new StringSequenceFieldSchemaConfiguration("field11", "field11",
                null, "pre", "su", "format", 100, 10, null);
        VersionFieldSchemaConfiguration field12 = new VersionFieldSchemaConfiguration("field12");
        UuidFieldSchemaConfiguration field13 = new UuidFieldSchemaConfiguration("field13", false);
        SingleReferenceFieldSchemaConfiguration field14 = new SingleReferenceFieldSchemaConfiguration("field14", "field14", null,
                "ref", null, true, true, true, null);
        SingleReferenceFieldSchemaConfiguration field15 = new SingleReferenceFieldSchemaConfiguration("field15", "field15", null,
                null, Collections.asSet("ref1", "ref2"), false, false, false, null);
        ReferenceFieldSchemaConfiguration field16 = new ReferenceFieldSchemaConfiguration("field16", "field16", null,
                "ref", null, true, true, true, null, false);
        ReferenceFieldSchemaConfiguration field17 = new ReferenceFieldSchemaConfiguration("field17", "field17", null,
                null, Collections.asSet("ref1", "ref2"), false, false, false, null, false);
        BinaryFieldSchemaConfiguration field18 = new BinaryFieldSchemaConfiguration("field18", "field18", null, "node", "field", true, false);
        TextFieldSchemaConfiguration field19 = new TextFieldSchemaConfiguration("field19", "field19", null, "node", "field", true, false);
        IndexedNumericFieldSchemaConfiguration field20 = new IndexedNumericFieldSchemaConfiguration("field20", "field20", null, DataType.LONG,
                Long.valueOf(100), Long.valueOf(1000), Collections.asSet(Long.valueOf(200), Long.valueOf(300)), "sequence",
                1, IndexType.TREE, false, false, true, false, false, true, null, null);
        IndexedUuidFieldSchemaConfiguration field21 = new IndexedUuidFieldSchemaConfiguration("field21", "field21", null, true,
                1, IndexType.TREE, false, false, false, null);
        IndexedStringFieldSchemaConfiguration field22 = new IndexedStringFieldSchemaConfiguration("field22", "field22", null, true, false,
                1, 2, "pattern", Collections.asSet("value1", "value2"), "sequence",
                1, IndexType.TREE, false, false, true, false, false, new CollatorSchemaConfiguration("ru_RU", Strength.TERTIARY), true, true, null, null);
        IndexFieldSchemaConfiguration field23 = new IndexFieldSchemaConfiguration("field23", new BTreeIndexSchemaConfiguration("index1",
                "index1", null, 1, false, 100, false, 100, new ByteArrayKeyNormalizerSchemaConfiguration(),
                new ByteArrayValueConverterSchemaConfiguration(), true, true, java.util.Collections.singletonMap("key", "value")));
        IndexFieldSchemaConfiguration field24 = new IndexFieldSchemaConfiguration("field24", new TreeIndexSchemaConfiguration("index2",
                "index2", null, 1, false, 100, false, 100, new CollatorKeyNormalizerSchemaConfiguration("ru_RU",
                com.exametrika.api.exadb.index.config.schema.CollatorKeyNormalizerSchemaConfiguration.Strength.SECONDARY),
                new LongValueConverterSchemaConfiguration(), true, true, java.util.Collections.singletonMap("key", "value")));
        IndexFieldSchemaConfiguration field25 = new IndexFieldSchemaConfiguration("field25", new HashIndexSchemaConfiguration("index3",
                "index3", null, 1, false, 100, false, 100, new CompositeKeyNormalizerSchemaConfiguration(Arrays.asList(
                new FixedStringKeyNormalizerSchemaConfiguration(), new StringKeyNormalizerSchemaConfiguration(),
                new UuidKeyNormalizerSchemaConfiguration(), new DescendingKeyNormalizerSchemaConfiguration(new NumericKeyNormalizerSchemaConfiguration(
                        com.exametrika.api.exadb.index.config.schema.NumericKeyNormalizerSchemaConfiguration.DataType.LONG)),
                new FixedCompositeKeyNormalizerSchemaConfiguration(Arrays.asList(new UuidKeyNormalizerSchemaConfiguration())))),
                new LongValueConverterSchemaConfiguration(), java.util.Collections.singletonMap("key", "value")));

        StructuredBlobFieldSchemaConfiguration field26 = new StructuredBlobFieldSchemaConfiguration("field26", "field26", null,
                "node", "field", true, false, Collections.asSet("class1", "class2"), false, 0, Arrays.asList(
                new StructuredBlobIndexSchemaConfiguration("first", 1, IndexType.TREE, false, 256, new StringKeyNormalizerSchemaConfiguration(),
                        false, true, null), new StructuredBlobIndexSchemaConfiguration("second", 0, IndexType.BTREE, true, 16,
                        new UuidKeyNormalizerSchemaConfiguration(),
                        true, false, null)),
                true, new TestRecordIndexerSchemaConfiguration());
        TagFieldSchemaConfiguration field27 = new TagFieldSchemaConfiguration("field27", "field27", null, 256, 1,
                IndexType.BTREE, "shared");

        ObjectSpaceSchemaConfiguration space1 = new ObjectSpaceSchemaConfiguration("space1", "space1", null,
                Collections.<NodeSchemaConfiguration>asSet(new TestNodeSchemaConfiguration("root", "root", null,
                        Arrays.<FieldSchemaConfiguration>asList()), new TestNodeSchemaConfiguration("node", "node", null,
                        Arrays.<FieldSchemaConfiguration>asList(field1, field2, field3, field4, field5, field6, field7, field8,
                                field9, field10, field11, field12, field13, field14, field15, field16, field17, field18, field19,
                                field20, field21, field22, field23, field24, field25, field26, field27))), "root", 1, 2);

        DatabaseSchemaConfiguration schema = new DatabaseSchemaConfiguration("module1", "module1", null, Collections.asSet(
                new DomainSchemaConfiguration("domain1", "domain1", null, Collections.<SpaceSchemaConfiguration>asSet(space1),
                        Collections.<DomainServiceSchemaConfiguration>asSet(new TestDomainServiceSchemaConfiguration()))),
                java.util.Collections.<DatabaseExtensionSchemaConfiguration>emptySet());

        ModuleSchemaConfiguration module1 = new ModuleSchemaConfiguration("module1", "alias", "description", Version.parse("1.2.3-pre+build"),
                schema, Collections.asSet(new ModuleDependencySchemaConfiguration("module2", Version.parse("2.3")),
                new ModuleDependencySchemaConfiguration("module3", Version.parse("3.4"))));
        ModuleSchemaConfiguration module2 = new ModuleSchemaConfiguration("module2", "module2", null, Version.parse("2.4+build"),
                new DatabaseSchemaConfiguration("module2"), Collections.<ModuleDependencySchemaConfiguration>asSet());
        ModuleSchemaConfiguration module3 = new ModuleSchemaConfiguration("module3", "module3", null, Version.parse("3.5-pre"),
                new DatabaseSchemaConfiguration("module3"), Collections.<ModuleDependencySchemaConfiguration>asSet());

        Map<String, ModuleSchemaConfiguration> modulesMap = new HashMap<String, ModuleSchemaConfiguration>();
        for (ModuleSchemaConfiguration module : modules)
            modulesMap.put(module.getName(), module);

        assertThat(modules.size(), is(3));
        assertThat(modulesMap.get(module1.getName()), is(module1));
        assertThat(modulesMap.get(module2.getName()), is(module2));
        assertThat(modulesMap.get(module3.getName()), is(module3));
    }

    private static String getResourcePath() {
        String className = ModuleSchemaLoaderTests.class.getName();
        int pos = className.lastIndexOf('.');
        return className.substring(0, pos).replace('.', '/');
    }
}
