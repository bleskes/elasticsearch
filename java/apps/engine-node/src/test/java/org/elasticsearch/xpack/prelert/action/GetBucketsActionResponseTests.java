package org.elasticsearch.xpack.prelert.action;

import org.elasticsearch.xpack.prelert.action.GetBucketsAction.Response;
import org.elasticsearch.xpack.prelert.job.persistence.QueryPage;
import org.elasticsearch.xpack.prelert.job.results.AnomalyRecord;
import org.elasticsearch.xpack.prelert.job.results.Bucket;
import org.elasticsearch.xpack.prelert.job.results.BucketInfluencer;
import org.elasticsearch.xpack.prelert.job.results.Influencer;
import org.elasticsearch.xpack.prelert.job.results.PartitionScore;
import org.elasticsearch.xpack.prelert.support.AbstractStreamableTestCase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GetBucketsActionResponseTests extends AbstractStreamableTestCase<GetBucketsAction.Response> {

    @Override
    protected Response createTestInstance() {
        int listSize = randomInt(10);
        List<Bucket> hits = new ArrayList<>(listSize);
        for (int j = 0; j < listSize; j++) {
            Bucket bucket = new Bucket();
            if (randomBoolean()) {
                bucket.setAnomalyScore(randomDouble());
            }
            if (randomBoolean()) {
                int size = randomInt(10);
                List<BucketInfluencer> bucketInfluencers = new ArrayList<>(size);
                for (int i = 0; i < size; i++) {
                    BucketInfluencer bucketInfluencer = new BucketInfluencer();
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
                    Influencer influencer = new Influencer(randomAsciiOfLengthBetween(1, 20), randomAsciiOfLengthBetween(1, 20));
                    influencer.setAnomalyScore(randomDouble());
                    influencer.setInfluencerFieldName(randomAsciiOfLengthBetween(1, 20));
                    influencer.setInfluencerFieldValue(randomAsciiOfLengthBetween(1, 20));
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
                    AnomalyRecord anomalyRecord = new AnomalyRecord();
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
            hits.add(bucket);
        }
        QueryPage<Bucket> buckets = new QueryPage<>(hits, listSize);
        return new Response(buckets);
    }

    @Override
    protected Response createBlankInstance() {
        return new Response();
    }

}
