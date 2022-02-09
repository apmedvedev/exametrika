/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.instrument.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.List;

import com.exametrika.api.instrument.IJoinPoint;
import com.exametrika.common.io.SerializationException;
import com.exametrika.common.io.impl.ByteInputStream;
import com.exametrika.common.io.impl.ByteOutputStream;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.IOs;
import com.exametrika.common.utils.Serializers;


/**
 * The {@link JoinPointLoader} is used to load and save join points.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public final class JoinPointLoader {
    public List<IJoinPoint> load(InputStream stream) {
        Assert.notNull(stream);

        ByteOutputStream outputStream = new ByteOutputStream(0x1000);
        try {
            IOs.copy(stream, outputStream, 0x1000);
        } catch (IOException e) {
            throw new SerializationException(e);
        }

        ByteInputStream in = new ByteInputStream(outputStream.getBuffer(), 0, outputStream.getLength());
        return Serializers.deserialize(in, JoinPointLoader.class.getClassLoader());
    }

    public void save(List<IJoinPoint> joinPoints, OutputStream stream) {
        Assert.notNull(joinPoints);
        Assert.notNull(stream);

        ByteOutputStream out = new ByteOutputStream(0x1000);
        Serializers.serialize(out, (Serializable) joinPoints);
        IOs.close(out);

        ByteInputStream inputStream = new ByteInputStream(out.getBuffer(), 0, out.getLength());
        try {
            IOs.copy(inputStream, stream, 0x1000);
        } catch (IOException e) {
            throw new SerializationException(e);
        }
    }
}