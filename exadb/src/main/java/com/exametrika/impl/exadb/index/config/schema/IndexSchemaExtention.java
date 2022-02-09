/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.index.config.schema;

import com.exametrika.api.exadb.index.config.schema.BTreeIndexSchemaConfiguration;
import com.exametrika.common.config.IConfigurationLoader.Parameters;
import com.exametrika.common.config.IConfigurationLoaderExtension;
import com.exametrika.common.config.IExtensionLoader;
import com.exametrika.common.utils.Classes;
import com.exametrika.common.utils.Pair;
import com.exametrika.spi.exadb.index.config.schema.IndexSchemaConfiguration;


/**
 * The {@link IndexSchemaExtention} is a helper class that is used to load index schemas.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class IndexSchemaExtention implements IConfigurationLoaderExtension {
    @Override
    public Parameters getParameters() {
        Parameters parameters = new Parameters();
        IExtensionLoader processor = new IndexSchemaLoader();
        parameters.typeLoaders.put("BTreeIndex", processor);
        parameters.typeLoaders.put("TreeIndex", processor);
        parameters.typeLoaders.put("HashIndex", processor);
        parameters.typeLoaders.put("FullTextIndex", processor);
        parameters.typeLoaders.put("RebuildStatisticsOperation", processor);
        parameters.typeLoaders.put("ByteArrayKeyNormalizer", processor);
        parameters.typeLoaders.put("CollatorKeyNormalizer", processor);
        parameters.typeLoaders.put("CompositeKeyNormalizer", processor);
        parameters.typeLoaders.put("FixedCompositeKeyNormalizer", processor);
        parameters.typeLoaders.put("StringKeyNormalizer", processor);
        parameters.typeLoaders.put("FixedStringKeyNormalizer", processor);
        parameters.typeLoaders.put("UuidKeyNormalizer", processor);
        parameters.typeLoaders.put("NumericKeyNormalizer", processor);
        parameters.typeLoaders.put("DescendingKeyNormalizer", processor);
        parameters.schemaMappings.put(IndexSchemaConfiguration.SCHEMA,
                new Pair("classpath:" + Classes.getResourcePath(BTreeIndexSchemaConfiguration.class) + "/index.dbschema", false));
        return parameters;
    }
}
