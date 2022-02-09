/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.objectdb.fields;

import java.util.Arrays;

import com.exametrika.api.exadb.index.IIndexManager;
import com.exametrika.api.exadb.index.IKeyNormalizer;
import com.exametrika.api.exadb.index.IUniqueIndex;
import com.exametrika.api.exadb.index.config.schema.FixedCompositeKeyNormalizerSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.NumericKeyNormalizerSchemaConfiguration;
import com.exametrika.api.exadb.index.config.schema.NumericKeyNormalizerSchemaConfiguration.DataType;
import com.exametrika.api.exadb.objectdb.INodeIndex;
import com.exametrika.api.exadb.objectdb.config.schema.StructuredBlobIndexSchemaConfiguration;
import com.exametrika.common.utils.Assert;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Pair;
import com.exametrika.spi.exadb.core.IDatabaseContext;
import com.exametrika.spi.exadb.index.config.schema.KeyNormalizerSchemaConfiguration;


/**
 * The {@link StructuredBlobIndex} implements {@link INodeIndex}.
 *
 * @param <K> key type
 * @param <V> node type
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class StructuredBlobIndex<K, V> implements INodeIndex<K, V> {
    protected final StructuredBlobIndexSchemaConfiguration configuration;
    protected final IDatabaseContext context;
    private IUniqueIndex<ByteArray, Long> index;
    private final StructuredBlobField field;
    protected final IKeyNormalizer keyNormalizer;

    public StructuredBlobIndex(StructuredBlobIndexSchemaConfiguration configuration, IDatabaseContext context,
                               IUniqueIndex<ByteArray, Long> index, StructuredBlobField field) {
        Assert.notNull(configuration);
        Assert.notNull(context);
        Assert.notNull(index);
        Assert.notNull(field);

        this.configuration = configuration;
        this.context = context;
        this.index = index;
        this.field = field;

        KeyNormalizerSchemaConfiguration keyNormalizer = new FixedCompositeKeyNormalizerSchemaConfiguration(Arrays.asList(
                new NumericKeyNormalizerSchemaConfiguration(DataType.LONG), new NumericKeyNormalizerSchemaConfiguration(DataType.SHORT),
                configuration.getKeyNormalizer()));
        this.keyNormalizer = keyNormalizer.createKeyNormalizer();
    }

    public IUniqueIndex<ByteArray, Long> getIndex() {
        if (!index.isStale())
            return index;
        else {
            index = refreshIndex(index.getId());
            return index;
        }
    }

    @Override
    public final boolean contains(K key) {
        ByteArray indexKey = getKey(key);

        Long id = getIndex().find(indexKey);
        if (id != null)
            return true;
        else
            return false;
    }

    @Override
    public final V find(K key) {
        ByteArray indexKey = getKey(key);

        Long id = getIndex().find(indexKey);
        if (id != null)
            return findById(id);
        else
            return null;
    }

    public final void add(K key, long id) {
        ByteArray indexKey = getKey(key);
        getIndex().add(indexKey, id);
    }

    public final void remove(K key) {
        ByteArray indexKey = getKey(key);
        getIndex().remove(indexKey);
    }

    protected final V findById(long id) {
        return (V) new Pair(id, field.get(id));
    }

    protected final ByteArray getKey(K key) {
        return keyNormalizer.normalize(Arrays.asList(field.getNode().getId(), field.getSchema().getIndex(), key));
    }

    private final IUniqueIndex<ByteArray, Long> refreshIndex(int id) {
        IIndexManager indexManager = context.findTransactionExtension(IIndexManager.NAME);
        return indexManager.getIndex(id);
    }
}
