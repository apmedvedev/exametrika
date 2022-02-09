/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.jvm.probes;

import java.util.UUID;

import com.exametrika.api.aggregator.common.model.ICallPath;
import com.exametrika.api.metrics.jvm.config.FileProbeConfiguration;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.metrics.jvm.probes.FileProbe.FileRawRequest;
import com.exametrika.impl.profiler.probes.ExitPointProbeCalibrateInfo;
import com.exametrika.impl.profiler.probes.ExitPointProbeCollector;
import com.exametrika.impl.profiler.probes.StackProbeCollector;
import com.exametrika.impl.profiler.probes.StackProbeRootCollector;
import com.exametrika.spi.aggregator.common.meters.ICounter;
import com.exametrika.spi.profiler.IRequest;


/**
 * The {@link FileProbeCollector} is a file probe collector.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class FileProbeCollector extends ExitPointProbeCollector {
    private final FileProbeConfiguration configuration;
    private ICounter readTimeCounter;
    private ICounter readBytesCounter;
    private ICounter writeTimeCounter;
    private ICounter writeBytesCounter;

    public FileProbeCollector(FileProbeConfiguration configuration,
                              int index, String name, UUID stackId, ICallPath callPath, StackProbeRootCollector root,
                              StackProbeCollector parent, JsonObject metadata, ExitPointProbeCalibrateInfo calibrateInfo, boolean leaf) {
        super(configuration, index, name, stackId, callPath, root, parent, metadata, calibrateInfo, leaf);

        Assert.notNull(configuration);

        this.configuration = configuration;
    }

    @Override
    protected void doCreateMeters() {
        if (configuration.getReadTimeCounter().isEnabled())
            readTimeCounter = getMeters().addMeter("app.file.read.time", configuration.getReadTimeCounter(), null);
        if (configuration.getReadBytesCounter().isEnabled())
            readBytesCounter = getMeters().addMeter("app.file.read.bytes", configuration.getReadBytesCounter(), null);
        if (configuration.getWriteTimeCounter().isEnabled())
            writeTimeCounter = getMeters().addMeter("app.file.write.time", configuration.getWriteTimeCounter(), null);
        if (configuration.getWriteBytesCounter().isEnabled())
            writeBytesCounter = getMeters().addMeter("app.file.write.bytes", configuration.getWriteBytesCounter(), null);
    }

    @Override
    protected void doClearMeters() {
        readTimeCounter = null;
        readBytesCounter = null;
        writeTimeCounter = null;
        writeBytesCounter = null;
    }

    @Override
    protected void doEndMeasure(IRequest request) {
        FileRawRequest fileRequest = (FileRawRequest) request;

        if (fileRequest.isRead()) {
            if (readTimeCounter != null)
                readTimeCounter.measureDelta(fileRequest.getDelta());
            if (readBytesCounter != null)
                readBytesCounter.measureDelta(fileRequest.getSize());
        } else {
            if (writeTimeCounter != null)
                writeTimeCounter.measureDelta(fileRequest.getDelta());
            if (writeBytesCounter != null)
                writeBytesCounter.measureDelta(fileRequest.getSize());
        }
    }
}
