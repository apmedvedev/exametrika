/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.common.model;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;


/**
 * The {@link NameMeasurementId} is an name-based identifier of particular measurement.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class NameMeasurementId implements IMeasurementId {
    private static final IMessages messages = Messages.get(IMessages.class);
    private final IScopeName scope;
    private final IMetricLocation location;
    private final String componentType;
    private final int hashCode;

    public NameMeasurementId(IScopeName scope, IMetricLocation location, String componentType) {
        Assert.notNull(scope);
        Assert.isTrue(!scope.isEmpty());
        Assert.notNull(location);
        Assert.notNull(componentType);
        Assert.isTrue(!componentType.isEmpty());

        this.scope = scope;
        this.location = location;
        this.componentType = componentType;
        this.hashCode = Objects.hashCode(scope, location, componentType);
    }

    public IScopeName getScope() {
        return scope;
    }

    public IMetricLocation getLocation() {
        return location;
    }

    @Override
    public String getComponentType() {
        return componentType;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof NameMeasurementId))
            return false;

        NameMeasurementId id = (NameMeasurementId) o;
        return scope.equals(id.scope) && location.equals(id.location) && componentType.equals(id.componentType);
    }

    @Override
    public int hashCode() {
        return hashCode;
    }

    @Override
    public String toString() {
        return messages.toString(scope, location, componentType).toString();
    }

    private interface IMessages {
        @DefaultMessage("scope: {0}, location: {1}, component type: {2}")
        ILocalizedMessage toString(IScopeName scope, IMetricLocation location, String componentType);
    }
}
