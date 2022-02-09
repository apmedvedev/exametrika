/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.config.schema;

import com.exametrika.api.component.config.model.ComponentModelSchemaConfiguration;
import com.exametrika.api.component.config.model.UserInterfaceSchemaConfiguration;
import com.exametrika.common.config.IConfigurationLoaderExtension;
import com.exametrika.common.utils.Classes;
import com.exametrika.common.utils.Pair;
import com.exametrika.impl.exadb.core.config.schema.ModuleSchemaLoader.Parameters;


/**
 * The {@link ComponentSchemaExtention} is a helper class that is used to load perfdb schemas.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ComponentSchemaExtention implements IConfigurationLoaderExtension {
    @Override
    public Parameters getParameters() {
        Parameters parameters = new Parameters();
        ComponentSchemaLoader processor = new ComponentSchemaLoader();
        parameters.typeLoaders.put("ComponentModel", processor);
        parameters.typeLoaders.put("BaseComponentDiscoveryStrategy", processor);
        parameters.typeLoaders.put("NodeDiscoveryStrategy", processor);
        parameters.typeLoaders.put("HostDiscoveryStrategy", processor);
        parameters.typeLoaders.put("TransactionDiscoveryStrategy", processor);
        parameters.typeLoaders.put("NodeDeletionStrategy", processor);
        parameters.typeLoaders.put("HostDeletionStrategy", processor);
        parameters.typeLoaders.put("TransactionDeletionStrategy", processor);
        parameters.typeLoaders.put("ExpressionComponentJobOperation", processor);
        parameters.typeLoaders.put("ComponentPrefixCheckPermissionStrategy", processor);
        parameters.typeLoaders.put("ComponentPatternCheckPermissionStrategy", processor);
        parameters.typeLoaders.put("GroupScopeAggregationStrategy", processor);
        parameters.typeLoaders.put("NodeGroupScopeAggregationStrategy", processor);
        parameters.schemaMappings.put(ComponentModelSchemaConfiguration.SCHEMA,
                new Pair("classpath:" + Classes.getResourcePath(ComponentModelSchemaConfiguration.class) + "/component.dbschema", false));
        parameters.schemaMappings.put(UserInterfaceSchemaConfiguration.UI_SCHEMA,
                new Pair("classpath:" + Classes.getResourcePath(UserInterfaceSchemaConfiguration.class) + "/ui.dbschema", false));
        return parameters;
    }
}
