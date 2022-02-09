/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.host.server.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.exametrika.api.metrics.host.server.config.model.HostSelectorSchemaConfiguration;

import com.exametrika.api.aggregator.config.model.MetricRepresentationSchemaConfiguration;
import com.exametrika.api.metrics.host.server.config.model.AllHostsSelectorSchemaConfiguration;
import com.exametrika.api.metrics.host.server.config.model.HostErrorsRepresentationSchemaConfiguration;
import com.exametrika.api.metrics.host.server.config.model.HostErrorsSchemaConfiguration;
import com.exametrika.api.metrics.host.server.config.model.HostWorkloadRepresentationSchemaConfiguration;
import com.exametrika.api.metrics.host.server.config.model.HostWorkloadSchemaConfiguration;
import com.exametrika.common.config.AbstractExtensionLoader;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.config.InvalidConfigurationException;
import com.exametrika.common.json.JsonObject;


/**
 * The {@link ServerHostMetricsSchemaLoader} is a loader of perfdb schemas.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ServerHostMetricsSchemaLoader extends AbstractExtensionLoader {
    @Override
    public Object loadExtension(String name, String type, Object object, ILoadContext context) {
        JsonObject element = (JsonObject) object;
        if (type.equals("HostWorkloadRepresentation")) {
            HostWorkloadRepresentationSchemaConfiguration.Type workloadType = HostWorkloadRepresentationSchemaConfiguration.Type.valueOf(
                    ((String) element.get("type")).toUpperCase().replace(".", "_"));
            double warningThreshold = element.get("warningThreshold");
            double errorThreshold = element.get("errorThreshold");
            return new HostWorkloadRepresentationSchemaConfiguration(name, workloadType, warningThreshold, errorThreshold);
        } else if (type.equals("HostErrorsRepresentation")) {
            HostErrorsRepresentationSchemaConfiguration.Type errorsType = HostErrorsRepresentationSchemaConfiguration.Type.valueOf(
                    ((String) element.get("type")).toUpperCase().replace(".", "_"));
            double warningThreshold = element.get("warningThreshold");
            double errorThreshold = element.get("errorThreshold");
            return new HostErrorsRepresentationSchemaConfiguration(name, errorsType, warningThreshold, errorThreshold);
        } else if (type.equals("HostWorkloadMetric")) {
            String baseRepresentation = element.get("baseRepresentation");
            List<HostWorkloadRepresentationSchemaConfiguration> representations = (List) loadMetricRepresentations("HostWorkloadRepresentation",
                    (JsonObject) element.get("representations"), context);
            return new HostWorkloadSchemaConfiguration(name, baseRepresentation, representations);
        } else if (type.equals("HostErrorsMetric")) {
            String baseRepresentation = element.get("baseRepresentation");
            List<HostErrorsRepresentationSchemaConfiguration> representations = (List) loadMetricRepresentations("HostErrorsRepresentation",
                    (JsonObject) element.get("representations"), context);
            return new HostErrorsSchemaConfiguration(name, baseRepresentation, representations);
        } else if (type.equals("AllHostsSelector"))
            return new AllHostsSelectorSchemaConfiguration(name);
        else if (type.equals("HostSelector"))
            return new HostSelectorSchemaConfiguration(name);
        else
            throw new InvalidConfigurationException();
    }

    private List<MetricRepresentationSchemaConfiguration> loadMetricRepresentations(String type, JsonObject object,
                                                                                    ILoadContext context) {
        List<MetricRepresentationSchemaConfiguration> list = new ArrayList<MetricRepresentationSchemaConfiguration>();
        for (Map.Entry<String, Object> entry : object)
            list.add((MetricRepresentationSchemaConfiguration) loadExtension(entry.getKey(), type, entry.getValue(), context));

        return list;
    }
}