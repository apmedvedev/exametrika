/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.common.model;

import java.util.List;

import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.common.utils.Strings;


/**
 * The {@link Measurement} represents a single value measurement.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class Measurement {
    private static final IMessages messages = Messages.get(IMessages.class);
    private final IMeasurementId id;
    private final IComponentValue value;
    private final long period;
    private final List<NameId> names;

    public Measurement(IMeasurementId id, IComponentValue value, long period, List<NameId> names) {
        Assert.notNull(id);
        Assert.notNull(value);

        this.id = id;
        this.value = value;
        this.period = period;
        this.names = names;
    }

    public IMeasurementId getId() {
        return id;
    }

    public IComponentValue getValue() {
        return value;
    }

    public long getPeriod() {
        return period;
    }

    public List<NameId> getNames() {
        return names;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof Measurement))
            return false;

        Measurement measurement = (Measurement) o;
        return id.equals(measurement.id) && value.equals(measurement.value) && Objects.equals(names, measurement.names);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, value, names);
    }

    @Override
    public String toString() {
        return messages.toString(id, value, names != null ? ("\nnames:\n[\n" + Strings.toString(names, true) + "\n]") : "").toString();
    }

    private interface IMessages {
        @DefaultMessage("{0}\n{1}{2}")
        ILocalizedMessage toString(IMeasurementId id, IComponentValue value, String names);
    }
}
