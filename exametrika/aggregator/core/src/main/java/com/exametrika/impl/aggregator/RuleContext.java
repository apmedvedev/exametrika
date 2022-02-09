/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.aggregator;

import gnu.trove.map.TLongObjectMap;
import gnu.trove.map.hash.TLongObjectHashMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Immutables;
import com.exametrika.spi.aggregator.IRuleContext;
import com.exametrika.spi.aggregator.IRuleExecutor;


/**
 * The {@link RuleContext} is a rule context.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class RuleContext implements IRuleContext {
    private TLongObjectMap<RuleExecutorInfo> executors;

    public static class RuleExecutorInfo {
        private final IRuleExecutor executor;
        private final Map<String, Object> facts = new HashMap<String, Object>();
        private final Map<String, Object> immutableFacts = Immutables.wrap(facts);

        public RuleExecutorInfo(IRuleExecutor executor) {
            Assert.notNull(executor);

            this.executor = executor;
        }

        public IRuleExecutor getExecutor() {
            return executor;
        }

        public Map<String, Object> getFacts() {
            return immutableFacts;
        }
    }

    public TLongObjectMap<RuleExecutorInfo> getExecutors() {
        return executors;
    }

    @Override
    public Map<String, Object> getFacts(IRuleExecutor ruleExecutor) {
        Assert.notNull(ruleExecutor);

        if (executors == null)
            return Collections.emptyMap();

        RuleExecutorInfo info = executors.get(ruleExecutor.getScopeId());
        if (info == null)
            return Collections.emptyMap();

        return info.immutableFacts;
    }

    @Override
    public Object getFact(IRuleExecutor ruleExecutor, String name) {
        Assert.notNull(ruleExecutor);
        Assert.notNull(name);

        return getFacts(ruleExecutor).get(name);
    }

    @Override
    public void setFact(IRuleExecutor ruleExecutor, String name, Object value) {
        Assert.notNull(ruleExecutor);
        Assert.notNull(name);
        Assert.notNull(value);

        RuleExecutorInfo info = ensureFacts(ruleExecutor);
        info.facts.put(name, value);
    }

    @Override
    public void addFact(IRuleExecutor ruleExecutor, String name, Object value) {
        Assert.notNull(ruleExecutor);
        Assert.notNull(name);
        Assert.notNull(value);

        RuleExecutorInfo info = ensureFacts(ruleExecutor);
        List<Object> fact = (List<Object>) info.facts.get(name);
        if (fact == null) {
            fact = new ArrayList<Object>();
            info.facts.put(name, fact);
        }

        fact.add(value);
    }

    @Override
    public void incrementFact(IRuleExecutor ruleExecutor, String name) {
        Assert.notNull(ruleExecutor);
        Assert.notNull(name);

        RuleExecutorInfo info = ensureFacts(ruleExecutor);
        Integer fact = (Integer) info.facts.get(name);
        if (fact == null)
            fact = 0;

        fact++;

        info.facts.put(name, fact);
    }

    private RuleExecutorInfo ensureFacts(IRuleExecutor ruleExecutor) {
        Assert.notNull(ruleExecutor);

        if (executors == null)
            executors = new TLongObjectHashMap<RuleExecutorInfo>();

        RuleExecutorInfo info = executors.get(ruleExecutor.getScopeId());
        if (info == null) {
            info = new RuleExecutorInfo(ruleExecutor);
            executors.put(ruleExecutor.getScopeId(), info);
        }
        return info;
    }
}
