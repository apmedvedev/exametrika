/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.core.config.schema;

import com.exametrika.api.exadb.core.config.schema.ModularDatabaseSchemaConfiguration;
import com.exametrika.common.config.IConfigurationLoader.Parameters;
import com.exametrika.common.config.IConfigurationLoaderExtension;
import com.exametrika.common.utils.Classes;
import com.exametrika.common.utils.Pair;


/**
 * The {@link InitialSchemaExtention} is a helper class that is used to load {@link ModularDatabaseSchemaConfiguration}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class InitialSchemaExtention implements IConfigurationLoaderExtension {
    @Override
    public Parameters getParameters() {
        Parameters parameters = new Parameters();
        InitialSchemaLoader processor = new InitialSchemaLoader();
        parameters.elementLoaders.put("initialSchema", processor);
        parameters.schemaMappings.put(ModularDatabaseSchemaConfiguration.SCHEMA,
                new Pair("classpath:" + Classes.getResourcePath(ModularDatabaseSchemaConfiguration.class) + "/initial.dbschema", false));
        parameters.topLevelElements.put("initialSchema", new Pair("InitialSchema", false));
        return parameters;
    }
}
