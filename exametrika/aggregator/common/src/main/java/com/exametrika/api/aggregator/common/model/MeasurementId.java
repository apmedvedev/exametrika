/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.common.model;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.CacheSizes;
import com.exametrika.common.utils.Memory;
import com.exametrika.common.utils.Objects;


/**
 * The {@link MeasurementId} is a numeric-based identifier of particular measurement.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class MeasurementId implements IMeasurementId {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final int CACHE_SIZE = Memory.getShallowSize(MeasurementId.class);
    private final long scopeId;
    private final long locationId;
    private final String componentType;
    private final int hashCode;

    public MeasurementId(long scopeId, long locationId, String componentType) {
        Assert.isTrue(scopeId != 0, "Scope id of component type ''{0}'' is 0.", componentType);
        Assert.notNull(componentType);
        Assert.isTrue(!componentType.isEmpty());

        this.scopeId = scopeId;
        this.locationId = locationId;
        this.componentType = componentType;
        this.hashCode = Objects.hashCode(scopeId, locationId, componentType);
    }

    public long getScopeId() {
        return scopeId;
    }

    public long getLocationId() {
        return locationId;
    }

    @Override
    public String getComponentType() {
        return componentType;
    }

    public int getCacheSize() {
        return CACHE_SIZE + CacheSizes.getStringCacheSize(componentType);
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof MeasurementId))
            return false;

        MeasurementId id = (MeasurementId) o;
        return scopeId == id.scopeId && locationId == id.locationId && componentType.equals(id.componentType);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return messages.toString(scopeId, locationId, componentType).toString();
    }

    private interface IMessages {
        @DefaultMessage("scope-id: {0}, location-id: {1}, component type: {2}")
        ILocalizedMessage toString(long scopeId, long locationId, String componentType);
    }
}
