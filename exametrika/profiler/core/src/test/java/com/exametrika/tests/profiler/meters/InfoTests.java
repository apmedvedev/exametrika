/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.profiler.meters;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import com.exametrika.api.aggregator.common.model.IMetricName;
import com.exametrika.api.aggregator.common.model.IScopeName;
import com.exametrika.api.aggregator.common.model.NameMeasurementId;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.time.impl.SystemTimeService;
import com.exametrika.impl.aggregator.common.meters.Info;
import com.exametrika.impl.aggregator.common.meters.Meters;
import com.exametrika.impl.aggregator.common.model.MeasurementIdProvider;
import com.exametrika.impl.aggregator.common.model.MetricName;
import com.exametrika.impl.aggregator.common.model.ScopeName;
import com.exametrika.impl.aggregator.common.values.ObjectValue;
import com.exametrika.impl.profiler.probes.ProbeInstanceContextProvider;
import com.exametrika.spi.aggregator.common.meters.config.InfoConfiguration;
import com.exametrika.tests.profiler.support.TestMeasurementContext;
import com.exametrika.tests.profiler.support.TestMeasurementHandler;
import com.exametrika.tests.profiler.support.TestMeasurementProvider;

/**
 * The {@link InfoTests} are tests for {@link Info} implementation.
 *
 * @author Medvedev-A
 * @see Info
 */
public class InfoTests {
    @Test
    public void testInfo() throws Throwable {
        IScopeName scope = ScopeName.get("scope");
        IMetricName metric = MetricName.get("metric");
        NameMeasurementId id = new NameMeasurementId(scope, metric, "componentType");

        ProbeInstanceContextProvider instanceContextProvider = new ProbeInstanceContextProvider(new SystemTimeService());
        TestMeasurementProvider provider = new TestMeasurementProvider();
        TestMeasurementHandler handler = new TestMeasurementHandler();
        TestMeasurementContext context = new TestMeasurementContext(handler);
        Info info = Meters.createMeter(null, "metricType", new InfoConfiguration(true), new MeasurementIdProvider(id), provider,
                instanceContextProvider, context);

        JsonObject object = Json.object().put("key", "value").toObject();
        JsonObject object2 = Json.object().put("key1", "value1").toObject();

        ObjectValue value = info.extract(0, false, true);
        assertThat(value, nullValue());

        info.measure(object);
        value = info.extract(0, false, true);
        assertThat(info.extract(0, false, true), nullValue());

        assertThat(value, is(new ObjectValue(object)));

        info.measure(object2);
        value = info.extract(0, false, true);

        assertThat(value, is(new ObjectValue(object2)));

        info.measure(object);
        value = info.extract(0, false, true);

        assertThat(value, is(new ObjectValue(object)));

        value = info.extract(0, true, true);

        assertThat(value, is(new ObjectValue(object)));

        assertThat(info.hasProvider(), is(true));
        provider.value = object2;
        info.measure();
        value = info.extract(0, false, true);
        assertThat(info.extract(0, false, true), nullValue());

        assertThat(value, is(new ObjectValue(object2)));
    }
}
