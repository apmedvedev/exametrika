/**
 * Copyright 2014 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.profiler;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.exametrika.api.aggregator.common.model.IName;
import com.exametrika.api.aggregator.common.model.Measurement;
import com.exametrika.api.aggregator.common.model.MeasurementSet;
import com.exametrika.api.aggregator.common.model.Measurements;
import com.exametrika.api.profiler.IProfilingService;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.io.impl.ByteInputStream;
import com.exametrika.common.io.impl.ByteOutputStream;
import com.exametrika.common.io.impl.DataDeserialization;
import com.exametrika.common.io.impl.DataSerialization;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.services.IService;
import com.exametrika.common.services.IServiceRegistry;
import com.exametrika.common.tasks.ITimerListener;
import com.exametrika.common.tasks.impl.Timer;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.common.utils.Files;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.Pair;
import com.exametrika.impl.aggregator.common.model.DeserializeNameDictionary;
import com.exametrika.impl.aggregator.common.model.MeasurementSerializers;
import com.exametrika.impl.aggregator.common.model.SerializeNameDictionary;
import com.exametrika.spi.aggregator.common.model.INameDictionary;
import com.exametrika.spi.aggregator.common.values.IAggregationSchema;
import com.exametrika.spi.aggregator.common.values.IComponentTypeAggregationSchema;
import com.exametrika.spi.profiler.IProfilerMeasurementHandler;


public class TestProfilerMeasurementHandler implements IProfilerMeasurementHandler, ITimerListener, IService {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(TestProfilerMeasurementHandler.class);
    private volatile IAggregationSchema schema;
    private SerializeNameDictionary serializeDictionary;
    private DeserializeNameDictionary deserializeDictionary;
    private final RandomAccessFile file;
    private boolean changed;
    private final Timer timer;

    public TestProfilerMeasurementHandler() {
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.started());

        File tempDir = new File(System.getProperty("java.io.tmpdir"), "profiler");
        tempDir.mkdirs();
        Files.emptyDir(tempDir);

        RandomAccessFile file = null;
        try {
            file = new RandomAccessFile(new File(tempDir, "measurements.dat"), "rw");
        } catch (IOException e) {
            Exceptions.wrapAndThrow(e);
        }

        this.file = file;
        timer = new Timer(3000, this, false, "Test profiler measurement handler timer thread.", null);
    }

    @Override
    public void wire(IServiceRegistry registry) {
        IProfilingService profilingService = registry.findService(IProfilingService.NAME);
        Assert.checkState(profilingService != null);

        profilingService.setMeasurementHandler(this);
    }

    @Override
    public void start(IServiceRegistry registry) {
        timer.start();
    }

    @Override
    public void stop(boolean fromShutdownHook) {
        timer.stop();
        IOs.close(file);
    }

    @Override
    public void setConfiguration(ILoadContext context) {
    }

    @Override
    public void onTimer(long currentTime) {
    }

    @Override
    public boolean canHandle() {
        return true;
    }

    @Override
    public void handle(MeasurementSet measurements) {
        IAggregationSchema schema = this.schema;
        if (schema == null || schema.getVersion() != measurements.getSchemaVersion())
            return;

        if (logger.isLogEnabled(LogLevel.TRACE))
            logger.log(LogLevel.TRACE, messages.handled(Measurements.toJson(measurements, true, true).toString()));

        ByteOutputStream outputStream = new ByteOutputStream();
        DataSerialization serialization = new DataSerialization(outputStream);
        serialization.setExtension(SerializeNameDictionary.EXTENTION_ID, serializeDictionary);

        for (Measurement measurement : measurements.getMeasurements()) {
            IComponentTypeAggregationSchema componentTypeSchema = schema.findComponentType(measurement.getId().getComponentType());
            if (componentTypeSchema == null) {
                List<String> componentTypes = new ArrayList<String>();
                for (IComponentTypeAggregationSchema componentType : schema.getComponentTypes())
                    componentTypes.add(componentType.getConfiguration().getName());

                if (logger.isLogEnabled(LogLevel.ERROR))
                    logger.log(LogLevel.ERROR, messages.componentTypeNotFound(measurement.getId().getComponentType(), componentTypes.toString()));
            }
        }

        MeasurementSerializers.serializeMeasurementSet(serialization, measurements, schema, serializeDictionary);

        ByteInputStream inputStream = new ByteInputStream(outputStream.getBuffer(), 0, outputStream.getLength());
        DataDeserialization deserialization = new DataDeserialization(inputStream);
        deserialization.setExtension(DeserializeNameDictionary.EXTENTION_ID, deserializeDictionary);

        MeasurementSerializers.deserializeMeasurementSet(deserialization, schema, deserializeDictionary);

        try {
            file.write(outputStream.getBuffer(), 0, outputStream.getLength());
        } catch (IOException e) {
            Exceptions.wrapAndThrow(e);
        }

        synchronized (this) {
            changed = true;
        }
    }

    @Override
    public void setSchema(IAggregationSchema schema) {
        serializeDictionary = new SerializeNameDictionary();
        deserializeDictionary = new DeserializeNameDictionary(new TestNameDictionary(), null);
        this.schema = schema;

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.schemaChanged(schema.getVersion()));
    }

    @Override
    public void onTimer() {
        synchronized (this) {
            if (!changed)
                return;

            changed = false;
        }

        try {
            file.getChannel().force(true);
        } catch (IOException e) {
            Exceptions.wrapAndThrow(e);
        }
    }

    private static class TestNameDictionary implements INameDictionary {
        private long id = 1;
        private Map<Long, Object> names = new HashMap<Long, Object>();

        @Override
        public synchronized long getName(IName name) {
            long id = this.id++;
            assertThat(names.put(id, name), nullValue());
            return id;
        }

        @Override
        public synchronized long getCallPath(long parentCallPathId, long metricId) {
            long id = this.id++;
            assertThat(names.put(id, new Pair(parentCallPathId, metricId)), nullValue());
            return id;
        }

        @Override
        public IName getName(long persistentNameId) {
            return null;
        }
    }

    private interface IMessages {
        @DefaultMessage("Test profiler measurement handler started.")
        ILocalizedMessage started();

        @DefaultMessage("Test profiler measurement handler schema is changed to version ''{0}''.")
        ILocalizedMessage schemaChanged(int version);

        @DefaultMessage("Test profiler measurement handler is received measurement:\n{0}")
        ILocalizedMessage handled(String measurement);

        @DefaultMessage("Component type ''{0}'' is not found in schema. Component types: {1}")
        ILocalizedMessage componentTypeNotFound(String componentType, String componentTypes);
    }
}