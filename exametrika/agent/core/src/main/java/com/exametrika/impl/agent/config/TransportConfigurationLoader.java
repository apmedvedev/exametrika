/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.agent.config;

import com.exametrika.api.agent.config.TransportConfiguration;
import com.exametrika.common.config.AbstractElementLoader;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Debug;


/**
 * The {@link TransportConfigurationLoader} is a configuration loader for transport configuration.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public abstract class TransportConfigurationLoader extends AbstractElementLoader {
    protected final TransportConfiguration loadTransport(JsonObject element) {
        if (element == null)
            return null;

        boolean debug = Debug.isDebug() ? true : (Boolean) element.get("debug");
        long selectionPeriod = element.get("selectionPeriod");
        long cleanupPeriod = element.get("cleanupPeriod");
        long compressionLevel = element.get("compressionLevel");
        long streamingMaxFragmentSize = element.get("streamingMaxFragmentSize");
        long heartbeatTrackPeriod = element.get("heartbeatTrackPeriod");
        long heartbeatStartPeriod = element.get("heartbeatStartPeriod");
        long heartbeatPeriod = element.get("heartbeatPeriod");
        long heartbeatFailureDetectionPeriod = element.get("heartbeatFailureDetectionPeriod");
        long transportChannelTimeout = element.get("transportChannelTimeout");
        long transportMaxChannelIdlePeriod = element.get("transportChannelIdlePeriod");
        long transportMaxUnlockSendQueueCapacity = element.get("transportMaxUnlockSendQueueCapacity");
        long transportMinLockSendQueueCapacity = element.get("transportMinLockSendQueueCapacity");
        long transportMaxPacketSize = element.get("transportMaxPacketSize");
        long transportMinReconnectPeriod = element.get("transportMinReconnectPeriod");

        return new TransportConfiguration(debug, selectionPeriod, cleanupPeriod, (int) compressionLevel, (int) streamingMaxFragmentSize,
                heartbeatTrackPeriod, heartbeatStartPeriod, heartbeatPeriod, heartbeatFailureDetectionPeriod,
                transportChannelTimeout, transportMaxChannelIdlePeriod,
                (int) transportMaxUnlockSendQueueCapacity, (int) transportMinLockSendQueueCapacity, (int) transportMaxPacketSize,
                transportMinReconnectPeriod);
    }
}
