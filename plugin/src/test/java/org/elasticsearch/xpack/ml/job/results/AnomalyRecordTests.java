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
package org.elasticsearch.xpack.ml.job.results;

import org.elasticsearch.common.io.stream.Writeable;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.xpack.ml.support.AbstractSerializingTestCase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class AnomalyRecordTests extends AbstractSerializingTestCase<AnomalyRecord> {

    @Override
    protected AnomalyRecord createTestInstance() {
        return createTestInstance("foo", 1);
    }

    public AnomalyRecord createTestInstance(String jobId, int sequenceNum) {
        AnomalyRecord anomalyRecord = new AnomalyRecord(jobId, new Date(randomNonNegativeLong()), randomNonNegativeLong(), sequenceNum);
        anomalyRecord.setActual(Collections.singletonList(randomDouble()));
        anomalyRecord.setTypical(Collections.singletonList(randomDouble()));
        anomalyRecord.setProbability(randomDouble());
        anomalyRecord.setRecordScore(randomDouble());
        anomalyRecord.setInitialRecordScore(randomDouble());
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
    protected AnomalyRecord parseInstance(XContentParser parser) {
        return AnomalyRecord.PARSER.apply(parser, null);
    }

    @SuppressWarnings("unchecked")
    public void testToXContentIncludesInputFields() throws IOException {
        AnomalyRecord record = createTestInstance();
        record.setByFieldName("byfn");
        record.setByFieldValue("byfv");
        record.setOverFieldName("overfn");
        record.setOverFieldValue("overfv");
        record.setPartitionFieldName("partfn");
        record.setPartitionFieldValue("partfv");

        Influence influence1 = new Influence("inffn", Arrays.asList("inffv1", "inffv2"));
        Influence influence2 = new Influence("inffn", Arrays.asList("inffv1", "inffv2"));
        record.setInfluencers(Arrays.asList(influence1, influence2));

        XContentBuilder builder = toXContent(record, XContentType.JSON);
        XContentParser parser = createParser(builder);
        Map<String, Object> map = parser.map();
        List<String> serialisedByFieldValues = (List<String>) map.get(record.getByFieldName());
        assertEquals(Collections.singletonList(record.getByFieldValue()), serialisedByFieldValues);
        List<String> serialisedOverFieldValues = (List<String>) map.get(record.getOverFieldName());
        assertEquals(Collections.singletonList(record.getOverFieldValue()), serialisedOverFieldValues);
        List<String> serialisedPartFieldValues = (List<String>) map.get(record.getPartitionFieldName());
        assertEquals(Collections.singletonList(record.getPartitionFieldValue()), serialisedPartFieldValues);

        List<String> serialisedInfFieldValues1 = (List<String>) map.get(influence1.getInfluencerFieldName());
        assertEquals(influence1.getInfluencerFieldValues(), serialisedInfFieldValues1);
        List<String> serialisedInfFieldValues2 = (List<String>) map.get(influence2.getInfluencerFieldName());
        assertEquals(influence2.getInfluencerFieldValues(), serialisedInfFieldValues2);
    }

    public void testToXContentOrdersDuplicateInputFields() throws IOException {
        AnomalyRecord record = createTestInstance();
        record.setByFieldName("car-make");
        record.setByFieldValue("ford");
        record.setOverFieldName("number-of-wheels");
        record.setOverFieldValue("4");
        record.setPartitionFieldName("spoiler");
        record.setPartitionFieldValue("yes");

        Influence influence1 = new Influence("car-make", Collections.singletonList("VW"));
        Influence influence2 = new Influence("number-of-wheels", Collections.singletonList("18"));
        Influence influence3 = new Influence("spoiler", Collections.singletonList("no"));
        record.setInfluencers(Arrays.asList(influence1, influence2, influence3));

        // influencer fields with the same name as a by/over/partitiion field
        // come second in the list
        XContentBuilder builder = toXContent(record, XContentType.JSON);
        XContentParser parser = createParser(builder);
        Map<String, Object> map = parser.map();
        List<String> serialisedCarMakeFieldValues = (List<String>) map.get("car-make");
        assertEquals(Arrays.asList("ford", "VW"), serialisedCarMakeFieldValues);
        List<String> serialisedNumberOfWheelsFieldValues = (List<String>) map.get("number-of-wheels");
        assertEquals(Arrays.asList("4", "18"), serialisedNumberOfWheelsFieldValues);
        List<String> serialisedSpoilerFieldValues = (List<String>) map.get("spoiler");
        assertEquals(Arrays.asList("yes", "no"), serialisedSpoilerFieldValues);
    }

    @SuppressWarnings("unchecked")
    public void testToXContentDoesNotIncludesReservedWordInputFields() throws IOException {
        AnomalyRecord record = createTestInstance();
        record.setByFieldName(AnomalyRecord.BUCKET_SPAN.getPreferredName());
        record.setByFieldValue("bar");

        XContentBuilder builder = toXContent(record, XContentType.JSON);
        XContentParser parser = createParser(builder);
        Object value = parser.map().get(AnomalyRecord.BUCKET_SPAN.getPreferredName());
        assertNotEquals("bar", value);
        assertEquals((Long)record.getBucketSpan(), (Long)value);
    }
}
