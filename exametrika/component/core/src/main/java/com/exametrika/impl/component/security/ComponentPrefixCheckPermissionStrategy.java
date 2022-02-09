/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.security;

import com.exametrika.impl.component.nodes.ComponentNode;
import com.exametrika.impl.component.nodes.IncidentNode;
import com.exametrika.impl.exadb.security.BasePrefixCheckPermissionStrategy;


/**
 * The {@link ComponentPrefixCheckPermissionStrategy} is a component prefix check permission strategy.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public class ComponentPrefixCheckPermissionStrategy extends BasePrefixCheckPermissionStrategy {
    @Override
    protected String getObjectLabel(Object object) {
        if (object instanceof ComponentNode)
            return ((ComponentNode) object).getScope().toString();
        else if (object instanceof IncidentNode)
            return ((IncidentNode) object).getComponent().getScope().toString();
        else
            return null;
    }
}
