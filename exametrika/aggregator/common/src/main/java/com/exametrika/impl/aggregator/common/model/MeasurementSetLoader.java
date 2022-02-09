/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.common.model;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.exametrika.api.aggregator.common.model.ICallPath;
import com.exametrika.api.aggregator.common.model.IMetricLocation;
import com.exametrika.api.aggregator.common.model.IMetricName;
import com.exametrika.api.aggregator.common.model.IName;
import com.exametrika.api.aggregator.common.model.IScopeName;
import com.exametrika.api.aggregator.common.model.Measurement;
import com.exametrika.api.aggregator.common.model.MeasurementId;
import com.exametrika.api.aggregator.common.model.MeasurementSet;
import com.exametrika.api.aggregator.common.model.Measurements;
import com.exametrika.api.aggregator.common.model.NameId;
import com.exametrika.api.aggregator.common.model.Names;
import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.common.config.InvalidConfigurationException;
import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.common.utils.Times;
import com.exametrika.impl.aggregator.common.values.ComponentValue;
import com.exametrika.impl.aggregator.common.values.HistogramValue;
import com.exametrika.impl.aggregator.common.values.InstanceRecord;
import com.exametrika.impl.aggregator.common.values.InstanceValue;
import com.exametrika.impl.aggregator.common.values.NameValue;
import com.exametrika.impl.aggregator.common.values.ObjectValue;
import com.exametrika.impl.aggregator.common.values.StackIdsValue;
import com.exametrika.impl.aggregator.common.values.StackValue;
import com.exametrika.impl.aggregator.common.values.StandardValue;
import com.exametrika.impl.aggregator.common.values.StatisticsValue;
import com.exametrika.spi.aggregator.common.model.INameDictionary;


/**
 * The {@link MeasurementSetLoader} is a loader of measurements.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class MeasurementSetLoader {
    private INameDictionary dictionary;
    private final DateFormat format = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SS");

    public MeasurementSetLoader(INameDictionary dictionary) {
        Assert.notNull(dictionary);

        this.dictionary = dictionary;
    }

    public MeasurementSet load(JsonObject element) {
        Object timeObject = element.get("time", Times.getCurrentTime());
        long time;
        if (timeObject instanceof Long)
            time = (Long) timeObject;
        else {
            try {
                time = format.parse((String) timeObject).getTime();
            } catch (ParseException e) {
                return Exceptions.wrapAndThrow(e);
            }
        }

        long schemaVersion = element.get("schemaVersion");
        boolean derived = element.get("derived", false);
        String domain = element.get("domain", null);
        List<Measurement> measurements = new ArrayList<Measurement>();
        for (Object child : (JsonArray) element.get("measurements"))
            measurements.add(loadMeasurement((JsonObject) child));

        return new MeasurementSet(measurements, domain, (int) schemaVersion, time, derived ? MeasurementSet.DERIVED_FLAG : 0);
    }

    private Measurement loadMeasurement(JsonObject element) {
        long period = element.get("period");
        MeasurementId id = loadId((JsonObject) element.get("id"));
        IComponentValue value = loadValue((JsonObject) element.get("value"));
        List<NameId> names = loadNames((JsonArray) element.get("names", null));
        return new Measurement(id, value, period, names);
    }

    private List<NameId> loadNames(JsonArray element) {
        if (element == null)
            return null;

        List<NameId> names = new ArrayList<NameId>();
        for (Object object : element) {
            String str = (String) object;
            IName name;
            if (str.charAt(0) == ICallPath.SEPARATOR)
                name = Names.getCallPath(str.substring(1));
            else if (str.charAt(0) == '$')
                name = Names.getScope(str.substring(1));
            else if (str.charAt(0) == '%')
                name = Names.getMetric(str.substring(1));
            else
                return Assert.error();

            long id = dictionary.getName(name);
            names.add(new NameId(name, id));
        }

        return names;
    }

    private IComponentValue loadValue(JsonObject element) {
        JsonObject metadata = element.get("metadata", null);
        List<IMetricValue> metrics = new ArrayList<IMetricValue>();
        for (Object child : (JsonArray) element.get("metrics"))
            metrics.add(loadMetric((JsonObject) child));

        return new ComponentValue(metrics, metadata);
    }

    private IMetricValue loadMetric(JsonObject element) {
        if (element == null)
            return null;

        String type = getType(element);
        if (type.equals("name"))
            return new NameValue(loadFields((JsonArray) element.get("fields")));
        else if (type.equals("stack"))
            return new StackValue(loadFields((JsonArray) element.get("inherent")), loadFields((JsonArray) element.get("total")));
        else if (type.equals("object"))
            return new ObjectValue(element.get("object"));
        else if (type.equals("stackIds"))
            return new StackIdsValue(loadIds((JsonArray) element.get("ids", null)));
        else
            throw new InvalidConfigurationException();
    }

    private List<IFieldValue> loadFields(JsonArray element) {
        List<IFieldValue> fields = new ArrayList<IFieldValue>();
        for (Object child : element)
            fields.add(loadField((JsonObject) child));

        return fields;
    }

    private IFieldValue loadField(JsonObject element) {
        String type = getType(element);
        if (type.equals("std")) {
            long count = element.get("count");
            long sum = element.get("sum");
            long min = element.get("min", Long.MAX_VALUE);
            long max = element.get("max", Long.MIN_VALUE);
            return new StandardValue(count, min, max, sum);
        } else if (type.equals("stat")) {
            double sumSquares = element.get("sumSquares");
            return new StatisticsValue(sumSquares);
        } else if (type.equals("histo")) {
            JsonArray binsArray = element.get("bins");
            long[] bins = new long[binsArray.size()];
            for (int i = 0; i < binsArray.size(); i++)
                bins[i] = (Long) binsArray.get(i);
            long minOutOfBounds = element.get("min-oob");
            long maxOutOfBounds = element.get("max-oob");
            return new HistogramValue(bins, minOutOfBounds, maxOutOfBounds);
        } else if (type.equals("instance")) {
            JsonArray recordsArray = element.get("records");
            List<InstanceRecord> records = new ArrayList<InstanceRecord>();
            for (int i = 0; i < recordsArray.size(); i++)
                records.add(loadRecord((JsonObject) recordsArray.get(i)));
            return new InstanceValue(records);
        } else
            throw new InvalidConfigurationException();
    }

    private InstanceRecord loadRecord(JsonObject element) {
        long value = element.get("value");
        long time = element.get("time", Times.getCurrentTime());
        JsonObject context = element.get("context", null);
        return new InstanceRecord(loadId((JsonObject) element.get("id")), context, value, time);
    }

    private MeasurementId loadId(JsonObject element) {
        String componentType = element.get("componentType");

        if (element.contains("scope")) {
            IScopeName scope = Names.getScope((String) element.get("scope"));

            long scopeId = dictionary.getName(scope);

            IMetricLocation location = Measurements.toLocation((String) element.get("location"));
            long locationId;
            if (location instanceof IMetricName)
                locationId = dictionary.getName(location);
            else
                locationId = dictionary.getName(location);

            return new MeasurementId(scopeId, locationId, componentType);
        } else {
            long scopeId = element.get("scope-id");
            long locationId = element.get("location-id");
            return new MeasurementId(scopeId, locationId, componentType);
        }
    }

    private String getType(JsonObject element) {
        if (element.contains("instanceOf"))
            return element.get("instanceOf");
        else
            throw new InvalidConfigurationException();
    }

    private Set<UUID> loadIds(JsonArray element) {
        if (element == null)
            return null;

        Set<UUID> ids = new LinkedHashSet<UUID>();
        for (Object child : element)
            ids.add(UUID.fromString((String) child));

        return ids;
    }
}