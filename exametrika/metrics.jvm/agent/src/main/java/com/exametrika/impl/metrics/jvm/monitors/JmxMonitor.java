/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.jvm.monitors;

import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

import com.exametrika.api.metrics.jvm.config.JmxAttributeConfiguration;
import com.exametrika.api.metrics.jvm.config.JmxMonitorConfiguration;
import com.exametrika.common.expression.CompileContext;
import com.exametrika.common.expression.Expressions;
import com.exametrika.common.expression.IExpression;
import com.exametrika.common.l10n.SystemException;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.spi.aggregator.common.meters.IMeasurementProvider;
import com.exametrika.spi.aggregator.common.meters.MeterExpressions;
import com.exametrika.spi.profiler.AbstractMonitor;
import com.exametrika.spi.profiler.IMonitorContext;


/**
 * The {@link JmxMonitor} is a monitor of JMX attributes.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class JmxMonitor extends AbstractMonitor {
    private final JmxMonitorConfiguration configuration;
    private final ObjectName name;
    private final List<IExpression> converterExpressions = new ArrayList<IExpression>();
    private final Map<String, Object> runtimeContext;

    public JmxMonitor(JmxMonitorConfiguration configuration, IMonitorContext context) {
        super(configuration.getComponentType(), configuration, context, false);

        this.configuration = configuration;
        runtimeContext = MeterExpressions.getRuntimeContext();
        CompileContext compileContext = Expressions.createCompileContext(null);

        for (JmxAttributeConfiguration attribute : configuration.getAttributes()) {
            if (attribute.getConverterExpression() != null)
                converterExpressions.add(Expressions.compile(attribute.getConverterExpression(), compileContext));
            else
                converterExpressions.add(null);
        }

        try {
            this.name = ObjectName.getInstance(configuration.getObject());
        } catch (MalformedObjectNameException e) {
            throw new SystemException(e);
        }
    }

    @Override
    protected void createMeters() {
        for (int i = 0; i < configuration.getAttributes().size(); i++) {
            final JmxAttributeConfiguration attribute = configuration.getAttributes().get(i);
            final int index = i;
            meters.addMeter(attribute.getMetricType(), attribute.getMeter(), new IMeasurementProvider() {
                @Override
                public Object getValue() {
                    try {
                        Object value = ManagementFactory.getPlatformMBeanServer().getAttribute(name, attribute.getAttribute());
                        if (converterExpressions.get(index) != null)
                            value = converterExpressions.get(index).execute(value, runtimeContext);

                        return value;
                    } catch (Exception e) {
                        return Exceptions.wrapAndThrow(e);
                    }
                }
            });
        }
    }
}
