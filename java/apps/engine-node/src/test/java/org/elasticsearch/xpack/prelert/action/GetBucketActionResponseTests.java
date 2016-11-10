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
package org.elasticsearch.xpack.prelert.action;

import org.elasticsearch.xpack.prelert.action.GetBucketAction.Response;
import org.elasticsearch.xpack.prelert.job.results.AnomalyRecord;
import org.elasticsearch.xpack.prelert.job.results.Bucket;
import org.elasticsearch.xpack.prelert.job.results.BucketInfluencer;
import org.elasticsearch.xpack.prelert.job.results.Influencer;
import org.elasticsearch.xpack.prelert.job.results.PartitionScore;
import org.elasticsearch.xpack.prelert.support.AbstractStreamableTestCase;
import org.elasticsearch.xpack.prelert.utils.SingleDocument;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GetBucketActionResponseTests extends AbstractStreamableTestCase<GetBucketAction.Response> {

    @Override
    protected Response createTestInstance() {
        if (randomBoolean()) {
            return new Response();
        } else {
            String jobId = "foo";
            Bucket bucket = new Bucket(jobId);
            if (randomBoolean()) {
                bucket.setAnomalyScore(randomDouble());
            }
            if (randomBoolean()) {
                int size = randomInt(10);
                List<BucketInfluencer> bucketInfluencers = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    BucketInfluencer bucketInfluencer = new BucketInfluencer("foo");
                    bucketInfluencer.setAnomalyScore(randomDouble());
                    bucketInfluencer.setInfluencerFieldName(randomAsciiOfLengthBetween(1, 20));
                    bucketInfluencer.setInitialAnomalyScore(randomDouble());
                    bucketInfluencer.setProbability(randomDouble());
                    bucketInfluencer.setRawAnomalyScore(randomDouble());
                    bucketInfluencers.add(bucketInfluencer);
                }
                bucket.setBucketInfluencers(bucketInfluencers);
            }
            if (randomBoolean()) {
                bucket.setBucketSpan(randomPositiveLong());
            }
            if (randomBoolean()) {
                bucket.setEventCount(randomPositiveLong());
            }
            if (randomBoolean()) {
                bucket.setId(randomAsciiOfLengthBetween(1, 20));
            }
            if (randomBoolean()) {
                int size = randomInt(10);
                List<Influencer> influencers = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    Influencer influencer = new Influencer(randomAsciiOfLengthBetween(1, 20), randomAsciiOfLengthBetween(1, 20),
                            randomAsciiOfLengthBetween(1, 20));
                    influencer.setAnomalyScore(randomDouble());
                    influencer.setInitialAnomalyScore(randomDouble());
                    influencer.setProbability(randomDouble());
                    influencer.setId(randomAsciiOfLengthBetween(1, 20));
                    influencer.setInterim(randomBoolean());
                    influencer.setTimestamp(new Date(randomLong()));
                    influencers.add(influencer);
                }
                bucket.setInfluencers(influencers);
            }
            if (randomBoolean()) {
                bucket.setInitialAnomalyScore(randomDouble());
            }
            if (randomBoolean()) {
                bucket.setInterim(randomBoolean());
            }
            if (randomBoolean()) {
                bucket.setMaxNormalizedProbability(randomDouble());
            }
            if (randomBoolean()) {
                int size = randomInt(10);
                List<PartitionScore> partitionScores = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    partitionScores.add(new PartitionScore(randomAsciiOfLengthBetween(1, 20), randomAsciiOfLengthBetween(1, 20),
                            randomDouble(), randomDouble()));
                }
                bucket.setPartitionScores(partitionScores);
            }
            if (randomBoolean()) {
                int size = randomInt(10);
                Map<String, Double> perPartitionMaxProbability = new HashMap<>(size);
                for (int i = 0; i < size; i++) {
                    perPartitionMaxProbability.put(randomAsciiOfLengthBetween(1, 20), randomDouble());
                }
                bucket.setPerPartitionMaxProbability(perPartitionMaxProbability);
            }
            if (randomBoolean()) {
                bucket.setProcessingTimeMs(randomLong());
            }
            if (randomBoolean()) {
                bucket.setRecordCount(randomInt());
            }
            if (randomBoolean()) {
                int size = randomInt(10);
                List<AnomalyRecord> records = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    AnomalyRecord anomalyRecord = new AnomalyRecord(jobId);
                    anomalyRecord.setAnomalyScore(randomDouble());
                    anomalyRecord.setActual(Collections.singletonList(randomDouble()));
                    anomalyRecord.setTypical(Collections.singletonList(randomDouble()));
                    anomalyRecord.setProbability(randomDouble());
                    anomalyRecord.setId(randomAsciiOfLengthBetween(1, 20));
                    anomalyRecord.setInterim(randomBoolean());
                    anomalyRecord.setTimestamp(new Date(randomLong()));
                    records.add(anomalyRecord);
                }
                bucket.setRecords(records);
            }
            if (randomBoolean()) {
                bucket.setTimestamp(new Date(randomLong()));
            }
            SingleDocument<Bucket> result = new SingleDocument<>(Bucket.TYPE.getPreferredName(), bucket);
            return new GetBucketAction.Response(result);
        }
    }

    @Override
    protected Response createBlankInstance() {
        return new GetBucketAction.Response();
    }

}
