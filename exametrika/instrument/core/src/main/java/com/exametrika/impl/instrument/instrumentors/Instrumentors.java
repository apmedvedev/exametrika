/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.instrument.instrumentors;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;

import com.exametrika.api.instrument.InstrumentationException;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.IOs;


/**
 * The {@link Instrumentors} contains various utility methods for instrumentors.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class Instrumentors {
    private static final IMessages messages = Messages.get(IMessages.class);

    public static String getMethodSignature(Constructor member) {
        StringBuilder builder = new StringBuilder();
        builder.append("<init>");
        builder.append('(');

        boolean first = true;
        Class[] parameterTypes = member.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            if (first)
                first = false;
            else
                builder.append(',');

            builder.append(parameterTypes[i].getSimpleName());
        }

        builder.append(')');

        return builder.toString();
    }

    public static String getMethodSignature(Method member) {
        StringBuilder builder = new StringBuilder();
        builder.append(member.getName());
        builder.append('(');

        boolean first = true;
        Class[] parameterTypes = member.getParameterTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            if (first)
                first = false;
            else
                builder.append(',');

            builder.append(parameterTypes[i].getSimpleName());
        }

        builder.append(')');
        builder.append(':');
        builder.append(member.getReturnType().getSimpleName());

        return builder.toString();
    }

    public static String getMethodSignature(String methodName, String methodDescriptor) {
        Type methodType = Type.getMethodType(methodDescriptor);
        StringBuilder builder = new StringBuilder();
        builder.append(methodName);
        builder.append('(');

        boolean first = true;
        Type[] parameterTypes = methodType.getArgumentTypes();
        for (int i = 0; i < parameterTypes.length; i++) {
            if (first)
                first = false;
            else
                builder.append(',');

            builder.append(getSimpleName(parameterTypes[i].getClassName()));
        }

        builder.append(')');
        builder.append(':');
        builder.append(getSimpleName(methodType.getReturnType().getClassName()));

        return builder.toString();
    }

    public static void addSuperTypes(Set<String> superTypes, Class<?> type, boolean addSelf) {
        if (addSelf)
            superTypes.add(type.getName());

        Class superClass = type.getSuperclass();
        if (superClass != null)
            addSuperTypes(superTypes, superClass, true);

        for (Class superInterface : type.getInterfaces())
            addSuperTypes(superTypes, superInterface, true);
    }

    public static void addSuperTypes(Set<String> superTypes, String typeName, boolean addSelf, ClassLoader classLoader) {
        if (addSelf)
            superTypes.add(Type.getObjectType(typeName).getClassName());

        ClassReader typeInfo = getTypeInfo(typeName, classLoader);

        String superClass = typeInfo.getSuperName();
        if (superClass != null)
            addSuperTypes(superTypes, superClass, true, classLoader);

        String[] superInterfaces = typeInfo.getInterfaces();
        if (superInterfaces != null) {
            for (int i = 0; i < superInterfaces.length; i++)
                addSuperTypes(superTypes, superInterfaces[i], true, classLoader);
        }
    }

    private static String getSimpleName(String className) {
        int pos = className.lastIndexOf('.');
        return className.substring(pos + 1);
    }

    private static ClassReader getTypeInfo(String type, ClassLoader classLoader) {
        InputStream is;
        if (classLoader != null)
            is = classLoader.getResourceAsStream(type + ".class");
        else
            is = ClassLoader.getSystemResourceAsStream(type + ".class");

        if (is != null) {
            try {
                return new ClassReader(is);
            } catch (IOException e) {
                throw new InstrumentationException(e);
            } finally {
                IOs.close(is);
            }
        } else
            throw new InstrumentationException(messages.resourceNotFound(type + ".class"));
    }

    private Instrumentors() {
    }

    private interface IMessages {
        @DefaultMessage("Class resource ''{0}'' is not found.")
        ILocalizedMessage resourceNotFound(String resourceName);
    }
}
