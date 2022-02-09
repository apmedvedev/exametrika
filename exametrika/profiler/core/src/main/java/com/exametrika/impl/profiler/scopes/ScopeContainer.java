/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.profiler.scopes;

import java.util.HashMap;
import java.util.Map;

import com.exametrika.api.profiler.config.ProfilerConfiguration;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.SimpleList;
import com.exametrika.impl.profiler.scopes.ThreadLocalAccessor.Container;


/**
 * The {@link ScopeContainer} is a thread container of scopes.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ScopeContainer {
    private final Container parent;
    private final ScopeContext context;
    private final Map<String, Scope> scopesMap = new HashMap<String, Scope>();
    private final SimpleList<Scope> scopes = new SimpleList<Scope>();
    private final Scope[] activeScopes;
    private final Scope[] prevScopes;

    public ScopeContainer(Container parent, ScopeContext context) {
        Assert.notNull(parent);
        Assert.notNull(context);

        this.parent = parent;
        this.context = context;
        this.activeScopes = new Scope[ProfilerConfiguration.MAX_SCOPE_TYPE_COUNT];
        this.prevScopes = new Scope[ProfilerConfiguration.MAX_SCOPE_TYPE_COUNT];
    }

    public void init(boolean system) {
        context.createPermanentScopes(this, scopes, activeScopes, system);
    }

    public Container getParent() {
        return parent;
    }

    public ScopeContext getContext() {
        return context;
    }

    public boolean hasScopes() {
        return !scopesMap.isEmpty();
    }

    public Scope get(String name, String type) {
        Scope scope = scopesMap.get(context.getScopeName(name));
        if (scope != null)
            return scope;

        return create(name, type);
    }

    public Scope createLocal(String name, String type, String entryPointComponentType) {
        return context.createScope(name, type, this, true, entryPointComponentType);
    }

    public void activateAll() {
        for (int i = 0; i < activeScopes.length; i++) {
            Scope scope = prevScopes[i];
            if (scope == null)
                continue;

            activeScopes[i] = scope;
            scope.activate();
            prevScopes[i] = null;
        }
    }

    public void deactivateAll() {
        for (int i = 0; i < activeScopes.length; i++) {
            Scope scope = activeScopes[i];
            if (scope == null)
                continue;

            scope.deactivate();
            prevScopes[i] = scope;
            activeScopes[i] = null;
        }
    }

    public void activate(Scope scope) {
        int slotIndex = scope.getSlotIndex();
        Scope previousScope = activeScopes[slotIndex];
        if (previousScope != null) {
            previousScope.deactivate();
            scope.setPreviousScope(previousScope);
        }

        activeScopes[slotIndex] = scope;
    }

    public void deactivate(Scope scope) {
        int slotIndex = scope.getSlotIndex();
        Scope previousScope = scope.getPreviousScope();
        activeScopes[slotIndex] = previousScope;
        if (previousScope != null) {
            scope.setPreviousScope(null);
            previousScope.activate();
        }
    }

    public boolean isExtractionRequired() {
        for (Scope scope : scopes.values()) {
            if (scope.isExtractionRequired() || scope.isIdle())
                return true;
        }

        return false;
    }

    public void extract() {
        for (Scope scope : scopes.values()) {
            if (scope.isIdle()) {
                if (scope.isActive())
                    scope.end();

                scope.getElement().remove();
                scopesMap.remove(scope.getName().toString());
            } else
                scope.extract();
        }
    }

    public JsonObject dump(int flags) {
        JsonObjectBuilder builder = new JsonObjectBuilder();
        for (Scope scope : scopes.values())
            builder.put(scope.getName().toString(), scope.dump(flags));

        return builder.toJson();
    }

    private Scope create(String name, String type) {
        Scope scope = context.createScope(name, type, this, false, null);
        scopesMap.put(scope.getName().toString(), scope);
        scopes.addLast(scope.getElement());

        return scope;
    }
}
