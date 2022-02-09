/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.probes;

import java.util.Map;

import com.exametrika.api.profiler.config.SimpleRequestMappingStrategyConfiguration;
import com.exametrika.common.expression.CompileContext;
import com.exametrika.common.expression.Expressions;
import com.exametrika.common.expression.IExpression;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.IRequest;
import com.exametrika.spi.profiler.IRequestMappingStrategy;
import com.exametrika.spi.profiler.IScope;
import com.exametrika.spi.profiler.Probes;
import com.exametrika.spi.profiler.Request;


/**
 * The {@link SimpleRequestMappingStrategy} is a simple request mapping strategy.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class SimpleRequestMappingStrategy implements IRequestMappingStrategy {
    private final IExpression nameExpression;
    private final IExpression metadataExpression;
    private final IExpression parametersExpression;
    private final IExpression requestFilter;
    protected final Map<String, Object> runtimeContext;
    protected final IProbeContext context;
    private final SimpleRequestMappingStrategyConfiguration configuration;

    public SimpleRequestMappingStrategy(SimpleRequestMappingStrategyConfiguration configuration, IProbeContext context) {
        Assert.notNull(configuration);
        Assert.notNull(context);

        CompileContext compileContext = Expressions.createCompileContext(null);
        runtimeContext = Probes.createRuntimeContext(context);

        nameExpression = Expressions.compile(configuration.getNameExpression(), compileContext);
        metadataExpression = Expressions.compile(configuration.getMetadataExpression(), compileContext);
        parametersExpression = Expressions.compile(configuration.getParametersExpression(), compileContext);

        if (configuration.getRequestFilter() != null)
            requestFilter = Expressions.compile(configuration.getRequestFilter(), compileContext);
        else
            requestFilter = null;

        this.context = context;
        this.configuration = configuration;
    }

    @Override
    public void onTimer(long currentTime) {
    }

    @Override
    public SimpleRequest begin(IScope scope, Object request) {
        if (!filterRequest(request))
            return null;

        String name = getRequestName(request);
        if (name == null)
            name = "<unknown>";

        return createRequest(scope, request, name);
    }

    @Override
    public IRequest get(String name, int variant, Object request) {
        return new SimpleRequest(name, request);
    }

    protected SimpleRequest createRequest(IScope scope, Object request, String name) {
        return new SimpleRequest(name, request);
    }

    private boolean filterRequest(Object request) {
        if (configuration.getRequestFilter() == null)
            return true;

        return requestFilter.execute(request, runtimeContext);
    }

    private String getRequestName(Object request) {
        return nameExpression.execute(request, runtimeContext);
    }

    private Map getRequestMetadata(Object request) {
        return metadataExpression.execute(request, runtimeContext);
    }

    private Map getRequestParameters(Object request) {
        return parametersExpression.execute(request, runtimeContext);
    }

    protected class SimpleRequest extends Request {
        private JsonObject metadata;
        private JsonObject parameters;

        public SimpleRequest(String name, Object request) {
            super(name, request);
        }

        @Override
        public JsonObject getMetadata() {
            if (metadata == null)
                metadata = JsonUtils.toJson(getRequestMetadata(getMetadataRequest()));

            return metadata;
        }

        @Override
        public JsonObject getParameters() {
            if (parameters == null)
                parameters = JsonUtils.toJson(getRequestParameters(getRawRequest()));

            return parameters;
        }

        @Override
        public void end() {
            parameters = null;
        }

        protected Object getMetadataRequest() {
            return getRawRequest();
        }
    }
}
