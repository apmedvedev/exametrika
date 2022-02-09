/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.aggregator.model;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Test;

import com.exametrika.api.aggregator.common.model.IMetricName;
import com.exametrika.api.aggregator.common.model.IScopeName;
import com.exametrika.api.aggregator.common.model.Measurement;
import com.exametrika.api.aggregator.common.model.MeasurementSet;
import com.exametrika.api.aggregator.common.model.NameMeasurementId;
import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.NameValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.StandardValueSchemaConfiguration;
import com.exametrika.common.json.Json;
import com.exametrika.common.time.ITimeService;
import com.exametrika.impl.aggregator.common.meters.AggregatingMeasurementHandler;
import com.exametrika.impl.aggregator.common.model.MetricName;
import com.exametrika.impl.aggregator.common.model.ScopeName;
import com.exametrika.impl.aggregator.common.values.AggregationSchema;
import com.exametrika.impl.aggregator.common.values.ComponentValue;
import com.exametrika.impl.aggregator.common.values.NameValue;
import com.exametrika.impl.aggregator.common.values.StandardValue;
import com.exametrika.spi.aggregator.common.meters.IMeasurementHandler;


/**
 * The {@link AggregationHandlerTests} are tests for aggregating handler.
 *
 * @author Medvedev-A
 */
public class AggregationHandlerTests {
    @Test
    public void testHandler() throws Throwable {
        IScopeName scope = ScopeName.get("scope");
        IMetricName metric = MetricName.get("metric");
        NameMeasurementId id = new NameMeasurementId(scope, metric, "componentType");

        TestTimeService timeService = new TestTimeService();
        TestMeasurementHandler measurementHandler = new TestMeasurementHandler();
        ComponentValueSchemaConfiguration component = new ComponentValueSchemaConfiguration("componentType", Arrays.asList(new NameValueSchemaConfiguration("metric",
                Arrays.asList(new StandardValueSchemaConfiguration()))));
        AggregationSchema schema = new AggregationSchema(Collections.singleton(component), 1);
        AggregatingMeasurementHandler handler = new AggregatingMeasurementHandler(schema, 1000, timeService, measurementHandler);

        NameValue nameValue1 = new NameValue(Arrays.asList(new StandardValue(100, 1, 10, 1000)));
        NameValue nameValue2 = new NameValue(Arrays.asList(new StandardValue(200, 2, 20, 2000)));
        NameValue nameValue3 = new NameValue(Arrays.asList(new StandardValue(300, 1, 20, 3000)));

        ComponentValue value1 = new ComponentValue(Arrays.asList(nameValue1), Json.object().put("key", "value").toObject());
        ComponentValue value2 = new ComponentValue(Arrays.asList(nameValue2), Json.object().put("key", "value").toObject());
        ComponentValue value3 = new ComponentValue(Arrays.asList(nameValue3), Json.object().put("key", "value").toObject());

        MeasurementSet set1 = new MeasurementSet(Collections.singletonList(new Measurement(id, value1, 10, null)), null, 0, 123, 0);
        handler.handle(set1);

        MeasurementSet set21 = new MeasurementSet(Collections.singletonList(new Measurement(id, value1, 20, null)), null, 1, 123, 0);
        MeasurementSet set22 = new MeasurementSet(Collections.singletonList(new Measurement(id, value2, 20, null)), null, 1, 123, 0);
        handler.handle(set21);
        handler.handle(set22);

        handler.onTimer();

        assertThat(measurementHandler.measurements.isEmpty(), is(true));
        timeService.time = 6000;

        handler.onTimer();
        handler.onTimer();

        assertThat(measurementHandler.measurements.size(), is(1));
        MeasurementSet set3 = measurementHandler.measurements.get(0);
        assertThat(set3.getMeasurements().size(), is(1));
        assertThat(set3.getSchemaVersion(), is(1));
        assertThat(set3.getTime(), is(6000l));
        measurementHandler.measurements.clear();

        Measurement measurement = set3.getMeasurements().get(0);
        assertThat(measurement, is(new Measurement(id, value3, 100, null)));
        assertThat(measurement.getPeriod(), is(5897l));

        handler.handle(set21);

        timeService.time = 12000;
        handler.onTimer();

        assertThat(measurementHandler.measurements.size(), is(1));
        set3 = measurementHandler.measurements.get(0);
        assertThat(set3.getMeasurements().size(), is(1));
        assertThat(set3.getSchemaVersion(), is(1));
        assertThat(set3.getTime(), is(12000l));
        measurementHandler.measurements.clear();

        measurement = set3.getMeasurements().get(0);
        assertThat(measurement, is(new Measurement(id, value1, 100, null)));
        assertThat(measurement.getPeriod(), is(11897l));

        timeService.time = 23000;
        handler.onTimer();

        timeService.time = 34000;
        handler.onTimer();
        assertThat(measurementHandler.measurements.isEmpty(), is(true));
        //Map map = Tests.get(handler, "measurements");
        //assertThat(map.isEmpty(), is(false));
    }

    private static class TestTimeService implements ITimeService {
        private long time;

        @Override
        public long getCurrentTime() {
            return time;
        }
    }

    private static class TestMeasurementHandler implements IMeasurementHandler {
        public List<MeasurementSet> measurements = new ArrayList<MeasurementSet>();

        @Override
        public boolean canHandle() {
            return true;
        }

        @Override
        public void handle(MeasurementSet measurements) {
            this.measurements.add(measurements);
        }
    }
}
