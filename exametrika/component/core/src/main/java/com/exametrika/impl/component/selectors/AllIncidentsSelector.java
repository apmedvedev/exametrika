/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.selectors;

import java.util.Map;

import com.exametrika.api.component.IComponentService;
import com.exametrika.api.component.nodes.IComponent;
import com.exametrika.api.component.nodes.IIncident;
import com.exametrika.api.component.nodes.IIncidentGroup;
import com.exametrika.api.component.schema.ISelectorSchema;
import com.exametrika.api.exadb.objectdb.INodeIndex;
import com.exametrika.api.exadb.objectdb.IObjectSpace;
import com.exametrika.api.exadb.objectdb.schema.IObjectSpaceSchema;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonObjectBuilder;
import com.exametrika.common.json.JsonUtils;
import com.exametrika.impl.component.nodes.ComponentVersionNode;
import com.exametrika.impl.component.services.AlertService;
import com.exametrika.impl.component.services.AlertService.IncidentChange;
import com.exametrika.impl.component.services.ComponentService;
import com.exametrika.spi.component.Selectors;
import com.exametrika.spi.component.Selectors.StateInfo;


/**
 * The {@link AllIncidentsSelector} is an all incidents selector.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class AllIncidentsSelector extends Selector {
    public AllIncidentsSelector(IComponent component, ISelectorSchema schema) {
        super(component, schema);
    }

    @Override
    protected Object doSelect(Map<String, ?> parameters) {
        ComponentService componentService = getSchema().getContext().getTransactionProvider().getTransaction().findDomainService(IComponentService.NAME);
        AlertService alertService = getSchema().getContext().getTransactionProvider().getTransaction().findDomainService(AlertService.NAME);
        long lastUpdateTime = ((Number) parameters.get("lastNotificationUpdateTime")).longValue();
        long currentTime = ((Number) parameters.get("currentTime")).longValue();

        IncidentChange firstChange = alertService.getFirstIncidentChange();

        Json json = Json.object();
        if (lastUpdateTime == 0 || (firstChange != null && lastUpdateTime < firstChange.time)) {
            Json rows = json.put("fullUpdate", true).putArray("rows");
            for (IIncident incident : componentService.getIncidents())
                buildIncident(0, incident, rows.addObject(), true);
        } else {
            IObjectSpaceSchema spaceSchema = componentService.getObjectSpaceSchema();

            IObjectSpace space = componentService.getObjectSpaceSchema().getSpace();
            INodeIndex<Long, IIncident> index = space.getIndex(spaceSchema.findNode("Incident").getPrimaryField());
            Json rows = json.put("incrementalStructuredUpdate", true).putArray("rows");

            for (IncidentChange change : alertService.getIncidentChanges()) {
                if (change.time > lastUpdateTime && change.time <= currentTime) {
                    if (change.add) {
                        IIncident incident = index.find(change.id);
                        if (incident != null) {
                            for (int i = 0; i < change.parentIds.size(); i++) {
                                long parentId = change.parentIds.get(i);
                                buildIncident(parentId, incident, rows.addObject()
                                        .put("id", (parentId != 0 ? (Long.toString(parentId) + "-") : "") + change.id)
                                        .putIf("parentId", Long.toString(parentId), parentId != 0)
                                        .put("showNotification", parentId == 0)
                                        .put("type", "add")
                                        .put("time", change.time)
                                        .putObject("value"), false);
                            }
                        }
                    } else {
                        for (int i = 0; i < change.parentIds.size(); i++) {
                            long parentId = change.parentIds.get(i);
                            rows.addObject()
                                    .put("id", (parentId != 0 ? (Long.toString(parentId) + "-") : "") + change.id)
                                    .put("type", "remove")
                                    .put("time", change.time);
                        }
                    }
                }
            }
        }

        return json.toObjectBuilder();
    }

    private void buildIncident(long parentGroupIncidentId, IIncident incident, Json json, boolean processGroup) {
        ComponentVersionNode version = (ComponentVersionNode) incident.getComponent().getCurrentVersion();

        StateInfo info = Selectors.buildState(version, true, null, null, 0, null);
        JsonObjectBuilder reference = Selectors.buildReference(version);
        reference.put("state", info.state);

        json.put("id", (parentGroupIncidentId != 0 ? (Long.toString(parentGroupIncidentId) + "-") : "") + incident.getIncidentId())
                .put("elementId", Integer.toString(incident.getIncidentId()))
                .putObject("title")
                .put("title", incident.getName())
                .put("description", incident.getAlert().getConfiguration().getDescription())
                .put("link", "#")
                .end()
                .put("time", incident.getCreationTime())
                .put("message", incident.getMessage())
                .put("componentId", incident.getComponent().getScopeId())
                .put("component", reference)
                .putIf("tags", JsonUtils.toJson(incident.getTags()), incident.getTags() != null);

        if (processGroup && incident instanceof IIncidentGroup) {
            Json children = json.putArray("children");
            for (IIncident child : ((IIncidentGroup) incident).getChildren())
                buildIncident(incident.getIncidentId(), child, children.addObject(), processGroup);
        }
    }
}
