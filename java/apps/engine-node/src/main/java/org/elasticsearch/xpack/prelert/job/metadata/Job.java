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

import org.elasticsearch.cluster.AbstractDiffable;
import org.elasticsearch.common.ParseField;
import org.elasticsearch.common.ParseFieldMatcherSupplier;
import org.elasticsearch.common.io.stream.StreamInput;
import org.elasticsearch.common.io.stream.StreamOutput;
import org.elasticsearch.common.xcontent.ConstructingObjectParser;
import org.elasticsearch.common.xcontent.ToXContent;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.xpack.prelert.job.JobDetails;

import java.io.IOException;
import java.util.Objects;

public class Job extends AbstractDiffable<Job> implements ToXContent {

    static final Job PROTO = new Job((JobDetails) null);

    public static final ConstructingObjectParser<Job, ParseFieldMatcherSupplier> PARSER =
            new ConstructingObjectParser<>("job", objects -> new Job((JobDetails) objects[0]));

    static {
        PARSER.declareObject(ConstructingObjectParser.constructorArg(), JobDetails.PARSER, new ParseField("job"));
    }

    // NORELEASE: A few fields of job details change frequently and this needs to be stored elsewhere
    // performance issue will occur if we don't change that
    // also it needs ot be converted from jackson databind to ES' xcontent
    private final JobDetails jobDetails;

    public Job(JobDetails jobDetails) {
        this.jobDetails = jobDetails;
    }

    public JobDetails getJobDetails() {
        return jobDetails;
    }

    @Override
    public Job readFrom(StreamInput in) throws IOException {
        return new Job(new JobDetails(in));
    }

    @Override
    public void writeTo(StreamOutput out) throws IOException {
        jobDetails.writeTo(out);
    }

    @Override
    public XContentBuilder toXContent(XContentBuilder builder, Params params) throws IOException {
        builder.startObject();
        builder.field("job", jobDetails);
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
