/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.nodes;

import java.util.List;

import com.exametrika.api.component.nodes.IIncident;
import com.exametrika.api.component.nodes.IIncidentGroup;
import com.exametrika.api.exadb.core.IDumpContext;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.fields.IReferenceField;
import com.exametrika.common.json.IJsonHandler;
import com.exametrika.common.utils.Assert;


/**
 * The {@link IncidentGroupNode} is an incident node object.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class IncidentGroupNode extends IncidentNode implements IIncidentGroup {
    private static final int CHILDREN_FIELD = 9;

    public IncidentGroupNode(INode node) {
        super(node);
    }

    @Override
    public Iterable<IIncident> getChildren() {
        IReferenceField<IIncident> field = getField(CHILDREN_FIELD);
        return new IncidentIterable(field);
    }

    public void addChild(IIncident incident) {
        Assert.notNull(incident);

        IReferenceField<IIncident> field = getField(CHILDREN_FIELD);
        field.add(incident);

        ((IncidentNode) incident).addGroup(this);
    }

    public void removeChild(IIncident incident, boolean resolved) {
        Assert.notNull(incident);

        IReferenceField<IIncident> field = getField(CHILDREN_FIELD);
        field.remove(incident);

        if (!field.iterator().hasNext())
            delete(resolved);
    }

    @Override
    public void dump(IJsonHandler json, IDumpContext context) {
        super.dump(json, context);

        boolean jsonChildren = false;
        for (IIncident incident : getChildren()) {
            if (!jsonChildren) {
                json.key("children");
                json.startArray();
                jsonChildren = true;
            }

            json.value(incident.toString());
        }

        if (jsonChildren)
            json.endArray();
    }

    @Override
    protected void doDelete(boolean resolved) {
        List<IIncident> incidents = com.exametrika.common.utils.Collections.toList(getChildren().iterator());
        for (IIncident incident : incidents)
            ((IncidentNode) incident).removeGroup(this, resolved);

        super.doDelete(resolved);
    }
}