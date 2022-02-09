/**
 * Copyright 2014 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.jvm.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.exametrika.api.aggregator.common.meters.config.ExpressionLogFilterConfiguration;
import com.exametrika.api.aggregator.common.meters.config.InstanceFieldConfiguration;
import com.exametrika.api.aggregator.common.meters.config.StandardFieldConfiguration;
import com.exametrika.api.aggregator.common.meters.config.UniformHistogramFieldConfiguration;
import com.exametrika.api.instrument.config.CallPointcut;
import com.exametrika.api.instrument.config.ClassFilter;
import com.exametrika.api.instrument.config.ClassNameFilter;
import com.exametrika.api.instrument.config.InstrumentationConfiguration;
import com.exametrika.api.instrument.config.InterceptPointcut;
import com.exametrika.api.instrument.config.InterceptPointcut.Kind;
import com.exametrika.api.instrument.config.MemberFilter;
import com.exametrika.api.instrument.config.MemberNameFilter;
import com.exametrika.api.instrument.config.Pointcut;
import com.exametrika.api.instrument.config.QualifiedMemberNameFilter;
import com.exametrika.api.instrument.config.QualifiedMethodFilter;
import com.exametrika.api.metrics.jvm.config.FileProbeConfiguration;
import com.exametrika.api.metrics.jvm.config.GcFilterConfiguration;
import com.exametrika.api.metrics.jvm.config.HttpConnectionProbeConfiguration;
import com.exametrika.api.metrics.jvm.config.HttpInterceptPointcut;
import com.exametrika.api.metrics.jvm.config.HttpServletProbeConfiguration;
import com.exametrika.api.metrics.jvm.config.JdbcConnectionProbeConfiguration;
import com.exametrika.api.metrics.jvm.config.JdbcProbeConfiguration;
import com.exametrika.api.metrics.jvm.config.JdbcRequestGroupingStrategyConfiguration;
import com.exametrika.api.metrics.jvm.config.JmsConsumerProbeConfiguration;
import com.exametrika.api.metrics.jvm.config.JmsProducerProbeConfiguration;
import com.exametrika.api.metrics.jvm.config.JmxAttributeConfiguration;
import com.exametrika.api.metrics.jvm.config.JmxMonitorConfiguration;
import com.exametrika.api.metrics.jvm.config.JulProbeConfiguration;
import com.exametrika.api.metrics.jvm.config.JvmBufferPoolMonitorConfiguration;
import com.exametrika.api.metrics.jvm.config.JvmCodeMonitorConfiguration;
import com.exametrika.api.metrics.jvm.config.JvmKpiMonitorConfiguration;
import com.exametrika.api.metrics.jvm.config.JvmMemoryMonitorConfiguration;
import com.exametrika.api.metrics.jvm.config.JvmSunMemoryMonitorConfiguration;
import com.exametrika.api.metrics.jvm.config.JvmSunThreadMonitorConfiguration;
import com.exametrika.api.metrics.jvm.config.JvmThreadMonitorConfiguration;
import com.exametrika.api.metrics.jvm.config.Log4jProbeConfiguration;
import com.exametrika.api.metrics.jvm.config.LogbackProbeConfiguration;
import com.exametrika.api.metrics.jvm.config.TcpProbeConfiguration;
import com.exametrika.api.metrics.jvm.config.UdpProbeConfiguration;
import com.exametrika.api.metrics.jvm.config.UrlRequestGroupingStrategyConfiguration;
import com.exametrika.common.config.AbstractExtensionLoader;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.config.InvalidConfigurationException;
import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Enums;
import com.exametrika.impl.metrics.jvm.boot.FileProbeInterceptor;
import com.exametrika.impl.metrics.jvm.boot.HttpConnectionProbeInterceptor;
import com.exametrika.impl.metrics.jvm.boot.HttpServletProbeInterceptor;
import com.exametrika.impl.metrics.jvm.boot.JdbcConnectionProbeInterceptor;
import com.exametrika.impl.metrics.jvm.boot.JdbcProbeInterceptor;
import com.exametrika.impl.metrics.jvm.boot.JmsConsumerProbeInterceptor;
import com.exametrika.impl.metrics.jvm.boot.JmsProducerProbeInterceptor;
import com.exametrika.impl.metrics.jvm.boot.JulProbeInterceptor;
import com.exametrika.impl.metrics.jvm.boot.Log4jProbeInterceptor;
import com.exametrika.impl.metrics.jvm.boot.LogbackProbeInterceptor;
import com.exametrika.impl.metrics.jvm.boot.TcpProbeInterceptor;
import com.exametrika.impl.metrics.jvm.boot.UdpProbeInterceptor;
import com.exametrika.spi.aggregator.common.meters.config.CounterConfiguration;
import com.exametrika.spi.aggregator.common.meters.config.LogConfiguration;
import com.exametrika.spi.aggregator.common.meters.config.MeterConfiguration;
import com.exametrika.spi.instrument.config.IInstrumentationLoadContext;
import com.exametrika.spi.instrument.config.StaticInterceptorConfiguration;
import com.exametrika.spi.profiler.config.EntryPointProbeConfiguration;
import com.exametrika.spi.profiler.config.EntryPointProbeConfiguration.PrimaryType;
import com.exametrika.spi.profiler.config.ExitPointProbeConfiguration;
import com.exametrika.spi.profiler.config.RequestMappingStrategyConfiguration;

/**
 * The {@link JvmMetricsConfigurationLoader} is a configuration loader of JVM metrics.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class JvmMetricsConfigurationLoader extends AbstractExtensionLoader {
    @Override
    public Object loadExtension(String name, String type, Object object, ILoadContext context) {
        JsonObject element = (JsonObject) object;

        if (type.equals("JvmBufferPoolMonitor")) {
            String scope = element.get("scope", null);
            long period = element.get("period");
            String measurementStrategy = element.get("measurementStrategy", null);

            return new JvmBufferPoolMonitorConfiguration(name, scope, period, measurementStrategy);
        } else if (type.equals("JvmCodeMonitor")) {
            String scope = element.get("scope", null);
            long period = element.get("period");
            String measurementStrategy = element.get("measurementStrategy", null);

            return new JvmCodeMonitorConfiguration(name, scope, period, measurementStrategy);
        } else if (type.equals("JvmKpiMonitor")) {
            String scope = element.get("scope", null);
            long period = element.get("period");
            String componentType = element.get("componentType");
            String measurementStrategy = element.get("measurementStrategy", null);
            long maxGcDuration = element.get("maxGcDuration");

            return new JvmKpiMonitorConfiguration(name, scope, componentType, period, measurementStrategy, maxGcDuration);
        } else if (type.equals("JvmMemoryMonitor")) {
            String scope = element.get("scope", null);
            long period = element.get("period");
            String measurementStrategy = element.get("measurementStrategy", null);

            return new JvmMemoryMonitorConfiguration(name, scope, period, measurementStrategy);
        } else if (type.equals("JvmSunMemoryMonitor")) {
            String scope = element.get("scope", null);
            long period = element.get("period");
            String measurementStrategy = element.get("measurementStrategy", null);

            CounterConfiguration timeCounter = load(null, "Counter", element.get("timeCounter", null), context,
                    new CounterConfiguration(true, Arrays.asList(new StandardFieldConfiguration(),
                            new UniformHistogramFieldConfiguration(0, 2000, 20),
                            new InstanceFieldConfiguration(10, true)), false, 0));
            CounterConfiguration bytesCounter = load(null, "Counter", element.get("bytesCounter", null), context,
                    new CounterConfiguration(true, Arrays.asList(new StandardFieldConfiguration(),
                            new UniformHistogramFieldConfiguration(0, 10000000, 20),
                            new InstanceFieldConfiguration(10, true)), false, 0));
            LogConfiguration log = load(null, "Log", element.get("log", null), context,
                    new LogConfiguration(true, new ExpressionLogFilterConfiguration(
                            "(value.object.end - value.object.start) > 100 && value.object.bytes > 100000"), null, null, null,
                            100, 512, 10000, 50, 100));
            long maxGcDuration = element.get("maxGcDuration");

            GcFilterConfiguration filter = loadFilter((JsonObject) element.get("filter", null));

            return new JvmSunMemoryMonitorConfiguration(name, scope, period, measurementStrategy, timeCounter, bytesCounter,
                    log, filter, maxGcDuration);
        } else if (type.equals("JvmThreadMonitor")) {
            String scope = element.get("scope", null);
            long period = element.get("period");
            String measurementStrategy = element.get("measurementStrategy", null);

            boolean contention = element.get("contention");
            boolean locks = element.get("locks");
            boolean stackTraces = element.get("stackTraces");
            long maxStackTraceDepth = element.get("maxStackTraceDepth");
            return new JvmThreadMonitorConfiguration(name, scope, period, measurementStrategy, contention, locks, stackTraces,
                    (int) maxStackTraceDepth);
        } else if (type.equals("JvmSunThreadMonitor")) {
            String scope = element.get("scope", null);
            long period = element.get("period");
            String measurementStrategy = element.get("measurementStrategy", null);

            boolean contention = element.get("contention");
            boolean memoryAllocation = element.get("memoryAllocation");
            boolean locks = element.get("locks");
            boolean stackTraces = element.get("stackTraces");
            long maxStackTraceDepth = element.get("maxStackTraceDepth");
            return new JvmSunThreadMonitorConfiguration(name, scope, period, measurementStrategy, contention, locks,
                    stackTraces, (int) maxStackTraceDepth, memoryAllocation);
        } else if (type.equals("JmxMonitor")) {
            String scope = element.get("scope", null);
            long period = element.get("period");
            String measurementStrategy = element.get("measurementStrategy", null);

            String componentType = element.get("componentType");
            String jmxObject = element.get("object");

            List<JmxAttributeConfiguration> attributes = loadJmxAttributes((JsonArray) element.get("attributes"), context);
            return new JmxMonitorConfiguration(name, scope, period, measurementStrategy, componentType, jmxObject, attributes);
        } else if (type.equals("JulProbe")) {
            String scopeType = element.get("scopeType");
            String measurementStrategy = element.get("measurementStrategy", null);
            long extractionPeriod = element.get("extractionPeriod");
            long warmupDelay = element.get("warmupDelay");
            LogConfiguration log = load(null, "Log", (JsonObject) element.get("log"), context);

            IInstrumentationLoadContext instrumentationContext = context.get(InstrumentationConfiguration.SCHEMA);
            QualifiedMethodFilter julFilter = new QualifiedMethodFilter(new ClassFilter("java.util.logging.Logger"),
                    new MemberFilter("log(LogRecord)*"));
            QualifiedMethodFilter methodFilter = new QualifiedMethodFilter(Arrays.asList(julFilter), null);

            InterceptPointcut pointcut = new InterceptPointcut(name, methodFilter, Enums.of(Kind.ENTER),
                    new StaticInterceptorConfiguration(JulProbeInterceptor.class), true, true, 0);
            instrumentationContext.addPointcut(pointcut);

            return new JulProbeConfiguration(name, scopeType, extractionPeriod, measurementStrategy, warmupDelay, log);
        } else if (type.equals("Log4jProbe")) {
            String scopeType = element.get("scopeType");
            String measurementStrategy = element.get("measurementStrategy", null);
            long extractionPeriod = element.get("extractionPeriod");
            long warmupDelay = element.get("warmupDelay");
            LogConfiguration log = load(null, "Log", (JsonObject) element.get("log"), context);

            IInstrumentationLoadContext instrumentationContext = context.get(InstrumentationConfiguration.SCHEMA);
            QualifiedMethodFilter log4jFilter = new QualifiedMethodFilter(new ClassFilter("org.apache.log4j.Category"),
                    new MemberFilter("callAppenders(LoggingEvent)*"));
            QualifiedMethodFilter methodFilter = new QualifiedMethodFilter(Arrays.asList(log4jFilter), null);

            InterceptPointcut pointcut = new InterceptPointcut(name, methodFilter, Enums.of(Kind.ENTER),
                    new StaticInterceptorConfiguration(Log4jProbeInterceptor.class), true, true, 0);
            instrumentationContext.addPointcut(pointcut);

            return new Log4jProbeConfiguration(name, scopeType, extractionPeriod, measurementStrategy, warmupDelay, log);
        } else if (type.equals("LogbackProbe")) {
            String scopeType = element.get("scopeType");
            String measurementStrategy = element.get("measurementStrategy", null);
            long extractionPeriod = element.get("extractionPeriod");
            long warmupDelay = element.get("warmupDelay");
            LogConfiguration log = load(null, "Log", (JsonObject) element.get("log"), context);

            IInstrumentationLoadContext instrumentationContext = context.get(InstrumentationConfiguration.SCHEMA);
            QualifiedMethodFilter logbackFilter = new QualifiedMethodFilter(new ClassFilter("ch.qos.logback.classic.Logger"),
                    new MemberFilter("callAppenders(ILoggingEvent)*"));
            QualifiedMethodFilter methodFilter = new QualifiedMethodFilter(Arrays.asList(logbackFilter), null);

            InterceptPointcut pointcut = new InterceptPointcut(name, methodFilter, Enums.of(Kind.ENTER),
                    new StaticInterceptorConfiguration(LogbackProbeInterceptor.class), true, true, 0);
            instrumentationContext.addPointcut(pointcut);

            return new LogbackProbeConfiguration(name, scopeType, extractionPeriod, measurementStrategy, warmupDelay, log);
        } else if (type.equals("HttpServletProbe")) {
            String scopeType = element.get("scopeType");
            String measurementStrategy = element.get("measurementStrategy", null);
            String stackMeasurementStrategy = element.get("stackMeasurementStrategy", null);
            long warmupDelay = element.get("warmupDelay");
            RequestMappingStrategyConfiguration requestMappingStrategy = load(null, null,
                    (JsonObject) element.get("requestMappingStrategy"), context);
            long maxDuration = element.get("maxDuration");
            String primaryEntryPointExpression = element.get("primaryEntryPointExpression", null);
            CounterConfiguration transactionTimeCounter = load(null, "Counter", (JsonObject) element.get("transactionTimeCounter"), context);
            LogConfiguration stalledRequestsLog = load(null, "Log", (JsonObject) element.get("stalledRequestsLog"), context);
            CounterConfiguration timeCounter = load(null, "Counter", (JsonObject) element.get("timeCounter"), context);
            CounterConfiguration receiveBytesCounter = load(null, "Counter", (JsonObject) element.get("receiveBytesCounter"), context);
            CounterConfiguration sendBytesCounter = load(null, "Counter", (JsonObject) element.get("sendBytesCounter"), context);
            LogConfiguration errorsLog = load(null, "Log", (JsonObject) element.get("errorsLog"), context);
            String allowPrimaryStr = element.get("allowPrimary");
            PrimaryType allowPrimary;
            if (allowPrimaryStr.equals("yes"))
                allowPrimary = PrimaryType.YES;
            else if (allowPrimaryStr.equals("no"))
                allowPrimary = PrimaryType.NO;
            else if (allowPrimaryStr.equals("always"))
                allowPrimary = PrimaryType.ALWAYS;
            else
                allowPrimary = Assert.error();
            boolean allowSecondary = element.get("allowSecondary");

            IInstrumentationLoadContext instrumentationContext = context.get(InstrumentationConfiguration.SCHEMA);
            QualifiedMethodFilter methodFilter = new QualifiedMethodFilter(new ClassFilter(
                    "javax.servlet.http.HttpServlet"), new MemberFilter(
                    "service(HttpServletRequest,HttpServletResponse)*"));
            QualifiedMemberNameFilter calledMethodFilter = new QualifiedMemberNameFilter(new ClassNameFilter(
                    "javax.servlet.http.HttpServlet"), new MemberNameFilter(Arrays.asList(
                    "doGet(HttpServletRequest,HttpServletResponse)*",
                    "doPut(HttpServletRequest,HttpServletResponse)*",
                    "doPost(HttpServletRequest,HttpServletResponse)*",
                    "doDelete(HttpServletRequest,HttpServletResponse)*",
                    "doHead(HttpServletRequest,HttpServletResponse)*",
                    "doTrace(HttpServletRequest,HttpServletResponse)*",
                    "doOptions(HttpServletRequest,HttpServletResponse)*"), null));

            Pointcut pointcut = new CallPointcut(name + "1", methodFilter, new StaticInterceptorConfiguration(HttpServletProbeInterceptor.class),
                    calledMethodFilter, true, false, EntryPointProbeConfiguration.POINTCUT_PRIORITY);
            instrumentationContext.addPointcut(pointcut);

            QualifiedMethodFilter errorsMethodFilter = new QualifiedMethodFilter(new ClassFilter(
                    new ClassNameFilter("javax.servlet.http.HttpServletResponse"), true, null), new MemberFilter(new MemberNameFilter(Arrays.asList(
                    "sendError(int)*", "sendError(int,String)*", "setStatus(int)*", "setStatus(int,String)*"),
                    null), null, null, null));
            QualifiedMethodFilter resultMethodFilter = new QualifiedMethodFilter(new ClassFilter(
                    new ClassNameFilter("javax.servlet.ServletResponse"), true, null), new MemberFilter(new MemberNameFilter(Arrays.asList(
                    "getOutputStream()*"), null), null, null, null));
            QualifiedMethodFilter inputStreamFilter = new QualifiedMethodFilter(new ClassFilter(
                    new ClassNameFilter("javax.servlet.ServletInputStream"), true, null),
                    new MemberFilter(new MemberNameFilter(Arrays.asList("read()*", "read(byte[],int,int)*",
                            "readLine(byte[],int,int)*"), null), null));
            QualifiedMethodFilter outputStreamFilter = new QualifiedMethodFilter(new ClassFilter(
                    new ClassNameFilter("javax.servlet.ServletOutputStream"), true, null),
                    new MemberFilter(new MemberNameFilter(Arrays.asList("write(int)*", "write(byte[],int,int)*"), null), null));
            QualifiedMethodFilter servletResponseFilter = new QualifiedMethodFilter(new ClassFilter(
                    new ClassNameFilter("javax.servlet.ServletResponse"), true, null),
                    new MemberFilter(new MemberNameFilter(Arrays.asList("reset()*", "resetBuffer()*", "setContentLength(int)*"), null), null));
            methodFilter = new QualifiedMethodFilter(Arrays.asList(errorsMethodFilter, resultMethodFilter,
                    inputStreamFilter, outputStreamFilter, servletResponseFilter), null);
            pointcut = new InterceptPointcut(name + "2", methodFilter, Enums.of(Kind.ENTER, Kind.RETURN_EXIT, Kind.THROW_EXIT),
                    new StaticInterceptorConfiguration(HttpServletProbeInterceptor.class), true, false,
                    EntryPointProbeConfiguration.POINTCUT_PRIORITY);
            instrumentationContext.addPointcut(pointcut);

            methodFilter = new QualifiedMethodFilter(new ClassFilter(
                    new ClassNameFilter("javax.servlet.http.HttpServletRequest"), true, null), new MemberFilter(new MemberNameFilter(Arrays.asList(
                    "getReader():BufferedReader"), null), null));
            pointcut = new HttpInterceptPointcut(name + "3", methodFilter, Enums.of(Kind.ENTER, Kind.RETURN_EXIT, Kind.THROW_EXIT),
                    new StaticInterceptorConfiguration(HttpServletProbeInterceptor.class),
                    "java.io.BufferedReader onReturnExitReader(java.lang.Object,java.lang.Object)",
                    EntryPointProbeConfiguration.POINTCUT_PRIORITY);
            instrumentationContext.addPointcut(pointcut);

            methodFilter = new QualifiedMethodFilter(new ClassFilter(
                    new ClassNameFilter("javax.servlet.http.HttpServletResponse"), true, null), new MemberFilter(new MemberNameFilter(Arrays.asList(
                    "getWriter():PrintWriter"), null), null));
            pointcut = new HttpInterceptPointcut(name + "4", methodFilter, Enums.of(Kind.ENTER, Kind.RETURN_EXIT, Kind.THROW_EXIT),
                    new StaticInterceptorConfiguration(HttpServletProbeInterceptor.class),
                    "java.io.PrintWriter onReturnExitWriter(java.lang.Object,java.lang.Object)",
                    EntryPointProbeConfiguration.POINTCUT_PRIORITY);
            instrumentationContext.addPointcut(pointcut);

            return new HttpServletProbeConfiguration(name, scopeType, measurementStrategy, warmupDelay,
                    requestMappingStrategy, maxDuration, transactionTimeCounter, stalledRequestsLog,
                    primaryEntryPointExpression, stackMeasurementStrategy, allowPrimary, allowSecondary,
                    timeCounter, receiveBytesCounter, sendBytesCounter, errorsLog);
        } else if (type.equals("HttpConnectionProbe")) {
            String scopeType = element.get("scopeType");
            String measurementStrategy = element.get("measurementStrategy", null);
            long warmupDelay = element.get("warmupDelay");
            RequestMappingStrategyConfiguration requestMappingStrategy = load(null, null,
                    (JsonObject) element.get("requestMappingStrategy"), context);
            CounterConfiguration timeCounter = load(null, "Counter", (JsonObject) element.get("timeCounter"), context);
            CounterConfiguration receiveBytesCounter = load(null, "Counter", (JsonObject) element.get("receiveBytesCounter"), context);
            CounterConfiguration sendBytesCounter = load(null, "Counter", (JsonObject) element.get("sendBytesCounter"), context);
            LogConfiguration errorsLog = load(null, "Log", (JsonObject) element.get("errorsLog"), context);

            IInstrumentationLoadContext instrumentationContext = context.get(InstrumentationConfiguration.SCHEMA);
            QualifiedMethodFilter methodFilter = new QualifiedMethodFilter(new ClassFilter(
                    new ClassNameFilter("sun.net.www.protocol.http.HttpURLConnection"), true, null), new MemberFilter(
                    new MemberNameFilter(Arrays.asList("connect()*", "getOutputStream()*"), null), null));
            Pointcut pointcut = new InterceptPointcut(name + "1", methodFilter, Enums.of(Kind.ENTER, Kind.RETURN_EXIT, Kind.THROW_EXIT),
                    new StaticInterceptorConfiguration(HttpConnectionProbeInterceptor.class), false, false,
                    ExitPointProbeConfiguration.POINTCUT_PRIORITY);
            instrumentationContext.addPointcut(pointcut);

            methodFilter = new QualifiedMethodFilter(new ClassFilter(
                    new ClassNameFilter("sun.net.www.protocol.http.HttpURLConnection"), true, null), new MemberFilter(new MemberNameFilter(Arrays.asList(
                    "getInputStream()*"), null), null));
            pointcut = new HttpInterceptPointcut(name + "2", methodFilter, Enums.of(Kind.ENTER, Kind.RETURN_EXIT, Kind.THROW_EXIT),
                    new StaticInterceptorConfiguration(HttpConnectionProbeInterceptor.class),
                    "java.io.InputStream onReturnExit(java.lang.Object,java.lang.Object)", ExitPointProbeConfiguration.POINTCUT_PRIORITY);
            instrumentationContext.addPointcut(pointcut);

            return new HttpConnectionProbeConfiguration(name, scopeType, measurementStrategy, warmupDelay,
                    requestMappingStrategy, timeCounter, receiveBytesCounter, sendBytesCounter, errorsLog);
        } else if (type.equals("JmsConsumerProbe")) {
            String scopeType = element.get("scopeType");
            String measurementStrategy = element.get("measurementStrategy", null);
            String stackMeasurementStrategy = element.get("stackMeasurementStrategy", null);
            long warmupDelay = element.get("warmupDelay");
            RequestMappingStrategyConfiguration requestMappingStrategy = load(null, null,
                    (JsonObject) element.get("requestMappingStrategy"), context);
            long maxDuration = element.get("maxDuration");
            String primaryEntryPointExpression = element.get("primaryEntryPointExpression", null);
            CounterConfiguration transactionTimeCounter = load(null, "Counter", (JsonObject) element.get("transactionTimeCounter"), context);
            LogConfiguration stalledRequestsLog = load(null, "Log", (JsonObject) element.get("stalledRequestsLog"), context);
            QualifiedMethodFilter enclosingMessageHandler = load(null, "QualifiedMethodFilter", (JsonObject) element.get("enclosingMessageHandler"), context);
            CounterConfiguration timeCounter = load(null, "Counter", (JsonObject) element.get("timeCounter"), context);
            CounterConfiguration receiveBytesCounter = load(null, "Counter", (JsonObject) element.get("receiveBytesCounter"), context);
            CounterConfiguration sendBytesCounter = load(null, "Counter", (JsonObject) element.get("sendBytesCounter"), context);
            LogConfiguration errorsLog = load(null, "Log", (JsonObject) element.get("errorsLog"), context);
            String allowPrimaryStr = element.get("allowPrimary");
            PrimaryType allowPrimary;
            if (allowPrimaryStr.equals("yes"))
                allowPrimary = PrimaryType.YES;
            else if (allowPrimaryStr.equals("no"))
                allowPrimary = PrimaryType.NO;
            else if (allowPrimaryStr.equals("always"))
                allowPrimary = PrimaryType.ALWAYS;
            else
                allowPrimary = Assert.error();
            boolean allowSecondary = element.get("allowSecondary");

            IInstrumentationLoadContext instrumentationContext = context.get(InstrumentationConfiguration.SCHEMA);
            QualifiedMethodFilter messageListenerFilter = new QualifiedMethodFilter(new ClassFilter(
                    new ClassNameFilter("javax.jms.MessageListener"), true, null), new MemberFilter("onMessage(Message)*"));

            Pointcut pointcut = new InterceptPointcut(name + "1", messageListenerFilter, Enums.of(Kind.ENTER,
                    Kind.RETURN_EXIT, Kind.THROW_EXIT), new StaticInterceptorConfiguration(JmsConsumerProbeInterceptor.class),
                    true, false, EntryPointProbeConfiguration.POINTCUT_PRIORITY);
            instrumentationContext.addPointcut(pointcut);

            QualifiedMemberNameFilter calledMethodFilter = new QualifiedMemberNameFilter(
                    new ClassNameFilter(Arrays.asList("javax.jms.JMSConsumer", "javax.jms.MessageConsumer", "javax.jms.QueueReceiver",
                            "javax.jms.TopicSubscriber"), null),
                    new MemberNameFilter(Arrays.asList("receive()*", "receive(long)*", "receiveNoWait()*"), null));

            pointcut = new CallPointcut(name + "2", enclosingMessageHandler, new StaticInterceptorConfiguration(JmsConsumerProbeInterceptor.class),
                    calledMethodFilter, true, false, 0);
            instrumentationContext.addPointcut(pointcut);

            return new JmsConsumerProbeConfiguration(name, scopeType, measurementStrategy, warmupDelay,
                    requestMappingStrategy, maxDuration, transactionTimeCounter,
                    stalledRequestsLog, primaryEntryPointExpression, stackMeasurementStrategy, allowPrimary, allowSecondary,
                    timeCounter, receiveBytesCounter, sendBytesCounter, errorsLog);
        } else if (type.equals("JmsProducerProbe")) {
            String scopeType = element.get("scopeType");
            String measurementStrategy = element.get("measurementStrategy", null);
            long warmupDelay = element.get("warmupDelay");
            CounterConfiguration bytesCounter = load(null, "Counter", (JsonObject) element.get("bytesCounter"), context);

            IInstrumentationLoadContext instrumentationContext = context.get(InstrumentationConfiguration.SCHEMA);
            QualifiedMethodFilter jmsProducerFilter = new QualifiedMethodFilter(new ClassFilter(
                    new ClassNameFilter("javax.jms.JMSProducer"), true, null), new MemberFilter(new MemberNameFilter(Arrays.asList(
                    "send(Destination,byte[])*", "send(Destination,Map)*",
                    "send(Destination,Message)*", "send(Destination,Serializable)*",
                    "send(Destination,String)*"), null), null));
            QualifiedMethodFilter messageProducerFilter = new QualifiedMethodFilter(new ClassFilter(
                    new ClassNameFilter("javax.jms.MessageProducer"), true, null), new MemberFilter(new MemberNameFilter(Arrays.asList(
                    "send(Destination,Message*", "send(Message*"), null), null));
            QualifiedMethodFilter queueSenderFilter = new QualifiedMethodFilter(new ClassFilter(
                    new ClassNameFilter("javax.jms.QueueSender"), true, null), new MemberFilter("send(Queue*"));
            QualifiedMethodFilter topicPublisherFilter = new QualifiedMethodFilter(new ClassFilter(
                    new ClassNameFilter("javax.jms.TopicPublisher"), true, null), new MemberFilter(new MemberNameFilter(Arrays.asList(
                    "publish(Topic,Message*", "publish(Message*"), null), null));
            QualifiedMethodFilter methodFilter = new QualifiedMethodFilter(Arrays.asList(jmsProducerFilter, messageProducerFilter,
                    queueSenderFilter, topicPublisherFilter), null);

            InterceptPointcut pointcut = new InterceptPointcut(name + "1", methodFilter, Enums.of(Kind.ENTER, Kind.RETURN_EXIT, Kind.THROW_EXIT),
                    new StaticInterceptorConfiguration(JmsProducerProbeInterceptor.class), true, false,
                    ExitPointProbeConfiguration.POINTCUT_PRIORITY);
            instrumentationContext.addPointcut(pointcut);

            if (bytesCounter.isEnabled()) {
                QualifiedMethodFilter bytesMessageFilter = new QualifiedMethodFilter(new ClassFilter(
                        new ClassNameFilter("javax.jms.BytesMessage"), true, null), new MemberFilter(new MemberNameFilter(Arrays.asList(
                        "writeBoolean(boolean)*", "writeByte(byte)*", "writeBytes(byte[])*", "writeBytes(byte[],int,int)*",
                        "writeChar(char)*", "writeDouble(double)*", "writeFloat(float)*", "writeInt(int)*",
                        "writeLong(long)*", "writeObject(Object)*", "writeShort(short)*", "writeUTF(String)*"), null), null));
                QualifiedMethodFilter streamMessageFilter = new QualifiedMethodFilter(new ClassFilter(
                        new ClassNameFilter("javax.jms.StreamMessage"), true, null), new MemberFilter(new MemberNameFilter(Arrays.asList(
                        "writeBoolean(boolean)*", "writeByte(byte)*", "writeBytes(byte[])*", "writeBytes(byte[],int,int)*",
                        "writeChar(char)*", "writeDouble(double)*", "writeFloat(float)*", "writeInt(int)*",
                        "writeLong(long)*", "writeObject(Object)*", "writeShort(short)*", "writeString(String)*"), null), null));
                methodFilter = new QualifiedMethodFilter(Arrays.asList(bytesMessageFilter, streamMessageFilter), null);

                pointcut = new InterceptPointcut(name + "2", methodFilter, Enums.of(Kind.ENTER, Kind.RETURN_EXIT, Kind.THROW_EXIT),
                        new StaticInterceptorConfiguration(JmsProducerProbeInterceptor.class), true, false,
                        ExitPointProbeConfiguration.POINTCUT_PRIORITY);
                instrumentationContext.addPointcut(pointcut);
            }

            return new JmsProducerProbeConfiguration(name, scopeType, measurementStrategy, warmupDelay,
                    bytesCounter);
        } else if (type.equals("FileProbe")) {
            String scopeType = element.get("scopeType");
            String measurementStrategy = element.get("measurementStrategy", null);
            long warmupDelay = element.get("warmupDelay");
            CounterConfiguration readTimeCounter = load(null, "Counter", (JsonObject) element.get("readTimeCounter"), context);
            CounterConfiguration readBytesCounter = load(null, "Counter", (JsonObject) element.get("readBytesCounter"), context);
            CounterConfiguration writeTimeCounter = load(null, "Counter", (JsonObject) element.get("writeTimeCounter"), context);
            CounterConfiguration writeBytesCounter = load(null, "Counter", (JsonObject) element.get("writeBytesCounter"), context);

            IInstrumentationLoadContext instrumentationContext = context.get(InstrumentationConfiguration.SCHEMA);
            QualifiedMethodFilter inputStreamFilter = new QualifiedMethodFilter(new ClassFilter("java.io.FileInputStream"),
                    new MemberFilter(new MemberNameFilter(Arrays.asList("read()*", "read(byte[],int,int)*", "read(byte[])*"), null), null));

            QualifiedMethodFilter outputStreamFilter = new QualifiedMethodFilter(new ClassFilter("java.io.FileOutputStream"),
                    new MemberFilter(new MemberNameFilter(Arrays.asList("write(int)*", "write(byte[],int,int)*", "write(byte[])*"), null), null));
            QualifiedMethodFilter channelFilter = new QualifiedMethodFilter(new ClassFilter("sun.nio.ch.FileChannelImpl"),
                    new MemberFilter(new MemberNameFilter(Arrays.asList("read(ByteBuffer)*",
                            "read(ByteBuffer[],int,int)*", "readInternal(ByteBuffer,long)*",
                            "write(ByteBuffer)*", "write(ByteBuffer[],int,int)*", "writeInternal(ByteBuffer,long)*"), null), null));

            QualifiedMethodFilter randomAccessFileFilter = new QualifiedMethodFilter(new ClassFilter("java.io.RandomAccessFile"),
                    new MemberFilter(new MemberNameFilter(Arrays.asList("read()*", "readBytes(byte[],int,int)*", "write(int)*",
                            "writeBytes(byte[],int,int)*"), null), null));
            QualifiedMethodFilter methodFilter = new QualifiedMethodFilter(Arrays.asList(inputStreamFilter, outputStreamFilter,
                    channelFilter, randomAccessFileFilter), null);

            InterceptPointcut pointcut = new InterceptPointcut(name, methodFilter, Enums.of(Kind.ENTER, Kind.RETURN_EXIT, Kind.THROW_EXIT),
                    new StaticInterceptorConfiguration(FileProbeInterceptor.class), true, false, ExitPointProbeConfiguration.POINTCUT_PRIORITY);
            instrumentationContext.addPointcut(pointcut);

            return new FileProbeConfiguration(name, scopeType, measurementStrategy, warmupDelay,
                    readTimeCounter, readBytesCounter, writeTimeCounter, writeBytesCounter);
        } else if (type.equals("TcpProbe")) {
            String scopeType = element.get("scopeType");
            String measurementStrategy = element.get("measurementStrategy", null);
            long warmupDelay = element.get("warmupDelay");
            CounterConfiguration connectTimeCounter = load(null, "Counter", (JsonObject) element.get("connectTimeCounter"), context);
            CounterConfiguration receiveTimeCounter = load(null, "Counter", (JsonObject) element.get("receiveTimeCounter"), context);
            CounterConfiguration receiveBytesCounter = load(null, "Counter", (JsonObject) element.get("receiveBytesCounter"), context);
            CounterConfiguration sendTimeCounter = load(null, "Counter", (JsonObject) element.get("sendTimeCounter"), context);
            CounterConfiguration sendBytesCounter = load(null, "Counter", (JsonObject) element.get("sendBytesCounter"), context);

            IInstrumentationLoadContext instrumentationContext = context.get(InstrumentationConfiguration.SCHEMA);
            QualifiedMethodFilter inputStreamFilter = new QualifiedMethodFilter(new ClassFilter("java.net.SocketInputStream"),
                    new MemberFilter("read(byte[],int,int,int)*"));
            QualifiedMethodFilter inputStreamFilter2 = new QualifiedMethodFilter(new ClassFilter("sun.nio.ch.SocketAdaptor$SocketInputStream"),
                    new MemberFilter("read(ByteBuffer)*"));
            QualifiedMethodFilter outputStreamFilter = new QualifiedMethodFilter(new ClassFilter("java.net.SocketOutputStream"),
                    new MemberFilter("socketWrite(byte[],int,int)*"));
            QualifiedMethodFilter channelFilter = new QualifiedMethodFilter(new ClassFilter("sun.nio.ch.SocketChannelImpl"),
                    new MemberFilter(new MemberNameFilter(Arrays.asList("read(ByteBuffer)*", "write(ByteBuffer)*",
                            "read(ByteBuffer[],int,int)*", "write(ByteBuffer[],int,int)*",
                            "connect(SocketAddress)*"), null), null));
            QualifiedMethodFilter socketFilter = new QualifiedMethodFilter(new ClassFilter("java.net.Socket"),
                    new MemberFilter("connect(SocketAddress,int)*"));
            QualifiedMethodFilter methodFilter = new QualifiedMethodFilter(Arrays.asList(inputStreamFilter, inputStreamFilter2,
                    outputStreamFilter, channelFilter, socketFilter), null);

            InterceptPointcut pointcut = new InterceptPointcut(name, methodFilter, Enums.of(Kind.ENTER, Kind.RETURN_EXIT, Kind.THROW_EXIT),
                    new StaticInterceptorConfiguration(TcpProbeInterceptor.class), true, false, ExitPointProbeConfiguration.POINTCUT_PRIORITY);
            instrumentationContext.addPointcut(pointcut);

            return new TcpProbeConfiguration(name, scopeType, measurementStrategy, warmupDelay,
                    connectTimeCounter, receiveTimeCounter, receiveBytesCounter, sendTimeCounter, sendBytesCounter);
        } else if (type.equals("UdpProbe")) {
            String scopeType = element.get("scopeType");
            String measurementStrategy = element.get("measurementStrategy", null);
            long warmupDelay = element.get("warmupDelay");
            CounterConfiguration receiveTimeCounter = load(null, "Counter", (JsonObject) element.get("receiveTimeCounter"),
                    context);
            CounterConfiguration receiveBytesCounter = load(null, "Counter", (JsonObject) element.get("receiveBytesCounter"),
                    context);
            CounterConfiguration sendTimeCounter = load(null, "Counter", (JsonObject) element.get("sendTimeCounter"),
                    context);
            CounterConfiguration sendBytesCounter = load(null, "Counter", (JsonObject) element.get("sendBytesCounter"),
                    context);

            IInstrumentationLoadContext instrumentationContext = context.get(InstrumentationConfiguration.SCHEMA);
            QualifiedMethodFilter socketFilter = new QualifiedMethodFilter(new ClassFilter(
                    new ClassNameFilter("java.net.DatagramSocket"), true, null),
                    new MemberFilter(new MemberNameFilter(Arrays.asList("send(DatagramPacket)*",
                            "receive(DatagramPacket)*"), null), null));
            QualifiedMethodFilter channelFilter = new QualifiedMethodFilter(new ClassFilter(
                    new ClassNameFilter("java.nio.channels.DatagramChannel"), true, null),
                    new MemberFilter(new MemberNameFilter(Arrays.asList("send(ByteBuffer,SocketAddress)*",
                            "receive(ByteBuffer)*", "read(ByteBuffer)*",
                            "read(ByteBuffer[],int,int)*", "write(ByteBuffer)*", "write(ByteBuffer[],int,int)*"), null), null));

            QualifiedMethodFilter methodFilter = new QualifiedMethodFilter(Arrays.asList(channelFilter, socketFilter), null);

            InterceptPointcut pointcut = new InterceptPointcut(name, methodFilter, Enums.of(Kind.ENTER, Kind.RETURN_EXIT, Kind.THROW_EXIT),
                    new StaticInterceptorConfiguration(UdpProbeInterceptor.class), true, false, ExitPointProbeConfiguration.POINTCUT_PRIORITY);
            instrumentationContext.addPointcut(pointcut);

            return new UdpProbeConfiguration(name, scopeType, measurementStrategy, warmupDelay,
                    receiveTimeCounter, receiveBytesCounter, sendTimeCounter, sendBytesCounter);
        } else if (type.equals("JdbcProbe")) {
            String scopeType = element.get("scopeType");
            String measurementStrategy = element.get("measurementStrategy", null);
            long warmupDelay = element.get("warmupDelay");
            RequestMappingStrategyConfiguration requestMappingStrategy = load(null, null, (JsonObject) element.get("requestMappingStrategy"),
                    context);
            CounterConfiguration queryTimeCounter = load(null, "Counter", (JsonObject) element.get("queryTimeCounter"),
                    context);

            IInstrumentationLoadContext instrumentationContext = context.get(InstrumentationConfiguration.SCHEMA);
            QualifiedMethodFilter connectionFilter = new QualifiedMethodFilter(new ClassFilter(new ClassNameFilter("java.sql.Connection"), true, null),
                    new MemberFilter(new MemberNameFilter(Arrays.asList("prepareCall(String*):CallableStatement",
                            "prepareStatement(String*):PreparedStatement"), null), null));
            QualifiedMethodFilter statementFilter = new QualifiedMethodFilter(new ClassFilter(new ClassNameFilter("java.sql.Statement"), true, null),
                    new MemberFilter(new MemberNameFilter(Arrays.asList("addBatch(String)*", "clearBatch()*",
                            "close()*", "execute(String*", "executeQuery(String)*", "executeBatch()*", "executeLargeBatch()*", "executeUpdate(String*",
                            "executeLargeUpdate(String*"), null), null));
            QualifiedMethodFilter preparedStatementFilter = new QualifiedMethodFilter(new ClassFilter(new ClassNameFilter("java.sql.PreparedStatement"), true, null),
                    new MemberFilter(new MemberNameFilter(Arrays.asList("setBigDecimal(int,BigDecimal)*", "setBoolean(int,boolean)*",
                            "setByte(int,byte)*", "setDate(int,Date*", "setDouble(int,double)*", "setFloat(int,float)*",
                            "setInt(int,int)*", "setLong(int,long)*", "setNString(int,String)*", "setNull(int*",
                            "setObject(int,Object*", "setRef(int,Ref)*", "setRowId(int,RowId)*",
                            "setShort(int,short)*", "setString(int,String)*", "setTime(int,Time*",
                            "setTimestamp(int,Timestamp*", "setURL(int,URL)*",
                            "clearParameters()*", "addBatch()*",
                            "execute()*", "executeQuery()*", "executeUpdate()*", "executeLargeUpdate()*"), null), null));
            QualifiedMethodFilter callableStatementFilter = new QualifiedMethodFilter(new ClassFilter(new ClassNameFilter("java.sql.CallableStatement"), true, null),
                    new MemberFilter(new MemberNameFilter(Arrays.asList("setBigDecimal(String,BigDecimal)*",
                            "setBoolean(String,boolean)*", "setByte(String,byte)*", "setDate(String,Date*",
                            "setDouble(String,double)*", "setFloat(String,float)*", "setInt(String,int)*",
                            "setLong(String,long)*", "setNString(String,String)*",
                            "setNull(String*", "setObject(String,Object*", "setRowId(String,RowId)*",
                            "setShort(String,short)*", "setString(String,String)*", "setTime(String,Time*",
                            "setTimestamp(String,Timestamp*", "setURL(String,URL)*"), null), null));

            QualifiedMethodFilter methodFilter = new QualifiedMethodFilter(Arrays.asList(connectionFilter, statementFilter,
                    preparedStatementFilter, callableStatementFilter), null);

            InterceptPointcut pointcut = new InterceptPointcut(name, methodFilter, Enums.of(Kind.ENTER, Kind.RETURN_EXIT, Kind.THROW_EXIT),
                    new StaticInterceptorConfiguration(JdbcProbeInterceptor.class), true, false, ExitPointProbeConfiguration.POINTCUT_PRIORITY);
            instrumentationContext.addPointcut(pointcut);

            return new JdbcProbeConfiguration(name, scopeType, measurementStrategy, warmupDelay, requestMappingStrategy,
                    queryTimeCounter);
        } else if (type.equals("JdbcConnectionProbe")) {
            String scopeType = element.get("scopeType");
            String measurementStrategy = element.get("measurementStrategy", null);
            long warmupDelay = element.get("warmupDelay");
            RequestMappingStrategyConfiguration requestMappingStrategy = load(null, null, (JsonObject) element.get("requestMappingStrategy"),
                    context);
            CounterConfiguration connectTimeCounter = load(null, "Counter", (JsonObject) element.get("connectTimeCounter"),
                    context);

            IInstrumentationLoadContext instrumentationContext = context.get(InstrumentationConfiguration.SCHEMA);
            QualifiedMethodFilter methodFilter = new QualifiedMethodFilter(new ClassFilter(new ClassNameFilter("javax.sql.DataSource"), true, null),
                    new MemberFilter(Arrays.asList(new MemberFilter("getConnection(*):Connection")), null));

            InterceptPointcut pointcut = new InterceptPointcut(name, methodFilter, Enums.of(Kind.ENTER, Kind.RETURN_EXIT, Kind.THROW_EXIT),
                    new StaticInterceptorConfiguration(JdbcConnectionProbeInterceptor.class), true, false, ExitPointProbeConfiguration.POINTCUT_PRIORITY);
            instrumentationContext.addPointcut(pointcut);

            return new JdbcConnectionProbeConfiguration(name, scopeType, measurementStrategy, warmupDelay,
                    requestMappingStrategy, connectTimeCounter);
        } else if (type.equals("UrlRequestGroupingStrategy"))
            return new UrlRequestGroupingStrategyConfiguration();
        else if (type.equals("JdbcRequestGroupingStrategy"))
            return new JdbcRequestGroupingStrategyConfiguration();
        else
            throw new InvalidConfigurationException();
    }

    private List<JmxAttributeConfiguration> loadJmxAttributes(JsonArray array, ILoadContext context) {
        List<JmxAttributeConfiguration> attributes = new ArrayList<JmxAttributeConfiguration>();
        for (Object e : array) {
            JsonObject element = (JsonObject) e;
            String metricType = element.get("metricType");
            MeterConfiguration meter = load(null, null, (JsonObject) element.get("meter"), context);
            String attribute = element.get("attribute");
            String converterExpression = element.get("converterExpression", null);
            attributes.add(new JmxAttributeConfiguration(metricType, meter, attribute, converterExpression));
        }

        return attributes;
    }

    private GcFilterConfiguration loadFilter(JsonObject element) {
        if (element == null)
            return null;

        long minDuration = element.get("minDuration");
        long minBytes = element.get("minBytes");

        return new GcFilterConfiguration(minDuration, minBytes);
    }
}