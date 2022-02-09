/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.config.schema;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exametrika.api.exadb.objectdb.config.schema.BinaryFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.BlobStoreFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.BodyFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.CollatorSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.CollatorSchemaConfiguration.Strength;
import com.exametrika.api.exadb.objectdb.config.schema.CompactionOperationSchemaConfiguration;
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
import com.exametrika.api.exadb.objectdb.config.schema.VariableStructuredBlobFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.VersionFieldSchemaConfiguration;
import com.exametrika.common.config.AbstractExtensionLoader;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.config.InvalidConfigurationException;
import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.exadb.index.config.schema.IndexSchemaConfiguration;
import com.exametrika.spi.exadb.index.config.schema.KeyNormalizerSchemaConfiguration;
import com.exametrika.spi.exadb.jobs.config.model.SchedulePeriodSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.JsonConverterSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.JsonValidatorSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.NodeSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.RecordIndexerSchemaConfiguration;


/**
 * The {@link ObjectSchemaLoader} is a loader of objectdb schemas.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ObjectSchemaLoader extends AbstractExtensionLoader {
    @Override
    public Object loadExtension(String name, String type, Object object, ILoadContext context) {
        JsonObject element = (JsonObject) object;
        if (type.equals("CompactionOperation"))
            return new CompactionOperationSchemaConfiguration();
        else if (type.equals("ObjectSpace")) {
            String alias = element.get("alias", name);
            String description = element.get("description", null);
            long pathIndex = element.get("pathIndex");
            long fullTextPathIndex = element.get("fullTextPathIndex");
            String rootNode = element.get("rootNode", null);
            Set<NodeSchemaConfiguration> nodes = loadNodes((JsonObject) element.get("nodes"), context);

            return new ObjectSpaceSchemaConfiguration(name, alias, description, nodes, rootNode, (int) pathIndex, (int) fullTextPathIndex, false);
        } else if (type.equals("BodyField")) {
            String alias = element.get("alias", name);
            String description = element.get("description", null);
            boolean compressed = element.get("compressed");

            return new BodyFieldSchemaConfiguration(name, alias, description, compressed);
        } else if (type.equals("ComputedField")) {
            String alias = element.get("alias", name);
            String description = element.get("description", null);
            String expression = element.get("expression");

            return new ComputedFieldSchemaConfiguration(name, alias, description, expression);
        } else if (type.equals("FileField") || type.equals("BlobStoreField")) {
            String alias = element.get("alias", name);
            String description = element.get("description", null);
            long pathIndex = element.get("pathIndex");
            long maxFileSize = element.get("maxFileSize");
            String directory = element.get("directory", null);
            PageType pageType = loadPageType((String) element.get("pageType"));
            boolean preload = element.get("preload");
            Map<String, String> properties = JsonUtils.toMap((JsonObject) element.get("properties"));

            if (type.equals("FileField"))
                return new FileFieldSchemaConfiguration(name, alias, description, (int) pathIndex, maxFileSize, directory,
                        pageType, preload, properties);
            else
                return new BlobStoreFieldSchemaConfiguration(name, alias, description, (int) pathIndex, maxFileSize,
                        directory, pageType, preload, properties, true);
        } else if (type.equals("IndexField")) {
            String alias = element.get("alias", name);
            String description = element.get("description", null);
            IndexSchemaConfiguration index = load(name, null, element.get("index"), context);

            return new IndexFieldSchemaConfiguration(name, alias, description, index);
        } else if (type.equals("JsonField")) {
            String alias = element.get("alias", name);
            String description = element.get("description", null);
            String schema = element.get("schema", null);
            String typeName = element.get("typeName", null);
            boolean compressed = element.get("compressed");
            boolean required = element.get("required");
            Set<JsonValidatorSchemaConfiguration> validators = loadValidators((JsonObject) element.get("validators"), context);
            Set<JsonConverterSchemaConfiguration> converters = loadConverters((JsonObject) element.get("converters"), context);

            return new JsonFieldSchemaConfiguration(name, alias, description, schema, validators, converters, typeName,
                    required, compressed);
        } else if (type.equals("SerializableField")) {
            String alias = element.get("alias", name);
            String description = element.get("description", null);
            boolean compressed = element.get("compressed");
            boolean required = element.get("required");
            Set<String> allowedClasses = JsonUtils.toSet((JsonArray) element.get("allowedClasses", null));

            return new SerializableFieldSchemaConfiguration(name, alias, description, required, compressed, allowedClasses);
        } else if (type.equals("StringField")) {
            String alias = element.get("alias", name);
            String description = element.get("description", null);
            boolean compressed = element.get("compressed");
            boolean required = element.get("required");
            long minSize = element.get("minSize");
            long maxSize = element.get("maxSize");
            String pattern = element.get("pattern", null);
            Set<String> enumeration = JsonUtils.toSet((JsonArray) element.get("enumeration", null));
            String sequenceField = element.get("sequenceField", null);

            return new StringFieldSchemaConfiguration(name, alias, description, required, compressed, (int) minSize, (int) maxSize,
                    pattern, enumeration, sequenceField);
        } else if (type.equals("IndexedStringField")) {
            String alias = element.get("alias", name);
            String description = element.get("description", null);
            boolean required = element.get("required");
            boolean compressed = element.get("compressed");
            long minSize = element.get("minSize");
            long maxSize = element.get("maxSize");
            String pattern = element.get("pattern", null);
            Set<String> enumeration = JsonUtils.toSet((JsonArray) element.get("enumeration", null));
            String sequenceField = element.get("sequenceField", null);
            long pathIndex = element.get("pathIndex");
            IndexType indexType = loadIndexType((String) element.get("indexType", null));
            boolean primary = element.get("primary");
            boolean unique = element.get("unique");
            boolean sorted = element.get("sorted");
            boolean ascending = element.get("ascending");
            boolean cached = element.get("cached");
            boolean fullText = element.get("fullText");
            boolean tokenized = element.get("tokenized");
            String indexName = element.get("indexName", null);
            String fullTextFieldName = element.get("fullTextFieldName", null);
            CollatorSchemaConfiguration collator = loadCollator((JsonObject) element.get("collator", null));

            return new IndexedStringFieldSchemaConfiguration(name, alias, description, required, compressed, (int) minSize,
                    (int) maxSize, pattern, enumeration, sequenceField, (int) pathIndex, indexType, primary, unique,
                    sorted, ascending, cached, collator, fullText, tokenized, indexName, fullTextFieldName);
        } else if (type.equals("PrimitiveField")) {
            String alias = element.get("alias", name);
            String description = element.get("description", null);
            DataType dataType = loadDataType((String) element.get("dataType"));

            return new PrimitiveFieldSchemaConfiguration(name, alias, description, dataType);
        } else if (type.equals("NumericField")) {
            String alias = element.get("alias", name);
            String description = element.get("description", null);
            DataType dataType = loadDataType((String) element.get("dataType"));
            Number min = element.get("min", null);
            Number max = element.get("max", null);
            Set<Number> enumeration = JsonUtils.toSet((JsonArray) element.get("enumeration", null));
            String sequenceField = element.get("sequenceField", null);

            return new NumericFieldSchemaConfiguration(name, alias, description, dataType, min, max, enumeration, sequenceField);
        } else if (type.equals("IndexedNumericField")) {
            String alias = element.get("alias", name);
            String description = element.get("description", null);
            DataType dataType = loadDataType((String) element.get("dataType"));
            Number min = element.get("min", null);
            Number max = element.get("max", null);
            Set<Number> enumeration = JsonUtils.toSet((JsonArray) element.get("enumeration", null));
            String sequenceField = element.get("sequenceField", null);
            long pathIndex = element.get("pathIndex");
            IndexType indexType = loadIndexType((String) element.get("indexType", null));
            boolean primary = element.get("primary");
            boolean unique = element.get("unique");
            boolean sorted = element.get("sorted");
            boolean ascending = element.get("ascending");
            boolean cached = element.get("cached");
            boolean fullText = element.get("fullText");
            String indexName = element.get("indexName", null);
            String fullTextFieldName = element.get("fullTextFieldName", null);

            return new IndexedNumericFieldSchemaConfiguration(name, alias, description, dataType, min, max, enumeration,
                    sequenceField, (int) pathIndex, indexType, primary, unique, sorted, ascending, cached, fullText,
                    indexName, fullTextFieldName);
        } else if (type.equals("NumericSequenceField")) {
            String alias = element.get("alias", name);
            String description = element.get("description", null);
            long initialValue = element.get("initialValue");
            long step = element.get("step");
            SchedulePeriodSchemaConfiguration period = load(null, null, element.get("period", null), context);

            return new NumericSequenceFieldSchemaConfiguration(name, alias, description, initialValue, (int) step, period);
        } else if (type.equals("StringSequenceField")) {
            String alias = element.get("alias", name);
            String description = element.get("description", null);
            long initialValue = element.get("initialValue");
            long step = element.get("step");
            SchedulePeriodSchemaConfiguration period = load(null, null, element.get("period", null), context);
            String prefix = element.get("prefix", null);
            String suffix = element.get("suffix", null);
            String numberFormat = element.get("numberFormat", null);

            return new StringSequenceFieldSchemaConfiguration(name, alias, description, prefix, suffix, numberFormat,
                    initialValue, (int) step, period);
        } else if (type.equals("VersionField")) {
            String alias = element.get("alias", name);
            String description = element.get("description", null);
            return new VersionFieldSchemaConfiguration(name, alias, description);
        } else if (type.equals("UuidField")) {
            String alias = element.get("alias", name);
            String description = element.get("description", null);
            boolean required = element.get("required");
            return new UuidFieldSchemaConfiguration(name, alias, description, required);
        } else if (type.equals("IndexedUuidField")) {
            String alias = element.get("alias", name);
            String description = element.get("description", null);
            boolean required = element.get("required");
            long pathIndex = element.get("pathIndex");
            IndexType indexType = loadIndexType((String) element.get("indexType"));
            boolean primary = element.get("primary");
            boolean unique = element.get("unique");
            boolean cached = element.get("cached");
            String indexName = element.get("indexName", null);

            return new IndexedUuidFieldSchemaConfiguration(name, alias, description, required, (int) pathIndex, indexType,
                    primary, unique, cached, indexName);
        } else if (type.equals("SingleReferenceField")) {
            String alias = element.get("alias", name);
            String description = element.get("description", null);
            String fieldReference = element.get("fieldReference", null);
            boolean required = element.get("required");
            boolean owning = element.get("owning");
            boolean bidirectional = element.get("bidirectional");
            Set<String> nodeReferences = JsonUtils.toSet((JsonArray) element.get("nodeReferences", null));
            String externalSpaceName = element.get("externalSpaceName", null);

            return new SingleReferenceFieldSchemaConfiguration(name, alias, description, fieldReference, nodeReferences,
                    required, owning, bidirectional, externalSpaceName);
        } else if (type.equals("ReferenceField")) {
            String alias = element.get("alias", name);
            String description = element.get("description", null);
            String fieldReference = element.get("fieldReference", null);
            boolean required = element.get("required");
            boolean owning = element.get("owning");
            boolean bidirectional = element.get("bidirectional");
            boolean stableOrder = element.get("stableOrder");
            Set<String> nodeReferences = JsonUtils.toSet((JsonArray) element.get("nodeReferences", null));
            String externalSpaceName = element.get("externalSpaceName", null);

            return new ReferenceFieldSchemaConfiguration(name, alias, description, fieldReference, nodeReferences,
                    required, owning, bidirectional, externalSpaceName, stableOrder);
        } else if (type.equals("BinaryField")) {
            String alias = element.get("alias", name);
            String description = element.get("description", null);
            boolean required = element.get("required");
            String blobStoreNode = element.get("blobStoreNode", null);
            String blobStoreField = element.get("blobStoreField");
            boolean compressed = element.get("compressed");

            return new BinaryFieldSchemaConfiguration(name, alias, description, blobStoreNode, blobStoreField, required, compressed);
        } else if (type.equals("TextField")) {
            String alias = element.get("alias", name);
            String description = element.get("description", null);
            boolean required = element.get("required");
            String blobStoreNode = element.get("blobStoreNode", null);
            String blobStoreField = element.get("blobStoreField");
            boolean compressed = element.get("compressed");

            return new TextFieldSchemaConfiguration(name, alias, description, blobStoreNode, blobStoreField, required, compressed);
        } else if (type.equals("StructuredBlobField")) {
            String alias = element.get("alias", name);
            String description = element.get("description", null);
            boolean required = element.get("required");
            String blobStoreNode = element.get("blobStoreNode", null);
            String blobStoreField = element.get("blobStoreField");
            boolean compressed = element.get("compressed");
            boolean fixedRecord = element.get("fixedRecord");
            long fixedRecordSize = element.get("fixedRecordSize");
            Set<String> allowedClasses = JsonUtils.toSet((JsonArray) element.get("allowedClasses", null));
            boolean fullTextIndex = element.get("fullTextIndex");
            List<StructuredBlobIndexSchemaConfiguration> indexes = loadStructuredBlobIndexes((JsonObject) element.get("indexes"), context);
            RecordIndexerSchemaConfiguration recordIndexer = load(null, null, element.get("recordIndexer"), context);

            return new StructuredBlobFieldSchemaConfiguration(name, alias, description, blobStoreNode, blobStoreField,
                    required, compressed, allowedClasses, fixedRecord, (int) fixedRecordSize, indexes, fullTextIndex, recordIndexer);
        } else if (type.equals("VariableStructuredBlobField")) {
            String alias = element.get("alias", name);
            String description = element.get("description", null);
            boolean required = element.get("required");
            String blobStoreNode = element.get("blobStoreNode", null);
            String blobStoreField = element.get("blobStoreField");
            boolean compressed = element.get("compressed");
            boolean fixedRecord = element.get("fixedRecord");
            long fixedRecordSize = element.get("fixedRecordSize");
            Set<String> allowedClasses = JsonUtils.toSet((JsonArray) element.get("allowedClasses", null));

            return new VariableStructuredBlobFieldSchemaConfiguration(name, alias, description, blobStoreNode, blobStoreField,
                    required, compressed, allowedClasses, fixedRecord, (int) fixedRecordSize);
        } else if (type.equals("TagField")) {
            String alias = element.get("alias", name);
            String description = element.get("description", null);
            long maxSize = element.get("maxSize");
            long pathIndex = element.get("pathIndex");
            IndexType indexType = loadIndexType((String) element.get("indexType", null));
            String indexName = element.get("indexName", null);

            return new TagFieldSchemaConfiguration(name, alias, description, (int) maxSize, (int) pathIndex, indexType,
                    indexName);
        } else
            throw new InvalidConfigurationException();
    }

    private List<StructuredBlobIndexSchemaConfiguration> loadStructuredBlobIndexes(JsonObject object, ILoadContext context) {
        List<StructuredBlobIndexSchemaConfiguration> list = new ArrayList<StructuredBlobIndexSchemaConfiguration>();
        for (Map.Entry<String, Object> entry : object) {
            String name = entry.getKey();
            JsonObject element = (JsonObject) entry.getValue();

            String alias = element.get("alias", name);
            String description = element.get("description", null);
            long pathIndex = element.get("pathIndex");
            IndexType indexType = loadIndexType((String) element.get("indexType", null));
            boolean unique = element.get("unique");
            boolean sorted = element.get("sorted");
            boolean fixedKey = element.get("fixedKey");
            long maxKeySize = element.get("maxKeySize");
            String indexName = element.get("indexName", null);
            KeyNormalizerSchemaConfiguration keyNormalizer = load(null, null, (JsonObject) element.get("keyNormalizer"), context);

            list.add(new StructuredBlobIndexSchemaConfiguration(name, alias, description, (int) pathIndex, indexType, fixedKey,
                    (int) maxKeySize, keyNormalizer, unique, sorted, indexName));
        }
        return list;
    }

    private CollatorSchemaConfiguration loadCollator(JsonObject element) {
        String locale = element.get("locale");
        Strength strength = loadStrength((String) element.get("strength"));
        return new CollatorSchemaConfiguration(locale, strength);
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

    private IndexType loadIndexType(String element) {
        if (element.equals("btree"))
            return IndexType.BTREE;
        else if (element.equals("tree"))
            return IndexType.TREE;
        else if (element.equals("hash"))
            return IndexType.HASH;
        else
            return Assert.error();
    }

    private DataType loadDataType(String element) {
        if (element.equals("byte"))
            return DataType.BYTE;
        else if (element.equals("char"))
            return DataType.CHAR;
        else if (element.equals("short"))
            return DataType.SHORT;
        else if (element.equals("int"))
            return DataType.INT;
        else if (element.equals("long"))
            return DataType.LONG;
        else if (element.equals("boolean"))
            return DataType.BOOLEAN;
        else if (element.equals("float"))
            return DataType.FLOAT;
        else if (element.equals("double"))
            return DataType.DOUBLE;
        else
            return Assert.error();
    }

    private PageType loadPageType(String element) {
        if (element.equals("small"))
            return PageType.SMALL;
        else if (element.equals("normal"))
            return PageType.NORMAL;
        else if (element.equals("smallMedium"))
            return PageType.SMALL_MEDIUM;
        else if (element.equals("medium"))
            return PageType.MEDIUM;
        else if (element.equals("largeMedium"))
            return PageType.LARGE_MEDIUM;
        else if (element.equals("large"))
            return PageType.LARGE;
        else if (element.equals("extraLarge"))
            return PageType.EXTRA_LARGE;
        else
            return Assert.error();
    }

    private Set<NodeSchemaConfiguration> loadNodes(JsonObject element, ILoadContext context) {
        Set<NodeSchemaConfiguration> nodes = new LinkedHashSet<NodeSchemaConfiguration>();
        for (Map.Entry<String, Object> entry : element)
            nodes.add((NodeSchemaConfiguration) load(entry.getKey(), null, (JsonObject) entry.getValue(), context));

        return nodes;
    }

    private Set<JsonValidatorSchemaConfiguration> loadValidators(JsonObject element, ILoadContext context) {
        Set<JsonValidatorSchemaConfiguration> validators = new LinkedHashSet<JsonValidatorSchemaConfiguration>();
        for (Map.Entry<String, Object> entry : element)
            validators.add((JsonValidatorSchemaConfiguration) load(entry.getKey(), null, (JsonObject) entry.getValue(), context));

        return validators;
    }

    private Set<JsonConverterSchemaConfiguration> loadConverters(JsonObject element, ILoadContext context) {
        Set<JsonConverterSchemaConfiguration> converters = new LinkedHashSet<JsonConverterSchemaConfiguration>();
        for (Map.Entry<String, Object> entry : element)
            converters.add((JsonConverterSchemaConfiguration) load(entry.getKey(), null, (JsonObject) entry.getValue(), context));

        return converters;
    }
}