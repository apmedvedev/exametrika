/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.instrument.instrumentors;

import static org.objectweb.asm.Opcodes.AALOAD;
import static org.objectweb.asm.Opcodes.AASTORE;
import static org.objectweb.asm.Opcodes.ANEWARRAY;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ATHROW;
import static org.objectweb.asm.Opcodes.BALOAD;
import static org.objectweb.asm.Opcodes.BASTORE;
import static org.objectweb.asm.Opcodes.CALOAD;
import static org.objectweb.asm.Opcodes.CASTORE;
import static org.objectweb.asm.Opcodes.DALOAD;
import static org.objectweb.asm.Opcodes.DASTORE;
import static org.objectweb.asm.Opcodes.DRETURN;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.FALOAD;
import static org.objectweb.asm.Opcodes.FASTORE;
import static org.objectweb.asm.Opcodes.FRETURN;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.IALOAD;
import static org.objectweb.asm.Opcodes.IASTORE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.IRETURN;
import static org.objectweb.asm.Opcodes.LALOAD;
import static org.objectweb.asm.Opcodes.LASTORE;
import static org.objectweb.asm.Opcodes.LRETURN;
import static org.objectweb.asm.Opcodes.MONITORENTER;
import static org.objectweb.asm.Opcodes.MONITOREXIT;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.NEWARRAY;
import static org.objectweb.asm.Opcodes.RETURN;
import static org.objectweb.asm.Opcodes.SALOAD;
import static org.objectweb.asm.Opcodes.SASTORE;
import static org.objectweb.asm.Opcodes.T_BOOLEAN;
import static org.objectweb.asm.Opcodes.T_BYTE;
import static org.objectweb.asm.Opcodes.T_CHAR;
import static org.objectweb.asm.Opcodes.T_DOUBLE;
import static org.objectweb.asm.Opcodes.T_FLOAT;
import static org.objectweb.asm.Opcodes.T_INT;
import static org.objectweb.asm.Opcodes.T_LONG;
import static org.objectweb.asm.Opcodes.T_SHORT;

import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;

import com.exametrika.api.instrument.config.Pointcut;
import com.exametrika.common.utils.Assert;


/**
 * The {@link MethodInstrumentorAdapter} represents a method instrumentor adapter.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are not thread safe.
 */
public class MethodInstrumentorAdapter extends GeneratorAdapter {
    private static final String JAVA_LANG_THROWABLE = Type.getInternalName(Throwable.class);
    private static final Type OBJECT_TYPE = Type.getType(Object.class);
    protected final String className;
    protected final String superName;
    protected final String methodName;
    protected final String methodSignature;
    protected final Set<String> annotations = new LinkedHashSet<String>();
    protected Set<Pointcut> pointcuts;
    protected final Class clazz;
    private boolean enterCalled;
    private Label start = new Label();
    private Label end = new Label();
    private Deque<NewInstanceInfo> newInstanceInfos = new LinkedList<NewInstanceInfo>();
    private List<TryCatchBlock> tryCatchBlocks = new ArrayList<TryCatchBlock>();
    private int lastLineNumber;
    protected boolean disabled;
    private boolean constructorCalled;

    public MethodInstrumentorAdapter(String className, String superName, String methodName, String methodSignature,
                                     int access, String desc, MethodVisitor mv, Set<Pointcut> pointcuts, Class clazz) {
        super(Opcodes.ASM5, mv, access, methodName, desc);

        Assert.notNull(methodName);
        Assert.notNull(methodSignature);
        Assert.notNull(pointcuts);

        this.className = className;
        this.superName = superName;
        this.methodName = methodName;
        this.methodSignature = methodSignature;
        this.pointcuts = pointcuts;
        this.clazz = clazz;
    }

    public int getLastLineNumber() {
        return lastLineNumber;
    }

    public void visitTryCatchBlockNoCache(Label start, Label end, Label handler, String type) {
        super.visitTryCatchBlock(start, end, handler, type);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        annotations.add(Type.getType(desc).getClassName());

        return mv.visitAnnotation(desc, visible);
    }

    @Override
    public void visitInsn(int opcode) {
        if (disabled) {
            super.visitInsn(opcode);
            return;
        }

        boolean arrayload = false;
        boolean arraystore = false;
        Type elementType = null;

        switch (opcode) {
            case DUP:
                if (newInstanceInfos.peek() != null)
                    newInstanceInfos.peek().dupCalled = true;
                break;
            case IALOAD:
                arrayload = true;
                elementType = Type.INT_TYPE;
                break;
            case FALOAD:
                arrayload = true;
                elementType = Type.FLOAT_TYPE;
                break;
            case AALOAD:
                arrayload = true;
                elementType = OBJECT_TYPE;
                break;
            case BALOAD:
                arrayload = true;
                elementType = Type.BOOLEAN_TYPE;
                break;
            case CALOAD:
                arrayload = true;
                elementType = Type.CHAR_TYPE;
                break;
            case SALOAD:
                arrayload = true;
                elementType = Type.SHORT_TYPE;
                break;
            case LALOAD:
                arrayload = true;
                elementType = Type.LONG_TYPE;
                break;
            case DALOAD:
                arrayload = true;
                elementType = Type.DOUBLE_TYPE;
                break;
            case IASTORE:
                arraystore = true;
                elementType = Type.INT_TYPE;
                break;
            case FASTORE:
                arraystore = true;
                elementType = Type.FLOAT_TYPE;
                break;
            case AASTORE:
                arraystore = true;
                elementType = OBJECT_TYPE;
                break;
            case BASTORE:
                arraystore = true;
                elementType = Type.BOOLEAN_TYPE;
                break;
            case CASTORE:
                arraystore = true;
                elementType = Type.CHAR_TYPE;
                break;
            case SASTORE:
                arraystore = true;
                elementType = Type.SHORT_TYPE;
                break;
            case LASTORE:
                arraystore = true;
                elementType = Type.LONG_TYPE;
                break;
            case DASTORE:
                arraystore = true;
                elementType = Type.DOUBLE_TYPE;
                break;
            case ATHROW:
                onThrow();
                break;
            case IRETURN:
            case ARETURN:
            case FRETURN:
            case LRETURN:
            case DRETURN:
            case RETURN:
                if (isConstructor()) {
                    constructorCalled = true;
                    doEnter();
                }

                if (isReturnExitIntercepted())
                    onReturnExit(opcode != RETURN);
                break;
        }

        if (arrayload)
            onBeforeArrayGet();
        else if (arraystore)
            onArraySet(elementType);

        if (opcode == MONITOREXIT)
            onMonitorBeforeExit();

        if (opcode == MONITORENTER)
            onMonitorBeforeEnter();

        super.visitInsn(opcode);

        if (arrayload)
            onAfterArrayGet(elementType);

        if (opcode == MONITORENTER)
            onMonitorAfterEnter();

        if (opcode == MONITOREXIT)
            onMonitorAfterExit();
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
        super.visitIntInsn(opcode, operand);

        if (disabled)
            return;

        if (opcode == NEWARRAY)
            onArrayNew(getArrayType(operand));
    }

    @Override
    public void visitTypeInsn(int opcode, String descriptor) {
        super.visitTypeInsn(opcode, descriptor);

        if (disabled)
            return;

        if (opcode == ANEWARRAY)
            onArrayNew(Type.getObjectType(descriptor).getClassName());
        else if (opcode == NEW) {
            NewInstanceInfo info = new NewInstanceInfo();
            info.className = Type.getObjectType(descriptor).getClassName();
            newInstanceInfos.addFirst(info);
        }
    }

    @Override
    public void visitMultiANewArrayInsn(String descriptor, int dimensions) {
        super.visitMultiANewArrayInsn(descriptor, dimensions);

        if (disabled)
            return;

        onArrayNew(Type.getObjectType(descriptor).getElementType().getClassName());
    }

    @Override
    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
        if (disabled) {
            super.visitTryCatchBlock(start, end, handler, type);
            return;
        }

        onTryCatchBlock(handler, type);

        if (isCallIntercepted()) {
            TryCatchBlock block = new TryCatchBlock();
            block.start = start;
            block.end = end;
            block.handler = handler;
            block.type = type;
            tryCatchBlocks.add(block);
        } else
            super.visitTryCatchBlock(start, end, handler, type);
    }

    @Override
    public void visitLabel(Label label) {
        super.visitLabel(label);

        if (disabled)
            return;

        onLabel(label);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        if (disabled) {
            super.visitFieldInsn(opcode, owner, name, desc);
            return;
        }

        boolean get;
        if (opcode == GETFIELD || opcode == GETSTATIC)
            get = true;
        else
            get = false;

        if (get)
            onBeforeFieldGet(opcode, owner, name, desc);
        else {
            if (!isConstructor() || constructorCalled)
                onFieldSet(opcode, owner, name, desc);
        }

        super.visitFieldInsn(opcode, owner, name, desc);

        if (get)
            onAfterFieldGet(opcode, owner, name, desc);
    }

    @Override
    public void visitLineNumber(int line, Label start) {
        if (disabled) {
            super.visitLineNumber(line, start);
            return;
        }

        lastLineNumber = line;

        onLine(line);

        super.visitLineNumber(line, start);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        if (disabled) {
            super.visitMethodInsn(opcode, owner, name, desc, itf);
            return;
        }

        String ownerClassName = Type.getObjectType(owner).getClassName();

        if (isCallIntercepted() && (!isConstructor() ||
                !(name.equals("<init>") && (ownerClassName.equals(className) || (superName != null && ownerClassName.equals(superName)))))) {
            onBeforeCall(opcode, owner, name, desc);

            super.visitMethodInsn(opcode, owner, name, desc, itf);

            onAfterCall(opcode, owner, name, desc);
        } else
            super.visitMethodInsn(opcode, owner, name, desc, itf);

        NewInstanceInfo newInstanceInfo = newInstanceInfos.peek();

        if (isConstructor() && !enterCalled) {
            boolean skip = false;
            if (opcode == INVOKESPECIAL && newInstanceInfo != null) {
                if (newInstanceInfo.className.equals(className) || (superName != null && newInstanceInfo.className.equals(superName)))
                    skip = true;
            }
            if (!skip && name.equals("<init>") &&
                    (ownerClassName.equals(className) || (superName != null && ownerClassName.equals(superName)))) {
                constructorCalled = true;
                doEnter();
            }
        }

        if (opcode == INVOKESPECIAL && newInstanceInfo != null && ownerClassName.equals(newInstanceInfo.className) && name.equals("<init>")) {
            if (newInstanceInfo.dupCalled)
                onObjectNew(newInstanceInfo.className);
            newInstanceInfos.removeFirst();
        }
    }

    @Override
    public void visitCode() {
        if (disabled) {
            super.visitCode();
            return;
        }

        if (!isConstructor())
            doEnter();

        super.visitCode();
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
        if (disabled) {
            super.visitMaxs(maxStack, maxLocals);
            return;
        }

        for (TryCatchBlock block : tryCatchBlocks)
            super.visitTryCatchBlock(block.start, block.end, block.handler, block.type);

        if (isThrowExitIntercepted()) {
            visitLabel(end);

            super.visitTryCatchBlock(start, end, end, JAVA_LANG_THROWABLE);

            onThrowExit();

            visitInsn(ATHROW);
        }
        super.visitMaxs(maxStack, maxLocals);
    }

    @Override
    public void loadThis() {
        if (isConstructor() && !constructorCalled)
            push((Type) null);
        else
            super.loadThis();
    }

    private void doEnter() {
        if (enterCalled)
            return;

        enterCalled = true;

        onEnter();

        if (isThrowExitIntercepted())
            visitLabel(start);
    }

    private boolean isConstructor() {
        return methodName.equals("<init>");
    }

    private String getArrayType(int typeCode) {
        switch (typeCode) {
            case T_BOOLEAN:
                return "boolean";
            case T_CHAR:
                return "char";
            case T_FLOAT:
                return "float";
            case T_DOUBLE:
                return "double";
            case T_BYTE:
                return "byte";
            case T_SHORT:
                return "short";
            case T_INT:
                return "int";
            case T_LONG:
                return "long";
            default:
                return Assert.error();
        }
    }

    protected boolean isThrowExitIntercepted() {
        return false;
    }

    protected boolean isReturnExitIntercepted() {
        return false;
    }

    protected boolean isCallIntercepted() {
        return false;
    }

    protected void onEnter() {
    }

    protected void onReturnExit(boolean hasRetVal) {
    }

    protected void onThrowExit() {
    }

    protected void onTryCatchBlock(Label label, String catchType) {
    }

    protected void onLabel(Label label) {
    }

    protected void onMonitorBeforeEnter() {
    }

    protected void onMonitorAfterEnter() {
    }

    protected void onMonitorBeforeExit() {
    }

    protected void onMonitorAfterExit() {
    }

    protected void onBeforeCall(int opcode, String owner, String name, String descriptor) {
    }

    protected void onAfterCall(int opcode, String owner, String name, String descriptor) {
    }

    protected void onThrow() {
    }

    protected void onObjectNew(String newInstanceClassName) {
    }

    protected void onArrayNew(String elementClassName) {
    }

    protected void onBeforeFieldGet(int opcode, String owner, String name, String descriptor) {
    }

    protected void onAfterFieldGet(int opcode, String owner, String name, String descriptor) {
    }

    protected void onFieldSet(int opcode, String owner, String name, String descriptor) {
    }

    protected void onBeforeArrayGet() {
    }

    protected void onAfterArrayGet(Type type) {
    }

    protected void onArraySet(Type type) {
    }

    protected void onLine(int line) {
    }

    private static class TryCatchBlock {
        private Label start;
        private Label end;
        private Label handler;
        private String type;
    }

    private static class NewInstanceInfo {
        private String className;
        private boolean dupCalled;
    }
}
