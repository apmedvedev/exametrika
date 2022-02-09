/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.instrument;

import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.exametrika.api.instrument.InstrumentationException;
import com.exametrika.api.instrument.config.ClassFilter;
import com.exametrika.common.json.Json;
import com.exametrika.common.json.JsonArray;
import com.exametrika.common.json.JsonObject;
import com.exametrika.common.log.ILogger;
import com.exametrika.common.log.LogLevel;
import com.exametrika.common.log.Loggers;
import com.exametrika.common.services.Services;
import com.exametrika.common.services.config.ServiceProviderConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.impl.instrument.config.InstrumentationConfigurationLoader;
import com.exametrika.impl.instrument.instrumentors.Instrumentors;
import com.exametrika.impl.instrument.instrumentors.SkipInstrumentationException;
import com.exametrika.spi.instrument.IClassTransformerExtension;


/**
 * The {@link FieldInjector} represents a class transformer which injects fields into target classes.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class FieldInjector implements IClassTransformerExtension {
    private static final ILogger logger = Loggers.get(FieldInjector.class);
    private final List<FieldInfo> fieldInfos = new ArrayList<FieldInfo>();
    private volatile boolean attached;

    public FieldInjector() {
        loadConfiguration();
    }

    @Override
    public void setAttached(boolean value) {
        attached = value;
    }

    @Override
    public byte[] transform(ClassLoader classLoader, String className, Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain, byte[] classFileBuffer) throws IllegalClassFormatException {
        if (attached)
            return null;

        className = Type.getObjectType(className).getClassName();

        List<FieldInfo> fieldInfos = match(className, this.fieldInfos);
        if (fieldInfos == null)
            return null;

        try {
            ClassReader classReader = new ClassReader(classFileBuffer);
            ClassWriter classWriter = new ClassWriter(classReader, 0);
            FieldInstrumentor fieldInstrumentor = new FieldInstrumentor(classWriter, fieldInfos, classLoader);

            classReader.accept(fieldInstrumentor, 0);

            return classWriter.toByteArray();
        } catch (SkipInstrumentationException e) {
            return null;
        }
    }

    private void loadConfiguration() {
        InstrumentationConfigurationLoader loader = new InstrumentationConfigurationLoader();
        for (ServiceProviderConfiguration provider : Services.loadProviderConfigurations(getClass())) {
            JsonObject parameters = provider.getParameters().get("parameters");
            String name = parameters.get("name");
            String type = parameters.get("type");
            int access = getFieldAccess((JsonArray) parameters.get("access", Json.array().add("private").toArray()));
            ClassFilter classFilter = loader.loadCompoundClassFilter(parameters.get("class", null));

            fieldInfos.add(new FieldInfo(name, 'L' + type.replace('.', '/') + ';', access, classFilter));
        }
    }

    private int getFieldAccess(JsonArray fieldAccess) {
        int access = 0;
        for (Object element : fieldAccess) {
            if (element.equals("private"))
                access |= Opcodes.ACC_PRIVATE;
            if (element.equals("protected"))
                access |= Opcodes.ACC_PROTECTED;
            if (element.equals("public"))
                access |= Opcodes.ACC_PUBLIC;
            if (element.equals("static"))
                access |= Opcodes.ACC_STATIC;
            if (element.equals("volatile"))
                access |= Opcodes.ACC_VOLATILE;
            if (element.equals("transient"))
                access |= Opcodes.ACC_TRANSIENT;
        }

        return access;
    }

    private List<FieldInfo> match(String className, List<FieldInfo> fieldInfos) {
        List<FieldInfo> matched = null;
        for (FieldInfo info : fieldInfos) {
            if (info.classFilter.notMatchClass(className))
                continue;

            if (matched == null)
                matched = new ArrayList<FieldInfo>();

            matched.add(info);
        }

        return matched;
    }

    private static class FieldInfo {
        private final String name;
        private final String descriptor;
        private final int access;
        private final ClassFilter classFilter;

        public FieldInfo(String name, String descriptor, int access, ClassFilter classFilter) {
            Assert.notNull(name);
            Assert.notNull(descriptor);
            Assert.notNull(classFilter);

            this.name = name;
            this.descriptor = descriptor;
            this.access = access;
            this.classFilter = classFilter;
        }
    }

    private static class FieldInstrumentor extends ClassVisitor {
        private final ClassLoader classLoader;
        private final List<FieldInfo> fieldInfos;
        private String className;
        private Set<String> superTypes = new LinkedHashSet<String>();
        private Set<String> annotations = new LinkedHashSet<String>();
        private boolean transformed;

        public FieldInstrumentor(ClassVisitor cv, List<FieldInfo> fieldInfos, ClassLoader classLoader) {
            super(Opcodes.ASM5, cv);

            Assert.notNull(fieldInfos);

            this.fieldInfos = fieldInfos;
            this.classLoader = classLoader;
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
            ClassLoader classLoader = this.classLoader;

            this.className = Type.getObjectType(name).getClassName();

            if ((access & Opcodes.ACC_INTERFACE) == Opcodes.ACC_INTERFACE)
                throw new SkipInstrumentationException();

            try {
                if (superName != null)
                    Instrumentors.addSuperTypes(this.superTypes, superName, true, classLoader);

                if (interfaces != null && interfaces.length > 0) {
                    for (int i = 0; i < interfaces.length; i++)
                        Instrumentors.addSuperTypes(this.superTypes, interfaces[i], true, classLoader);
                }
            } catch (InstrumentationException e) {
                logger.log(LogLevel.ERROR, e);

                throw new SkipInstrumentationException();
            }

            cv.visit(version, access, name, signature, superName, interfaces);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
            annotations.add(Type.getType(desc).getClassName());

            return cv.visitAnnotation(desc, visible);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            transform();

            return cv.visitMethod(access, name, desc, signature, exceptions);
        }

        @Override
        public void visitEnd() {
            transform();

            cv.visitEnd();
        }

        private void transform() {
            if (transformed)
                return;

            transformed = true;
            List<FieldInfo> matched = matchClass(className, superTypes, annotations);
            if (matched == null)
                throw new SkipInstrumentationException();

            for (FieldInfo info : matched) {
                FieldVisitor fv = cv.visitField(info.access, info.name, info.descriptor, null, null);
                if (fv != null)
                    fv.visitEnd();
            }
        }

        private List<FieldInfo> matchClass(String className, Set<String> superTypes, Set<String> annotations) {
            List<FieldInfo> matched = null;
            for (FieldInfo info : fieldInfos) {
                if (info.classFilter.matchClass(className, superTypes, annotations)) {
                    if (matched == null)
                        matched = new ArrayList<FieldInfo>();

                    matched.add(info);
                }
            }

            return matched;
        }
    }
}
