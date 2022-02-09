/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.aggregator.fields;

import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import com.exametrika.api.aggregator.IPeriodName;
import com.exametrika.api.aggregator.IPeriodNameManager;
import com.exametrika.api.aggregator.common.model.IName;
import com.exametrika.api.aggregator.common.model.MeasurementId;
import com.exametrika.api.aggregator.common.model.NameMeasurementId;
import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.api.aggregator.common.values.config.ComponentValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.InstanceValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.NameValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.ObjectValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.StackValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.StandardValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.StatisticsValueSchemaConfiguration;
import com.exametrika.api.aggregator.common.values.config.UniformHistogramValueSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.ComponentRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.CustomHistogramRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.InstanceRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.LogarithmicHistogramRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.NameRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.ObjectRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.PercentageRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.PeriodRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.RateRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.StackRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.StandardRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.StatisticsRepresentationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.UniformHistogramRepresentationSchemaConfiguration;
import com.exametrika.common.config.resource.IResourceManager;
import com.exametrika.common.io.impl.ByteInputStream;
import com.exametrika.common.io.impl.ByteOutputStream;
import com.exametrika.common.io.impl.DataDeserialization;
import com.exametrika.common.io.impl.DataSerialization;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonDiff;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonSerializers;
import com.exametrika.common.utils.Classes;
import com.exametrika.common.utils.Collections;
import com.exametrika.common.utils.Pair;
import com.exametrika.impl.aggregator.common.model.CallPath;
import com.exametrika.impl.aggregator.common.model.DeserializeNameDictionary;
import com.exametrika.impl.aggregator.common.model.MetricName;
import com.exametrika.impl.aggregator.common.model.ScopeName;
import com.exametrika.impl.aggregator.common.model.SerializeNameDictionary;
import com.exametrika.impl.aggregator.common.values.AggregationContext;
import com.exametrika.impl.aggregator.common.values.ComponentBuilder;
import com.exametrika.impl.aggregator.common.values.ComponentValue;
import com.exametrika.impl.aggregator.common.values.HistogramValue;
import com.exametrika.impl.aggregator.common.values.InstanceRecord;
import com.exametrika.impl.aggregator.common.values.InstanceValue;
import com.exametrika.impl.aggregator.common.values.NameBuilder;
import com.exametrika.impl.aggregator.common.values.NameValue;
import com.exametrika.impl.aggregator.common.values.ObjectBuilder;
import com.exametrika.impl.aggregator.common.values.ObjectValue;
import com.exametrika.impl.aggregator.common.values.StackBuilder;
import com.exametrika.impl.aggregator.common.values.StackValue;
import com.exametrika.impl.aggregator.common.values.StandardValue;
import com.exametrika.impl.aggregator.common.values.StatisticsValue;
import com.exametrika.impl.aggregator.values.ComputeContext;
import com.exametrika.spi.aggregator.IComponentAccessor;
import com.exametrika.spi.aggregator.IComponentAccessorFactory;
import com.exametrika.spi.aggregator.IComponentComputer;
import com.exametrika.spi.aggregator.IComputeContext;
import com.exametrika.spi.aggregator.IMetricComputer;
import com.exametrika.spi.aggregator.INavigationAccessorFactory;
import com.exametrika.spi.aggregator.common.model.INameDictionary;
import com.exametrika.spi.aggregator.common.values.IComponentAggregator;
import com.exametrika.spi.aggregator.common.values.IComponentValueSerializer;
import com.exametrika.spi.aggregator.common.values.IMetricAggregator;
import com.exametrika.spi.aggregator.common.values.IMetricValueSerializer;


/**
 * The {@link ValueTests} are tests for aggregation field implementations.
 *
 * @author Medvedev-A
 * @see IResourceManager
 */
public class ValueTests {
    @Test
    public void testNameAggregators() {
        NameValueSchemaConfiguration nameField = new NameValueSchemaConfiguration("", Arrays.asList(new StandardValueSchemaConfiguration(),
                new StatisticsValueSchemaConfiguration(), new UniformHistogramValueSchemaConfiguration(0, 10, 3), new InstanceValueSchemaConfiguration(3, true)));

        IMetricAggregator aggregator = nameField.createAggregator();

        NameMeasurementId id = new NameMeasurementId(ScopeName.get("scope"), MetricName.get("metric"), "metricType");

        NameBuilder builder = (NameBuilder) nameField.createBuilder();
        NameValue value1 = new NameValue(Arrays.asList(new StandardValue(10, 1, 100, 1000), new StatisticsValue(1000),
                new HistogramValue(new long[]{1, 3, 1}, 2, 3), new InstanceValue(Arrays.asList(new InstanceRecord(id,
                        Json.object().put("key", "value").toObject(), 10, 123), new InstanceRecord(id,
                        Json.object().put("key", "value").toObject(), 100, 123)))));
        NameValue value11 = new NameValue(Arrays.asList(new StandardValue(10, 1, 100, 1000), new StatisticsValue(1000),
                new HistogramValue(new long[]{1, 3, 1}, 2, 3), new InstanceValue(Arrays.asList(new InstanceRecord(id,
                        Json.object().put("key", "value").toObject(), 100, 123)))));
        NameValue value2 = new NameValue(Arrays.asList(new StandardValue(10, 0, 200, 1000), new StatisticsValue(1000),
                new HistogramValue(new long[]{1, 3, 1}, 2, 3), new InstanceValue(Arrays.asList(new InstanceRecord(id,
                        Json.object().put("key", "value").toObject(), 1, 123), new InstanceRecord(id,
                        Json.object().put("key", "value").toObject(), 200, 123)))));
        NameValue value22 = new NameValue(Arrays.asList(new StandardValue(20, 0, 200, 2000), new StatisticsValue(2000),
                new HistogramValue(new long[]{2, 6, 2}, 4, 6), new InstanceValue(Arrays.asList(new InstanceRecord(id,
                        Json.object().put("key", "value").toObject(), 200, 123)))));

        AggregationContext context = new AggregationContext();
        aggregator.aggregate(builder, value1, context);
        IMetricValue result = builder.toValue();
        if (!result.equals(value11)) {
            System.out.println(new JsonDiff(true).diff(result.toJson(), value11.toJson()));
            assertThat(result, is((IMetricValue) value11));
        }

        NameBuilder builder2 = (NameBuilder) nameField.createBuilder();
        builder2.set(builder.toValue());

        assertThat(builder2, is(builder));

        aggregator.aggregate(builder, value2, context);
        assertThat(builder.toValue(), is((IMetricValue) value22));
    }

    @Test
    public void testStackAggregators() {
        StackValueSchemaConfiguration stackField = new StackValueSchemaConfiguration("", Arrays.asList(new StandardValueSchemaConfiguration(),
                new StatisticsValueSchemaConfiguration(), new UniformHistogramValueSchemaConfiguration(0, 10, 3), new InstanceValueSchemaConfiguration(3, true)));

        IMetricAggregator aggregator = stackField.createAggregator();

        NameMeasurementId id = new NameMeasurementId(ScopeName.get("scope"), CallPath.get("metric"), "metricType");

        StackBuilder builder = (StackBuilder) stackField.createBuilder();
        StackValue value1 = new StackValue(Arrays.asList(new StandardValue(10, 1, 100, 1000), new StatisticsValue(1000),
                new HistogramValue(new long[]{1, 3, 1}, 2, 3), new InstanceValue(Arrays.asList(new InstanceRecord(id,
                        Json.object().put("key", "value").toObject(), 10, 123), new InstanceRecord(id,
                        Json.object().put("key", "value").toObject(), 100, 123)))),
                Arrays.asList(new StandardValue(10, 1, 100, 1000), new StatisticsValue(1000),
                        new HistogramValue(new long[]{1, 3, 1}, 2, 3), new InstanceValue(Arrays.asList(new InstanceRecord(id,
                                Json.object().put("key", "value").toObject(), 10, 123), new InstanceRecord(id,
                                Json.object().put("key", "value").toObject(), 100, 123)))));
        StackValue value11 = new StackValue(Arrays.asList(new StandardValue(10, 1, 100, 1000), new StatisticsValue(1000),
                new HistogramValue(new long[]{1, 3, 1}, 2, 3), new InstanceValue(Arrays.asList(new InstanceRecord(id,
                        Json.object().put("key", "value").toObject(), 100, 123)))),
                Arrays.asList(new StandardValue(10, 1, 100, 1000), new StatisticsValue(1000),
                        new HistogramValue(new long[]{1, 3, 1}, 2, 3), new InstanceValue(Arrays.asList(new InstanceRecord(id,
                                Json.object().put("key", "value").toObject(), 100, 123)))));
        StackValue value2 = new StackValue(Arrays.asList(new StandardValue(10, 0, 200, 1000), new StatisticsValue(1000),
                new HistogramValue(new long[]{1, 3, 1}, 2, 3), new InstanceValue(Arrays.asList(new InstanceRecord(id,
                        Json.object().put("key", "value").toObject(), 1, 123), new InstanceRecord(id,
                        Json.object().put("key", "value").toObject(), 200, 123)))),
                Arrays.asList(new StandardValue(10, 0, 200, 1000), new StatisticsValue(1000),
                        new HistogramValue(new long[]{1, 3, 1}, 2, 3), new InstanceValue(Arrays.asList(new InstanceRecord(id,
                                Json.object().put("key", "value").toObject(), 1, 123), new InstanceRecord(id,
                                Json.object().put("key", "value").toObject(), 200, 123)))));
        StackValue value22 = new StackValue(Arrays.asList(new StandardValue(20, 0, 200, 2000), new StatisticsValue(2000),
                new HistogramValue(new long[]{2, 6, 2}, 4, 6), new InstanceValue(Arrays.asList(new InstanceRecord(id,
                        Json.object().put("key", "value").toObject(), 200, 123)))),
                Arrays.asList(new StandardValue(20, 0, 200, 2000), new StatisticsValue(2000),
                        new HistogramValue(new long[]{2, 6, 2}, 4, 6), new InstanceValue(Arrays.asList(new InstanceRecord(id,
                                Json.object().put("key", "value").toObject(), 200, 123)))));

        AggregationContext context = new AggregationContext();
        aggregator.aggregate(builder, value1, context);
        assertThat(builder.toValue(), is((IMetricValue) value11));

        StackBuilder builder2 = (StackBuilder) stackField.createBuilder();
        builder2.set(builder.toValue());

        assertThat(builder2, is(builder));

        aggregator.aggregate(builder, value2, context);
        assertThat(builder.toValue(), is((IMetricValue) value22));
    }

    @Test
    public void testObjectAggregators() {
        ObjectValueSchemaConfiguration objectField = new ObjectValueSchemaConfiguration("");

        IMetricAggregator aggregator = objectField.createAggregator();

        ObjectBuilder builder = (ObjectBuilder) objectField.createBuilder();
        ObjectValue value1 = new ObjectValue("hello");
        ObjectValue value2 = new ObjectValue("hello2");

        AggregationContext context = new AggregationContext();
        aggregator.aggregate(builder, value1, context);
        assertThat(builder.toValue(), is((IMetricValue) value1));

        ObjectBuilder builder2 = (ObjectBuilder) objectField.createBuilder();
        builder2.set(builder.toValue());

        assertThat(builder2, is(builder));

        aggregator.aggregate(builder, value2, context);
        assertThat(builder.toValue(), is((IMetricValue) value2));
    }

    @Test
    public void testComponentAggregators() {
        ComponentValueSchemaConfiguration componentField = new ComponentValueSchemaConfiguration("componentType", Arrays.asList(new NameValueSchemaConfiguration("", Arrays.asList(new StandardValueSchemaConfiguration(),
                new StatisticsValueSchemaConfiguration(), new UniformHistogramValueSchemaConfiguration(0, 10, 3), new InstanceValueSchemaConfiguration(3, true)))));

        IComponentAggregator aggregator = componentField.createAggregator();

        NameMeasurementId id = new NameMeasurementId(ScopeName.get("scope"), MetricName.get("metric"), "metricType");

        ComponentBuilder builder = (ComponentBuilder) componentField.createBuilder();
        ComponentValue value1 = new ComponentValue(Arrays.asList(new NameValue(Arrays.asList(new StandardValue(10, 1, 100, 1000), new StatisticsValue(1000),
                new HistogramValue(new long[]{1, 3, 1}, 2, 3), new InstanceValue(Arrays.asList(new InstanceRecord(id,
                        Json.object().put("key", "value").toObject(), 10, 123), new InstanceRecord(id,
                        Json.object().put("key", "value").toObject(), 100, 123)))))), null);
        ComponentValue value11 = new ComponentValue(Arrays.asList(new NameValue(Arrays.asList(new StandardValue(10, 1, 100, 1000), new StatisticsValue(1000),
                new HistogramValue(new long[]{1, 3, 1}, 2, 3), new InstanceValue(Arrays.asList(new InstanceRecord(id,
                        Json.object().put("key", "value").toObject(), 100, 123)))))), null);
        ComponentValue value2 = new ComponentValue(Arrays.asList(new NameValue(Arrays.asList(new StandardValue(10, 0, 200, 1000), new StatisticsValue(1000),
                new HistogramValue(new long[]{1, 3, 1}, 2, 3), new InstanceValue(Arrays.asList(new InstanceRecord(id,
                        Json.object().put("key", "value").toObject(), 1, 123), new InstanceRecord(id,
                        Json.object().put("key", "value").toObject(), 200, 123)))))), Json.object().put("key", "metadata").toObject());
        ComponentValue value22 = new ComponentValue(Arrays.asList(new NameValue(Arrays.asList(new StandardValue(20, 0, 200, 2000), new StatisticsValue(2000),
                new HistogramValue(new long[]{2, 6, 2}, 4, 6), new InstanceValue(Arrays.asList(new InstanceRecord(id,
                        Json.object().put("key", "value").toObject(), 200, 123)))))), Json.object().put("key", "metadata").toObject());

        AggregationContext context = new AggregationContext();
        aggregator.aggregate(builder, value1, context);
        assertThat(builder.toValue(), is((IComponentValue) value11));

        ComponentBuilder builder2 = (ComponentBuilder) componentField.createBuilder();
        builder2.set(builder.toValue());

        assertThat(builder2, is(builder));

        aggregator.aggregate(builder, value2, context);
        assertThat(builder.toValue(), is((IComponentValue) value22));
    }

    @Test
    public void testNameSerializers() throws Throwable {
        NameValueSchemaConfiguration nameField = new NameValueSchemaConfiguration("", Arrays.asList(new StandardValueSchemaConfiguration(),
                new StatisticsValueSchemaConfiguration(), new UniformHistogramValueSchemaConfiguration(0, 10, 3), new InstanceValueSchemaConfiguration(3, true)));

        IMetricValueSerializer builderSerializer = nameField.createSerializer(true);
        IMetricValueSerializer valueSerializer = nameField.createSerializer(false);

        TestNameDictionary testDictionary = new TestNameDictionary();
        DeserializeNameDictionary deserializeDictionary = new DeserializeNameDictionary(testDictionary, null);

        SerializeNameDictionary serializeDictionary = new SerializeNameDictionary();
        ByteOutputStream outputStream = new ByteOutputStream();
        DataSerialization serialization = new DataSerialization(outputStream);
        serialization.setExtension(SerializeNameDictionary.EXTENTION_ID, serializeDictionary);

        NameMeasurementId id = new NameMeasurementId(ScopeName.get("scope"), MetricName.get("metric"), "metricType");
        MeasurementId id2 = new MeasurementId(1, 2, "metricType");

        NameBuilder builder = (NameBuilder) nameField.createBuilder();

        NameValue value1 = new NameValue(Arrays.asList(new StandardValue(10, 1, 100, 1000), new StatisticsValue(1000),
                new HistogramValue(new long[]{1, 3, 1}, 2, 3), new InstanceValue(Arrays.asList(new InstanceRecord(id,
                        Json.object().put("key", "value").toObject(), 10, 123), new InstanceRecord(id,
                        Json.object().put("key", "value").toObject(), 100, 123)))));
        builder.set(value1);
        NameValue value11 = new NameValue(Arrays.asList(new StandardValue(10, 1, 100, 1000), new StatisticsValue(1000),
                new HistogramValue(new long[]{1, 3, 1}, 2, 3), new InstanceValue(Arrays.asList(new InstanceRecord(id2,
                        Json.object().put("key", "value").toObject(), 100, 123), new InstanceRecord(id2,
                        Json.object().put("key", "value").toObject(), 10, 123)))));
        NameValue value12 = new NameValue(Arrays.asList(new StandardValue(10, 1, 100, 1000), new StatisticsValue(1000),
                new HistogramValue(new long[]{1, 3, 1}, 2, 3), new InstanceValue(Arrays.asList(new InstanceRecord(id2,
                        Json.object().put("key", "value").toObject(), 10, 123), new InstanceRecord(id2,
                        Json.object().put("key", "value").toObject(), 100, 123)))));
        valueSerializer.serialize(serialization, value1);
        builderSerializer.serialize(serialization, builder);

        ByteInputStream inputStream = new ByteInputStream(outputStream.getBuffer(), 0, outputStream.getLength());
        DataDeserialization deserialization = new DataDeserialization(inputStream);
        deserialization.setExtension(DeserializeNameDictionary.EXTENTION_ID, deserializeDictionary);
        inputStream.close();
        outputStream.close();

        NameValue value2 = (NameValue) valueSerializer.deserialize(deserialization);
        NameBuilder builder2 = (NameBuilder) builderSerializer.deserialize(deserialization);

        assertThat(value2, is(value12));
        assertThat(value11, is(builder2.toValue()));
    }

    @Test
    public void testStackSerializers() throws Throwable {
        StackValueSchemaConfiguration stackField = new StackValueSchemaConfiguration("", Arrays.asList(new StandardValueSchemaConfiguration(),
                new StatisticsValueSchemaConfiguration(), new UniformHistogramValueSchemaConfiguration(0, 10, 3), new InstanceValueSchemaConfiguration(3, true)));

        IMetricValueSerializer builderSerializer = stackField.createSerializer(true);
        IMetricValueSerializer valueSerializer = stackField.createSerializer(false);

        TestNameDictionary testDictionary = new TestNameDictionary();
        DeserializeNameDictionary deserializeDictionary = new DeserializeNameDictionary(testDictionary, null);

        SerializeNameDictionary serializeDictionary = new SerializeNameDictionary();
        ByteOutputStream outputStream = new ByteOutputStream();
        DataSerialization serialization = new DataSerialization(outputStream);
        serialization.setExtension(SerializeNameDictionary.EXTENTION_ID, serializeDictionary);

        NameMeasurementId id = new NameMeasurementId(ScopeName.get("scope"), MetricName.get("metric"), "metricType");
        MeasurementId id2 = new MeasurementId(1, 2, "metricType");

        StackBuilder builder = (StackBuilder) stackField.createBuilder();

        StackValue value1 = new StackValue(Arrays.asList(new StandardValue(10, 1, 100, 1000), new StatisticsValue(1000),
                new HistogramValue(new long[]{1, 3, 1}, 2, 3), new InstanceValue(Arrays.asList(new InstanceRecord(id,
                        Json.object().put("key", "value").toObject(), 10, 123), new InstanceRecord(id,
                        Json.object().put("key", "value").toObject(), 100, 123)))),
                Arrays.asList(new StandardValue(10, 1, 100, 1000), new StatisticsValue(1000),
                        new HistogramValue(new long[]{1, 3, 1}, 2, 3), new InstanceValue(Arrays.asList(new InstanceRecord(id,
                                Json.object().put("key", "value").toObject(), 10, 123), new InstanceRecord(id,
                                Json.object().put("key", "value").toObject(), 100, 123)))));
        builder.set(value1);
        StackValue value11 = new StackValue(Arrays.asList(new StandardValue(10, 1, 100, 1000), new StatisticsValue(1000),
                new HistogramValue(new long[]{1, 3, 1}, 2, 3), new InstanceValue(Arrays.asList(new InstanceRecord(id2,
                        Json.object().put("key", "value").toObject(), 100, 123), new InstanceRecord(id2,
                        Json.object().put("key", "value").toObject(), 10, 123)))),
                Arrays.asList(new StandardValue(10, 1, 100, 1000), new StatisticsValue(1000),
                        new HistogramValue(new long[]{1, 3, 1}, 2, 3), new InstanceValue(Arrays.asList(new InstanceRecord(id2,
                                Json.object().put("key", "value").toObject(), 100, 123), new InstanceRecord(id2,
                                Json.object().put("key", "value").toObject(), 10, 123)))));
        StackValue value12 = new StackValue(Arrays.asList(new StandardValue(10, 1, 100, 1000), new StatisticsValue(1000),
                new HistogramValue(new long[]{1, 3, 1}, 2, 3), new InstanceValue(Arrays.asList(new InstanceRecord(id2,
                        Json.object().put("key", "value").toObject(), 10, 123), new InstanceRecord(id2,
                        Json.object().put("key", "value").toObject(), 100, 123)))),
                Arrays.asList(new StandardValue(10, 1, 100, 1000), new StatisticsValue(1000),
                        new HistogramValue(new long[]{1, 3, 1}, 2, 3), new InstanceValue(Arrays.asList(new InstanceRecord(id2,
                                Json.object().put("key", "value").toObject(), 10, 123), new InstanceRecord(id2,
                                Json.object().put("key", "value").toObject(), 100, 123)))));
        valueSerializer.serialize(serialization, value1);
        builderSerializer.serialize(serialization, builder);

        ByteInputStream inputStream = new ByteInputStream(outputStream.getBuffer(), 0, outputStream.getLength());
        DataDeserialization deserialization = new DataDeserialization(inputStream);
        deserialization.setExtension(DeserializeNameDictionary.EXTENTION_ID, deserializeDictionary);

        StackValue value2 = (StackValue) valueSerializer.deserialize(deserialization);
        StackBuilder builder2 = (StackBuilder) builderSerializer.deserialize(deserialization);

        assertThat(value2, is(value12));
        assertThat(value11, is(builder2.toValue()));

        inputStream.close();
        outputStream.close();
    }

    @Test
    public void testComponentSerializers() throws Throwable {
        ComponentValueSchemaConfiguration nameField = new ComponentValueSchemaConfiguration("componentType", Arrays.asList(new NameValueSchemaConfiguration("", Arrays.asList(new StandardValueSchemaConfiguration(),
                new StatisticsValueSchemaConfiguration(), new UniformHistogramValueSchemaConfiguration(0, 10, 3), new InstanceValueSchemaConfiguration(3, true)))));

        IComponentValueSerializer builderSerializer = nameField.createSerializer(true);
        IComponentValueSerializer valueSerializer = nameField.createSerializer(false);

        TestNameDictionary testDictionary = new TestNameDictionary();
        DeserializeNameDictionary deserializeDictionary = new DeserializeNameDictionary(testDictionary, null);

        SerializeNameDictionary serializeDictionary = new SerializeNameDictionary();
        ByteOutputStream outputStream = new ByteOutputStream();
        DataSerialization serialization = new DataSerialization(outputStream);
        serialization.setExtension(SerializeNameDictionary.EXTENTION_ID, serializeDictionary);

        NameMeasurementId id = new NameMeasurementId(ScopeName.get("scope"), MetricName.get("metric"), "metricType");
        MeasurementId id2 = new MeasurementId(1, 2, "metricType");

        ComponentBuilder builder = (ComponentBuilder) nameField.createBuilder();

        ComponentValue value1 = new ComponentValue(Arrays.asList(new NameValue(Arrays.asList(new StandardValue(10, 1, 100, 1000), new StatisticsValue(1000),
                new HistogramValue(new long[]{1, 3, 1}, 2, 3), new InstanceValue(Arrays.asList(new InstanceRecord(id,
                        Json.object().put("key", "value").toObject(), 10, 123), new InstanceRecord(id,
                        Json.object().put("key", "value").toObject(), 100, 123)))))), Json.object().put("key", "value").toObject());
        builder.set(value1);
        ComponentValue value11 = new ComponentValue(Arrays.asList(new NameValue(Arrays.asList(new StandardValue(10, 1, 100, 1000), new StatisticsValue(1000),
                new HistogramValue(new long[]{1, 3, 1}, 2, 3), new InstanceValue(Arrays.asList(new InstanceRecord(id2,
                        Json.object().put("key", "value").toObject(), 100, 123), new InstanceRecord(id2,
                        Json.object().put("key", "value").toObject(), 10, 123)))))), Json.object().put("key", "value").toObject());
        ComponentValue value12 = new ComponentValue(Arrays.asList(new NameValue(Arrays.asList(new StandardValue(10, 1, 100, 1000), new StatisticsValue(1000),
                new HistogramValue(new long[]{1, 3, 1}, 2, 3), new InstanceValue(Arrays.asList(new InstanceRecord(id2,
                        Json.object().put("key", "value").toObject(), 10, 123), new InstanceRecord(id2,
                        Json.object().put("key", "value").toObject(), 100, 123)))))), Json.object().put("key", "value").toObject());
        valueSerializer.serialize(serialization, value1, true);
        builderSerializer.serialize(serialization, builder, true);

        ByteInputStream inputStream = new ByteInputStream(outputStream.getBuffer(), 0, outputStream.getLength());
        DataDeserialization deserialization = new DataDeserialization(inputStream);
        deserialization.setExtension(DeserializeNameDictionary.EXTENTION_ID, deserializeDictionary);

        ComponentValue value2 = (ComponentValue) valueSerializer.deserialize(deserialization, true, null);
        ComponentBuilder builder2 = (ComponentBuilder) builderSerializer.deserialize(deserialization, true, null);

        assertThat(value2, is(value12));
        assertThat(value11, is(builder2.toValue()));

        inputStream.close();
        outputStream.close();
    }

    @Test
    public void testObjectSerializers() throws Throwable {
        ObjectValueSchemaConfiguration objectField = new ObjectValueSchemaConfiguration("");

        IMetricValueSerializer builderSerializer = objectField.createSerializer(true);
        IMetricValueSerializer valueSerializer = objectField.createSerializer(false);

        ByteOutputStream outputStream = new ByteOutputStream();
        DataSerialization serialization = new DataSerialization(outputStream);

        ObjectBuilder builder = (ObjectBuilder) objectField.createBuilder();

        ObjectValue value1 = new ObjectValue("hello");
        builder.set(value1);
        valueSerializer.serialize(serialization, value1);
        builderSerializer.serialize(serialization, builder);

        ByteInputStream inputStream = new ByteInputStream(outputStream.getBuffer(), 0, outputStream.getLength());
        DataDeserialization deserialization = new DataDeserialization(inputStream);

        ObjectValue value2 = (ObjectValue) valueSerializer.deserialize(deserialization);
        ObjectBuilder builder2 = (ObjectBuilder) builderSerializer.deserialize(deserialization);

        inputStream.close();
        outputStream.close();

        assertThat(value2, is(value1));
        assertThat(value1, is(builder2.toValue()));
    }

    @Test
    public void testNameComputers() {
        NameValueSchemaConfiguration nameField = new NameValueSchemaConfiguration("name", Arrays.asList(new StandardValueSchemaConfiguration(),
                new StatisticsValueSchemaConfiguration(), new UniformHistogramValueSchemaConfiguration(0, 10, 3), new InstanceValueSchemaConfiguration(3, true)));
        ComponentValueSchemaConfiguration schema = new ComponentValueSchemaConfiguration("test", Arrays.asList(nameField));
        NameRepresentationSchemaConfiguration nameRepresentation = new NameRepresentationSchemaConfiguration("name",
                Arrays.asList(new StandardRepresentationSchemaConfiguration(true),
                        new StatisticsRepresentationSchemaConfiguration(true), new UniformHistogramRepresentationSchemaConfiguration(10,
                                100, 3, true, true, true, true, Arrays.asList(10, 25, 50, 75, 90), true),
                        new InstanceRepresentationSchemaConfiguration(true), new RateRepresentationSchemaConfiguration("std.sum", true),
                        new RateRepresentationSchemaConfiguration("myRate", "histo.bins", true),
                        new PeriodRepresentationSchemaConfiguration(null, "test", "std.avg", true), new PeriodRepresentationSchemaConfiguration("myPeriod", "test", "histo.bins", true),
                        new PercentageRepresentationSchemaConfiguration("test", "std.count", true),
                        new PercentageRepresentationSchemaConfiguration("test", "rate(std.sum)", true),
                        new PercentageRepresentationSchemaConfiguration("test", "myRate", true),
                        new PercentageRepresentationSchemaConfiguration("myPercentage", "test", null, "testNode", "std.count", "std.count", true)));

        ComponentRepresentationSchemaConfiguration componentRepresentation = new ComponentRepresentationSchemaConfiguration("test",
                java.util.Collections.singletonMap("name", nameRepresentation));
        IMetricComputer computer = nameRepresentation.createComputer(schema, componentRepresentation, componentRepresentation.createAccessorFactory(schema), 0);

        MeasurementId id = new MeasurementId(1, 2, "metricType");
        NameBuilder builder = (NameBuilder) nameField.createBuilder();
        NameValue value1 = new NameValue(Arrays.asList(new StandardValue(10, 1, 100, 1000), new StatisticsValue(120000),
                new HistogramValue(new long[]{1, 3, 1}, 2, 3), new InstanceValue(Arrays.asList(new InstanceRecord(id,
                        Json.object().put("key", "value").toObject(), 10, 123), new InstanceRecord(id,
                        Json.object().put("key", "value").toObject(), 100, 123)))));
        NameValue value2 = new NameValue(Arrays.asList(new StandardValue(20, 0, 200, 3000), new StatisticsValue(2000),
                new HistogramValue(new long[]{2, 6, 2}, 4, 6), new InstanceValue(Arrays.asList(new InstanceRecord(id,
                        Json.object().put("key", "value").toObject(), 1, 123), new InstanceRecord(id,
                        Json.object().put("key", "value").toObject(), 200, 123)))));
        builder.set(value1);

        TestPeriodNameManager nameManager = new TestPeriodNameManager(Arrays.asList(new TestPeriodName(1, ScopeName.get("scope")),
                new TestPeriodName(2, MetricName.get("metric"))));

        TestNode node = new TestNode();
        node.value2 = new ComponentValue(Arrays.asList(value2), null);

        ComputeContext context = new ComputeContext();
        context.setNodeType("testNode");
        context.setObject(node);
        context.setPeriod(10000);
        context.setNameManager(nameManager);

        JsonObject res = (JsonObject) computer.compute(null, builder, context);
        res = JsonSerializers.read(res.toString(), false);

        JsonObject ethalon = JsonSerializers.load("classpath:" + Classes.getResourcePath(getClass()) + "/data/data1.json", false);
        assertThat(res, is(ethalon));

        context = new ComputeContext();
        context.setObject(node);
        context.setNameManager(nameManager);

        res = (JsonObject) computer.compute(null, builder, context);
        res = JsonSerializers.read(res.toString(), false);

        ethalon = JsonSerializers.load("classpath:" + Classes.getResourcePath(getClass()) + "/data/data2.json", false);
        assertThat(res, is(ethalon));

        assertThat(new UniformHistogramRepresentationSchemaConfiguration(10, 100, 3, true, true, true, true,
                        Arrays.asList(10, 25, 50, 75, 90), true).getScale(),
                is(Json.array().add(10).add(40).add(70).add(100).toArray()));
        assertThat(new LogarithmicHistogramRepresentationSchemaConfiguration(10, 3, true, true, true, true,
                        Arrays.asList(10, 25, 50, 75, 90), true).getScale(),
                is(Json.array().add(10).add(20).add(40).add(80).toArray()));
        assertThat(new CustomHistogramRepresentationSchemaConfiguration(Arrays.asList(10l, 12l, 14l, 18l), true, true, true, true,
                        Arrays.asList(10, 25, 50, 75, 90), true).getScale(),
                is(Json.array().add(10).add(12).add(14).add(18).toArray()));
    }

    @Test
    public void testStackComputers() {
        StackValueSchemaConfiguration stackField = new StackValueSchemaConfiguration("stack", Arrays.asList(new StandardValueSchemaConfiguration(),
                new StatisticsValueSchemaConfiguration(), new UniformHistogramValueSchemaConfiguration(0, 10, 3), new InstanceValueSchemaConfiguration(3, true)));
        ComponentValueSchemaConfiguration schema = new ComponentValueSchemaConfiguration("test", Arrays.asList(stackField));
        StackRepresentationSchemaConfiguration stackRepresentation = new StackRepresentationSchemaConfiguration("stack",
                Arrays.asList(new StandardRepresentationSchemaConfiguration(true),
                        new StatisticsRepresentationSchemaConfiguration(true),
                        new UniformHistogramRepresentationSchemaConfiguration(10, 100, 3, true, true, true, true, Arrays.asList(10, 25, 50, 75, 90), true),
                        new InstanceRepresentationSchemaConfiguration(true), new RateRepresentationSchemaConfiguration("std.sum", true),
                        new RateRepresentationSchemaConfiguration("myRate", "histo.bins", true),
                        new PeriodRepresentationSchemaConfiguration(null, "test", "std.avg", true), new PeriodRepresentationSchemaConfiguration("myPeriod", "test", "histo.bins", true),
                        new PercentageRepresentationSchemaConfiguration("test", "std.count", true),
                        new PercentageRepresentationSchemaConfiguration("test", "rate(std.sum)", true),
                        new PercentageRepresentationSchemaConfiguration("test", "myRate", true),
                        new PercentageRepresentationSchemaConfiguration(null, "test", null, "testNode", "std.count", "std.count", true),
                        new PercentageRepresentationSchemaConfiguration(null, "test", null, "testNode", "std.count", "total.std.count", true),
                        new PercentageRepresentationSchemaConfiguration("myPercentage", "test", null, "testNode", "inherent.std.count", "std.count", true)
                ));

        ComponentRepresentationSchemaConfiguration componentRepresentation = new ComponentRepresentationSchemaConfiguration("test",
                java.util.Collections.singletonMap("stack", stackRepresentation));
        IMetricComputer computer = stackRepresentation.createComputer(schema, componentRepresentation, componentRepresentation.createAccessorFactory(schema), 0);

        MeasurementId id = new MeasurementId(1, 2, "metricType");
        StackBuilder builder = (StackBuilder) stackField.createBuilder();
        StackValue value1 = new StackValue(Arrays.asList(new StandardValue(10, 1, 100, 1000), new StatisticsValue(120000),
                new HistogramValue(new long[]{1, 3, 1}, 2, 3), new InstanceValue(Arrays.asList(new InstanceRecord(id,
                        Json.object().put("key", "value").toObject(), 10, 123), new InstanceRecord(id,
                        Json.object().put("key", "value").toObject(), 100, 123)))),
                Arrays.asList(new StandardValue(10, 1, 100, 1000), new StatisticsValue(120000),
                        new HistogramValue(new long[]{1, 3, 1}, 2, 3), new InstanceValue(Arrays.asList(new InstanceRecord(id,
                                Json.object().put("key", "value").toObject(), 10, 123), new InstanceRecord(id,
                                Json.object().put("key", "value").toObject(), 100, 123)))));
        StackValue value2 = new StackValue(Arrays.asList(new StandardValue(20, 0, 200, 3000), new StatisticsValue(2000),
                new HistogramValue(new long[]{2, 6, 2}, 4, 6), new InstanceValue(Arrays.asList(new InstanceRecord(id,
                        Json.object().put("key", "value").toObject(), 1, 123), new InstanceRecord(id,
                        Json.object().put("key", "value").toObject(), 200, 123)))),
                Arrays.asList(new StandardValue(20, 0, 200, 3000), new StatisticsValue(2000),
                        new HistogramValue(new long[]{2, 6, 2}, 4, 6), new InstanceValue(Arrays.asList(new InstanceRecord(id,
                                Json.object().put("key", "value").toObject(), 1, 123), new InstanceRecord(id,
                                Json.object().put("key", "value").toObject(), 200, 123)))));
        builder.set(value1);

        TestPeriodNameManager nameManager = new TestPeriodNameManager(Arrays.asList(new TestPeriodName(1, ScopeName.get("scope")),
                new TestPeriodName(2, MetricName.get("metric"))));

        TestNode node = new TestNode();
        node.value2 = new ComponentValue(Arrays.asList(value2), null);

        ComputeContext context = new ComputeContext();
        context.setNodeType("testNode");
        context.setObject(node);
        context.setPeriod(10000);
        context.setNameManager(nameManager);

        JsonObject res = (JsonObject) computer.compute(null, builder, context);
        res = JsonSerializers.read(res.toString(), false);

        JsonObject ethalon = JsonSerializers.load("classpath:" + Classes.getResourcePath(getClass()) + "/data/data3.json", false);
        assertThat(res, is(ethalon));

        context = new ComputeContext();
        context.setObject(node);
        context.setNameManager(nameManager);

        res = (JsonObject) computer.compute(null, builder, context);
        res = JsonSerializers.read(res.toString(), false);

        ethalon = JsonSerializers.load("classpath:" + Classes.getResourcePath(getClass()) + "/data/data4.json", false);
        assertThat(res, is(ethalon));
    }

    @Test
    public void testObjectComputers() {
        ObjectRepresentationSchemaConfiguration objectRepresentation = new ObjectRepresentationSchemaConfiguration("object");
        ObjectRepresentationSchemaConfiguration objectRepresentation2 = new TestRepresentationSchemaConfiguration();
        ComputeContext context = new ComputeContext();
        IMetricComputer computer = objectRepresentation.createComputer(null, null, null, 0);
        IMetricComputer computer2 = objectRepresentation2.createComputer(null, null, null, 0);

        assertThat(computer.compute(null, new ObjectValue(0), context), is((Object) 0l));

        assertThat((String) computer2.compute(null, new ObjectValue(0), context), is("first"));
        assertThat((String) computer2.compute(null, new ObjectValue(100), context), is("other"));
    }

    @Test
    public void testComponentComputers() {
        ComponentValueSchemaConfiguration nameField = new ComponentValueSchemaConfiguration("componentType",
                Arrays.asList(new NameValueSchemaConfiguration("name", Arrays.asList(new StandardValueSchemaConfiguration(),
                        new StatisticsValueSchemaConfiguration(), new UniformHistogramValueSchemaConfiguration(0, 10, 3), new InstanceValueSchemaConfiguration(3, true)))));

        ComponentRepresentationSchemaConfiguration nameRepresentation = new ComponentRepresentationSchemaConfiguration("component",
                java.util.Collections.singletonMap("name", new NameRepresentationSchemaConfiguration("name",
                        Arrays.asList(new StandardRepresentationSchemaConfiguration(true),
                                new StatisticsRepresentationSchemaConfiguration(true), new UniformHistogramRepresentationSchemaConfiguration(
                                        10, 100, 3, true, true, true, true, Arrays.asList(10, 25, 50, 75, 90), true),
                                new InstanceRepresentationSchemaConfiguration(true), new RateRepresentationSchemaConfiguration("std.sum", true),
                                new RateRepresentationSchemaConfiguration("myRate", "histo.bins", true),
                                new PeriodRepresentationSchemaConfiguration(null, "test", "std.avg", true), new PeriodRepresentationSchemaConfiguration("myPeriod", "test", "histo.bins", true),
                                new PercentageRepresentationSchemaConfiguration("test", "std.count", true),
                                new PercentageRepresentationSchemaConfiguration("test", "rate(std.sum)", true),
                                new PercentageRepresentationSchemaConfiguration("test", "myRate", true),
                                new PercentageRepresentationSchemaConfiguration("myPercentage", "test", null, "testNode", "std.count", "std.count", true)))));

        IComponentComputer computer = nameRepresentation.createComputer(nameField);

        MeasurementId id = new MeasurementId(1, 2, "metricType");
        ComponentBuilder builder = (ComponentBuilder) nameField.createBuilder();
        ComponentValue value1 = new ComponentValue(Arrays.asList(new NameValue(Arrays.asList(new StandardValue(10, 1, 100, 1000), new StatisticsValue(120000),
                new HistogramValue(new long[]{1, 3, 1}, 2, 3), new InstanceValue(Arrays.asList(new InstanceRecord(id,
                        Json.object().put("key", "value").toObject(), 10, 123), new InstanceRecord(id,
                        Json.object().put("key", "value").toObject(), 100, 123)))))), Json.object().put("key", "value").toObject());
        ComponentValue value2 = new ComponentValue(Arrays.asList(new NameValue(Arrays.asList(new StandardValue(20, 0, 200, 3000), new StatisticsValue(2000),
                new HistogramValue(new long[]{2, 6, 2}, 4, 6), new InstanceValue(Arrays.asList(new InstanceRecord(id,
                        Json.object().put("key", "value").toObject(), 1, 123), new InstanceRecord(id,
                        Json.object().put("key", "value").toObject(), 200, 123)))))), null);
        builder.set(value1);

        TestPeriodNameManager nameManager = new TestPeriodNameManager(Arrays.asList(new TestPeriodName(1, ScopeName.get("scope")),
                new TestPeriodName(2, MetricName.get("metric"))));

        TestNode node = new TestNode();
        node.value2 = value2;

        ComputeContext context = new ComputeContext();
        context.setNodeType("testNode");
        context.setObject(node);
        context.setPeriod(10000);
        context.setNameManager(nameManager);

        JsonObject res = (JsonObject) computer.compute(builder, context, false, true);
        res = JsonSerializers.read(res.toString(), false);
        JsonObject ethalon = JsonSerializers.load("classpath:" + Classes.getResourcePath(getClass()) + "/data/data7.json", false);
        assertThat(res, is(ethalon));

        context = new ComputeContext();
        context.setObject(node);
        context.setNameManager(nameManager);

        res = (JsonObject) computer.compute(builder, context, false, true);
        res = JsonSerializers.read(res.toString(), false);

        ethalon = JsonSerializers.load("classpath:" + Classes.getResourcePath(getClass()) + "/data/data8.json", false);
        assertThat(res, is(ethalon));
    }

    @Test
    public void testPartialComponentComputers() {
        ComponentValueSchemaConfiguration nameField = new ComponentValueSchemaConfiguration("componentType",
                Arrays.asList(new NameValueSchemaConfiguration("name2", Arrays.asList(new StandardValueSchemaConfiguration())),
                        new NameValueSchemaConfiguration("name", Arrays.asList(new StandardValueSchemaConfiguration(),
                                new StatisticsValueSchemaConfiguration(), new UniformHistogramValueSchemaConfiguration(0, 10, 3), new InstanceValueSchemaConfiguration(3, true)))));

        ComponentRepresentationSchemaConfiguration nameRepresentation = new ComponentRepresentationSchemaConfiguration("component",
                java.util.Collections.singletonMap("name", new NameRepresentationSchemaConfiguration("name",
                        Arrays.asList(new StandardRepresentationSchemaConfiguration(false),
                                new StatisticsRepresentationSchemaConfiguration(false), new UniformHistogramRepresentationSchemaConfiguration(
                                        10, 100, 3, true, true, true, true, Arrays.asList(10, 25, 50, 75, 90), false),
                                new InstanceRepresentationSchemaConfiguration(false), new RateRepresentationSchemaConfiguration("std.sum", true),
                                new RateRepresentationSchemaConfiguration("myRate", "histo.bins", true),
                                new PeriodRepresentationSchemaConfiguration(null, "test", "std.avg", true), new PeriodRepresentationSchemaConfiguration("myPeriod", "test", "histo.bins", true),
                                new PercentageRepresentationSchemaConfiguration("test", "std.count", true),
                                new PercentageRepresentationSchemaConfiguration("test", "rate(std.sum)", true),
                                new PercentageRepresentationSchemaConfiguration("test", "myRate", true),
                                new PercentageRepresentationSchemaConfiguration("myPercentage", "test", null, "testNode", "std.count", "std.count", true)))));

        IComponentComputer computer = nameRepresentation.createComputer(nameField);

        MeasurementId id = new MeasurementId(1, 2, "metricType");
        ComponentBuilder builder = (ComponentBuilder) nameField.createBuilder();
        ComponentValue value1 = new ComponentValue(Arrays.asList(new NameValue(Arrays.asList(new StandardValue(10, 1, 100, 1000))),
                new NameValue(Arrays.asList(new StandardValue(10, 1, 100, 1000), new StatisticsValue(120000),
                        new HistogramValue(new long[]{1, 3, 1}, 2, 3), new InstanceValue(Arrays.asList(new InstanceRecord(id,
                                Json.object().put("key", "value").toObject(), 10, 123), new InstanceRecord(id,
                                Json.object().put("key", "value").toObject(), 100, 123)))))), Json.object().put("key", "value").toObject());
        ComponentValue value2 = new ComponentValue(Arrays.asList(new NameValue(Arrays.asList(new StandardValue(10, 1, 100, 1000))),
                new NameValue(Arrays.asList(new StandardValue(20, 0, 200, 3000), new StatisticsValue(2000),
                        new HistogramValue(new long[]{2, 6, 2}, 4, 6), new InstanceValue(Arrays.asList(new InstanceRecord(id,
                                Json.object().put("key", "value").toObject(), 1, 123), new InstanceRecord(id,
                                Json.object().put("key", "value").toObject(), 200, 123)))))), null);
        builder.set(value1);

        TestPeriodNameManager nameManager = new TestPeriodNameManager(Arrays.asList(new TestPeriodName(1, ScopeName.get("scope")),
                new TestPeriodName(2, MetricName.get("metric"))));

        TestNode node = new TestNode();
        node.value2 = value2;

        ComputeContext context = new ComputeContext();
        context.setNodeType("testNode");
        context.setObject(node);
        context.setPeriod(10000);
        context.setNameManager(nameManager);

        JsonObject res = (JsonObject) computer.compute(builder, context, false, true);
        res = JsonSerializers.read(res.toString(), false);
        JsonObject ethalon = JsonSerializers.load("classpath:" + Classes.getResourcePath(getClass()) + "/data/data9.json", false);
        assertThat(res, is(ethalon));
    }

    private static class TestNameDictionary implements INameDictionary {
        private long id = 1;
        private Map<Long, Object> names = new HashMap<Long, Object>();

        @Override
        public long getName(IName name) {
            long id = this.id++;
            assertThat(names.put(id, name), nullValue());
            return id;
        }

        @Override
        public long getCallPath(long parentCallPathId, long metricId) {
            long id = this.id++;
            assertThat(names.put(id, new Pair(parentCallPathId, metricId)), nullValue());
            return id;
        }

        @Override
        public IName getName(long persistentNameId) {
            return null;
        }
    }

    public static class TestPeriodName implements IPeriodName {
        private final long id;
        private final IName name;

        public TestPeriodName(long id, IName name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public long getId() {
            return id;
        }

        @Override
        public <T extends IName> T getName() {
            return (T) name;
        }

        @Override
        public boolean isStale() {
            return false;
        }

        @Override
        public void refresh() {
        }
    }

    public static class TestPeriodNameManager implements IPeriodNameManager {
        private Map<Long, TestPeriodName> nameByIdMap = new HashMap<Long, TestPeriodName>();
        private Map<IName, TestPeriodName> nameByNameMap = new HashMap<IName, TestPeriodName>();

        public TestPeriodNameManager(List<TestPeriodName> names) {
            for (TestPeriodName name : names) {
                nameByIdMap.put(name.getId(), name);
                nameByNameMap.put(name.getName(), name);
            }
        }

        @Override
        public IPeriodName addName(IName name) {
            return null;
        }

        @Override
        public IPeriodName findById(long id) {
            return nameByIdMap.get(id);
        }

        @Override
        public IPeriodName findByName(IName name) {
            return nameByNameMap.get(name);
        }
    }

    public static class TestNavigationAccessorFactory implements INavigationAccessorFactory {

        @Override
        public Set<String> getTypes() {
            return Collections.asSet("test");
        }

        @Override
        public IComponentAccessor createAccessor(String navigationType, String navigationArgs, IComponentAccessor localAccessor) {
            return new TestNavigationAccessor(localAccessor);
        }
    }

    private static class TestNavigationAccessor implements IComponentAccessor {
        private final IComponentAccessor localAccessor;

        public TestNavigationAccessor(IComponentAccessor localAccessor) {
            this.localAccessor = localAccessor;
        }

        @Override
        public Object get(IComponentValue value, IComputeContext context) {
            return localAccessor.get(((TestNode) context.getObject()).value2, context);
        }

        @Override
        public Object get(IComponentValue componentValue, IMetricValue value, IComputeContext context) {
            return get(componentValue, context);
        }

        @Override
        public Object get(IComponentValue componentValue, IMetricValue metricValue, IFieldValue value,
                          IComputeContext context) {
            return get(componentValue, context);
        }
    }

    private static class TestNode {
        private IComponentValue value2;
    }

    private static class TestRepresentationSchemaConfiguration extends ObjectRepresentationSchemaConfiguration {
        public TestRepresentationSchemaConfiguration() {
            super("test");
        }

        @Override
        public IMetricComputer createComputer(ComponentValueSchemaConfiguration schema,
                                              ComponentRepresentationSchemaConfiguration configuration, IComponentAccessorFactory componentAccessorFactory,
                                              int metricIndex) {
            return new TestComputer();
        }
    }

    private static class TestComputer implements IMetricComputer {
        @Override
        public Object compute(IComponentValue v, IMetricValue rootValue, IComputeContext context) {
            ObjectValue objectValue = (ObjectValue) rootValue;
            switch (((Long) objectValue.getObject()).intValue()) {
                case 0:
                    return "first";
                case 1:
                    return "second";
                case 2:
                    return "third";
                default:
                    return "other";
            }
        }

        @Override
        public void computeSecondary(IComponentValue v, IMetricValue value, IComputeContext context) {
        }
    }
}