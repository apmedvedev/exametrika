/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.jvm.monitors;

import java.lang.management.ClassLoadingMXBean;
import java.lang.management.CompilationMXBean;
import java.lang.management.ManagementFactory;

import com.exametrika.spi.aggregator.common.meters.IMeasurementProvider;
import com.exametrika.spi.profiler.AbstractMonitor;
import com.exametrika.spi.profiler.IMonitorContext;
import com.exametrika.spi.profiler.config.MonitorConfiguration;


/**
 * The {@link JvmCodeMonitor} is a monitor of code statistics of JVM.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class JvmCodeMonitor extends AbstractMonitor {
    public JvmCodeMonitor(MonitorConfiguration configuration, IMonitorContext context) {
        super("jvm.code", configuration, context, false);
    }

    @Override
    protected void createMeters() {
        final ClassLoadingMXBean classLoading = ManagementFactory.getClassLoadingMXBean();

        meters.addGauge("jvm.code.loadedClasses", new IMeasurementProvider() {
            @Override
            public Object getValue() {
                return classLoading.getTotalLoadedClassCount();
            }
        });

        meters.addGauge("jvm.code.unloadedClasses", new IMeasurementProvider() {
            @Override
            public Object getValue() {
                return classLoading.getUnloadedClassCount();
            }
        });

        meters.addGauge("jvm.code.currentClasses", new IMeasurementProvider() {
            @Override
            public Object getValue() {
                return classLoading.getLoadedClassCount();
            }
        });

        final CompilationMXBean compilation = ManagementFactory.getCompilationMXBean();

        meters.addCounter("host.process.time", false, 0, new IMeasurementProvider() {
            @Override
            public Object getValue() {
                return System.nanoTime() / 1000000;
            }
        });

        meters.addCounter("jvm.code.compilationTime", false, 0, new IMeasurementProvider() {
            @Override
            public Object getValue() {
                return compilation.getTotalCompilationTime();
            }
        });
    }
}
