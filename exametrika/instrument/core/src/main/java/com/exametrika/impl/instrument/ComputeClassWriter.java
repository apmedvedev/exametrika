/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.instrument;

import java.io.IOException;
import java.io.InputStream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import com.exametrika.api.instrument.InstrumentationException;
import com.exametrika.common.l10n.DefaultMessage;
import com.exametrika.common.l10n.ILocalizedMessage;
import com.exametrika.common.l10n.Messages;
import com.exametrika.common.utils.IOs;

/**
 * The {@link ComputeClassWriter} represents an class writer that uses class reader to compute common super class.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class ComputeClassWriter extends ClassWriter {
    private static final IMessages messages = Messages.get(IMessages.class);
    private final ClassLoader classLoader;

    public ComputeClassWriter(ClassReader classReader, int flags, ClassLoader classLoader) {
        super(classReader, flags);

        this.classLoader = classLoader;
    }

    @Override
    protected String getCommonSuperClass(String type1, String type2) {
        try {
            ClassReader info1 = getTypeInfo(type1);
            ClassReader info2 = getTypeInfo(type2);
            if ((info1.getAccess() & Opcodes.ACC_INTERFACE) != 0) {
                if (getTypeImplements(type2, info2, type1))
                    return type1;
                if ((info2.getAccess() & Opcodes.ACC_INTERFACE) != 0) {
                    if (getTypeImplements(type1, info1, type2))
                        return type2;
                }
                return "java/lang/Object";
            }

            if ((info2.getAccess() & Opcodes.ACC_INTERFACE) != 0) {
                if (getTypeImplements(type1, info1, type2))
                    return type2;
                else
                    return "java/lang/Object";
            }

            StringBuilder b1 = getTypeAncestors(type1, info1);
            StringBuilder b2 = getTypeAncestors(type2, info2);

            String result = "java/lang/Object";

            int end1 = b1.length();
            int end2 = b2.length();

            while (true) {
                int start1 = b1.lastIndexOf(";", end1 - 1);
                int start2 = b2.lastIndexOf(";", end2 - 1);
                if (start1 != -1 && start2 != -1 && end1 - start1 == end2 - start2) {
                    String p1 = b1.substring(start1 + 1, end1);
                    String p2 = b2.substring(start2 + 1, end2);
                    if (p1.equals(p2)) {
                        result = p1;
                        end1 = start1;
                        end2 = start2;
                    } else
                        return result;
                } else
                    return result;
            }
        } catch (IOException e) {
            throw new InstrumentationException(e);
        }
    }

    private StringBuilder getTypeAncestors(String type, ClassReader info) throws IOException {
        StringBuilder b = new StringBuilder();
        while (!"java/lang/Object".equals(type)) {
            b.append(';').append(type);
            type = info.getSuperName();
            info = getTypeInfo(type);
        }
        return b;
    }

    private boolean getTypeImplements(String type, ClassReader info, String interfaceName) throws IOException {
        while (!"java/lang/Object".equals(type)) {
            String[] interfaces = info.getInterfaces();
            for (int i = 0; i < interfaces.length; ++i) {
                if (interfaces[i].equals(interfaceName))
                    return true;
            }

            for (int i = 0; i < interfaces.length; ++i) {
                if (getTypeImplements(interfaces[i], getTypeInfo(interfaces[i]), interfaceName))
                    return true;
            }

            type = info.getSuperName();
            info = getTypeInfo(type);
        }
        return false;
    }

    private ClassReader getTypeInfo(String type) throws IOException {
        InputStream is;
        if (classLoader != null)
            is = classLoader.getResourceAsStream(type + ".class");
        else
            is = ClassLoader.getSystemResourceAsStream(type + ".class");

        if (is != null) {
            try {
                return new ClassReader(is);
            } finally {
                IOs.close(is);
            }
        } else
            throw new InstrumentationException(messages.resourceNotFound(type + ".class"));
    }

    private interface IMessages {
        @DefaultMessage("Could not compute common superclass. types: {0}, {1}, class: {2}.")
        ILocalizedMessage error(String type1, String type2, String className);

        @DefaultMessage("Class resource ''{0}'' is not found.")
        ILocalizedMessage resourceNotFound(String resourceName);
    }
}
