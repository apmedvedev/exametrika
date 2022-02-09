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
import com.exametrika.api.metrics.host.server.config.model.HostErrorsRepresentationSchemaConfiguration;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.aggregator.common.values.ObjectBuilder;
import com.exametrika.impl.aggregator.values.ComputeContext;
import com.exametrika.impl.aggregator.values.ObjectComputer;
import com.exametrika.spi.aggregator.IComponentAccessor;
import com.exametrika.spi.aggregator.IComponentAccessorFactory;
import com.exametrika.spi.aggregator.IComputeContext;
import com.exametrika.spi.exadb.objectdb.fields.IField;


/**
 * The {@link HostErrorsComputer} is an host errors computer.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class HostErrorsComputer extends ObjectComputer {
    private final HostErrorsRepresentationSchemaConfiguration configuration;
    private IComponentAccessor firstAccessor;
    private IComponentAccessor secondAccessor;
    private String componentRepresentationName;
    private final int metricIndex;

    public HostErrorsComputer(HostErrorsRepresentationSchemaConfiguration configuration, IComponentAccessorFactory componentAccessorFactory,
                              String metricName, String componentRepresentationName, int metricIndex) {
        this.configuration = configuration;
        this.componentRepresentationName = componentRepresentationName;
        this.metricIndex = metricIndex;
        switch (configuration.getType()) {
            case HOST_SWAP_ERRORS:
                firstAccessor = componentAccessorFactory.createAccessor(null, null, "host.swap.pagesIn.rate");
                break;
            case HOST_NET_ERRORS:
            case HOST_TCP_ERRORS:
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
        double errors = 0;
        if (!node.isDerived()) {
            switch (configuration.getType()) {
                case HOST_SWAP_ERRORS: {
                    Number pagesIn = (Number) firstAccessor.get(componentValue, context);
                    if (pagesIn != null)
                        errors = pagesIn.doubleValue();
                    break;
                }
                case HOST_NET_ERRORS:
                case HOST_TCP_ERRORS: {
                    IAggregationNodeSchema schema = null;
                    switch (configuration.getType()) {
                        case HOST_NET_ERRORS: {
                            schema = node.getSchema().getParent().findAggregationNode("host.net");
                            if (firstAccessor == null) {
                                IComponentRepresentationSchema componentSchema = schema.getAggregationField().findRepresentation(componentRepresentationName);
                                IComponentAccessorFactory accessorFactory = componentSchema.getAccessorFactory();
                                firstAccessor = accessorFactory.createAccessor(null, null, "host.net.rx.errors.rate");
                                secondAccessor = accessorFactory.createAccessor(null, null, "host.net.tx.errors.rate");
                            }
                            break;
                        }
                        case HOST_TCP_ERRORS: {
                            schema = node.getSchema().getParent().findAggregationNode("host.tcp");
                            if (firstAccessor == null) {
                                IComponentRepresentationSchema componentSchema = schema.getAggregationField().findRepresentation(componentRepresentationName);
                                IComponentAccessorFactory accessorFactory = componentSchema.getAccessorFactory();
                                firstAccessor = accessorFactory.createAccessor(null, null, "host.tcp.inErrors.rate");
                                secondAccessor = accessorFactory.createAccessor(null, null, "host.tcp.outErrors.rate");
                            }
                            break;
                        }
                        default:
                            schema = Assert.error();
                    }
                    INameNode baseNode = node.getPeriod().findNode(node.getLocation(), schema);
                    if (baseNode != null) {
                        for (INameNode child : baseNode.getScopeChildren()) {
                            IComponentValue baseValue = child.getAggregationField().getValue(false);
                            Number inErrors = (Number) firstAccessor.get(baseValue, context);
                            Number outErrors = (Number) secondAccessor.get(baseValue, context);
                            if (inErrors != null)
                                errors += inErrors.doubleValue();
                            if (outErrors != null)
                                errors += outErrors.doubleValue();
                        }
                    }
                    break;
                }
                default:
                    Assert.error();
            }
        } else {
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
                errors = sum / count;
        }

        String state;
        if (errors < configuration.getWarningThreshold())
            state = "normal";
        else if (errors < configuration.getErrorThreshold())
            state = "warning";
        else
            state = "error";

        JsonObjectBuilder fields = new JsonObjectBuilder();
        fields.put("value", errors);
        fields.put("state", state);

        ObjectBuilder builder = (ObjectBuilder) value;
        builder.setObject(fields.toJson());
    }
}
