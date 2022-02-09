/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.instrument.instrumentors;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

/**
 * The {@link InstructionCounter} is used to count instructions from the beginning of the current method.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public final class InstructionCounter extends MethodVisitor implements IInstructionCounter {
    private int count;

    public InstructionCounter(MethodVisitor mv) {
        super(Opcodes.ASM5, mv);
    }

    @Override
    public int getCount() {
        return count;
    }

    @Override
    public void visitInsn(int opcode) {
        mv.visitInsn(opcode);

        count++;
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        mv.visitIntInsn(opcode, operand);

        count++;
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        mv.visitVarInsn(opcode, var);

        count++;
    }

    @Override
    public void visitTypeInsn(int opcode, String type) {
        mv.visitTypeInsn(opcode, type);

        count++;
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        mv.visitFieldInsn(opcode, owner, name, desc);

        count++;
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        mv.visitMethodInsn(opcode, owner, name, desc, itf);

        count++;
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
        mv.visitJumpInsn(opcode, label);

        count++;
    }

    @Override
    public void visitLdcInsn(Object cst) {
        mv.visitLdcInsn(cst);

        count++;
    }

    @Override
    public void visitIincInsn(int var, int increment) {
        mv.visitIincInsn(var, increment);

        count++;
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
        mv.visitTableSwitchInsn(min, max, dflt, labels);

        count++;
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
        mv.visitLookupSwitchInsn(dflt, keys, labels);

        count++;
    }

    @Override
    public void visitMultiANewArrayInsn(String desc, int dims) {
        mv.visitMultiANewArrayInsn(desc, dims);

        count++;
    }
}
