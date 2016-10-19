/*
 * ELASTICSEARCH CONFIDENTIAL
 * __________________
 *
 *  [2014] Elasticsearch Incorporated. All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Elasticsearch Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Elasticsearch Incorporated
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Elasticsearch Incorporated.
 */
package org.elasticsearch.xpack.prelert.job.metadata;

import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.common.io.stream.InputStreamStreamInput;
import org.elasticsearch.common.io.stream.OutputStreamStreamOutput;
import org.elasticsearch.common.xcontent.*;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.test.ESTestCase;
import org.elasticsearch.xpack.prelert.job.JobConfiguration;
import org.elasticsearch.xpack.prelert.job.JobDetails;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public class PrelertMetadataTest extends ESTestCase {

    public void testSerialization() throws Exception {
        PrelertMetadata.Builder builder = new PrelertMetadata.Builder();

        // NORELEASE: randomize jobs once it is moved over to ES' xcontent:
        builder.putJob(new Job(new JobConfiguration("job1").build()), false);
        builder.putJob(new Job(new JobConfiguration("job2").build()), false);
        builder.putJob(new Job(new JobConfiguration("job3").build()), false);

        builder.putAllocation("job1", "node1");
        builder.putAllocation("job2", "node1");
        builder.putAllocation("job3", "node1");

        PrelertMetadata expected = builder.build();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        expected.writeTo(new OutputStreamStreamOutput(out));

        PrelertMetadata result = (PrelertMetadata)
                PrelertMetadata.PROTO.readFrom(new InputStreamStreamInput(new ByteArrayInputStream(out.toByteArray())));
        assertThat(result, equalTo(expected));
    }

    public void testFromXContent() throws IOException {
        PrelertMetadata.Builder builder = new PrelertMetadata.Builder();

        // NORELEASE: randomize jobs once it is moved over to ES' xcontent:
        builder.putJob(new Job(new JobConfiguration("job1").build()), false);
        builder.putJob(new Job(new JobConfiguration("job2").build()), false);
        builder.putJob(new Job(new JobConfiguration("job3").build()), false);

        builder.putAllocation("job1", "node1");
        builder.putAllocation("job2", "node1");
        builder.putAllocation("job3", "node1");

        PrelertMetadata expected = builder.build();

        XContentBuilder xBuilder = XContentFactory.contentBuilder(XContentType.SMILE);
        xBuilder.prettyPrint();
        xBuilder.startObject();
        expected.toXContent(xBuilder, ToXContent.EMPTY_PARAMS);
        xBuilder.endObject();
        XContentBuilder shuffled = shuffleXContent(xBuilder);
        final XContentParser parser = XContentFactory.xContent(shuffled.bytes()).createParser(shuffled.bytes());
        MetaData.Custom custom = expected.fromXContent(parser);
        assertTrue(custom instanceof PrelertMetadata);
        PrelertMetadata result = (PrelertMetadata) custom;
        assertThat(result, equalTo(expected));
    }

    public void testPutJob() {
        PrelertMetadata.Builder builder = new PrelertMetadata.Builder();
        builder.putJob(new Job(new JobConfiguration("1").build()), false);
        builder.putJob(new Job(new JobConfiguration("2").build()), false);

        ElasticsearchStatusException e = expectThrows(ElasticsearchStatusException.class,
                () -> builder.putJob(new Job(new JobConfiguration("2").build()), false));
        assertThat(e.status(), equalTo(RestStatus.BAD_REQUEST));
        assertThat(e.getHeader("errorCode").get(0), equalTo("10110"));

        builder.putJob(new Job(new JobConfiguration("2").build()), true);

        PrelertMetadata result = builder.build();
        assertThat(result.getJobs().size(), equalTo(2));
        assertThat(result.getJobs().get("1"), notNullValue());
        assertThat(result.getJobs().get("2"), notNullValue());
    }

}
