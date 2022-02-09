/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.config.schema;

import com.exametrika.api.aggregator.config.model.AggregationSchemaConfiguration;
import com.exametrika.api.aggregator.config.schema.PeriodDatabaseExtensionSchemaConfiguration;
import com.exametrika.common.config.IConfigurationLoaderExtension;
import com.exametrika.common.utils.Classes;
import com.exametrika.common.utils.Pair;
import com.exametrika.impl.exadb.core.config.schema.ModuleSchemaLoader.Parameters;


/**
 * The {@link PeriodSchemaExtention} is a helper class that is used to load perfdb schemas.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class PeriodSchemaExtention implements IConfigurationLoaderExtension {
    @Override
    public Parameters getParameters() {
        Parameters parameters = new Parameters();
        PeriodSchemaLoader processor = new PeriodSchemaLoader();
        parameters.typeLoaders.put("ArchiveOperation", processor);
        parameters.typeLoaders.put("TruncationOperation", processor);
        parameters.typeLoaders.put("PeriodSpace", processor);
        parameters.typeLoaders.put("IndexedLocationField", processor);
        parameters.typeLoaders.put("PeriodDatabaseExtension", processor);
        parameters.typeLoaders.put("LocationKeyNormalizer", processor);
        parameters.typeLoaders.put("PeriodAggregationField", processor);
        parameters.typeLoaders.put("LogAggregationField", processor);
        parameters.typeLoaders.put("AggregationSchema", processor);
        parameters.typeLoaders.put("PeriodTypeAggregationSchema", processor);
        parameters.typeLoaders.put("SimpleMeasurementFilter", processor);
        parameters.schemaMappings.put(PeriodDatabaseExtensionSchemaConfiguration.SCHEMA,
                new Pair("classpath:" + Classes.getResourcePath(PeriodDatabaseExtensionSchemaConfiguration.class) + "/perfdb.dbschema", false));
        parameters.schemaMappings.put(AggregationSchemaConfiguration.SCHEMA,
                new Pair("classpath:" + Classes.getResourcePath(AggregationSchemaConfiguration.class) + "/aggregation.dbschema", false));
        return parameters;
    }
}
