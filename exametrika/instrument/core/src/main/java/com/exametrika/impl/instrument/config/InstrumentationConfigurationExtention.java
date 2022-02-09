/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.instrument.config;

import com.exametrika.api.instrument.config.InstrumentationConfiguration;
import com.exametrika.common.config.IConfigurationLoader.Parameters;
import com.exametrika.common.config.IConfigurationLoaderExtension;
import com.exametrika.common.utils.Classes;
import com.exametrika.common.utils.Pair;


/**
 * The {@link InstrumentationConfigurationExtention} is a helper class that is used to load {@link InstrumentationConfiguration}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class InstrumentationConfigurationExtention implements IConfigurationLoaderExtension {
    @Override
    public Parameters getParameters() {
        Parameters parameters = new Parameters();
        InstrumentationConfigurationLoader loader = new InstrumentationConfigurationLoader();
        parameters.elementLoaders.put("instrumentation", loader);
        parameters.typeLoaders.put("ClassNameFilter", loader);
        parameters.typeLoaders.put("ClassFilter", loader);
        parameters.typeLoaders.put("MemberNameFilter", loader);
        parameters.typeLoaders.put("MemberFilter", loader);
        parameters.typeLoaders.put("QualifiedMemberNameFilter", loader);
        parameters.typeLoaders.put("QualifiedMethodFilter", loader);
        parameters.typeLoaders.put("CompoundClassFilterExpression", loader);
        parameters.typeLoaders.put("CompoundClassNameFilterExpression", loader);

        parameters.contextFactories.put(InstrumentationConfiguration.SCHEMA, new InstrumentationLoadContext());
        parameters.schemaMappings.put(InstrumentationConfiguration.SCHEMA,
                new Pair("classpath:" + Classes.getResourcePath(InstrumentationConfiguration.class) + "/instrumentation.schema", false));
        parameters.topLevelElements.put("instrumentation", new Pair("Instrumentation", false));
        return parameters;
    }
}
