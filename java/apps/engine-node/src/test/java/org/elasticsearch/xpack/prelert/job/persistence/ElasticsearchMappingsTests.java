/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016
 *
 * Notice: this software, and all information contained
 * therein, is the exclusive property of Elasticsearch BV
 * and its licensors, if any, and is protected under applicable
 * domestic and foreign law, and international treaties.
 *
 * Reproduction, republication or distribution without the
 * express written consent of Elasticsearch BV is
 * strictly prohibited.
 */
package org.elasticsearch.xpack.prelert.job.persistence;

import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.CategorizerState;
import org.elasticsearch.xpack.prelert.job.DataCounts;
import org.elasticsearch.xpack.prelert.job.Job;
import org.elasticsearch.xpack.prelert.job.ModelSizeStats;
import org.elasticsearch.xpack.prelert.job.ModelSnapshot;
import org.elasticsearch.xpack.prelert.job.ModelState;
import org.elasticsearch.xpack.prelert.job.SchedulerConfig;
import org.elasticsearch.xpack.prelert.job.audit.AuditActivity;
import org.elasticsearch.xpack.prelert.job.audit.AuditMessage;
import org.elasticsearch.xpack.prelert.job.metadata.Allocation;
import org.elasticsearch.xpack.prelert.job.quantiles.Quantiles;
import org.elasticsearch.xpack.prelert.job.results.AnomalyRecord;
import org.elasticsearch.xpack.prelert.job.results.Bucket;
import org.elasticsearch.xpack.prelert.job.results.BucketInfluencer;
import org.elasticsearch.xpack.prelert.job.results.CategoryDefinition;
import org.elasticsearch.xpack.prelert.job.results.Influencer;
import org.elasticsearch.xpack.prelert.job.results.ModelDebugOutput;
import org.elasticsearch.xpack.prelert.job.results.ReservedFieldNames;
import org.elasticsearch.xpack.prelert.job.usage.Usage;
import org.elasticsearch.xpack.prelert.lists.ListDocument;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;


public class ElasticsearchMappingsTests extends ESTestCase {
    private void parseJson(JsonParser parser, Set<String> expected) throws IOException {
        try {
            JsonToken token = parser.nextToken();
            while (token != null && token != JsonToken.END_OBJECT) {
                switch (token) {
                case START_OBJECT:
                    parseJson(parser, expected);
                    break;
                case FIELD_NAME:
                    String fieldName = parser.getCurrentName();
                    expected.add(fieldName);
                    break;
                default:
                    break;
                }
                token = parser.nextToken();
            }
        } catch (JsonParseException e) {
            fail("Cannot parse JSON: " + e);
        }
    }

    public void testReservedFields()
            throws IOException, ClassNotFoundException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Set<String> overridden = new HashSet<>();

        // These are not reserved because they're Elasticsearch keywords, not
        // field names
        overridden.add(ElasticsearchMappings.ALL);
        overridden.add(ElasticsearchMappings.ANALYZER);
        overridden.add(ElasticsearchMappings.COPY_TO);
        overridden.add(ElasticsearchMappings.DYNAMIC);
        overridden.add(ElasticsearchMappings.ENABLED);
        overridden.add(ElasticsearchMappings.INCLUDE_IN_ALL);
        overridden.add(ElasticsearchMappings.INDEX);
        overridden.add(ElasticsearchMappings.NESTED);
        overridden.add(ElasticsearchMappings.NO);
        overridden.add(ElasticsearchMappings.PARENT);
        overridden.add(ElasticsearchMappings.PROPERTIES);
        overridden.add(ElasticsearchMappings.TYPE);
        overridden.add(ElasticsearchMappings.WHITESPACE);

        // These are not reserved because they're data types, not field names
        overridden.add(AnomalyRecord.TYPE.getPreferredName());
        overridden.add(AuditActivity.TYPE.getPreferredName());
        overridden.add(AuditMessage.TYPE.getPreferredName());
        overridden.add(Bucket.TYPE.getPreferredName());
        overridden.add(DataCounts.TYPE.getPreferredName());
        overridden.add(ReservedFieldNames.BUCKET_PROCESSING_TIME_TYPE);
        overridden.add(BucketInfluencer.TYPE.getPreferredName());
        overridden.add(CategorizerState.TYPE);
        overridden.add(CategoryDefinition.TYPE.getPreferredName());
        overridden.add(Influencer.TYPE.getPreferredName());
        overridden.add(Job.TYPE);
        overridden.add(ListDocument.TYPE.getPreferredName());
        overridden.add(ModelDebugOutput.TYPE.getPreferredName());
        overridden.add(ModelState.TYPE);
        overridden.add(ModelSnapshot.TYPE.getPreferredName());
        overridden.add(ModelSizeStats.TYPE.getPreferredName());
        overridden.add(Quantiles.TYPE.getPreferredName());
        overridden.add(Usage.TYPE);

        // These are not reserved because they're in the prelert-int index, not
        // prelertresults-*
        overridden.add(ListDocument.ID.getPreferredName());
        overridden.add(ListDocument.ITEMS.getPreferredName());

        // These are not reserved because they're analyzed strings, i.e. the
        // same type as user-specified fields
        overridden.add(Job.DESCRIPTION.getPreferredName());
        overridden.add(Allocation.STATUS.getPreferredName());
        overridden.add(ModelSnapshot.DESCRIPTION.getPreferredName());
        overridden.add(SchedulerConfig.USERNAME.getPreferredName());

        Set<String> expected = new HashSet<>();

        XContentBuilder builder = ElasticsearchMappings.auditActivityMapping();
        BufferedInputStream inputStream = new BufferedInputStream(
                new ByteArrayInputStream(builder.string().getBytes(StandardCharsets.UTF_8)));
        JsonParser parser = new JsonFactory().createParser(inputStream);
        parseJson(parser, expected);

        builder = ElasticsearchMappings.auditMessageMapping();
        inputStream = new BufferedInputStream(new ByteArrayInputStream(builder.string().getBytes(StandardCharsets.UTF_8)));
        parser = new JsonFactory().createParser(inputStream);
        parseJson(parser, expected);

        builder = ElasticsearchMappings.bucketInfluencerMapping();
        inputStream = new BufferedInputStream(new ByteArrayInputStream(builder.string().getBytes(StandardCharsets.UTF_8)));
        parser = new JsonFactory().createParser(inputStream);
        parseJson(parser, expected);

        builder = ElasticsearchMappings.bucketMapping();
        inputStream = new BufferedInputStream(new ByteArrayInputStream(builder.string().getBytes(StandardCharsets.UTF_8)));
        parser = new JsonFactory().createParser(inputStream);
        parseJson(parser, expected);

        builder = ElasticsearchMappings.bucketPartitionMaxNormalizedScores();
        inputStream = new BufferedInputStream(new ByteArrayInputStream(builder.string().getBytes(StandardCharsets.UTF_8)));
        parser = new JsonFactory().createParser(inputStream);
        parseJson(parser, expected);

        builder = ElasticsearchMappings.categorizerStateMapping();
        inputStream = new BufferedInputStream(new ByteArrayInputStream(builder.string().getBytes(StandardCharsets.UTF_8)));
        parser = new JsonFactory().createParser(inputStream);
        parseJson(parser, expected);

        builder = ElasticsearchMappings.categoryDefinitionMapping();
        inputStream = new BufferedInputStream(new ByteArrayInputStream(builder.string().getBytes(StandardCharsets.UTF_8)));
        parser = new JsonFactory().createParser(inputStream);
        parseJson(parser, expected);

        builder = ElasticsearchMappings.dataCountsMapping();
        inputStream = new BufferedInputStream(new ByteArrayInputStream(builder.string().getBytes(StandardCharsets.UTF_8)));
        parser = new JsonFactory().createParser(inputStream);
        parseJson(parser, expected);

        builder = ElasticsearchMappings.influencerMapping(null);
        inputStream = new BufferedInputStream(new ByteArrayInputStream(builder.string().getBytes(StandardCharsets.UTF_8)));
        parser = new JsonFactory().createParser(inputStream);
        parseJson(parser, expected);

        builder = ElasticsearchMappings.modelDebugOutputMapping(null);
        inputStream = new BufferedInputStream(new ByteArrayInputStream(builder.string().getBytes(StandardCharsets.UTF_8)));
        parser = new JsonFactory().createParser(inputStream);
        parseJson(parser, expected);

        builder = ElasticsearchMappings.modelSizeStatsMapping();
        inputStream = new BufferedInputStream(new ByteArrayInputStream(builder.string().getBytes(StandardCharsets.UTF_8)));
        parser = new JsonFactory().createParser(inputStream);
        parseJson(parser, expected);

        builder = ElasticsearchMappings.modelSnapshotMapping();
        inputStream = new BufferedInputStream(new ByteArrayInputStream(builder.string().getBytes(StandardCharsets.UTF_8)));
        parser = new JsonFactory().createParser(inputStream);
        parseJson(parser, expected);

        builder = ElasticsearchMappings.modelStateMapping();
        inputStream = new BufferedInputStream(new ByteArrayInputStream(builder.string().getBytes(StandardCharsets.UTF_8)));
        parser = new JsonFactory().createParser(inputStream);
        parseJson(parser, expected);

        builder = ElasticsearchMappings.processingTimeMapping();
        inputStream = new BufferedInputStream(new ByteArrayInputStream(builder.string().getBytes(StandardCharsets.UTF_8)));
        parser = new JsonFactory().createParser(inputStream);
        parseJson(parser, expected);

        builder = ElasticsearchMappings.quantilesMapping();
        inputStream = new BufferedInputStream(new ByteArrayInputStream(builder.string().getBytes(StandardCharsets.UTF_8)));
        parser = new JsonFactory().createParser(inputStream);
        parseJson(parser, expected);

        builder = ElasticsearchMappings.recordMapping(null);
        inputStream = new BufferedInputStream(new ByteArrayInputStream(builder.string().getBytes(StandardCharsets.UTF_8)));
        parser = new JsonFactory().createParser(inputStream);
        parseJson(parser, expected);

        builder = ElasticsearchMappings.usageMapping();
        inputStream = new BufferedInputStream(new ByteArrayInputStream(builder.string().getBytes(StandardCharsets.UTF_8)));
        parser = new JsonFactory().createParser(inputStream);
        parseJson(parser, expected);

        expected.removeAll(overridden);
        for (String s : expected) {
            // By comparing like this the failure messages say which string is
            // missing
            String reserved = ReservedFieldNames.RESERVED_FIELD_NAMES.contains(s) ? s : null;
            assertEquals(s, reserved);
        }
    }

}
