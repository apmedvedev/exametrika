/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.exa.server.nodes;

import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.metrics.exa.server.nodes.IExaServerComponentVersion;
import com.exametrika.impl.component.nodes.HealthComponentVersionNode;


/**
 * The {@link ExaServerComponentVersionNode} is a exa server component version node object.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are not thread safe.
 */
public class ExaServerComponentVersionNode extends HealthComponentVersionNode implements IExaServerComponentVersion {
    public ExaServerComponentVersionNode(INode node) {
        super(node);
    }
}