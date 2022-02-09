/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.schema;

import java.util.Map;

import com.exametrika.api.exadb.objectdb.INode;
import com.exametrika.api.exadb.objectdb.config.schema.ComputedFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.common.expression.CompileContext;
import com.exametrika.common.expression.Expressions;
import com.exametrika.common.expression.IExpression;
import com.exametrika.impl.exadb.objectdb.fields.ComputedField;
import com.exametrika.spi.exadb.objectdb.fields.IField;
import com.exametrika.spi.exadb.objectdb.fields.IFieldObject;
import com.exametrika.spi.exadb.objectdb.fields.ISimpleField;


/**
 * The {@link ComputedFieldSchema} is a computed field schema.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class ComputedFieldSchema extends SimpleFieldSchema implements IFieldSchema {
    private IExpression expression;
    private Map<String, Object> runtimeContext;

    public ComputedFieldSchema(ComputedFieldSchemaConfiguration configuration, int index, int offset) {
        super(configuration, index, offset);
    }

    public <T> T execute(INode node) {
        if (expression == null)
            compile();

        return expression.execute(node, runtimeContext);
    }

    public <T> T execute(INode node, Map<String, ? extends Object> variables) {
        if (expression == null)
            compile();

        return expression.execute(node, variables);
    }

    @Override
    public ComputedFieldSchemaConfiguration getConfiguration() {
        return (ComputedFieldSchemaConfiguration) configuration;
    }

    @Override
    public IFieldObject createField(IField field) {
        return new ComputedField((ISimpleField) field);
    }

    private void compile() {
        CompileContext compileContext = Expressions.createCompileContext(null);
        this.expression = Expressions.compile(getConfiguration().getExpression(), compileContext);
        runtimeContext = Expressions.createRuntimeContext(null, true);
    }
}
