/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.profiler.meters;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.exametrika.api.aggregator.common.meters.config.CustomHistogramFieldConfiguration;
import com.exametrika.api.aggregator.common.meters.config.InstanceFieldConfiguration;
import com.exametrika.api.aggregator.common.meters.config.LogarithmicHistogramFieldConfiguration;
import com.exametrika.api.aggregator.common.meters.config.StandardFieldConfiguration;
import com.exametrika.api.aggregator.common.meters.config.StatisticsFieldConfiguration;
import com.exametrika.api.aggregator.common.meters.config.UniformHistogramFieldConfiguration;
import com.exametrika.api.aggregator.common.model.IMetricName;
import com.exametrika.api.aggregator.common.model.IScopeName;
import com.exametrika.api.aggregator.common.model.NameMeasurementId;
import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.time.impl.SystemTimeService;
import com.exametrika.impl.aggregator.common.meters.Gauge;
import com.exametrika.impl.aggregator.common.meters.Meters;
import com.exametrika.impl.aggregator.common.model.MeasurementIdProvider;
import com.exametrika.impl.aggregator.common.model.MetricName;
import com.exametrika.impl.aggregator.common.model.ScopeName;
import com.exametrika.impl.aggregator.common.values.HistogramValue;
import com.exametrika.impl.aggregator.common.values.InstanceRecord;
import com.exametrika.impl.aggregator.common.values.InstanceValue;
import com.exametrika.impl.aggregator.common.values.NameValue;
import com.exametrika.impl.aggregator.common.values.StandardValue;
import com.exametrika.impl.aggregator.common.values.StatisticsValue;
import com.exametrika.impl.profiler.probes.ProbeInstanceContextProvider;
import com.exametrika.spi.aggregator.common.meters.config.FieldConfiguration;
import com.exametrika.spi.aggregator.common.meters.config.GaugeConfiguration;
import com.exametrika.tests.profiler.support.TestMeasurementContext;
import com.exametrika.tests.profiler.support.TestMeasurementHandler;
import com.exametrika.tests.profiler.support.TestMeasurementProvider;

/**
 * The {@link GaugeTests} are tests for {@link Gauge} implementation.
 *
 * @author Medvedev-A
 * @see Gauge
 */
public class GaugeTests {
    @Test
    public void testGauge() throws Throwable {
        IScopeName scope = ScopeName.get("scope");
        IMetricName metric = MetricName.get("metric");
        NameMeasurementId id = new NameMeasurementId(scope, metric, "componentType");

        ProbeInstanceContextProvider instanceContextProvider = new ProbeInstanceContextProvider(new SystemTimeService());
        TestMeasurementProvider provider = new TestMeasurementProvider();
        TestMeasurementHandler handler = new TestMeasurementHandler();
        TestMeasurementContext context = new TestMeasurementContext(handler);
        Gauge gauge = Meters.createMeter(null, "metricType", new GaugeConfiguration(true), new MeasurementIdProvider(id), provider,
                instanceContextProvider, context);

        assertThat(gauge.hasProvider(), is(true));
        assertThat(gauge.hasInstanceFields(), is(false));

        NameValue value = gauge.extract(0, false, true);
        assertThat(value, nullValue());

        gauge.measure(10);
        gauge.measure(100);
        value = gauge.extract(0, false, true);
        assertThat(gauge.extract(0, false, true), nullValue());

        assertThat(value, is(new NameValue(Arrays.asList(new StandardValue(2, 10, 100, 110)))));

        gauge = Meters.createMeter(null, "metricType", new GaugeConfiguration(true), new MeasurementIdProvider(id), provider,
                instanceContextProvider, context);
        assertThat(gauge.hasProvider(), is(true));
        provider.value = 10;
        gauge.measure();
        provider.value = 100;
        gauge.measure();
        value = gauge.extract(0, false, true);
        assertThat(gauge.extract(0, false, true), nullValue());

        assertThat(value, is(new NameValue(Arrays.asList(new StandardValue(2, 10, 100, 110)))));
    }

    @Test
    public void testCollection() {
        IScopeName scope = ScopeName.get("scope");
        IMetricName metric = MetricName.get("metric");
        NameMeasurementId id = new NameMeasurementId(scope, metric, "componentType");

        ProbeInstanceContextProvider instanceContextProvider = new ProbeInstanceContextProvider(new SystemTimeService());
        TestMeasurementHandler handler = new TestMeasurementHandler();
        TestMeasurementContext context = new TestMeasurementContext(handler);
        Gauge gauge = Meters.createMeter(null, "metricType", new GaugeConfiguration(true), new MeasurementIdProvider(id), null,
                instanceContextProvider, context);
        assertThat(gauge.hasProvider(), is(false));

        gauge.measure(10);
        NameValue value = gauge.extract(0, false, true);

        assertThat(value, is(new NameValue(Arrays.asList(new StandardValue(1, 10, 10, 10)))));

        gauge.measure(10);

        value = gauge.extract(0, false, true);

        assertThat(value, is(new NameValue(Arrays.asList(new StandardValue(1, 10, 10, 10)))));

        gauge.measure(10);
        value = gauge.extract(0, false, true);

        assertThat(value, is(new NameValue(Arrays.asList(new StandardValue(1, 10, 10, 10)))));

        gauge.measure(10);
        value = gauge.extract(0, true, true);

        assertThat(value, is(new NameValue(Arrays.asList(new StandardValue(1, 10, 10, 10)))));
    }

    @Test
    public void testFieldContainers() throws Throwable {
        NameMeasurementId id = new NameMeasurementId(ScopeName.get("scope"), MetricName.get("metric"), "componentType");
        List<FieldConfiguration> fields =
                Arrays.<FieldConfiguration>asList(new StandardFieldConfiguration(), new StatisticsFieldConfiguration(),
                        new UniformHistogramFieldConfiguration(10, 90, 10),
                        new LogarithmicHistogramFieldConfiguration(10, 7),
                        new CustomHistogramFieldConfiguration(Arrays.<Long>asList(20l, 40l, 60l, 80l)),
                        new InstanceFieldConfiguration(10, true));

        ProbeInstanceContextProvider instanceContextProvider = new ProbeInstanceContextProvider(new SystemTimeService());
        TestMeasurementProvider provider = new TestMeasurementProvider();
        TestMeasurementHandler handler = new TestMeasurementHandler();
        TestMeasurementContext context = new TestMeasurementContext(handler);
        Gauge gauge = Meters.createMeter(null, "metricType", new GaugeConfiguration(true, fields), new MeasurementIdProvider(id), provider,
                instanceContextProvider, context);

        JsonObjectBuilder builder = null;

        List<InstanceRecord> records1 = new ArrayList<InstanceRecord>();
        for (int i = 0; i < 200; i++) {
            if ((i % 10) == 0) {
                builder = new JsonObjectBuilder();
                builder.put("key" + i, "value" + i);
                instanceContextProvider.setContext(builder.toJson());
            }

            if (i > 100 && (i % 10) == 9)
                records1.add(new InstanceRecord(id, builder.toJson(), i, 0));

            gauge.measure(i);
        }

        Collections.reverse(records1);

        NameValue value = gauge.extract(0, false, true);

        assertThat(value, is(new NameValue(Arrays.<IFieldValue>asList(new StandardValue(200, 0, 199, 19900),
                new StatisticsValue(2646700), new HistogramValue(new long[]{8, 8, 8, 8, 8, 8, 8, 8, 8, 8}, 10, 110),
                new HistogramValue(new long[]{6, 16, 32, 64, 72, 0, 0}, 10, 0), new HistogramValue(new long[]{20, 20, 20}, 20, 120),
                new InstanceValue(records1)))));
    }
}
