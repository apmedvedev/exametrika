/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.jvm.probes;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

import javax.sql.DataSource;

import com.exametrika.api.aggregator.common.model.ICallPath;
import com.exametrika.api.metrics.jvm.config.JdbcConnectionProbeConfiguration;
import com.exametrika.api.profiler.config.AppStackCounterType;
import com.exametrika.common.json.JsonObject;
import com.exametrika.impl.metrics.jvm.boot.JdbcConnectionProbeInterceptor;
import com.exametrika.impl.profiler.probes.ExitPointProbe;
import com.exametrika.impl.profiler.probes.ExitPointProbeCalibrateInfo;
import com.exametrika.impl.profiler.probes.ExitPointProbeCollector;
import com.exametrika.impl.profiler.probes.StackProbeCollector;
import com.exametrika.impl.profiler.probes.StackProbeRootCollector;
import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor.Container;
import com.exametrika.spi.metrics.jvm.IJdbcConnectionRawRequest;
import com.exametrika.spi.profiler.IProbeContext;


/**
 * The {@link JdbcConnectionProbe} is a JDBC connection probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class JdbcConnectionProbe extends ExitPointProbe {
    private final JdbcConnectionProbeConfiguration configuration;

    public static class JdbcConnectionRawRequest implements IJdbcConnectionRawRequest {
        private final long startTime;
        private final long delta;
        private final Connection connection;
        private final DataSource dataSource;

        public JdbcConnectionRawRequest(long startTime, long delta, Connection connection, DataSource dataSource) {
            this.startTime = startTime;
            this.delta = delta;
            this.connection = connection;
            this.dataSource = dataSource;
        }

        public long getStartTime() {
            return startTime;
        }

        public long getDelta() {
            return delta;
        }

        @Override
        public String getUrl() {
            try {
                if (connection != null && connection.getMetaData() != null)
                    return connection.getMetaData().getURL();
            } catch (SQLException e) {
            }

            return null;
        }

        @Override
        public Connection getConnection() {
            return connection;
        }

        @Override
        public DataSource getDataSource() {
            return dataSource;
        }
    }

    public JdbcConnectionProbe(JdbcConnectionProbeConfiguration configuration, IProbeContext context, int index) {
        super(configuration, context, index, "jdbcConnectionProbe");

        this.configuration = configuration;
    }

    @Override
    public void start() {
        JdbcConnectionProbeInterceptor.interceptor = this;
    }

    @Override
    public void stop() {
        JdbcConnectionProbeInterceptor.interceptor = null;
    }

    @Override
    public Object onEnter(int index, int version, Object instance, Object[] params) {
        Container container = threadLocalAccessor.get();
        if (container == null || container.inCall || container.top == null || isRecursive())
            return null;

        container.inCall = true;
        setRecursive(true);

        long value = getStartTime();

        container.inCall = false;

        return value;
    }

    @Override
    public void onReturnExit(int index, int version, Object param, Object instance, Object retVal) {
        Container container = threadLocalAccessor.get();
        if (container == null || container.inCall)
            return;

        container.inCall = true;
        setRecursive(false);

        updateRequest(container, param, instance, retVal);

        container.inCall = false;
    }

    @Override
    public void onThrowExit(int index, int version, Object param, Object instance, Throwable exception) {
        onReturnExit(index, version, param, instance, 0);
    }

    @Override
    protected ExitPointProbeCollector doCreateCollector(int index, String name, UUID stackId, ICallPath callPath,
                                                        StackProbeRootCollector root, StackProbeCollector parent, JsonObject metadata,
                                                        ExitPointProbeCalibrateInfo calibrateInfo, boolean leaf) {
        return new JdbcConnectionProbeCollector(configuration, index, name, stackId, callPath, root, parent, metadata,
                calibrateInfo, leaf);
    }

    @Override
    protected Object createCalibratingRequest() {
        return new JdbcConnectionRawRequest(0, 0, null, null);
    }

    private void updateRequest(Container container, Object rawRequest, Object instance, Object retVal) {
        long startTime = (Long) rawRequest;
        long delta = getTimeDelta(startTime);

        JdbcConnectionRawRequest request = new JdbcConnectionRawRequest(startTime, delta, (Connection) retVal, (DataSource) instance);

        beginRequest(container, request);

        long[] counters = container.counters;
        counters[AppStackCounterType.DB_TIME.ordinal()] += delta;
        counters[AppStackCounterType.DB_CONNECT_COUNT.ordinal()]++;
        counters[AppStackCounterType.DB_CONNECT_TIME.ordinal()] += delta;

        endRequest(container, null, delta, 0);
    }
}
