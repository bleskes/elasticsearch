/************************************************************
 *                                                          *
 * Contents of file Copyright (c) Prelert Ltd 2006-2016     *
 *                                                          *
 *----------------------------------------------------------*
 *----------------------------------------------------------*
 * WARNING:                                                 *
 * THIS FILE CONTAINS UNPUBLISHED PROPRIETARY               *
 * SOURCE CODE WHICH IS THE PROPERTY OF PRELERT LTD AND     *
 * PARENT OR SUBSIDIARY COMPANIES.                          *
 * PLEASE READ THE FOLLOWING AND TAKE CAREFUL NOTE:         *
 *                                                          *
 * This source code is confidential and any person who      *
 * receives a copy of it, or believes that they are viewing *
 * it without permission is asked to notify Prelert Ltd     *
 * on +44 (0)20 3567 1249 or email to legal@prelert.com.    *
 * All intellectual property rights in this source code     *
 * are owned by Prelert Ltd.  No part of this source code   *
 * may be reproduced, adapted or transmitted in any form or *
 * by any means, electronic, mechanical, photocopying,      *
 * recording or otherwise.                                  *
 *                                                          *
 *----------------------------------------------------------*
 *                                                          *
 *                                                          *
 ************************************************************/

package com.prelert.job.persistence.elasticsearch;

import java.util.Objects;

import org.apache.log4j.Logger;
import org.elasticsearch.action.ShardOperationFailedException;
import org.elasticsearch.action.admin.indices.forcemerge.ForceMergeAction;
import org.elasticsearch.action.admin.indices.forcemerge.ForceMergeResponse;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.action.delete.DeleteAction;
import org.elasticsearch.action.delete.DeleteRequestBuilder;
import org.elasticsearch.action.search.SearchAction;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.index.query.HasParentQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

import com.prelert.job.persistence.JobDataDeleter;
import com.prelert.job.results.AnomalyRecord;
import com.prelert.job.results.Bucket;
import com.prelert.job.results.Influencer;

public class ElasticsearchBulkDeleter implements JobDataDeleter
{
    private static final Logger LOGGER = Logger.getLogger(ElasticsearchBulkDeleter.class);

    private final Client m_Client;
    private final ElasticsearchJobId m_JobId;
    private final BulkRequestBuilder m_BulkRequestBuilder;
    private long m_DeletedBucketCount;
    private long m_DeletedRecordCount;
    private long m_DeletedInfluencerCount;

    public ElasticsearchBulkDeleter(Client client, String jobId)
    {
        m_Client = Objects.requireNonNull(client);
        m_JobId = new ElasticsearchJobId(jobId);
        m_BulkRequestBuilder = client.prepareBulk();
        m_DeletedBucketCount = 0;
        m_DeletedRecordCount = 0;
        m_DeletedInfluencerCount = 0;
    }

    @Override
    public void deleteBucket(Bucket bucket)
    {
        deleteRecords(bucket);
        m_BulkRequestBuilder.add(
                m_Client.prepareDelete(m_JobId.getIndex(), Bucket.TYPE, bucket.getId()));
        m_DeletedBucketCount++;
    }

    @Override
    public void deleteRecords(Bucket bucket)
    {
        HasParentQueryBuilder recordsQuery = QueryBuilders.hasParentQuery(Bucket.TYPE,
                QueryBuilders.matchQuery(Bucket.ID, bucket.getId()));

        SearchResponse searchResponse = SearchAction.INSTANCE.newRequestBuilder(m_Client)
                .setIndices(m_JobId.getIndex())
                .setQuery(recordsQuery)
                .execute().actionGet();

        for (SearchHit hit : searchResponse.getHits())
        {
            DeleteRequestBuilder deleteRequest = DeleteAction.INSTANCE.newRequestBuilder(m_Client)
                    .setIndex(m_JobId.getIndex())
                    .setParent(bucket.getId())
                    .setType(AnomalyRecord.TYPE)
                    .setId(hit.getId());
            m_BulkRequestBuilder.add(deleteRequest);
            m_DeletedRecordCount++;
        }
    }

    @Override
    public void deleteInfluencer(Influencer influencer)
    {
        m_BulkRequestBuilder.add(
                m_Client.prepareDelete(m_JobId.getIndex(), Influencer.TYPE, influencer.getId()));
        m_DeletedInfluencerCount++;
    }

    @Override
    public void commit()
    {
        if (m_BulkRequestBuilder.numberOfActions() == 0)
        {
            return;
        }

        LOGGER.debug("Requesting deletion of " + m_DeletedInfluencerCount + " influencers, "
                 + m_DeletedBucketCount + " buckets, " + " and " + m_DeletedRecordCount + " records");

        try
        {
            executeBulkRequest();
        }
        catch (RuntimeException e)
        {
            LOGGER.error("Failed to perform bulk delete", e);
        }
    }

    private void executeBulkRequest()
    {
        BulkResponse bulkResponse = m_BulkRequestBuilder.execute().actionGet();
        if (bulkResponse.hasFailures())
        {
            LOGGER.error(bulkResponse.buildFailureMessage());
        }

        // We need to do a force-merge request to ACTUALLY delete the results from disk
        ForceMergeResponse forceMergeResponse = ForceMergeAction.INSTANCE.newRequestBuilder(m_Client)
                .setIndices(m_JobId.getIndex())
                .setOnlyExpungeDeletes(true)
                .get();
        for (ShardOperationFailedException shardFailure: forceMergeResponse.getShardFailures())
        {
            LOGGER.error("Shard failed during force-merge: " + shardFailure.reason());
        }
    }
}
