/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.spi.aggregator.common.meters.config;

import java.util.List;

import com.exametrika.common.utils.Objects;


/**
 * The {@link CounterConfiguration} is a configuration of counter.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class CounterConfiguration extends FieldMeterConfiguration {
    private final boolean useDeltas;
    private final int smoothingSize;

    public CounterConfiguration(boolean enabled, boolean useDeltas, int smoothingSize) {
        super(enabled);

        this.useDeltas = useDeltas;
        this.smoothingSize = smoothingSize;
    }

    public CounterConfiguration(boolean enabled, List<? extends FieldConfiguration> fields, boolean useDeltas, int smoothingSize) {
        super(enabled, fields);

        this.useDeltas = useDeltas;
        this.smoothingSize = smoothingSize;
    }

    public final boolean getUseDeltas() {
        return useDeltas;
    }

    public final int getSmoothingSize() {
        return smoothingSize;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof CounterConfiguration))
            return false;

        CounterConfiguration configuration = (CounterConfiguration) o;
        return super.equals(configuration) && useDeltas == configuration.useDeltas && smoothingSize == configuration.smoothingSize;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(useDeltas, smoothingSize);
    }

    @Override
    public String toString() {
        return "counter";
    }
}
