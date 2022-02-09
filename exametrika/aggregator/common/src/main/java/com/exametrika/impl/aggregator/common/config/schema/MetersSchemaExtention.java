/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.config.schema;

import com.exametrika.api.aggregator.common.meters.config.StandardFieldConfiguration;
import com.exametrika.common.config.IConfigurationLoader.Parameters;
import com.exametrika.common.config.IConfigurationLoaderExtension;
import com.exametrika.common.config.property.MapPropertyResolver;
import com.exametrika.common.utils.Classes;
import com.exametrika.common.utils.MapBuilder;
import com.exametrika.common.utils.Pair;
import com.exametrika.impl.aggregator.common.config.MetersSchemaLoader;


/**
 * The {@link MetersSchemaExtention} is a helper class that is used to load meters schema.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class MetersSchemaExtention implements IConfigurationLoaderExtension {
    @Override
    public Parameters getParameters() {
        Parameters parameters = new Parameters();
        MetersSchemaLoader loader = new MetersSchemaLoader("meters.");
        parameters.typeLoaders.put("meters.Counter", loader);
        parameters.typeLoaders.put("meters.Gauge", loader);
        parameters.typeLoaders.put("meters.Log", loader);
        parameters.typeLoaders.put("meters.Info", loader);
        parameters.typeLoaders.put("meters.StandardFields", loader);
        parameters.typeLoaders.put("meters.StatisticsFields", loader);
        parameters.typeLoaders.put("meters.UniformHistogramFields", loader);
        parameters.typeLoaders.put("meters.LogarithmicHistogramFields", loader);
        parameters.typeLoaders.put("meters.CustomHistogramFields", loader);
        parameters.typeLoaders.put("meters.InstanceFields", loader);
        parameters.propertyResolvers.add(new MapPropertyResolver(new MapBuilder<String, String>().put("meters.prefix", "meters.").toMap()));
        parameters.schemaMappings.put("com.exametrika.meters-1.0",
                new Pair("classpath:" + Classes.getResourcePath(StandardFieldConfiguration.class) + "/meters.dbschema", false));
        return parameters;
    }
}
