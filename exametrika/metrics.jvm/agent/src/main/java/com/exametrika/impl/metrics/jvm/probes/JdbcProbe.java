/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.jvm.probes;

import java.sql.Statement;
import java.util.UUID;

import com.exametrika.api.aggregator.common.model.ICallPath;
import com.exametrika.api.instrument.IJoinPoint;
import com.exametrika.api.metrics.jvm.config.JdbcProbeConfiguration;
import com.exametrika.api.profiler.config.AppStackCounterType;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.WeakIdentityHashMap;
import com.exametrika.impl.metrics.jvm.boot.JdbcProbeInterceptor;
import com.exametrika.impl.profiler.probes.ExitPointProbe;
import com.exametrika.impl.profiler.probes.ExitPointProbeCalibrateInfo;
import com.exametrika.impl.profiler.probes.ExitPointProbeCollector;
import com.exametrika.impl.profiler.probes.StackProbeCollector;
import com.exametrika.impl.profiler.probes.StackProbeRootCollector;
import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor.Container;
import com.exametrika.spi.metrics.jvm.IJdbcRawRequest;
import com.exametrika.spi.metrics.jvm.JdbcBatchQueryInfo;
import com.exametrika.spi.profiler.IProbeContext;


/**
 * The {@link JdbcProbe} is a JDBC probe.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class JdbcProbe extends ExitPointProbe {
    private final JdbcProbeConfiguration configuration;

    public static class JdbcRawRequest implements IJdbcRawRequest {
        private long startTime;
        private final JdbcBatchQueryInfo query;
        private final Statement statement;
        private long delta;

        public JdbcRawRequest(JdbcBatchQueryInfo query, Statement statement) {
            this.query = query;
            this.statement = statement;
        }

        public long getStartTime() {
            return startTime;
        }

        @Override
        public String getUrl() {
            try {
                if (statement != null && statement.getConnection() != null && statement.getConnection().getMetaData() != null)
                    return statement.getConnection().getMetaData().getURL();
            } catch (Throwable e) {
            }

            return null;
        }

        @Override
        public JdbcBatchQueryInfo getQuery() {
            return query;
        }

        @Override
        public Statement getStatement() {
            return statement;
        }

        public long getDelta() {
            return delta;
        }
    }

    public JdbcProbe(JdbcProbeConfiguration configuration, IProbeContext context, int index) {
        super(configuration, context, index, "jdbcProbe");

        this.configuration = configuration;
    }

    @Override
    public Object allocate() {
        return new JdbcExitPointInfo();
    }

    @Override
    public void start() {
        JdbcProbeInterceptor.interceptor = this;
    }

    @Override
    public void stop() {
        JdbcProbeInterceptor.interceptor = null;
    }

    @Override
    public Object onEnter(int index, int version, Object instance, Object[] params) {
        Container container = threadLocalAccessor.get();
        if (container == null || container.inCall || container.top == null || isRecursive())
            return null;

        container.inCall = true;

        Object request = createRequest(index, instance, params);
        if (request instanceof JdbcRawRequest) {
            beginRequest(container, request);
            ((JdbcRawRequest) request).startTime = getStartTime();
        }

        if (request != null)
            setRecursive(true);

        container.inCall = false;

        return request;
    }

    @Override
    public void onReturnExit(int index, int version, Object param, Object instance, Object retVal) {
        Container container = threadLocalAccessor.get();
        if (container == null || container.inCall)
            return;

        container.inCall = true;
        setRecursive(false);

        updateRequest(container, param, instance, retVal);

        if (param instanceof JdbcRawRequest)
            endRequest(container, null, ((JdbcRawRequest) param).delta, 0);

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
        return new JdbcProbeCollector(configuration, index, name, stackId, callPath, root, parent, metadata, calibrateInfo, leaf);
    }

    @Override
    protected Object createCalibratingRequest() {
        return new JdbcRawRequest(new JdbcBatchQueryInfo(), null);
    }

    private Object createRequest(int index, Object instance, Object[] params) {
        IJoinPoint joinPoint = context.getJoinPointProvider().findJoinPoint(index, -1);
        if (joinPoint == null)
            return null;

        if (joinPoint.getMethodName().startsWith("set")) {
            Statement statement = (Statement) instance;
            JdbcBatchQueryInfo info = getQueryInfo(statement);
            if (info != null && params.length > 1) {
                Object value = normalizeValue(params[1]);

                if (params[0] instanceof Integer)
                    info.setParameter((Integer) params[0], value);
                else if (params[0] instanceof String)
                    info.setParameter((String) params[0], value);
            }

            return null;
        } else if (joinPoint.getMethodName().startsWith("addBatch")) {
            Statement statement = (Statement) instance;
            JdbcBatchQueryInfo info = getQueryInfo(statement);
            if (info != null) {
                if (params.length == 0)
                    info.addBatch();
                else if (params[0] instanceof String)
                    info.addBatch((String) params[0]);
            }
            return null;
        } else if (joinPoint.getMethodName().startsWith("clearParameters")) {
            Statement statement = (Statement) instance;
            JdbcBatchQueryInfo info = getQueryInfo(statement);
            if (info != null)
                info.clearParameters();

            return null;
        } else if (joinPoint.getMethodName().startsWith("clearBatch")) {
            Statement statement = (Statement) instance;
            JdbcBatchQueryInfo info = getQueryInfo(statement);
            if (info != null)
                info.clearBatch();

            return null;
        } else if (joinPoint.getMethodName().startsWith("close")) {
            Statement statement = (Statement) instance;
            JdbcBatchQueryInfo info = getQueryInfo(statement);
            if (info != null)
                info.clear();

            return null;
        } else if (joinPoint.getMethodName().startsWith("prepare")) {
            if (params[0] instanceof String)
                return params[0];
            else
                return null;
        }

        if (joinPoint.getMethodName().startsWith("execute")) {
            Statement statement = (Statement) instance;
            JdbcBatchQueryInfo info = getQueryInfo(statement);
            if (info != null) {
                if (params.length > 0 && params[0] instanceof String)
                    info.setQueryText((String) params[0]);

                return new JdbcRawRequest(info, statement);
            } else
                return null;
        } else
            return null;
    }

    private Object normalizeValue(Object value) {
        if (value instanceof Double || value instanceof Boolean || value instanceof String || value == null)
            return value;
        else if (value instanceof Float)
            return ((Float) value).doubleValue();
        else if (value instanceof Number)
            return ((Number) value).longValue();
        else
            return value.toString();
    }

    private void updateRequest(Container container, Object rawRequest, Object instance, Object retVal) {
        if (rawRequest instanceof String && retVal instanceof Statement) {
            JdbcBatchQueryInfo info = getQueryInfo((Statement) retVal);
            if (info != null)
                info.setQueryText((String) rawRequest);

            return;
        }

        JdbcRawRequest request = (JdbcRawRequest) rawRequest;
        request.delta = getTimeDelta(request.startTime);

        long[] counters = container.counters;
        counters[AppStackCounterType.DB_TIME.ordinal()] += request.delta;
        counters[AppStackCounterType.DB_QUERY_COUNT.ordinal()]++;
        counters[AppStackCounterType.DB_QUERY_TIME.ordinal()] += request.delta;
    }

    private JdbcBatchQueryInfo getQueryInfo(Statement statement) {
        JdbcExitPointInfo slotInfo = slot.get();
        JdbcBatchQueryInfo info = slotInfo.queries.get(statement);
        if (info == null) {
            info = new JdbcBatchQueryInfo();
            slotInfo.queries.put(statement, info);
        }

        return info;
    }

    private static class JdbcExitPointInfo extends ExitPointInfo {
        private final WeakIdentityHashMap<Statement, JdbcBatchQueryInfo> queries = new WeakIdentityHashMap<Statement, JdbcBatchQueryInfo>();
    }
}
