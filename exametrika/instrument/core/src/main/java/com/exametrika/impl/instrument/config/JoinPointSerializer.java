/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.instrument.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.exametrika.api.instrument.IJoinPoint;
import com.exametrika.api.instrument.config.ArrayGetPointcut;
import com.exametrika.api.instrument.config.ArraySetPointcut;
import com.exametrika.api.instrument.config.CallPointcut;
import com.exametrika.api.instrument.config.CatchPointcut;
import com.exametrika.api.instrument.config.ClassFilter;
import com.exametrika.api.instrument.config.ClassNameFilter;
import com.exametrika.api.instrument.config.FieldGetPointcut;
import com.exametrika.api.instrument.config.FieldSetPointcut;
import com.exametrika.api.instrument.config.InterceptPointcut;
import com.exametrika.api.instrument.config.LinePointcut;
import com.exametrika.api.instrument.config.MemberFilter;
import com.exametrika.api.instrument.config.MemberNameFilter;
import com.exametrika.api.instrument.config.MonitorInterceptPointcut;
import com.exametrika.api.instrument.config.NewArrayPointcut;
import com.exametrika.api.instrument.config.NewObjectPointcut;
import com.exametrika.api.instrument.config.Pointcut;
import com.exametrika.api.instrument.config.QualifiedMemberNameFilter;
import com.exametrika.api.instrument.config.QualifiedMethodFilter;
import com.exametrika.api.instrument.config.ThrowPointcut;
import com.exametrika.common.io.IDeserialization;
import com.exametrika.common.io.ISerialization;
import com.exametrika.common.io.impl.AbstractSerializer;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.Serializers;
import com.exametrika.spi.instrument.config.InterceptorConfiguration;


/**
 * The {@link JoinPointSerializer} is a serializer of {@link JoinPoint}.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class JoinPointSerializer extends AbstractSerializer {
    private static final UUID ID = UUID.fromString("dcea3d58-270b-4565-a803-171dea2349d3");

    public JoinPointSerializer() {
        super(ID, JoinPoint.class);
    }

    @Override
    public Object deserialize(IDeserialization deserialization, UUID id) {
        IJoinPoint.Kind kind = Serializers.readEnum(deserialization, IJoinPoint.Kind.class);
        int joinPointId = deserialization.readInt();
        String className = deserialization.readString();
        String methodName = deserialization.readString();
        String methodSignature = deserialization.readString();
        String calledClassName = deserialization.readString();
        String calledMemberName = deserialization.readString();
        String calledMethodSignature = deserialization.readString();
        int lineNumber = deserialization.readInt();
        int overloadNumber = deserialization.readInt();
        String sourceFileName = deserialization.readString();
        String sourceDebug = deserialization.readString();
        Pointcut pointcut = readPointcut(deserialization);

        return new JoinPoint(kind, joinPointId, 0, className, methodName, methodSignature, overloadNumber, pointcut, calledClassName,
                calledMemberName, calledMethodSignature, sourceFileName, sourceDebug, lineNumber);
    }

    @Override
    public void serialize(ISerialization serialization, Object object) {
        JoinPoint joinPoint = (JoinPoint) object;
        Serializers.writeEnum(serialization, joinPoint.getKind());
        serialization.writeInt(joinPoint.getId());
        serialization.writeString(joinPoint.getClassName());
        serialization.writeString(joinPoint.getMethodName());
        serialization.writeString(joinPoint.getMethodSignature());
        serialization.writeString(joinPoint.getCalledClassName());
        serialization.writeString(joinPoint.getCalledMemberName());
        serialization.writeString(joinPoint.getCalledMethodSignature());
        serialization.writeInt(joinPoint.getSourceLineNumber());
        serialization.writeInt(joinPoint.getOverloadNumber());
        serialization.writeString(joinPoint.getSourceFileName());
        serialization.writeString(joinPoint.getSourceDebug());
        writePointcut(serialization, joinPoint.getPointcut());
    }

    private Pointcut readPointcut(IDeserialization deserialization) {
        String pointcutClassName = deserialization.readString();
        QualifiedMethodFilter methodFilter = readQualifiedMethodFilter(deserialization);
        String name = deserialization.readString();
        InterceptorConfiguration interceptor = deserialization.readObject();
        boolean singleton = deserialization.readBoolean();
        int priority = deserialization.readInt();

        if (pointcutClassName.equals(ArrayGetPointcut.class.getName())) {
            boolean useParams = deserialization.readBoolean();
            return new ArrayGetPointcut(name, methodFilter, interceptor, useParams, singleton);
        } else if (pointcutClassName.equals(ArraySetPointcut.class.getName())) {
            boolean useParams = deserialization.readBoolean();
            return new ArraySetPointcut(name, methodFilter, interceptor, useParams, singleton);
        } else if (pointcutClassName.equals(CallPointcut.class.getName())) {
            boolean useParams = deserialization.readBoolean();
            QualifiedMemberNameFilter calledMethodFilter = readQualifiedMemberNameFilter(deserialization);
            return new CallPointcut(name, methodFilter, interceptor, calledMethodFilter, useParams, singleton, priority);
        } else if (pointcutClassName.equals(CatchPointcut.class.getName())) {
            ClassNameFilter exceptionClassFilter = readClassNameFilter(deserialization);
            return new CatchPointcut(name, methodFilter, interceptor, exceptionClassFilter, singleton);
        } else if (pointcutClassName.equals(FieldGetPointcut.class.getName())) {
            boolean useParams = deserialization.readBoolean();
            QualifiedMemberNameFilter fieldFilter = readQualifiedMemberNameFilter(deserialization);
            return new FieldGetPointcut(name, methodFilter, interceptor, fieldFilter, useParams, singleton);
        } else if (pointcutClassName.equals(FieldSetPointcut.class.getName())) {
            boolean useParams = deserialization.readBoolean();
            QualifiedMemberNameFilter fieldFilter = readQualifiedMemberNameFilter(deserialization);
            return new FieldSetPointcut(name, methodFilter, interceptor, fieldFilter, useParams, singleton);
        } else if (pointcutClassName.equals(InterceptPointcut.class.getName())) {
            boolean useParams = deserialization.readBoolean();
            Set<InterceptPointcut.Kind> kinds = Serializers.readEnumSet(deserialization, InterceptPointcut.Kind.class);
            return new InterceptPointcut(name, methodFilter, kinds, interceptor, useParams, singleton, priority);
        } else if (pointcutClassName.equals(LinePointcut.class.getName())) {
            int startLine = deserialization.readInt();
            int endLine = deserialization.readInt();
            return new LinePointcut(name, methodFilter, interceptor, startLine, endLine, singleton);
        } else if (pointcutClassName.equals(MonitorInterceptPointcut.class.getName())) {
            Set<MonitorInterceptPointcut.Kind> kinds = Serializers.readEnumSet(deserialization, MonitorInterceptPointcut.Kind.class);
            return new MonitorInterceptPointcut(name, methodFilter, kinds, interceptor, singleton);
        } else if (pointcutClassName.equals(NewArrayPointcut.class.getName())) {
            ClassNameFilter elementClassFilter = readClassNameFilter(deserialization);
            return new NewArrayPointcut(name, methodFilter, interceptor, elementClassFilter, singleton);
        } else if (pointcutClassName.equals(NewObjectPointcut.class.getName())) {
            ClassNameFilter newInstanceClassFilter = readClassNameFilter(deserialization);
            return new NewObjectPointcut(name, methodFilter, interceptor, newInstanceClassFilter, singleton);
        } else if (pointcutClassName.equals(ThrowPointcut.class.getName()))
            return new ThrowPointcut(name, methodFilter, interceptor, singleton);
        else
            Assert.error();

        return null;
    }

    private void writePointcut(ISerialization serialization, Pointcut pointcut) {
        serialization.writeString(pointcut.getClass().getName());
        writeQualifiedMethodFilter(serialization, pointcut.getMethodFilter());
        serialization.writeString(pointcut.getName());
        serialization.writeObject(pointcut.getInterceptor());
        serialization.writeBoolean(pointcut.isSingleton());
        serialization.writeInt(pointcut.getPriority());

        if (pointcut instanceof ArrayGetPointcut)
            serialization.writeBoolean(((ArrayGetPointcut) pointcut).getUseParams());
        else if (pointcut instanceof ArraySetPointcut)
            serialization.writeBoolean(((ArraySetPointcut) pointcut).getUseParams());
        else if (pointcut instanceof CallPointcut) {
            CallPointcut callPointcut = (CallPointcut) pointcut;
            serialization.writeBoolean(callPointcut.getUseParams());
            writeQualifiedMemberNameFilter(serialization, callPointcut.getCalledMethodFilter());
        } else if (pointcut instanceof CatchPointcut)
            writeClassNameFilter(serialization, ((CatchPointcut) pointcut).getExceptionClassFilter());
        else if (pointcut instanceof FieldGetPointcut) {
            FieldGetPointcut fieldPointcut = (FieldGetPointcut) pointcut;
            serialization.writeBoolean(fieldPointcut.getUseParams());
            writeQualifiedMemberNameFilter(serialization, fieldPointcut.getFieldFilter());
        } else if (pointcut instanceof FieldSetPointcut) {
            FieldSetPointcut fieldPointcut = (FieldSetPointcut) pointcut;
            serialization.writeBoolean(fieldPointcut.getUseParams());
            writeQualifiedMemberNameFilter(serialization, fieldPointcut.getFieldFilter());
        } else if (pointcut.getClass().equals(InterceptPointcut.class)) {
            InterceptPointcut interceptPointcut = (InterceptPointcut) pointcut;
            serialization.writeBoolean(interceptPointcut.getUseParams());
            Serializers.writeEnumSet(serialization, interceptPointcut.getKinds());
        } else if (pointcut instanceof LinePointcut) {
            serialization.writeInt(((LinePointcut) pointcut).getStartLine());
            serialization.writeInt(((LinePointcut) pointcut).getEndLine());
        } else if (pointcut instanceof MonitorInterceptPointcut)
            Serializers.writeEnumSet(serialization, ((MonitorInterceptPointcut) pointcut).getKinds());
        else if (pointcut instanceof NewArrayPointcut)
            writeClassNameFilter(serialization, ((NewArrayPointcut) pointcut).getElementClassFilter());
        else if (pointcut instanceof NewObjectPointcut)
            writeClassNameFilter(serialization, ((NewObjectPointcut) pointcut).getNewInstanceClassFilter());
        else if (pointcut instanceof ThrowPointcut)
            ;
        else
            Assert.error();
    }

    private QualifiedMethodFilter readQualifiedMethodFilter(IDeserialization deserialization) {
        if (!deserialization.readBoolean())
            return null;

        ClassFilter classFilter = readClassFilter(deserialization);
        MemberFilter methodFilter = readMemberFilter(deserialization);

        int count = deserialization.readInt();
        List<QualifiedMethodFilter> includeMethods = null;
        if (count > 0) {
            includeMethods = new ArrayList<QualifiedMethodFilter>(count);
            for (int i = 0; i < count; i++)
                includeMethods.add(readQualifiedMethodFilter(deserialization));
        }

        count = deserialization.readInt();
        List<QualifiedMethodFilter> excludeMethods = null;
        if (count > 0) {
            excludeMethods = new ArrayList<QualifiedMethodFilter>(count);
            for (int i = 0; i < count; i++)
                excludeMethods.add(readQualifiedMethodFilter(deserialization));
        }

        int minInstruction = deserialization.readInt();
        int maxInstruction = deserialization.readInt();

        return new QualifiedMethodFilter(classFilter, methodFilter, includeMethods, excludeMethods, minInstruction, maxInstruction);
    }

    private void writeQualifiedMethodFilter(ISerialization serialization, QualifiedMethodFilter methodFilter) {
        serialization.writeBoolean(methodFilter != null);

        if (methodFilter != null) {
            writeClassFilter(serialization, methodFilter.getClassFilter());
            writeMemberFilter(serialization, methodFilter.getMethodFilter());

            if (methodFilter.getIncludeMethods() != null) {
                serialization.writeInt(methodFilter.getIncludeMethods().size());
                for (QualifiedMethodFilter filter : methodFilter.getIncludeMethods())
                    writeQualifiedMethodFilter(serialization, filter);
            } else
                serialization.writeInt(0);

            if (methodFilter.getExcludeMethods() != null) {
                serialization.writeInt(methodFilter.getExcludeMethods().size());
                for (QualifiedMethodFilter filter : methodFilter.getExcludeMethods())
                    writeQualifiedMethodFilter(serialization, filter);
            } else
                serialization.writeInt(0);

            serialization.writeInt(methodFilter.getMinInstruction());
            serialization.writeInt(methodFilter.getMaxInstruction());
        }
    }

    private QualifiedMemberNameFilter readQualifiedMemberNameFilter(IDeserialization deserialization) {
        if (!deserialization.readBoolean())
            return null;

        ClassNameFilter classNameFilter = readClassNameFilter(deserialization);
        MemberNameFilter memberNameFilter = readMemberNameFilter(deserialization);

        int count = deserialization.readInt();
        List<QualifiedMemberNameFilter> includeMethods = null;
        if (count > 0) {
            includeMethods = new ArrayList<QualifiedMemberNameFilter>(count);
            for (int i = 0; i < count; i++)
                includeMethods.add(readQualifiedMemberNameFilter(deserialization));
        }

        count = deserialization.readInt();
        List<QualifiedMemberNameFilter> excludeMethods = null;
        if (count > 0) {
            excludeMethods = new ArrayList<QualifiedMemberNameFilter>(count);
            for (int i = 0; i < count; i++)
                excludeMethods.add(readQualifiedMemberNameFilter(deserialization));
        }

        return new QualifiedMemberNameFilter(classNameFilter, memberNameFilter, includeMethods, excludeMethods);
    }

    private void writeQualifiedMemberNameFilter(ISerialization serialization, QualifiedMemberNameFilter memberFilter) {
        serialization.writeBoolean(memberFilter != null);

        if (memberFilter != null) {
            writeClassNameFilter(serialization, memberFilter.getClassNameFilter());
            writeMemberNameFilter(serialization, memberFilter.getMemberNameFilter());

            if (memberFilter.getIncludeMembers() != null) {
                serialization.writeInt(memberFilter.getIncludeMembers().size());
                for (QualifiedMemberNameFilter filter : memberFilter.getIncludeMembers())
                    writeQualifiedMemberNameFilter(serialization, filter);
            } else
                serialization.writeInt(0);

            if (memberFilter.getExcludeMembers() != null) {
                serialization.writeInt(memberFilter.getExcludeMembers().size());
                for (QualifiedMemberNameFilter filter : memberFilter.getExcludeMembers())
                    writeQualifiedMemberNameFilter(serialization, filter);
            } else
                serialization.writeInt(0);
        }
    }

    private ClassFilter readClassFilter(IDeserialization deserialization) {
        if (!deserialization.readBoolean())
            return null;

        ClassNameFilter classNameFilter = readClassNameFilter(deserialization);
        boolean includeSubclasses = deserialization.readBoolean();

        int count = deserialization.readInt();
        List<ClassNameFilter> annotations = null;
        if (count > 0) {
            annotations = new ArrayList<ClassNameFilter>(count);
            for (int i = 0; i < count; i++)
                annotations.add(readClassNameFilter(deserialization));
        }

        count = deserialization.readInt();
        List<ClassFilter> includeClasses = null;
        if (count > 0) {
            includeClasses = new ArrayList<ClassFilter>(count);
            for (int i = 0; i < count; i++)
                includeClasses.add(readClassFilter(deserialization));
        }

        count = deserialization.readInt();
        List<ClassFilter> excludeClasses = null;
        if (count > 0) {
            excludeClasses = new ArrayList<ClassFilter>(count);
            for (int i = 0; i < count; i++)
                excludeClasses.add(readClassFilter(deserialization));
        }

        return new ClassFilter(classNameFilter, includeSubclasses, annotations, includeClasses, excludeClasses);
    }

    private void writeClassFilter(ISerialization serialization, ClassFilter classFilter) {
        serialization.writeBoolean(classFilter != null);

        if (classFilter != null) {
            writeClassNameFilter(serialization, classFilter.getClassNameFilter());
            serialization.writeBoolean(classFilter.isIncludeSubclasses());

            if (classFilter.getAnnotations() != null) {
                serialization.writeInt(classFilter.getAnnotations().size());
                for (ClassNameFilter filter : classFilter.getAnnotations())
                    writeClassNameFilter(serialization, filter);
            } else
                serialization.writeInt(0);

            if (classFilter.getIncludeClasses() != null) {
                serialization.writeInt(classFilter.getIncludeClasses().size());
                for (ClassFilter filter : classFilter.getIncludeClasses())
                    writeClassFilter(serialization, filter);
            } else
                serialization.writeInt(0);

            if (classFilter.getExcludeClasses() != null) {
                serialization.writeInt(classFilter.getExcludeClasses().size());
                for (ClassFilter filter : classFilter.getExcludeClasses())
                    writeClassFilter(serialization, filter);
            } else
                serialization.writeInt(0);
        }
    }

    private ClassNameFilter readClassNameFilter(IDeserialization deserialization) {
        if (!deserialization.readBoolean())
            return null;

        String classNameExpression = deserialization.readString();

        int count = deserialization.readInt();
        List<ClassNameFilter> includeClasses = null;
        if (count > 0) {
            includeClasses = new ArrayList<ClassNameFilter>(count);
            for (int i = 0; i < count; i++)
                includeClasses.add(readClassNameFilter(deserialization));
        }

        count = deserialization.readInt();
        List<ClassNameFilter> excludeClasses = null;
        if (count > 0) {
            excludeClasses = new ArrayList<ClassNameFilter>(count);
            for (int i = 0; i < count; i++)
                excludeClasses.add(readClassNameFilter(deserialization));
        }

        return new ClassNameFilter(classNameExpression, includeClasses, excludeClasses);
    }

    private void writeClassNameFilter(ISerialization serialization, ClassNameFilter classFilter) {
        serialization.writeBoolean(classFilter != null);

        if (classFilter != null) {
            serialization.writeString(classFilter.getClassNameExpression());

            if (classFilter.getIncludeClasses() != null) {
                serialization.writeInt(classFilter.getIncludeClasses().size());
                for (ClassNameFilter filter : classFilter.getIncludeClasses())
                    writeClassNameFilter(serialization, filter);
            } else
                serialization.writeInt(0);

            if (classFilter.getExcludeClasses() != null) {
                serialization.writeInt(classFilter.getExcludeClasses().size());
                for (ClassNameFilter filter : classFilter.getExcludeClasses())
                    writeClassNameFilter(serialization, filter);
            } else
                serialization.writeInt(0);
        }
    }

    private MemberFilter readMemberFilter(IDeserialization deserialization) {
        if (!deserialization.readBoolean())
            return null;

        MemberNameFilter memberNameFilter = readMemberNameFilter(deserialization);

        int count = deserialization.readInt();
        List<ClassNameFilter> annotations = null;
        if (count > 0) {
            annotations = new ArrayList<ClassNameFilter>(count);
            for (int i = 0; i < count; i++)
                annotations.add(readClassNameFilter(deserialization));
        }

        count = deserialization.readInt();
        List<MemberFilter> includeMembers = null;
        if (count > 0) {
            includeMembers = new ArrayList<MemberFilter>(count);
            for (int i = 0; i < count; i++)
                includeMembers.add(readMemberFilter(deserialization));
        }

        count = deserialization.readInt();
        List<MemberFilter> excludeMembers = null;
        if (count > 0) {
            excludeMembers = new ArrayList<MemberFilter>(count);
            for (int i = 0; i < count; i++)
                excludeMembers.add(readMemberFilter(deserialization));
        }

        return new MemberFilter(memberNameFilter, annotations, includeMembers, excludeMembers);
    }

    private void writeMemberFilter(ISerialization serialization, MemberFilter memberFilter) {
        serialization.writeBoolean(memberFilter != null);

        if (memberFilter != null) {
            writeMemberNameFilter(serialization, memberFilter.getMemberNameFilter());

            if (memberFilter.getAnnotations() != null) {
                serialization.writeInt(memberFilter.getAnnotations().size());
                for (ClassNameFilter filter : memberFilter.getAnnotations())
                    writeClassNameFilter(serialization, filter);
            } else
                serialization.writeInt(0);

            if (memberFilter.getIncludeMembers() != null) {
                serialization.writeInt(memberFilter.getIncludeMembers().size());
                for (MemberFilter filter : memberFilter.getIncludeMembers())
                    writeMemberFilter(serialization, filter);
            } else
                serialization.writeInt(0);

            if (memberFilter.getExcludeMembers() != null) {
                serialization.writeInt(memberFilter.getExcludeMembers().size());
                for (MemberFilter filter : memberFilter.getExcludeMembers())
                    writeMemberFilter(serialization, filter);
            } else
                serialization.writeInt(0);
        }
    }

    private MemberNameFilter readMemberNameFilter(IDeserialization deserialization) {
        if (!deserialization.readBoolean())
            return null;

        String memberNameExpression = deserialization.readString();

        int count = deserialization.readInt();
        List<MemberNameFilter> includeMembers = null;
        if (count > 0) {
            includeMembers = new ArrayList<MemberNameFilter>(count);
            for (int i = 0; i < count; i++)
                includeMembers.add(readMemberNameFilter(deserialization));
        }

        count = deserialization.readInt();
        List<MemberNameFilter> excludeMembers = null;
        if (count > 0) {
            excludeMembers = new ArrayList<MemberNameFilter>(count);
            for (int i = 0; i < count; i++)
                excludeMembers.add(readMemberNameFilter(deserialization));
        }

        return new MemberNameFilter(memberNameExpression, includeMembers, excludeMembers);
    }

    private void writeMemberNameFilter(ISerialization serialization, MemberNameFilter memberFilter) {
        serialization.writeBoolean(memberFilter != null);

        if (memberFilter != null) {
            serialization.writeString(memberFilter.getMemberNameExpression());

            if (memberFilter.getIncludeMembers() != null) {
                serialization.writeInt(memberFilter.getIncludeMembers().size());
                for (MemberNameFilter filter : memberFilter.getIncludeMembers())
                    writeMemberNameFilter(serialization, filter);
            } else
                serialization.writeInt(0);

            if (memberFilter.getExcludeMembers() != null) {
                serialization.writeInt(memberFilter.getExcludeMembers().size());
                for (MemberNameFilter filter : memberFilter.getExcludeMembers())
                    writeMemberNameFilter(serialization, filter);
            } else
                serialization.writeInt(0);
        }
    }
}
