/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.metrics.exa.server.config;

import java.util.List;
import java.util.Set;

import com.exametrika.api.exadb.jobs.config.model.JobSchemaConfiguration;
import com.exametrika.api.metrics.exa.server.config.AllExaAgentsSelectorSchemaConfiguration;
import com.exametrika.api.metrics.exa.server.config.ExaAgentSelectorSchemaConfiguration;
import com.exametrika.api.metrics.exa.server.config.ExaServerSelectorSchemaConfiguration;
import com.exametrika.api.metrics.exa.server.config.model.ExaAgentComponentSchemaConfiguration;
import com.exametrika.api.metrics.exa.server.config.model.ExaAgentDiscoveryStrategySchemaConfiguration;
import com.exametrika.api.metrics.exa.server.config.model.ExaServerComponentSchemaConfiguration;
import com.exametrika.api.metrics.exa.server.config.model.ExaServerDiscoveryStrategySchemaConfiguration;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.config.InvalidConfigurationException;
import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonObject;
import com.exametrika.impl.component.config.schema.ComponentSchemaLoader;
import com.exametrika.spi.component.config.model.ActionSchemaConfiguration;
import com.exametrika.spi.component.config.model.AlertSchemaConfiguration;
import com.exametrika.spi.component.config.model.GroupDiscoveryStrategySchemaConfiguration;
import com.exametrika.spi.component.config.model.RuleSchemaConfiguration;
import com.exametrika.spi.component.config.model.SelectorSchemaConfiguration;


/**
 * The {@link ExaServerMetricsSchemaLoader} is a loader of perfdb schemas.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ExaServerMetricsSchemaLoader extends ComponentSchemaLoader {
    @Override
    public Object loadExtension(String name, String type, Object object, ILoadContext context) {
        JsonObject element = (JsonObject) object;
        if (type.equals("ExaServerSelector"))
            return new ExaServerSelectorSchemaConfiguration(name);
        else if (type.equals("ExaAgentSelector"))
            return new ExaAgentSelectorSchemaConfiguration(name);
        else if (type.equals("AllExaAgentsSelector"))
            return new AllExaAgentsSelectorSchemaConfiguration(name);
        else if (type.equals("ExaAgentDiscoveryStrategy"))
            return new ExaAgentDiscoveryStrategySchemaConfiguration((String) element.get("component"));
        else if (type.equals("ExaServerDiscoveryStrategy"))
            return new ExaServerDiscoveryStrategySchemaConfiguration((String) element.get("component"));
        else if (type.equals("exaAgent")) {
            Set<ActionSchemaConfiguration> actions = loadActions(element, context);
            Set<RuleSchemaConfiguration> rules = loadRules((JsonObject) element.get("rules"), context);
            Set<JobSchemaConfiguration> jobs = load(null, "Jobs", element.get("jobs"), context);
            Set<SelectorSchemaConfiguration> selectors = loadSelectors(element, context);
            Set<AlertSchemaConfiguration> alerts = loadAlerts((JsonObject) element.get("alerts"), context);
            List<GroupDiscoveryStrategySchemaConfiguration> groupDiscoveryStrategies = loadGroupDiscoveryStrategies(
                    (JsonArray) element.get("groupDiscoveryStrategies"), context);
            String healthComponentType = element.get("healthComponentType");

            return new ExaAgentComponentSchemaConfiguration(name, actions, rules, selectors, alerts, groupDiscoveryStrategies, jobs,
                    healthComponentType);
        } else if (type.equals("exaServer")) {
            Set<ActionSchemaConfiguration> actions = loadActions(element, context);
            Set<RuleSchemaConfiguration> rules = loadRules((JsonObject) element.get("rules"), context);
            Set<JobSchemaConfiguration> jobs = load(null, "Jobs", element.get("jobs"), context);
            Set<SelectorSchemaConfiguration> selectors = loadSelectors(element, context);
            Set<AlertSchemaConfiguration> alerts = loadAlerts((JsonObject) element.get("alerts"), context);
            List<GroupDiscoveryStrategySchemaConfiguration> groupDiscoveryStrategies = loadGroupDiscoveryStrategies(
                    (JsonArray) element.get("groupDiscoveryStrategies"), context);
            String healthComponentType = element.get("healthComponentType");

            return new ExaServerComponentSchemaConfiguration(name, actions, rules, selectors, alerts, groupDiscoveryStrategies, jobs,
                    healthComponentType);
        } else
            throw new InvalidConfigurationException();
    }
}