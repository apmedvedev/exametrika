/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.core.config.schema;

import com.exametrika.api.exadb.core.config.schema.ModuleSchemaConfiguration;
import com.exametrika.common.config.IConfigurationLoader.Parameters;
import com.exametrika.common.config.IConfigurationLoaderExtension;
import com.exametrika.common.utils.Classes;
import com.exametrika.common.utils.Pair;


/**
 * The {@link CoreSchemaExtention} is a helper class that is used to load {@link ModuleSchemaConfiguration}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class CoreSchemaExtention implements IConfigurationLoaderExtension {
    @Override
    public Parameters getParameters() {
        Parameters parameters = new Parameters();
        CoreSchemaLoader processor = new CoreSchemaLoader();
        parameters.elementLoaders.put("modules", processor);
        parameters.typeLoaders.put("BackupOperation", processor);
        parameters.typeLoaders.put("FileArchiveStore", processor);
        parameters.typeLoaders.put("NullArchiveStore", processor);
        parameters.typeLoaders.put("Modules", processor);
        parameters.schemaMappings.put(ModuleSchemaConfiguration.SCHEMA,
                new Pair("classpath:" + Classes.getResourcePath(ModuleSchemaConfiguration.class) + "/core.dbschema", false));
        parameters.topLevelElements.put("modules", new Pair("Modules", false));
        return parameters;
    }
}
