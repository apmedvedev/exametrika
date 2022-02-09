/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.config;

import com.exametrika.api.aggregator.common.meters.config.StandardFieldConfiguration;
import com.exametrika.common.config.IConfigurationLoader.Parameters;
import com.exametrika.common.config.property.MapPropertyResolver;
import com.exametrika.common.config.IConfigurationLoaderExtension;
import com.exametrika.common.utils.Classes;
import com.exametrika.common.utils.MapBuilder;
import com.exametrika.common.utils.Pair;


/**
 * The {@link MetersConfigurationExtention} is a helper class that is used to load meters schema.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class MetersConfigurationExtention implements IConfigurationLoaderExtension {
    @Override
    public Parameters getParameters() {
        Parameters parameters = new Parameters();
        MetersSchemaLoader loader = new MetersSchemaLoader("");
        parameters.typeLoaders.put("Counter", loader);
        parameters.typeLoaders.put("Gauge", loader);
        parameters.typeLoaders.put("Log", loader);
        parameters.typeLoaders.put("Info", loader);
        parameters.typeLoaders.put("StandardFields", loader);
        parameters.typeLoaders.put("StatisticsFields", loader);
        parameters.typeLoaders.put("UniformHistogramFields", loader);
        parameters.typeLoaders.put("LogarithmicHistogramFields", loader);
        parameters.typeLoaders.put("CustomHistogramFields", loader);
        parameters.typeLoaders.put("InstanceFields", loader);
        parameters.propertyResolvers.add(new MapPropertyResolver(new MapBuilder<String, String>().put("meters.prefix", "").toMap()));
        parameters.schemaMappings.put("com.exametrika.meters-1.0",
                new Pair("classpath:" + Classes.getResourcePath(StandardFieldConfiguration.class) + "/meters.dbschema", false));
        return parameters;
    }
}
