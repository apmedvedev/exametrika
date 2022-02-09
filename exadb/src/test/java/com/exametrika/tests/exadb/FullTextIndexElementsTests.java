/**
 * Copyright 2007 Andrey Medvedev. All rights reserved.
 */
package com.exametrika.tests.exadb;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.Test;

import com.exametrika.api.exadb.fulltext.IDocument;
import com.exametrika.api.exadb.fulltext.IField;
import com.exametrika.api.exadb.fulltext.INumericField;
import com.exametrika.api.exadb.fulltext.IStringField;
import com.exametrika.api.exadb.fulltext.ITextField;
import com.exametrika.api.exadb.fulltext.config.schema.CollationKeyAnalyzerSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.config.schema.CollationKeyAnalyzerSchemaConfiguration.Strength;
import com.exametrika.api.exadb.fulltext.config.schema.CompositeQuerySchemaConfiguration;
import com.exametrika.api.exadb.fulltext.config.schema.CompositeQuerySchemaConfiguration.Occur;
import com.exametrika.api.exadb.fulltext.config.schema.DocumentSchemaBuilder;
import com.exametrika.api.exadb.fulltext.config.schema.DocumentSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.config.schema.ExpressionQuerySchemaConfiguration;
import com.exametrika.api.exadb.fulltext.config.schema.NumericFieldSchemaConfiguration.DataType;
import com.exametrika.api.exadb.fulltext.config.schema.Queries;
import com.exametrika.api.exadb.fulltext.config.schema.SimpleAnalyzerSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.config.schema.StandardAnalyzerSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.config.schema.StringFieldSchemaBuilder;
import com.exametrika.api.exadb.fulltext.config.schema.TermQuerySchemaConfiguration;
import com.exametrika.api.exadb.fulltext.config.schema.WhitespaceAnalyzerSchemaConfiguration;
import com.exametrika.api.exadb.fulltext.schema.IDocumentSchema;
import com.exametrika.common.utils.Pair;
import com.exametrika.spi.exadb.fulltext.config.schema.QuerySchemaConfiguration;


/**
 * The {@link FullTextIndexElementsTests} are tests for different index elements.
 *
 * @author Medvedev-A
 */
public class FullTextIndexElementsTests {
    @Test
    public void testIndexDocument() {
        DocumentSchemaBuilder builder = new DocumentSchemaBuilder();
        DocumentSchemaConfiguration configuration = builder.analyzer(new StandardAnalyzerSchemaConfiguration())
                .stringField("field1")
                .stored()
                .indexed()
                .end()
                .textField("field1")
                .tokenized()
                .analyzer(new CollationKeyAnalyzerSchemaConfiguration("ru_RU", Strength.PRIMARY))
                .end()
                .numericField("field2")
                .stored()
                .type(DataType.INT)
                .end()
                .toConfiguration();

        assertThat(configuration.findField("field10"), nullValue());
        assertThat(configuration.findField("field1"), is(new StringFieldSchemaBuilder(builder, "field1", true).stored().indexed().toConfiguration()));
        assertThat(configuration.getFields("field1"), is(Arrays.asList(
                new StringFieldSchemaBuilder(builder, "field1", true).stored().indexed().toConfiguration(),
                new StringFieldSchemaBuilder(builder, "field1", false).tokenized().analyzer(new CollationKeyAnalyzerSchemaConfiguration("ru_RU", Strength.PRIMARY)).toConfiguration())));
        IDocumentSchema schema = configuration.createSchema();
        assertThat(schema.getFields().size(), is(3));
        assertThat(schema.findField("field10"), nullValue());
        assertThat(schema.findField("field1") != null, is(true));
        IDocument document = schema.createDocument("", new StringReader(""), 10);
        List<IField> fields = document.getFields();
        assertThat(fields.size(), is(3));
        assertThat(fields.get(0) instanceof IStringField, is(true));
        assertThat(fields.get(1) instanceof ITextField, is(true));
        assertThat(fields.get(2) instanceof INumericField, is(true));
        assertThat(document.findField("field1") == fields.get(0), is(true));
        assertThat(document.findField("field2") == fields.get(2), is(true));
    }

    @Test
    public void testQueries() {
        TermQuerySchemaConfiguration configuration1 = Queries.term("field", "value").boost(2.0f).toConfiguration();
        assertThat(configuration1, is(new TermQuerySchemaConfiguration("field", "value", 2.0f)));
        assertThat(configuration1.createQuery(new DocumentSchemaBuilder().toConfiguration().createSchema()) != null, is(true));

        ExpressionQuerySchemaConfiguration configuration2 = Queries.expression("field", "value")
                .boost(2.0f)
                .locale(Locale.getDefault())
                .timeZone(TimeZone.getDefault()).toConfiguration();
        assertThat(configuration2, is(new ExpressionQuerySchemaConfiguration("field", "value",
                Locale.getDefault(), TimeZone.getDefault(), 2.0f)));
        assertThat(configuration2.createQuery(new DocumentSchemaBuilder().toConfiguration().createSchema()) != null, is(true));

        CompositeQuerySchemaConfiguration configuration3 = Queries.composite().boost(2.0f).minimumShouldMatch(1)
                .must()
                .term("field1", "value1").end()
                .should()
                .expression("field1", "value1").end()
                .mustNot()
                .composite()
                .term("field2", "value2").end()
                .end().toConfiguration();
        assertThat(configuration3, is(new CompositeQuerySchemaConfiguration(Arrays.<Pair<Occur, QuerySchemaConfiguration>>asList(new Pair(Occur.MUST,
                        new TermQuerySchemaConfiguration("field1", "value1", 1.0f)),
                new Pair(Occur.SHOULD, new ExpressionQuerySchemaConfiguration("field1", "value1", null, null, 1.0f)),
                new Pair(Occur.MUST_NOT, new CompositeQuerySchemaConfiguration(Arrays.<Pair<Occur, QuerySchemaConfiguration>>asList(
                        new Pair(Occur.MUST, new TermQuerySchemaConfiguration("field2", "value2", 1.0f))), 0, 1.0f))), 1, 2.0f)));
        assertThat(configuration3.createQuery(new DocumentSchemaBuilder().toConfiguration().createSchema()) != null, is(true));
    }

    @Test
    public void testAnalyzers() {
        SimpleAnalyzerSchemaConfiguration configuration1 = new SimpleAnalyzerSchemaConfiguration();
        assertThat(configuration1.createAnalyzer() != null, is(true));
        StandardAnalyzerSchemaConfiguration configuration2 = new StandardAnalyzerSchemaConfiguration();
        assertThat(configuration2.createAnalyzer() != null, is(true));
        WhitespaceAnalyzerSchemaConfiguration configuration3 = new WhitespaceAnalyzerSchemaConfiguration();
        assertThat(configuration3.createAnalyzer() != null, is(true));
        CollationKeyAnalyzerSchemaConfiguration configuration4 = new CollationKeyAnalyzerSchemaConfiguration("ru_RU", Strength.PRIMARY);
        assertThat(configuration4.createAnalyzer() != null, is(true));
    }
}
