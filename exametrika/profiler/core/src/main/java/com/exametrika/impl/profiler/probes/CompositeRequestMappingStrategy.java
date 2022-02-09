/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.probes;

import java.util.ArrayList;
import java.util.List;

import com.exametrika.api.profiler.config.CompositeRequestMappingStrategyConfiguration;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.profiler.IDumpProvider;
import com.exametrika.spi.profiler.IProbeContext;
import com.exametrika.spi.profiler.IRequest;
import com.exametrika.spi.profiler.IRequestMappingStrategy;
import com.exametrika.spi.profiler.IScope;
import com.exametrika.spi.profiler.IThreadLocalProvider;
import com.exametrika.spi.profiler.IThreadLocalProviderRegistrar;
import com.exametrika.spi.profiler.IThreadLocalProviderRegistry;
import com.exametrika.spi.profiler.Request;
import com.exametrika.spi.profiler.config.RequestMappingStrategyConfiguration;


/**
 * The {@link CompositeRequestMappingStrategy} is a composite request mapping strategy.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class CompositeRequestMappingStrategy implements IRequestMappingStrategy, IThreadLocalProviderRegistrar, IDumpProvider {
    private final List<IRequestMappingStrategy> strategies;

    public CompositeRequestMappingStrategy(CompositeRequestMappingStrategyConfiguration configuration, IProbeContext context) {
        Assert.notNull(configuration);
        Assert.notNull(context);

        List<IRequestMappingStrategy> strategies = new ArrayList<IRequestMappingStrategy>();
        for (RequestMappingStrategyConfiguration strategyConfiguration : configuration.getStrategies())
            strategies.add(strategyConfiguration.createStrategy(context));

        this.strategies = strategies;
    }

    @Override
    public void register(IThreadLocalProviderRegistry registry) {
        for (IRequestMappingStrategy strategy : strategies) {
            if (strategy instanceof IThreadLocalProviderRegistrar)
                ((IThreadLocalProviderRegistrar) strategy).register(registry);
            else if (strategy instanceof IThreadLocalProvider)
                registry.addProvider((IThreadLocalProvider) strategy);
        }
    }

    @Override
    public void onTimer(long currentTime) {
        for (IRequestMappingStrategy strategy : strategies)
            strategy.onTimer(currentTime);
    }

    @Override
    public IRequest begin(IScope scope, Object rawRequest) {
        List<IRequest> requests = new ArrayList<IRequest>(strategies.size());
        IRequest measuredRequest = null;
        int variant = 0;
        for (int i = 0; i < strategies.size(); i++) {
            IRequestMappingStrategy strategy = strategies.get(i);
            IRequest request = strategy.begin(scope, rawRequest);
            if (request == null)
                continue;

            requests.add(request);

            if (measuredRequest == null && request.canMeasure()) {
                measuredRequest = request;
                variant = i;
            }
        }

        if (requests.isEmpty())
            return null;
        else
            return new CompositeRequest(requests, measuredRequest, variant);
    }

    @Override
    public IRequest get(String name, int variant, Object request) {
        if (variant < strategies.size())
            return strategies.get(variant).get(name, 0, request);
        else
            return new Request(name, request);
    }

    @Override
    public String getName() {
        return getClass().getSimpleName();
    }

    @Override
    public JsonObject dump(int flags) {
        JsonObjectBuilder builder = new JsonObjectBuilder();
        for (IRequestMappingStrategy strategy : strategies) {
            if (!(strategy instanceof IDumpProvider))
                continue;

            IDumpProvider dumpProvider = (IDumpProvider) strategy;
            builder.put(dumpProvider.getName(), dumpProvider.dump(flags));
        }

        return builder.toJson();
    }

    private static class CompositeRequest implements IRequest {
        private final List<IRequest> requests;
        private final IRequest measuredRequest;
        private final int variant;

        public CompositeRequest(List<IRequest> requests, IRequest measuredRequest, int variant) {
            Assert.notNull(requests);

            this.requests = requests;
            this.measuredRequest = measuredRequest;
            this.variant = variant;
        }

        @Override
        public boolean canMeasure() {
            return measuredRequest != null;
        }

        @Override
        public String getName() {
            if (measuredRequest != null)
                return measuredRequest.getName();
            else
                return "";
        }

        @Override
        public int getVariant() {
            return variant;
        }

        @Override
        public JsonObject getMetadata() {
            if (measuredRequest != null)
                return measuredRequest.getMetadata();
            else
                return null;
        }

        @Override
        public JsonObject getParameters() {
            if (measuredRequest != null)
                return measuredRequest.getParameters();
            else
                return null;
        }

        @Override
        public Object getRawRequest() {
            if (measuredRequest != null)
                return measuredRequest.getRawRequest();
            else
                return null;
        }

        @Override
        public JsonObject getError() {
            if (measuredRequest != null)
                return measuredRequest.getError();
            else
                return null;
        }

        @Override
        public void setError(JsonObject value) {
            if (measuredRequest != null)
                measuredRequest.setError(value);
        }

        @Override
        public void end() {
            for (IRequest request : requests)
                request.end();
        }

        @Override
        public String toString() {
            if (measuredRequest != null)
                return measuredRequest.toString();
            else
                return "";
        }
    }
}
