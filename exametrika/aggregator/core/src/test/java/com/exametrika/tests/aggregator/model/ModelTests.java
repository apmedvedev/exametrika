/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.aggregator.model;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import com.exametrika.api.aggregator.common.model.IMetricLocation;
import com.exametrika.api.aggregator.common.model.IMetricName;
import com.exametrika.api.aggregator.common.model.IName;
import com.exametrika.api.aggregator.common.model.Measurement;
import com.exametrika.api.aggregator.common.model.MeasurementId;
import com.exametrika.api.aggregator.common.model.MeasurementSet;
import com.exametrika.api.aggregator.common.model.Measurements;
import com.exametrika.api.aggregator.common.model.NameId;
import com.exametrika.api.aggregator.common.model.NameMeasurementId;
import com.exametrika.api.aggregator.common.model.Names;
import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.common.io.impl.ByteInputStream;
import com.exametrika.common.io.impl.ByteOutputStream;
import com.exametrika.common.io.impl.DataDeserialization;
import com.exametrika.common.io.impl.DataSerialization;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.tests.Expected;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.MapBuilder;
import com.exametrika.common.utils.Pair;
import com.exametrika.impl.aggregator.common.model.CallPath;
import com.exametrika.impl.aggregator.common.model.DeserializeNameDictionary;
import com.exametrika.impl.aggregator.common.model.MeasurementSerializers;
import com.exametrika.impl.aggregator.common.model.MetricName;
import com.exametrika.impl.aggregator.common.model.ScopeName;
import com.exametrika.impl.aggregator.common.model.SerializeNameDictionary;
import com.exametrika.impl.aggregator.common.model.SerializeNameDictionary.SerializeNameId;
import com.exametrika.impl.aggregator.common.values.ComponentSerializer;
import com.exametrika.impl.aggregator.common.values.ComponentValue;
import com.exametrika.impl.aggregator.common.values.HistogramSerializer;
import com.exametrika.impl.aggregator.common.values.HistogramValue;
import com.exametrika.impl.aggregator.common.values.InstanceRecord;
import com.exametrika.impl.aggregator.common.values.InstanceSerializer;
import com.exametrika.impl.aggregator.common.values.InstanceValue;
import com.exametrika.impl.aggregator.common.values.NameSerializer;
import com.exametrika.impl.aggregator.common.values.NameValue;
import com.exametrika.impl.aggregator.common.values.ObjectSerializer;
import com.exametrika.impl.aggregator.common.values.ObjectValue;
import com.exametrika.impl.aggregator.common.values.StackSerializer;
import com.exametrika.impl.aggregator.common.values.StackValue;
import com.exametrika.impl.aggregator.common.values.StandardSerializer;
import com.exametrika.impl.aggregator.common.values.StandardValue;
import com.exametrika.impl.aggregator.common.values.StatisticsSerializer;
import com.exametrika.impl.aggregator.common.values.StatisticsValue;
import com.exametrika.spi.aggregator.common.model.INameDictionary;
import com.exametrika.spi.aggregator.common.values.IComponentValueBuilder;
import com.exametrika.spi.aggregator.common.values.IComponentValueSerializer;
import com.exametrika.spi.aggregator.common.values.IMetricValueSerializer;


/**
 * The {@link ModelTests} are tests for model classes.
 *
 * @author Medvedev-A
 */
public class ModelTests {
    @Test
    public void testEscape() throws Throwable {
        assertThat(Names.escape("aa.bb.cc"), is("aa..bb..cc"));
        assertThat(Names.escape(".aa.bb.cc."), is("..aa..bb..cc.."));
        assertThat(Names.escape("."), is(".."));

        assertThat(Names.unescape("aa..bb..cc"), is("aa.bb.cc"));
        assertThat(Names.unescape("..aa..bb..cc.."), is(".aa.bb.cc."));
        assertThat(Names.unescape(".."), is("."));
    }

    @Test
    public void testMetricName() throws Throwable {
        MetricName name = MetricName.root();
        assertThat(name.getSegments().isEmpty(), is(true));
        assertThat(name.isEmpty(), is(true));
        assertThat(name, is(MetricName.root()));
        assertThat(MetricName.get(name.toString()), is(name));

        name = MetricName.get("first.second");
        assertThat(MetricName.get(name.toString()), is(name));
        assertThat(name.getSegments(), is(Arrays.asList("first", "second")));
        assertThat(name.isEmpty(), is(false));
        assertThat(name.getLastSegment(), is("second"));
        assertThat(name.startsWith(MetricName.get("first")), is(true));
        assertThat(name.startsWith(MetricName.get("first.second")), is(true));
        assertThat(name.startsWith(MetricName.get("first.second.third")), is(false));
        assertThat(name.startsWith(MetricName.get("first.seco")), is(false));

        name = MetricName.get("first.second.third");
        assertThat(MetricName.get(name.toString()), is(name));
        assertThat(MetricName.get(Arrays.asList("first.second", "third")), is(MetricName.get("first..second.third")));
        assertThat(MetricName.get(Arrays.asList("second.", "third")), is(MetricName.get("second...third")));
        assertThat(MetricName.get("first..second.third").toString(), is("first..second.third"));

        new Expected(IllegalArgumentException.class, new Runnable() {
            @Override
            public void run() {
                MetricName.get(Arrays.asList("", "third"));
            }
        });

        MetricName name2 = MetricName.get("aa.bb.cc");
        assertThat(MetricName.get("aa.bb.cc") == name2, is(true));

        MetricName name3 = MetricName.get("aa.bb.cc");
        assertThat(MetricName.get("aa.bb.cc") == name3, is(true));
        assertThat(name3, is(name2));
        assertThat(name3 == name2, is(true));

        assertThat(MetricName.get("aa.bb.cc1").toString(), is("aa.bb.cc1"));

        long id = name2.getId();
        name = null;
        name2 = null;
        name3 = null;
        System.gc();
        Thread.sleep(200);
        System.gc();
        assertThat(MetricName.get("aa.bb.cc").getId() > id, is(true));

        MetricName.reset();
        assertThat(MetricName.get("aa.bb.cc").getId(), is(1l));
    }

    @Test
    public void testScopeName() throws Throwable {
        ScopeName name = ScopeName.root();
        assertThat(name.getSegments().isEmpty(), is(true));
        assertThat(name.isEmpty(), is(true));
        assertThat(name, is(ScopeName.root()));
        assertThat(ScopeName.get(name.toString()), is(name));

        name = ScopeName.get("first.second");
        assertThat(ScopeName.get(name.toString()), is(name));
        assertThat(name.getSegments(), is(Arrays.asList("first", "second")));
        assertThat(name.isEmpty(), is(false));
        assertThat(name.getLastSegment(), is("second"));
        assertThat(name.startsWith(ScopeName.get("first")), is(true));
        assertThat(name.startsWith(ScopeName.get("first.second")), is(true));
        assertThat(name.startsWith(ScopeName.get("first.second.third")), is(false));
        assertThat(name.startsWith(ScopeName.get("first.seco")), is(false));

        name = ScopeName.get("first.second.third");
        assertThat(ScopeName.get(name.toString()), is(name));

        assertThat(MetricName.get(Arrays.asList("first.second", "third")), is(MetricName.get("first..second.third")));
        assertThat(MetricName.get(Arrays.asList("second.", "third")), is(MetricName.get("second...third")));
        assertThat(MetricName.get("first..second.third").toString(), is("first..second.third"));

        new Expected(IllegalArgumentException.class, new Runnable() {
            @Override
            public void run() {
                ScopeName.get(Arrays.asList("", "third"));
            }
        });

        ScopeName name2 = ScopeName.get("aa.bb.cc");
        assertThat(ScopeName.get("aa.bb.cc") == name2, is(true));

        ScopeName name3 = ScopeName.get("aa.bb.cc");
        assertThat(ScopeName.get("aa.bb.cc") == name3, is(true));
        assertThat(name3, is(name2));
        assertThat(name3 == name2, is(true));

        assertThat(ScopeName.get("aa.bb.cc1").toString(), is("aa.bb.cc1"));

        long id = name2.getId();
        name = null;
        name2 = null;
        name3 = null;
        System.gc();
        Thread.sleep(200);
        System.gc();
        assertThat(ScopeName.get("aa.bb.cc").getId() > id, is(true));

        ScopeName.reset();
        assertThat(ScopeName.get("aa.bb.cc").getId(), is(1l));
    }

    @Test
    public void testCallPath() throws Throwable {
        CallPath callPath = CallPath.root();
        assertThat(callPath.getSegments().isEmpty(), is(true));
        assertThat(callPath.isEmpty(), is(true));
        assertThat(callPath, is(CallPath.root()));

        callPath = CallPath.get(Arrays.asList(MetricName.get("first"), MetricName.get("second")));
        assertThat(CallPath.get(callPath.toString()), is(callPath));
        assertThat(CallPath.get(callPath.getSegments()), is(callPath));
        assertThat(callPath.getSegments(), is(Arrays.<IMetricName>asList(MetricName.get("first"), MetricName.get("second"))));
        assertThat(callPath.isEmpty(), is(false));
        assertThat(callPath.getLastSegment(), is((IMetricName) MetricName.get("second")));

        callPath = CallPath.get(Arrays.asList(MetricName.get("first"), MetricName.get("second"), MetricName.get("third")));
        assertThat(CallPath.get("first⟶second⟶third"), is(callPath));

        new Expected(IllegalArgumentException.class, new Runnable() {
            @Override
            public void run() {
                CallPath.get(Arrays.asList(MetricName.get("first⟶second"), MetricName.get("third")));
            }
        });
        new Expected(IllegalArgumentException.class, new Runnable() {
            @Override
            public void run() {
                CallPath.get(Arrays.asList(MetricName.get(""), MetricName.get("third")));
            }
        });

        CallPath name2 = CallPath.get("aa⟶bb⟶cc");
        assertThat(CallPath.get("aa⟶bb⟶cc") == name2, is(true));

        CallPath name3 = CallPath.get("aa⟶bb⟶cc");
        assertThat(CallPath.get("aa⟶bb⟶cc") == name3, is(true));
        assertThat(name3, is(name2));
        assertThat(name3 == name2, is(true));

        assertThat(CallPath.get("aa⟶bb⟶cc1").toString(), is("aa⟶bb⟶cc1"));

        long id = name2.getId();
        callPath = null;
        name2 = null;
        name3 = null;
        System.gc();
        Thread.sleep(200);
        System.gc();
        assertThat(CallPath.get("aa⟶bb⟶cc").getId() > id, is(true));

        CallPath.reset();
        assertThat(CallPath.get("aa⟶bb⟶cc").getId(), is(3l));
        assertThat(CallPath.get("aa⟶bb⟶cc").getParent() == CallPath.get("aa⟶bb"), is(true));
        assertThat(CallPath.get("aa⟶bb").getId(), is(2l));
        assertThat(CallPath.get("aa⟶bb").getParent() == CallPath.get("aa"), is(true));
        assertThat(CallPath.get("aa").getId(), is(1l));
        assertThat(CallPath.get("aa").getParent() == CallPath.root(), is(true));
        assertThat(CallPath.root().getChild(MetricName.get("aa")) == CallPath.get("aa"), is(true));
    }

    @Test
    public void testMeasurements() throws Throwable {
        assertThat(new NameMeasurementId(ScopeName.get("scope"), MetricName.get("metric"), "dimension"),
                is(new NameMeasurementId(ScopeName.get("scope"), MetricName.get("metric"), "dimension")));
        assertThat(new NameMeasurementId(ScopeName.get("scope"), MetricName.get("metric"), "dimension"),
                not(new NameMeasurementId(ScopeName.get("scope"), MetricName.get("metric"), "dimension2")));

        assertThat(new MeasurementId(1, 2, "dimension"), is(new MeasurementId(1, 2, "dimension")));
        assertThat(new MeasurementId(2, 3, "dimension"), not(new MeasurementId(3, 4, "dimension2")));
    }

    @Test
    public void testMeasurementIdSerializers() {
        ByteOutputStream outputStream = new ByteOutputStream();
        DataSerialization serialization = new DataSerialization(outputStream);

        ScopeName scope = ScopeName.get("scope");
        MetricName metric = MetricName.get("metric");
        CallPath callPath = CallPath.get("metric1⟶metric2");
        NameMeasurementId id1 = new NameMeasurementId(scope, metric, "dimension");
        NameMeasurementId id2 = new NameMeasurementId(ScopeName.get("scope"), callPath, "dimension");
        NameMeasurementId id3 = new NameMeasurementId(ScopeName.get("scope"), MetricName.root(), "dimension");
        NameMeasurementId id4 = new NameMeasurementId(ScopeName.get("scope"), CallPath.root(), "dimension");

        SerializeNameDictionary serializeDictionary = new SerializeNameDictionary();

        MeasurementSerializers.serializeMeasurementId(serialization, id1, serializeDictionary);
        MeasurementSerializers.serializeMeasurementId(serialization, id1, serializeDictionary);
        MeasurementSerializers.serializeMeasurementId(serialization, id2, serializeDictionary);
        MeasurementSerializers.serializeMeasurementId(serialization, id2, serializeDictionary);
        MeasurementSerializers.serializeMeasurementId(serialization, id3, serializeDictionary);
        MeasurementSerializers.serializeMeasurementId(serialization, id4, serializeDictionary);
        MeasurementSerializers.serializeName(serialization, null, serializeDictionary);
        MeasurementSerializers.serializeLocation(serialization, metric);
        MeasurementSerializers.serializeLocation(serialization, callPath);
        MeasurementSerializers.serializeLocation(serialization, null);
        MeasurementSerializers.serializeLocation(serialization, MetricName.root());
        MeasurementSerializers.serializeLocation(serialization, CallPath.root());
        MeasurementSerializers.serializeScopeName(serialization, scope);
        MeasurementSerializers.serializeScopeName(serialization, null);
        MeasurementSerializers.serializeScopeName(serialization, ScopeName.root());
        MeasurementSerializers.serializeMetricName(serialization, metric);
        MeasurementSerializers.serializeMetricName(serialization, null);
        MeasurementSerializers.serializeMetricName(serialization, MetricName.root());
        MeasurementSerializers.serializeCallPath(serialization, callPath);
        MeasurementSerializers.serializeCallPath(serialization, null);
        MeasurementSerializers.serializeCallPath(serialization, CallPath.root());

        ByteInputStream inputStream = new ByteInputStream(outputStream.getBuffer(), 0, outputStream.getLength());
        DataDeserialization deserialization = new DataDeserialization(inputStream);

        TestNameDictionary testDictionary = new TestNameDictionary();
        DeserializeNameDictionary deserializeDictionary = new DeserializeNameDictionary(testDictionary, null);

        MeasurementId id11 = MeasurementSerializers.deserializeMeasurementId(deserialization, deserializeDictionary);
        assertThat(id11.getComponentType(), is("dimension"));
        assertThat(testDictionary.get(id11.getScopeId()) == id1.getScope(), is(true));
        assertThat(testDictionary.get(id11.getLocationId()) == id1.getLocation(), is(true));

        assertThat(MeasurementSerializers.deserializeMeasurementId(deserialization, deserializeDictionary), is(id11));

        MeasurementId id12 = MeasurementSerializers.deserializeMeasurementId(deserialization, deserializeDictionary);
        assertThat(id12.getComponentType(), is("dimension"));
        assertThat(testDictionary.get(id12.getScopeId()) == id2.getScope(), is(true));
        assertThat(testDictionary.get(id12.getLocationId()) == id2.getLocation(), is(true));
        assertThat(MeasurementSerializers.deserializeMeasurementId(deserialization, deserializeDictionary), is(id12));

        MeasurementId id13 = MeasurementSerializers.deserializeMeasurementId(deserialization, deserializeDictionary);
        assertThat(id13.getComponentType(), is("dimension"));
        assertThat(testDictionary.get(id13.getScopeId()) == id3.getScope(), is(true));
        assertThat(testDictionary.names.get(id13.getLocationId()), nullValue());

        MeasurementId id14 = MeasurementSerializers.deserializeMeasurementId(deserialization, deserializeDictionary);
        assertThat(id14.getComponentType(), is("dimension"));
        assertThat(testDictionary.get(id14.getScopeId()) == id4.getScope(), is(true));
        assertThat(id14.getLocationId(), is(0l));

        assertThat(MeasurementSerializers.deserializeName(deserialization, deserializeDictionary), is(0l));
        assertThat(MeasurementSerializers.deserializeLocation(deserialization), is((IMetricLocation) metric));
        assertThat(MeasurementSerializers.deserializeLocation(deserialization), is((IMetricLocation) callPath));
        assertThat(MeasurementSerializers.deserializeLocation(deserialization), nullValue());
        assertThat((MetricName) MeasurementSerializers.deserializeLocation(deserialization), is(MetricName.root()));
        assertThat((CallPath) MeasurementSerializers.deserializeLocation(deserialization), is(CallPath.root()));
        assertThat(MeasurementSerializers.deserializeScopeName(deserialization), is(scope));
        assertThat(MeasurementSerializers.deserializeScopeName(deserialization), nullValue());
        assertThat(MeasurementSerializers.deserializeScopeName(deserialization), is(ScopeName.root()));
        assertThat(MeasurementSerializers.deserializeMetricName(deserialization), is(metric));
        assertThat(MeasurementSerializers.deserializeMetricName(deserialization), nullValue());
        assertThat(MeasurementSerializers.deserializeMetricName(deserialization), is(MetricName.root()));
        assertThat(MeasurementSerializers.deserializeCallPath(deserialization), is(callPath));
        assertThat(MeasurementSerializers.deserializeCallPath(deserialization), nullValue());
        assertThat(MeasurementSerializers.deserializeCallPath(deserialization), is(CallPath.root()));
    }

    @Test
    public void testMeasurementSerializers() {
        ScopeName scope = ScopeName.get("scope");
        MetricName metric = MetricName.get("metric");
        CallPath callPath = CallPath.get("metric1⟶metric2");
        NameMeasurementId id11 = new NameMeasurementId(scope, metric, "componentType");
        MeasurementId id21 = new MeasurementId(1, 5, "componentType");
        NameMeasurementId id12 = new NameMeasurementId(ScopeName.get("scope"), callPath, "componentType");
        MeasurementId id22 = new MeasurementId(1, 6, "componentType");

        List<NameId> names = Arrays.asList(new NameId(ScopeName.root()), new NameId(scope), new NameId(MetricName.root()),
                new NameId(metric), new NameId(CallPath.root()), new NameId(callPath));
        List<NameId> names2 = Arrays.asList(new NameId(0), new NameId(1), new NameId(0),
                new NameId(6), new NameId(0), new NameId(5));

        TestNameDictionary testDictionary = new TestNameDictionary();
        DeserializeNameDictionary deserializeDictionary = new DeserializeNameDictionary(testDictionary, null);

        SerializeNameDictionary serializeDictionary = new SerializeNameDictionary();

        NameSerializer nameValueSerializer = new NameSerializer(false, Arrays.asList(new StandardSerializer(false), new StatisticsSerializer(false),
                new HistogramSerializer(false, 10), new InstanceSerializer(false, true)));
        StackSerializer stackValueSerializer = new StackSerializer(false, Arrays.asList(new StandardSerializer(false), new StatisticsSerializer(false),
                new HistogramSerializer(false, 10), new InstanceSerializer(false, true)));
        ObjectSerializer objectValueSerializer = new ObjectSerializer(false);
        NameSerializer nameBuilderSerializer = new NameSerializer(true, Arrays.asList(new StandardSerializer(true),
                new StatisticsSerializer(true), new HistogramSerializer(true, 10), new InstanceSerializer(true, true)));
        StackSerializer stackBuilderSerializer = new StackSerializer(true, Arrays.asList(new StandardSerializer(true),
                new StatisticsSerializer(true), new HistogramSerializer(true, 10), new InstanceSerializer(true, true)));
        ObjectSerializer objectBuilderSerializer = new ObjectSerializer(true);

        ByteOutputStream outputStream = new ByteOutputStream();
        DataSerialization serialization = new DataSerialization(outputStream);
        serialization.setExtension(SerializeNameDictionary.EXTENTION_ID, serializeDictionary);

        MeasurementId id1 = new MeasurementId(1, 1, "componentType1");
        MeasurementId id2 = new MeasurementId(1, 1, "componentType2");
        MeasurementId id3 = new MeasurementId(1, 1, "componentType3");

        long[] bins = new long[10];
        Arrays.fill(bins, 123);
        Measurement measurement1 = new Measurement(id1, new ComponentValue(Arrays.asList(new NameValue(Arrays.asList(new StandardValue(10, 20, 30, 40),
                new StatisticsValue(100), new HistogramValue(bins, 10, 20), new InstanceValue(
                        Arrays.asList(new InstanceRecord(id12, Json.object().put("key", "value").toObject(), 100, 1000),
                                new InstanceRecord(id11, null, 10, 1000)))))), null), 10, names);
        Measurement measurement2 = new Measurement(id1, new ComponentValue(Arrays.asList(new NameValue(Arrays.asList(new StandardValue(10, 20, 30, 40),
                new StatisticsValue(100), new HistogramValue(bins, 10, 20), new InstanceValue(
                        Arrays.asList(new InstanceRecord(id12, Json.object().put("key", "value").toObject(), 100, 1000),
                                new InstanceRecord(id11, null, 10, 1000)))))), Json.object().put("key", "value").toObject()), 10, null);
        Measurement measurement3 = new Measurement(id2, new ComponentValue(Arrays.asList(new StackValue(Arrays.asList(new StandardValue(10, 20, 30, 40),
                new StatisticsValue(100), new HistogramValue(bins, 10, 20), new InstanceValue(
                        Arrays.asList(new InstanceRecord(id12, Json.object().put("key", "value").toObject(), 100, 1000),
                                new InstanceRecord(id11, null, 10, 1000)))), Arrays.asList(new StandardValue(10, 20, 30, 40),
                new StatisticsValue(100), new HistogramValue(bins, 10, 20), new InstanceValue(
                        Arrays.asList(new InstanceRecord(id12, Json.object().put("key", "value").toObject(), 100, 1000),
                                new InstanceRecord(id11, null, 10, 1000)))))), null), 10, null);
        Measurement measurement4 = new Measurement(id2, new ComponentValue(Arrays.asList(new StackValue(Arrays.asList(new StandardValue(10, 20, 30, 40),
                new StatisticsValue(100), new HistogramValue(bins, 10, 20), new InstanceValue(
                        Arrays.asList(new InstanceRecord(id12, Json.object().put("key", "value").toObject(), 100, 1000),
                                new InstanceRecord(id11, null, 10, 1000)))), Arrays.asList(new StandardValue(10, 20, 30, 40),
                new StatisticsValue(100), new HistogramValue(bins, 10, 20), new InstanceValue(
                        Arrays.asList(new InstanceRecord(id12, Json.object().put("key", "value").toObject(), 100, 1000),
                                new InstanceRecord(id11, null, 10, 1000)))))), Json.object().put("key", "value").toObject()), 10, null);

        Measurement measurement21 = new Measurement(id1, new ComponentValue(Arrays.asList(new NameValue(Arrays.asList(new StandardValue(10, 20, 30, 40),
                new StatisticsValue(100), new HistogramValue(bins, 10, 20), new InstanceValue(
                        Arrays.asList(new InstanceRecord(id21, Json.object().put("key", "value").toObject(), 100, 1000),
                                new InstanceRecord(id22, null, 10, 1000)))))), null), 10, names2);
        Measurement measurement22 = new Measurement(id1, new ComponentValue(Arrays.asList(new NameValue(Arrays.asList(new StandardValue(10, 20, 30, 40),
                new StatisticsValue(100), new HistogramValue(bins, 10, 20), new InstanceValue(
                        Arrays.asList(new InstanceRecord(id21, Json.object().put("key", "value").toObject(), 100, 1000),
                                new InstanceRecord(id22, null, 10, 1000)))))), Json.object().put("key", "value").toObject()), 10, null);
        Measurement measurement23 = new Measurement(id2, new ComponentValue(Arrays.asList(new StackValue(Arrays.asList(new StandardValue(10, 20, 30, 40),
                new StatisticsValue(100), new HistogramValue(bins, 10, 20), new InstanceValue(
                        Arrays.asList(new InstanceRecord(id21, Json.object().put("key", "value").toObject(), 100, 1000),
                                new InstanceRecord(id22, null, 10, 1000)))), Arrays.asList(new StandardValue(10, 20, 30, 40),
                new StatisticsValue(100), new HistogramValue(bins, 10, 20), new InstanceValue(
                        Arrays.asList(new InstanceRecord(id21, Json.object().put("key", "value").toObject(), 100, 1000),
                                new InstanceRecord(id22, null, 10, 1000)))))), null), 10, null);
        Measurement measurement24 = new Measurement(id2, new ComponentValue(Arrays.asList(new StackValue(Arrays.asList(new StandardValue(10, 20, 30, 40),
                new StatisticsValue(100), new HistogramValue(bins, 10, 20), new InstanceValue(
                        Arrays.asList(new InstanceRecord(id21, Json.object().put("key", "value").toObject(), 100, 1000),
                                new InstanceRecord(id22, null, 10, 1000)))), Arrays.asList(new StandardValue(10, 20, 30, 40),
                new StatisticsValue(100), new HistogramValue(bins, 10, 20), new InstanceValue(
                        Arrays.asList(new InstanceRecord(id21, Json.object().put("key", "value").toObject(), 100, 1000),
                                new InstanceRecord(id22, null, 10, 1000)))))), Json.object().put("key", "value").toObject()), 10, null);

        Measurement measurement5 = new Measurement(id3, new ComponentValue(Arrays.asList(
                new ObjectValue(Json.object().put("key", "value").toObject())), null), 10, null);
        Measurement measurement6 = new Measurement(id3, new ComponentValue(Arrays.asList(
                new ObjectValue(Json.object().put("key", "value").toObject())),
                Json.object().put("key", "value").toObject()), 10, null);
        Measurement measurement7 = new Measurement(id3, new ComponentValue(Arrays.asList((IMetricValue) null), null), 10, null);

        MeasurementSet measurements1 = new MeasurementSet(Arrays.asList(measurement1, measurement2, measurement3, measurement4,
                measurement5, measurement6, measurement7), null, 2, 100, 0);
        MeasurementSet measurements2 = new MeasurementSet(Arrays.asList(measurement1, measurement2, measurement3, measurement4,
                measurement5, measurement6), "domain", 2, 100, MeasurementSet.DERIVED_FLAG);

        Map<String, IComponentValueSerializer> valueSerializers = new MapBuilder<String, IComponentValueSerializer>()
                .put("componentType1", new ComponentSerializer(false, Arrays.<IMetricValueSerializer>asList(nameValueSerializer)))
                .put("componentType2", new ComponentSerializer(false, Arrays.<IMetricValueSerializer>asList(stackValueSerializer)))
                .put("componentType3", new ComponentSerializer(false, Arrays.<IMetricValueSerializer>asList(objectValueSerializer)))
                .toMap();

        Map<String, IComponentValueSerializer> builderSerializers = new MapBuilder<String, IComponentValueSerializer>()
                .put("componentType1", new ComponentSerializer(true, Arrays.<IMetricValueSerializer>asList(nameBuilderSerializer)))
                .put("componentType2", new ComponentSerializer(true, Arrays.<IMetricValueSerializer>asList(stackBuilderSerializer)))
                .put("componentType3", new ComponentSerializer(true, Arrays.<IMetricValueSerializer>asList(objectBuilderSerializer)))
                .toMap();

        MeasurementSerializers.serializeMeasurementSet(serialization, measurements1, valueSerializers, serializeDictionary);
        MeasurementSerializers.serializeMeasurementSet(serialization, measurements2, builderSerializers, serializeDictionary);

        ByteInputStream inputStream = new ByteInputStream(outputStream.getBuffer(), 0, outputStream.getLength());
        DataDeserialization deserialization = new DataDeserialization(inputStream);
        deserialization.setExtension(DeserializeNameDictionary.EXTENTION_ID, deserializeDictionary);

        MeasurementSet measurements11 = MeasurementSerializers.deserializeMeasurementSet(deserialization, valueSerializers, deserializeDictionary);
        assertThat(measurements11.getSchemaVersion(), is(2));
        assertThat(measurements11.getTime(), is(100l));
        assertThat(measurements11.isDerived(), is(false));
        assertThat(measurements11.getDomain(), nullValue());
        assertThat(measurements11.getMeasurements().size(), is(7));
        assertThat(measurements11.getMeasurements().get(0), is(measurement21));
        assertThat(measurements11.getMeasurements().get(1), is(measurement22));
        assertThat(measurements11.getMeasurements().get(2), is(measurement23));
        assertThat(measurements11.getMeasurements().get(3), is(measurement24));
        assertThat(measurements11.getMeasurements().get(4), is(measurement5));
        assertThat(measurements11.getMeasurements().get(5), is(measurement6));
        assertThat(measurements11.getMeasurements().get(6), is(measurement7));

        for (int i = 0; i < measurement1.getNames().size(); i++) {
            NameId nameId1 = measurement1.getNames().get(i);
            NameId nameId2 = measurement21.getNames().get(i);

            if (nameId2.getId() != 0)
                assertThat(testDictionary.get(nameId2.getId()), is((Object) nameId1.getName()));
            else
                assertThat(nameId1.getId(), is(0l));
        }

        MeasurementSet measurements12 = MeasurementSerializers.deserializeMeasurementSet(deserialization, builderSerializers, deserializeDictionary);
        assertThat(measurements12.getSchemaVersion(), is(2));
        assertThat(measurements12.getTime(), is(100l));
        assertThat(measurements12.isDerived(), is(true));
        assertThat(measurements12.getDomain(), is("domain"));
        assertThat(measurements12.getMeasurements().size(), is(6));

        Measurement measurement11 = measurements12.getMeasurements().get(0);
        assertThat(measurement11.getPeriod(), is(10l));
        assertThat(((IComponentValueBuilder) measurement11.getValue()).toValue(), is(measurement21.getValue()));
        ((IComponentValueBuilder) measurement11.getValue()).set(measurement1.getValue());
        assertThat(((IComponentValueBuilder) measurement11.getValue()).toValue(), is(measurement1.getValue()));

        Measurement measurement12 = measurements12.getMeasurements().get(1);
        assertThat(((IComponentValueBuilder) measurement12.getValue()).toValue(), is(measurement22.getValue()));
        ((IComponentValueBuilder) measurement12.getValue()).set(measurement2.getValue());
        assertThat(((IComponentValueBuilder) measurement12.getValue()).toValue(), is(measurement2.getValue()));

        Measurement measurement13 = measurements12.getMeasurements().get(2);
        assertThat(((IComponentValueBuilder) measurement13.getValue()).toValue(), is(measurement23.getValue()));
        ((IComponentValueBuilder) measurement13.getValue()).set(measurement3.getValue());
        assertThat(((IComponentValueBuilder) measurement13.getValue()).toValue(), is(measurement3.getValue()));

        Measurement measurement14 = measurements12.getMeasurements().get(3);
        assertThat(((IComponentValueBuilder) measurement14.getValue()).toValue(), is(measurement24.getValue()));
        ((IComponentValueBuilder) measurement14.getValue()).set(measurement4.getValue());
        assertThat(((IComponentValueBuilder) measurement14.getValue()).toValue(), is(measurement4.getValue()));

        Measurement measurement15 = measurements12.getMeasurements().get(4);
        assertThat(((IComponentValueBuilder) measurement15.getValue()).toValue(), is(measurement5.getValue()));
        ((IComponentValueBuilder) measurement15.getValue()).set(measurement5.getValue());
        assertThat(((IComponentValueBuilder) measurement15.getValue()).toValue(), is(measurement5.getValue()));

        Measurement measurement16 = measurements12.getMeasurements().get(5);
        assertThat(((IComponentValueBuilder) measurement16.getValue()).toValue(), is(measurement6.getValue()));
        ((IComponentValueBuilder) measurement16.getValue()).set(measurement6.getValue());
        assertThat(((IComponentValueBuilder) measurement16.getValue()).toValue(), is(measurement6.getValue()));
    }

    @Test
    public void testJsonSerializers() {
        ScopeName scope = ScopeName.get("scope");
        MetricName metric = MetricName.get("metric");
        CallPath callPath = CallPath.get("metric1⟶metric2");

        TestNameDictionary2 testDictionary = new TestNameDictionary2();
        testDictionary.names.put(scope, 1l);
        testDictionary.names.put(metric, 2l);
        testDictionary.names.put(callPath, 3l);

        MeasurementId id1 = new MeasurementId(1, 2, "componentType1");
        MeasurementId id2 = new MeasurementId(1, 3, "componentType2");

        List<NameId> names = Arrays.asList(new NameId(ScopeName.root()), new NameId(scope), new NameId(MetricName.root()),
                new NameId(metric), new NameId(CallPath.root()), new NameId(callPath));

        long[] bins = new long[10];
        Arrays.fill(bins, 123);
        Measurement measurement1 = new Measurement(id1, new ComponentValue(Arrays.asList(new NameValue(Arrays.asList(new StandardValue(10, 20, 30, 40),
                new StatisticsValue(100), new HistogramValue(bins, 10, 20), new InstanceValue(
                        Arrays.asList(new InstanceRecord(id1, Json.object().put("key", "value").toObject(), 100, 1000),
                                new InstanceRecord(id1, null, 10, 1000)))))), null), 10, names);
        Measurement measurement2 = new Measurement(id1, new ComponentValue(Arrays.asList(new NameValue(Arrays.asList(new StandardValue(10, 20, 30, 40),
                new StatisticsValue(100), new HistogramValue(bins, 10, 20), new InstanceValue(
                        Arrays.asList(new InstanceRecord(id1, Json.object().put("key", "value").toObject(), 100, 1000),
                                new InstanceRecord(id1, null, 10, 1000)))))), Json.object().put("key", "value").toObject()), 10, null);
        Measurement measurement3 = new Measurement(id2, new ComponentValue(Arrays.asList(new StackValue(Arrays.asList(new StandardValue(10, 20, 30, 40),
                new StatisticsValue(100), new HistogramValue(bins, 10, 20), new InstanceValue(
                        Arrays.asList(new InstanceRecord(id2, Json.object().put("key", "value").toObject(), 100, 1000),
                                new InstanceRecord(id2, null, 10, 1000)))), Arrays.asList(new StandardValue(10, 20, 30, 40),
                new StatisticsValue(100), new HistogramValue(bins, 10, 20), new InstanceValue(
                        Arrays.asList(new InstanceRecord(id2, Json.object().put("key", "value").toObject(), 100, 1000),
                                new InstanceRecord(id2, null, 10, 1000)))))), null), 10, null);
        Measurement measurement4 = new Measurement(id2, new ComponentValue(Arrays.asList(new StackValue(Arrays.asList(new StandardValue(10, 20, 30, 40),
                new StatisticsValue(100), new HistogramValue(bins, 10, 20), new InstanceValue(
                        Arrays.asList(new InstanceRecord(id2, Json.object().put("key", "value").toObject(), 100, 1000),
                                new InstanceRecord(id2, null, 10, 1000)))), Arrays.asList(new StandardValue(10, 20, 30, 40),
                new StatisticsValue(100), new HistogramValue(bins, 10, 20), new InstanceValue(
                        Arrays.asList(new InstanceRecord(id2, Json.object().put("key", "value").toObject(), 100, 1000),
                                new InstanceRecord(id2, null, 10, 1000)))))), Json.object().put("key", "value").toObject()), 10, null);

        Measurement measurement21 = new Measurement(id1, new ComponentValue(Arrays.asList(new NameValue(Arrays.asList(new StandardValue(10, 20, 30, 40),
                new StatisticsValue(100), new HistogramValue(bins, 10, 20), new InstanceValue(
                        Arrays.asList(new InstanceRecord(id1, Json.object().put("key", "value").toObject(), 100, 1000),
                                new InstanceRecord(id1, null, 10, 1000)))))), null), 10, names);
        Measurement measurement22 = new Measurement(id1, new ComponentValue(Arrays.asList(new NameValue(Arrays.asList(new StandardValue(10, 20, 30, 40),
                new StatisticsValue(100), new HistogramValue(bins, 10, 20), new InstanceValue(
                        Arrays.asList(new InstanceRecord(id1, Json.object().put("key", "value").toObject(), 100, 1000),
                                new InstanceRecord(id1, null, 10, 1000)))))), Json.object().put("key", "value").toObject()), 10, null);
        Measurement measurement23 = new Measurement(id2, new ComponentValue(Arrays.asList(new StackValue(Arrays.asList(new StandardValue(10, 20, 30, 40),
                new StatisticsValue(100), new HistogramValue(bins, 10, 20), new InstanceValue(
                        Arrays.asList(new InstanceRecord(id2, Json.object().put("key", "value").toObject(), 100, 1000),
                                new InstanceRecord(id2, null, 10, 1000)))), Arrays.asList(new StandardValue(10, 20, 30, 40),
                new StatisticsValue(100), new HistogramValue(bins, 10, 20), new InstanceValue(
                        Arrays.asList(new InstanceRecord(id2, Json.object().put("key", "value").toObject(), 100, 1000),
                                new InstanceRecord(id2, null, 10, 1000)))))), null), 10, null);
        Measurement measurement24 = new Measurement(id2, new ComponentValue(Arrays.asList(new StackValue(Arrays.asList(new StandardValue(10, 20, 30, 40),
                new StatisticsValue(100), new HistogramValue(bins, 10, 20), new InstanceValue(
                        Arrays.asList(new InstanceRecord(id2, Json.object().put("key", "value").toObject(), 100, 1000),
                                new InstanceRecord(id2, null, 10, 1000)))), Arrays.asList(new StandardValue(10, 20, 30, 40),
                new StatisticsValue(100), new HistogramValue(bins, 10, 20), new InstanceValue(
                        Arrays.asList(new InstanceRecord(id2, Json.object().put("key", "value").toObject(), 100, 1000),
                                new InstanceRecord(id2, null, 10, 1000)))))), Json.object().put("key", "value").toObject()), 10, null);

        Measurement measurement5 = new Measurement(id1, new ComponentValue(Arrays.asList(
                new ObjectValue(Json.object().put("key", "value").toObject())), null), 10, null);
        Measurement measurement6 = new Measurement(id1, new ComponentValue(Arrays.asList(
                new ObjectValue(Json.object().put("key", "value").toObject())),
                Json.object().put("key", "value").toObject()), 10, null);
        Measurement measurement7 = new Measurement(id1, new ComponentValue(Arrays.asList((IMetricValue) null), null), 10, null);

        MeasurementSet measurements1 = new MeasurementSet(Arrays.asList(measurement1, measurement2, measurement3, measurement4,
                measurement5, measurement6, measurement7), null, 2, 100, 0);
        MeasurementSet measurements2 = new MeasurementSet(Arrays.asList(measurement1, measurement2, measurement3, measurement4,
                measurement5, measurement6, measurement7), "domain", 2, 100, MeasurementSet.DERIVED_FLAG);

        JsonObject json1 = Measurements.toJson(measurements1, true, true);
        JsonObject json2 = Measurements.toJson(measurements2, true, true);

        MeasurementSet measurements11 = Measurements.fromJson(json1, testDictionary);
        assertThat(measurements11.getSchemaVersion(), is(2));
        assertThat(measurements11.getTime(), is(100l));
        assertThat(measurements11.isDerived(), is(false));
        assertThat(measurements11.getDomain(), nullValue());
        assertThat(measurements11.getMeasurements().size(), is(7));
        assertThat(measurements11.getMeasurements().get(0), is(measurement21));
        assertThat(measurements11.getMeasurements().get(0).getPeriod(), is(measurement21.getPeriod()));

        assertThat(measurements11.getMeasurements().get(1), is(measurement22));
        assertThat(measurements11.getMeasurements().get(2), is(measurement23));
        assertThat(measurements11.getMeasurements().get(3), is(measurement24));
        assertThat(measurements11.getMeasurements().get(4), is(measurement5));
        assertThat(measurements11.getMeasurements().get(5), is(measurement6));
        assertThat(measurements11.getMeasurements().get(6), is(measurement7));

        MeasurementSet measurements21 = Measurements.fromJson(json2, testDictionary);
        assertThat(measurements21.getSchemaVersion(), is(2));
        assertThat(measurements21.getTime(), is(100l));
        assertThat(measurements21.isDerived(), is(true));
        assertThat(measurements21.getDomain(), is("domain"));
        assertThat(measurements21.getMeasurements().size(), is(7));
    }

    @Test
    public void testSerializeNameDictionary() throws Throwable {
        SerializeNameDictionary dictionary = new SerializeNameDictionary();

        ScopeName scope = ScopeName.get("scope");
        MetricName metric = MetricName.get("metric");
        CallPath callPath = CallPath.get("metric1⟶metric2");

        assertThat(dictionary.getScopeId(scope), is(-1l));
        assertThat(dictionary.putScope(scope), is(0l));
        assertThat(dictionary.getScopeId(scope), is(0l));

        assertThat(dictionary.getMetricId(metric), is(-1l));
        assertThat(dictionary.putMetric(metric), is(0l));
        assertThat(dictionary.getMetricId(metric), is(0l));

        assertThat(dictionary.getCallPathId(callPath), is(-1l));
        assertThat(dictionary.putCallPath(callPath), is(0l));
        assertThat(dictionary.getCallPathId(callPath), is(0l));

        dictionary.reset();

        assertThat(dictionary.getScopeId(scope), is(-1l));
        assertThat(dictionary.putScope(scope), is(0l));
        assertThat(dictionary.getScopeId(scope), is(0l));

        assertThat(dictionary.getMetricId(metric), is(-1l));
        assertThat(dictionary.putMetric(metric), is(0l));
        assertThat(dictionary.getMetricId(metric), is(0l));

        assertThat(dictionary.getCallPathId(callPath), is(-1l));
        assertThat(dictionary.putCallPath(callPath), is(0l));
        assertThat(dictionary.getCallPathId(callPath), is(0l));

        scope = null;
        System.gc();
        Thread.sleep(200);

        dictionary.getScopeId(ScopeName.root());

        List<SerializeNameId> list = dictionary.takeRemovedNames();
        assertThat(list, is(Arrays.asList(new SerializeNameId(SerializeNameDictionary.SCOPE_NAME, 0))));

        metric = null;
        System.gc();
        Thread.sleep(200);

        dictionary.getMetricId(MetricName.root());

        list = dictionary.takeRemovedNames();
        assertThat(list, is(Arrays.asList(new SerializeNameId(SerializeNameDictionary.METRIC_NAME, 0))));

        callPath = null;
        System.gc();
        Thread.sleep(200);

        dictionary.getCallPathId(CallPath.root());

        list = dictionary.takeRemovedNames();
        assertThat(list, is(Arrays.asList(new SerializeNameId(SerializeNameDictionary.CALLPATH_NAME, 0))));

        assertThat(dictionary.takeRemovedNames().isEmpty(), is(true));
    }

    @Test
    public void testDeserializeNameDictionary() throws Throwable {
        TestNameDictionary nameDictionary = new TestNameDictionary();
        final DeserializeNameDictionary dictionary = new DeserializeNameDictionary(nameDictionary, null);

        ScopeName scope = ScopeName.get("scope");
        MetricName metric = MetricName.get("metric");

        long id = dictionary.putScope(1, scope);
        assertThat(dictionary.putScope(1, scope), is(id));
        assertThat(dictionary.getScopeId(1) == 1, is(true));
        assertThat((ScopeName) nameDictionary.get(id), is(scope));
        dictionary.removeScope(1);
        new Expected(AssertionError.class, new Runnable() {
            @Override
            public void run() {
                dictionary.getScopeId(1);
            }
        });

        id = dictionary.putMetric(1, metric);
        assertThat(dictionary.putMetric(1, metric), is(id));
        assertThat(dictionary.getMetricId(1) == 2, is(true));
        assertThat((MetricName) nameDictionary.get(id), is(metric));
        dictionary.removeMetric(1);
        new Expected(AssertionError.class, new Runnable() {
            @Override
            public void run() {
                dictionary.getMetricId(1);
            }
        });

        id = dictionary.putCallPath(1, 1, 2);
        assertThat(dictionary.putCallPath(1, 1, 2), is(id));
        assertThat(dictionary.getCallPathId(1) == 3, is(true));
        dictionary.removeCallPath(1);
        new Expected(AssertionError.class, new Runnable() {
            @Override
            public void run() {
                dictionary.getCallPathId(1);
            }
        });

        dictionary.putScope(1, scope);
        dictionary.putMetric(1, metric);
        dictionary.putCallPath(1, 1, 2);
        dictionary.reset();

        new Expected(AssertionError.class, new Runnable() {
            @Override
            public void run() {
                dictionary.getScopeId(1);
            }
        });
        new Expected(AssertionError.class, new Runnable() {
            @Override
            public void run() {
                dictionary.getMetricId(1);
            }
        });
        new Expected(AssertionError.class, new Runnable() {
            @Override
            public void run() {
                dictionary.getCallPathId(1);
            }
        });
    }

    private static class TestNameDictionary implements INameDictionary {
        private long id = 1;
        private Map<Long, Object> names = new HashMap<Long, Object>();

        public <T> T get(long id) {
            Object value = names.get(id);
            if (value instanceof Pair) {
                Pair<Long, Long> pair = (Pair<Long, Long>) value;

                CallPath parent = get(pair.getKey());
                MetricName segment = get(pair.getValue());
                return (T) CallPath.get(parent, segment);
            } else
                return (T) value;
        }

        @Override
        public long getName(IName name) {
            Assert.notNull(name);
            if (name.isEmpty())
                return 0;

            long id = this.id++;
            assertThat(names.put(id, name), nullValue());
            return id;
        }

        @Override
        public long getCallPath(long parentCallPathId, long metricId) {
            if (parentCallPathId == 0 && metricId == 0)
                return 0;

            long id = this.id++;
            assertThat(names.put(id, new Pair(parentCallPathId, metricId)), nullValue());
            return id;
        }

        @Override
        public IName getName(long persistentNameId) {
            return null;
        }
    }

    private static class TestNameDictionary2 implements INameDictionary {
        private long id = 1;
        private Map<Object, Long> names = new HashMap<Object, Long>();

        @Override
        public long getName(IName name) {
            Long id = names.get(name);
            if (id != null)
                return id;

            id = this.id++;
            names.put(name, id);
            return id;
        }

        @Override
        public long getCallPath(long parentCallPathId, long metricId) {
            Assert.supports(false);
            return 0;
        }

        @Override
        public IName getName(long persistentNameId) {
            return null;
        }
    }
}
