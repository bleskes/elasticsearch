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

package org.elasticsearch.xpack.monitoring.agent.exporter.local;

import org.apache.logging.log4j.Logger;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.xpack.monitoring.agent.exporter.ExportBulk;
import org.elasticsearch.xpack.monitoring.agent.exporter.ExportException;
import org.elasticsearch.xpack.monitoring.agent.exporter.MonitoringDoc;
import org.elasticsearch.xpack.monitoring.agent.resolver.MonitoringIndexNameResolver;
import org.elasticsearch.xpack.monitoring.agent.resolver.ResolversRegistry;
import org.elasticsearch.xpack.security.InternalClient;

import java.util.Arrays;
import java.util.Collection;

import static org.elasticsearch.xpack.monitoring.agent.exporter.Exporter.EXPORT_PIPELINE_NAME;

/**
 * LocalBulk exports monitoring data in the local cluster using bulk requests. Its usage is not thread safe since the
 * {@link LocalBulk#add(Collection)},  {@link LocalBulk#flush()} and  {@link LocalBulk#doClose()} methods are not synchronized.
 */
public class LocalBulk extends ExportBulk {

    private final Logger logger;
    private final InternalClient client;
    private final ResolversRegistry resolvers;
    private final boolean usePipeline;

    private BulkRequestBuilder requestBuilder;


    public LocalBulk(String name, Logger logger, InternalClient client, ResolversRegistry resolvers, boolean usePipeline) {
        super(name);
        this.logger = logger;
        this.client = client;
        this.resolvers = resolvers;
        this.usePipeline = usePipeline;
    }

    @Override
    public void doAdd(Collection<MonitoringDoc> docs) throws ExportException {
        ExportException exception = null;

        for (MonitoringDoc doc : docs) {
            if (isClosed()) {
                return;
            }
            if (requestBuilder == null) {
                requestBuilder = client.prepareBulk();
            }

            try {
                MonitoringIndexNameResolver<MonitoringDoc> resolver = resolvers.getResolver(doc);
                IndexRequest request = new IndexRequest(resolver.index(doc), resolver.type(doc), resolver.id(doc));
                request.source(resolver.source(doc, XContentType.SMILE));

                // allow the use of ingest pipelines to be completely optional
                if (usePipeline) {
                    request.setPipeline(EXPORT_PIPELINE_NAME);
                }

                requestBuilder.add(request);

                if (logger.isTraceEnabled()) {
                    logger.trace("local exporter [{}] - added index request [index={}, type={}, id={}, pipeline={}]",
                                 name, request.index(), request.type(), request.id(), request.getPipeline());
                }
            } catch (Exception e) {
                if (exception == null) {
                    exception = new ExportException("failed to add documents to export bulk [{}]", name);
                }
                exception.addExportException(new ExportException("failed to add document [{}]", e, doc, name));
            }
        }

        if (exception != null) {
            throw exception;
        }
    }

    @Override
    public void doFlush() throws ExportException {
        if (requestBuilder == null || requestBuilder.numberOfActions() == 0 || isClosed()) {
            return;
        }
        try {
            logger.trace("exporter [{}] - exporting {} documents", name, requestBuilder.numberOfActions());
            BulkResponse bulkResponse = requestBuilder.get();

            if (bulkResponse.hasFailures()) {
                throwExportException(bulkResponse.getItems());
            }
        } catch (Exception e) {
            throw new ExportException("failed to flush export bulk [{}]", e, name);
        } finally {
            requestBuilder = null;
        }
    }

    void throwExportException(BulkItemResponse[] bulkItemResponses) {
        ExportException exception = new ExportException("bulk [{}] reports failures when exporting documents", name);

        Arrays.stream(bulkItemResponses)
                .filter(BulkItemResponse::isFailed)
                .map(item -> new ExportException(item.getFailure().getCause()))
                .forEach(exception::addExportException);

        if (exception.hasExportExceptions()) {
            throw exception;
        }
    }

    @Override
    protected void doClose() throws ExportException {
        if (isClosed() == false) {
            requestBuilder = null;
        }
    }
}
