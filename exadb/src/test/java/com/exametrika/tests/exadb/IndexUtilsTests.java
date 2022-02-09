/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.exadb;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.Test;

import com.exametrika.api.exadb.index.IKeyNormalizer;
import com.exametrika.api.exadb.index.IValueConverter;
import com.exametrika.api.exadb.index.Indexes;
import com.exametrika.common.utils.ByteArray;
import com.ibm.icu.text.Collator;
import com.ibm.icu.util.ULocale;


/**
 * The {@link IndexUtilsTests} are tests for {@link Indexes}.
 *
 * @author Medvedev-A
 */
public class IndexUtilsTests {
    @Test
    public void testNormalizers() {
        IKeyNormalizer normalizer = Indexes.createByteKeyNormalizer();
        assertThat(normalizer.normalize(Byte.MIN_VALUE).compareTo(normalizer.normalize(Byte.MAX_VALUE)) < 0, is(true));
        assertThat(normalizer.normalize(Byte.MAX_VALUE).compareTo(normalizer.normalize(Byte.MIN_VALUE)) > 0, is(true));
        assertThat(normalizer.normalize(Byte.MAX_VALUE).compareTo(normalizer.normalize(Byte.MAX_VALUE)) == 0, is(true));

        normalizer = Indexes.createCharKeyNormalizer();
        assertThat(normalizer.normalize(Character.MIN_VALUE).compareTo(normalizer.normalize(Character.MAX_VALUE)) < 0, is(true));
        assertThat(normalizer.normalize(Character.MAX_VALUE).compareTo(normalizer.normalize(Character.MIN_VALUE)) > 0, is(true));
        assertThat(normalizer.normalize(Character.MAX_VALUE).compareTo(normalizer.normalize(Character.MAX_VALUE)) == 0, is(true));

        normalizer = Indexes.createShortKeyNormalizer();
        assertThat(normalizer.normalize(Short.MIN_VALUE).compareTo(normalizer.normalize(Short.MAX_VALUE)) < 0, is(true));
        assertThat(normalizer.normalize(Short.MAX_VALUE).compareTo(normalizer.normalize(Short.MIN_VALUE)) > 0, is(true));
        assertThat(normalizer.normalize(Short.MAX_VALUE).compareTo(normalizer.normalize(Short.MAX_VALUE)) == 0, is(true));

        normalizer = Indexes.createIntKeyNormalizer();
        assertThat(normalizer.normalize(Integer.MIN_VALUE).compareTo(normalizer.normalize(Integer.MAX_VALUE)) < 0, is(true));
        assertThat(normalizer.normalize(Integer.MAX_VALUE).compareTo(normalizer.normalize(Integer.MIN_VALUE)) > 0, is(true));
        assertThat(normalizer.normalize(Integer.MAX_VALUE).compareTo(normalizer.normalize(Integer.MAX_VALUE)) == 0, is(true));

        normalizer = Indexes.createLongKeyNormalizer();
        assertThat(normalizer.normalize(Long.MIN_VALUE).compareTo(normalizer.normalize(Long.MAX_VALUE)) < 0, is(true));
        assertThat(normalizer.normalize(Long.MAX_VALUE).compareTo(normalizer.normalize(Long.MIN_VALUE)) > 0, is(true));
        assertThat(normalizer.normalize(Long.MAX_VALUE).compareTo(normalizer.normalize(Long.MAX_VALUE)) == 0, is(true));

        normalizer = Indexes.createFloatKeyNormalizer();
        assertThat(normalizer.normalize(Float.MIN_VALUE).compareTo(normalizer.normalize(Float.MAX_VALUE)) < 0, is(true));
        assertThat(normalizer.normalize(-Float.MIN_VALUE).compareTo(normalizer.normalize(-Float.MAX_VALUE)) > 0, is(true));
        assertThat(normalizer.normalize(-Float.MAX_VALUE).compareTo(normalizer.normalize(Float.MAX_VALUE)) < 0, is(true));
        assertThat(normalizer.normalize(-Float.MIN_VALUE).compareTo(normalizer.normalize(Float.MIN_VALUE)) < 0, is(true));
        assertThat(normalizer.normalize(-Float.MIN_VALUE).compareTo(normalizer.normalize(-Float.MIN_VALUE)) == 0, is(true));
        assertThat(normalizer.normalize(Float.MAX_VALUE).compareTo(normalizer.normalize(Float.MAX_VALUE)) == 0, is(true));

        normalizer = Indexes.createDoubleKeyNormalizer();
        assertThat(normalizer.normalize(Double.MIN_VALUE).compareTo(normalizer.normalize(Double.MAX_VALUE)) < 0, is(true));
        assertThat(normalizer.normalize(-Double.MIN_VALUE).compareTo(normalizer.normalize(-Double.MAX_VALUE)) > 0, is(true));
        assertThat(normalizer.normalize(-Double.MAX_VALUE).compareTo(normalizer.normalize(Double.MAX_VALUE)) < 0, is(true));
        assertThat(normalizer.normalize(-Double.MIN_VALUE).compareTo(normalizer.normalize(Double.MIN_VALUE)) < 0, is(true));
        assertThat(normalizer.normalize(-Double.MIN_VALUE).compareTo(normalizer.normalize(-Double.MIN_VALUE)) == 0, is(true));
        assertThat(normalizer.normalize(Double.MAX_VALUE).compareTo(normalizer.normalize(Double.MAX_VALUE)) == 0, is(true));

        normalizer = Indexes.createUUIDKeyNormalizer();
        UUID id1 = new UUID(0, 0);
        UUID id2 = new UUID(Long.MAX_VALUE, Long.MAX_VALUE);
        assertThat(normalizer.normalize(id1).compareTo(normalizer.normalize(id2)) < 0, is(true));
        assertThat(normalizer.normalize(id2).compareTo(normalizer.normalize(id1)) > 0, is(true));
        assertThat(normalizer.normalize(id1).compareTo(normalizer.normalize(id1)) == 0, is(true));

        normalizer = Indexes.createStringKeyNormalizer();
        String str1 = "hello";
        String str2 = "helm";
        assertThat(normalizer.normalize(str1).compareTo(normalizer.normalize(str2)) < 0, is(true));
        assertThat(normalizer.normalize(str2).compareTo(normalizer.normalize(str1)) > 0, is(true));
        assertThat(normalizer.normalize(str1).compareTo(normalizer.normalize(str1)) == 0, is(true));

        normalizer = Indexes.createFixedStringKeyNormalizer();
        assertThat(normalizer.normalize(str1).compareTo(normalizer.normalize(str2)) < 0, is(true));
        assertThat(normalizer.normalize(str2).compareTo(normalizer.normalize(str1)) > 0, is(true));
        assertThat(normalizer.normalize(str1).compareTo(normalizer.normalize(str1)) == 0, is(true));

        Collator collator = Collator.getInstance(new ULocale("da_DK"));
        collator.setStrength(Collator.PRIMARY);

        normalizer = Indexes.createCollationKeyNormalizer(collator);
        List<ByteArray> list = new ArrayList<ByteArray>();
        list.add(normalizer.normalize("HAT"));
        list.add(normalizer.normalize("HUT"));
        list.add(normalizer.normalize("H\u00C5T"));
        list.add(normalizer.normalize("H\u00D8T"));
        list.add(normalizer.normalize("HOT"));
        Collections.sort(list);

        List<ByteArray> list2 = new ArrayList<ByteArray>();
        list2.add(normalizer.normalize("HAT"));
        list2.add(normalizer.normalize("HOT"));
        list2.add(normalizer.normalize("HUT"));
        list2.add(normalizer.normalize("H\u00D8T"));
        list2.add(normalizer.normalize("H\u00C5T"));

        assertThat(list, is(list2));

        normalizer = Indexes.createDescendingNormalizer(normalizer);
        list = new ArrayList<ByteArray>();
        list.add(normalizer.normalize("HAT"));
        list.add(normalizer.normalize("HUT"));
        list.add(normalizer.normalize("H\u00C5T"));
        list.add(normalizer.normalize("H\u00D8T"));
        list.add(normalizer.normalize("HOT"));
        Collections.sort(list);

        list2 = new ArrayList<ByteArray>();
        list2.add(normalizer.normalize("H\u00C5T"));
        list2.add(normalizer.normalize("H\u00D8T"));
        list2.add(normalizer.normalize("HUT"));
        list2.add(normalizer.normalize("HOT"));
        list2.add(normalizer.normalize("HAT"));

        assertThat(list, is(list2));

        collator = Collator.getInstance(new ULocale("ru_RU"));
        collator.setStrength(Collator.SECONDARY);
        normalizer = Indexes.createCollationKeyNormalizer(collator);
        assertThat(normalizer.normalize("ПРИВЕТ").compareTo(normalizer.normalize("привет")) == 0, is(true));

        normalizer = Indexes.createCompositeNormalizer(Arrays.asList(Indexes.createStringKeyNormalizer(), Indexes.createStringKeyNormalizer()));
        ByteArray key1 = normalizer.normalize(Arrays.asList("a", "bc"));
        ByteArray key2 = normalizer.normalize(Arrays.asList("ab", "c"));
        ByteArray key3 = normalizer.normalize(Arrays.asList("abc", ""));
        assertThat(key1.compareTo(key2) < 0, is(true));
        assertThat(key2.compareTo(key3) < 0, is(true));

        normalizer = Indexes.createDescendingNormalizer(normalizer);
        key1 = normalizer.normalize(Arrays.asList("a", "bc"));
        key2 = normalizer.normalize(Arrays.asList("ab", "c"));
        key3 = normalizer.normalize(Arrays.asList("abc", ""));
        assertThat(key1.compareTo(key2) > 0, is(true));
        assertThat(key2.compareTo(key3) > 0, is(true));

        normalizer = Indexes.createFixedCompositeNormalizer(Arrays.asList(Indexes.createIntKeyNormalizer(), Indexes.createLongKeyNormalizer()));
        key1 = normalizer.normalize(Arrays.asList(10, 100l));
        key2 = normalizer.normalize(Arrays.asList(10, 200l));
        key3 = normalizer.normalize(Arrays.asList(100, 10l));
        assertThat(key1.compareTo(key2) < 0, is(true));
        assertThat(key2.compareTo(key3) < 0, is(true));
    }

    @Test
    public void testConverters() {
        IValueConverter<Long> converter1 = Indexes.createLongValueConverter();
        assertThat(converter1.toValue(converter1.toByteArray(1234567890l)), is(1234567890l));

        IValueConverter<Integer> converter2 = Indexes.createIntValueConverter();
        assertThat(converter2.toValue(converter2.toByteArray(1234567890)), is(1234567890));
    }
}
