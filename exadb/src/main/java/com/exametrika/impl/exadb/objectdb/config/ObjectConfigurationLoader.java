/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.config;

import com.exametrika.api.exadb.core.config.DatabaseConfiguration;
import com.exametrika.api.exadb.objectdb.config.ObjectDatabaseExtensionConfiguration;
import com.exametrika.common.config.AbstractExtensionLoader;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.config.InvalidConfigurationException;
import com.exametrika.common.json.JsonObject;


/**
 * The {@link ObjectConfigurationLoader} is a loader of {@link DatabaseConfiguration}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ObjectConfigurationLoader extends AbstractExtensionLoader {
    @Override
    public Object loadExtension(String name, String type, Object object, ILoadContext context) {
        JsonObject element = (JsonObject) object;
        if (type.equals("ObjectDatabaseExtension")) {
            int maxFreeNodeCacheSize = ((Long) element.get("maxFreeNodeCacheSize")).intValue();
            long maxFreeNodeIdlePeriod = element.get("maxFreeNodeIdlePeriod");

            return new ObjectDatabaseExtensionConfiguration(maxFreeNodeCacheSize, maxFreeNodeIdlePeriod);
        } else
            throw new InvalidConfigurationException();
    }
}