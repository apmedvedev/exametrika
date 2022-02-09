/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.instrument;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Type;

import com.exametrika.api.instrument.config.InstrumentationConfiguration;
import com.exametrika.api.instrument.config.Pointcut;
import com.exametrika.common.services.Services;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.IOs;
import com.exametrika.impl.instrument.instrumentors.ClassInstrumentor;
import com.exametrika.impl.instrument.instrumentors.SkipInstrumentationException;
import com.exametrika.spi.instrument.IClassTransformerExtension;
import com.exametrika.spi.instrument.IInterceptorAllocator;


/**
 * The {@link StaticClassTransformer} represents a class transformer which is used at buildtime.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class StaticClassTransformer {
    private final IInterceptorAllocator interceptorAllocator;
    private final ClassLoader classLoader;
    private final InstrumentationConfiguration configuration;
    private final File outputPath;
    private final List<IClassTransformerExtension> transformerExtensions;
    private final String computeFrames = System.getProperty("com.exametrika.instrument.computeFrames");

    public StaticClassTransformer(IInterceptorAllocator interceptorAllocator, ClassLoader classLoader, InstrumentationConfiguration configuration, File outputPath) {
        Assert.notNull(interceptorAllocator);
        Assert.notNull(configuration);
        Assert.notNull(outputPath);

        this.interceptorAllocator = interceptorAllocator;
        this.classLoader = classLoader;
        this.configuration = configuration;
        this.outputPath = outputPath;

        transformerExtensions = Services.loadProviders(IClassTransformerExtension.class, getClass().getClassLoader());
    }

    public byte[] transform(byte[] classFileBuffer) throws IllegalClassFormatException {
        Assert.notNull(classFileBuffer);

        String className = new ClassReader(classFileBuffer).getClassName();
        String binaryClassName = Type.getObjectType(className).getClassName();
        byte[] result = classFileBuffer;
        boolean transformed = false;

        for (ClassFileTransformer transformerExtension : transformerExtensions) {
            try {
                byte[] extensionResult = transformerExtension.transform(classLoader, className, null, null, result);
                if (extensionResult != null) {
                    result = extensionResult;
                    transformed = true;
                }
            } catch (Throwable e) {
                IllegalClassFormatException exception = new IllegalClassFormatException();
                exception.initCause(e);

                throw exception;
            }
        }

        if (!transformed)
            result = null;

        Set<Pointcut> pointcuts = new LinkedHashSet<Pointcut>(configuration.getPointcuts());
        pointcuts = match(binaryClassName, pointcuts);
        if (pointcuts == null) {
            saveData(outputPath, binaryClassName, result != null ? result : classFileBuffer);
            return result;
        }

        try {
            boolean computeFrames;
            if (this.computeFrames != null)
                computeFrames = this.computeFrames.equals("true");
            else {
                int classMajorVersion = classFileBuffer[7];
                computeFrames = classMajorVersion >= 51;
            }

            int flags = ClassWriter.COMPUTE_MAXS | (computeFrames ? ClassWriter.COMPUTE_FRAMES : 0);

            ClassReader classReader = new ClassReader(result != null ? result : classFileBuffer);
            ComputeClassWriter classWriter = new ComputeClassWriter(classReader, flags, classLoader);
            ClassInstrumentor classInstrumentor = new ClassInstrumentor(classWriter, interceptorAllocator, classLoader, pointcuts, null, null,
                    new HashSet<String>());

            try {
                classReader.accept(classInstrumentor, ClassReader.EXPAND_FRAMES);

                if (!classInstrumentor.getErrorMethods().isEmpty()) {
                    classReader = new ClassReader(result != null ? result : classFileBuffer);
                    classWriter = new ComputeClassWriter(classReader, flags, classLoader);
                    classInstrumentor = new ClassInstrumentor(classWriter, interceptorAllocator, classLoader, pointcuts, null, null,
                            classInstrumentor.getErrorMethods());

                    classReader.accept(classInstrumentor, ClassReader.EXPAND_FRAMES);
                }

                byte[] data = classWriter.toByteArray();

                saveData(outputPath, binaryClassName, data);

                return data;
            } catch (SkipInstrumentationException e) {
                saveData(outputPath, binaryClassName, result != null ? result : classFileBuffer);
                return null;
            }
        } catch (Exception e) {
            IllegalClassFormatException exception = new IllegalClassFormatException();
            exception.initCause(e);

            throw exception;
        }
    }

    private Set<Pointcut> match(String className, Set<Pointcut> pointcuts) {
        Set<Pointcut> matched = null;
        for (Pointcut pointcut : pointcuts) {
            if (pointcut.getMethodFilter() != null && pointcut.getMethodFilter().notMatchClass(className))
                continue;

            if (matched == null)
                matched = new LinkedHashSet<Pointcut>();

            matched.add(pointcut);
        }

        return matched;
    }

    private void saveData(File outputPath, String className, byte[] data) {
        File outFile = new File(outputPath, className.replace('.', File.separatorChar) + ".class");
        outFile.getParentFile().mkdirs();
        OutputStream stream = null;
        try {
            stream = new BufferedOutputStream(new FileOutputStream(outFile));
            stream.write(data, 0, data.length);
            stream.close();
        } catch (IOException e) {
            IOs.close(stream);
        }
    }
}
