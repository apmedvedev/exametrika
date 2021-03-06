/**
 * Copyright 2013 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.common.lz4.impl;

import static com.exametrika.common.lz4.impl.UnsafeUtils.readIntLE;
import static com.exametrika.common.lz4.impl.Utils.checkRange;
import static java.lang.Integer.rotateLeft;

public final class XXHash32 {
    private static final int PRIME1 = -1640531535;
    private static final int PRIME2 = -2048144777;
    private static final int PRIME3 = -1028477379;
    private static final int PRIME4 = 668265263;
    private static final int PRIME5 = 374761393;

    public static int hash(byte[] buf, int off, int len, int seed) {
        checkRange(buf, off, len);

        final int end = off + len;
        int h32;

        if (len >= 16) {
            final int limit = end - 16;
            int v1 = seed + PRIME1 + PRIME2;
            int v2 = seed + PRIME2;
            int v3 = seed + 0;
            int v4 = seed - PRIME1;
            do {
                v1 += readIntLE(buf, off) * PRIME2;
                v1 = rotateLeft(v1, 13);
                v1 *= PRIME1;
                off += 4;

                v2 += readIntLE(buf, off) * PRIME2;
                v2 = rotateLeft(v2, 13);
                v2 *= PRIME1;
                off += 4;

                v3 += readIntLE(buf, off) * PRIME2;
                v3 = rotateLeft(v3, 13);
                v3 *= PRIME1;
                off += 4;

                v4 += readIntLE(buf, off) * PRIME2;
                v4 = rotateLeft(v4, 13);
                v4 *= PRIME1;
                off += 4;
            }
            while (off <= limit);

            h32 = rotateLeft(v1, 1) + rotateLeft(v2, 7) + rotateLeft(v3, 12) + rotateLeft(v4, 18);
        } else {
            h32 = seed + PRIME5;
        }

        h32 += len;

        while (off <= end - 4) {
            h32 += readIntLE(buf, off) * PRIME3;
            h32 = rotateLeft(h32, 17) * PRIME4;
            off += 4;
        }

        while (off < end) {
            h32 += (buf[off] & 0xFF) * PRIME5;
            h32 = rotateLeft(h32, 11) * PRIME1;
            ++off;
        }

        h32 ^= h32 >>> 15;
        h32 *= PRIME2;
        h32 ^= h32 >>> 13;
        h32 *= PRIME3;
        h32 ^= h32 >>> 16;

        return h32;
    }

    private XXHash32() {
    }
}
