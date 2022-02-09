/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.exa.probes;


import java.util.ArrayList;

import com.exametrika.api.aggregator.common.model.NameMeasurementId;
import com.exametrika.api.metrics.exa.config.ExaMessagingProbeConfiguration;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Collections;
import com.exametrika.common.utils.SlotAllocator;
import com.exametrika.common.utils.SlotAllocator.Slot;
import com.exametrika.impl.aggregator.common.model.MetricName;
import com.exametrika.impl.metrics.exa.probes.ExaMessagingProbe.CollectorInfo;
import com.exametrika.impl.profiler.probes.BaseProbeCollector;
import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor.Container;
import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor.ThreadLocalSlot;
import com.exametrika.spi.aggregator.common.fields.IInstanceContextProvider;
import com.exametrika.spi.aggregator.common.meters.ICounter;
import com.exametrika.spi.aggregator.common.meters.ILog;
import com.exametrika.spi.aggregator.common.meters.LogEvent;
import com.exametrika.spi.aggregator.common.meters.MeterContainer;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.IScope;
import com.exametrika.spi.profiler.IThreadLocalSlot;


/**
 * The {@link ExaMessagingProbeCollector} is an Exa messaging probe collector.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class ExaMessagingProbeCollector extends BaseProbeCollector {
    private static final IMessages messages = Messages.get(IMessages.class);
    private final ExaMessagingProbeConfiguration configuration;
    private final SlotAllocator slotAllocator;
    private final ThreadLocalSlot slot;
    private ArrayList<ExaMessagingMeterContainer> meterContainers = new ArrayList<ExaMessagingMeterContainer>();
    private int refreshIndex = -1;

    public ExaMessagingProbeCollector(ExaMessagingProbeConfiguration configuration, IProbeContext context, IScope scope,
                                      IThreadLocalSlot slot, Container container, SlotAllocator slotAllocator) {
        super(configuration, context, scope, container, null, false, configuration.getComponentType());

        Assert.notNull(slotAllocator);
        Assert.notNull(slot);
        Assert.notNull(container);

        this.slotAllocator = slotAllocator;
        this.slot = (ThreadLocalSlot) slot;
        this.configuration = configuration;
    }

    @Override
    public void begin() {
        super.begin();

        CollectorInfo info = slot.get(false);
        info.collector = this;
    }

    @Override
    public void end() {
        CollectorInfo info = slot.get(false);
        info.collector = null;

        super.end();
    }

    public void measureSend(int id, int messageSize) {
        updateMetersContainers(id >= meterContainers.size());

        ExaMessagingMeterContainer meters = getMeters(id);
        if (meters != null)
            meters.measureSend(messageSize);

        extract();
    }

    public void measureReceive(int id, int messageSize) {
        updateMetersContainers(id >= meterContainers.size());

        ExaMessagingMeterContainer meters = getMeters(id);
        if (meters != null)
            meters.measureReceive(messageSize);

        extract();
    }

    public void measureFailure(int id, String nodeName) {
        updateMetersContainers(id >= meterContainers.size());

        ExaMessagingMeterContainer meters = getMeters(id);
        if (meters != null)
            meters.measureFailure(nodeName);

        extract();
    }

    @Override
    protected void createMeters() {
    }

    @Override
    protected void updateMetersContainers(boolean force) {
        if (!force && refreshIndex == slotAllocator.getRefreshIndex())
            return;

        refreshIndex = slotAllocator.getRefreshIndex();

        for (int i = 0; i < slotAllocator.getSlotCount(); i++) {
            Slot slot = slotAllocator.getSlot(i);
            if (slot != null) {
                if (i >= meterContainers.size())
                    Collections.set(meterContainers, i, null);

                if (meterContainers.get(i) == null) {
                    ExaMessagingMeterContainer meterContainer = new ExaMessagingMeterContainer(
                            getMeasurementId(null, MetricName.get(slot.name), slot.componentType),
                            context, container.contextProvider, slot.metadata);
                    addMeters(meterContainer);
                    meterContainers.set(i, meterContainer);
                }
            } else if (i < meterContainers.size() && meterContainers.get(i) != null) {
                meterContainers.get(i).delete();
                meterContainers.set(i, null);
            }
        }
    }

    private ExaMessagingMeterContainer getMeters(int id) {
        if (id < meterContainers.size())
            return meterContainers.get(id);
        else
            return null;
    }

    private class ExaMessagingMeterContainer extends MeterContainer {
        private ICounter sendBytes;
        private ICounter receiveBytes;
        private ILog errors;

        public ExaMessagingMeterContainer(NameMeasurementId id, IProbeContext context,
                                          IInstanceContextProvider contextProvider, JsonObject metadata) {
            super(id, context, contextProvider);

            createMeters();
            setMetadata(Json.object(metadata)
                    .put("node", context.getConfiguration().getNodeName())
                    .toObject());
        }

        public void measureSend(int messageSize) {
            sendBytes.measureDelta(messageSize);
        }

        public void measureReceive(int messageSize) {
            receiveBytes.measureDelta(messageSize);
        }

        public void measureFailure(String nodeName) {
            long time = ((IProbeContext) context).getTimeService().getCurrentTime();
            errors.measure(new LogEvent(errors.getId(), "error", time, messages.nodeFailed(nodeName).toString(), null,
                    Json.object().put("failedNode", nodeName).toObjectBuilder(), true));
        }

        protected void createMeters() {
            sendBytes = addMeter("exa.messaging.send.bytes", configuration.getSendBytes(), null);
            receiveBytes = addMeter("exa.messaging.receive.bytes", configuration.getReceiveBytes(), null);
            errors = addLog("exa.messaging.errors.log", configuration.getErrors());
        }
    }

    private interface IMessages {
        @DefaultMessage("Node ''{0}'' has failed.")
        ILocalizedMessage nodeFailed(String nodeName);
    }
}
