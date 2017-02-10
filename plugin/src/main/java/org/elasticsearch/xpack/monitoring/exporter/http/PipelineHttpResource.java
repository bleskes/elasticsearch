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
package org.elasticsearch.xpack.monitoring.exporter.http;

import org.apache.http.HttpEntity;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.logging.log4j.Logger;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.common.Nullable;
import org.elasticsearch.common.logging.Loggers;
import org.elasticsearch.common.unit.TimeValue;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * {@code PipelineHttpResource}s allow the checking and uploading of ingest pipelines to a remote cluster.
 * <p>
 * In the future, we will need to also support the transformation or replacement of pipelines based on their version, but we do not need
 * that functionality until some breaking change in the Monitoring API requires it.
 */
public class PipelineHttpResource extends PublishableHttpResource {

    private static final Logger logger = Loggers.getLogger(PipelineHttpResource.class);

    /**
     * The name of the pipeline that is sent to the remote cluster.
     */
    private final String pipelineName;
    /**
     * Provides a fully formed template (e.g., no variables that need replaced).
     */
    private final Supplier<byte[]> pipeline;

    /**
     * Create a new {@link PipelineHttpResource}.
     *
     * @param resourceOwnerName The user-recognizable name
     * @param masterTimeout Master timeout to use with any request.
     * @param pipelineName The name of the template (e.g., ".pipeline123").
     * @param pipeline The pipeline provider.
     */
    public PipelineHttpResource(final String resourceOwnerName, @Nullable final TimeValue masterTimeout,
                                final String pipelineName, final Supplier<byte[]> pipeline) {
        super(resourceOwnerName, masterTimeout, PublishableHttpResource.NO_BODY_PARAMETERS);

        this.pipelineName = Objects.requireNonNull(pipelineName);
        this.pipeline = Objects.requireNonNull(pipeline);
    }

    /**
     * Determine if the current {@linkplain #pipelineName pipeline} exists.
     */
    @Override
    protected CheckResponse doCheck(final RestClient client) {
        return simpleCheckForResource(client, logger,
                                      "/_ingest/pipeline", pipelineName, "monitoring pipeline",
                                      resourceOwnerName, "monitoring cluster");
    }

    /**
     * Publish the current {@linkplain #pipelineName pipeline}.
     */
    @Override
    protected boolean doPublish(final RestClient client) {
        return putResource(client, logger,
                           "/_ingest/pipeline", pipelineName, this::pipelineToHttpEntity, "monitoring pipeline",
                           resourceOwnerName, "monitoring cluster");
    }

    /**
     * Create a {@link HttpEntity} for the {@link #pipeline}.
     *
     * @return Never {@code null}.
     */
    HttpEntity pipelineToHttpEntity() {
        return new ByteArrayEntity(pipeline.get(), ContentType.APPLICATION_JSON);
    }

}
