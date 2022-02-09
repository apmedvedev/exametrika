/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.nodes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.exametrika.api.aggregator.IPeriodName;
import com.exametrika.api.aggregator.IPeriodNameManager;
import com.exametrika.api.aggregator.common.model.IScopeName;
import com.exametrika.api.aggregator.nodes.IAggregationNode;
import com.exametrika.api.component.IAction;
import com.exametrika.api.component.ISelector;
import com.exametrika.api.component.config.model.ComponentSchemaConfiguration;
import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.api.component.nodes.IComponentVersion;
import com.exametrika.api.component.nodes.IGroupComponent;
import com.exametrika.api.component.nodes.IIncident;
import com.exametrika.api.component.nodes.IIncidentGroup;
import com.exametrika.api.component.schema.IActionSchema;
import com.exametrika.api.component.schema.ISelectorSchema;
import com.exametrika.api.exadb.core.IDumpContext;
import com.exametrika.api.exadb.core.IOperationWrapper;
import com.exametrika.api.exadb.jobs.IJob;
import com.exametrika.api.exadb.jobs.IJobService;
import com.exametrika.api.exadb.jobs.config.model.JobSchemaConfiguration;
import com.exametrika.api.exadb.jobs.config.model.OneTimeJobSchemaConfiguration;
import com.exametrika.api.exadb.jobs.config.model.RecurrentJobSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.IObjectSpace;
import com.exametrika.api.exadb.objectdb.fields.IJsonBlobField;
import com.exametrika.api.exadb.objectdb.fields.INumericField;
import com.exametrika.api.exadb.objectdb.fields.INumericSequenceField;
import com.exametrika.api.exadb.objectdb.fields.IReferenceField;
import com.exametrika.api.exadb.objectdb.fields.ISerializableField;
import com.exametrika.api.exadb.objectdb.fields.ISingleReferenceField;
import com.exametrika.api.exadb.objectdb.fields.IStructuredBlobField;
import com.exametrika.api.exadb.objectdb.fields.ITagField;
import com.exametrika.api.exadb.security.IPermission;
import com.exametrika.common.json.IJsonHandler;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.json.JsonSerializers;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Objects;
import com.exametrika.common.utils.Times;
import com.exametrika.impl.aggregator.common.model.ScopeName;
import com.exametrika.impl.component.fields.VersionChangeRecord;
import com.exametrika.impl.component.schema.ComponentNodeSchema;
import com.exametrika.impl.component.services.AlertService;
import com.exametrika.impl.exadb.objectdb.ObjectNodeObject;
import com.exametrika.impl.exadb.objectdb.fields.JsonRecord;
import com.exametrika.impl.exadb.objectdb.schema.NodeSpaceSchema;
import com.exametrika.spi.aggregator.IRuleContext;
import com.exametrika.spi.aggregator.IRuleExecutor;
import com.exametrika.spi.component.IAlert;
import com.exametrika.spi.component.IHealthCheck;
import com.exametrika.spi.component.IComplexRule;
import com.exametrika.spi.component.IIncidentGroupRule;
import com.exametrika.spi.component.IRule;
import com.exametrika.spi.component.ISimpleRule;
import com.exametrika.spi.component.ITimeSnapshotOperation;
import com.exametrika.spi.component.IVersionChangeRecord;
import com.exametrika.spi.component.config.model.AlertSchemaConfiguration;
import com.exametrika.spi.component.config.model.RuleSchemaConfiguration;
import com.exametrika.spi.exadb.core.IDatabaseContext;


/**
 * The {@link ComponentNode} is a component node object.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are not thread safe.
 */
public class ComponentNode extends ObjectNodeObject implements IComponent, IRuleExecutor {
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(ComponentNode.class);
    private static final int SCOPE_FIELD = 0;
    private static final int CURRENT_VERSION_FIELD = 1;
    private static final int LOG_SEQUENCE_FIELD = 2;
    private static final int ACTION_LOG_FIELD = 3;
    private static final int RULES_FIELD = 4;
    private static final int ALERTS_FIELD = 5;
    private static final int INCIDENTS_FIELD = 6;
    private static final int JOBS_FIELD = 7;
    private static final int TAGS_FIELD = 8;
    private IPeriodName scope;
    private ComponentVersionNode snapshotVersion;
    private long snapshotTime;
    private List<ISimpleRule> simpleRules;
    private List<IComplexRule> complexRules;
    protected List<IHealthCheck> healthChecks;
    private Map<String, IAlert> alerts;
    private Map<String, IIncident> incidents;

    public ComponentNode(INode node) {
        super(node);
    }

    public final boolean isAccessAlowed() {
        return getSchema().getViewPermission().isAccessAllowed(this);
    }

    @Override
    public final boolean allowDeletion() {
        return false;
    }

    @Override
    public ComponentNodeSchema getSchema() {
        return (ComponentNodeSchema) super.getSchema();
    }

    @Override
    public ComponentSchemaConfiguration getConfiguration() {
        return getSchema().getConfiguration().getComponent();
    }

    @Override
    public final long getScopeId() {
        INumericField field = getField(SCOPE_FIELD);
        return field.getLong();
    }

    @Override
    public final IScopeName getScope() {
        if (scope != null && !scope.isStale())
            return scope.getName();
        else
            return refreshScope();
    }

    @Override
    public final String getTitle() {
        IComponentVersion version = get();
        if (version == null)
            version = getCurrentVersion();

        return version.getTitle();
    }

    @Override
    public final String getDescription() {
        IComponentVersion version = get();
        if (version == null)
            version = getCurrentVersion();

        return version.getDescription();
    }

    @Override
    public final Long getKey() {
        return getScopeId();
    }

    @Override
    public final IComponentVersion get() {
        ComponentVersionNode currentVersion = getCurrentVersion();

        long selectionTime;
        Object operation = getNode().getTransaction().getOperation();
        if (operation instanceof ITimeSnapshotOperation) {
            ITimeSnapshotOperation snapshotOperation = (ITimeSnapshotOperation) operation;
            selectionTime = snapshotOperation.getTime();
        } else if (operation instanceof IOperationWrapper && ((IOperationWrapper) operation).getOperation() instanceof ITimeSnapshotOperation) {
            ITimeSnapshotOperation snapshotOperation = (ITimeSnapshotOperation) ((IOperationWrapper) operation).getOperation();
            selectionTime = snapshotOperation.getTime();
        } else
            return currentVersion;

        if (snapshotTime == selectionTime)
            return snapshotVersion;

        snapshotVersion = currentVersion.readSnapshotVersion(selectionTime);
        snapshotTime = selectionTime;
        return snapshotVersion;
    }

    @Override
    public final ComponentVersionNode getCurrentVersion() {
        ISingleReferenceField<ComponentVersionNode> field = getField(CURRENT_VERSION_FIELD);
        return field.get();
    }

    public final void setCurrentVersion(ComponentVersionNode version) {
        ISingleReferenceField<ComponentVersionNode> field = getField(CURRENT_VERSION_FIELD);
        field.set(version);
    }

    public final ComponentVersionNode addVersion() {
        return addVersion(true);
    }

    public final IAlert findAlertSchema(String name) {
        ensureRuleCache();

        return alerts.get(name);
    }

    @Override
    public final void setOptions(JsonObject metadata) {
        if (Objects.equals(getCurrentVersion().getOptions(), metadata))
            return;

        IPermission permission = getSchema().getEditOptionsPermission();
        permission.beginCheck(this);

        ComponentVersionNode node = addVersion();
        node.setOptions(metadata);

        permission.endCheck();
    }

    public final void setProperties(JsonObject metadata) {
        if (Objects.equals(getCurrentVersion().getProperties(), metadata))
            return;

        ComponentVersionNode node = addVersion();
        node.setProperties(metadata);
    }

    @Override
    public final <T extends IAction> T createAction(String name) {
        Assert.notNull(name);
        Assert.checkState(!getCurrentVersion().isDeleted());

        IActionSchema actionSchema = getSchema().getActions().get(name);
        Assert.notNull(actionSchema);

        IPermission permission = actionSchema.getExecutePermission();
        permission.beginCheck(this);

        T result = actionSchema.getConfiguration().createAction(this, actionSchema);

        permission.endCheck();

        return result;
    }

    @Override
    public final <T extends ISelector> T createSelector(String name) {
        Assert.notNull(name);

        ISelectorSchema selectorSchema = getSchema().getSelectors().get(name);
        Assert.notNull(selectorSchema);

        IPermission permission = selectorSchema.getExecutePermission();
        permission.beginCheck(this);

        T result = selectorSchema.getConfiguration().createSelector(this, selectorSchema);

        permission.endCheck();

        return result;
    }

    @Override
    public final ActionLogNode getActionLog() {
        ISingleReferenceField<ActionLogNode> field = getField(ACTION_LOG_FIELD);
        return field.get();
    }

    @Override
    public Iterable<RuleSchemaConfiguration> getRules() {
        ISerializableField<List<RuleSchemaConfiguration>> field = getField(RULES_FIELD);
        List<RuleSchemaConfiguration> list = field.get();
        if (list != null)
            return list;
        else
            return Collections.emptyList();
    }

    @Override
    public RuleSchemaConfiguration findRule(String ruleName) {
        Assert.notNull(ruleName);

        ISerializableField<List<RuleSchemaConfiguration>> field = getField(RULES_FIELD);
        List<RuleSchemaConfiguration> rules = field.get();
        if (rules != null) {
            for (int i = 0; i < rules.size(); i++) {
                RuleSchemaConfiguration rule = rules.get(i);
                if (rule.getName().equals(ruleName))
                    return rule;
            }
        }

        return null;
    }

    @Override
    public void addRule(RuleSchemaConfiguration ruleConfiguration) {
        Assert.notNull(ruleConfiguration);

        IPermission permission = getSchema().getEditRulesPermission();
        permission.beginCheck(this);

        ISerializableField<List<RuleSchemaConfiguration>> field = getField(RULES_FIELD);
        List<RuleSchemaConfiguration> rules = field.get();
        if (rules == null)
            rules = new ArrayList<RuleSchemaConfiguration>();

        boolean set = false;
        for (int i = 0; i < rules.size(); i++) {
            RuleSchemaConfiguration rule = rules.get(i);
            if (rule.getName().equals(ruleConfiguration.getName())) {
                rules.set(i, ruleConfiguration);
                set = true;
                break;
            }
        }

        if (!set)
            rules.add(ruleConfiguration);

        field.set(rules);

        invalidateRuleCache();

        permission.endCheck();
    }

    @Override
    public void removeRule(String ruleName) {
        Assert.notNull(ruleName);

        IPermission permission = getSchema().getEditRulesPermission();
        permission.beginCheck(this);

        ISerializableField<List<RuleSchemaConfiguration>> field = getField(RULES_FIELD);
        List<RuleSchemaConfiguration> rules = field.get();
        if (rules != null) {
            for (int i = 0; i < rules.size(); i++) {
                RuleSchemaConfiguration rule = rules.get(i);
                if (rule.getName().equals(ruleName)) {
                    rules.remove(i);
                    break;
                }
            }
        }

        field.set(rules);

        invalidateRuleCache();

        permission.endCheck();
    }

    @Override
    public void removeAllRules() {
        IPermission permission = getSchema().getEditRulesPermission();
        permission.beginCheck(this);

        ISerializableField<List<RuleSchemaConfiguration>> field = getField(RULES_FIELD);
        field.set(null);

        invalidateRuleCache();

        permission.endCheck();
    }

    @Override
    public Iterable<AlertSchemaConfiguration> getAlerts() {
        ISerializableField<List<AlertSchemaConfiguration>> field = getField(ALERTS_FIELD);
        List<AlertSchemaConfiguration> list = field.get();
        if (list != null)
            return list;
        else
            return Collections.emptyList();
    }

    @Override
    public AlertSchemaConfiguration findAlert(String alertName) {
        Assert.notNull(alertName);

        ISerializableField<List<AlertSchemaConfiguration>> field = getField(ALERTS_FIELD);
        List<AlertSchemaConfiguration> alerts = field.get();
        if (alerts != null) {
            for (int i = 0; i < alerts.size(); i++) {
                AlertSchemaConfiguration alert = alerts.get(i);
                if (alert.getName().equals(alertName))
                    return alert;
            }
        }

        return null;
    }

    @Override
    public void addAlert(AlertSchemaConfiguration alertConfiguration) {
        Assert.notNull(alertConfiguration);

        IPermission permission = getSchema().getEditAlertsPermission();
        permission.beginCheck(this);

        ISerializableField<List<AlertSchemaConfiguration>> field = getField(ALERTS_FIELD);
        List<AlertSchemaConfiguration> alerts = field.get();
        if (alerts == null)
            alerts = new ArrayList<AlertSchemaConfiguration>();

        boolean set = false;
        for (int i = 0; i < alerts.size(); i++) {
            AlertSchemaConfiguration alert = alerts.get(i);
            if (alert.getName().equals(alertConfiguration.getName())) {
                alerts.set(i, alertConfiguration);
                set = true;
                break;
            }
        }

        if (!set)
            alerts.add(alertConfiguration);

        field.set(alerts);

        invalidateRuleCache();

        permission.endCheck();
    }

    @Override
    public void removeAlert(String alertName) {
        Assert.notNull(alertName);

        IPermission permission = getSchema().getEditAlertsPermission();
        permission.beginCheck(this);

        ISerializableField<List<AlertSchemaConfiguration>> field = getField(ALERTS_FIELD);
        List<AlertSchemaConfiguration> alerts = field.get();
        if (alerts != null) {
            for (int i = 0; i < alerts.size(); i++) {
                AlertSchemaConfiguration alert = alerts.get(i);
                if (alert.getName().equals(alertName)) {
                    alerts.remove(i);
                    break;
                }
            }
        }

        field.set(alerts);

        invalidateRuleCache();

        permission.endCheck();
    }

    @Override
    public void removeAllAlerts() {
        IPermission permission = getSchema().getEditAlertsPermission();
        permission.beginCheck(this);

        ISerializableField<List<AlertSchemaConfiguration>> field = getField(ALERTS_FIELD);
        field.set(null);

        invalidateRuleCache();

        permission.endCheck();
    }

    @Override
    public Iterable<IIncident> getIncidents() {
        IReferenceField<IIncident> field = getField(INCIDENTS_FIELD);
        return field;
    }

    public IIncident findIncident(String name) {
        ensureRuleCache();

        return incidents.get(name);
    }

    public IncidentNode createIncident(IAlert alert, boolean group) {
        IncidentNode incident = getSpace().createNode(null, getSchema().getParent().findNode(group ? "IncidentGroup" : "Incident"));
        incident.init(alert.getConfiguration(), Times.getCurrentTime(), this);

        return incident;
    }

    public void addIncident(IncidentNode incident) {
        IReferenceField<IIncident> field = getField(INCIDENTS_FIELD);
        field.add(incident);

        if (incidents != null) {
            IIncident oldIncident = incidents.put(incident.getName(), incident);
            if (oldIncident != null)
                oldIncident.delete(false);
        }

        incident.logOn();

        addToIncidentGroups(incident);

        if (incident.getRefCount() == 0) {
            ((ComponentRootNode) getSpace().getRootNode()).addIncident(incident);
            getAlertService().send(incident, true, false);
        }

        getAlertService().addIncidentChange(incident.getIncidentId(), incident.getParentIds(), true, Times.getCurrentTime());
    }

    public void removeIncident(IncidentNode incident, boolean resolved) {
        IReferenceField<IIncident> field = getField(INCIDENTS_FIELD);
        field.remove(incident);

        if (incidents != null)
            incidents.remove(incident.getName());

        incident.logOff(resolved);

        if (incident.getRefCount() == 0) {
            ((ComponentRootNode) getSpace().getRootNode()).removeIncident(incident);
            getAlertService().send(incident, false, resolved);
        }

        getAlertService().addIncidentChange(incident.getIncidentId(), incident.getParentIds(), false, Times.getCurrentTime());
    }

    public void removeAllIncidents() {
        List<IIncident> incidents = com.exametrika.common.utils.Collections.toList(getIncidents().iterator());
        for (IIncident incident : incidents)
            incident.delete(false);
    }

    public void addToIncidentGroups(IIncident incident) {
        ensureRuleCache();

        if (!(incident instanceof IIncidentGroup) || incident.getComponent() != this) {
            for (IAlert alert : alerts.values()) {
                if (alert instanceof IIncidentGroupRule)
                    ((IIncidentGroupRule) alert).onIncidentCreated(this, incident);
            }
        }

        for (IGroupComponent group : ((IComponentVersion) getCurrentVersion()).getGroups())
            ((GroupComponentNode) group).addToIncidentGroups(incident);
    }

    @Override
    public Iterable<IJob> getJobs() {
        IReferenceField<IJob> jobs = getField(JOBS_FIELD);
        return new JobIterable(jobs, this);
    }

    @Override
    public JobProxy findJob(String jobName) {
        Assert.notNull(jobName);

        jobName = getQualifiedJobName(jobName);

        IReferenceField<IJob> jobs = getField(JOBS_FIELD);
        for (IJob job : jobs) {
            if (job.getJobSchema().getName().equals(jobName))
                return new JobProxy(job, this);
        }

        return null;
    }

    @Override
    public IJob addJob(JobSchemaConfiguration jobConfiguration) {
        Assert.notNull(jobConfiguration);

        IPermission permission = getSchema().getEditJobsPermission();
        permission.beginCheck(this);

        IReferenceField<IJob> jobs = getField(JOBS_FIELD);
        boolean jobExists = findJob(jobConfiguration.getName()) != null;

        jobConfiguration = createJobConfiguration(jobConfiguration);

        IJobService jobService = getTransaction().findDomainService(IJobService.NAME);
        IJob job = jobService.addJob(jobConfiguration);

        if (!jobExists)
            jobs.add(job);

        permission.endCheck();

        return new JobProxy(job, this);
    }

    @Override
    public void removeJob(String jobName) {
        Assert.notNull(jobName);

        IPermission permission = getSchema().getEditJobsPermission();
        permission.beginCheck(this);

        JobProxy job = findJob(jobName);
        if (job != null) {
            IReferenceField<IJob> jobs = getField(JOBS_FIELD);
            jobs.remove(job.getJob());
            job.delete();
        }

        permission.endCheck();
    }

    @Override
    public void removeAllJobs() {
        IPermission permission = getSchema().getEditJobsPermission();
        permission.beginCheck(this);

        IReferenceField<IJob> jobs = getField(JOBS_FIELD);
        for (IJob job : jobs)
            job.delete();

        jobs.clear();

        permission.endCheck();
    }

    @Override
    public void delete() {
        ComponentVersionNode version = getCurrentVersion();
        if (version.isDeleted())
            return;

        IPermission permission = getSchema().getDeletePermission();
        permission.beginCheck(this);

        doBeforeDelete();

        ComponentVersionNode node = addVersion(false);
        node.setDeleted();

        doAfterDelete(version);

        permission.endCheck();
    }

    @Override
    public final List<String> getTags() {
        ITagField field = getField(TAGS_FIELD);
        return field.get();
    }

    @Override
    public final void setTags(List<String> tags) {
        IPermission permission = getSchema().getEditTagsPermission();
        permission.beginCheck(this);

        ITagField field = getField(TAGS_FIELD);
        field.set(tags);

        permission.endCheck();
    }

    public void addGroup(GroupComponentNode group) {
        ComponentVersionNode node = addVersion();
        node.addGroup(group);
        invalidateRuleCache();

        addVersionChange(this, group, IVersionChangeRecord.Type.ADD);
    }

    public void removeGroup(GroupComponentNode group) {
        ComponentVersionNode node = addVersion();
        node.removeGroup(group);
        invalidateRuleCache();

        addVersionChange(this, group, IVersionChangeRecord.Type.REMOVE);
    }

    public final void log(String action) {
        log(action, (Object[]) null);
    }

    public final void log(String action, Object... parameters) {
        IJsonBlobField log = ensureLog();
        INumericSequenceField sequence = getField(LOG_SEQUENCE_FIELD);

        long id = sequence.getNext();

        Json json = Json.object()
                .put("id", id)
                .put("time", Times.getCurrentTime())
                .put("component", getScope().toString())
                .put("type", getSchema().getConfiguration().getComponent().getName())
                .put("action", action);

        if (parameters != null && parameters.length > 0) {
            if (parameters.length == 1)
                json.put("parameters", JsonUtils.toJson(parameters[0]));
            else
                json.put("parameters", JsonUtils.toJson(parameters));
        }

        JsonObject value = json.toObject();

        log.add(new JsonRecord(value));

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.log(this, value));
    }

    public final long logStart(String action, Object parameters) {
        IJsonBlobField log = ensureLog();
        INumericSequenceField sequence = getField(LOG_SEQUENCE_FIELD);

        long id = sequence.getNext();

        JsonObject value = Json.object()
                .put("id", id)
                .put("time", Times.getCurrentTime())
                .put("component", getScope().toString())
                .put("type", getSchema().getConfiguration().getComponent().getName())
                .put("state", "started")
                .put("action", action)
                .putIf("parameters", parameters, parameters != null)
                .toObject();

        log.add(new JsonRecord(value));

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.log(this, value));

        return id;
    }

    public final void logSuccess(long id, String action, Object result) {
        IJsonBlobField log = ensureLog();

        JsonObject value = Json.object()
                .put("id", id)
                .put("time", Times.getCurrentTime())
                .put("component", getScope().toString())
                .put("type", getSchema().getConfiguration().getComponent().getName())
                .put("state", "succeeded")
                .put("action", action)
                .putIf("result", result, result != null)
                .toObject();

        log.add(new JsonRecord(value));

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.log(this, value));
    }

    public final void logError(long id, String action, Object error) {
        IJsonBlobField log = ensureLog();

        JsonObject value = Json.object()
                .put("id", id)
                .put("time", Times.getCurrentTime())
                .put("component", getScope().toString())
                .put("type", getSchema().getConfiguration().getComponent().getName())
                .put("state", "failed")
                .put("action", action)
                .putIf("error", error, error != null)
                .toObject();

        log.add(new JsonRecord(value));

        if (logger.isLogEnabled(LogLevel.DEBUG))
            logger.log(LogLevel.DEBUG, messages.log(this, value));
    }

    @Override
    public final void onCreated(Object primaryKey, Object[] args) {
        ComponentNodeSchema schema = getSchema();
        IObjectSpace space = getSpace();
        space.createNode(null, schema.getVersion(), this);

        createInitialJobs();
    }

    public void createInitialJobs() {
        ComponentNodeSchema schema = getSchema();
        for (JobSchemaConfiguration job : schema.getConfiguration().getComponent().getJobs().values())
            addJob(job);
    }

    public boolean allowExecution() {
        return !getCurrentVersion().isDeleted();
    }

    @Override
    public final void executeSimpleRules(IAggregationNode aggregationNode, IRuleContext context) {
        if (!allowExecution())
            return;

        ensureRuleCache();

        doExecuteRules(aggregationNode, context);

        for (int i = 0; i < simpleRules.size(); i++)
            simpleRules.get(i).execute(this, aggregationNode, context);
    }

    @Override
    public final void executeComplexRules(Map<String, Object> facts) {
        if (!allowExecution())
            return;

        ensureRuleCache();

        for (int i = 0; i < complexRules.size(); i++)
            complexRules.get(i).execute(this, facts);
    }

    @Override
    public String toString() {
        ComponentNodeSchema schema = getSchema();
        ComponentSchemaConfiguration componentType = schema.getConfiguration().getComponent();

        return getScope().toString() + ":" + componentType.getName() + " - " + super.toString();
    }

    @Override
    public void dump(IJsonHandler json, IDumpContext context) {
        super.dump(json, context);

        json.key("scope");
        json.value(getScope());
        json.key("component");
        json.value(getSchema().getConfiguration().getComponent().getName());

        json.key("versions");
        json.startArray();
        IComponentVersion version = getCurrentVersion();
        while (version != null) {
            json.startObject();
            ((ComponentVersionNode) version).dump(json, context);
            version = version.getPreviousVersion();
            json.endObject();
        }
        json.endArray();

        if (getActionLog() != null)
            getActionLog().dump(json, context);

        boolean jsonRules = false;
        for (RuleSchemaConfiguration rule : getRules()) {
            if (!jsonRules) {
                json.key("rules");
                json.startArray();
                jsonRules = true;
            }

            json.value(rule.toString());
        }

        if (jsonRules)
            json.endArray();

        boolean jsonAlerts = false;
        for (AlertSchemaConfiguration alert : getAlerts()) {
            if (!jsonAlerts) {
                json.key("alerts");
                json.startArray();
                jsonAlerts = true;
            }

            json.value(alert.toString());
        }

        if (jsonAlerts)
            json.endArray();

        boolean jsonIncidents = false;
        for (IIncident alert : getIncidents()) {
            if (!jsonIncidents) {
                json.key("incidents");
                json.startArray();
                jsonIncidents = true;
            }

            json.startObject();
            ((IncidentNode) alert).dump(json, context);
            json.endObject();
        }

        if (jsonIncidents)
            json.endArray();

        List<String> tags = getTags();
        if (tags != null) {
            json.key("tags");
            JsonSerializers.write(json, JsonUtils.toJson(tags));
        }
    }

    @Override
    public void onUnloaded() {
        super.onUnloaded();

        invalidateRuleCache();
        scope = null;
        snapshotVersion = null;
        snapshotTime = 0;
    }

    public final void invalidateRuleCache() {
        simpleRules = null;
        complexRules = null;
        healthChecks = null;
        alerts = null;
        incidents = null;
    }

    protected void doBeforeDelete() {
        invalidateRuleCache();
        removeAllJobs();
        removeAllIncidents();
    }

    protected void doAfterDelete(ComponentVersionNode version) {
        for (IGroupComponent group : version.getGroups())
            ((GroupComponentNode) group).checkDynamicAutoDeletion();

        scope = null;
        snapshotVersion = null;
        snapshotTime = 0;
    }

    protected void doBuildRuleCache(Map<String, ISimpleRule> simpleRules, Map<String, IComplexRule> complexRules,
                                    Map<String, IHealthCheck> healthChecks, Map<String, IAlert> alerts, IDatabaseContext context) {
        for (IGroupComponent group : ((IComponentVersion) getCurrentVersion()).getGroups()) {
            for (RuleSchemaConfiguration ruleConfiguration : group.getGroupRules()) {
                if (!ruleConfiguration.isEnabled())
                    continue;

                IRule rule = ruleConfiguration.createRule(context);
                addRule(rule, simpleRules, complexRules, healthChecks);
            }

            for (AlertSchemaConfiguration alertConfiguration : group.getGroupAlerts()) {
                if (!alertConfiguration.isEnabled())
                    continue;

                IAlert alert = alertConfiguration.createAlert(context);
                if (alert instanceof IRule)
                    addRule((IRule) alert, simpleRules, complexRules, healthChecks);

                alerts.put(alertConfiguration.getName(), alert);
            }
        }
    }

    protected void doExecuteRules(IAggregationNode aggregationNode, IRuleContext context) {
    }

    protected final void ensureRuleCache() {
        if (simpleRules != null)
            return;

        buildRuleCache();
    }

    protected final void addRule(IRule rule, Map<String, ISimpleRule> simpleRules, Map<String, IComplexRule> complexRules,
                                 Map<String, IHealthCheck> healthChecks) {
        if (rule instanceof ISimpleRule)
            simpleRules.put(rule.getName(), (ISimpleRule) rule);
        if (rule instanceof IComplexRule)
            complexRules.put(rule.getName(), (IComplexRule) rule);
        if (rule instanceof IHealthCheck)
            healthChecks.put(rule.getName(), (IHealthCheck) rule);
    }

    protected final void addVersionChange(ComponentNode component, IGroupComponent group, IVersionChangeRecord.Type type) {
        if (component instanceof GroupComponentNode && ((GroupComponentVersionNode) component.getCurrentVersion()).isPredefined())
            return;

        ComponentRootNode root = getSpace().getRootNode();
        IStructuredBlobField<IVersionChangeRecord> versionChanges = root.getVersionChanges();
        ComponentVersionNode current = component.getCurrentVersion();
        ComponentVersionNode prev = (ComponentVersionNode) current.getPreviousVersion();

        long groupScopeId = 0;
        if (group != null && !((GroupComponentVersionNode) group.getCurrentVersion()).isPredefined())
            groupScopeId = group.getScopeId();

        versionChanges.add(new VersionChangeRecord(component.getSchema().getIndex(), current.getTime(), type,
                component.getScopeId(), groupScopeId, current.getId(), prev != null ? prev.getId() : 0));
    }

    protected List<GroupComponentNode> getParentGroups(ComponentVersionNode version) {
        return (List) com.exametrika.common.utils.Collections.toList(version.getGroups().iterator());
    }

    private IScopeName refreshScope() {
        long scopeId = getScopeId();
        if (scopeId == 0)
            return ScopeName.root();

        IPeriodNameManager nameManager = getTransaction().findExtension(IPeriodNameManager.NAME);
        scope = nameManager.findById(scopeId);
        if (scope != null)
            return scope.getName();
        else
            return null;
    }

    private IJsonBlobField ensureLog() {
        ISingleReferenceField<ActionLogNode> field = getField(ACTION_LOG_FIELD);
        ActionLogNode node = field.get();
        if (node == null) {
            node = getSpace().createNode(null, getSchema().getActionLog());
            field.set(node);
        }

        return node.getLogField();
    }

    private ComponentVersionNode addVersion(boolean copyFields) {
        Assert.checkState(!isReadOnly());

        ISingleReferenceField<ComponentVersionNode> field = getField(CURRENT_VERSION_FIELD);
        ComponentVersionNode version = field.get();
        if (!version.isReadOnly())
            return version;

        ComponentVersionNode copy = version.copy(copyFields);
        field.set(copy);

        for (IGroupComponent group : getParentGroups(version))
            addVersionChange(this, group, copyFields ? IVersionChangeRecord.Type.CHANGE : IVersionChangeRecord.Type.REMOVE);

        return copy;
    }

    private void buildRuleCache() {
        NodeSpaceSchema spaceSchema = ((NodeSpaceSchema) getSchema().getParent());
        IDatabaseContext context = spaceSchema.getContext();

        Map<String, ISimpleRule> simpleRules = new HashMap<String, ISimpleRule>();
        Map<String, IComplexRule> complexRules = new HashMap<String, IComplexRule>();
        Map<String, IHealthCheck> healthChecks = new HashMap<String, IHealthCheck>();
        Map<String, IAlert> alerts = new HashMap<String, IAlert>();

        ComponentNodeSchema schema = getSchema();
        for (IRule rule : schema.getRules())
            addRule(rule, simpleRules, complexRules, healthChecks);
        for (IAlert alert : schema.getAlerts()) {
            if (alert instanceof IRule)
                addRule((IRule) alert, simpleRules, complexRules, healthChecks);

            alerts.put(alert.getConfiguration().getName(), alert);
        }

        for (RuleSchemaConfiguration ruleConfiguration : getRules()) {
            if (!ruleConfiguration.isEnabled())
                continue;

            IRule rule = ruleConfiguration.createRule(context);
            addRule(rule, simpleRules, complexRules, healthChecks);
        }

        for (AlertSchemaConfiguration alertConfiguration : getAlerts()) {
            if (!alertConfiguration.isEnabled())
                continue;

            IAlert alert = alertConfiguration.createAlert(context);
            if (alert instanceof IRule)
                addRule((IRule) alert, simpleRules, complexRules, healthChecks);

            alerts.put(alertConfiguration.getName(), alert);
        }

        doBuildRuleCache(simpleRules, complexRules, healthChecks, alerts, context);

        this.simpleRules = new ArrayList<ISimpleRule>(simpleRules.values());
        this.complexRules = new ArrayList<IComplexRule>(complexRules.values());
        this.healthChecks = new ArrayList<IHealthCheck>(healthChecks.values());
        this.alerts = alerts;

        this.incidents = new HashMap<String, IIncident>();
        List<IIncident> incidents = com.exametrika.common.utils.Collections.toList(getIncidents().iterator());
        for (IIncident incident : incidents) {
            if (!alerts.containsKey(incident.getName())) {
                if (!getTransaction().isReadOnly())
                    incident.delete(false);
            } else {
                IIncident oldIncident = this.incidents.put(incident.getName(), incident);
                if (oldIncident != null)
                    oldIncident.delete(false);
            }
        }
    }

    private JobSchemaConfiguration createJobConfiguration(JobSchemaConfiguration jobConfiguration) {
        String jobName = getQualifiedJobName(jobConfiguration.getName());
        Map<String, Object> parameters = new HashMap<String, Object>(jobConfiguration.getParameters());
        parameters.put("component.scopeId", getScopeId());
        if (jobConfiguration instanceof OneTimeJobSchemaConfiguration) {
            OneTimeJobSchemaConfiguration schema = (OneTimeJobSchemaConfiguration) jobConfiguration;
            return new OneTimeJobSchemaConfiguration(jobName, schema.getDescription(), schema.getGroup(), parameters,
                    schema.getOperation(), schema.getSchedule(), schema.isEnabled(), schema.getRestartCount(), schema.getRestartPeriod(),
                    schema.getMaxExecutionPeriod());
        } else if (jobConfiguration instanceof RecurrentJobSchemaConfiguration) {
            RecurrentJobSchemaConfiguration schema = (RecurrentJobSchemaConfiguration) jobConfiguration;
            return new RecurrentJobSchemaConfiguration(jobName, schema.getDescription(), schema.getGroup(), parameters,
                    schema.getOperation(), schema.getSchedule(), schema.isEnabled(), schema.getRestartCount(), schema.getRestartPeriod(),
                    schema.getMaxExecutionPeriod(), schema.getRepeatCount(), schema.getPeriod());
        } else
            return Assert.error();
    }

    protected final AlertService getAlertService() {
        return getTransaction().findDomainService(AlertService.NAME);
    }

    private String getQualifiedJobName(String jobName) {
        String prefix = "component." + getScopeId() + ".";
        if (!jobName.startsWith(prefix))
            jobName = prefix + jobName;
        return jobName;
    }

    private interface IMessages {
        @DefaultMessage("Component ''{0}'' log:\n{1}")
        ILocalizedMessage log(ComponentNode component, JsonObject params);
    }
}