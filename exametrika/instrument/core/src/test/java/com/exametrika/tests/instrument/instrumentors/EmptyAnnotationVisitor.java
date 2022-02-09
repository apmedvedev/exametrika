/**
 * Copyright 2014 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.instrument.instrumentors;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Opcodes;

public class EmptyAnnotationVisitor extends AnnotationVisitor {

    public EmptyAnnotationVisitor() {
        super(Opcodes.ASM5);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String name, String desc) {
        return this;
    }

    @Override
    public AnnotationVisitor visitArray(String name) {
        return this;
    }
}