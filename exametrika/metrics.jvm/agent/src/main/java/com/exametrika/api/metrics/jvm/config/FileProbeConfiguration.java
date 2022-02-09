/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.metrics.jvm.config;

import java.util.Set;

import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaBuilder;
import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.impl.metrics.jvm.probes.FileProbe;
import com.exametrika.spi.aggregator.common.meters.config.CounterConfiguration;
import com.exametrika.spi.profiler.IProbe;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.config.ExitPointProbeConfiguration;

/**
 * The {@link FileProbeConfiguration} is a configuration of file probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class FileProbeConfiguration extends ExitPointProbeConfiguration {
    private final CounterConfiguration readTimeCounter;
    private final CounterConfiguration readBytesCounter;
    private final CounterConfiguration writeTimeCounter;
    private final CounterConfiguration writeBytesCounter;

    public FileProbeConfiguration(String name, String scopeType, String measurementStrategy,
                                  long warmupDelay, CounterConfiguration readTimeCounter,
                                  CounterConfiguration readBytesCounter, CounterConfiguration writeTimeCounter,
                                  CounterConfiguration writeBytesCounter) {
        super(name, scopeType, measurementStrategy, warmupDelay, null);

        Assert.notNull(readTimeCounter);
        Assert.notNull(readBytesCounter);
        Assert.notNull(writeTimeCounter);
        Assert.notNull(writeBytesCounter);

        this.readTimeCounter = readTimeCounter;
        this.readBytesCounter = readBytesCounter;
        this.writeTimeCounter = writeTimeCounter;
        this.writeBytesCounter = writeBytesCounter;
    }

    @Override
    public String getType() {
        return super.getType() + ",local,file";
    }

    @Override
    public String getExitPointType() {
        return "files";
    }

    public CounterConfiguration getReadTimeCounter() {
        return readTimeCounter;
    }

    public CounterConfiguration getReadBytesCounter() {
        return readBytesCounter;
    }

    public CounterConfiguration getWriteTimeCounter() {
        return writeTimeCounter;
    }

    public CounterConfiguration getWriteBytesCounter() {
        return writeBytesCounter;
    }

    @Override
    public String getComponentType() {
        return "app.file";
    }

    @Override
    public boolean isAsync() {
        return false;
    }

    @Override
    public boolean isIntermediate() {
        return false;
    }

    @Override
    public boolean isPermanentHotspot() {
        return false;
    }

    @Override
    public IProbe createProbe(int index, IProbeContext context) {
        return new FileProbe(this, context, index);
    }

    @Override
    public void buildComponentSchemas(ComponentValueSchemaBuilder builder,
                                      Set<ComponentValueSchemaConfiguration> components) {
        if (readTimeCounter.isEnabled())
            builder.metric(readTimeCounter.getSchema("app.file.read.time"));
        if (readBytesCounter.isEnabled())
            builder.metric(readBytesCounter.getSchema("app.file.read.bytes"));
        if (writeTimeCounter.isEnabled())
            builder.metric(writeTimeCounter.getSchema("app.file.write.time"));
        if (writeBytesCounter.isEnabled())
            builder.metric(writeBytesCounter.getSchema("app.file.write.bytes"));
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof FileProbeConfiguration))
            return false;

        FileProbeConfiguration configuration = (FileProbeConfiguration) o;
        return super.equals(configuration) && readTimeCounter.equals(configuration.readTimeCounter) &&
                readBytesCounter.equals(configuration.readBytesCounter) && writeTimeCounter.equals(configuration.writeTimeCounter) &&
                writeBytesCounter.equals(configuration.writeBytesCounter);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(readTimeCounter, readBytesCounter, writeTimeCounter, writeBytesCounter);
    }
}
