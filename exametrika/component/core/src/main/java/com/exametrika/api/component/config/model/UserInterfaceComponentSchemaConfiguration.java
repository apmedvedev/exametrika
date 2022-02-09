/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.config.model;

import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.exadb.core.config.schema.SchemaConfiguration;


/**
 * The {@link UserInterfaceComponentSchemaConfiguration} is a component user interface schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class UserInterfaceComponentSchemaConfiguration extends SchemaConfiguration {
    private final JsonObject views;
    private final String defaultView;

    public UserInterfaceComponentSchemaConfiguration(String name, JsonObject views, String defaultView) {
        super(name, name, null);

        Assert.notNull(views);

        this.views = views;
        this.defaultView = defaultView;
    }

    public JsonObject getViews() {
        return views;
    }

    public String getDefaultView() {
        return defaultView;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof UserInterfaceComponentSchemaConfiguration))
            return false;

        UserInterfaceComponentSchemaConfiguration configuration = (UserInterfaceComponentSchemaConfiguration) o;
        return super.equals(configuration) && views.equals(configuration.views) && Objects.equals(defaultView, configuration.defaultView);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(views, defaultView);
    }
}
