/**
 * Copyright 2014 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.instrument.instrumentors;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class EmptyMethodVisitor extends MethodVisitor {
    private AnnotationVisitor av = new EmptyAnnotationVisitor();

    public EmptyMethodVisitor() {
        super(Opcodes.ASM5);
    }

    @Override
    public AnnotationVisitor visitAnnotationDefault() {
        return av;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        return av;
    }

    @Override
    public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
        return av;
    }
}