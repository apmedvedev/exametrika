/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.config;

import com.exametrika.api.profiler.config.ProfilerConfiguration;
import com.exametrika.common.config.IConfigurationLoader.Parameters;
import com.exametrika.common.config.IConfigurationLoaderExtension;
import com.exametrika.common.json.schema.JsonPathReferenceValidator;
import com.exametrika.common.utils.Classes;
import com.exametrika.common.utils.Pair;


/**
 * The {@link ProfilerConfigurationExtention} is a helper class that is used to load {@link ProfilerConfiguration}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class ProfilerConfigurationExtention implements IConfigurationLoaderExtension {
    @Override
    public Parameters getParameters() {
        Parameters parameters = new Parameters();
        ProfilerConfigurationLoader loader = new ProfilerConfigurationLoader();
        parameters.elementLoaders.put("profiler", loader);
        parameters.typeLoaders.put("SimpleRequestMappingStrategy", loader);
        parameters.typeLoaders.put("HotspotRequestMappingStrategy", loader);
        parameters.typeLoaders.put("ThresholdRequestMappingStrategy", loader);
        parameters.typeLoaders.put("CompositeRequestMappingStrategy", loader);
        parameters.contextFactories.put(ProfilerConfiguration.SCHEMA, new ProfilerLoadContext());
        parameters.schemaMappings.put(ProfilerConfiguration.SCHEMA,
                new Pair("classpath:" + Classes.getResourcePath(ProfilerConfiguration.class) + "/profiler.schema", false));
        parameters.validators.put("measurementStrategyReference", new JsonPathReferenceValidator("profiler.measurementStrategies"));
        parameters.validators.put("childMonitor", new ChildMonitorValidator());
        parameters.topLevelElements.put("profiler", new Pair("Profiler", false));
        return parameters;
    }
}
