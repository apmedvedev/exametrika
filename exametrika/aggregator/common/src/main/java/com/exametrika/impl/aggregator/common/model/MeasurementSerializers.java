/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.exametrika.api.aggregator.common.model.ICallPath;
import com.exametrika.api.aggregator.common.model.IMeasurementId;
import com.exametrika.api.aggregator.common.model.IMetricLocation;
import com.exametrika.api.aggregator.common.model.IMetricName;
import com.exametrika.api.aggregator.common.model.IName;
import com.exametrika.api.aggregator.common.model.IScopeName;
import com.exametrika.api.aggregator.common.model.Measurement;
import com.exametrika.api.aggregator.common.model.MeasurementId;
import com.exametrika.api.aggregator.common.model.MeasurementSet;
import com.exametrika.api.aggregator.common.model.NameId;
import com.exametrika.api.aggregator.common.model.NameMeasurementId;
import com.exametrika.api.aggregator.common.model.Names;
import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.common.io.IDataDeserialization;
import com.exametrika.common.io.IDataSerialization;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.aggregator.common.model.IDeserializeNameDictionary;
import com.exametrika.spi.aggregator.common.model.ISerializeNameDictionary;
import com.exametrika.spi.aggregator.common.values.IAggregationSchema;
import com.exametrika.spi.aggregator.common.values.IComponentTypeAggregationSchema;
import com.exametrika.spi.aggregator.common.values.IComponentValueSerializer;


/**
 * The {@link MeasurementSerializers} is an serializer utils for measurements.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class MeasurementSerializers {
    private static final short START_MARKER = 0x1717;
    private static final short END_MARKER = 0x7171;

    public static void serializeMeasurementSet(IDataSerialization serialization, MeasurementSet measurements,
                                               IAggregationSchema schema, ISerializeNameDictionary dictionary) {
        serialization.writeInt(measurements.getSchemaVersion());
        serialization.writeLong(measurements.getTime());
        serialization.writeInt(measurements.getFlags());
        serialization.writeString(measurements.getDomain());
        serialization.writeInt(measurements.getMeasurements().size());

        for (Measurement measurement : measurements.getMeasurements()) {
            IComponentTypeAggregationSchema componentTypeSchema = schema.findComponentType(measurement.getId().getComponentType());
            Assert.notNull(componentTypeSchema, "Component ''{0}'' is not found.", measurement.getId().getComponentType());

            serializeMeasurement(serialization, measurement, componentTypeSchema.getValueSerializer(), dictionary);
        }
    }

    public static void serializeMeasurementSet(IDataSerialization serialization, MeasurementSet measurements,
                                               Map<String, IComponentValueSerializer> serializers, ISerializeNameDictionary dictionary) {
        serialization.writeInt(measurements.getSchemaVersion());
        serialization.writeLong(measurements.getTime());
        serialization.writeInt(measurements.getFlags());
        serialization.writeString(measurements.getDomain());
        serialization.writeInt(measurements.getMeasurements().size());

        for (Measurement measurement : measurements.getMeasurements()) {
            IComponentValueSerializer serializer = serializers.get(measurement.getId().getComponentType());
            Assert.notNull(serializer);

            serializeMeasurement(serialization, measurement, serializer, dictionary);
        }
    }

    public static void serializeMeasurement(IDataSerialization serialization, Measurement measurement,
                                            IComponentValueSerializer serializer, ISerializeNameDictionary dictionary) {
        serialization.writeShort(START_MARKER);
        serializeMeasurementId(serialization, measurement.getId(), dictionary);
        serialization.writeLong(measurement.getPeriod());
        serializer.serialize(serialization, measurement.getValue(), true);
        serializeNames(serialization, measurement.getNames(), dictionary);
        serialization.writeShort(END_MARKER);
    }

    public static MeasurementSet deserializeMeasurementSet(IDataDeserialization deserialization,
                                                           IAggregationSchema schema, IDeserializeNameDictionary dictionary) {
        int schemaVersion = deserialization.readInt();
        long time = deserialization.readLong();
        int flags = deserialization.readInt();
        String domain = deserialization.readString();

        int count = deserialization.readInt();
        List<Measurement> measurements = new ArrayList<Measurement>(count);

        for (int i = 0; i < count; i++)
            measurements.add(deserializeMeasurement(deserialization, schema, dictionary));

        return new MeasurementSet(measurements, domain, schemaVersion, time, flags);
    }

    public static MeasurementSet deserializeMeasurementSet(IDataDeserialization deserialization,
                                                           Map<String, IComponentValueSerializer> serializers, IDeserializeNameDictionary dictionary) {
        int schemaVersion = deserialization.readInt();
        long time = deserialization.readLong();
        int flags = deserialization.readInt();
        String domain = deserialization.readString();

        int count = deserialization.readInt();
        List<Measurement> measurements = new ArrayList<Measurement>(count);

        for (int i = 0; i < count; i++)
            measurements.add(deserializeMeasurement(deserialization, serializers, dictionary));

        return new MeasurementSet(measurements, domain, schemaVersion, time, flags);
    }

    public static Measurement deserializeMeasurement(IDataDeserialization deserialization, IAggregationSchema schema,
                                                     IDeserializeNameDictionary dictionary) {
        Assert.isTrue(deserialization.readShort() == START_MARKER, "Measurement deserialization has failed. Incorrect start marker.");

        IMeasurementId id = deserializeMeasurementId(deserialization, dictionary);
        long period = deserialization.readLong();

        IComponentTypeAggregationSchema componentTypeSchema = schema.findComponentType(id.getComponentType());
        Assert.notNull(componentTypeSchema, "Component type ''{0}'' is not found.", id.getComponentType());

        IComponentValue value = componentTypeSchema.getValueSerializer().deserialize(deserialization, true, null);
        List<NameId> names = deserializeNames(deserialization, dictionary);

        Assert.isTrue(deserialization.readShort() == END_MARKER, "Measurement deserialization of component type ''{0}'' has failed. Incorrect end marker.",
                id.getComponentType());

        return new Measurement(id, value, period, names);
    }

    public static Measurement deserializeMeasurement(IDataDeserialization deserialization, Map<String, IComponentValueSerializer> serializers,
                                                     IDeserializeNameDictionary dictionary) {
        Assert.isTrue(deserialization.readShort() == START_MARKER, "Measurement deserialization has failed. Incorrect start marker.");

        IMeasurementId id = deserializeMeasurementId(deserialization, dictionary);
        long period = deserialization.readLong();


        IComponentValueSerializer serializer = serializers.get(id.getComponentType());
        Assert.notNull(serializer);

        IComponentValue value = serializer.deserialize(deserialization, true, null);
        List<NameId> names = deserializeNames(deserialization, dictionary);
        Assert.isTrue(deserialization.readShort() == END_MARKER, "Measurement deserialization of component type ''{0}'' has failed. Incorrect end marker.",
                id.getComponentType());

        return new Measurement(id, value, period, names);
    }

    public static void serializeMeasurementId(IDataSerialization serialization, IMeasurementId measurementId,
                                              ISerializeNameDictionary dictionary) {
        if (measurementId instanceof NameMeasurementId) {
            NameMeasurementId id = (NameMeasurementId) measurementId;
            serialization.writeBoolean(true);
            serializeScopeName(serialization, id.getScope(), dictionary);
            serializeName(serialization, id.getLocation(), dictionary);
            serialization.writeString(id.getComponentType());
        } else if (dictionary == null || !dictionary.convertIdsToNames()) {
            MeasurementId id = (MeasurementId) measurementId;
            serialization.writeBoolean(false);
            serialization.writeLong(id.getScopeId());
            serialization.writeLong(id.getLocationId());
            serialization.writeString(id.getComponentType());
        } else {
            MeasurementId id = (MeasurementId) measurementId;
            IScopeName scope;
            if (id.getScopeId() != 0)
                scope = (IScopeName) dictionary.getName(id.getScopeId());
            else
                scope = Names.rootScope();

            IMetricLocation location;
            if (id.getLocationId() != 0)
                location = (IMetricLocation) dictionary.getName(id.getLocationId());
            else
                location = Names.rootMetric();

            serialization.writeBoolean(true);
            serializeScopeName(serialization, scope, dictionary);
            serializeName(serialization, location, dictionary);
            serialization.writeString(id.getComponentType());
        }
    }

    public static MeasurementId deserializeMeasurementId(IDataDeserialization deserialization, IDeserializeNameDictionary dictionary) {
        if (deserialization.readBoolean()) {
            long scopeId = deserializeScopeName(deserialization, dictionary);
            long locationId = deserializeName(deserialization, dictionary);
            String metricType = deserialization.readString();
            return new MeasurementId(scopeId, locationId, metricType);
        } else {
            long scopeId = deserialization.readLong();
            long locationId = deserialization.readLong();
            String metricType = deserialization.readString();
            return new MeasurementId(scopeId, locationId, metricType);
        }
    }

    private static void serializeNames(IDataSerialization serialization, List<NameId> names,
                                       ISerializeNameDictionary dictionary) {
        if (names == null) {
            serialization.writeBoolean(false);
            return;
        }

        serialization.writeBoolean(true);
        serialization.writeInt(names.size());
        for (NameId nameId : names)
            serializeName(serialization, nameId.getName(), dictionary);
    }

    private static List<NameId> deserializeNames(IDataDeserialization deserialization, IDeserializeNameDictionary dictionary) {
        if (!deserialization.readBoolean())
            return null;

        int count = deserialization.readInt();
        List<NameId> names = new ArrayList<NameId>(count);
        for (int i = 0; i < count; i++)
            names.add(new NameId(deserializeName(deserialization, dictionary)));

        return names;
    }

    public static void serializeName(IDataSerialization serialization, IName name, ISerializeNameDictionary dictionary) {
        if (name == null)
            serialization.writeByte((byte) 0);
        else if (name instanceof MetricName) {
            serialization.writeByte((byte) 1);
            serializeMetricName(serialization, (MetricName) name, dictionary);
        } else if (name instanceof CallPath) {
            serialization.writeByte((byte) 2);
            serializeCallPath(serialization, (CallPath) name, dictionary);
        } else if (name instanceof ScopeName) {
            serialization.writeByte((byte) 3);
            serializeScopeName(serialization, (ScopeName) name, dictionary);
        } else
            Assert.error();
    }

    public static long deserializeName(IDataDeserialization deserialization, IDeserializeNameDictionary dictionary) {
        switch (deserialization.readByte()) {
            case 0:
                return 0;
            case 1:
                return deserializeMetricName(deserialization, dictionary);
            case 2:
                return deserializeCallPath(deserialization, dictionary);
            case 3:
                return deserializeScopeName(deserialization, dictionary);
            default:
                return Assert.error();
        }
    }

    public static void serializeLocation(IDataSerialization serialization, IMetricLocation location) {
        if (location == null)
            serialization.writeByte((byte) 0);
        else if (location instanceof MetricName) {
            serialization.writeByte((byte) 1);
            serializeMetricName(serialization, (MetricName) location);
        } else if (location instanceof CallPath) {
            serialization.writeByte((byte) 2);
            serializeCallPath(serialization, (CallPath) location);
        } else
            Assert.error();
    }

    public static IMetricLocation deserializeLocation(IDataDeserialization deserialization) {
        switch (deserialization.readByte()) {
            case 0:
                return null;
            case 1:
                return deserializeMetricName(deserialization);
            case 2:
                return deserializeCallPath(deserialization);
            default:
                return Assert.error();
        }
    }

    public static void serializeScopeName(IDataSerialization serialization, IScopeName name, ISerializeNameDictionary dictionary) {
        ScopeName scope = (ScopeName) name;
        long id = dictionary.getScopeId(scope);
        if (id == -1) {
            id = dictionary.putScope(scope);
            serialization.writeBoolean(true);
            serializeScopeName(serialization, scope);
            serialization.writeLong(id);
        } else {
            serialization.writeBoolean(false);
            serialization.writeLong(id);
        }
    }

    public static long deserializeScopeName(IDataDeserialization deserialization, IDeserializeNameDictionary dictionary) {
        if (deserialization.readBoolean()) {
            IScopeName name = deserializeScopeName(deserialization);
            long id = deserialization.readLong();
            return dictionary.putScope(id, name);
        } else {
            long id = deserialization.readLong();
            return dictionary.getScopeId(id);
        }
    }

    public static void serializeScopeName(IDataSerialization serialization, IScopeName name) {
        serialization.writeString(name != null ? name.toString() : null);
    }

    public static final ScopeName deserializeScopeName(IDataDeserialization deserialization) {
        String str = deserialization.readString();
        if (str != null)
            return ScopeName.get(str);
        else
            return null;
    }

    public static void serializeMetricName(IDataSerialization serialization, IMetricName name, ISerializeNameDictionary dictionary) {
        MetricName metric = (MetricName) name;
        long id = dictionary.getMetricId(metric);
        if (id == -1) {
            id = dictionary.putMetric(metric);
            serialization.writeBoolean(true);
            serializeMetricName(serialization, metric);
            serialization.writeLong(id);
        } else {
            serialization.writeBoolean(false);
            serialization.writeLong(id);
        }
    }

    public static long deserializeMetricName(IDataDeserialization deserialization, IDeserializeNameDictionary dictionary) {
        if (deserialization.readBoolean()) {
            IMetricName name = deserializeMetricName(deserialization);
            long id = deserialization.readLong();
            return dictionary.putMetric(id, name);
        } else {
            long id = deserialization.readLong();
            return dictionary.getMetricId(id);
        }
    }

    public static void serializeMetricName(IDataSerialization serialization, IMetricName name) {
        serialization.writeString(name != null ? name.toString() : null);
    }

    public static MetricName deserializeMetricName(IDataDeserialization deserialization) {
        String str = deserialization.readString();
        if (str != null)
            return MetricName.get(str);
        else
            return null;
    }

    public static void serializeCallPath(IDataSerialization serialization, ICallPath name, ISerializeNameDictionary dictionary) {
        CallPath callPath = (CallPath) name;
        if (callPath.getParent() == null) {
            serialization.writeByte((byte) 2);
            return;
        }

        long id = dictionary.getCallPathId(callPath);
        if (id == -1) {
            id = dictionary.putCallPath(callPath);
            serialization.writeByte((byte) 1);
            serializeCallPath(serialization, callPath.getParent(), dictionary);
            serializeMetricName(serialization, callPath.getLastSegment(), dictionary);
            serialization.writeLong(id);
        } else {
            serialization.writeByte((byte) 0);
            serialization.writeLong(id);
        }
    }

    public static long deserializeCallPath(IDataDeserialization deserialization, IDeserializeNameDictionary dictionary) {
        byte b = deserialization.readByte();

        if (b == 2)
            return 0;
        else if (b == 1) {
            long parentCallPathId = deserializeCallPath(deserialization, dictionary);
            long metricId = deserializeMetricName(deserialization, dictionary);
            long id = deserialization.readLong();

            return dictionary.putCallPath(id, parentCallPathId, metricId);
        } else if (b == 0) {
            long id = deserialization.readLong();
            return dictionary.getCallPathId(id);
        } else
            return Assert.error();
    }

    public static void serializeCallPath(IDataSerialization serialization, ICallPath callPath) {
        serialization.writeString(callPath != null ? callPath.toString() : null);
    }

    public static CallPath deserializeCallPath(IDataDeserialization deserialization) {
        String str = deserialization.readString();
        if (str != null)
            return CallPath.get(str);
        else
            return null;
    }

    private MeasurementSerializers() {
    }
}
