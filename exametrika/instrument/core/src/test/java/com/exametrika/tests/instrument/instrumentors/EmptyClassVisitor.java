/**
 * Copyright 2014 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.instrument.instrumentors;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class EmptyClassVisitor extends ClassVisitor {
    private AnnotationVisitor av = new EmptyAnnotationVisitor();

    public EmptyClassVisitor() {
        super(Opcodes.ASM5);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        return av;
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        return new FieldVisitor(Opcodes.ASM5) {

            @Override
            public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
                return av;
            }
        };
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        return new EmptyMethodVisitor();
    }
}