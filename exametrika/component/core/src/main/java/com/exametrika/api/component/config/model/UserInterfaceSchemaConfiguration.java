/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.config.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.exametrika.api.aggregator.config.model.PeriodTypeSchemaConfiguration;
import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonArrayBuilder;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.exadb.core.config.schema.SchemaConfiguration;


/**
 * The {@link UserInterfaceSchemaConfiguration} is a component model user interface schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class UserInterfaceSchemaConfiguration extends SchemaConfiguration {
    public static final String UI_SCHEMA = "com.exametrika.component.ui-1.0";
    private final List<PeriodTypeSchemaConfiguration> periods;
    private final JsonObject models;
    private final JsonObject navBar;
    private final JsonArray notifications;
    private final JsonObject views;
    private final Map<String, UserInterfaceComponentSchemaConfiguration> components;

    public UserInterfaceSchemaConfiguration(List<PeriodTypeSchemaConfiguration> periods, JsonObject models,
                                            JsonObject navBar, JsonArray notifications, JsonObject views,
                                            Map<String, UserInterfaceComponentSchemaConfiguration> components) {
        super("UserInterface", "UserInterface", null);

        Assert.notNull(periods);
        Assert.notNull(models);
        Assert.notNull(navBar);
        Assert.notNull(notifications);
        Assert.notNull(views);
        Assert.notNull(components);

        this.periods = Immutables.wrap(periods);
        this.models = models;
        this.navBar = navBar;
        this.notifications = notifications;
        this.views = views;
        this.components = Immutables.wrap(components);
    }

    public List<PeriodTypeSchemaConfiguration> getPeriods() {
        return periods;
    }

    public JsonObject getModels() {
        return models;
    }

    public JsonObject getNavBar() {
        return navBar;
    }

    public JsonArray getNotifications() {
        return notifications;
    }

    public JsonObject getViews() {
        return views;
    }

    public Map<String, UserInterfaceComponentSchemaConfiguration> getComponents() {
        return components;
    }

    @Override
    public <T extends SchemaConfiguration> T combine(T schema) {
        UserInterfaceSchemaConfiguration ui = (UserInterfaceSchemaConfiguration) schema;
        JsonObjectBuilder models = JsonUtils.mergeObjects(new JsonObjectBuilder(this.models), ui.getModels(), "", false);
        JsonObjectBuilder navBar = JsonUtils.mergeObjects(new JsonObjectBuilder(this.navBar), ui.getNavBar(), "", false);
        JsonArrayBuilder notifications = new JsonArrayBuilder(this.notifications);
        notifications.addAll(ui.getNotifications());
        JsonObjectBuilder views = JsonUtils.mergeObjects(new JsonObjectBuilder(this.views), ui.getViews(), "", false);

        Map<String, UserInterfaceComponentSchemaConfiguration> components = new LinkedHashMap<String, UserInterfaceComponentSchemaConfiguration>(this.components);
        for (UserInterfaceComponentSchemaConfiguration component : ui.components.values()) {
            UserInterfaceComponentSchemaConfiguration combined = combine(component, components);
            components.put(combined.getName(), combined);
        }

        return (T) new UserInterfaceSchemaConfiguration(combine(periods, ui.periods), models.toJson(),
                navBar.toJson(), notifications.toJson(), views.toJson(), components);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof UserInterfaceSchemaConfiguration))
            return false;

        UserInterfaceSchemaConfiguration configuration = (UserInterfaceSchemaConfiguration) o;
        return super.equals(configuration) && periods.equals(configuration.periods) && models.equals(configuration.models) &&
                navBar.equals(configuration.navBar) && notifications.equals(configuration.notifications) &&
                views.equals(configuration.views) && components.equals(configuration.components);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(periods, models, navBar, notifications, views, components);
    }
}
