/**
 * Copyright 2008 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.component.nodes;

import com.exametrika.api.component.nodes.IActionLog;
import com.exametrika.api.exadb.core.IDumpContext;
import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.fields.IJsonBlobField;
import com.exametrika.api.exadb.objectdb.fields.IJsonRecord;
import com.exametrika.common.json.IJsonHandler;
import com.exametrika.common.json.JsonSerializers;
import com.exametrika.impl.exadb.objectdb.ObjectNodeObject;


/**
 * The {@link ActionLogNode} is a component node object.
 *
 * @author Medvedev_A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class ActionLogNode extends ObjectNodeObject implements IActionLog {
    private static final int LOG_FIELD = 0;

    public ActionLogNode(INode node) {
        super(node);
    }

    public IJsonBlobField getLogField() {
        return getField(LOG_FIELD);
    }

    @Override
    public Iterable<IJsonRecord> getLog() {
        IJsonBlobField field = getField(LOG_FIELD);
        return field.getRecords();
    }

    @Override
    public void dump(IJsonHandler json, IDumpContext context) {
        super.dump(null, context);

        json.key("actionLog");
        json.startArray();

        IJsonBlobField field = getLogField();
        for (IJsonRecord record : field.getRecords())
            JsonSerializers.write(json, record.getValue());

        json.endArray();
    }
}