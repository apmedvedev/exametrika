/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.values;

import java.util.Set;

import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.aggregator.common.values.IFieldValue;
import com.exametrika.api.aggregator.common.values.IMetricValue;
import com.exametrika.api.aggregator.fields.IAggregationField;
import com.exametrika.api.aggregator.nodes.IAggregationNode;
import com.exametrika.api.aggregator.nodes.IBackgroundRootNode;
import com.exametrika.api.aggregator.nodes.IEntryPointNode;
import com.exametrika.api.aggregator.nodes.INameNode;
import com.exametrika.api.aggregator.nodes.ISecondaryEntryPointNode;
import com.exametrika.api.aggregator.nodes.IStackNode;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Collections;
import com.exametrika.spi.aggregator.IComponentAccessor;
import com.exametrika.spi.aggregator.IComputeContext;
import com.exametrika.spi.aggregator.INavigationAccessorFactory;
import com.exametrika.spi.exadb.objectdb.fields.IField;


/**
 * The {@link HierarchyNavigationAccessorFactory} is an implementation of {@link INavigationAccessorFactory} for aggregation hierarchy.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class HierarchyNavigationAccessorFactory implements INavigationAccessorFactory {
    @Override
    public Set<String> getTypes() {
        return Collections.asSet("scopeParent", "scopeRoot", "metricNameParent", "metricNameRoot", "callPathParent", "callPathRoot", "transactionRoot",
                "transactionSegmentRoot", "parentExitPoint");
    }

    @Override
    public IComponentAccessor createAccessor(String navigationType, String navigationArgs, IComponentAccessor localAccessor) {
        return new HierarchyNavigationAccessor(getType(navigationType), localAccessor);
    }

    private Type getType(String navigationType) {
        if (navigationType.equals("scopeParent"))
            return Type.SCOPE_PARENT;
        else if (navigationType.equals("scopeRoot"))
            return Type.SCOPE_ROOT;
        else if (navigationType.equals("metricNameParent"))
            return Type.METRIC_NAME_PARENT;
        else if (navigationType.equals("metricNameRoot"))
            return Type.METRIC_NAME_ROOT;
        else if (navigationType.equals("callPathParent"))
            return Type.CALLPATH_PARENT;
        else if (navigationType.equals("callPathRoot"))
            return Type.CALLPATH_ROOT;
        else if (navigationType.equals("transactionRoot"))
            return Type.TRANSACTION_ROOT;
        else if (navigationType.equals("transactionSegmentRoot"))
            return Type.TRANSACTION_SEGMENT_ROOT;
        else if (navigationType.equals("parentExitPoint"))
            return Type.PARENT_EXIT_POINT;
        else
            return Assert.error();
    }

    private enum Type {
        SCOPE_PARENT,
        SCOPE_ROOT,
        METRIC_NAME_PARENT,
        METRIC_NAME_ROOT,
        CALLPATH_PARENT,
        CALLPATH_ROOT,
        TRANSACTION_ROOT,
        TRANSACTION_SEGMENT_ROOT,
        PARENT_EXIT_POINT
    }

    private static class HierarchyNavigationAccessor implements IComponentAccessor {
        private final Type type;
        private final IComponentAccessor localAccessor;

        public HierarchyNavigationAccessor(Type type, IComponentAccessor localAccessor) {
            Assert.notNull(localAccessor);

            this.type = type;
            this.localAccessor = localAccessor;
        }

        @Override
        public Object get(IComponentValue value, IComputeContext context) {
            if (!(context.getObject() instanceof IAggregationField))
                return null;

            IAggregationNode node = (IAggregationNode) ((IField) context.getObject()).getNode().getObject();
            IAggregationNode baseNode = null;
            switch (type) {
                case SCOPE_PARENT:
                    if (node instanceof INameNode)
                        baseNode = ((INameNode) node).getScopeParent();
                    else if (node instanceof IBackgroundRootNode)
                        baseNode = ((IBackgroundRootNode) node).getScopeParent();
                    else if (node instanceof IEntryPointNode)
                        baseNode = ((IEntryPointNode) node).getScopeParent();
                    break;
                case SCOPE_ROOT:
                    if (node instanceof INameNode) {
                        baseNode = node;
                        while (((INameNode) baseNode).getScopeParent() != null)
                            baseNode = ((INameNode) baseNode).getScopeParent();
                    } else if (node instanceof IBackgroundRootNode) {
                        baseNode = node;
                        while (((IBackgroundRootNode) baseNode).getScopeParent() != null)
                            baseNode = ((IBackgroundRootNode) baseNode).getScopeParent();
                    } else if (node instanceof IEntryPointNode) {
                        baseNode = node;
                        while (((IEntryPointNode) baseNode).getScopeParent() != null)
                            baseNode = ((IEntryPointNode) baseNode).getScopeParent();
                    }
                    break;
                case METRIC_NAME_PARENT:
                    if (node instanceof INameNode)
                        baseNode = ((INameNode) node).getMetricParent();
                    break;
                case METRIC_NAME_ROOT:
                    if (node instanceof INameNode) {
                        baseNode = node;
                        while (((INameNode) baseNode).getMetricParent() != null)
                            baseNode = ((INameNode) baseNode).getMetricParent();
                    }
                    break;
                case CALLPATH_PARENT:
                    if (node instanceof IStackNode)
                        baseNode = ((IStackNode) node).getParent();
                    break;
                case CALLPATH_ROOT:
                    if (node instanceof IStackNode)
                        baseNode = ((IStackNode) node).getRoot();
                    break;
                case TRANSACTION_ROOT:
                    if (node instanceof IStackNode)
                        baseNode = ((IStackNode) node).getTransactionRoot();
                    break;
                case TRANSACTION_SEGMENT_ROOT:
                    if (node instanceof IStackNode) {
                        baseNode = ((IStackNode) node).getRoot();
                        while (baseNode instanceof ISecondaryEntryPointNode &&
                                ((ISecondaryEntryPointNode) baseNode).isSync() && ((ISecondaryEntryPointNode) baseNode).getParentExitPoint() != null)
                            baseNode = ((ISecondaryEntryPointNode) baseNode).getParentExitPoint().getRoot();
                    }
                    break;
                case PARENT_EXIT_POINT:
                    if (node instanceof ISecondaryEntryPointNode)
                        baseNode = ((ISecondaryEntryPointNode) node).getParentExitPoint();
                    break;
                default:
                    Assert.error();
            }

            if (baseNode == null)
                return null;

            IComponentValue componentValue = baseNode.getAggregationField().getValue(false);
            return localAccessor.get(componentValue, context);
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
}
