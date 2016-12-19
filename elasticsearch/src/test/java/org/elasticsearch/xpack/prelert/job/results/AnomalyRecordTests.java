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
public class AnomalyRecordTests extends AbstractSerializingTestCase<AnomalyRecord> {

    @Override
    protected AnomalyRecord createTestInstance() {
        return createTestInstance("foo", 1);
    }

    public AnomalyRecord createTestInstance(String jobId, int sequenceNum) {
        AnomalyRecord anomalyRecord = new AnomalyRecord(jobId, new Date(randomPositiveLong()), randomPositiveLong(), sequenceNum);
        anomalyRecord.setActual(Collections.singletonList(randomDouble()));
        anomalyRecord.setTypical(Collections.singletonList(randomDouble()));
        anomalyRecord.setAnomalyScore(randomDouble());
        anomalyRecord.setProbability(randomDouble());
        anomalyRecord.setNormalizedProbability(randomDouble());
        anomalyRecord.setInitialNormalizedProbability(randomDouble());
        anomalyRecord.setInterim(randomBoolean());
        if (randomBoolean()) {
            anomalyRecord.setFieldName(randomAsciiOfLength(12));
        }
        if (randomBoolean()) {
            anomalyRecord.setByFieldName(randomAsciiOfLength(12));
            anomalyRecord.setByFieldValue(randomAsciiOfLength(12));
        }
        if (randomBoolean()) {
            anomalyRecord.setPartitionFieldName(randomAsciiOfLength(12));
            anomalyRecord.setPartitionFieldValue(randomAsciiOfLength(12));
        }
        if (randomBoolean()) {
            anomalyRecord.setOverFieldName(randomAsciiOfLength(12));
            anomalyRecord.setOverFieldValue(randomAsciiOfLength(12));
        }
        anomalyRecord.setFunction(randomAsciiOfLengthBetween(5, 20));
        anomalyRecord.setFunctionDescription(randomAsciiOfLengthBetween(5, 20));
        if (randomBoolean()) {
            anomalyRecord.setCorrelatedByFieldValue(randomAsciiOfLength(16));
        }
        if (randomBoolean()) {
            int count = randomIntBetween(0, 9);
            List<Influence>  influences = new ArrayList<>();
            for (int i=0; i<count; i++) {
                influences.add(new Influence(randomAsciiOfLength(8), Collections.singletonList(randomAsciiOfLengthBetween(1, 28))));
            }
            anomalyRecord.setInfluencers(influences);
        }
        if (randomBoolean()) {
            int count = randomIntBetween(0, 9);
            List<AnomalyCause>  causes = new ArrayList<>();
            for (int i=0; i<count; i++) {
                causes.add(new AnomalyCauseTests().createTestInstance());
            }
            anomalyRecord.setCauses(causes);
        }

        return anomalyRecord;
    }

    @Override
    protected Writeable.Reader<AnomalyRecord> instanceReader() {
        return AnomalyRecord::new;
    }

    @Override
    protected AnomalyRecord parseInstance(XContentParser parser, ParseFieldMatcher matcher) {
        return AnomalyRecord.PARSER.apply(parser, () -> matcher);
    }
}
