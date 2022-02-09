/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.profiler.meters;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;

import org.junit.Before;
import org.junit.Test;

import com.exametrika.api.aggregator.common.meters.config.CountLogProviderConfiguration;
import com.exametrika.api.aggregator.common.meters.config.ExpressionLogFilterConfiguration;
import com.exametrika.api.aggregator.common.meters.config.ExpressionLogProviderConfiguration;
import com.exametrika.api.aggregator.common.meters.config.LogMeterConfiguration;
import com.exametrika.api.aggregator.common.model.Measurement;
import com.exametrika.api.aggregator.common.model.MeasurementSet;
import com.exametrika.api.aggregator.common.model.Measurements;
import com.exametrika.api.aggregator.common.model.NameMeasurementId;
import com.exametrika.api.aggregator.common.values.IObjectValue;
import com.exametrika.api.profiler.config.DumpType;
import com.exametrika.api.profiler.config.ProfilerConfiguration;
import com.exametrika.api.profiler.config.ScopeConfiguration;
import com.exametrika.api.profiler.config.TimeSource;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonArrayBuilder;
import com.exametrika.common.json.JsonSerializers;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.tasks.impl.TaskQueue;
import com.exametrika.common.utils.Classes;
import com.exametrika.common.utils.Collections;
import com.exametrika.common.utils.Enums;
import com.exametrika.impl.aggregator.common.meters.Log;
import com.exametrika.impl.aggregator.common.meters.Meters;
import com.exametrika.impl.aggregator.common.model.MeasurementIdProvider;
import com.exametrika.impl.aggregator.common.model.MetricName;
import com.exametrika.impl.aggregator.common.model.ScopeName;
import com.exametrika.impl.profiler.monitors.MonitorContext;
import com.exametrika.spi.aggregator.common.meters.ILog;
import com.exametrika.spi.aggregator.common.meters.IMeasurementProvider;
import com.exametrika.spi.aggregator.common.meters.LogEvent;
import com.exametrika.spi.aggregator.common.meters.MeterContainer;
import com.exametrika.spi.aggregator.common.meters.config.CounterConfiguration;
import com.exametrika.spi.aggregator.common.meters.config.InfoConfiguration;
import com.exametrika.spi.aggregator.common.meters.config.LogConfiguration;
import com.exametrika.spi.profiler.config.MeasurementStrategyConfiguration;
import com.exametrika.spi.profiler.config.MonitorConfiguration;
import com.exametrika.spi.profiler.config.ProbeConfiguration;
import com.exametrika.tests.profiler.MonitorsTests.TestMeasurementHandler;
import com.exametrika.tests.profiler.support.TestTimeService;


/**
 * The {@link LogTests} are tests for {@link Log} implementation.
 *
 * @author Medvedev-A
 * @see Log
 */
public class LogTests {
    private MonitorContext context;
    private MeterContainer meterContainer;
    private NameMeasurementId id;
    private TestMeasurementHandler handler;

    @Before
    public void setUp() {
        handler = new TestMeasurementHandler();
        TestTimeService timeService = new TestTimeService();
        context = new MonitorContext(new ProfilerConfiguration("node", TimeSource.WALL_TIME, Collections.<MeasurementStrategyConfiguration>asSet(),
                Collections.<ScopeConfiguration>asSet(), Collections.<MonitorConfiguration>asSet(), Collections.<ProbeConfiguration>asSet(),
                1, 1, 100, 1000, 1000, new File(""), 100000, Enums.noneOf(DumpType.class), 60000, JsonUtils.EMPTY_OBJECT, null),
                timeService, handler, new TaskQueue<Runnable>(), new HashMap(), null);
        id = new NameMeasurementId(ScopeName.get("scope"), MetricName.get("metric"), "componentType");
        meterContainer = new MeterContainer(id, context, context);
    }

    @Test
    public void testLog() throws Throwable {
        LogConfiguration configuration1 = new LogConfiguration(true, null, null, null, null, 100, 512, 1000, 1000, 1);
        ILog log1 = Meters.createMeter(meterContainer, "componentType.log", configuration1, new MeasurementIdProvider(id), null, context, context);

        log1.measure(new LogEvent(id, "type", 123, "message", null, null, false));
        assertThat(handler.measurements.size(), is(1));

        ILog log2 = Meters.createMeter(meterContainer, "componentType.log", configuration1, new MeasurementIdProvider(id), new IMeasurementProvider() {
            @Override
            public Object getValue() throws Exception {
                return new LogEvent(id, "type", 123, "message", null, null, false);
            }
        }, context, context);

        log2.measure();
        assertThat(handler.measurements.size(), is(2));
        log2.measure((Object) new LogEvent(id, "type", 123, "message", null, null, false));
        assertThat(handler.measurements.size(), is(3));

        LogConfiguration configuration2 = new LogConfiguration(true, null, null, null, null, 100, 512, 1000, 1000, 3);
        ILog log3 = Meters.createMeter(meterContainer, "componentType.log", configuration2, new MeasurementIdProvider(id), null, context, context);
        for (int i = 0; i < 3; i++)
            log3.measure(new LogEvent(id, "type", i, "message", null, null, false));

        assertThat(handler.measurements.size(), is(4));
        assertThat(handler.measurements.get(3).getMeasurements().size(), is(1));
        assertThat(((JsonArray) ((IObjectValue) handler.measurements.get(3).getMeasurements().get(0).getValue().getMetrics().get(0)).getObject()).size(), is(3));

        log3.measure(new LogEvent(id, "type", 4, "message", null, null, false));
        assertThat(handler.measurements.size(), is(4));
        log3.extract(0, false, false);
        assertThat(handler.measurements.size(), is(4));
        log3.extract(0, false, true);
        log3.extract(0, false, true);
        assertThat(handler.measurements.size(), is(5));
        assertThat(handler.measurements.get(4).getMeasurements().size(), is(1));
        assertThat(((JsonArray) ((IObjectValue) handler.measurements.get(4).getMeasurements().get(0).getValue().getMetrics().get(0)).getObject()).size(), is(1));

        LogConfiguration configuration3 = new LogConfiguration(true, new ExpressionLogFilterConfiguration("message == 'test'"), null,
                null, null, 100, 512, 1000, 1000, 1);
        ILog log4 = Meters.createMeter(meterContainer, "componentType.log", configuration3, new MeasurementIdProvider(id), null, context, context);
        log4.measure(new LogEvent(id, "type", 0, "message", null, null, false));
        assertThat(handler.measurements.size(), is(5));
        log4.measure(new LogEvent(id, "type", 0, "test", null, null, false));
        assertThat(handler.measurements.size(), is(6));

        LogConfiguration configuration4 = new LogConfiguration(true, null, null, null, new ExpressionLogProviderConfiguration("event"),
                100, 512, 1000, 1000, 1);
        ILog log5 = Meters.createMeter(meterContainer, "componentType.log", configuration4, new MeasurementIdProvider(id), null, context, context);
        log5.measure(new LogEvent(id, "type", 0, "message", null, null, false));

        log5.measure(new LogEvent(id, "type", 0, null, new Exception("test exception"), Json.object().put("param1", "value1").toObjectBuilder(), false));

        LogConfiguration configuration5 = new LogConfiguration(true, null, null, null, new ExpressionLogProviderConfiguration("event"),
                2, 3, 1000, 1000, 1);
        ILog log6 = Meters.createMeter(meterContainer, "componentType.log", configuration5, new MeasurementIdProvider(id), null, context, context);
        log6.measure(new LogEvent(id, "type", 0, null, new Exception("test exception"), Json.object().put("param1", "value1").toObjectBuilder(), false));

        LogConfiguration configuration6 = new LogConfiguration(true, null, Arrays.asList(new LogMeterConfiguration("metric1",
                        new CounterConfiguration(true, true, 0), new ExpressionLogFilterConfiguration("message == 'test'"),
                        new CountLogProviderConfiguration()), new LogMeterConfiguration("metric2",
                        new InfoConfiguration(true), null, new ExpressionLogProviderConfiguration("message")),
                new LogMeterConfiguration("componentType.metric3", new LogConfiguration(true, null, null, null, null, 100, 512, 1000,
                        1000, 1), new ExpressionLogFilterConfiguration("message == 'test'"),
                        null)), new ExpressionLogFilterConfiguration("false"), null,
                2, 3, 1000, 1000, 1);
        ILog log7 = Meters.createMeter(meterContainer, "componentType.log", configuration6, new MeasurementIdProvider(id), null, context, context);
        log7.measure(new LogEvent(id, "type", 0, "test", null, null, false));
        log7.measure(new LogEvent(id, "type", 0, "message", null, null, false));
        log7.measure(new LogEvent(id, "type", 0, "test", null, null, false));

        Measurement measurement = meterContainer.extract(0, 0, false, true);
        handler.handle(new MeasurementSet(java.util.Collections.singletonList(measurement), null, 0, 111, 0));
        assertThat(handler.measurements.size(), is(12));

        LogConfiguration configuration7 = new LogConfiguration(true, null, null, null, null, 100, 512, 2, 1, 1);
        ILog log8 = Meters.createMeter(meterContainer, "componentType.log", configuration7, new MeasurementIdProvider(id), null, context, context);
        log8.measure(new LogEvent(id, "type", 1000, "test1", null, null, false));
        log8.measure(new LogEvent(id, "type", 1000, "test2", null, null, false));
        log8.measure(new LogEvent(id, "type", 1000, "test3", null, null, false));
        assertThat(handler.measurements.size(), is(14));

        log8.measure(new LogEvent(id, "type", 2000, null, new Exception("test4"), null, false));
        log8.measure(new LogEvent(id, "type", 2000, null, new Exception("test5"), null, false));
        log8.measure(new LogEvent(id, "type", 2000, null, new Exception("test6"), null, false));
        assertThat(handler.measurements.size(), is(16));

        JsonArrayBuilder builder = new JsonArrayBuilder();
        for (int i = 0; i < handler.measurements.size(); i++)
            builder.add(Measurements.toJson(handler.measurements.get(i), true, false));

        builder = JsonSerializers.read(builder.toString(), true);

        JsonArray ethalon = JsonSerializers.load("classpath:" + Classes.getResourcePath(getClass()) + "/data.json", false);
        if (!builder.toJson().equals(ethalon)) {
            //System.out.println(builder.toJson());
            System.out.println(new com.exametrika.common.json.JsonDiff(true).diff(builder.toJson(), ethalon));
            assertThat(builder.toJson(), is(ethalon));
        }
    }
}
