/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.profiler.meters;

import static org.hamcrest.CoreMatchers.is;
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
import com.exametrika.api.aggregator.common.model.IScopeName;
import com.exametrika.api.aggregator.common.model.NameMeasurementId;
import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.common.json.JsonDiff;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.time.impl.SystemTimeService;
import com.exametrika.impl.aggregator.common.meters.Meters;
import com.exametrika.impl.aggregator.common.model.CallPath;
import com.exametrika.impl.aggregator.common.model.MeasurementIdProvider;
import com.exametrika.impl.aggregator.common.model.MetricName;
import com.exametrika.impl.aggregator.common.model.ScopeName;
import com.exametrika.impl.aggregator.common.values.HistogramValue;
import com.exametrika.impl.aggregator.common.values.InstanceRecord;
import com.exametrika.impl.aggregator.common.values.InstanceValue;
import com.exametrika.impl.aggregator.common.values.StackValue;
import com.exametrika.impl.aggregator.common.values.StandardValue;
import com.exametrika.impl.aggregator.common.values.StatisticsValue;
import com.exametrika.impl.profiler.probes.ProbeInstanceContextProvider;
import com.exametrika.impl.profiler.probes.StackCounter;
import com.exametrika.spi.aggregator.common.meters.IMeasurementProvider;
import com.exametrika.spi.aggregator.common.meters.config.FieldConfiguration;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.config.StackCounterConfiguration;
import com.exametrika.tests.profiler.support.TestMeasurementHandler;
import com.exametrika.tests.profiler.support.TestMeasurementProvider;

/**
 * The {@link StackCounterTests} are tests for {@link StackCounter} implementation.
 *
 * @author Medvedev-A
 * @see StackCounter
 */
public class StackCounterTests {
    @Test
    public void testStackCounter() throws Throwable {
        IScopeName scope = ScopeName.get("scope");
        MetricName metric1 = MetricName.get("metric1");
        MetricName metric2 = MetricName.get("metric2");
        CallPath callPath1 = CallPath.get(CallPath.root(), metric1);
        CallPath callPath2 = CallPath.get(callPath1, metric2);

        NameMeasurementId id1 = new NameMeasurementId(scope, CallPath.root(), "componentType");
        NameMeasurementId id2 = new NameMeasurementId(scope, callPath1, "componentType");
        NameMeasurementId id3 = new NameMeasurementId(scope, callPath2, "componentType");

        ProbeInstanceContextProvider instanceContextProvider = new ProbeInstanceContextProvider(new SystemTimeService());
        TestMeasurementProvider provider = new TestMeasurementProvider();
        TestStackCounterConfiguration configuration = new TestStackCounterConfiguration(true);
        StackCounter root = new StackCounter(configuration, 0,
                Meters.createFieldCollectors(configuration.getFields(), instanceContextProvider, new MeasurementIdProvider(id1)),
                Meters.createFieldCollectors(configuration.getFields(), instanceContextProvider, new MeasurementIdProvider(id1)),
                provider);
        StackCounter counter1 = new StackCounter(configuration, 0,
                Meters.createFieldCollectors(configuration.getFields(), instanceContextProvider, new MeasurementIdProvider(id2)),
                Meters.createFieldCollectors(configuration.getFields(), instanceContextProvider, new MeasurementIdProvider(id2)),
                provider);
        StackCounter counter2 = new StackCounter(configuration, 0,
                Meters.createFieldCollectors(configuration.getFields(), instanceContextProvider, new MeasurementIdProvider(id3)),
                Meters.createFieldCollectors(configuration.getFields(), instanceContextProvider, new MeasurementIdProvider(id3)),
                provider);

        provider.value = 10;
        counter1.beginMeasure();
        provider.value = 20;
        counter2.beginMeasure();
        provider.value = 30;
        counter2.endMeasure(false, counter1);
        provider.value = 40;
        counter1.endMeasure(false, root);

        StackValue value = counter1.extract(0, 0, 0, true);

        assertThat(value, is(new StackValue(Arrays.asList(new StandardValue(1, 20, 20, 20)),
                Arrays.asList(new StandardValue(1, 30, 30, 30)))));

        value = counter2.extract(0, 0, 0, true);
        assertThat(value, is(new StackValue(Arrays.asList(new StandardValue(1, 10, 10, 10)),
                Arrays.asList(new StandardValue(1, 10, 10, 10)))));
    }

    @Test
    public void testFieldContainers() throws Throwable {
        IScopeName scope = ScopeName.get("scope");
        MetricName metric1 = MetricName.get("metric1");
        MetricName metric2 = MetricName.get("metric2");
        CallPath callPath1 = CallPath.get(CallPath.root(), metric1);
        CallPath callPath2 = CallPath.get(callPath1, metric2);

        NameMeasurementId id1 = new NameMeasurementId(scope, callPath1, "componentType");
        NameMeasurementId id2 = new NameMeasurementId(scope, callPath2, "componentType");
        NameMeasurementId id3 = new NameMeasurementId(scope, CallPath.root(), "componentType");

        List<FieldConfiguration> fields =
                Arrays.<FieldConfiguration>asList(new StandardFieldConfiguration(), new StatisticsFieldConfiguration(),
                        new UniformHistogramFieldConfiguration(10, 90, 10),
                        new LogarithmicHistogramFieldConfiguration(10, 7),
                        new CustomHistogramFieldConfiguration(Arrays.<Long>asList(20l, 40l, 60l, 80l)),
                        new InstanceFieldConfiguration(10, true));

        ProbeInstanceContextProvider instanceContextProvider = new ProbeInstanceContextProvider(new SystemTimeService());
        TestMeasurementProvider provider = new TestMeasurementProvider();
        TestMeasurementHandler handler = new TestMeasurementHandler();
        TestStackCounterConfiguration configuration = new TestStackCounterConfiguration(true, fields);
        StackCounter root = new StackCounter(configuration, 0,
                Meters.createFieldCollectors(configuration.getFields(), instanceContextProvider, new MeasurementIdProvider(id3)),
                Meters.createFieldCollectors(configuration.getFields(), instanceContextProvider, new MeasurementIdProvider(id3)),
                provider);
        StackCounter counter1 = new StackCounter(configuration, 0,
                Meters.createFieldCollectors(configuration.getFields(), instanceContextProvider, new MeasurementIdProvider(id1)),
                Meters.createFieldCollectors(configuration.getFields(), instanceContextProvider, new MeasurementIdProvider(id1)),
                provider);
        StackCounter counter2 = new StackCounter(configuration, 0,
                Meters.createFieldCollectors(configuration.getFields(), instanceContextProvider, new MeasurementIdProvider(id2)),
                Meters.createFieldCollectors(configuration.getFields(), instanceContextProvider, new MeasurementIdProvider(id2)),
                provider);

        JsonObjectBuilder builder = new JsonObjectBuilder();
        builder.put("key", "value");
        instanceContextProvider.setContext(builder.toJson());

        List<InstanceRecord> records1 = new ArrayList<InstanceRecord>();
        for (int i = 0; i < 200; i++) {
            provider.value = 0;
            counter2.beginMeasure();
            provider.value = i;
            counter2.endMeasure(false, counter1);
        }

        records1.add(new InstanceRecord(id2, builder.toJson(), 199, 0));
        Collections.reverse(records1);

        StackValue result = counter2.extract(0, 0, 0, true);
        StackValue ethalon = new StackValue(Arrays.<IFieldValue>asList(new StandardValue(200, 0, 199, 19900),
                new StatisticsValue(2646700), new HistogramValue(new long[]{8, 8, 8, 8, 8, 8, 8, 8, 8, 8}, 10, 110),
                new HistogramValue(new long[]{6, 16, 32, 64, 72, 0, 0}, 10, 0), new HistogramValue(new long[]{20, 20, 20}, 20, 120),
                new InstanceValue(records1)), Arrays.<IFieldValue>asList(new StandardValue(200, 0, 199, 19900),
                new StatisticsValue(2646700), new HistogramValue(new long[]{8, 8, 8, 8, 8, 8, 8, 8, 8, 8}, 10, 110),
                new HistogramValue(new long[]{6, 16, 32, 64, 72, 0, 0}, 10, 0), new HistogramValue(new long[]{20, 20, 20}, 20, 120),
                new InstanceValue(records1)));
        if (!result.equals(ethalon)) {
            System.out.println(new JsonDiff(true).diff(result.toJson(), ethalon.toJson()));
            assertThat(result, is(ethalon));
        }

        handler.measurements.clear();

        provider.value = 10;
        counter1.beginMeasure();
        provider.value = 20;
        counter2.beginMeasure();
        provider.value = 30;
        counter2.endMeasure(false, counter1);
        provider.value = 40;
        counter1.endMeasure(false, root);

        result = counter1.extract(0, 10, 10, true);
        ethalon = new StackValue(Arrays.<IFieldValue>asList(new StandardValue(10, 20, 20, 200),
                new StatisticsValue(4000), new HistogramValue(new long[]{0, 10, 0, 0, 0, 0, 0, 0, 0, 0}, 0, 0),
                new HistogramValue(new long[]{0, 10, 0, 0, 0, 0, 0}, 0, 0), new HistogramValue(new long[]{10, 0, 0}, 0, 0),
                new InstanceValue(Arrays.asList(new InstanceRecord(id1, builder.toJson(), 20, 0)))), Arrays.<IFieldValue>asList(new StandardValue(10, 30, 30, 300),
                new StatisticsValue(9000), new HistogramValue(new long[]{0, 0, 10, 0, 0, 0, 0, 0, 0, 0}, 0, 0),
                new HistogramValue(new long[]{0, 10, 0, 0, 0, 0, 0}, 0, 0), new HistogramValue(new long[]{10, 0, 0}, 0, 0),
                new InstanceValue(Arrays.asList(new InstanceRecord(id1, builder.toJson(), 30, 0)))));
        assertThat(result, is(ethalon));

        if (!result.equals(ethalon)) {
            System.out.println(new JsonDiff(true).diff(result.toJson(), ethalon.toJson()));
            assertThat(result, is(ethalon));
        }
    }

    private static final class TestStackCounterConfiguration extends StackCounterConfiguration {
        public TestStackCounterConfiguration(boolean enabled) {
            super(enabled);
        }

        public TestStackCounterConfiguration(boolean enabled, List<? extends FieldConfiguration> fields) {
            super(enabled, fields);
        }

        @Override
        public String getMetricType() {
            return "metricType";
        }

        @Override
        public boolean isFast() {
            return false;
        }

        @Override
        public IMeasurementProvider createProvider(IProbeContext context) {
            return new TestMeasurementProvider();
        }
    }
}
