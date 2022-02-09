/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.exametrika.api.aggregator.IPeriod;
import com.exametrika.api.aggregator.IPeriodName;
import com.exametrika.api.aggregator.IPeriodNameManager;
import com.exametrika.api.aggregator.common.model.Measurement;
import com.exametrika.api.aggregator.common.model.MeasurementSet;
import com.exametrika.api.aggregator.common.model.Names;
import com.exametrika.api.aggregator.schema.ICycleSchema;
import com.exametrika.api.aggregator.schema.IPeriodSpaceSchema;
import com.exametrika.api.component.config.model.ComponentModelSchemaConfiguration;
import com.exametrika.api.component.nodes.IHealthComponent;
import com.exametrika.api.component.nodes.IHealthComponentVersion.State;
import com.exametrika.api.exadb.objectdb.INodeIndex;
import com.exametrika.api.exadb.objectdb.IObjectSpace;
import com.exametrika.api.exadb.objectdb.schema.IObjectSpaceSchema;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.component.nodes.HealthComponentNode;
import com.exametrika.impl.component.nodes.HealthComponentVersionNode;
import com.exametrika.impl.component.nodes.ComponentRootNode;
import com.exametrika.impl.component.schema.HealthServiceSchema;
import com.exametrika.spi.aggregator.IAggregationService;
import com.exametrika.spi.aggregator.IPeriodClosureListener;
import com.exametrika.spi.component.IAgentFailureDetector;
import com.exametrika.spi.exadb.core.DomainService;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link HealthService} is a health service implementation.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public final class HealthService extends DomainService implements IPeriodClosureListener {
    public static final String NAME = "component.HealthService";
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(HealthService.class);
    private IAggregationService aggregationService;
    private HealthMeterContainer meters;
    private long startPeriodTime;
    private IAgentFailureDetector failureDetector;
    private AvailabilityFailureListener listener;
    private IPeriodNameManager nameManager;

    public void setSchema(ComponentModelSchemaConfiguration componentModel) {
        if (componentModel.getHealth() != null) {
            startPeriodTime = getCurrentPeriod(componentModel.getHealth().getFirstAggregationPeriod()).getStartTime();
            ComponentMeasurementContext measurementContext = new ComponentMeasurementContext(aggregationService, componentModel.getVersion());
            meters = new HealthMeterContainer(componentModel.getHealth(), measurementContext);
        } else
            meters = null;
    }

    @Override
    public void start(IDatabaseContext context) {
        super.start(context);

        aggregationService = context.getTransactionProvider().getTransaction().findDomainService(IAggregationService.NAME);
        aggregationService.addPeriodClosureListener(this);

        ComponentModelSchemaConfiguration componentModel = ((HealthServiceSchema) this.schema).getComponentModel();
        if (componentModel.getHealth() != null) {
            startPeriodTime = getCurrentPeriod(componentModel.getHealth().getFirstAggregationPeriod()).getStartTime();
            ComponentMeasurementContext measurementContext = new ComponentMeasurementContext(aggregationService, componentModel.getVersion());
            meters = new HealthMeterContainer(componentModel.getHealth(), measurementContext);
        } else
            meters = null;

        failureDetector = context.getDatabase().findParameter(IAgentFailureDetector.NAME);
        listener = new AvailabilityFailureListener(this, context);
        failureDetector.addFailureListener(listener);

        nameManager = context.findTransactionExtension(IPeriodNameManager.NAME);
    }

    @Override
    public void stop() {
        aggregationService.removePeriodClosureListener(this);
        failureDetector.removeFailureListener(listener);
    }

    @Override
    public void onBeforePeriodClosed(IPeriod period) {
        ComponentModelSchemaConfiguration componentModel = ((HealthServiceSchema) this.schema).getComponentModel();
        if (componentModel.getHealth() == null)
            return;

        if (!period.getSpace().getSchema().getConfiguration().getName().equals(componentModel.getHealth().getFirstAggregationPeriod()))
            return;

        IObjectSpaceSchema spaceSchema = schema.getParent().findSpace("component");
        IObjectSpace space = spaceSchema.getSpace();
        ComponentRootNode root = space.getRootNode();
        List<Measurement> list = new ArrayList<Measurement>();
        for (IHealthComponent component : root.getHealthComponents()) {
            HealthComponentVersionNode version = (HealthComponentVersionNode) component.getCurrentVersion();
            long startTime = version.getStartStopTime();

            if (startTime < startPeriodTime)
                startTime = startPeriodTime;

            Measurement measurement = meters.measureStateCompleted(component, version.getState(), version.getState(),
                    startTime, period.getEndTime(), false);
            if (measurement != null)
                list.add(measurement);
        }

        startPeriodTime = period.getEndTime();

        if (!list.isEmpty()) {
            MeasurementSet measurements = new MeasurementSet(list, null, componentModel.getVersion(),
                    period.getEndTime(), 0);
            aggregationService.aggregate(measurements);
        }
    }

    public boolean isAgentActive(IHealthComponent component) {
        return failureDetector.isActive(component.getScope().toString());
    }

    public void onComponentCreated(IHealthComponent component, long time) {
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.componentCreated(component.getScope().toString()));
    }

    public void onComponentDeleted(IHealthComponent component, long time) {
        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.componentDeleted(component.getScope().toString()));
    }

    public void onComponentStateCompleted(IHealthComponent component, State oldState, State newState, long startTime, long endTime) {
        if (startTime < startPeriodTime)
            startTime = startPeriodTime;

        if (meters != null) {
            Measurement measurement = meters.measureStateCompleted(component, oldState, newState, startTime, endTime, true);
            if (measurement != null) {
                ComponentModelSchemaConfiguration componentModel = ((HealthServiceSchema) this.schema).getComponentModel();
                MeasurementSet measurements = new MeasurementSet(Collections.singletonList(measurement), null,
                        componentModel.getVersion(), endTime, 0);
                aggregationService.aggregate(measurements);
            }
        }

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.componentStateChanged(component.getScope().toString(), oldState.toString().toLowerCase(),
                    newState.toString().toLowerCase()));
    }

    public void onAgentActivated(String agentId) {
        HealthComponentNode component = findComponent(agentId);

        if (component != null)
            component.setNormalState();
    }

    public void onAgentFailed(String agentId) {
        HealthComponentNode component = findComponent(agentId);
        if (component != null)
            component.setUnavailableState();
    }

    private IPeriod getCurrentPeriod(String periodType) {
        IPeriodSpaceSchema spaceSchema = schema.getParent().getParent().findDomain("aggregation").findSpace("aggregation");
        ICycleSchema cycle = spaceSchema.findCycle(periodType);
        Assert.notNull(cycle);

        return cycle.getCurrentCycle().getSpace().getCurrentPeriod();
    }

    private HealthComponentNode findComponent(String agentId) {
        IObjectSpaceSchema spaceSchema = schema.getParent().findSpace("component");
        IObjectSpace space = spaceSchema.getSpace();

        IPeriodName name = nameManager.findByName(Names.getScope(agentId));
        if (name == null)
            return null;

        INodeIndex<Long, HealthComponentNode> index = space.findIndex("componentIndex");
        return index.find(name.getId());
    }

    private interface IMessages {
        @DefaultMessage("Component ''{0}'' has been created.")
        ILocalizedMessage componentCreated(String component);

        @DefaultMessage("Component ''{0}'' has been deleted.")
        ILocalizedMessage componentDeleted(String component);

        @DefaultMessage("Component ''{0}'' state has been changed from ''{1}'' to ''{2}''.")
        ILocalizedMessage componentStateChanged(String component, String oldState, String newState);
    }
}
