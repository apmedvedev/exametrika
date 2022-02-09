/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.fields;

import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;

import com.exametrika.api.exadb.objectdb.config.schema.TextFieldSchemaConfiguration;
import com.exametrika.api.exadb.objectdb.fields.ITextField;
import com.exametrika.spi.exadb.objectdb.fields.ISimpleField;


/**
 * The {@link TextField} is a text blob field.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class TextField extends StreamBlobField implements ITextField {
    public TextField(ISimpleField field) {
        super(field);
    }

    @Override
    public <T> T get() {
        return (T) createReader();
    }

    @Override
    public Reader createReader() {
        return new InputStreamReader(createInputStream());
    }

    @Override
    public Writer createWriter() {
        return new BufferedWriter(new OutputStreamWriter(createOutputStream()));
    }

    @Override
    protected boolean isCompressed() {
        return ((TextFieldSchemaConfiguration) getSchema().getConfiguration()).isCompressed();
    }
}
