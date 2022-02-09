/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.exadb.core.config.schema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;
import com.exametrika.spi.exadb.core.config.schema.DomainServiceSchemaConfiguration;
import com.exametrika.spi.exadb.core.config.schema.SchemaConfiguration;
import com.exametrika.spi.exadb.core.config.schema.SpaceSchemaConfiguration;


/**
 * The {@link DomainSchemaConfiguration} is a domain schema configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class DomainSchemaConfiguration extends SchemaConfiguration {
    private List<SpaceSchemaConfiguration> spaces;
    private final Map<String, SpaceSchemaConfiguration> spacesMap;
    private final Map<String, SpaceSchemaConfiguration> spacesByAliasMap;
    private List<DomainServiceSchemaConfiguration> domainServices;
    private final Map<String, DomainServiceSchemaConfiguration> domainServicesMap;
    private final Map<String, DomainServiceSchemaConfiguration> domainServicesByAliasMap;
    private boolean freezed;

    public DomainSchemaConfiguration(String name, boolean freezed) {
        this(name, name, null, Collections.<SpaceSchemaConfiguration>emptySet(), Collections.<DomainServiceSchemaConfiguration>emptySet(), false);
    }

    public DomainSchemaConfiguration(String name, Set<? extends SpaceSchemaConfiguration> spaces) {
        this(name, name, null, spaces, Collections.<DomainServiceSchemaConfiguration>emptySet());
    }

    public DomainSchemaConfiguration(String name, Set<? extends SpaceSchemaConfiguration> spaces,
                                     Set<? extends DomainServiceSchemaConfiguration> domainServices) {
        this(name, name, null, spaces, domainServices);
    }

    public DomainSchemaConfiguration(String name, String alias, String description, Set<? extends SpaceSchemaConfiguration> spaces,
                                     Set<? extends DomainServiceSchemaConfiguration> domainServices) {
        this(name, alias, description, spaces, domainServices, true);
    }

    public DomainSchemaConfiguration(String name, String alias, String description, Set<? extends SpaceSchemaConfiguration> spaces,
                                     Set<? extends DomainServiceSchemaConfiguration> domainServices, boolean freezed) {
        super(name, alias, description);

        Assert.notNull(spaces);
        Assert.notNull(domainServices);

        Map<String, SpaceSchemaConfiguration> spacesMap = new HashMap<String, SpaceSchemaConfiguration>();
        Map<String, SpaceSchemaConfiguration> spacesByAliasMap = new HashMap<String, SpaceSchemaConfiguration>();
        for (SpaceSchemaConfiguration space : spaces) {
            Assert.isNull(spacesMap.put(space.getName(), space));
            Assert.isNull(spacesByAliasMap.put(space.getAlias(), space));
        }

        List<SpaceSchemaConfiguration> spacesList = new ArrayList(spaces);
        Collections.sort(spacesList, new Comparator<SpaceSchemaConfiguration>() {
            @Override
            public int compare(SpaceSchemaConfiguration o1, SpaceSchemaConfiguration o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        this.freezed = freezed;
        if (freezed)
            this.spaces = Immutables.wrap(spacesList);
        else
            this.spaces = spacesList;
        this.spacesMap = spacesMap;
        this.spacesByAliasMap = spacesByAliasMap;

        Map<String, DomainServiceSchemaConfiguration> domainServicesMap = new HashMap<String, DomainServiceSchemaConfiguration>();
        Map<String, DomainServiceSchemaConfiguration> domainServicesByAliasMap = new HashMap<String, DomainServiceSchemaConfiguration>();
        for (DomainServiceSchemaConfiguration service : domainServices) {
            Assert.isNull(domainServicesMap.put(service.getName(), service));
            Assert.isNull(domainServicesByAliasMap.put(service.getAlias(), service));
        }

        List<DomainServiceSchemaConfiguration> domainServicesList = new ArrayList(domainServices);
        Collections.sort(domainServicesList, new Comparator<DomainServiceSchemaConfiguration>() {
            @Override
            public int compare(DomainServiceSchemaConfiguration o1, DomainServiceSchemaConfiguration o2) {
                return o1.getName().compareTo(o2.getName());
            }
        });
        if (freezed)
            this.domainServices = Immutables.wrap(domainServicesList);
        else
            this.domainServices = domainServicesList;
        this.domainServicesMap = domainServicesMap;
        this.domainServicesByAliasMap = domainServicesByAliasMap;
    }

    public List<SpaceSchemaConfiguration> getSpaces() {
        return spaces;
    }

    public SpaceSchemaConfiguration findSpace(String name) {
        Assert.notNull(name);

        return spacesMap.get(name);
    }

    public SpaceSchemaConfiguration findSpaceByAlias(String alias) {
        Assert.notNull(alias);

        return spacesByAliasMap.get(alias);
    }

    public List<DomainServiceSchemaConfiguration> getDomainServices() {
        return domainServices;
    }

    public DomainServiceSchemaConfiguration findDomainService(String name) {
        Assert.notNull(name);

        return domainServicesMap.get(name);
    }

    public DomainServiceSchemaConfiguration findDomainServiceByAlias(String alias) {
        Assert.notNull(alias);

        return domainServicesByAliasMap.get(alias);
    }

    @Override
    public <T extends SchemaConfiguration> T combine(T schema) {
        Set<SpaceSchemaConfiguration> spaces = new HashSet<SpaceSchemaConfiguration>();
        Map<String, SpaceSchemaConfiguration> spacesMap = new HashMap<String, SpaceSchemaConfiguration>(this.spacesMap);
        for (SpaceSchemaConfiguration space : ((DomainSchemaConfiguration) schema).getSpaces())
            spaces.add(combine(space, spacesMap));
        spaces.addAll(spacesMap.values());

        Set<DomainServiceSchemaConfiguration> domainServices = new HashSet<DomainServiceSchemaConfiguration>();
        Map<String, DomainServiceSchemaConfiguration> domainServicesMap = new HashMap<String, DomainServiceSchemaConfiguration>(this.domainServicesMap);
        for (DomainServiceSchemaConfiguration domainService : ((DomainSchemaConfiguration) schema).getDomainServices())
            domainServices.add(combine(domainService, domainServicesMap));
        domainServices.addAll(domainServicesMap.values());

        return (T) new DomainSchemaConfiguration(combine(getName(), schema.getName()), combine(getAlias(), schema.getAlias()),
                combine(getDescription(), schema.getDescription()), spaces, domainServices);
    }

    public void addSpace(SpaceSchemaConfiguration space) {
        Assert.notNull(space);
        Assert.checkState(!freezed);
        Assert.isTrue(findSpace(space.getName()) == null);
        Assert.isTrue(findSpaceByAlias(space.getAlias()) == null);

        spaces.add(space);
        spacesMap.put(space.getName(), space);
        spacesByAliasMap.put(space.getAlias(), space);
    }

    public void addDomainService(DomainServiceSchemaConfiguration domainService) {
        Assert.notNull(domainService);
        Assert.checkState(!freezed);
        Assert.isTrue(findDomainService(domainService.getName()) == null);
        Assert.isTrue(findDomainServiceByAlias(domainService.getAlias()) == null);

        domainServices.add(domainService);
        domainServicesMap.put(domainService.getName(), domainService);
        domainServicesByAliasMap.put(domainService.getAlias(), domainService);
    }

    public void freeze() {
        if (freezed)
            return;

        freezed = true;
        for (SpaceSchemaConfiguration space : spaces)
            space.freeze();
        spaces = Immutables.wrap(spaces);

        for (DomainServiceSchemaConfiguration domainService : domainServices)
            domainService.freeze();
        domainServices = Immutables.wrap(domainServices);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof DomainSchemaConfiguration))
            return false;

        DomainSchemaConfiguration configuration = (DomainSchemaConfiguration) o;
        return super.equals(configuration) && spaces.equals(configuration.spaces) && domainServices.equals(configuration.domainServices);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(spaces, domainServices);
    }
}
