/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.host.server.values;

import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.api.aggregator.common.values.IObjectValue;
import com.exametrika.api.aggregator.nodes.INameNode;
import com.exametrika.api.aggregator.schema.IAggregationNodeSchema;
import com.exametrika.api.aggregator.schema.IComponentRepresentationSchema;
import com.exametrika.api.exadb.objectdb.fields.IJsonField;
import com.exametrika.api.metrics.host.server.config.model.HostWorkloadRepresentationSchemaConfiguration;
import com.exametrika.api.metrics.host.server.config.model.HostWorkloadRepresentationSchemaConfiguration.Type;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Numbers;
import com.exametrika.impl.aggregator.common.values.ObjectBuilder;
import com.exametrika.impl.aggregator.values.ComputeContext;
import com.exametrika.impl.aggregator.values.ObjectComputer;
import com.exametrika.spi.aggregator.IComponentAccessor;
import com.exametrika.spi.aggregator.IComponentAccessorFactory;
import com.exametrika.spi.aggregator.IComputeContext;
import com.exametrika.spi.exadb.objectdb.fields.IField;


/**
 * The {@link HostWorkloadComputer} is an host workload computer.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class HostWorkloadComputer extends ObjectComputer {
    private final HostWorkloadRepresentationSchemaConfiguration configuration;
    private IComponentAccessor firstAccessor;
    private IComponentAccessor secondAccessor;
    private String componentRepresentationName;
    private final int metricIndex;

    public HostWorkloadComputer(HostWorkloadRepresentationSchemaConfiguration configuration, IComponentAccessorFactory componentAccessorFactory,
                                String metricName, String componentRepresentationName, int metricIndex) {

        this.configuration = configuration;
        this.componentRepresentationName = componentRepresentationName;
        this.metricIndex = metricIndex;
        switch (configuration.getType()) {
            case HOST_CPU_WORKLOAD:
                firstAccessor = componentAccessorFactory.createAccessor(null, null, "host.cpu.used.std.sum");
                secondAccessor = componentAccessorFactory.createAccessor(null, null, "host.cpu.total.std.sum");
                break;
            case HOST_MEMORY_WORKLOAD:
                firstAccessor = componentAccessorFactory.createAccessor(null, null, "host.memory.used.std.sum");
                secondAccessor = componentAccessorFactory.createAccessor(null, null, "host.memory.total.std.sum");
                break;
            case HOST_SWAP_WORKLOAD:
                firstAccessor = componentAccessorFactory.createAccessor(null, null, "host.swap.used.std.sum");
                secondAccessor = componentAccessorFactory.createAccessor(null, null, "host.swap.total.std.sum");
                break;
            case HOST_DISK_WORKLOAD:
            case HOST_NET_RECEIVE_WORKLOAD:
            case HOST_NET_SEND_WORKLOAD:
                break;
            default:
                Assert.error();
        }
    }

    public Object getValue(IMetricValue value, boolean thresholds) {
        if (!thresholds) {
            IObjectValue metricValue = (IObjectValue) value;
            if (metricValue != null)
                return metricValue.getObject();
            else
                return null;
        } else
            return Json.array().add(configuration.getWarningThreshold()).add(configuration.getErrorThreshold()).toArray();
    }

    @Override
    public void computeSecondary(IComponentValue componentValue, IMetricValue value, IComputeContext context) {
        if (value == null)
            return;
        if (((IObjectValue) value).getObject() instanceof JsonObject)
            return;

        INameNode node = (INameNode) ((IField) context.getObject()).getNode().getObject();
        double workload = 0;
        switch (configuration.getType()) {
            case HOST_MEMORY_WORKLOAD:
            case HOST_SWAP_WORKLOAD:
            case HOST_CPU_WORKLOAD: {
                Number used = (Number) firstAccessor.get(componentValue, context);
                Number total = (Number) secondAccessor.get(componentValue, context);
                if (used != null && total != null && total.longValue() != 0)
                    workload = Numbers.percents(used.doubleValue(), total.doubleValue());
                break;
            }
            case HOST_DISK_WORKLOAD: {
                if (!node.isDerived()) {
                    IAggregationNodeSchema schema = node.getSchema().getParent().findAggregationNode("host.fs");
                    if (firstAccessor == null) {
                        IComponentRepresentationSchema componentSchema = schema.getAggregationField().findRepresentation(componentRepresentationName);
                        IComponentAccessorFactory accessorFactory = componentSchema.getAccessorFactory();
                        firstAccessor = accessorFactory.createAccessor(null, null, "host.disk.used.std.sum");
                        secondAccessor = accessorFactory.createAccessor(null, null, "host.disk.total.std.sum");
                    }
                    INameNode baseNode = node.getPeriod().findNode(node.getLocation(), schema);
                    if (baseNode != null) {
                        double maxWorkload = 0;
                        for (INameNode child : baseNode.getScopeChildren()) {
                            IComponentValue baseValue = child.getAggregationField().getValue(false);
                            Number used = (Number) firstAccessor.get(baseValue, context);
                            Number total = (Number) secondAccessor.get(baseValue, context);
                            if (used != null && total != null && total.longValue() != 0) {
                                double currentWorkload = Numbers.percents(used.doubleValue(), total.doubleValue());
                                if (maxWorkload < currentWorkload)
                                    maxWorkload = currentWorkload;
                            }
                        }
                        workload = maxWorkload;
                    }
                }
                break;
            }
            case HOST_NET_RECEIVE_WORKLOAD:
            case HOST_NET_SEND_WORKLOAD: {
                if (!node.isDerived()) {
                    IAggregationNodeSchema schema = node.getSchema().getParent().findAggregationNode("host.net");
                    if (firstAccessor == null) {
                        IComponentRepresentationSchema componentSchema = schema.getAggregationField().findRepresentation(componentRepresentationName);
                        IComponentAccessorFactory accessorFactory = componentSchema.getAccessorFactory();
                        switch (configuration.getType()) {
                            case HOST_NET_RECEIVE_WORKLOAD:
                                firstAccessor = accessorFactory.createAccessor(null, null, "host.net.received.std.sum");
                                break;
                            case HOST_NET_SEND_WORKLOAD:
                                firstAccessor = accessorFactory.createAccessor(null, null, "host.net.sent.std.sum");
                                break;
                        }
                    }
                    INameNode baseNode = node.getPeriod().findNode(node.getLocation(), schema);
                    if (baseNode != null) {
                        double maxWorkload = 0;
                        for (INameNode child : baseNode.getScopeChildren()) {
                            IComponentValue baseValue = child.getAggregationField().getValue(false);
                            IJsonField metadataField = child.getField(child.getSchema().getAggregationField().getMetadataFieldIndex());
                            JsonObject metadata = metadataField.get();
                            Number received = (Number) firstAccessor.get(baseValue, context);
                            if (received != null && metadata != null) {
                                Long maxSpeed = metadata.get("speed", null);
                                if (maxSpeed != null && maxSpeed != 0) {
                                    double speed = received.doubleValue() * 1000 / context.getPeriod();
                                    double currentWorkload = speed * 100 / maxSpeed;
                                    if (maxWorkload < currentWorkload)
                                        maxWorkload = currentWorkload;
                                }
                            }
                        }
                        workload = maxWorkload;
                    }
                }
                break;
            }
            default:
                Assert.error();
        }

        if (node.isDerived() && (configuration.getType() == Type.HOST_DISK_WORKLOAD ||
                configuration.getType() == Type.HOST_NET_RECEIVE_WORKLOAD ||
                configuration.getType() == Type.HOST_NET_SEND_WORKLOAD)) {
            double sum = 0;
            int count = 0;
            for (INameNode child : node.getScopeChildren()) {
                IComponentValue componentChildValue = child.getAggregationField().getValue(false);
                IObjectValue childValue = (IObjectValue) componentChildValue.getMetrics().get(metricIndex);
                Object oldObject = context.getObject();
                ((ComputeContext) context).setObject(child.getAggregationField());
                computeSecondary(componentChildValue, childValue, context);
                ((ComputeContext) context).setObject(oldObject);

                sum += (Double) ((JsonObject) childValue.getObject()).get("value");
                count++;
            }

            if (count > 0)
                workload = sum / count;
        }

        String state;
        if (workload < configuration.getWarningThreshold())
            state = "normal";
        else if (workload < configuration.getErrorThreshold())
            state = "warning";
        else
            state = "error";

        JsonObjectBuilder fields = new JsonObjectBuilder();
        fields.put("value", workload);
        fields.put("state", state);

        ObjectBuilder builder = (ObjectBuilder) value;
        builder.setObject(fields.toJson());
    }
}
