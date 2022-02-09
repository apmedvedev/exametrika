/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.schema;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import com.exametrika.api.component.config.model.ComponentModelSchemaConfiguration;
import com.exametrika.api.component.config.model.ComponentSchemaConfiguration;
import com.exametrika.api.component.config.model.GroupComponentSchemaConfiguration;
import com.exametrika.api.component.config.schema.ActionLogDocumentSchemaFactoryConfiguration;
import com.exametrika.api.component.config.schema.ActionLogNodeSchemaConfiguration;
import com.exametrika.api.component.config.schema.ActionServiceSchemaConfiguration;
import com.exametrika.api.component.config.schema.AlertServiceSchemaConfiguration;
import com.exametrika.api.component.config.schema.BehaviorTypeNodeSchemaConfiguration;
import com.exametrika.api.component.config.schema.BehaviorTypeServiceSchemaConfiguration;
import com.exametrika.api.component.config.schema.ComponentRootNodeSchemaConfiguration;
import com.exametrika.api.component.config.schema.ComponentServiceSchemaConfiguration;
import com.exametrika.api.component.config.schema.GroupServiceSchemaConfiguration;
import com.exametrika.api.component.config.schema.HealthServiceSchemaConfiguration;
import com.exametrika.api.component.config.schema.IncidentGroupNodeSchemaConfiguration;
import com.exametrika.api.component.config.schema.IncidentNodeSchemaConfiguration;
import com.exametrika.api.component.config.schema.RuleServiceSchemaConfiguration;
import com.exametrika.api.component.config.schema.SelectionServiceSchemaConfiguration;
import com.exametrika.api.component.config.schema.UserInterfaceServiceSchemaConfiguration;
import com.exametrika.api.component.config.schema.VersionChangesFieldSchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.DomainSchemaConfiguration;
import com.exametrika.api.exadb.core.config.schema.ModuleSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.BlobStoreFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.FileFieldSchemaConfiguration.PageType;
import com.exametrika.api.exadb.objectdb.config.schema.IndexType;
import com.exametrika.api.exadb.objectdb.config.schema.IndexedNumericFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.JsonBlobFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.JsonFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.NumericFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.NumericSequenceFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.ObjectSpaceSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.PrimitiveFieldSchemaConfiguration.DataType;
import com.exametrika.api.exadb.objectdb.config.schema.ReferenceFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.SerializableFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.SingleReferenceFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.StringFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.config.schema.TagFieldSchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.spi.exadb.core.config.schema.DomainServiceSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.FieldSchemaConfiguration;
import com.exametrika.spi.exadb.objectdb.config.schema.ObjectNodeSchemaConfiguration;


/**
 * The {@link ComponentSchemaBuilder} is a builder of component database module schema configuration for specified component model schema.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class ComponentSchemaBuilder {
    public void buildSchema(ComponentModelSchemaConfiguration schema, ModuleSchemaConfiguration module) {
        Assert.notNull(schema);

        Set<ObjectNodeSchemaConfiguration> nodes = buildNodes(schema);

        ObjectSpaceSchemaConfiguration componentSpace = new ObjectSpaceSchemaConfiguration("component", "component",
                "Component space.", nodes, "root", 0, 0);

        DomainServiceSchemaConfiguration ruleService = new RuleServiceSchemaConfiguration();
        DomainServiceSchemaConfiguration actionService = new ActionServiceSchemaConfiguration();
        DomainServiceSchemaConfiguration behaviorTypeService = new BehaviorTypeServiceSchemaConfiguration();
        DomainServiceSchemaConfiguration healthService = new HealthServiceSchemaConfiguration();
        DomainServiceSchemaConfiguration alertService = new AlertServiceSchemaConfiguration();
        DomainServiceSchemaConfiguration groupService = new GroupServiceSchemaConfiguration();
        DomainServiceSchemaConfiguration componentService = new ComponentServiceSchemaConfiguration(schema);
        DomainServiceSchemaConfiguration uiService = new UserInterfaceServiceSchemaConfiguration();
        DomainServiceSchemaConfiguration selectionService = new SelectionServiceSchemaConfiguration();

        DomainSchemaConfiguration componentDomain = new DomainSchemaConfiguration("component", "component",
                "Component domain.", Collections.singleton(componentSpace),
                com.exametrika.common.utils.Collections.asSet(ruleService, actionService, behaviorTypeService, healthService,
                        alertService, groupService, componentService, uiService, selectionService));

        module.getSchema().addDomain(componentDomain);
    }

    private Set<ObjectNodeSchemaConfiguration> buildNodes(ComponentModelSchemaConfiguration schema) {
        Set<ComponentSchemaConfiguration> components = schema.getComponents();

        Set<ObjectNodeSchemaConfiguration> nodes = new LinkedHashSet<ObjectNodeSchemaConfiguration>();
        nodes.add(new ComponentRootNodeSchemaConfiguration("root", "root", "Component root node.",
                Arrays.<FieldSchemaConfiguration>asList(
                        new BlobStoreFieldSchemaConfiguration("blobStore", "blobStore", null, 0, Long.MAX_VALUE, null,
                                PageType.SMALL, false, Collections.<String, String>emptyMap(), false),
                        new SingleReferenceFieldSchemaConfiguration("rootGroup", null),
                        new ReferenceFieldSchemaConfiguration("hosts", null),
                        new ReferenceFieldSchemaConfiguration("nodes", null),
                        new ReferenceFieldSchemaConfiguration("transactions", null),
                        new ReferenceFieldSchemaConfiguration("healthComponents", null),
                        new ReferenceFieldSchemaConfiguration("incidents", null),
                        new NumericSequenceFieldSchemaConfiguration("incidentSequence", 1, 1, null),
                        new VersionChangesFieldSchemaConfiguration("versionChanges", "versionChanges", null, "blobStore")
                )));

        boolean rootGroupFound = false;
        for (ComponentSchemaConfiguration component : components) {
            if (component.getName().equals("RootGroup")) {
                Assert.isInstanceOf(GroupComponentSchemaConfiguration.class, component);
                rootGroupFound = true;
                break;
            }
        }

        if (!rootGroupFound)
            new GroupComponentSchemaConfiguration("RootGroup").buildNodeSchemas(nodes);

        nodes.add(new ActionLogNodeSchemaConfiguration("ActionLog", "Action log node.", Arrays.<FieldSchemaConfiguration>asList(
                new JsonBlobFieldSchemaConfiguration("log", "log", null, "blobStore", true, new ActionLogDocumentSchemaFactoryConfiguration()))));

        nodes.add(new BehaviorTypeNodeSchemaConfiguration("BehaviorType", "Behavior type node.", Arrays.<FieldSchemaConfiguration>asList(
                new IndexedNumericFieldSchemaConfiguration("typeId", DataType.INT),
                new StringFieldSchemaConfiguration("name", 128), new JsonFieldSchemaConfiguration("metadata"),
                new TagFieldSchemaConfiguration("tags"))));

        nodes.add(new IncidentNodeSchemaConfiguration("Incident", "Incident node.", Arrays.<FieldSchemaConfiguration>asList(
                new IndexedNumericFieldSchemaConfiguration("id", "id", null, DataType.INT, null, null, null, "incidentSequence", 0,
                        IndexType.BTREE, true, true, false, true, true, false, null, null),
                new StringFieldSchemaConfiguration("name", 128),
                new StringFieldSchemaConfiguration("message", 256),
                new NumericFieldSchemaConfiguration("creationTime", DataType.LONG),
                new SerializableFieldSchemaConfiguration("lastNotificationTimes"),
                new NumericFieldSchemaConfiguration("refCount", DataType.INT),
                new SingleReferenceFieldSchemaConfiguration("component", null),
                new ReferenceFieldSchemaConfiguration("groups", null),
                new TagFieldSchemaConfiguration("tags"))));

        nodes.add(new IncidentGroupNodeSchemaConfiguration("IncidentGroup", "Incident group node.", Arrays.<FieldSchemaConfiguration>asList(
                new IndexedNumericFieldSchemaConfiguration("id", "id", null, DataType.INT, null, null, null, "incidentSequence", 0,
                        IndexType.BTREE, true, true, false, true, true, false, null, null),
                new StringFieldSchemaConfiguration("name", 128),
                new StringFieldSchemaConfiguration("message", 256),
                new NumericFieldSchemaConfiguration("creationTime", DataType.LONG),
                new SerializableFieldSchemaConfiguration("lastNotificationTimes"),
                new NumericFieldSchemaConfiguration("refCount", DataType.INT),
                new SingleReferenceFieldSchemaConfiguration("component", null),
                new ReferenceFieldSchemaConfiguration("groups", null),
                new TagFieldSchemaConfiguration("tags"),
                new ReferenceFieldSchemaConfiguration("children", null))));

        for (ComponentSchemaConfiguration component : components)
            component.buildNodeSchemas(nodes);

        return nodes;
    }
}
