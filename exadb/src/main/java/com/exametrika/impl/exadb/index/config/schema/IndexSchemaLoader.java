/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.index.config.schema;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.exametrika.api.exadb.fulltext.config.schema.FullTextIndexSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.BTreeIndexSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.ByteArrayKeyNormalizerSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.ByteArrayValueConverterSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.CollatorKeyNormalizerSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.CollatorKeyNormalizerSchemaConfiguration.Strength;
import com.exametrika.api.exadb.index.config.schema.CompositeKeyNormalizerSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.DescendingKeyNormalizerSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.FixedCompositeKeyNormalizerSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.FixedStringKeyNormalizerSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.HashIndexSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.IntValueConverterSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.LongValueConverterSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.NumericKeyNormalizerSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.NumericKeyNormalizerSchemaConfiguration.DataType;
import com.exametrika.api.exadb.index.config.schema.RebuildStatisticsOperationSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.StringKeyNormalizerSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.TreeIndexSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.UuidKeyNormalizerSchemaConfiguration;
import com.exametrika.common.config.AbstractExtensionLoader;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.config.InvalidConfigurationException;
import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.exadb.index.config.schema.KeyNormalizerSchemaConfiguration;
import com.exametrika.spi.exadb.index.config.schema.ValueConverterSchemaConfiguration;


/**
 * The {@link IndexSchemaLoader} is a loader of perfdb schemas.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class IndexSchemaLoader extends AbstractExtensionLoader {
    @Override
    public Object loadExtension(String name, String type, Object object, ILoadContext context) {
        JsonObject element = (JsonObject) object;
        if (type.equals("FullTextIndex")) {
            name = element.get("name");
            String alias = element.get("alias", name);
            String description = element.get("description", null);
            long pathIndex = element.get("pathIndex");

            return new FullTextIndexSchemaConfiguration(name, alias, description, (int) pathIndex);
        } else if (type.equals("BTreeIndex")) {
            name = element.get("name");
            String alias = element.get("alias", name);
            String description = element.get("description", null);
            long pathIndex = element.get("pathIndex");
            Map<String, String> properties = JsonUtils.toMap((JsonObject) element.get("properties"));
            boolean fixedKey = element.get("fixedKey");
            long maxKeySize = element.get("maxKeySize");
            boolean fixedValue = element.get("fixedValue");
            long maxValueSize = element.get("maxValueSize");
            boolean unique = element.get("unique");
            boolean sorted = element.get("sorted");
            KeyNormalizerSchemaConfiguration keyNormalizer = load(null, null, (JsonObject) element.get("keyNormalizer"), context);
            ValueConverterSchemaConfiguration valueConverter = loadValueConverter((JsonObject) element.get("valueConverter"), context);

            return new BTreeIndexSchemaConfiguration(name, alias, description, (int) pathIndex, fixedKey, (int) maxKeySize,
                    fixedValue, (int) maxValueSize, keyNormalizer, valueConverter, sorted, unique, properties);
        } else if (type.equals("TreeIndex")) {
            name = element.get("name");
            String alias = element.get("alias", name);
            String description = element.get("description", null);
            long pathIndex = element.get("pathIndex");
            Map<String, String> properties = JsonUtils.toMap((JsonObject) element.get("properties"));
            boolean fixedKey = element.get("fixedKey");
            long maxKeySize = element.get("maxKeySize");
            boolean fixedValue = element.get("fixedValue");
            long maxValueSize = element.get("maxValueSize");
            boolean sorted = element.get("sorted");
            boolean unique = element.get("unique");
            KeyNormalizerSchemaConfiguration keyNormalizer = load(null, null, (JsonObject) element.get("keyNormalizer"), context);
            ValueConverterSchemaConfiguration valueConverter = loadValueConverter((JsonObject) element.get("valueConverter"), context);

            return new TreeIndexSchemaConfiguration(name, alias, description, (int) pathIndex, fixedKey, (int) maxKeySize,
                    fixedValue, (int) maxValueSize, keyNormalizer, valueConverter, sorted, unique, properties);
        } else if (type.equals("HashIndex")) {
            name = element.get("name");
            String alias = element.get("alias", name);
            String description = element.get("description", null);
            long pathIndex = element.get("pathIndex");
            Map<String, String> properties = JsonUtils.toMap((JsonObject) element.get("properties"));
            boolean fixedKey = element.get("fixedKey");
            long maxKeySize = element.get("maxKeySize");
            boolean fixedValue = element.get("fixedValue");
            long maxValueSize = element.get("maxValueSize");
            KeyNormalizerSchemaConfiguration keyNormalizer = load(null, null, (JsonObject) element.get("keyNormalizer"), context);
            ValueConverterSchemaConfiguration valueConverter = loadValueConverter((JsonObject) element.get("valueConverter"), context);

            return new HashIndexSchemaConfiguration(name, alias, description, (int) pathIndex, fixedKey, (int) maxKeySize,
                    fixedValue, (int) maxValueSize, keyNormalizer, valueConverter, properties);
        } else if (type.equals("RebuildStatisticsOperation")) {
            double keyRatio = element.get("keyRatio");
            long rebuildThreshold = element.get("rebuildThreshold");

            return new RebuildStatisticsOperationSchemaConfiguration(keyRatio, rebuildThreshold);
        } else if (type.equals("ByteArrayKeyNormalizer"))
            return new ByteArrayKeyNormalizerSchemaConfiguration();
        else if (type.equals("CollatorKeyNormalizer")) {
            String locale = element.get("locale");
            Strength strength = loadStrength((String) element.get("strength"));
            return new CollatorKeyNormalizerSchemaConfiguration(locale, strength);
        } else if (type.equals("CompositeKeyNormalizer")) {
            List<KeyNormalizerSchemaConfiguration> keyNormalizers = new ArrayList<KeyNormalizerSchemaConfiguration>();
            JsonArray array = element.get("keyNormalizers");
            for (Object arrayElement : array)
                keyNormalizers.add((KeyNormalizerSchemaConfiguration) load(null, null, (JsonObject) arrayElement, context));
            return new CompositeKeyNormalizerSchemaConfiguration(keyNormalizers);
        } else if (type.equals("FixedCompositeKeyNormalizer")) {
            List<KeyNormalizerSchemaConfiguration> keyNormalizers = new ArrayList<KeyNormalizerSchemaConfiguration>();
            JsonArray array = element.get("keyNormalizers");
            for (Object arrayElement : array)
                keyNormalizers.add((KeyNormalizerSchemaConfiguration) load(null, null, (JsonObject) arrayElement, context));
            return new FixedCompositeKeyNormalizerSchemaConfiguration(keyNormalizers);
        } else if (type.equals("StringKeyNormalizer"))
            return new StringKeyNormalizerSchemaConfiguration();
        else if (type.equals("FixedStringKeyNormalizer"))
            return new FixedStringKeyNormalizerSchemaConfiguration();
        else if (type.equals("UuidKeyNormalizer"))
            return new UuidKeyNormalizerSchemaConfiguration();
        else if (type.equals("NumericKeyNormalizer")) {
            DataType dataType = loadDataType((String) element.get("dataType"));
            return new NumericKeyNormalizerSchemaConfiguration(dataType);
        } else if (type.equals("DescendingKeyNormalizer")) {
            KeyNormalizerSchemaConfiguration keyNormalizer = load(null, null, (JsonObject) element.get("keyNormalizer"), context);
            return new DescendingKeyNormalizerSchemaConfiguration(keyNormalizer);
        } else
            throw new InvalidConfigurationException();
    }

    private ValueConverterSchemaConfiguration loadValueConverter(JsonObject element, ILoadContext context) {
        String type = getType(element);
        if (type.equals("ByteArrayValueConverter"))
            return new ByteArrayValueConverterSchemaConfiguration();
        else if (type.equals("LongValueConverter"))
            return new LongValueConverterSchemaConfiguration();
        else if (type.equals("IntValueConverter"))
            return new IntValueConverterSchemaConfiguration();
        else
            return load(null, type, element, context);
    }

    private Strength loadStrength(String element) {
        if (element.equals("primary"))
            return Strength.PRIMARY;
        else if (element.equals("secondary"))
            return Strength.SECONDARY;
        else if (element.equals("tertiary"))
            return Strength.TERTIARY;
        else if (element.equals("quaternary"))
            return Strength.QUATERNARY;
        else if (element.equals("identical"))
            return Strength.IDENTICAL;
        else
            return Assert.error();
    }

    private DataType loadDataType(String element) {
        if (element.equals("byte"))
            return DataType.BYTE;
        else if (element.equals("short"))
            return DataType.SHORT;
        else if (element.equals("int"))
            return DataType.INT;
        else if (element.equals("long"))
            return DataType.LONG;
        else if (element.equals("float"))
            return DataType.FLOAT;
        else if (element.equals("double"))
            return DataType.DOUBLE;
        else
            return Assert.error();
    }
}