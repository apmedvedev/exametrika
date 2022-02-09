/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.security.config;

import com.exametrika.api.exadb.security.config.SecurityServiceConfiguration;
import com.exametrika.common.config.AbstractExtensionLoader;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.config.InvalidConfigurationException;
import com.exametrika.common.json.JsonObject;


/**
 * The {@link SecurityConfigurationLoader} is a loader of {@link SecurityServiceConfiguration}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class SecurityConfigurationLoader extends AbstractExtensionLoader {
    @Override
    public Object loadExtension(String name, String type, Object object, ILoadContext context) {
        JsonObject element = (JsonObject) object;
        if (type.equals("SecurityService")) {
            long sessionTimeoutPeriod = element.get("sessionTimeoutPeriod");
            long roleMappingUpdatePeriod = element.get("roleMappingUpdatePeriod");
            return new SecurityServiceConfiguration(sessionTimeoutPeriod, roleMappingUpdatePeriod);
        } else
            throw new InvalidConfigurationException();
    }
}