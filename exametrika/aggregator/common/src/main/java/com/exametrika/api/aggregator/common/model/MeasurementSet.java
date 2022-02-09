/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.common.model;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.common.utils.Objects;
import com.exametrika.common.utils.Strings;


/**
 * The {@link MeasurementSet} represents a set of measurements.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class MeasurementSet {
    public static final int DERIVED_FLAG = 0x1;
    public static final int RESPONSE_FLAG = 0x2;
    private static final IMessages messages = Messages.get(IMessages.class);
    private final List<Measurement> measurements;
    private final String domain;
    private final int schemaVersion;
    private final long time;
    private final int flags;

    public MeasurementSet(List<Measurement> measurements, String domain, int schemaVersion, long time, int flags) {
        Assert.notNull(measurements);

        this.measurements = Immutables.wrap(measurements);
        this.domain = domain;
        this.schemaVersion = schemaVersion;
        this.time = time;
        this.flags = flags;
    }

    public List<Measurement> getMeasurements() {
        return measurements;
    }

    public String getDomain() {
        return domain;
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public long getTime() {
        return time;
    }

    public boolean isDerived() {
        return (flags & DERIVED_FLAG) != 0;
    }

    public boolean isResponse() {
        return (flags & RESPONSE_FLAG) != 0;
    }

    public int getFlags() {
        return flags;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof MeasurementSet))
            return false;

        MeasurementSet set = (MeasurementSet) o;
        return measurements.equals(set.measurements) && Objects.equals(domain, set.domain) &&
                schemaVersion == set.schemaVersion && flags == set.flags;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(measurements, domain, schemaVersion, flags);
    }

    @Override
    public String toString() {
        List<String> flagsList = new ArrayList<String>();
        buildFlagsList(getFlags(), flagsList);
        return messages.toString(domain != null ? domain : "", schemaVersion, !flagsList.isEmpty() ? flagsList.toString() : "",
                Strings.toString(measurements, true)).toString();
    }

    private void buildFlagsList(int flags, List<String> list) {
        if ((flags & DERIVED_FLAG) != 0)
            list.add("derived");
        if ((flags & RESPONSE_FLAG) != 0)
            list.add("response");
    }

    private interface IMessages {
        @DefaultMessage("domain: {0}, version: {1}{2}\n{3}")
        ILocalizedMessage toString(String domain, int schemaVersion, String flags, String measurements);
    }
}
