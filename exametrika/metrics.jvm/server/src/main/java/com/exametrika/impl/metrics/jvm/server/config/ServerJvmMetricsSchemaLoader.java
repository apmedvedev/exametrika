/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.jvm.server.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.exametrika.api.aggregator.config.model.MetricRepresentationSchemaConfiguration;
import com.exametrika.api.metrics.jvm.server.config.model.AllHotspotsSelectorSchemaConfiguration;
import com.exametrika.api.metrics.jvm.server.config.model.AllJvmNodesSelectorSchemaConfiguration;
import com.exametrika.api.metrics.jvm.server.config.model.AllTransactionsSelectorSchemaConfiguration;
import com.exametrika.api.metrics.jvm.server.config.model.AppErrorsRepresentationSchemaConfiguration;
import com.exametrika.api.metrics.jvm.server.config.model.AppErrorsSchemaConfiguration;
import com.exametrika.api.metrics.jvm.server.config.model.AppWorkloadRepresentationSchemaConfiguration;
import com.exametrika.api.metrics.jvm.server.config.model.AppWorkloadSchemaConfiguration;
import com.exametrika.api.metrics.jvm.server.config.model.JvmErrorsRepresentationSchemaConfiguration;
import com.exametrika.api.metrics.jvm.server.config.model.JvmErrorsSchemaConfiguration;
import com.exametrika.api.metrics.jvm.server.config.model.JvmNodeSelectorSchemaConfiguration;
import com.exametrika.api.metrics.jvm.server.config.model.JvmWorkloadRepresentationSchemaConfiguration;
import com.exametrika.api.metrics.jvm.server.config.model.JvmWorkloadSchemaConfiguration;
import com.exametrika.api.metrics.jvm.server.config.model.TransactionSelectorSchemaConfiguration;
import com.exametrika.common.config.AbstractExtensionLoader;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.config.InvalidConfigurationException;
import com.exametrika.common.json.JsonObject;


/**
 * The {@link ServerJvmMetricsSchemaLoader} is a loader of perfdb schemas.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ServerJvmMetricsSchemaLoader extends AbstractExtensionLoader {
    @Override
    public Object loadExtension(String name, String type, Object object, ILoadContext context) {
        JsonObject element = (JsonObject) object;
        if (type.equals("JvmWorkloadRepresentation")) {
            JvmWorkloadRepresentationSchemaConfiguration.Type workloadType = JvmWorkloadRepresentationSchemaConfiguration.Type.valueOf(
                    ((String) element.get("type")).toUpperCase().replace(".", "_"));
            double warningThreshold = element.get("warningThreshold");
            double errorThreshold = element.get("errorThreshold");
            return new JvmWorkloadRepresentationSchemaConfiguration(name, workloadType, warningThreshold, errorThreshold);
        } else if (type.equals("JvmErrorsRepresentation")) {
            JvmErrorsRepresentationSchemaConfiguration.Type errorsType = JvmErrorsRepresentationSchemaConfiguration.Type.valueOf(
                    ((String) element.get("type")).toUpperCase().replace(".", "_"));
            double warningThreshold = element.get("warningThreshold");
            double errorThreshold = element.get("errorThreshold");
            return new JvmErrorsRepresentationSchemaConfiguration(name, errorsType, warningThreshold, errorThreshold);
        } else if (type.equals("AppWorkloadRepresentation")) {
            AppWorkloadRepresentationSchemaConfiguration.Type workloadType = AppWorkloadRepresentationSchemaConfiguration.Type.valueOf(
                    ((String) element.get("type")).toUpperCase().replace(".", "_"));
            double warningThreshold = element.get("warningThreshold");
            double errorThreshold = element.get("errorThreshold");
            return new AppWorkloadRepresentationSchemaConfiguration(name, workloadType, warningThreshold, errorThreshold);
        } else if (type.equals("AppErrorsRepresentation")) {
            AppErrorsRepresentationSchemaConfiguration.Type errorsType = AppErrorsRepresentationSchemaConfiguration.Type.valueOf(
                    ((String) element.get("type")).toUpperCase().replace(".", "_"));
            double warningThreshold = element.get("warningThreshold");
            double errorThreshold = element.get("errorThreshold");
            return new AppErrorsRepresentationSchemaConfiguration(name, errorsType, warningThreshold, errorThreshold);
        } else if (type.equals("JvmWorkloadMetric")) {
            String baseRepresentation = element.get("baseRepresentation");
            List<JvmWorkloadRepresentationSchemaConfiguration> representations = (List) loadMetricRepresentations("JvmWorkloadRepresentation",
                    (JsonObject) element.get("representations"), context);
            return new JvmWorkloadSchemaConfiguration(name, baseRepresentation, representations);
        } else if (type.equals("JvmErrorsMetric")) {
            String baseRepresentation = element.get("baseRepresentation");
            List<JvmErrorsRepresentationSchemaConfiguration> representations = (List) loadMetricRepresentations("JvmErrorsRepresentation",
                    (JsonObject) element.get("representations"), context);
            return new JvmErrorsSchemaConfiguration(name, baseRepresentation, representations);
        } else if (type.equals("AppWorkloadMetric")) {
            String baseRepresentation = element.get("baseRepresentation");
            List<AppWorkloadRepresentationSchemaConfiguration> representations = (List) loadMetricRepresentations("AppWorkloadRepresentation",
                    (JsonObject) element.get("representations"), context);
            return new AppWorkloadSchemaConfiguration(name, baseRepresentation, representations);
        } else if (type.equals("AppErrorsMetric")) {
            String baseRepresentation = element.get("baseRepresentation");
            List<AppErrorsRepresentationSchemaConfiguration> representations = (List) loadMetricRepresentations("AppErrorsRepresentation",
                    (JsonObject) element.get("representations"), context);
            return new AppErrorsSchemaConfiguration(name, baseRepresentation, representations);
        } else if (type.equals("AllJvmNodesSelector"))
            return new AllJvmNodesSelectorSchemaConfiguration(name);
        else if (type.equals("JvmNodeSelector"))
            return new JvmNodeSelectorSchemaConfiguration(name);
        else if (type.equals("AllTransactionsSelector"))
            return new AllTransactionsSelectorSchemaConfiguration(name);
        else if (type.equals("TransactionSelector"))
            return new TransactionSelectorSchemaConfiguration(name);
        else if (type.equals("AllHotspotsSelector"))
            return new AllHotspotsSelectorSchemaConfiguration(name);
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