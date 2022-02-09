/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.jvm.server.selectors;

import java.util.Map;

import com.exametrika.api.aggregator.common.values.IComponentValue;
import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.api.component.schema.ISelectorSchema;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.aggregator.IComponentAccessorFactory;
import com.exametrika.spi.aggregator.IComputeContext;


/**
 * The {@link AllHotspotsSelector} is a hotspot selector.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class AllHotspotsSelector extends ApplicationSelector {
    public AllHotspotsSelector(IComponent component, ISelectorSchema schema) {
        super(component, schema, "");
    }

    @Override
    protected boolean isTransaction() {
        return Assert.error();
    }

    @Override
    protected JsonObjectBuilder doBuildKpiMetrics(long time, IComponentValue value, IComponentAccessorFactory accessorFactory,
                                                  IComputeContext computeContext) {
        return Assert.error();
    }

    @Override
    protected Object doSelect(Map<String, ?> parameters) {
        String type = (String) parameters.get("type");

        if (type.equals("allMethods"))
            return selectHotspotMethods(true, true, parameters);
        else if (type.equals("allErrors"))
            return selectHotspotErrors(true, true, parameters);
        else if (type.equals("allFailures"))
            return selectHotspotFailures(true, true, parameters);
        else if (type.equals("allJdbcQueries"))
            return selectHotspotJdbcQueries(true, true, parameters);
        else if (type.equals("allJdbcConnections"))
            return selectHotspotJdbcConnections(true, true, parameters);
        else if (type.equals("allHttpConnections"))
            return selectHotspotHttpConnections(true, true, parameters);
        else if (type.equals("allJmsProducers"))
            return selectHotspotJmsProducers(true, true, parameters);
        else if (type.equals("allTcps"))
            return selectHotspotTcps(true, true, parameters);
        else if (type.equals("allUdps"))
            return selectHotspotUdps(true, true, parameters);
        else if (type.equals("allFiles"))
            return selectHotspotFiles(true, true, parameters);
        else if (type.equals("allBackgroundMethods"))
            return selectHotspotMethods(true, false, parameters);
        else if (type.equals("allBackgroundErrors"))
            return selectHotspotErrors(true, false, parameters);
        else if (type.equals("allBackgroundJdbcQueries"))
            return selectHotspotJdbcQueries(true, false, parameters);
        else if (type.equals("allBackgroundJdbcConnections"))
            return selectHotspotJdbcConnections(true, false, parameters);
        else if (type.equals("allBackgroundHttpConnections"))
            return selectHotspotHttpConnections(true, false, parameters);
        else if (type.equals("allBackgroundJmsProducers"))
            return selectHotspotJmsProducers(true, false, parameters);
        else if (type.equals("allBackgroundTcps"))
            return selectHotspotTcps(true, false, parameters);
        else if (type.equals("allBackgroundUdps"))
            return selectHotspotUdps(true, false, parameters);
        else if (type.equals("allBackgroundFiles"))
            return selectHotspotFiles(true, false, parameters);
        else
            return Assert.error();
    }
}
