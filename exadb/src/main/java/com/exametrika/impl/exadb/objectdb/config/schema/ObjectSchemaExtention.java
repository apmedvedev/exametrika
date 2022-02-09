/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.config.schema;

import com.exametrika.api.exadb.objectdb.config.schema.ObjectSpaceSchemaConfiguration;
import com.exametrika.common.config.IConfigurationLoader.Parameters;
import com.exametrika.common.config.IConfigurationLoaderExtension;
import com.exametrika.common.config.IExtensionLoader;
import com.exametrika.common.utils.Classes;
import com.exametrika.common.utils.Pair;


/**
 * The {@link ObjectSchemaExtention} is a helper class that is used to load objectdb schemas.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ObjectSchemaExtention implements IConfigurationLoaderExtension {
    @Override
    public Parameters getParameters() {
        Parameters parameters = new Parameters();
        IExtensionLoader processor = new ObjectSchemaLoader();
        parameters.typeLoaders.put("CompactionOperation", processor);
        parameters.typeLoaders.put("ObjectSpace", processor);
        parameters.typeLoaders.put("BodyField", processor);
        parameters.typeLoaders.put("ComputedField", processor);
        parameters.typeLoaders.put("FileField", processor);
        parameters.typeLoaders.put("BlobStoreField", processor);
        parameters.typeLoaders.put("IndexField", processor);
        parameters.typeLoaders.put("JsonField", processor);
        parameters.typeLoaders.put("SerializableField", processor);
        parameters.typeLoaders.put("StringField", processor);
        parameters.typeLoaders.put("PrimitiveField", processor);
        parameters.typeLoaders.put("NumericField", processor);
        parameters.typeLoaders.put("NumericSequenceField", processor);
        parameters.typeLoaders.put("StringSequenceField", processor);
        parameters.typeLoaders.put("VersionField", processor);
        parameters.typeLoaders.put("UuidField", processor);
        parameters.typeLoaders.put("SingleReferenceField", processor);
        parameters.typeLoaders.put("ReferenceField", processor);
        parameters.typeLoaders.put("BinaryField", processor);
        parameters.typeLoaders.put("TextField", processor);
        parameters.typeLoaders.put("IndexedNumericField", processor);
        parameters.typeLoaders.put("IndexedUuidField", processor);
        parameters.typeLoaders.put("IndexedStringField", processor);
        parameters.typeLoaders.put("StructuredBlobField", processor);
        parameters.typeLoaders.put("VariableStructuredBlobField", processor);
        parameters.typeLoaders.put("TagField", processor);
        parameters.schemaMappings.put(ObjectSpaceSchemaConfiguration.SCHEMA,
                new Pair("classpath:" + Classes.getResourcePath(ObjectSpaceSchemaConfiguration.class) + "/objectdb.dbschema", false));
        return parameters;
    }
}
