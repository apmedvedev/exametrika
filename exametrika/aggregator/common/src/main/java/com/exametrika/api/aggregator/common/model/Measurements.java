/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.aggregator.common.model;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonArrayBuilder;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.aggregator.common.model.CallPath;
import com.exametrika.impl.aggregator.common.model.MeasurementSetLoader;
import com.exametrika.impl.aggregator.common.model.MetricName;
import com.exametrika.spi.aggregator.common.model.INameDictionary;


/**
 * The {@link Measurements} is an measurement utils.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class Measurements {
    public static JsonObject toJson(MeasurementSet measurements, boolean full, boolean convertTime) {
        JsonObjectBuilder builder = new JsonObjectBuilder();
        if (full) {
            if (convertTime) {
                DateFormat format = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SS");
                builder.put("time", format.format(new Date(measurements.getTime())));
            } else
                builder.put("time", measurements.getTime());

            builder.put("schemaVersion", measurements.getSchemaVersion());
            if (measurements.isDerived())
                builder.put("derived", true);
            if (measurements.getDomain() != null)
                builder.put("domain", measurements.getDomain());
        }

        JsonArrayBuilder measurementsBuilder = new JsonArrayBuilder();
        for (Measurement measurement : measurements.getMeasurements())
            measurementsBuilder.add(toJson(measurement, full));

        builder.put("measurements", measurementsBuilder.toJson());

        return builder.toJson();
    }

    public static JsonObject toJson(Measurement measurement, boolean full) {
        return Json.object()
                .put("id", toJson(measurement.getId()))
                .put("value", measurement.getValue().toJson())
                .putIf("period", measurement.getPeriod(), full)
                .putIf("names", buildNames(measurement.getNames()), measurement.getNames() != null)
                .toObject();
    }

    public static JsonObject toJson(Measurement measurement, List<String> metricTypes, JsonObject metadata) {
        return Json.object()
                .put("id", toJson(measurement.getId()))
                .put("value", measurement.getValue().toJson(metricTypes, metadata))
                .putIf("names", buildNames(measurement.getNames()), measurement.getNames() != null)
                .toObject();
    }

    public static JsonObject toJson(IMeasurementId id) {
        JsonObjectBuilder fields = new JsonObjectBuilder();

        if (id instanceof NameMeasurementId) {
            NameMeasurementId measurementId = (NameMeasurementId) id;
            fields.put("scope", measurementId.getScope().toString());
            if (measurementId.getLocation() instanceof IMetricName)
                fields.put("location", measurementId.getLocation().toString());
            else
                fields.put("location", ICallPath.SEPARATOR + measurementId.getLocation().toString());
            fields.put("componentType", measurementId.getComponentType());
        } else {
            MeasurementId measurementId = (MeasurementId) id;
            fields.put("scope-id", measurementId.getScopeId());
            fields.put("location-id", measurementId.getLocationId());
            fields.put("componentType", measurementId.getComponentType());
        }

        return fields.toJson();
    }

    public static MeasurementSet fromJson(JsonObject element, INameDictionary dictionary) {
        MeasurementSetLoader loader = new MeasurementSetLoader(dictionary);
        return loader.load(element);
    }

    public static IMetricLocation toLocation(String metricLocation) {
        if (metricLocation.length() >= 1 && metricLocation.charAt(0) == ICallPath.SEPARATOR)
            return CallPath.get(metricLocation.substring(1));
        else
            return MetricName.get(metricLocation);
    }

    private static JsonArray buildNames(List<NameId> names) {
        if (names == null)
            return null;

        JsonArrayBuilder builder = new JsonArrayBuilder();
        for (NameId nameId : names) {
            IName name = nameId.getName();
            if (name instanceof ICallPath)
                builder.add(ICallPath.SEPARATOR + name.toString());
            else if (name instanceof IScopeName)
                builder.add('$' + name.toString());
            else if (name instanceof IMetricName)
                builder.add('%' + name.toString());
            else
                return Assert.error();
        }

        return builder;
    }

    private Measurements() {
    }
}
