/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.security.config.schema;

import com.exametrika.api.exadb.security.config.model.SecuritySchemaConfiguration;
import com.exametrika.common.config.IConfigurationLoaderExtension;
import com.exametrika.common.config.IExtensionLoader;
import com.exametrika.common.utils.Classes;
import com.exametrika.common.utils.Pair;
import com.exametrika.impl.exadb.core.config.schema.ModuleSchemaLoader.Parameters;


/**
 * The {@link SecuritySchemaExtention} is a helper class that is used to load {@link SecuritySchemaConfiguration}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class SecuritySchemaExtention implements IConfigurationLoaderExtension {
    @Override
    public Parameters getParameters() {
        Parameters parameters = new Parameters();
        IExtensionLoader processor = new SecuritySchemaLoader();
        parameters.typeLoaders.put("Security", processor);
        parameters.schemaMappings.put(SecuritySchemaConfiguration.SCHEMA,
                new Pair("classpath:" + Classes.getResourcePath(SecuritySchemaConfiguration.class) + "/security.dbschema", false));
        return parameters;
    }
}
