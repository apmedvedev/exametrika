/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.bridge;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.exametrika.common.services.Services;
import com.exametrika.common.services.config.ServiceProviderConfiguration;
import com.exametrika.common.time.ITimeService;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.boot.utils.PathClassLoader;
import com.exametrika.spi.profiler.boot.IBridge;


/**
 * The {@link BridgeHolder} is a bridge holder.
 *
 * @param <T> bridge interface type
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class BridgeHolder<T extends IBridge> {
    private final Class<T> bridgeType;
    private final Set<String> baseTypes;
    private final long bridgeIdleTime;
    private final ITimeService timeService;
    private volatile ArrayList<BridgeInfo> bridges = new ArrayList<BridgeInfo>();

    public BridgeHolder(Class<T> bridgeType, Set<String> baseTypes, long bridgeIdleTime, ITimeService timeService) {
        Assert.notNull(bridgeType);
        Assert.notNull(baseTypes);
        Assert.notNull(timeService);

        this.bridgeType = bridgeType;
        this.baseTypes = baseTypes;
        this.bridgeIdleTime = bridgeIdleTime;
        this.timeService = timeService;
    }

    public T get(Object request) {
        Assert.notNull(request);

        T bridge = find(request);
        if (bridge != null)
            return bridge;

        return create(request);
    }

    public synchronized void onTimer() {
        long currentTime = timeService.getCurrentTime();

        Set<Integer> indexes = null;
        for (int i = 0; i < bridges.size(); i++) {
            BridgeInfo info = bridges.get(i);
            if (info.accessed) {
                info.accessed = false;
                info.lastAccessTime = currentTime;
            } else if (currentTime - info.lastAccessTime > bridgeIdleTime) {
                if (indexes == null)
                    indexes = new HashSet<Integer>();
                indexes.add(i);
            }
        }

        if (indexes != null) {
            ArrayList<BridgeInfo> bridges = new ArrayList<BridgeInfo>();
            for (int i = 0; i < this.bridges.size(); i++) {
                if (!indexes.contains(i))
                    bridges.add(this.bridges.get(i));
            }

            this.bridges = bridges;
        }
    }

    private T find(Object request) {
        List<BridgeInfo> bridges = this.bridges;
        for (int i = 0; i < bridges.size(); i++) {
            BridgeInfo info = bridges.get(i);
            if (info.bridge.supports(request)) {
                info.accessed = true;
                return (T) info.bridge;
            }
        }

        return null;
    }

    private synchronized T create(Object request) {
        T bridge = find(request);
        if (bridge != null)
            return bridge;

        Class baseClass = null;
        for (String baseType : baseTypes) {
            baseClass = getBaseClass(request.getClass(), baseType);
            if (baseClass != null)
                break;
        }
        Assert.notNull(baseClass);

        BridgeClassLoader classLoader = new BridgeClassLoader((PathClassLoader) getClass().getClassLoader(), baseClass.getClassLoader());
        List<ServiceProviderConfiguration> providerConfigurations = Services.loadProviderConfigurations(bridgeType, getClass().getClassLoader());
        Assert.isTrue(providerConfigurations.size() == 1);

        bridge = providerConfigurations.get(0).createInstance(bridgeType, classLoader, null);
        Assert.notNull(bridge);
        Assert.isTrue(bridge.supports(request));

        ArrayList<BridgeInfo> bridges = (ArrayList<BridgeInfo>) this.bridges.clone();
        BridgeInfo info = new BridgeInfo();
        info.bridge = bridge;
        info.accessed = true;

        bridges.add(info);
        this.bridges = bridges;
        return bridge;
    }

    private static Class getBaseClass(Class clazz, String className) {
        if (clazz.getName().equals(className))
            return clazz;

        Class superClass = clazz.getSuperclass();
        if (superClass != null) {
            Class base = getBaseClass(superClass, className);
            if (base != null)
                return base;
        }

        Class[] interfaces = clazz.getInterfaces();
        for (int i = 0; i < interfaces.length; i++) {
            Class base = getBaseClass(interfaces[i], className);
            if (base != null)
                return base;
        }

        return null;
    }

    private static class BridgeInfo {
        IBridge bridge;
        boolean accessed;
        long lastAccessTime;
    }
}
