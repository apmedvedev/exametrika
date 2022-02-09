/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.services;

import gnu.trove.list.TIntList;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.exametrika.api.component.config.AlertServiceConfiguration;
import com.exametrika.api.component.config.model.AlertChannelSchemaConfiguration;
import com.exametrika.api.component.config.model.AlertRecipientSchemaConfiguration;
import com.exametrika.api.component.nodes.IIncident;
import com.exametrika.api.exadb.objectdb.schema.IObjectSpaceSchema;
import com.exametrika.api.exadb.security.ISecurityService;
import com.exametrika.api.exadb.security.ISubject;
import com.exametrika.api.exadb.security.IUser;
import com.exametrika.api.exadb.security.IUserGroup;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.services.Services;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Collections;
import com.exametrika.common.utils.Strings;
import com.exametrika.impl.component.nodes.ComponentNode;
import com.exametrika.impl.component.nodes.ComponentRootNode;
import com.exametrika.impl.component.nodes.IncidentNode;
import com.exametrika.spi.aggregator.common.meters.MeterExpressions;
import com.exametrika.spi.component.AlertMessage;
import com.exametrika.spi.component.AlertRecipient;
import com.exametrika.spi.component.IAlert;
import com.exametrika.spi.component.IAlertChannel;
import com.exametrika.spi.component.schema.IAlertChannelSchema;
import com.exametrika.spi.exadb.core.DomainService;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.spi.exadb.core.config.DomainServiceConfiguration;


/**
 * The {@link AlertService} is a alert service implementation.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are thread safe.
 */
public final class AlertService extends DomainService {
    public static final String NAME = "component.AlertService";
    private static final IMessages messages = Messages.get(IMessages.class);
    private static final ILogger logger = Loggers.get(AlertService.class);
    private AlertServiceConfiguration configuration = new AlertServiceConfiguration();
    private final Map<String, IAlertChannel> channels = new LinkedHashMap<String, IAlertChannel>();
    private long lastScheduleTime;
    private IObjectSpaceSchema spaceSchema;
    private ISecurityService securityService;
    private Deque<IncidentChange> incidentsChanges = new ArrayDeque<IncidentChange>();

    public static class IncidentChange {
        public final long id;
        public final TIntList parentIds;
        public final boolean add;
        public final long time;

        public IncidentChange(long id, TIntList parentIds, boolean add, long time) {
            this.id = id;
            this.parentIds = parentIds;
            this.add = add;
            this.time = time;
        }
    }

    public AlertService() {
        List<IAlertChannel> channels = Services.loadProviders(IAlertChannel.class);
        for (IAlertChannel channel : channels)
            this.channels.put(channel.getName(), channel);
    }

    @Override
    public DomainServiceConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public void setConfiguration(DomainServiceConfiguration configuration, boolean clearCache) {
        if (configuration == null)
            configuration = new AlertServiceConfiguration();

        this.configuration = (AlertServiceConfiguration) configuration;

        for (IAlertChannel channel : channels.values())
            channel.setConfiguration(this.configuration.getChannels().get(channel.getName()));

        if (clearCache)
            clearCaches();
    }

    @Override
    public void start(IDatabaseContext context) {
        this.context = context;
        this.lastScheduleTime = context.getTimeService().getCurrentTime();
        for (IAlertChannel channel : channels.values())
            channel.start();
    }

    @Override
    public void stop() {
        for (IAlertChannel channel : channels.values())
            channel.stop();
    }

    @Override
    public void clearCaches() {
        spaceSchema = null;
    }

    @Override
    public void onTimer(long currentTime) {
        if (currentTime > lastScheduleTime + configuration.getSchedulePeriod()) {
            ensureSpace();

            Map<IAlertChannel, List<AlertMessage>> messagesMap = new LinkedHashMap<IAlertChannel, List<AlertMessage>>();
            ComponentRootNode root = spaceSchema.getSpace().getRootNode();
            for (IIncident inc : Collections.toList(root.getIncidents().iterator())) {
                IncidentNode incident = (IncidentNode) inc;
                IAlert alert = incident.getAlert();
                if (alert == null)
                    continue;

                for (int i = 0; i < alert.getChannels().size(); i++) {
                    IAlertChannelSchema channelSchema = alert.getChannels().get(i);
                    if (channelSchema.getPeriod() == null || channelSchema.getStatusTemplate() == null)
                        continue;
                    if (channelSchema.getSchedule() != null && !channelSchema.getSchedule().evaluate(currentTime))
                        continue;

                    long lastNotificationTime = incident.getLastNotificationTimes()[i];
                    if (!channelSchema.getPeriod().evaluate(lastNotificationTime, currentTime))
                        continue;

                    incident.setLastNotificationTime(i, currentTime);
                    incident.logStatus();
                    buildMessage(channelSchema, incident, AlertMessage.Type.STATUS, false, messagesMap);
                }
            }

            if (!messagesMap.isEmpty())
                send(messagesMap);

            lastScheduleTime = currentTime;
        }
    }

    public void send(IIncident incident, boolean on, boolean resolved) {
        long currentTime = context.getTimeService().getCurrentTime();

        Map<IAlertChannel, List<AlertMessage>> messagesMap = new LinkedHashMap<IAlertChannel, List<AlertMessage>>();
        IAlert alert = ((IncidentNode) incident).getAlert();
        if (alert == null)
            return;

        for (int i = 0; i < alert.getChannels().size(); i++) {
            IAlertChannelSchema channelSchema = alert.getChannels().get(i);
            ((IncidentNode) incident).setLastNotificationTime(i, currentTime);

            buildMessage(channelSchema, (IncidentNode) incident, on ? AlertMessage.Type.ON : AlertMessage.Type.OFF, resolved, messagesMap);
        }

        send(messagesMap);
    }

    public IncidentChange getFirstIncidentChange() {
        return incidentsChanges.peekFirst();
    }

    public Iterable<IncidentChange> getIncidentChanges() {
        return incidentsChanges;
    }

    public void addIncidentChange(long id, TIntList parentIds, boolean add, long time) {
        incidentsChanges.addLast(new IncidentChange(id, parentIds, add, time));
        if (incidentsChanges.size() > 1000)
            incidentsChanges.removeFirst();
    }

    private void buildMessage(IAlertChannelSchema channelSchema, IncidentNode incident, AlertMessage.Type type, boolean resolved,
                              Map<IAlertChannel, List<AlertMessage>> messagesMap) {
        AlertChannelSchemaConfiguration configuration = channelSchema.getConfiguration();
        final IAlertChannel channel = channels.get(configuration.getName());
        if (channel == null) {
            if (logger.isLogEnabled(LogLevel.WARNING))
                logger.log(LogLevel.WARNING, messages.channelNotFound(configuration.getName()));
            return;
        }

        String text = "";
        incident.setResolved(resolved);
        switch (type) {
            case ON:
                text = channelSchema.getOnTemplate().execute(incident, MeterExpressions.getRuntimeContext());
                break;
            case OFF:
                if (channelSchema.getOffTemplate() != null)
                    text = channelSchema.getOffTemplate().execute(incident, MeterExpressions.getRuntimeContext());
                break;
            case STATUS:
                if (channelSchema.getStatusTemplate() != null)
                    text = channelSchema.getStatusTemplate().execute(incident, MeterExpressions.getRuntimeContext());
                break;
            default:
                Assert.error();
        }

        if (Strings.isEmpty(text))
            return;

        List<AlertMessage> alertMessages = messagesMap.get(channel);
        if (alertMessages == null) {
            alertMessages = new ArrayList<AlertMessage>();
            messagesMap.put(channel, alertMessages);
        }

        List<AlertRecipient> recipients = buildRecipients(configuration, incident);
        if (!recipients.isEmpty())
            alertMessages.add(new AlertMessage(type, configuration, recipients, text));
        else if (logger.isLogEnabled(LogLevel.WARNING))
            logger.log(LogLevel.WARNING, messages.recipientsNotFound());
    }

    private List<AlertRecipient> buildRecipients(AlertChannelSchemaConfiguration configuration, IIncident incident) {
        Map<String, AlertRecipient> recipients = new LinkedHashMap<String, AlertRecipient>();
        for (AlertRecipientSchemaConfiguration recipient : configuration.getRecipients())
            buildRecipients(configuration.getName(), recipient, incident, recipients);

        return new ArrayList(recipients.values());
    }

    private void buildRecipients(String channelName, AlertRecipientSchemaConfiguration schema, IIncident incident,
                                 Map<String, AlertRecipient> recipients) {
        ensureSecurityService();

        switch (schema.getType()) {
            case ADDRESS:
                recipients.put(schema.getAddress(), new AlertRecipient(schema.getName(), schema.getAddress()));
                break;
            case USER:
                buildRecipients(channelName, securityService.findUser(schema.getName()), recipients);
                break;
            case USER_GROUP:
                buildRecipients(channelName, securityService.findUserGroup(schema.getName()), recipients);
                break;
            case ROLE:
                ComponentNode component = (ComponentNode) incident.getComponent();
                Set<ISubject> subjects = securityService.findSubjects(schema.getName(), component.getSchema().getViewPermission(), component);
                for (ISubject subject : subjects)
                    buildRecipients(channelName, subject, recipients);
                break;
            default:
                Assert.error();
        }
    }

    private void buildRecipients(String channelName, ISubject subject, Map<String, AlertRecipient> recipients) {
        if (subject == null)
            return;

        JsonObject metadata = subject.getMetadata();
        if (metadata != null) {
            String address = metadata.select("contacts?." + channelName + "?", null);
            if (address != null) {
                String name = subject.getDescription() != null ? subject.getDescription() : subject.getName();
                recipients.put(address, new AlertRecipient(name, address));
                return;
            }
        }

        if (subject instanceof IUserGroup) {
            IUserGroup group = (IUserGroup) subject;
            for (IUserGroup child : group.getChildren())
                buildRecipients(channelName, child, recipients);
            for (IUser user : group.getUsers())
                buildRecipients(channelName, user, recipients);
        }
    }

    private void send(final Map<IAlertChannel, List<AlertMessage>> messagesMap) {
        context.getCompartment().execute(new Runnable() {
            @Override
            public void run() {
                for (Map.Entry<IAlertChannel, List<AlertMessage>> entry : messagesMap.entrySet())
                    entry.getKey().send(entry.getValue());
            }
        });
    }

    private void ensureSpace() {
        if (spaceSchema == null) {
            spaceSchema = schema.getParent().findSpace("component");
            Assert.notNull(spaceSchema);
        }
    }

    private void ensureSecurityService() {
        securityService = context.getTransactionProvider().getTransaction().findDomainService(ISecurityService.NAME);
    }

    private interface IMessages {
        @DefaultMessage("Alert recipients are not found.")
        ILocalizedMessage recipientsNotFound();

        @DefaultMessage("Alert channel ''{0}'' is not found.")
        ILocalizedMessage channelNotFound(String name);
    }
}
