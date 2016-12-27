/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2016 Elasticsearch BV. All Rights Reserved.
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

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.results.AnomalyRecord;
import org.elasticsearch.xpack.prelert.job.results.Bucket;
import org.elasticsearch.xpack.prelert.job.results.BucketInfluencer;
import org.elasticsearch.xpack.prelert.job.results.Influencer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;


public class JobResultsPersisterTests extends ESTestCase {

    private static final String JOB_ID = "foo";

    public void testPersistBucket_OneRecord() throws IOException {
        AtomicReference<BulkRequest> reference = new AtomicReference<>();
        Client client = mockClient(reference);
        Bucket bucket = new Bucket("foo", new Date(), 123456);
        bucket.setAnomalyScore(99.9);
        bucket.setEventCount(57);
        bucket.setInitialAnomalyScore(88.8);
        bucket.setMaxNormalizedProbability(42.0);
        bucket.setProcessingTimeMs(8888);
        bucket.setRecordCount(1);

        BucketInfluencer bi = new BucketInfluencer(JOB_ID, new Date(), 600, 1);
        bi.setAnomalyScore(14.15);
        bi.setInfluencerFieldName("biOne");
        bi.setInitialAnomalyScore(18.12);
        bi.setProbability(0.0054);
        bi.setRawAnomalyScore(19.19);
        bucket.addBucketInfluencer(bi);

        // We are adding a record but it shouldn't be persisted as part of the bucket
        AnomalyRecord record = new AnomalyRecord(JOB_ID, new Date(), 600, 2);
        record.setAnomalyScore(99.8);
        bucket.setRecords(Arrays.asList(record));

        JobResultsPersister persister = new JobResultsPersister(Settings.EMPTY, client);
        persister.bulkPersisterBuilder(JOB_ID).persistBucket(bucket).executeRequest();
        BulkRequest bulkRequest = reference.get();
        assertEquals(2, bulkRequest.numberOfActions());

        String s = ((IndexRequest)bulkRequest.requests().get(0)).source().utf8ToString();
        assertTrue(s.matches(".*anomaly_score.:99\\.9.*"));
        assertTrue(s.matches(".*initial_anomaly_score.:88\\.8.*"));
        assertTrue(s.matches(".*max_normalized_probability.:42\\.0.*"));
        assertTrue(s.matches(".*record_count.:1.*"));
        assertTrue(s.matches(".*event_count.:57.*"));
        assertTrue(s.matches(".*bucket_span.:123456.*"));
        assertTrue(s.matches(".*processing_time_ms.:8888.*"));
        // There should NOT be any nested records
        assertFalse(s.matches(".*records*"));

        s = ((IndexRequest)bulkRequest.requests().get(1)).source().utf8ToString();
        assertTrue(s.matches(".*probability.:0\\.0054.*"));
        assertTrue(s.matches(".*influencer_field_name.:.biOne.*"));
        assertTrue(s.matches(".*initial_anomaly_score.:18\\.12.*"));
        assertTrue(s.matches(".*anomaly_score.:14\\.15.*"));
        assertTrue(s.matches(".*raw_anomaly_score.:19\\.19.*"));
    }

    public void testPersistRecords() throws IOException {
        AtomicReference<BulkRequest> reference = new AtomicReference<>();
        Client client = mockClient(reference);

        List<AnomalyRecord> records = new ArrayList<>();
        AnomalyRecord r1 = new AnomalyRecord(JOB_ID, new Date(), 42, 1);
        records.add(r1);
        List<Double> actuals = new ArrayList<>();
        actuals.add(5.0);
        actuals.add(5.1);
        r1.setActual(actuals);
        r1.setAnomalyScore(99.8);
        r1.setByFieldName("byName");
        r1.setByFieldValue("byValue");
        r1.setCorrelatedByFieldValue("testCorrelations");
        r1.setDetectorIndex(3);
        r1.setFieldName("testFieldName");
        r1.setFunction("testFunction");
        r1.setFunctionDescription("testDescription");
        r1.setInitialNormalizedProbability(23.4);
        r1.setNormalizedProbability(0.005);
        r1.setOverFieldName("overName");
        r1.setOverFieldValue("overValue");
        r1.setPartitionFieldName("partName");
        r1.setPartitionFieldValue("partValue");
        r1.setProbability(0.1);
        List<Double> typicals = new ArrayList<>();
        typicals.add(0.44);
        typicals.add(998765.3);
        r1.setTypical(typicals);

        JobResultsPersister persister = new JobResultsPersister(Settings.EMPTY, client);
        persister.bulkPersisterBuilder(JOB_ID).persistRecords(records).executeRequest();
        BulkRequest bulkRequest = reference.get();
        assertEquals(1, bulkRequest.numberOfActions());

        String s = ((IndexRequest) bulkRequest.requests().get(0)).source().utf8ToString();
        assertTrue(s.matches(".*detector_index.:3.*"));
        assertTrue(s.matches(".*\"probability\":0\\.1.*"));
        assertTrue(s.matches(".*\"anomaly_score\":99\\.8.*"));
        assertTrue(s.matches(".*\"normalized_probability\":0\\.005.*"));
        assertTrue(s.matches(".*initial_normalized_probability.:23.4.*"));
        assertTrue(s.matches(".*bucket_span.:42.*"));
        assertTrue(s.matches(".*by_field_name.:.byName.*"));
        assertTrue(s.matches(".*by_field_value.:.byValue.*"));
        assertTrue(s.matches(".*correlated_by_field_value.:.testCorrelations.*"));
        assertTrue(s.matches(".*typical.:.0\\.44,998765\\.3.*"));
        assertTrue(s.matches(".*actual.:.5\\.0,5\\.1.*"));
        assertTrue(s.matches(".*field_name.:.testFieldName.*"));
        assertTrue(s.matches(".*function.:.testFunction.*"));
        assertTrue(s.matches(".*function_description.:.testDescription.*"));
        assertTrue(s.matches(".*partition_field_name.:.partName.*"));
        assertTrue(s.matches(".*partition_field_value.:.partValue.*"));
        assertTrue(s.matches(".*over_field_name.:.overName.*"));
        assertTrue(s.matches(".*over_field_value.:.overValue.*"));
    }

    public void testPersistInfluencers() throws IOException {
        AtomicReference<BulkRequest> reference = new AtomicReference<>();
        Client client = mockClient(reference);

        List<Influencer> influencers = new ArrayList<>();
        Influencer inf = new Influencer(JOB_ID, "infName1", "infValue1", new Date(), 600, 1);
        inf.setAnomalyScore(16);
        inf.setInitialAnomalyScore(55.5);
        inf.setProbability(0.4);
        influencers.add(inf);

        JobResultsPersister persister = new JobResultsPersister(Settings.EMPTY, client);
        persister.bulkPersisterBuilder(JOB_ID).persistInfluencers(influencers).executeRequest();
        BulkRequest bulkRequest = reference.get();
        assertEquals(1, bulkRequest.numberOfActions());

        String s = ((IndexRequest) bulkRequest.requests().get(0)).source().utf8ToString();
        assertTrue(s.matches(".*probability.:0\\.4.*"));
        assertTrue(s.matches(".*influencer_field_name.:.infName1.*"));
        assertTrue(s.matches(".*influencer_field_value.:.infValue1.*"));
        assertTrue(s.matches(".*initial_anomaly_score.:55\\.5.*"));
        assertTrue(s.matches(".*anomaly_score.:16\\.0.*"));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private Client mockClient(AtomicReference reference) {
        Client client = mock(Client.class);
        doAnswer(invocationOnMock -> {
            reference.set(invocationOnMock.getArguments()[1]);
            ActionListener listener = (ActionListener) invocationOnMock.getArguments()[2];
            listener.onResponse(new BulkResponse(new BulkItemResponse[0], 0L));
            return null;
        }).when(client).execute(any(), any(), any());
        return client;
    }
}
