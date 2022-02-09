/**
 * Copyright 2010 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.impl.exadb.security.schema;

import java.security.MessageDigest;
import java.util.List;
import java.util.Random;

import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;
import com.exametrika.api.exadb.objectdb.schema.IFieldSchema;
import com.exametrika.api.exadb.security.config.schema.UserNodeSchemaConfiguration;
import com.exametrika.api.exadb.security.schema.IUserNodeSchema;
import com.exametrika.common.utils.ByteArray;
import com.exametrika.common.utils.Exceptions;
import com.exametrika.impl.exadb.objectdb.schema.ObjectNodeSchema;


/**
 * The {@link UserNodeSchema} represents a schema of user node.
 *
 * @author Medvedev-A
 * @threadsafety This class and its methods are thread safe.
 */
public class UserNodeSchema extends ObjectNodeSchema implements IUserNodeSchema {
    private static final int SALT_LENGTH = 10;
    private final Random random = new Random();
    private final MessageDigest digest;

    public UserNodeSchema(UserNodeSchemaConfiguration configuration, int index, List<IFieldSchema> fields,
                          IDocumentSchema fullTextSchema) {
        super(configuration, index, fields, fullTextSchema);

        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public ByteArray createPasswordHash(String password, ByteArray credentials) {
        try {
            ByteArray salt;
            if (credentials != null)
                salt = credentials.subArray(0, SALT_LENGTH);
            else {
                byte[] buffer = new byte[SALT_LENGTH];
                random.nextBytes(buffer);
                salt = new ByteArray(buffer);
            }

            digest.update(salt.getBuffer(), salt.getOffset(), salt.getLength());
            digest.update(password.getBytes("UTF-8"));
            byte[] buffer = new byte[SALT_LENGTH + digest.getDigestLength()];
            System.arraycopy(salt.getBuffer(), salt.getOffset(), buffer, 0, SALT_LENGTH);
            System.arraycopy(digest.digest(), 0, buffer, SALT_LENGTH, digest.getDigestLength());

            return new ByteArray(buffer);
        } catch (Exception e) {
            return Exceptions.wrapAndThrow(e);
        }
    }
}
