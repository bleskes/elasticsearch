/*
 * ELASTICSEARCH CONFIDENTIAL
 *
 * Copyright (c) 2017 Elasticsearch BV. All Rights Reserved.
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

package org.elasticsearch.xpack.ml.job.persistence;

import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.ml.job.process.normalizer.BucketNormalizable;
import org.elasticsearch.xpack.ml.job.results.Bucket;
import org.elasticsearch.xpack.ml.job.results.BucketInfluencer;

import java.util.Date;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JobRenormalizedResultsPersisterTests extends ESTestCase {

    public void testUpdateBucket() {
        BucketNormalizable bn = createBucketNormalizable();
        JobRenormalizedResultsPersister persister = createJobRenormalizedResultsPersister();
        persister.updateBucket(bn);

        assertEquals(3, persister.getBulkRequest().numberOfActions());
        assertThat(persister.getBulkRequest().requests().get(0).toString(), containsString("foo-index"));
    }

    public void testExecuteRequestResetsBulkRequest() {
        BucketNormalizable bn = createBucketNormalizable();
        JobRenormalizedResultsPersister persister = createJobRenormalizedResultsPersister();
        persister.updateBucket(bn);
        persister.executeRequest();
        assertEquals(0, persister.getBulkRequest().numberOfActions());
    }

    private JobRenormalizedResultsPersister createJobRenormalizedResultsPersister() {
        BulkResponse bulkResponse = mock(BulkResponse.class);
        when(bulkResponse.hasFailures()).thenReturn(false);

        Client client = new MockClientBuilder("cluster").bulk(bulkResponse).build();
        return new JobRenormalizedResultsPersister("foo", Settings.EMPTY, client);
    }

    private BucketNormalizable createBucketNormalizable() {
        Date now = new Date();
        Bucket bucket = new Bucket("foo", now, 1);
        bucket.addBucketInfluencer(new BucketInfluencer("foo", now, 1));
        bucket.addBucketInfluencer(new BucketInfluencer("foo", now, 1));
        return new BucketNormalizable(bucket, "foo-index");
    }
}
