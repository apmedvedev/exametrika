/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator.nodes;


import com.exametrika.api.aggregator.nodes.Dependency;
import com.exametrika.api.aggregator.nodes.IStackNameNode;
import com.exametrika.api.aggregator.nodes.IStackNode;
import com.exametrika.api.exadb.core.IDumpContext;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.fields.IReferenceField;
import com.exametrika.common.json.IJsonHandler;


/**
 * The {@link StackNameNode} is a stack name node.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are not thread safe.
 */
public class StackNameNode extends NameNode implements IStackNameNode {
    protected static final int DEPENDENCIES_FIELD = 6;

    public StackNameNode(INode node) {
        super(node);
    }

    @Override
    public String getNodeType() {
        return isDerived() ? "stackNameDerived" : "stackName";
    }

    @Override
    public Iterable<Dependency<IStackNode>> getDependencies() {
        IReferenceField<IStackNode> dependencies = getField(DEPENDENCIES_FIELD);
        return new DependencyIterable(dependencies);
    }

    @Override
    public void dump(IJsonHandler json, IDumpContext context) {
        super.dump(json, context);

        boolean jsonDependencies = false;
        for (Dependency<IStackNode> dependency : getDependencies()) {
            if (!jsonDependencies) {
                json.key("dependencies");
                json.startArray();
                jsonDependencies = true;
            }

            String dependencyName = dependency.getNode().getSchema().getQualifiedName() + "@" +
                    dependency.getNode().getId() + (dependency.isTotal() ? "[inherent,total]" : "[inherent]");
            json.value(dependencyName);
        }

        if (jsonDependencies)
            json.endArray();
    }
}