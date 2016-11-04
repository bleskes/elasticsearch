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
package org.elasticsearch.xpack.prelert.job.metadata;

import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.cluster.metadata.MetaData;
import org.elasticsearch.common.io.stream.InputStreamStreamInput;
import org.elasticsearch.common.io.stream.OutputStreamStreamOutput;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.rest.RestStatus;
import org.elasticsearch.test.ESTestCase;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.elasticsearch.xpack.prelert.job.JobTests.buildJobBuilder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;

public class PrelertMetadataTests extends ESTestCase {

    public void testSerialization() throws Exception {
        PrelertMetadata.Builder builder = new PrelertMetadata.Builder();

        // NORELEASE: randomize jobs once it is moved over to ES' xcontent:
        builder.putJob(buildJobBuilder("job1").build(), false);
        builder.putJob(buildJobBuilder("job2").build(), false);
        builder.putJob(buildJobBuilder("job3").build(), false);

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
        builder.putJob(buildJobBuilder("job1").build(), false);
        builder.putJob(buildJobBuilder("job2").build(), false);
        builder.putJob(buildJobBuilder("job3").build(), false);

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
        builder.putJob(buildJobBuilder("1").build(), false);
        builder.putJob(buildJobBuilder("2").build(), false);

        ElasticsearchStatusException e = expectThrows(ElasticsearchStatusException.class,
                () -> builder.putJob(buildJobBuilder("2").build(), false));
        assertThat(e.status(), equalTo(RestStatus.BAD_REQUEST));
        assertThat(e.getHeader("errorCode").get(0), equalTo("10110"));

        builder.putJob(buildJobBuilder("2").build(), true);

        PrelertMetadata result = builder.build();
        assertThat(result.getJobs().size(), equalTo(2));
        assertThat(result.getJobs().get("1"), notNullValue());
        assertThat(result.getJobs().get("2"), notNullValue());
    }

}
