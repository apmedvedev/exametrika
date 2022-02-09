/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.config.schema;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exametrika.api.aggregator.config.model.AggregationComponentTypeSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.AggregationSchemaConfiguration;
import com.exametrika.api.aggregator.config.model.PeriodTypeSchemaConfiguration;
import com.exametrika.api.aggregator.nodes.ISecondaryEntryPointNode.CombineType;
import com.exametrika.api.component.config.model.AgentComponentSchemaConfiguration;
import com.exametrika.api.component.config.model.AlertChannelSchemaConfiguration;
import com.exametrika.api.component.config.model.AlertRecipientSchemaConfiguration;
import com.exametrika.api.component.config.model.AlertRecipientSchemaConfiguration.Type;
import com.exametrika.api.component.config.model.AllIncidentsSelectorSchemaConfiguration;
import com.exametrika.api.component.config.model.BaseComponentDiscoveryStrategySchemaConfiguration;
import com.exametrika.api.component.config.model.ComponentModelSchemaConfiguration;
import com.exametrika.api.component.config.model.ComponentPatternCheckPermissionStrategySchemaConfiguration;
import com.exametrika.api.component.config.model.ComponentPrefixCheckPermissionStrategySchemaConfiguration;
import com.exametrika.api.component.config.model.ComponentSchemaConfiguration;
import com.exametrika.api.component.config.model.DisableMaintenanceModeActionSchemaConfiguration;
import com.exametrika.api.component.config.model.EnableMaintenanceModeActionSchemaConfiguration;
import com.exametrika.api.component.config.model.ExpressionComplexAlertSchemaConfiguration;
import com.exametrika.api.component.config.model.ExpressionComplexRuleSchemaConfiguration;
import com.exametrika.api.component.config.model.ExpressionComponentJobOperationSchemaConfiguration;
import com.exametrika.api.component.config.model.ExpressionGroupAvailabilityConditionSchemaConfiguration;
import com.exametrika.api.component.config.model.ExpressionHealthCheckSchemaConfiguration;
import com.exametrika.api.component.config.model.ExpressionIncidentGroupSchemaConfiguration;
import com.exametrika.api.component.config.model.ExpressionSimpleAlertSchemaConfiguration;
import com.exametrika.api.component.config.model.ExpressionSimpleRuleSchemaConfiguration;
import com.exametrika.api.component.config.model.GroupComponentSchemaConfiguration;
import com.exametrika.api.component.config.model.GroupScopeAggregationStrategySchemaConfiguration;
import com.exametrika.api.component.config.model.HealthAlertSchemaConfiguration;
import com.exametrika.api.component.config.model.HealthComponentSchemaConfiguration;
import com.exametrika.api.component.config.model.HealthSchemaConfiguration;
import com.exametrika.api.component.config.model.HostComponentSchemaConfiguration;
import com.exametrika.api.component.config.model.HostDeletionStrategySchemaConfiguration;
import com.exametrika.api.component.config.model.HostDiscoveryStrategySchemaConfiguration;
import com.exametrika.api.component.config.model.LogActionSchemaConfiguration;
import com.exametrika.api.component.config.model.MailAlertChannelSchemaConfiguration;
import com.exametrika.api.component.config.model.MeasurementStrategyActionSchemaConfiguration;
import com.exametrika.api.component.config.model.NodeComponentSchemaConfiguration;
import com.exametrika.api.component.config.model.NodeDeletionStrategySchemaConfiguration;
import com.exametrika.api.component.config.model.NodeDiscoveryStrategySchemaConfiguration;
import com.exametrika.api.component.config.model.NodeGroupScopeAggregationStrategySchemaConfiguration;
import com.exametrika.api.component.config.model.PatternGroupDiscoveryStrategySchemaConfiguration;
import com.exametrika.api.component.config.model.PredefinedGroupSchemaConfiguration;
import com.exametrika.api.component.config.model.SimpleGroupDiscoveryStrategySchemaConfiguration;
import com.exametrika.api.component.config.model.TagIncidentGroupSchemaConfiguration;
import com.exametrika.api.component.config.model.TransactionComponentSchemaConfiguration;
import com.exametrika.api.component.config.model.TransactionDeletionStrategySchemaConfiguration;
import com.exametrika.api.component.config.model.TransactionDiscoveryStrategySchemaConfiguration;
import com.exametrika.api.component.config.model.TransactionGroupDiscoveryStrategySchemaConfiguration;
import com.exametrika.api.component.config.model.UserInterfaceComponentSchemaConfiguration;
import com.exametrika.api.component.config.model.UserInterfaceSchemaConfiguration;
import com.exametrika.api.component.nodes.IHealthComponentVersion;
import com.exametrika.api.component.nodes.IHealthComponentVersion.State;
import com.exametrika.api.exadb.core.config.schema.ModuleSchemaConfiguration;
import com.exametrika.api.exadb.jobs.config.model.JobSchemaConfiguration;
import com.exametrika.api.exadb.jobs.config.model.StandardSchedulePeriodSchemaConfiguration;
import com.exametrika.api.exadb.security.config.model.SecuritySchemaConfiguration;
import com.exametrika.common.config.AbstractExtensionLoader;
import com.exametrika.common.config.ConfigurationLoader;
import com.exametrika.common.config.ILoadContext;
import com.exametrika.common.config.InvalidConfigurationException;
import com.exametrika.common.config.property.SystemPropertyResolver;
import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.aggregator.schema.AggregationSchemaBuilder;
import com.exametrika.impl.component.schema.ComponentSchemaBuilder;
import com.exametrika.impl.exadb.core.config.schema.SchemaLoadContext;
import com.exametrika.impl.exadb.security.schema.SecuritySchemaBuilder;
import com.exametrika.spi.aggregator.common.meters.config.CounterConfiguration;
import com.exametrika.spi.component.config.model.ActionSchemaConfiguration;
import com.exametrika.spi.component.config.model.AlertSchemaConfiguration;
import com.exametrika.spi.component.config.model.GroupAvailabilityConditionSchemaConfiguration;
import com.exametrika.spi.component.config.model.GroupDiscoveryStrategySchemaConfiguration;
import com.exametrika.spi.component.config.model.RuleSchemaConfiguration;
import com.exametrika.spi.component.config.model.SelectorSchemaConfiguration;
import com.exametrika.spi.exadb.jobs.config.model.SchedulePeriodSchemaConfiguration;
import com.exametrika.spi.exadb.jobs.config.model.ScheduleSchemaConfiguration;


/**
 * The {@link ComponentSchemaLoader} is a loader of component schemas.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class ComponentSchemaLoader extends AbstractExtensionLoader {
    @Override
    public Object loadExtension(String name, String type, Object object, ILoadContext context) {
        JsonObject element = (JsonObject) object;
        if (type.equals("ComponentModel")) {
            loadComponentModel(element, context);
            return null;
        } else if (type.equals("BaseComponentDiscoveryStrategy"))
            return new BaseComponentDiscoveryStrategySchemaConfiguration((String) element.get("component"));
        else if (type.equals("NodeDiscoveryStrategy"))
            return new NodeDiscoveryStrategySchemaConfiguration((String) element.get("component"));
        else if (type.equals("HostDiscoveryStrategy"))
            return new HostDiscoveryStrategySchemaConfiguration((String) element.get("component"));
        else if (type.equals("TransactionDiscoveryStrategy"))
            return new TransactionDiscoveryStrategySchemaConfiguration((String) element.get("component"));
        else if (type.equals("NodeDeletionStrategy"))
            return new NodeDeletionStrategySchemaConfiguration();
        else if (type.equals("HostDeletionStrategy"))
            return new HostDeletionStrategySchemaConfiguration();
        else if (type.equals("TransactionDeletionStrategy")) {
            long retentionPeriodCount = element.get("retentionPeriodCount");
            return new TransactionDeletionStrategySchemaConfiguration((int) retentionPeriodCount);
        } else if (type.equals("ExpressionComponentJobOperation"))
            return new ExpressionComponentJobOperationSchemaConfiguration((String) element.get("expression"));
        else if (type.equals("ComponentPrefixCheckPermissionStrategy"))
            return new ComponentPrefixCheckPermissionStrategySchemaConfiguration();
        else if (type.equals("ComponentPatternCheckPermissionStrategy"))
            return new ComponentPatternCheckPermissionStrategySchemaConfiguration();
        else if (type.equals("GroupScopeAggregationStrategy")) {
            String hierarchyType = element.get("hierarchyType");
            boolean hasSubScope = element.get("hasSubScope");
            return new GroupScopeAggregationStrategySchemaConfiguration(hierarchyType, hasSubScope);
        } else if (type.equals("NodeGroupScopeAggregationStrategy")) {
            String hierarchyType = element.get("hierarchyType");
            return new NodeGroupScopeAggregationStrategySchemaConfiguration(hierarchyType);
        } else
            throw new InvalidConfigurationException();
    }

    private void loadComponentModel(JsonObject element, ILoadContext context) {
        SchemaLoadContext loadContext = context.get(ModuleSchemaConfiguration.SCHEMA);
        ModuleSchemaConfiguration currentModule = loadContext.getCurrentModule();

        long version = element.get("version");

        String combineTypeStr = element.get("combineType");
        CombineType combineType;
        if (combineTypeStr.equals("stack"))
            combineType = CombineType.STACK;
        else if (combineTypeStr.equals("transaction"))
            combineType = CombineType.TRANSACTION;
        else if (combineTypeStr.equals("node"))
            combineType = CombineType.NODE;
        else if (combineTypeStr.equals("all"))
            combineType = CombineType.ALL;
        else
            combineType = Assert.error();

        List<PeriodTypeSchemaConfiguration> periods = new ArrayList<PeriodTypeSchemaConfiguration>();
        for (Map.Entry<String, Object> entry : (JsonObject) element.get("periods"))
            periods.add(loadPeriodType(entry.getKey(), (JsonObject) entry.getValue(), context));

        Map<String, Set<AggregationComponentTypeSchemaConfiguration>> componentTypesMap = new LinkedHashMap<String,
                Set<AggregationComponentTypeSchemaConfiguration>>();
        Set<ComponentSchemaConfiguration> components = new LinkedHashSet<ComponentSchemaConfiguration>();
        Map<String, UserInterfaceComponentSchemaConfiguration> uiComponents = new LinkedHashMap<String, UserInterfaceComponentSchemaConfiguration>();
        for (Map.Entry<String, Object> entry : (JsonObject) element.get("components"))
            components.add(loadComponent(entry.getKey(), (JsonObject) entry.getValue(), context, componentTypesMap,
                    uiComponents));

        List<PeriodTypeSchemaConfiguration> periodTypes = new ArrayList<PeriodTypeSchemaConfiguration>();
        for (PeriodTypeSchemaConfiguration period : periods) {
            Set<AggregationComponentTypeSchemaConfiguration> componentTypes = componentTypesMap.remove(period.getName());
            if (componentTypes == null)
                periodTypes.add(period);
            else
                periodTypes.add(new PeriodTypeSchemaConfiguration(period.getName(), componentTypes, period.getPeriod(),
                        period.getCyclePeriodCount(), period.isNonAggregating(), period.getParentDomain()));
        }

        Assert.isTrue(componentTypesMap.isEmpty());

        HealthSchemaConfiguration health = loadHealth((JsonObject) element.get("health", null), context);
        Map<String, PredefinedGroupSchemaConfiguration> groups = loadGroups((JsonObject) element.get("groups"), context);

        SecuritySchemaConfiguration securityModelSchema = load(null, "Security", element.get("security"), context);
        UserInterfaceSchemaConfiguration userInterface = loadUserInterface(periods, (JsonObject) element.get("ui", null), uiComponents);
        ComponentModelSchemaConfiguration componentModelSchema = new ComponentModelSchemaConfiguration(components, health,
                groups, userInterface, (int) version);

        ComponentSchemaBuilder componentBuilder = new ComponentSchemaBuilder();
        componentBuilder.buildSchema(componentModelSchema, currentModule);

        SecuritySchemaBuilder securityBuilder = new SecuritySchemaBuilder();
        securityBuilder.buildSchema(securityModelSchema, currentModule);

        AggregationSchemaConfiguration aggregationSchema = new AggregationSchemaConfiguration(periodTypes, combineType, (int) version);
        AggregationSchemaBuilder aggregationBuilder = new AggregationSchemaBuilder();
        aggregationBuilder.buildSchema(aggregationSchema, currentModule);
    }

    private UserInterfaceSchemaConfiguration loadUserInterface(List<PeriodTypeSchemaConfiguration> periods, JsonObject element,
                                                               Map<String, UserInterfaceComponentSchemaConfiguration> uiComponents) {
        if (element == null)
            return null;

        return new UserInterfaceSchemaConfiguration(periods, (JsonObject) element.get("models"),
                (JsonObject) element.get("navBar"), (JsonArray) element.get("notifications"),
                (JsonObject) element.get("views"), uiComponents);
    }

    private HealthSchemaConfiguration loadHealth(JsonObject element, ILoadContext context) {
        if (element == null)
            return null;

        String firstAggregationPeriod = element.get("firstAggregationPeriod");
        CounterConfiguration totalCounter = load(null, "meters.Counter", element.get("totalCounter"), context);
        CounterConfiguration upCounter = load(null, "meters.Counter", element.get("upCounter"), context);
        CounterConfiguration downCounter = load(null, "meters.Counter", element.get("downCounter"), context);
        CounterConfiguration failureCounter = load(null, "meters.Counter", element.get("failureCounter"), context);
        CounterConfiguration maintenanceCounter = load(null, "meters.Counter", element.get("maintenanceCounter"), context);

        return new HealthSchemaConfiguration(firstAggregationPeriod, totalCounter, upCounter, downCounter, failureCounter, maintenanceCounter);
    }

    private Map<String, PredefinedGroupSchemaConfiguration> loadGroups(JsonObject element, ILoadContext context) {
        Map<String, PredefinedGroupSchemaConfiguration> groups = new LinkedHashMap<String, PredefinedGroupSchemaConfiguration>();
        for (Map.Entry<String, Object> entry : element)
            groups.put(entry.getKey(), loadGroup(entry.getKey(), (JsonObject) entry.getValue(), context));

        return groups;
    }

    private PredefinedGroupSchemaConfiguration loadGroup(String name, JsonObject element, ILoadContext context) {
        String groupType = element.get("groupType");
        Map<String, PredefinedGroupSchemaConfiguration> groups = loadGroups((JsonObject) element.get("groups"), context);
        JsonObject options = element.get("options", null);
        JsonObject properties = element.get("properties", null);
        Set<String> tags = JsonUtils.toSet((JsonArray) element.get("tags"));
        Set<RuleSchemaConfiguration> rules = loadRules((JsonObject) element.get("rules"), context);
        Set<AlertSchemaConfiguration> alerts = loadAlerts((JsonObject) element.get("alerts"), context);
        Set<RuleSchemaConfiguration> groupRules = loadRules((JsonObject) element.get("groupRules"), context);
        Set<AlertSchemaConfiguration> groupAlerts = loadAlerts((JsonObject) element.get("groupAlerts"), context);
        Set<JobSchemaConfiguration> jobs = load(null, "Jobs", element.get("jobs"), context);
        return new PredefinedGroupSchemaConfiguration(groupType, name, groups, options, properties, tags, rules,
                alerts, groupRules, groupAlerts, jobs);
    }

    private PeriodTypeSchemaConfiguration loadPeriodType(String name, JsonObject element, ILoadContext context) {
        StandardSchedulePeriodSchemaConfiguration period = load(null, "StandardSchedulePeriod", element.get("period"), context);
        long cyclePeriodCount = element.get("cyclePeriodCount");
        boolean nonAggregating = element.get("nonAggregating");
        String parentDomain = element.get("parentDomain", null);

        Set<AggregationComponentTypeSchemaConfiguration> componentTypes = new LinkedHashSet<AggregationComponentTypeSchemaConfiguration>();
        return new PeriodTypeSchemaConfiguration(name, componentTypes, period, (int) cyclePeriodCount, nonAggregating,
                parentDomain);
    }

    private ComponentSchemaConfiguration loadComponent(String name, JsonObject element, ILoadContext context,
                                                       Map<String, Set<AggregationComponentTypeSchemaConfiguration>> componentTypesMap,
                                                       Map<String, UserInterfaceComponentSchemaConfiguration> uiComponents) {
        String type = getType(element);

        JsonObject views = element.get("views", null);
        if (views != null && !views.isEmpty())
            uiComponents.put(name, new UserInterfaceComponentSchemaConfiguration(name, views, (String) element.get("defaultView", null)));

        if (element.contains("aggregationSchema"))
            loadAggregationSchema(element, context, componentTypesMap);

        if (type.equals("component")) {
            Set<ActionSchemaConfiguration> actions = loadActions(element, context);
            Set<RuleSchemaConfiguration> rules = loadRules((JsonObject) element.get("rules"), context);
            Set<JobSchemaConfiguration> jobs = load(null, "Jobs", element.get("jobs"), context);
            Set<SelectorSchemaConfiguration> selectors = loadSelectors(element, context);
            Set<AlertSchemaConfiguration> alerts = loadAlerts((JsonObject) element.get("alerts"), context);
            List<GroupDiscoveryStrategySchemaConfiguration> groupDiscoveryStrategies = loadGroupDiscoveryStrategies(
                    (JsonArray) element.get("groupDiscoveryStrategies"), context);
            return new ComponentSchemaConfiguration(name, actions, rules, selectors, alerts, groupDiscoveryStrategies, jobs);
        } else if (type.equals("aggregationComponent")) {
            Set<ActionSchemaConfiguration> actions = loadActions(element, context);
            Set<RuleSchemaConfiguration> rules = loadRules((JsonObject) element.get("rules"), context);
            Set<JobSchemaConfiguration> jobs = load(null, "Jobs", element.get("jobs"), context);
            Set<SelectorSchemaConfiguration> selectors = loadSelectors(element, context);
            Set<AlertSchemaConfiguration> alerts = loadAlerts((JsonObject) element.get("alerts"), context);
            List<GroupDiscoveryStrategySchemaConfiguration> groupDiscoveryStrategies = loadGroupDiscoveryStrategies(
                    (JsonArray) element.get("groupDiscoveryStrategies"), context);

            return new ComponentSchemaConfiguration(name, actions, rules, selectors, alerts, groupDiscoveryStrategies, jobs);
        } else if (type.equals("healthComponent")) {
            Set<ActionSchemaConfiguration> actions = loadActions(element, context);
            Set<RuleSchemaConfiguration> rules = loadRules((JsonObject) element.get("rules"), context);
            Set<JobSchemaConfiguration> jobs = load(null, "Jobs", element.get("jobs"), context);
            Set<SelectorSchemaConfiguration> selectors = loadSelectors(element, context);
            Set<AlertSchemaConfiguration> alerts = loadAlerts((JsonObject) element.get("alerts"), context);
            List<GroupDiscoveryStrategySchemaConfiguration> groupDiscoveryStrategies = loadGroupDiscoveryStrategies(
                    (JsonArray) element.get("groupDiscoveryStrategies"), context);
            String healthComponentType = element.get("healthComponentType");

            return new HealthComponentSchemaConfiguration(name, actions, rules, selectors, alerts, groupDiscoveryStrategies, jobs,
                    healthComponentType);
        } else if (type.equals("agent")) {
            Set<ActionSchemaConfiguration> actions = loadActions(element, context);
            Set<RuleSchemaConfiguration> rules = loadRules((JsonObject) element.get("rules"), context);
            Set<JobSchemaConfiguration> jobs = load(null, "Jobs", element.get("jobs"), context);
            Set<SelectorSchemaConfiguration> selectors = loadSelectors(element, context);
            Set<AlertSchemaConfiguration> alerts = loadAlerts((JsonObject) element.get("alerts"), context);
            List<GroupDiscoveryStrategySchemaConfiguration> groupDiscoveryStrategies = loadGroupDiscoveryStrategies(
                    (JsonArray) element.get("groupDiscoveryStrategies"), context);
            String healthComponentType = element.get("healthComponentType");
            String inlineProfilerConfiguration = loadInlineProfilerConfiguration(element);

            return new AgentComponentSchemaConfiguration(name, actions, rules, selectors, alerts, groupDiscoveryStrategies, jobs,
                    healthComponentType, inlineProfilerConfiguration);
        } else if (type.equals("host")) {
            Set<ActionSchemaConfiguration> actions = loadActions(element, context);
            Set<RuleSchemaConfiguration> rules = loadRules((JsonObject) element.get("rules"), context);
            Set<JobSchemaConfiguration> jobs = load(null, "Jobs", element.get("jobs"), context);
            Set<SelectorSchemaConfiguration> selectors = loadSelectors(element, context);
            Set<AlertSchemaConfiguration> alerts = loadAlerts((JsonObject) element.get("alerts"), context);
            List<GroupDiscoveryStrategySchemaConfiguration> groupDiscoveryStrategies = loadGroupDiscoveryStrategies(
                    (JsonArray) element.get("groupDiscoveryStrategies"), context);
            String healthComponentType = element.get("healthComponentType");
            String inlineProfilerConfiguration = loadInlineProfilerConfiguration(element);

            return new HostComponentSchemaConfiguration(name, actions, rules, selectors, alerts, groupDiscoveryStrategies, jobs,
                    healthComponentType, inlineProfilerConfiguration);
        } else if (type.equals("node")) {
            Set<ActionSchemaConfiguration> actions = loadActions(element, context);
            Set<RuleSchemaConfiguration> rules = loadRules((JsonObject) element.get("rules"), context);
            Set<JobSchemaConfiguration> jobs = load(null, "Jobs", element.get("jobs"), context);
            Set<SelectorSchemaConfiguration> selectors = loadSelectors(element, context);
            Set<AlertSchemaConfiguration> alerts = loadAlerts((JsonObject) element.get("alerts"), context);
            List<GroupDiscoveryStrategySchemaConfiguration> groupDiscoveryStrategies = loadGroupDiscoveryStrategies(
                    (JsonArray) element.get("groupDiscoveryStrategies"), context);
            String healthComponentType = element.get("healthComponentType");
            String inlineProfilerConfiguration = loadInlineProfilerConfiguration(element);

            return new NodeComponentSchemaConfiguration(name, actions, rules, selectors, alerts, groupDiscoveryStrategies, jobs,
                    healthComponentType, inlineProfilerConfiguration);
        } else if (type.equals("transaction")) {
            Set<ActionSchemaConfiguration> actions = loadActions(element, context);
            Set<RuleSchemaConfiguration> rules = loadRules((JsonObject) element.get("rules"), context);
            Set<JobSchemaConfiguration> jobs = load(null, "Jobs", element.get("jobs"), context);
            Set<SelectorSchemaConfiguration> selectors = loadSelectors(element, context);
            Set<AlertSchemaConfiguration> alerts = loadAlerts((JsonObject) element.get("alerts"), context);
            List<GroupDiscoveryStrategySchemaConfiguration> groupDiscoveryStrategies = loadGroupDiscoveryStrategies(
                    (JsonArray) element.get("groupDiscoveryStrategies"), context);
            String healthComponentType = element.get("healthComponentType");

            return new TransactionComponentSchemaConfiguration(name, actions, rules, selectors, alerts, groupDiscoveryStrategies,
                    jobs, healthComponentType);
        } else if (type.equals("group")) {
            Set<ActionSchemaConfiguration> actions = loadActions(element, context);
            Set<RuleSchemaConfiguration> rules = loadRules((JsonObject) element.get("rules"), context);
            Set<JobSchemaConfiguration> jobs = load(null, "Jobs", element.get("jobs"), context);
            Set<SelectorSchemaConfiguration> selectors = loadSelectors(element, context);
            Set<AlertSchemaConfiguration> alerts = loadAlerts((JsonObject) element.get("alerts"), context);
            List<GroupDiscoveryStrategySchemaConfiguration> groupDiscoveryStrategies = loadGroupDiscoveryStrategies(
                    (JsonArray) element.get("groupDiscoveryStrategies"), context);
            boolean allowComponents = element.get("allowComponents");
            boolean allowGroups = element.get("allowGroups");
            Set<String> componentTypes = JsonUtils.toSet((JsonArray) element.get("componentTypes", null));
            Set<String> groupTypes = JsonUtils.toSet((JsonArray) element.get("groupTypes", null));
            boolean aggregationGroup = element.get("aggregationGroup");
            GroupAvailabilityConditionSchemaConfiguration availabilityCondition = loadAvailabilityCondition(
                    (JsonObject) element.get("availabilityCondition", null), context);
            String healthComponentType = element.get("healthComponentType", null);
            return new GroupComponentSchemaConfiguration(name, actions, rules, selectors, alerts, groupDiscoveryStrategies, jobs,
                    allowComponents, allowGroups, componentTypes, groupTypes, aggregationGroup, availabilityCondition,
                    healthComponentType);
        } else
            return super.load(name, type, element, context);
    }

    private String loadInlineProfilerConfiguration(JsonObject element) {
        String configurationName = element.get("profilerConfigurationName");
        ConfigurationLoader loader = new ConfigurationLoader();
        return loader.createInlineConfiguration(configurationName, new SystemPropertyResolver());
    }

    protected final Set<ActionSchemaConfiguration> loadActions(JsonObject element, ILoadContext context) {
        Set<ActionSchemaConfiguration> actions = new LinkedHashSet<ActionSchemaConfiguration>();
        for (Map.Entry<String, Object> entry : (JsonObject) element.get("actions")) {
            ActionSchemaConfiguration action = loadAction(entry.getKey(), (JsonObject) entry.getValue(), context);
            actions.add(action);
        }

        return actions;
    }

    protected final ActionSchemaConfiguration loadAction(String name, JsonObject element, ILoadContext context) {
        String type = getType(element);
        if (type.equals("EnableMaintenanceModeAction"))
            return new EnableMaintenanceModeActionSchemaConfiguration(name);
        else if (type.equals("DisableMaintenanceModeAction"))
            return new DisableMaintenanceModeActionSchemaConfiguration(name);
        else if (type.equals("LogAction"))
            return new LogActionSchemaConfiguration(name);
        else if (type.equals("MeasurementStrategyAction"))
            return new MeasurementStrategyActionSchemaConfiguration(name);
        else
            return load(name, null, element, context);
    }

    protected final Set<RuleSchemaConfiguration> loadRules(JsonObject element, ILoadContext context) {
        Set<RuleSchemaConfiguration> rules = new LinkedHashSet<RuleSchemaConfiguration>();
        for (Map.Entry<String, Object> entry : element) {
            RuleSchemaConfiguration rule = loadRule(entry.getKey(), (JsonObject) entry.getValue(), context);
            rules.add(rule);
        }

        return rules;
    }

    private RuleSchemaConfiguration loadRule(String name, JsonObject element, ILoadContext context) {
        String type = getType(element);

        boolean enabled = element.get("enabled");

        if (type.equals("ExpressionSimpleRule"))
            return new ExpressionSimpleRuleSchemaConfiguration(name, (String) element.get("expression"), enabled);
        else if (type.equals("ExpressionComplexRule"))
            return new ExpressionComplexRuleSchemaConfiguration(name, (String) element.get("expression"), enabled);
        else if (type.equals("ExpressionHealthCheck"))
            return new ExpressionHealthCheckSchemaConfiguration(name, (String) element.get("expression"), enabled);
        else
            return load(name, null, element, context);
    }

    protected final Set<SelectorSchemaConfiguration> loadSelectors(JsonObject element, ILoadContext context) {
        Set<SelectorSchemaConfiguration> selectors = new LinkedHashSet<SelectorSchemaConfiguration>();
        for (Map.Entry<String, Object> entry : (JsonObject) element.get("selectors")) {
            SelectorSchemaConfiguration selector = loadSelector(entry.getKey(), (JsonObject) entry.getValue(), context);
            selectors.add(selector);
        }

        return selectors;
    }

    private SelectorSchemaConfiguration loadSelector(String name, JsonObject element, ILoadContext context) {
        String type = getType(element);
        if (type.equals("AllIncidentsSelector"))
            return new AllIncidentsSelectorSchemaConfiguration(name);
        else
            return load(name, type, element, context);
    }

    protected final Set<AlertSchemaConfiguration> loadAlerts(JsonObject element, ILoadContext context) {
        Set<AlertSchemaConfiguration> alerts = new LinkedHashSet<AlertSchemaConfiguration>();
        for (Map.Entry<String, Object> entry : element) {
            AlertSchemaConfiguration alert = loadAlert(entry.getKey(), (JsonObject) entry.getValue(), context);
            alerts.add(alert);
        }

        return alerts;
    }

    private AlertSchemaConfiguration loadAlert(String name, JsonObject element, ILoadContext context) {
        String type = getType(element);

        List<AlertChannelSchemaConfiguration> channels = new ArrayList<AlertChannelSchemaConfiguration>();
        for (Object child : (JsonArray) element.get("channels"))
            channels.add(loadAlertChannel((JsonObject) child, context));

        List<String> tags = new ArrayList<String>();
        for (Object child : (JsonArray) element.get("tags"))
            tags.add((String) child);

        String description = element.get("description", null);
        boolean enabled = element.get("enabled");

        if (type.equals("HealthAlert")) {
            IHealthComponentVersion.State stateThreshold;
            String str = element.get("stateThreshold");
            if (str.equals("healthWarning"))
                stateThreshold = State.HEALTH_WARNING;
            else if (str.equals("healthError"))
                stateThreshold = State.HEALTH_ERROR;
            else if (str.equals("unavailable"))
                stateThreshold = State.UNAVAILABLE;
            else
                stateThreshold = Assert.error();

            return new HealthAlertSchemaConfiguration(name, description, channels, tags, enabled, stateThreshold);
        } else if (type.equals("ExpressionSimpleAlert")) {
            String onCondition = element.get("onCondition");
            String offCondition = element.get("offCondition", null);
            return new ExpressionSimpleAlertSchemaConfiguration(name, description, channels, tags, enabled, onCondition, offCondition);
        } else if (type.equals("ExpressionComplexAlert")) {
            String onCondition = element.get("onCondition");
            String offCondition = element.get("offCondition", null);
            return new ExpressionComplexAlertSchemaConfiguration(name, description, channels, tags, enabled, onCondition, offCondition);
        } else if (type.equals("TagIncidentGroup")) {
            String pattern = element.get("pattern");
            return new TagIncidentGroupSchemaConfiguration(name, description, channels, tags, enabled, pattern);
        } else if (type.equals("ExpressionIncidentGroup")) {
            String expression = element.get("expression");
            return new ExpressionIncidentGroupSchemaConfiguration(name, description, channels, tags, enabled, expression);
        } else
            return load(name, null, element, context);
    }

    private AlertChannelSchemaConfiguration loadAlertChannel(JsonObject element, ILoadContext context) {
        String type = getType(element);

        if (type.equals("MailAlertChannel")) {
            String name = element.get("name");
            String onTemplate = element.get("onTemplate");
            String offTemplate = element.get("offTemplate", null);
            String statusTemplate = element.get("statusTemplate", null);
            ScheduleSchemaConfiguration schedule = load(null, "StandardSchedule", element.get("schedule", null), context);
            SchedulePeriodSchemaConfiguration period = load(null, "StandardSchedulePeriod", element.get("period", null), context);
            List<AlertRecipientSchemaConfiguration> recipients = new ArrayList<AlertRecipientSchemaConfiguration>();
            for (Object child : (JsonArray) element.get("recipients"))
                recipients.add(loadAlertRecipient((JsonObject) child));

            String onSubject = element.get("onSubject");
            String offSubject = element.get("offSubject", onSubject);
            String statusSubject = element.get("statusSubject", onSubject);
            boolean formatted = element.get("formatted");
            String senderName = element.get("senderName", null);
            String senderAddress = element.get("senderAddress", null);

            return new MailAlertChannelSchemaConfiguration(name, onTemplate, offTemplate, statusTemplate, schedule, period, recipients,
                    onSubject, offSubject, statusSubject, formatted, senderName, senderAddress);
        } else
            return load(null, null, element, context);
    }

    private AlertRecipientSchemaConfiguration loadAlertRecipient(JsonObject element) {
        AlertRecipientSchemaConfiguration.Type type = loadAlertRecipientType((String) element.get("type"));
        String name = element.get("name");
        if (type != AlertRecipientSchemaConfiguration.Type.ADDRESS)
            return new AlertRecipientSchemaConfiguration(type, name);
        else {
            String address = element.get("address");
            return new AlertRecipientSchemaConfiguration(name, address);
        }
    }

    private AlertRecipientSchemaConfiguration.Type loadAlertRecipientType(String element) {
        if (element.equals("role"))
            return Type.ROLE;
        else if (element.equals("userGroup"))
            return Type.USER_GROUP;
        else if (element.equals("user"))
            return Type.USER;
        else if (element.equals("address"))
            return Type.ADDRESS;
        else
            return Assert.error();
    }

    private void loadAggregationSchema(JsonObject element, ILoadContext context,
                                       Map<String, Set<AggregationComponentTypeSchemaConfiguration>> componentTypesMap) {
        for (Map.Entry<String, Object> entry : (JsonObject) element.get("aggregationSchema")) {
            Set<AggregationComponentTypeSchemaConfiguration> componentTypes = load(null, "PeriodTypeAggregationSchema", entry.getValue(), context);
            Assert.notNull(componentTypes);

            Set<AggregationComponentTypeSchemaConfiguration> list = componentTypesMap.get(entry.getKey());
            if (list == null) {
                list = new LinkedHashSet<AggregationComponentTypeSchemaConfiguration>();
                componentTypesMap.put(entry.getKey(), list);
            }

            list.addAll(componentTypes);
        }
    }

    private GroupAvailabilityConditionSchemaConfiguration loadAvailabilityCondition(JsonObject element,
                                                                                    ILoadContext context) {
        if (element == null)
            return null;

        String type = getType(element);
        if (type.equals("ExpressionGroupAvailabilityCondition")) {
            String expression = element.get("expression");
            return new ExpressionGroupAvailabilityConditionSchemaConfiguration(expression);
        } else
            return load(null, null, element, context);
    }

    protected final List<GroupDiscoveryStrategySchemaConfiguration> loadGroupDiscoveryStrategies(JsonArray element, ILoadContext context) {
        List<GroupDiscoveryStrategySchemaConfiguration> discoveryStrategies = new ArrayList<GroupDiscoveryStrategySchemaConfiguration>();
        for (Object child : element) {
            GroupDiscoveryStrategySchemaConfiguration discoveryStrategy = loadGroupDiscoveryStrategy((JsonObject) child, context);
            discoveryStrategies.add(discoveryStrategy);
        }

        return discoveryStrategies;
    }

    private GroupDiscoveryStrategySchemaConfiguration loadGroupDiscoveryStrategy(JsonObject element, ILoadContext context) {
        String type = getType(element);
        if (type.equals("PatternGroupDiscoveryStrategy")) {
            String component = element.get("component");
            String pattern = element.get("pattern");
            String group = element.get("group", null);
            return new PatternGroupDiscoveryStrategySchemaConfiguration(component, pattern, group);
        } else if (type.equals("TransactionGroupDiscoveryStrategy")) {
            String component = element.get("component");
            String group = element.get("group", null);
            return new TransactionGroupDiscoveryStrategySchemaConfiguration(component, group);
        } else if (type.equals("SimpleGroupDiscoveryStrategy")) {
            String group = element.get("group");
            return new SimpleGroupDiscoveryStrategySchemaConfiguration(group);
        } else
            return load(null, type, element, context);
    }
}