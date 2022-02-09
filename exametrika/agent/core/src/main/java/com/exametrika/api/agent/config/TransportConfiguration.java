/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.api.agent.config;

import com.exametrika.common.config.Configuration;
import com.exametrika.common.utils.Objects;


/**
 * The {@link TransportConfiguration} is a configuration for low level server channel transport.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class TransportConfiguration extends Configuration {
    public static final String SCHEMA = "com.exametrika.transport-1.0";
    private final boolean debug;
    private final long selectionPeriod;
    private final long cleanupPeriod;
    private final int compressionLevel;
    private final int streamingMaxFragmentSize;
    private final long heartbeatTrackPeriod;
    private final long heartbeatStartPeriod;
    private final long heartbeatPeriod;
    private final long heartbeatFailureDetectionPeriod;
    private final long transportChannelTimeout;
    private final long transportMaxChannelIdlePeriod;
    private final int transportMaxUnlockSendQueueCapacity;
    private final int transportMinLockSendQueueCapacity;
    private final int transportMaxPacketSize;
    private final long transportMinReconnectPeriod;

    public TransportConfiguration(boolean debug, long selectionPeriod, long cleanupPeriod, int compressionLevel,
                                  int streamingMaxFragmentSize, long heartbeatTrackPeriod,
                                  long heartbeatStartPeriod, long heartbeatPeriod, long heartbeatFailureDetectionPeriod,
                                  long transportChannelTimeout, long transportMaxChannelIdlePeriod,
                                  int transportMaxUnlockSendQueueCapacity, int transportMinLockSendQueueCapacity, int transportMaxPacketSize, long transportMinReconnectPeriod) {
        this.debug = debug;
        this.selectionPeriod = selectionPeriod;
        this.cleanupPeriod = cleanupPeriod;
        this.compressionLevel = compressionLevel;
        this.streamingMaxFragmentSize = streamingMaxFragmentSize;
        this.heartbeatTrackPeriod = heartbeatTrackPeriod;
        this.heartbeatStartPeriod = heartbeatStartPeriod;
        this.heartbeatPeriod = heartbeatPeriod;
        this.heartbeatFailureDetectionPeriod = heartbeatFailureDetectionPeriod;
        this.transportChannelTimeout = transportChannelTimeout;
        this.transportMaxChannelIdlePeriod = transportMaxChannelIdlePeriod;
        this.transportMaxUnlockSendQueueCapacity = transportMaxUnlockSendQueueCapacity;
        this.transportMinLockSendQueueCapacity = transportMinLockSendQueueCapacity;
        this.transportMaxPacketSize = transportMaxPacketSize;
        this.transportMinReconnectPeriod = transportMinReconnectPeriod;
    }

    public boolean isDebug() {
        return debug;
    }

    public long getSelectionPeriod() {
        return selectionPeriod;
    }

    public long getCleanupPeriod() {
        return cleanupPeriod;
    }

    public int getCompressionLevel() {
        return compressionLevel;
    }

    public int getStreamingMaxFragmentSize() {
        return streamingMaxFragmentSize;
    }

    public long getHeartbeatTrackPeriod() {
        return heartbeatTrackPeriod;
    }

    public long getHeartbeatStartPeriod() {
        return heartbeatStartPeriod;
    }

    public long getHeartbeatPeriod() {
        return heartbeatPeriod;
    }

    public long getHeartbeatFailureDetectionPeriod() {
        return heartbeatFailureDetectionPeriod;
    }

    public long getTransportChannelTimeout() {
        return transportChannelTimeout;
    }

    public long getTransportMaxChannelIdlePeriod() {
        return transportMaxChannelIdlePeriod;
    }

    public int getTransportMaxUnlockSendQueueCapacity() {
        return transportMaxUnlockSendQueueCapacity;
    }

    public int getTransportMinLockSendQueueCapacity() {
        return transportMinLockSendQueueCapacity;
    }

    public int getTransportMaxPacketSize() {
        return transportMaxPacketSize;
    }

    public long getTransportMinReconnectPeriod() {
        return transportMinReconnectPeriod;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof TransportConfiguration))
            return false;

        TransportConfiguration configuration = (TransportConfiguration) o;
        return debug == configuration.debug && selectionPeriod == configuration.selectionPeriod && cleanupPeriod == configuration.cleanupPeriod &&
                compressionLevel == configuration.compressionLevel && streamingMaxFragmentSize == configuration.streamingMaxFragmentSize &&
                heartbeatTrackPeriod == configuration.heartbeatTrackPeriod &&
                heartbeatStartPeriod == configuration.heartbeatStartPeriod && heartbeatPeriod == configuration.heartbeatPeriod &&
                heartbeatFailureDetectionPeriod == configuration.heartbeatFailureDetectionPeriod &&
                transportChannelTimeout == configuration.transportChannelTimeout &&
                transportMaxChannelIdlePeriod == configuration.transportMaxChannelIdlePeriod &&
                transportMaxUnlockSendQueueCapacity == configuration.transportMaxUnlockSendQueueCapacity &&
                transportMinLockSendQueueCapacity == configuration.transportMinLockSendQueueCapacity &&
                transportMaxPacketSize == configuration.transportMaxPacketSize &&
                transportMinReconnectPeriod == configuration.transportMinReconnectPeriod;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(debug, selectionPeriod, cleanupPeriod, compressionLevel, streamingMaxFragmentSize,
                heartbeatTrackPeriod, heartbeatStartPeriod, heartbeatPeriod, heartbeatFailureDetectionPeriod, transportChannelTimeout,
                transportMaxChannelIdlePeriod, transportMaxUnlockSendQueueCapacity,
                transportMinLockSendQueueCapacity, transportMaxPacketSize, transportMinReconnectPeriod);
    }
}
