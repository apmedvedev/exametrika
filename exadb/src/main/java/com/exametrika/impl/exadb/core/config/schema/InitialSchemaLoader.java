/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.core.config.schema;

import com.exametrika.api.exadb.core.config.schema.ModuleSchemaConfiguration;
import com.exametrika.common.config.AbstractElementLoader;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.json.JsonObject;


/**
 * The {@link InitialSchemaLoader} is a configuration loader for initial database schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class InitialSchemaLoader extends AbstractElementLoader {
    @Override
    public void loadElement(JsonObject element, ILoadContext context) {
        InitialSchemaLoadContext loadContext = context.get(ModuleSchemaConfiguration.SCHEMA);
        loadContext.setName((String) element.get("name"));
        loadContext.setAlias((String) element.get("alias"));
        loadContext.setDescription((String) element.get("description", null));
        String locale = element.get("locale", null);
        loadContext.setLocale(locale);
        String timeZone = element.get("timeZone", null);
        loadContext.setTimeZone(timeZone);

        context.setParameter("timeZone", timeZone);
        context.setParameter("locale", locale);

        load(null, "Modules", element.get("modules"), context);
    }
}
