/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.component.config.model;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.exadb.core.config.schema.SchemaConfiguration;


/**
 * The {@link ComponentModelSchemaConfiguration} is a component model schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ComponentModelSchemaConfiguration extends SchemaConfiguration {
    public static final String SCHEMA = "com.exametrika.component-1.0";
    private final Set<ComponentSchemaConfiguration> components;
    private final Map<String, ComponentSchemaConfiguration> componentsMap;
    private final HealthSchemaConfiguration health;
    private Map<String, PredefinedGroupSchemaConfiguration> groups;
    private UserInterfaceSchemaConfiguration userInterface;
    private final int version;

    public ComponentModelSchemaConfiguration(Set<ComponentSchemaConfiguration> components,
                                             HealthSchemaConfiguration health, Map<String, PredefinedGroupSchemaConfiguration> groups,
                                             UserInterfaceSchemaConfiguration userInterface, int version) {
        super("ComponentModel", "ComponentModel", null);

        Assert.notNull(components);
        Assert.notNull(groups);

        Map<String, ComponentSchemaConfiguration> componentsMap = new HashMap<String, ComponentSchemaConfiguration>();
        for (ComponentSchemaConfiguration component : components)
            Assert.isNull(componentsMap.put(component.getName(), component));

        this.components = Immutables.wrap(components);
        this.componentsMap = componentsMap;
        this.health = health;
        this.groups = Immutables.wrap(groups);
        this.userInterface = userInterface;
        this.version = version;
    }

    public Set<ComponentSchemaConfiguration> getComponents() {
        return components;
    }

    public ComponentSchemaConfiguration findComponent(String name) {
        Assert.notNull(name);

        return componentsMap.get(name);
    }

    public HealthSchemaConfiguration getHealth() {
        return health;
    }

    public Map<String, PredefinedGroupSchemaConfiguration> getGroups() {
        return groups;
    }

    public UserInterfaceSchemaConfiguration getUserInterface() {
        return userInterface;
    }

    public int getVersion() {
        return version;
    }

    @Override
    public <T extends SchemaConfiguration> T combine(T schema) {
        ComponentModelSchemaConfiguration componentModel = (ComponentModelSchemaConfiguration) schema;
        Set<ComponentSchemaConfiguration> components = new LinkedHashSet<ComponentSchemaConfiguration>();
        Map<String, ComponentSchemaConfiguration> componentMap = new LinkedHashMap<String, ComponentSchemaConfiguration>(this.componentsMap);
        for (ComponentSchemaConfiguration component : componentModel.getComponents())
            components.add(combine(component, componentMap));
        components.addAll(componentMap.values());

        Map<String, PredefinedGroupSchemaConfiguration> groups = new LinkedHashMap<String, PredefinedGroupSchemaConfiguration>(this.groups);
        for (PredefinedGroupSchemaConfiguration group : componentModel.getGroups().values()) {
            Assert.isTrue(!groups.containsKey(group.getName()));
            groups.put(group.getName(), group);
        }

        return (T) new ComponentModelSchemaConfiguration(components, combine(health, componentModel.health),
                groups, combine(userInterface, componentModel.userInterface), combine(version, componentModel.version));
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ComponentModelSchemaConfiguration))
            return false;

        ComponentModelSchemaConfiguration configuration = (ComponentModelSchemaConfiguration) o;
        return super.equals(configuration) && components.equals(configuration.components) &&
                Objects.equals(health, configuration.health) &&
                groups.equals(configuration.groups) && Objects.equals(userInterface, configuration.userInterface) && version == configuration.version;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(components, health, groups, userInterface, version);
    }

    @Override
    public String toString() {
        return components.toString();
    }
}
