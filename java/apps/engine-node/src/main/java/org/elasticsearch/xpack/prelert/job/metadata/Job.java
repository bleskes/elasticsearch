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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.elasticsearch.ElasticsearchParseException;
import org.elasticsearch.cluster.AbstractDiffable;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentParser;
import org.elasticsearch.xpack.prelert.job.JobDetails;

import java.io.IOException;
import java.util.Objects;

public class Job extends AbstractDiffable<Job> implements ToXContent {

    private static final ObjectMapper objectMapper = new ObjectMapper();
    static final Job PROTO = new Job((JobDetails) null);

    // NORELEASE: A few fields of job details change frequently and this needs to be stored elsewhere
    // performance issue will occur if we don't change that
    // also it needs ot be converted from jackson databind to ES' xcontent
    private final JobDetails jobDetails;

    public Job(XContentParser parser) throws IOException {
        String data = null;
        String currentField = null;
        for (XContentParser.Token token = parser.nextToken(); token != XContentParser.Token.END_OBJECT; token = parser.nextToken()) {
            if (token == XContentParser.Token.FIELD_NAME) {
                currentField = parser.currentName();
            } else if (token == XContentParser.Token.VALUE_STRING) {
                if ("raw_data".equals(currentField)) {
                    data = parser.text();
                } else {
                    throw new ElasticsearchParseException("Illegal field [" + currentField + "]");
                }
            } else {
                throw new ElasticsearchParseException("Illegal token [" + token + "]");
            }
        }
        jobDetails = objectMapper.readValue(data, JobDetails.class);
    }

    public Job(JobDetails jobDetails) {
        this.jobDetails = jobDetails;
    }

    public Job(String data) throws IOException {
        jobDetails = objectMapper.readValue(data, JobDetails.class);
    }

    public JobDetails getJobDetails() {
        return jobDetails;
    }

    @Override
    public Job readFrom(StreamInput in) throws IOException {
        JobDetails jobDetails = objectMapper.readValue(in.readString(), JobDetails.class);
        return new Job(jobDetails);
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        String data = objectMapper.writeValueAsString(jobDetails);
        out.writeString(data);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        String data = objectMapper.writeValueAsString(jobDetails);
        builder.startObject();
        builder.field("raw_data", data);
        builder.endObject();
        return builder;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Job job = (Job) o;
        return Objects.equals(jobDetails, job.jobDetails);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jobDetails);
    }
}
